package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 路由配置匹配器。
 */
@Slf4j
@Component
public class RoutingConfigMatcher {

    private final RoutingConfigClient routingConfigClient;
    private final FallbackModelSelector fallbackModelSelector;
    private final ModelHealthResolver modelHealthResolver;

    public RoutingConfigMatcher(RoutingConfigClient routingConfigClient,
                                FallbackModelSelector fallbackModelSelector) {
        this.routingConfigClient = routingConfigClient;
        this.fallbackModelSelector = fallbackModelSelector;
        this.modelHealthResolver = new ModelHealthResolver();
    }

    @Autowired
    public RoutingConfigMatcher(RoutingConfigClient routingConfigClient,
                                FallbackModelSelector fallbackModelSelector,
                                ModelHealthResolver modelHealthResolver) {
        this.routingConfigClient = routingConfigClient;
        this.fallbackModelSelector = fallbackModelSelector;
        this.modelHealthResolver = modelHealthResolver;
    }

    public RuleDecision evaluate(String tenantId, RuleMatchContext context) {
        String sourceModel = context != null ? context.getSourceModel() : null;
        RoutingConfigCache cache = routingConfigClient.loadActiveConfig(tenantId);
        if (cache == null || !cache.isPresent() || cache.getRules() == null || cache.getRules().isEmpty()) {
            return RuleDecision.noMatch(sourceModel);
        }
        return cache.getRules().stream()
            .filter(Objects::nonNull)
            .filter(rule -> "0".equals(rule.getStatus()) || "ENABLED".equalsIgnoreCase(rule.getStatus()))
            .sorted(Comparator.comparing(rule -> rule.getPriority() != null ? rule.getPriority() : Integer.MAX_VALUE))
            .filter(rule -> matches(rule, context))
            .findFirst()
            .map(rule -> decisionFor(rule, context))
            .orElseGet(() -> RuleDecision.noMatch(sourceModel));
    }

    private boolean matches(RoutingConfigRuleCache rule, RuleMatchContext context) {
        if (context == null) {
            return false;
        }
        List<Boolean> conditionResults = new ArrayList<>();
        if (hasValues(rule.getApiKeyIds())) {
            conditionResults.add(matchesAny(rule.getApiKeyIds(), context.getApiKeyId()));
        }
        List<String> departmentValues = hasValues(rule.getDepartments()) ? rule.getDepartments() : rule.getTeamTags();
        if (hasValues(departmentValues)) {
            conditionResults.add(matchesAny(departmentValues, nonBlank(context.getDepartment(), context.getTeamTag())));
        }
        if (hasValues(rule.getUserIds())) {
            conditionResults.add(matchesAny(rule.getUserIds(), context.getUserId()));
        }
        if (hasValues(rule.getAppIds())) {
            conditionResults.add(matchesAny(rule.getAppIds(), context.getAppId()));
        }
        if (hasValues(rule.getPaths())) {
            conditionResults.add(matchesPath(rule.getPaths(), context.getPath()));
        }
        if (hasValues(rule.getSourceModels())) {
            conditionResults.add(matchesAny(rule.getSourceModels(), context.getSourceModel()));
        }
        if (hasValues(rule.getModelTypes())) {
            conditionResults.add(matchesAnyIgnoreCase(rule.getModelTypes(), context.getModelType()));
        }
        if (hasValues(rule.getKeywords())) {
            conditionResults.add(matchesKeywords(rule.getKeywords(), context.getRequest(), rule.getKeywordLogic()));
        }
        if (hasValues(rule.getTools())) {
            conditionResults.add(matchesTools(rule.getTools(), context.getRequest(), rule.getToolLogic()));
        }
        if (rule.getHeaders() != null && !rule.getHeaders().isEmpty()) {
            conditionResults.add(matchesHeaders(rule.getHeaders(), context.getHeaders()));
        }
        if (conditionResults.isEmpty()) {
            return true;
        }
        return "ANY".equals(normalize(rule.getLogic(), "ALL"))
            ? conditionResults.stream().anyMatch(Boolean.TRUE::equals)
            : conditionResults.stream().allMatch(Boolean.TRUE::equals);
    }

    private RuleDecision decisionFor(RoutingConfigRuleCache rule, RuleMatchContext context) {
        String actionType = normalize(rule.getActionType(), "ORIGINAL_MODEL");
        String executionMode = normalize(rule.getExecutionMode(), "ENFORCE");
        String sourceModel = context.getSourceModel();
        ClassificationDecision classification = classify(rule, context);
        String proposedTarget = switch (actionType) {
            case "TARGET_MODEL" -> nonBlank(rule.getTargetModel(), sourceModel);
            case "MODEL_GROUP" -> firstHealthy(rule.getModelGroup(), rule.getModelHealth(), sourceModel);
            case "CLASSIFIER" -> classification.targetModel();
            default -> sourceModel;
        };
        String effectiveTarget = proposedTarget;
        boolean fallbackApplied = false;
        String fallbackModel = null;
        String fallbackReason = null;
        if (!"DENY".equals(actionType) && !modelHealthResolver.isHealthy(proposedTarget, rule.getModelHealth())) {
            fallbackModel = fallbackModelSelector.selectFirstHealthy(rule.getFallbackModels(), rule.getModelHealth());
            if (fallbackModel != null) {
                effectiveTarget = fallbackModel;
                fallbackApplied = true;
                fallbackReason = "ORIGINAL_MODEL".equals(actionType) ? "SOURCE_UNHEALTHY" : "TARGET_UNHEALTHY";
            }
        }
        if ("RECORD_ONLY".equals(executionMode)) {
            effectiveTarget = sourceModel;
        }
        return RuleDecision.builder()
            .matched(true)
            .ruleId(rule.getRuleId())
            .ruleName(rule.getRuleName())
            .sourceModel(sourceModel)
            .targetModel(proposedTarget)
            .effectiveTargetModel(effectiveTarget)
            .actionType(actionType)
            .executionMode(executionMode)
            .matchSummary(matchSummary(rule))
            .fallbackApplied(fallbackApplied)
            .fallbackModel(fallbackModel)
            .fallbackReason(fallbackReason)
            .classificationResult("CLASSIFIER".equals(actionType) ? classification.result() : null)
            .classifierConfidence("CLASSIFIER".equals(actionType) ? classification.confidence() : null)
            .build();
    }

    private ClassificationDecision classify(RoutingConfigRuleCache rule, RuleMatchContext context) {
        String simpleModel = nonBlank(rule.getSimpleModel(), null);
        String complexModel = nonBlank(rule.getComplexModel(), null);
        boolean complex = isComplexRequest(context);
        if (complex && complexModel != null) {
            return new ClassificationDecision(complexModel, "COMPLEX", 0.65D);
        }
        if (!complex && simpleModel != null) {
            return new ClassificationDecision(simpleModel, "SIMPLE", 0.65D);
        }
        if (simpleModel != null) {
            return new ClassificationDecision(simpleModel, "UNKNOWN", 0.5D);
        }
        if (complexModel != null) {
            return new ClassificationDecision(complexModel, "UNKNOWN", 0.5D);
        }
        return new ClassificationDecision(context.getSourceModel(), "UNKNOWN", 0.0D);
    }

    private boolean isComplexRequest(RuleMatchContext context) {
        if (context == null || context.getRequest() == null) {
            return false;
        }
        JsonNode request = context.getRequest();
        if (!extractTools(request).isEmpty()) {
            return true;
        }
        JsonNode messages = request.path("messages");
        if (messages.isArray() && messages.size() > 6) {
            return true;
        }
        String requestText = extractRequestText(request, null);
        return requestText != null && requestText.length() > 2000;
    }

    private boolean matchesAny(List<String> expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return expected.contains(actual);
    }

    private boolean matchesAnyIgnoreCase(List<String> expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return expected.stream().anyMatch(value -> value != null && value.equalsIgnoreCase(actual));
    }

    private boolean matchesPath(List<RoutingConfigRuleCache.PathCondition> paths, String actualPath) {
        if (paths == null || paths.isEmpty()) {
            return true;
        }
        if (actualPath == null) {
            return false;
        }
        for (RoutingConfigRuleCache.PathCondition path : paths) {
            if (path == null || path.getValue() == null) {
                continue;
            }
            String type = normalize(path.getType(), "PREFIX");
            if ("EXACT".equals(type) && actualPath.equals(path.getValue())) {
                return true;
            }
            if ("PREFIX".equals(type) && actualPath.startsWith(path.getValue())) {
                return true;
            }
            if ("REGEX".equals(type) && regexMatches(path.getValue(), actualPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesKeywords(List<RoutingConfigRuleCache.KeywordCondition> keywords, JsonNode request, String logic) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        int validCount = 0;
        int matchedCount = 0;
        for (RoutingConfigRuleCache.KeywordCondition keyword : keywords) {
            if (keyword == null || keyword.getValue() == null || keyword.getValue().isBlank()) {
                continue;
            }
            validCount++;
            String requestText = extractRequestText(request, keyword.getField());
            if (requestText == null || requestText.isBlank()) {
                continue;
            }
            String type = normalize(keyword.getMatchType(), "CONTAINS");
            String value = keyword.getValue();
            String actual = requestText;
            if (!keyword.isCaseSensitive()) {
                value = value.toLowerCase(Locale.ROOT);
                actual = actual.toLowerCase(Locale.ROOT);
            }
            boolean matched = (("EQUALS".equals(type) || "EXACT".equals(type)) && actual.equals(value))
                || ("PREFIX".equals(type) && actual.startsWith(value))
                || ("CONTAINS".equals(type) && actual.contains(value))
                || ("REGEX".equals(type) && regexMatches(value, actual));
            if (matched) {
                matchedCount++;
                if (!"ALL".equals(normalize(logic, "ANY"))) {
                    return true;
                }
            }
        }
        return validCount == 0 || matchedCount == validCount;
    }

    private boolean matchesTools(List<String> expectedTools, JsonNode request, String logic) {
        if (expectedTools == null || expectedTools.isEmpty()) {
            return true;
        }
        List<String> actualTools = extractTools(request);
        long matchedCount = expectedTools.stream().filter(actualTools::contains).count();
        return "ALL".equals(normalize(logic, "ANY"))
            ? matchedCount == expectedTools.size()
            : matchedCount > 0;
    }

    private boolean matchesHeaders(Map<String, String> expectedHeaders, Map<String, String> actualHeaders) {
        if (expectedHeaders == null || expectedHeaders.isEmpty()) {
            return true;
        }
        if (actualHeaders == null || actualHeaders.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> expected : expectedHeaders.entrySet()) {
            String actual = findHeader(actualHeaders, expected.getKey());
            if (!Objects.equals(expected.getValue(), actual)) {
                return false;
            }
        }
        return true;
    }

    private String extractRequestText(JsonNode request, String field) {
        if (request == null || request.isNull()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (field == null || field.isBlank()) {
            JsonNode messages = request.path("messages");
            if (messages.isArray()) {
                for (JsonNode msg : messages) {
                    if (builder.length() >= 512) break;
                    JsonNode content = msg.path("content");
                    if (content.isTextual()) {
                        builder.append(content.asText()).append("\n");
                    } else if (content.isArray()) {
                        for (JsonNode block : content) {
                            if (builder.length() >= 512) break;
                            if ("text".equals(block.path("type").asText(""))) {
                                builder.append(block.path("text").asText()).append("\n");
                            }
                        }
                    }
                }
            }
            if (builder.length() == 0) {
                appendJsonText(builder, request.path("prompt"));
            }
            if (builder.length() == 0) {
                appendJsonText(builder, request.path("input"));
            }
        } else {
            appendJsonText(builder, request.path(field));
        }
        return builder.toString();
    }

    private void appendJsonText(StringBuilder builder, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            builder.append(node.asText()).append('\n');
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                appendJsonText(builder, child);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                appendJsonText(builder, field.getValue());
            }
        }
    }

    private List<String> extractTools(JsonNode request) {
        List<String> tools = new ArrayList<>();
        appendTools(tools, request != null ? request.path("tools") : null);
        appendTools(tools, request != null ? request.path("functions") : null);
        return tools;
    }

    private void appendTools(List<String> tools, JsonNode node) {
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode item : node) {
            String name = item.path("function").path("name").asText(null);
            if (name == null) {
                name = item.path("name").asText(null);
            }
            if (name != null && !name.isBlank()) {
                tools.add(name);
            }
        }
    }

    private String matchSummary(RoutingConfigRuleCache rule) {
        List<String> parts = new ArrayList<>();
        appendCount(parts, "apiKeyIds", rule.getApiKeyIds());
        if (rule.getDepartments() != null && !rule.getDepartments().isEmpty()) {
            appendCount(parts, "departments", rule.getDepartments());
        } else {
            appendCount(parts, "teamTags", rule.getTeamTags());
        }
        appendCount(parts, "userIds", rule.getUserIds());
        appendCount(parts, "appIds", rule.getAppIds());
        appendCount(parts, "paths", rule.getPaths());
        appendCount(parts, "sourceModels", rule.getSourceModels());
        appendCount(parts, "modelTypes", rule.getModelTypes());
        appendCount(parts, "keywords", rule.getKeywords());
        appendCount(parts, "tools", rule.getTools());
        if (rule.getHeaders() != null && !rule.getHeaders().isEmpty()) {
            parts.add("headers=" + rule.getHeaders().size());
        }
        return parts.isEmpty() ? "all_requests" : String.join(",", parts);
    }

    private void appendCount(List<String> parts, String name, List<?> values) {
        if (values != null && !values.isEmpty()) {
            parts.add(name + "=" + values.size());
        }
    }

    private boolean hasValues(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private boolean regexMatches(String regex, String value) {
        try {
            return Pattern.compile(regex).matcher(value).find();
        } catch (PatternSyntaxException e) {
            log.warn("[RoutingConfig] Invalid regex {}: {}", regex, e.getMessage());
            return false;
        }
    }

    private String findHeader(Map<String, String> headers, String key) {
        if (key == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private String nonBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstNonBlank(List<String> values, String defaultValue) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return defaultValue;
    }

    private String firstHealthy(List<String> values, Map<String, Boolean> modelHealth, String defaultValue) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank() && modelHealthResolver.isHealthy(value, modelHealth)) {
                    return value;
                }
            }
        }
        return firstNonBlank(values, defaultValue);
    }

    private record ClassificationDecision(String targetModel, String result, Double confidence) {
    }
}
