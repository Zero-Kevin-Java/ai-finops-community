package org.afo.gateway.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class GlobalRequestInterceptorTest {

    private final GlobalRequestInterceptor interceptor = new GlobalRequestInterceptor();

    @Test
    void preservesClientRequestId() {
        String requestId = "codex-request-20260701";
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/v1/chat/completions")
                .header("X-Request-Id", requestId)
                .build());
        WebFilterChain chain = downstream -> Mono.empty();

        interceptor.filter(exchange, chain).block();

        assertEquals(requestId, exchange.getAttribute("requestId"));
        assertEquals(requestId, exchange.getResponse().getHeaders().getFirst("X-Request-Id"));
        assertNotNull(exchange.getAttribute("startTime"));
    }

    @Test
    void generatesRequestIdWhenClientHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/v1/chat/completions").build());
        WebFilterChain chain = downstream -> Mono.empty();

        interceptor.filter(exchange, chain).block();

        String requestId = exchange.getAttribute("requestId");
        assertNotNull(requestId);
        assertEquals(requestId, exchange.getResponse().getHeaders().getFirst("X-Request-Id"));
    }
}
