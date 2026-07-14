package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.routing.RouteDecisionEngine;
import org.afo.gateway.util.ProxyPathUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 模型准入前置过滤器。
 *
 * <p>只负责缓存请求体和提取原始请求模型；API Key scope、模型策略和 routing-config
 * 在模型策略过滤器之后统一执行，避免多处重复决策。</p>
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Slf4j
@Order(-72)
@Component
public class ModelAccessFilter implements WebFilter {

    public static final String ATTR_ROUTE_RESULT = "routeResult";
    public static final String ATTR_REQUEST_MODEL = "requestModel";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelAccessFilter() {
    }

    /**
     * Compatibility constructor for older tests/configuration. Routing decisions are no longer made here.
     */
    public ModelAccessFilter(RouteDecisionEngine ignoredRouteDecisionEngine) {
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!ProxyPathUtils.isProxyPath(path)) {
            return chain.filter(exchange);
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bodyBytes);
                DataBufferUtils.release(dataBuffer);
                return bodyBytes;
            })
            .defaultIfEmpty(new byte[0])
            .flatMap(bodyBytes -> evaluateAndContinue(exchange, chain, bodyBytes));
    }

    private Mono<Void> evaluateAndContinue(ServerWebExchange exchange, WebFilterChain chain, byte[] bodyBytes) {
        String requestId = exchange.getAttribute("requestId");
        String model = extractModel(bodyBytes, requestId);

        if (model != null) {
            exchange.getAttributes().put(ATTR_REQUEST_MODEL, model);
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(new CachedBodyRequestDecorator(exchange, bodyBytes))
            .build();
        return chain.filter(mutatedExchange);
    }

    private String extractModel(byte[] bodyBytes, String requestId) {
        if (bodyBytes.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(bodyBytes);
            return node.has("model") ? node.get("model").asText(null) : null;
        } catch (Exception e) {
            log.warn("[ModelAccessFilter][{}] Failed to parse request body: {}", requestId, e.getMessage());
            return null;
        }
    }


    private static class CachedBodyRequestDecorator extends ServerHttpRequestDecorator {

        private final ServerWebExchange exchange;
        private final byte[] bodyBytes;

        private CachedBodyRequestDecorator(ServerWebExchange exchange, byte[] bodyBytes) {
            super(exchange.getRequest());
            this.exchange = exchange;
            this.bodyBytes = bodyBytes;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes)));
        }
    }
}
