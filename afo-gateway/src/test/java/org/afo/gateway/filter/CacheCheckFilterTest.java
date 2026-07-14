package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.afo.gateway.cache.GatewayCacheService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class CacheCheckFilterTest {

    private final GatewayCacheService cacheService = mock(GatewayCacheService.class);
    private final CacheCheckFilter filter = new CacheCheckFilter(cacheService);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static MockServerWebExchange emptyExchange() {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get("/v1/chat/completions").build());
    }

    @Test
    void hashSameForSameMessages() throws Exception {
        String body = """
            {
                "model": "gpt-4",
                "messages": [
                    {"role": "system", "content": "You are helpful."},
                    {"role": "user", "content": "Hello"}
                ]
            }
            """;

        JsonNode request1 = objectMapper.readTree(body);
        JsonNode request2 = objectMapper.readTree(body);

        String hash1 = filter.computeCanonicalHash(request1, emptyExchange()).block();
        String hash2 = filter.computeCanonicalHash(request2, emptyExchange()).block();

        assertEquals(hash1, hash2);
    }

    @Test
    void hashDifferentForDifferentContent() throws Exception {
        JsonNode request1 = objectMapper.readTree("""
            {
                "messages": [
                    {"role": "user", "content": "Hello"}
                ]
            }
            """);

        JsonNode request2 = objectMapper.readTree("""
            {
                "messages": [
                    {"role": "user", "content": "World"}
                ]
            }
            """);

        String hash1 = filter.computeCanonicalHash(request1, emptyExchange()).block();
        String hash2 = filter.computeCanonicalHash(request2, emptyExchange()).block();

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashIgnoresRoleOrder() throws Exception {
        JsonNode request1 = objectMapper.readTree("""
            {
                "messages": [
                    {"role": "system", "content": "You are helpful."},
                    {"role": "user", "content": "Hello"},
                    {"role": "assistant", "content": "Hi there"}
                ]
            }
            """);

        JsonNode request2 = objectMapper.readTree("""
            {
                "messages": [
                    {"role": "user", "content": "Hello"},
                    {"role": "assistant", "content": "Hi there"},
                    {"role": "system", "content": "You are helpful."}
                ]
            }
            """);

        String hash1 = filter.computeCanonicalHash(request1, emptyExchange()).block();
        String hash2 = filter.computeCanonicalHash(request2, emptyExchange()).block();

        assertEquals(hash1, hash2);
    }

    @Test
    void skipsCacheForNonProxyPathContainingChatCompletions() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/llm/v1/chat/completions")
                .body("""
                    {"model":"deepseek-v4-pro","messages":[{"role":"user","content":"hi"}]}
                    """));
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(cacheService, never()).loadConfig(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
