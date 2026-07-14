package org.afo.strategy.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes strategy cache refresh messages consumed by the gateway.
 */
@Component
@RequiredArgsConstructor
public class GatewayStrategyCachePublisher {

    private static final String REFRESH_TOPIC = "gateway:cache:refresh";
    private static final String MODEL_ACCESS = "model-access";
    private static final String ROUTING_CONFIG = "routing-config";

    private final StringRedisTemplate stringRedisTemplate;
    private final StrategyTenantResolver tenantResolver;

    public void publishModelAccess(String tenantId) {
        stringRedisTemplate.convertAndSend(REFRESH_TOPIC, MODEL_ACCESS + ":" + tenantResolver.resolve(tenantId));
    }

    public void publishRoutingConfig(String tenantId) {
        stringRedisTemplate.convertAndSend(REFRESH_TOPIC, ROUTING_CONFIG + ":" + tenantResolver.resolve(tenantId));
    }
}
