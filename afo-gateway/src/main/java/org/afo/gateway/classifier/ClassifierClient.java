package org.afo.gateway.classifier;

import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.config.ClassifierProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import reactor.core.publisher.Mono;

/**
 * 分类器客户端
 *
 * L0职责：仅心跳检查，不参与路由决策
 * D4职责：新增 /classify 端点调用，SMART_AUTO/SMART_CUSTOM 模式调用分类器
 *
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Component
public class ClassifierClient {

    private final WebClient classifierWebClient;
    private final ClassifierProperties properties;

    public ClassifierClient(ClassifierProperties properties) {
        this.properties = properties;
        this.classifierWebClient = WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }

    /**
     * 调用分类器 /health 端点
     * 同步阻塞（@Scheduled 线程池中执行）
     *
     * @return true=健康
     */
    public boolean checkHealth() {
        try {
            String response = classifierWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .block();

            log.debug("[ClassifierClient] Health check response: {}", response);
            return response != null &&
                   (response.contains("\"status\":\"ok\"") ||
                    response.contains("\"healthy\":true"));
        } catch (Exception e) {
            log.warn("[ClassifierClient] Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 调用分类器 /classify 端点（响应式）
     *
     * @param prompt   请求体中的 prompt 文本（截断 512 字符）
     * @param tenantId 租户 ID
     * @return 分类结果（is_simple + confidence）
     */
    public Mono<ClassifyResponse> classify(String prompt, String tenantId) {
        if (prompt == null || prompt.isEmpty()) {
            return Mono.just(new ClassifyResponse());
        }

        String truncated = prompt.length() > 512 ? prompt.substring(0, 512) : prompt;

        ClassifyRequest request = new ClassifyRequest(truncated, tenantId != null ? tenantId : "0");

        return classifierWebClient.post()
            .uri("/classify")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ClassifyResponse.class)
            .timeout(Duration.ofMillis(properties.getTimeout()))
            .onErrorResume(e -> {
                log.warn("[ClassifierClient] Classify failed for tenant={}: {}, falling back to is_simple=false",
                    tenantId, e.getMessage());
                ClassifyResponse fallback = new ClassifyResponse();
                fallback.setSimple(false);
                fallback.setConfidence(0.0);
                fallback.setModelUsed("none");
                return Mono.just(fallback);
            });
    }
}
