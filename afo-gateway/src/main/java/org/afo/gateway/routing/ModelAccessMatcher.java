package org.afo.gateway.routing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.config.GatewayCacheConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业模型准入匹配器。
 *
 * <p>控制面不可用或没有有效策略时按兼容策略放行，避免上线后误拒绝历史请求。</p>
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Slf4j
@Component
public class ModelAccessMatcher {

    private static final String REDIS_KEY_PREFIX = "gateway:model-access:";
    private static final String ENTERPRISE_MODEL_DENIED = "ENTERPRISE_MODEL_DENIED";
    private static final String ENTERPRISE_MODEL_NOT_ALLOWED = "ENTERPRISE_MODEL_NOT_ALLOWED";
    private static final String ENTERPRISE_MODEL_UNLISTED_DENIED = "ENTERPRISE_MODEL_UNLISTED_DENIED";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient adminWebClient;
    private final ObjectMapper objectMapper;
    private final GatewayCacheConfig cacheConfig;

    @Value("${afo.gateway.admin.internal-token:}")
    private String adminInternalToken;

    /** 本地内存缓存：tenantId -> 当前有效策略。 */
    private final Map<String, ModelAccessPolicyCache> localCache = new ConcurrentHashMap<>();

    public ModelAccessMatcher(ReactiveRedisTemplate<String, String> redisTemplate,
                              WebClient adminWebClient,
                              ObjectMapper objectMapper,
                              GatewayCacheConfig cacheConfig) {
        this.redisTemplate = redisTemplate;
        this.adminWebClient = adminWebClient;
        this.objectMapper = objectMapper;
        this.cacheConfig = cacheConfig;
    }

    @PostConstruct
    public void init() {
        loadAllPolicies();
    }

    /**
     * 校验模型是否通过企业模型准入。
     */
    public ModelAccessDecision evaluate(String tenantId, String model) {
        if (model == null || model.isBlank()) {
            return ModelAccessDecision.allowed();
        }
        ModelAccessPolicyCache policy = getPolicyForTenant(resolveTenantId(tenantId));
        if (policy == null || !policy.isPresent() || !"0".equals(policy.getStatus())) {
            return ModelAccessDecision.allowed();
        }

        if (policy.getDeniedModels() != null && policy.getDeniedModels().contains(model)) {
            return ModelAccessDecision.denied(ENTERPRISE_MODEL_DENIED, policy.getPolicyId());
        }

        List<String> allowedModels = policy.getAllowedModels();
        if (allowedModels != null && !allowedModels.isEmpty()) {
            return allowedModels.contains(model)
                ? ModelAccessDecision.allowed()
                : ModelAccessDecision.denied(ENTERPRISE_MODEL_NOT_ALLOWED, policy.getPolicyId());
        }

        if ("DENY_UNLISTED".equals(policy.getDefaultMode())) {
            return ModelAccessDecision.denied(ENTERPRISE_MODEL_UNLISTED_DENIED, policy.getPolicyId());
        }
        return ModelAccessDecision.allowed();
    }

    private ModelAccessPolicyCache getPolicyForTenant(String tenantId) {
        return localCache.computeIfAbsent(tenantId, this::loadPolicyFromRedis);
    }

    private ModelAccessPolicyCache loadPolicyFromRedis(String tenantId) {
        String redisKey = REDIS_KEY_PREFIX + tenantId;
        try {
            String json = redisTemplate.opsForValue().get(redisKey)
                .timeout(Duration.ofSeconds(2))
                .block();
            if (json != null && !json.isBlank()) {
                ModelAccessPolicyCache policy = objectMapper.readValue(json, ModelAccessPolicyCache.class);
                log.debug("[ModelAccess] Loaded policy from Redis for tenant {}", tenantId);
                return policy;
            }
        } catch (Exception e) {
            log.warn("[ModelAccess] Failed to load policy from Redis for tenant {}: {}", tenantId, e.getMessage());
        }
        return loadPolicyFromAdmin(tenantId);
    }

    private ModelAccessPolicyCache loadPolicyFromAdmin(String tenantId) {
        try {
            JsonNode response = adminWebClient.get()
                .uri("/api/gateway/model-access/" + tenantId + "/active")
                .headers(headers -> {
                    if (adminInternalToken != null && !adminInternalToken.isBlank()) {
                        headers.set("X-Internal-Token", adminInternalToken);
                    }
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .block();

            ModelAccessPolicyCache policy = convertAdminResponse(response);
            cachePolicy(tenantId, policy);
            log.info("[ModelAccess] Loaded policy from admin for tenant {}, present={}", tenantId, policy.isPresent());
            return policy;
        } catch (Exception e) {
            log.warn("[ModelAccess] Failed to load policy from admin for tenant {}: {}", tenantId, e.getMessage());
            return ModelAccessPolicyCache.empty();
        }
    }

    private ModelAccessPolicyCache convertAdminResponse(JsonNode response) {
        if (response == null || !response.has("code") || response.get("code").asInt() != 200
            || !response.has("data") || response.get("data").isNull()) {
            return ModelAccessPolicyCache.empty();
        }
        JsonNode data = response.get("data");
        ModelAccessPolicyCache policy = new ModelAccessPolicyCache();
        policy.setPresent(true);
        policy.setPolicyId(data.path("policyId").isMissingNode() ? null : data.path("policyId").asLong());
        policy.setTenantId(data.path("tenantId").asText(null));
        policy.setDefaultMode(data.path("defaultMode").asText("ALLOW_UNLISTED"));
        policy.setStatus(data.path("status").asText("0"));
        policy.setAllowedModels(readJsonArrayText(data.path("allowedModels").asText("[]")));
        policy.setDeniedModels(readJsonArrayText(data.path("deniedModels").asText("[]")));
        return policy;
    }

    private List<String> readJsonArrayText(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[ModelAccess] Invalid model list JSON from admin: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void cachePolicy(String tenantId, ModelAccessPolicyCache policy) {
        try {
            String redisKey = REDIS_KEY_PREFIX + tenantId;
            String json = objectMapper.writeValueAsString(policy);
            redisTemplate.opsForValue()
                .set(redisKey, json, Duration.ofSeconds(cacheConfig.getModelAccessTtl()))
                .block();
        } catch (Exception e) {
            log.warn("[ModelAccess] Failed to cache policy to Redis: {}", e.getMessage());
        }
    }

    private void loadAllPolicies() {
        log.info("[ModelAccess] Starting model access cache preheat...");
        try {
            redisTemplate.keys(REDIS_KEY_PREFIX + "*")
                .timeout(Duration.ofSeconds(5))
                .collectList()
                .blockOptional(Duration.ofSeconds(10))
                .ifPresent(keys -> {
                    for (String key : keys) {
                        String tenantId = key.substring(REDIS_KEY_PREFIX.length());
                        localCache.put(tenantId, loadPolicyFromRedis(tenantId));
                    }
                    if (!keys.isEmpty()) {
                        log.info("[ModelAccess] Preheated policies for {} tenants from Redis", keys.size());
                    }
                });
        } catch (Exception e) {
            log.warn("[ModelAccess] Redis preheat failed: {}", e.getMessage());
        }
        log.info("[ModelAccess] Model access cache preheat completed");
    }

    /**
     * 定时刷新当前已知租户的模型准入策略。
     */
    @Scheduled(fixedRate = 300000)
    public void refreshAllPolicies() {
        List<String> tenantIds = new ArrayList<>(localCache.keySet());
        for (String tenantId : tenantIds) {
            localCache.put(tenantId, loadPolicyFromRedis(tenantId));
        }
        if (!tenantIds.isEmpty()) {
            log.info("[ModelAccess] Refreshed policies for {} tenants", tenantIds.size());
        }
    }

    /**
     * 刷新指定租户策略。
     */
    public void refreshTenant(String tenantId) {
        if ("*".equals(tenantId)) {
            localCache.clear();
            return;
        }
        tenantId = resolveTenantId(tenantId);
        log.info("[ModelAccess] Refreshing model access policy for tenant {}", tenantId);
        localCache.remove(tenantId);
        localCache.put(tenantId, loadPolicyFromRedis(tenantId));
    }

    private String resolveTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "000000" : tenantId;
    }
}
