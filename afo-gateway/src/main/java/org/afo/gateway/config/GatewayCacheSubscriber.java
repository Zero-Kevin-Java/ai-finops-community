package org.afo.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.routing.DefaultRoutingConfigClient;
import org.afo.gateway.routing.ModelAccessMatcher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 网关缓存订阅器
 * 
 * 订阅 Redis Pub/Sub 频道，接收控制面缓存变更通知，
 * 主动清除本地/Redis 旧缓存并重新加载。
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayCacheSubscriber {

    private static final String REFRESH_TOPIC = "gateway:cache:refresh";
    private static final String APIKEY_PREFIX = "gateway:apikey:";
    private static final String MODEL_ACCESS_PREFIX = "gateway:model-access:";
    private static final String ROUTING_CONFIG_PREFIX = "gateway:routing-config:";
    private static final String CACHE_CONFIG_PREFIX = "gateway:cache-config:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ModelAccessMatcher modelAccessMatcher;
    private final DefaultRoutingConfigClient routingConfigClient;

    @PostConstruct
    public void subscribe() {
        log.info("[GatewayCacheSubscriber] Subscribing to topic: {}", REFRESH_TOPIC);
        redisTemplate.listenToChannel(REFRESH_TOPIC)
            .doOnNext(message -> {
                String payload = message.getMessage();
                log.debug("[GatewayCacheSubscriber] Received: {}", payload);
                handleRefresh(payload);
            })
            .doOnError(e -> log.error("[GatewayCacheSubscriber] Subscription error", e))
            .subscribe();
    }

    void handleRefresh(String payload) {
        int colonIdx = payload.indexOf(':');
        if (colonIdx <= 0 || colonIdx >= payload.length() - 1) {
            log.warn("[GatewayCacheSubscriber] Invalid format: {}", payload);
            return;
        }
        String cacheType = payload.substring(0, colonIdx);
        String cacheKey = payload.substring(colonIdx + 1);

        switch (cacheType) {
            case "apikey" -> {
                if ("*".equals(cacheKey)) {
                    redisTemplate.keys(APIKEY_PREFIX + "*")
                        .flatMap(redisTemplate::delete)
                        .subscribe();
                    log.info("[GatewayCacheSubscriber] Deleted all API Key caches");
                    return;
                }
                String redisKey = APIKEY_PREFIX + cacheKey;
                redisTemplate.delete(redisKey).subscribe();
                log.info("[GatewayCacheSubscriber] Deleted API Key cache: {}", redisKey);
            }
            case "model-access", "whitelist" -> {
                if ("*".equals(cacheKey)) {
                    redisTemplate.keys(MODEL_ACCESS_PREFIX + "*")
                        .flatMap(redisTemplate::delete)
                        .subscribe();
                    modelAccessMatcher.refreshAllPolicies();
                    log.info("[GatewayCacheSubscriber] Refreshed all model access policies");
                    return;
                }
                String redisKey = MODEL_ACCESS_PREFIX + cacheKey;
                redisTemplate.delete(redisKey).subscribe();
                modelAccessMatcher.refreshTenant(cacheKey);
                log.info("[GatewayCacheSubscriber] Refreshed model access policy for tenant: {}", cacheKey);
            }
            case "routing-config" -> {
                if ("*".equals(cacheKey)) {
                    redisTemplate.keys(ROUTING_CONFIG_PREFIX + "*")
                        .flatMap(redisTemplate::delete)
                        .doOnComplete(() -> routingConfigClient.refresh("*"))
                        .subscribe();
                    log.info("[GatewayCacheSubscriber] Refreshed all routing configs");
                    return;
                }
                String redisKey = ROUTING_CONFIG_PREFIX + cacheKey;
                redisTemplate.delete(redisKey)
                    .doOnSuccess(ignored -> routingConfigClient.refresh(cacheKey))
                    .subscribe();
                log.info("[GatewayCacheSubscriber] Refreshed routing config for tenant: {}", cacheKey);
            }
            case "cache-config" -> {
                if ("*".equals(cacheKey)) {
                    redisTemplate.keys(CACHE_CONFIG_PREFIX + "*")
                        .flatMap(redisTemplate::delete)
                        .subscribe();
                    log.info("[GatewayCacheSubscriber] Deleted all cache config caches");
                    return;
                }
                String redisKey = CACHE_CONFIG_PREFIX + cacheKey;
                redisTemplate.delete(redisKey).subscribe();
                log.info("[GatewayCacheSubscriber] Deleted cache config: {}", redisKey);
            }
            default -> log.warn("[GatewayCacheSubscriber] Unknown cache type: {}", cacheType);
        }
    }
}
