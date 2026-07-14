package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API Key 鉴权过滤器
 * 
 * L0职责：
 * 1. 从 Header 中提取 API Key（`X-API-Key` 或 `Authorization: Bearer`）
 * 2. 检查 Redis 缓存 `gateway:apikey:{sha256(key)}`
 * 3. 缓存未命中时调用 `/api/gateway/api-keys/validate` 接口验证
 * 4. 验证成功写入 Redis 缓存（TTL 5分钟）并放行
 * 5. 验证失败返回 401
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Order(-90)
@Component
@RequiredArgsConstructor
public class AuthFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CACHE_PREFIX = "gateway:apikey:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final WebClient adminWebClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            return chain.filter(exchange);
        }
        
        String apiKey = extractApiKey(exchange);
        if (apiKey == null || apiKey.isBlank()) {
            return unauthorized(exchange, "API_KEY_MISSING");
        }

        String cacheKey = CACHE_PREFIX + sha256(apiKey);

        // 先查 Redis 缓存
        return redisTemplate.opsForValue().get(cacheKey)
            .defaultIfEmpty("__MISS__")
            .flatMap(cached -> {
                if ("__MISS__".equals(cached)) {
                    return validateAndCache(exchange, chain, apiKey, cacheKey);
                }
                try {
                    JsonNode node = objectMapper.readTree(cached);
                    if (!node.has("keyScope")) {
                        log.info("[AuthFilter] Cached API Key data missing keyScope, refreshing {}", cacheKey.substring(0, 20) + "...");
                        return validateAndCache(exchange, chain, apiKey, cacheKey);
                    }
                    populateExchangeAttributes(exchange, node);
                    log.debug("[AuthFilter] Cache hit for key {}", cacheKey.substring(0, 20) + "...");
                    return chain.filter(exchange);
                } catch (Exception e) {
                    log.error("[AuthFilter] Failed to parse cached API Key data", e);
                    return validateAndCache(exchange, chain, apiKey, cacheKey);
                }
            });
    }

    private Mono<Void> validateAndCache(ServerWebExchange exchange, WebFilterChain chain,
                                         String apiKey, String cacheKey) {
        String requestId = exchange.getAttribute("requestId");
        log.debug("[AuthFilter][{}] Cache miss, validating API Key", requestId);

        return adminWebClient.get()
            .uri("/api/gateway/api-keys/validate?key={key}", apiKey)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(3))
            .flatMap(bodyString -> {
                log.debug("[AuthFilter][{}] Validate response: {}", requestId, bodyString);
                try {
                    JsonNode body = objectMapper.readTree(bodyString);
                    int code = body.has("code") ? body.get("code").asInt() : -1;
                    boolean hasData = body.has("data") && !body.get("data").isNull();
                    log.debug("[AuthFilter][{}] code={}, hasData={}", requestId, code, hasData);

                    if (code == 200 && hasData) {
                        JsonNode data = body.get("data");
                        log.debug("[AuthFilter][{}] data.tenantId={}", requestId, data.has("tenantId") ? data.get("tenantId").asText() : "missing");
                        populateExchangeAttributes(exchange, data);
                        log.debug("[AuthFilter][{}] tenantId={}, apiKeyId={}", requestId,
                            exchange.getAttribute("tenantId"), exchange.getAttribute("apiKeyId"));

                        String dataStr = data.toString();
                        log.debug("[AuthFilter][{}] Caching key={}, data={}", requestId, cacheKey, dataStr);

                        return redisTemplate.opsForValue()
                            .set(cacheKey, dataStr, CACHE_TTL)
                            .doOnSuccess(v -> log.debug("[AuthFilter][{}] Cache set success", requestId))
                            .doOnError(e -> log.error("[AuthFilter][{}] Cache set error: {}", requestId, e.getMessage()))
                            .then(chain.filter(exchange))
                            .doOnSuccess(v -> log.debug("[AuthFilter][{}] Chain filter success", requestId))
                            .doOnError(e -> log.error("[AuthFilter][{}] Chain filter error: {}", requestId, e.getMessage()));
                    }
                } catch (Exception e) {
                    log.error("[AuthFilter][{}] Parse error", requestId, e);
                }
                return unauthorized(exchange, denyReasonFromBody(bodyString));
            })
            .onErrorResume(e -> {
                log.error("[AuthFilter][{}] Remote validate failed: {} - {}", requestId, e.getClass().getSimpleName(), e.getMessage());
                return unauthorized(exchange, "API_KEY_VALIDATE_FAILED");
            });
    }

    private void populateExchangeAttributes(ServerWebExchange exchange, JsonNode data) {
        exchange.getAttributes().put("tenantId", data.has("tenantId") ? data.get("tenantId").asText() : "0");
        exchange.getAttributes().put("apiKeyId", data.has("apiKeyId") ? data.get("apiKeyId").asText() : "");
        exchange.getAttributes().put("keyScope", data.has("keyScope") && !data.get("keyScope").isNull() ? data.get("keyScope").asText() : "");
        String teamTag = (data.has("teamTag") && !data.get("teamTag").isNull()) ? data.get("teamTag").asText() : "";
        exchange.getAttributes().put("teamTag", teamTag);
        if (data.has("projectId") && !data.get("projectId").isNull()) {
            exchange.getAttributes().put("projectId", data.get("projectId").asLong());
        }
        if (data.has("clientId") && !data.get("clientId").isNull()) {
            exchange.getAttributes().put("clientId", data.get("clientId").asLong());
            exchange.getAttributes().put("appId", data.get("clientId").asText());
        }
        if (data.has("ownerUserId") && !data.get("ownerUserId").isNull()) {
            exchange.getAttributes().put("userId", data.get("ownerUserId").asText());
        } else if (data.has("userId") && !data.get("userId").isNull()) {
            exchange.getAttributes().put("userId", data.get("userId").asText());
        }
        if (data.has("deptId") && !data.get("deptId").isNull()) {
            exchange.getAttributes().put("department", data.get("deptId").asText());
        } else if (data.has("department") && !data.get("department").isNull()) {
            exchange.getAttributes().put("department", data.get("department").asText());
        }
    }

    private String extractApiKey(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        String apiKey = headers.getFirst(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange, String denyReason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("denyLayer", "API_KEY_AUTH");
        data.put("denyReason", denyReason);
        data.put("requestId", exchange.getAttribute("requestId"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("msg", "API Key access denied");
        body.put("data", data);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":401,\"msg\":\"API Key access denied\"}".getBytes(StandardCharsets.UTF_8);
        }
        
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }

    private String denyReasonFromBody(String bodyString) {
        try {
            JsonNode body = objectMapper.readTree(bodyString);
            if (body.has("data") && body.get("data").has("denyReason")) {
                return body.get("data").get("denyReason").asText("API_KEY_INVALID");
            }
            String message = body.has("msg") && !body.get("msg").isNull() ? body.get("msg").asText() : "";
            if (message.contains("过期")) {
                return "API_KEY_EXPIRED";
            }
            if (message.contains("停用")) {
                return "API_KEY_DISABLED";
            }
        } catch (Exception ignored) {
            return "API_KEY_INVALID";
        }
        return "API_KEY_INVALID";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

}
