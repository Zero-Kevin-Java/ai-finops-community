package org.afo.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 网关缓存发布器
 * 
 * 当 API Key / 白名单发生变更时发布刷新事件，通知网关清除旧缓存
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class GatewayCachePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String REFRESH_TOPIC = "gateway:cache:refresh";

    /**
     * 发布缓存刷新事件
     * @param cacheType 缓存类型: apikey / whitelist / model-access / routing-config / cache-config
     * @param cacheKey 具体缓存 Key
     */
    public void publishRefresh(String cacheType, String cacheKey) {
        String message = cacheType + ":" + cacheKey;
        stringRedisTemplate.convertAndSend(REFRESH_TOPIC, message);
    }
}
