package org.afo.gateway.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.config.GatewayCacheConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayCacheService {

    private static final String REDIS_CONFIG_PREFIX = "gateway:cache-config:";
    private static final String REDIS_STATS_PREFIX = "gateway:cache-stats:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    @Qualifier("adminWebClient")
    private final WebClient adminWebClient;
    private final ObjectMapper objectMapper;
    private final GatewayCacheConfig gatewayCacheConfig;

    @Value("${afo.gateway.admin.internal-token:}")
    private String adminInternalToken;

    private final Map<String, CacheConfig> localConfigCache = new ConcurrentHashMap<>();

    public Mono<CacheConfig> loadConfig(String tenantId, Long projectId, Long clientId) {
        String cacheKey = buildConfigKey(tenantId, projectId, clientId);
        CacheConfig cached = localConfigCache.get(cacheKey);
        if (cached != null) {
            return Mono.just(cached);
        }
        return loadFromRedis(tenantId, projectId, clientId)
            .doOnNext(config -> localConfigCache.put(cacheKey, config));
    }

    public Mono<CacheEntryPayload> findByHash(String tenantId, Long projectId, Long clientId,
                                               String modelCode, String promptHash) {
        return adminWebClient.get()
            .uri("/gateway/cache-lookup/{tenantId}?projectId={projectId}&clientId={clientId}&modelCode={modelCode}&hash={hash}",
                tenantId, projectId, clientId, modelCode, promptHash)
            .header("X-Internal-Token", adminInternalToken != null ? adminInternalToken : "")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofSeconds(2))
            .flatMap(response -> {
                if (response != null && response.path("code").asInt() == 200 && !response.path("data").isNull()) {
                    JsonNode data = response.path("data");
                    CacheEntryPayload entry = new CacheEntryPayload();
                    entry.setEntryId(data.path("entryId").asLong());
                    entry.setResponseText(data.path("responseText").asText());
                    entry.setHitCount(data.path("hitCount").asLong());
                    entry.setTokenCount(data.path("tokenCount").asInt(0));
                    return Mono.just(entry);
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                log.warn("[GatewayCacheService] Cache lookup failed: tenant={}, project={}, model={}, error={}",
                    tenantId, projectId, modelCode, e.getMessage());
                return Mono.empty();
            });
    }

    public void saveEntryAsync(String tenantId, Long projectId, Long clientId,
                                String modelCode, String promptHash,
                                String promptText, String responseText,
                                CacheConfig config) {
        if (responseText == null || responseText.isEmpty()) return;

        Mono.fromRunnable(() -> {
            try {
                ObjectNode payloadNode = objectMapper.createObjectNode()
                    .put("tenantId", tenantId)
                    .put("projectId", projectId);
                if (clientId != null) {
                    payloadNode.put("clientId", clientId);
                } else {
                    payloadNode.putNull("clientId");
                }
                payloadNode
                    .put("modelCode", modelCode)
                    .put("promptHash", promptHash)
                    .put("promptText", promptText != null ? promptText : "")
                    .put("responseText", responseText)
                    .put("ttlSeconds", config != null && config.getTtlSeconds() != null
                        ? config.getTtlSeconds() : 3600)
                    .put("maxEntries", config != null && config.getMaxEntries() != null
                        ? config.getMaxEntries() : 10000);

                adminWebClient.post()
                    .uri("/gateway/cache-entry")
                    .header("X-Internal-Token", adminInternalToken != null ? adminInternalToken : "")
                    .bodyValue(payloadNode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .subscribe(
                        result -> log.info("[GatewayCacheService] Cache entry saved: hash={}, tenant={}, project={}",
                            promptHash, tenantId, projectId),
                        error -> log.warn("[GatewayCacheService] Cache entry save failed: hash={}, tenant={}, project={}, error={}",
                            promptHash, tenantId, projectId, error.getMessage())
                    );
            } catch (Exception e) {
                log.warn("[GatewayCacheService] Cache entry save error: hash={}, tenant={}, error={}",
                    promptHash, tenantId, e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    public void incrementStats(String tenantId, Long projectId, Long clientId, boolean hit) {
        String key = REDIS_STATS_PREFIX + tenantId + ":" + projectId + ":" + clientId;
        String field = hit ? "hits" : "misses";
        redisTemplate.opsForHash().increment(key, field, 1).subscribe();
    }

    public void writeHitLogAsync(String tenantId, Long projectId, Long clientId,
                                  Long entryId, String modelCode, String promptHash,
                                  Object apiKeyId, Object requestId, int tokenCount) {
        Mono.fromRunnable(() -> {
            try {
                JsonNode payload = objectMapper.createObjectNode()
                    .put("tenantId", tenantId)
                    .put("projectId", projectId)
                    .put("clientId", clientId)
                    .put("entryId", entryId != null ? entryId : 0)
                    .put("modelCode", modelCode != null ? modelCode : "")
                    .put("promptHash", promptHash != null ? promptHash : "")
                    .put("tokenSaved", tokenCount)
                    .put("apiKeyId", apiKeyId instanceof Long l ? l : null)
                    .put("requestId", requestId instanceof String s ? s : null);

                adminWebClient.post()
                    .uri("/gateway/cache-hit-log")
                    .header("X-Internal-Token", adminInternalToken != null ? adminInternalToken : "")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .subscribe(
                        result -> {},
                        error -> log.warn("[GatewayCacheService] Hit log write failed: entry={}, error={}",
                            entryId, error.getMessage())
                    );
            } catch (Exception e) {
                log.warn("[GatewayCacheService] Hit log write error: entry={}, error={}", entryId, e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private String buildConfigKey(String tenantId, Long projectId, Long clientId) {
        return tenantId + ":" + projectId + ":" + (clientId != null ? clientId : "");
    }

    private Mono<CacheConfig> loadFromRedis(String tenantId, Long projectId, Long clientId) {
        String redisKey = REDIS_CONFIG_PREFIX + buildConfigKey(tenantId, projectId, clientId);
        return redisTemplate.opsForValue().get(redisKey)
            .flatMap(json -> {
                if (json == null || json.isBlank()) {
                    return loadFromAdmin(tenantId, projectId, clientId);
                }
                try {
                    return Mono.just(objectMapper.readValue(json, CacheConfig.class));
                } catch (JsonProcessingException e) {
                    log.warn("[GatewayCacheService] Failed to parse Redis config", e);
                    return loadFromAdmin(tenantId, projectId, clientId);
                }
            })
            .switchIfEmpty(loadFromAdmin(tenantId, projectId, clientId));
    }

    private Mono<CacheConfig> loadFromAdmin(String tenantId, Long projectId, Long clientId) {
        String uri = "/gateway/cache-config/" + tenantId + "/" + projectId;
        if (clientId != null) {
            uri += "/" + clientId;
        }
        return adminWebClient.get()
            .uri(uri)
            .header("X-Internal-Token", adminInternalToken != null ? adminInternalToken : "")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofSeconds(3))
            .flatMap(response -> {
                if (response != null && response.path("code").asInt() == 200 && !response.path("data").isNull()) {
                    JsonNode data = response.path("data");
                    CacheConfig config = new CacheConfig();
                    config.setConfigId(data.path("configId").asLong());
                    config.setEnabled(data.path("enabled").asText("0"));
                    config.setMatchMode(data.path("matchMode").asText("EXACT"));
                    config.setTtlSeconds(data.path("ttlSeconds").asInt(3600));
                    config.setMaxEntries(data.path("maxEntries").asInt(10000));
                    if (data.has("similarityThreshold")) {
                        config.setSimilarityThreshold(
                            new BigDecimal(data.path("similarityThreshold").asText("0.95")));
                    }
                    try {
                        String redisKey = REDIS_CONFIG_PREFIX + buildConfigKey(tenantId, projectId, clientId);
                        String json = objectMapper.writeValueAsString(config);
                        int ttl = gatewayCacheConfig.getCacheConfigTtl() > 0
                            ? gatewayCacheConfig.getCacheConfigTtl() : 600;
                        redisTemplate.opsForValue().set(redisKey, json, Duration.ofSeconds(ttl)).subscribe();
                    } catch (Exception e) {
                        log.debug("[GatewayCacheService] Failed to cache config in Redis: {}", e.getMessage());
                    }
                    return Mono.just(config);
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                log.debug("[GatewayCacheService] Admin config lookup failed: {}", e.getMessage());
                return Mono.empty();
            });
    }
}
