package org.afo.gateway.consumer;

import lombok.extern.slf4j.Slf4j;
import org.afo.common.rabbitmq.message.SimpleTaskRouteChangeEvent;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单任务路由变更消费者（L1 — MQ 事件驱动 Redis 同步）
 *
 * 监听 yy.simple.route 队列，通过 adminWebClient 回源获取全量路由后写回 Redis Hash。
 *
 * @author AI-FinOps Team
 * @since 2026-05-12
 */
@Slf4j
@Component
@RabbitListener(queues = "yy.simple.route")
public class SimpleTaskRouteChangeConsumer {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient adminWebClient;

    @Value("${afo.gateway.admin.internal-token:}")
    private String internalToken;

    public SimpleTaskRouteChangeConsumer(ReactiveRedisTemplate<String, String> redisTemplate,
                                         @Qualifier("adminWebClient") WebClient adminWebClient) {
        this.redisTemplate = redisTemplate;
        this.adminWebClient = adminWebClient;
    }

    @RabbitHandler
    public void handle(SimpleTaskRouteChangeEvent event) {
        String tenantId = event.getTenantId();
        log.debug("[SimpleTaskRouteChangeConsumer] Received change event for tenant={}", tenantId);

        adminWebClient.get()
            .uri("/api/simple-route/tenant/{tenantId}/all", tenantId)
            .header("X-Internal-Token", internalToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnNext(response -> {
                Object data = response.get("data");
                if (!(data instanceof List)) {
                    log.warn("[SimpleTaskRouteChangeConsumer] Unexpected response for tenant={}", tenantId);
                    return;
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routes = (List<Map<String, Object>>) data;
                String key = "gateway:simple-route:" + tenantId;
                redisTemplate.delete(key).subscribe();

                Map<String, String> map = new HashMap<>();
                for (Map<String, Object> r : routes) {
                    if (r.get("targetModel") == null) continue;
                    if (r.get("originalModel") != null) {
                        map.put((String) r.get("originalModel"), (String) r.get("targetModel"));
                    }
                }
                if (!map.isEmpty()) {
                    redisTemplate.opsForHash().putAll(key, map).subscribe();
                }
                log.debug("[SimpleTaskRouteChangeConsumer] Synced {} routes for tenant={} to Redis", map.size(), tenantId);
            })
            .onErrorResume(e -> {
                log.error("[SimpleTaskRouteChangeConsumer] Failed to sync routes for tenant={}: {}", tenantId, e.getMessage());
                return reactor.core.publisher.Mono.empty();
            })
            .subscribe();
    }
}
