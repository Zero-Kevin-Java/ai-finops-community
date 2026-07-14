package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.config.GatewayCacheConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis + 控制面路由配置客户端。
 */
@Slf4j
@Component
public class DefaultRoutingConfigClient implements RoutingConfigClient {

    static final String REDIS_KEY_PREFIX = "gateway:routing-config:";

    @Value("${afo.gateway.admin.base-url:http://127.0.0.1:8080}")
    private String adminBaseUrl;

    private final StringRedisTemplate blockingRedis;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayCacheConfig cacheConfig;
    private final Map<String, RoutingConfigCache> localCache = new ConcurrentHashMap<>();

    @Value("${afo.gateway.admin.internal-token:}")
    private String adminInternalToken;

    public DefaultRoutingConfigClient(StringRedisTemplate blockingRedis,
                                      ObjectMapper objectMapper,
                                      GatewayCacheConfig cacheConfig) {
        this.blockingRedis = blockingRedis;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.cacheConfig = cacheConfig;
    }

    @Override
    public RoutingConfigCache loadActiveConfig(String tenantId) {
        String resolvedTenantId = resolveTenantId(tenantId);
        RoutingConfigCache cached = localCache.get(resolvedTenantId);
        if (cached != null) {
            return cached;
        }
        RoutingConfigCache loaded = loadFromRedis(resolvedTenantId);
        if (loaded != null && loaded.isPresent()) {
            localCache.put(resolvedTenantId, loaded);
        }
        return loaded != null ? loaded : RoutingConfigCache.empty();
    }

    public void refresh(String tenantId) {
        if ("*".equals(tenantId)) {
            localCache.clear();
            return;
        }
        localCache.remove(resolveTenantId(tenantId));
    }

    private RoutingConfigCache loadFromRedis(String tenantId) {
        String redisKey = REDIS_KEY_PREFIX + tenantId;
        try {
            String json = blockingRedis.opsForValue().get(redisKey);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, RoutingConfigCache.class);
            }
        } catch (Exception e) {
            log.warn("[RoutingConfig] Failed to load Redis cache for tenant={}: {}", tenantId, e.getMessage());
        }
        return loadFromAdmin(tenantId);
    }

    private RoutingConfigCache loadFromAdmin(String tenantId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (adminInternalToken != null && !adminInternalToken.isBlank()) {
                headers.set("X-Internal-Token", adminInternalToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                adminBaseUrl + "/api/gateway/routing-config/{tenantId}/active",
                HttpMethod.GET,
                entity,
                JsonNode.class,
                tenantId);
            JsonNode body = response.getBody();
            RoutingConfigCache cache = convertAdminResponse(tenantId, body);
            if (cache.isPresent()) {
                cacheToRedis(tenantId, cache);
            }
            return cache;
        } catch (Exception e) {
            log.warn("[RoutingConfig] Admin fallback failed for tenant={}: {}", tenantId, e.getMessage());
            return RoutingConfigCache.empty();
        }
    }

    private RoutingConfigCache convertAdminResponse(String tenantId, JsonNode response) {
        if (response == null || response.path("code").asInt() != 200 || response.path("data").isNull()) {
            return RoutingConfigCache.empty();
        }
        RoutingConfigCache cache = new RoutingConfigCache();
        cache.setTenantId(tenantId);
        cache.setPresent(true);
        JsonNode data = response.path("data");
        if (data.isArray()) {
            List<RoutingConfigRuleCache> rules = new ArrayList<>();
            for (JsonNode item : data) {
                rules.add(toRuleCache(item));
            }
            cache.setRules(rules);
        } else {
            cache = objectMapper.convertValue(data, RoutingConfigCache.class);
            cache.setPresent(true);
            if (cache.getTenantId() == null || cache.getTenantId().isBlank()) {
                cache.setTenantId(tenantId);
            }
        }
        return cache;
    }

    private RoutingConfigRuleCache toRuleCache(JsonNode item) {
        RoutingConfigRuleCache rule = new RoutingConfigRuleCache();
        rule.setRuleId(item.path("ruleId").isMissingNode() || item.path("ruleId").isNull() ? null : item.path("ruleId").asLong());
        rule.setRuleName(item.path("ruleName").asText(null));
        rule.setPriority(item.path("priority").isInt() ? item.path("priority").asInt() : Integer.MAX_VALUE);
        rule.setStatus(item.path("status").asText("0"));
        rule.setActionType(item.path("actionType").asText("ORIGINAL_MODEL"));
        rule.setExecutionMode(item.path("executionMode").asText("ENFORCE"));

        JsonNode matchConfig = parseObject(item.path("matchConfig").asText("{}"));
        rule.setLogic(matchConfig.path("logic").asText(matchConfig.path("matchLogic").asText("ALL")));
        rule.setApiKeyIds(readStringList(matchConfig.path("apiKeyIds")));
        rule.setTeamTags(readStringList(matchConfig.path("teamTags")));
        rule.setDepartments(readStringList(matchConfig.path("departments")));
        rule.setUserIds(readStringList(matchConfig.path("userIds")));
        rule.setAppIds(readStringList(matchConfig.path("appIds")));
        rule.setSourceModels(readStringList(matchConfig.path("sourceModels")));
        rule.setModelTypes(readStringList(matchConfig.path("modelTypes")));
        rule.setKeywordLogic(matchConfig.path("keywordLogic").asText("ANY"));
        rule.setTools(readStringList(matchConfig.path("tools")));
        rule.setToolLogic(matchConfig.path("toolLogic").asText("ANY"));
        rule.setPaths(readPaths(matchConfig.path("paths")));
        rule.setKeywords(readKeywords(matchConfig.path("keywords")));
        rule.setHeaders(readHeaders(matchConfig.path("headers")));

        JsonNode actionConfig = parseObject(item.path("actionConfig").asText("{}"));
        rule.setTargetModel(actionConfig.path("targetModel").asText(null));
        rule.setModelGroup(readStringList(actionConfig.path("models")));
        if (rule.getModelGroup().isEmpty()) {
            String modelGroup = actionConfig.path("modelGroup").asText(null);
            if (modelGroup != null && modelGroup.contains(",")) {
                rule.setModelGroup(splitCsv(modelGroup));
            }
        }
        rule.setSimpleModel(firstText(actionConfig, "simpleTaskTargetModel", "simpleModel"));
        rule.setComplexModel(actionConfig.path("complexModel").asText(null));

        JsonNode fallbackConfig = parseObject(item.path("fallbackConfig").asText("{}"));
        List<String> fallbackModels = readStringList(fallbackConfig.path("fallbackModels"));
        String fallbackModel = fallbackConfig.path("fallbackModel").asText(null);
        if (fallbackModels.isEmpty() && fallbackModel != null && !fallbackModel.isBlank()) {
            fallbackModels = List.of(fallbackModel);
        }
        rule.setFallbackModels(fallbackModels);
        return rule;
    }

    private JsonNode parseObject(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private List<RoutingConfigRuleCache.PathCondition> readPaths(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<RoutingConfigRuleCache.PathCondition> paths = new ArrayList<>();
        for (JsonNode item : node) {
            RoutingConfigRuleCache.PathCondition path = new RoutingConfigRuleCache.PathCondition();
            path.setType(item.path("type").asText("PREFIX"));
            path.setValue(item.path("value").asText(null));
            paths.add(path);
        }
        return paths;
    }

    private List<RoutingConfigRuleCache.KeywordCondition> readKeywords(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<RoutingConfigRuleCache.KeywordCondition> keywords = new ArrayList<>();
        for (JsonNode item : node) {
            RoutingConfigRuleCache.KeywordCondition keyword = new RoutingConfigRuleCache.KeywordCondition();
            keyword.setField(item.path("field").asText(null));
            keyword.setValue(item.path("value").asText(null));
            keyword.setMatchType(item.path("type").asText(item.path("matchType").asText("CONTAINS")));
            keyword.setCaseSensitive(item.path("caseSensitive").asBoolean(true));
            keywords.add(keyword);
        }
        return keywords;
    }

    private Map<String, String> readHeaders(JsonNode node) {
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        if (node == null || !node.isArray()) {
            return headers;
        }
        for (JsonNode item : node) {
            String name = item.path("name").asText(null);
            String value = item.path("value").asText(null);
            if (name != null && !name.isBlank() && value != null) {
                headers.put(name, value);
            }
        }
        return headers;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void cacheToRedis(String tenantId, RoutingConfigCache cache) {
        try {
            int ttl = cacheConfig.getCacheConfigTtl() > 0 ? cacheConfig.getCacheConfigTtl() : 600;
            blockingRedis.opsForValue().set(
                REDIS_KEY_PREFIX + tenantId,
                objectMapper.writeValueAsString(cache),
                Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("[RoutingConfig] Failed to write Redis cache for tenant={}: {}", tenantId, e.getMessage());
        }
    }

    private String resolveTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "000000" : tenantId;
    }
}
