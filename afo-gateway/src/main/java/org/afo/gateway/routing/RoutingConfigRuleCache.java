package org.afo.gateway.routing;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单条路由规则缓存。
 */
@Data
public class RoutingConfigRuleCache {

    private Long ruleId;
    private String ruleName;
    private Integer priority = Integer.MAX_VALUE;
    private String status = "0";
    private String logic = "ALL";
    private List<String> apiKeyIds = new ArrayList<>();
    private List<String> teamTags = new ArrayList<>();
    private List<String> departments = new ArrayList<>();
    private List<String> userIds = new ArrayList<>();
    private List<String> appIds = new ArrayList<>();
    private List<PathCondition> paths = new ArrayList<>();
    private List<String> sourceModels = new ArrayList<>();
    private List<String> modelTypes = new ArrayList<>();
    private String keywordLogic = "ANY";
    private List<KeywordCondition> keywords = new ArrayList<>();
    private String toolLogic = "ANY";
    private List<String> tools = new ArrayList<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private String actionType = "ORIGINAL_MODEL";
    private String executionMode = "ENFORCE";
    private String targetModel;
    private List<String> modelGroup = new ArrayList<>();
    private String simpleModel;
    private String complexModel;
    private List<String> fallbackModels = new ArrayList<>();
    private Map<String, Boolean> modelHealth = new LinkedHashMap<>();

    public static RuleBuilder ruleBuilder() {
        return new RuleBuilder();
    }

    @Data
    public static class PathCondition {
        private String type = "PREFIX";
        private String value;
    }

    @Data
    public static class KeywordCondition {
        private String field;
        private String value;
        private String matchType = "CONTAINS";
        private boolean caseSensitive = true;
    }

    public static class RuleBuilder {
        private final RoutingConfigRuleCache rule = new RoutingConfigRuleCache();

        public RuleBuilder ruleId(Long ruleId) {
            rule.setRuleId(ruleId);
            return this;
        }

        public RuleBuilder ruleName(String ruleName) {
            rule.setRuleName(ruleName);
            return this;
        }

        public RuleBuilder priority(Integer priority) {
            rule.setPriority(priority);
            return this;
        }

        public RuleBuilder status(String status) {
            rule.setStatus(status);
            return this;
        }

        public RuleBuilder apiKeyIds(String... apiKeyIds) {
            rule.setApiKeyIds(nonNullList(apiKeyIds));
            return this;
        }

        public RuleBuilder logic(String logic) {
            rule.setLogic(logic);
            return this;
        }

        public RuleBuilder teamTags(String... teamTags) {
            rule.setTeamTags(nonNullList(teamTags));
            return this;
        }

        public RuleBuilder departments(String... departments) {
            rule.setDepartments(nonNullList(departments));
            return this;
        }

        public RuleBuilder userIds(String... userIds) {
            rule.setUserIds(nonNullList(userIds));
            return this;
        }

        public RuleBuilder appIds(String... appIds) {
            rule.setAppIds(nonNullList(appIds));
            return this;
        }

        public RuleBuilder path(String value, String type) {
            PathCondition condition = new PathCondition();
            condition.setValue(value);
            condition.setType(type);
            rule.getPaths().add(condition);
            return this;
        }

        public RuleBuilder sourceModels(String... sourceModels) {
            rule.setSourceModels(nonNullList(sourceModels));
            return this;
        }

        public RuleBuilder modelTypes(String... modelTypes) {
            rule.setModelTypes(nonNullList(modelTypes));
            return this;
        }

        public RuleBuilder keyword(String value, String matchType) {
            KeywordCondition condition = new KeywordCondition();
            condition.setField("prompt");
            condition.setValue(value);
            condition.setMatchType(matchType);
            rule.getKeywords().add(condition);
            return this;
        }

        public RuleBuilder keywordLogic(String keywordLogic) {
            rule.setKeywordLogic(keywordLogic);
            return this;
        }

        public RuleBuilder tools(String... tools) {
            rule.setTools(nonNullList(tools));
            return this;
        }

        public RuleBuilder toolLogic(String toolLogic) {
            rule.setToolLogic(toolLogic);
            return this;
        }

        public RuleBuilder headers(Map<String, String> headers) {
            rule.setHeaders(headers != null ? new LinkedHashMap<>(headers) : new LinkedHashMap<>());
            return this;
        }

        public RuleBuilder actionType(String actionType) {
            rule.setActionType(actionType);
            return this;
        }

        public RuleBuilder executionMode(String executionMode) {
            rule.setExecutionMode(executionMode);
            return this;
        }

        public RuleBuilder targetModel(String targetModel) {
            rule.setTargetModel(targetModel);
            return this;
        }

        public RuleBuilder modelGroup(String... modelGroup) {
            rule.setModelGroup(nonNullList(modelGroup));
            return this;
        }

        public RuleBuilder simpleModel(String simpleModel) {
            rule.setSimpleModel(simpleModel);
            return this;
        }

        public RuleBuilder complexModel(String complexModel) {
            rule.setComplexModel(complexModel);
            return this;
        }

        public RuleBuilder fallbackModels(String... fallbackModels) {
            rule.setFallbackModels(nonNullList(fallbackModels));
            return this;
        }

        public RuleBuilder modelHealth(Map<String, Boolean> modelHealth) {
            rule.setModelHealth(modelHealth != null ? new LinkedHashMap<>(modelHealth) : new LinkedHashMap<>());
            return this;
        }

        public RoutingConfigRuleCache build() {
            return rule;
        }

        private List<String> nonNullList(String... values) {
            List<String> result = new ArrayList<>();
            if (values == null) {
                return result;
            }
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    result.add(value);
                }
            }
            return result;
        }
    }
}
