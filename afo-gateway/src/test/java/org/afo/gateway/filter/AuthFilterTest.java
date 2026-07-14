package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class AuthFilterTest {

    private final ReactiveRedisTemplate<String, String> redisTemplate = mock(ReactiveRedisTemplate.class);
    private final ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingApiKeyReturnsStableDeniedResponse() {
        AuthFilter filter = new AuthFilter(unusedWebClient(), redisTemplate, objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v1/models").build());
        exchange.getAttributes().put("requestId", "req-missing");

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertDeniedResponse(exchange, "API_KEY_MISSING", "req-missing");
    }

    @Test
    void invalidApiKeyReturnsStableDeniedResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        AuthFilter filter = new AuthFilter(jsonWebClient("{\"code\":500,\"msg\":\"API Key 不存在或已失效\"}"), redisTemplate, objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v1/models")
            .header("X-API-Key", "afo_sk_invalid")
            .build());
        exchange.getAttributes().put("requestId", "req-invalid");

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertDeniedResponse(exchange, "API_KEY_INVALID", "req-invalid");
    }

    @Test
    void disabledApiKeyReturnsDisabledReason() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        AuthFilter filter = new AuthFilter(jsonWebClient("{\"code\":500,\"msg\":\"API Key 已停用\"}"), redisTemplate, objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v1/models")
            .header("X-API-Key", "afo_sk_disabled")
            .build());
        exchange.getAttributes().put("requestId", "req-disabled");

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertDeniedResponse(exchange, "API_KEY_DISABLED", "req-disabled");
    }

    @Test
    void expiredApiKeyReturnsExpiredReason() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        AuthFilter filter = new AuthFilter(jsonWebClient("{\"code\":500,\"msg\":\"API Key 已过期\"}"), redisTemplate, objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v1/models")
            .header("X-API-Key", "afo_sk_expired")
            .build());
        exchange.getAttributes().put("requestId", "req-expired");

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertDeniedResponse(exchange, "API_KEY_EXPIRED", "req-expired");
    }

    private WebClient unusedWebClient() {
        return jsonWebClient("{}");
    }

    private WebClient jsonWebClient(String body) {
        return WebClient.builder()
            .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).body(body).build()))
            .build();
    }

    private void assertDeniedResponse(MockServerWebExchange exchange, String denyReason, String requestId) {
        try {
            JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertEquals(401, body.get("code").asInt());
            assertEquals("API Key access denied", body.get("msg").asText());
            assertEquals("API_KEY_AUTH", body.get("data").get("denyLayer").asText());
            assertEquals(denyReason, body.get("data").get("denyReason").asText());
            assertEquals(requestId, body.get("data").get("requestId").asText());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
