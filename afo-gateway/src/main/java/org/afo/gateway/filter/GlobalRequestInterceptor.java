package org.afo.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * 全局请求拦截器（记录请求元数据、计算延迟）
 * 
 * L0职责：
 * 1. 记录请求开始时间（用于延迟统计）
 * 2. 生成 Request ID（用于链路追踪）
 * 3. 不记录 Prompt/Response 原文（符合L1层安全要求）
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Order(-100)
@Component
public class GlobalRequestInterceptor implements WebFilter {

    private static final int MAX_REQUEST_ID_LENGTH = 64;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant startTime = Instant.now();
        String requestId = resolveRequestId(exchange);
        
        exchange.getResponse().getHeaders().add("X-Request-Id", requestId);
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", startTime);
        
        log.debug("[{}] {} {} started", 
            requestId, 
            exchange.getRequest().getMethod(), 
            exchange.getRequest().getURI());
        
        return chain.filter(exchange)
            .doOnSuccess(aVoid -> logRequestMetrics(exchange))
            .doOnError(throwable -> logRequestError(exchange, throwable));
    }
    
    private void logRequestMetrics(ServerWebExchange exchange) {
        Instant startTime = exchange.getAttribute("startTime");
        String requestId = exchange.getAttribute("requestId");
        
        if (startTime != null) {
            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            log.debug("[{}] completed in {}ms", requestId, latencyMs);
        }
    }
    
    private void logRequestError(ServerWebExchange exchange, Throwable throwable) {
        String requestId = exchange.getAttribute("requestId");
        log.error("[{}] request failed: {}", requestId, throwable.getMessage());
    }
    
    private String resolveRequestId(ServerWebExchange exchange) {
        String clientRequestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (clientRequestId != null) {
            String trimmed = clientRequestId.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_REQUEST_ID_LENGTH) {
                return trimmed;
            }
            log.warn("Ignored invalid X-Request-Id length={}, max={}", trimmed.length(), MAX_REQUEST_ID_LENGTH);
        }
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId != null) {
            String trimmed = traceId.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_REQUEST_ID_LENGTH) {
                return trimmed;
            }
        }
        return generateRequestId();
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

}
