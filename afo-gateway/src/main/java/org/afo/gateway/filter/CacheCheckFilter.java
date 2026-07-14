package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.gateway.cache.CacheConfig;
import org.afo.gateway.cache.GatewayCacheService;
import org.afo.gateway.util.ProxyPathUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Order(-70)
@Component
public class CacheCheckFilter implements WebFilter {

    private static final List<String> ROLE_ORDER = List.of("system", "user", "assistant", "tool");

    private final GatewayCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CacheCheckFilter(GatewayCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!ProxyPathUtils.isProxyPath(path)) {
            return chain.filter(exchange);
        }

        String rawTenantId = exchange.getAttribute("tenantId");
        final String tenantId;
        if (rawTenantId == null || rawTenantId.isBlank()) {
            tenantId = "000000";
        } else {
            tenantId = rawTenantId;
        }

        Long projectId = exchange.getAttribute("projectId");
        Long clientId = exchange.getAttribute("clientId");
        String modelCode = exchange.getAttribute(ModelAccessFilter.ATTR_REQUEST_MODEL);
        final String requestId = exchange.getAttribute("requestId");

        log.debug("[CacheCheckFilter][{}] Entering cache check: tenantId={}, projectId={}, clientId={}, path={}",
            requestId, tenantId, projectId, clientId, path);

        return DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bodyBytes);
                DataBufferUtils.release(dataBuffer);
                return bodyBytes;
            })
            .defaultIfEmpty(new byte[0])
            .flatMap(bodyBytes -> {
                if (bodyBytes.length == 0) {
                    return chain.filter(exchange);
                }

                JsonNode request;
                try {
                    request = objectMapper.readTree(bodyBytes);
                } catch (Exception e) {
                    return chain.filter(exchange);
                }

                if (!isCacheable(request, exchange)) {
                    return chain.filter(rewrap(exchange, bodyBytes));
                }

                boolean stream = request.has("stream") && request.get("stream").asBoolean(false);

                final String finalModelCode = modelCode != null ? modelCode : request.path("model").asText("");
                final Long finalProjectId = projectId;
                final Long finalClientId = clientId;

                return cacheService.loadConfig(tenantId, finalProjectId, finalClientId)
                    .filter(CacheConfig::isEnabled)
                    .flatMap(config -> computeCanonicalHash(request, exchange)
                        .flatMap(promptHash -> cacheService.findByHash(
                                tenantId, finalProjectId, finalClientId, finalModelCode, promptHash)
                            .flatMap(entry -> {
                                log.info("[CacheCheckFilter] Cache HIT: tenant={}, project={}, client={}, model={}",
                                    tenantId, finalProjectId, finalClientId, finalModelCode);
                                cacheService.incrementStats(tenantId, finalProjectId, finalClientId, true);
                                cacheService.writeHitLogAsync(tenantId, finalProjectId, finalClientId,
                                    entry.getEntryId(), finalModelCode, promptHash,
                                    exchange.getAttribute("apiKeyId"),
                                    exchange.getAttribute("requestId"),
                                    entry.getTokenCount() != null ? entry.getTokenCount() : 0);
                                String protocol = exchange.getAttribute("reqProtocol");
                                return writeCachedResponse(exchange, entry.getResponseText(), stream, protocol);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                cacheService.incrementStats(tenantId, finalProjectId, finalClientId, false);
                                log.info("[CacheCheckFilter][{}] Cache MISS: tenant={}, project={}, client={}, model={}, hash={}",
                                    requestId, tenantId, finalProjectId, finalClientId, finalModelCode,
                                    promptHash.substring(0, Math.min(16, promptHash.length())));
                                ServerWebExchange missExchange = rewrap(exchange, bodyBytes);
                                String promptTextCapture = truncate(extractPromptText(request), 500);
                                ServerHttpResponseDecorator decorated = new ResponseCaptureDecorator(
                                    missExchange.getResponse(),
                                    cacheService, objectMapper,
                                    tenantId, finalProjectId, finalClientId,
                                    finalModelCode, promptHash, promptTextCapture,
                                    config, stream, requestId);
                                missExchange = missExchange.mutate().response(decorated).build();
                                return chain.filter(missExchange);
                            }))))
                    .switchIfEmpty(Mono.defer(() -> {
                        log.info("[CacheCheckFilter][{}] Cache DISABLED or config not found: tenant={}, project={}, client={}",
                            requestId, tenantId, finalProjectId, finalClientId);
                        return chain.filter(rewrap(exchange, bodyBytes));
                    }));
            });
    }

    private boolean isCacheable(JsonNode request, ServerWebExchange exchange) {
        if (request.has("tools") || request.has("functions")) return false;
        return true;
    }

    /* test-visible */ Mono<String> computeCanonicalHash(JsonNode request, ServerWebExchange exchange) {
        String precomputed = exchange.getAttribute("reqCanonicalHashSource");
        if (precomputed != null && !precomputed.isEmpty()) {
            return Mono.just(sha256(precomputed));
        }
        JsonNode messages = request.path("messages");
        if (messages == null || !messages.isArray()) return Mono.just("");

        return Flux.fromIterable(() -> messages.elements())
            .filter(msg -> msg.has("role") && msg.has("content"))
            .sort(Comparator.comparingInt(m -> {
                int idx = ROLE_ORDER.indexOf(m.get("role").asText(""));
                return idx >= 0 ? idx : 99;
            }))
            .map(m -> {
                String role = m.get("role").asText().trim();
                String content = extractTextContent(m.get("content"));
                return role + ":" + content;
            })
            .collectList()
            .map(list -> list.stream()
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .trim())
            .map(CacheCheckFilter::sha256);
    }

    private static String sha256(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String extractTextContent(JsonNode content) {
        if (content.isTextual()) {
            return content.asText().trim();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            content.forEach(item -> {
                if ("text".equals(item.path("type").asText(""))) {
                    sb.append(item.path("text").asText(""));
                }
            });
            return sb.toString().trim();
        }
        return "";
    }

    private String extractPromptText(JsonNode request) {
        JsonNode messages = request.path("messages");
        if (messages != null && messages.isArray()) {
            StringBuilder sb = new StringBuilder();
            messages.forEach(msg -> {
                if (msg.has("content") && sb.length() < 512) {
                    JsonNode content = msg.get("content");
                    if (content.isTextual()) {
                        sb.append(content.asText()).append(" ");
                    } else if (content.isArray()) {
                        for (JsonNode block : content) {
                            if ("text".equals(block.path("type").asText(""))) {
                                sb.append(block.path("text").asText()).append(" ");
                            }
                        }
                    }
                }
            });
            return sb.toString().trim();
        }
        return "";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    private ServerWebExchange rewrap(ServerWebExchange exchange, byte[] bodyBytes) {
        return exchange.mutate()
            .request(new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
                }
            })
            .build();
    }

    private Mono<Void> writeCachedResponse(ServerWebExchange exchange,
                                            String responseText, boolean stream,
                                            String protocol) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        if (stream) {
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
            String sseBody = "anthropic".equals(protocol)
                ? buildAnthropicStreamChunk(responseText)
                : buildStreamChunk(responseText);
            byte[] bytes = sseBody.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = "anthropic".equals(protocol)
            ? buildAnthropicJsonResponse(responseText)
            : buildJsonResponse(responseText);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String buildStreamChunk(String text) {
        String escaped = escapeJson(text);
        return "data: {\"id\":\"cache-hit\",\"object\":\"chat.completion.chunk\","
            + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\""
            + escaped
            + "\"},\"finish_reason\":\"stop\"}]}\n\n"
            + "data: [DONE]\n\n";
    }

    private String buildJsonResponse(String text) {
        if (text.trim().startsWith("{")) {
            return text.replaceFirst("\"id\":\"[^\"]*\"", "\"id\":\"cache-hit\"");
        }
        String escaped = escapeJson(text);
        return "{\"id\":\"cache-hit\",\"object\":\"chat.completion\","
            + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\""
            + escaped
            + "\"},\"finish_reason\":\"stop\"}]}";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String buildAnthropicJsonResponse(String text) {
        String escaped = escapeJson(text);
        return "{\"id\":\"msg_cache-hit\",\"type\":\"message\","
            + "\"role\":\"assistant\",\"model\":\"claude-cache\","
            + "\"content\":[{\"type\":\"text\",\"text\":\"" + escaped + "\"}],"
            + "\"stop_reason\":\"end_turn\","
            + "\"usage\":{\"input_tokens\":0,\"output_tokens\":0}}";
    }

    private String buildAnthropicStreamChunk(String text) {
        String escaped = escapeJson(text);
        return "event: message_start\n"
            + "data: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_cache-hit\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-cache\",\"content\":[],\"usage\":{\"input_tokens\":0}}}\n\n"
            + "event: content_block_start\n"
            + "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n"
            + "event: content_block_delta\n"
            + "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"" + escaped + "\"}}\n\n"
            + "event: content_block_stop\n"
            + "data: {\"type\":\"content_block_stop\",\"index\":0}\n\n"
            + "event: message_delta\n"
            + "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":0}}\n\n"
            + "event: message_stop\n"
            + "data: {\"type\":\"message_stop\"}\n\n";
    }

    static class ResponseCaptureDecorator extends ServerHttpResponseDecorator {

        private final GatewayCacheService cacheService;
        private final ObjectMapper objectMapper;
        private final String tenantId;
        private final Long projectId;
        private final Long clientId;
        private final String modelCode;
        private final String promptHash;
        private final String promptText;
        private final CacheConfig config;
        private final boolean isStream;
        private final String requestId;

        ResponseCaptureDecorator(ServerHttpResponse delegate,
                                 GatewayCacheService cacheService,
                                 ObjectMapper objectMapper,
                                 String tenantId, Long projectId, Long clientId,
                                 String modelCode, String promptHash,
                                 String promptText, CacheConfig config,
                                 boolean isStream, String requestId) {
            super(delegate);
            this.cacheService = cacheService;
            this.objectMapper = objectMapper;
            this.tenantId = tenantId;
            this.projectId = projectId;
            this.clientId = clientId;
            this.modelCode = modelCode;
            this.promptHash = promptHash;
            this.promptText = promptText;
            this.config = config;
            this.isStream = isStream;
            this.requestId = requestId;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            StringBuilder acc = new StringBuilder();
            Flux<DataBuffer> intercepted = Flux.from(body)
                .map(b -> (DataBuffer) b)
                .doOnNext(buffer -> {
                    ByteBuffer bb = buffer.asByteBuffer();
                    if (bb.remaining() == 0) return;
                    byte[] bytes = new byte[bb.remaining()];
                    bb.get(bytes);
                    acc.append(new String(bytes, StandardCharsets.UTF_8));
                })
                .doFinally(signalType -> {
                    if (signalType != SignalType.ON_COMPLETE) {
                        log.warn("[ResponseCaptureDecorator][{}] Response stream terminated with signal={}, skipping cache write", requestId, signalType);
                        return;
                    }
                    String rawResponse = acc.toString();
                    if (rawResponse.isEmpty()) {
                        log.warn("[ResponseCaptureDecorator][{}] Captured response is empty (signal={}), no cache entry will be written. This may indicate upstream model returned empty content.", requestId, signalType);
                        return;
                    }
                    String extractedText = extractResponseText(rawResponse);
                    if (extractedText == null || extractedText.isEmpty()) {
                        log.warn("[ResponseCaptureDecorator][{}] Extracted response text is empty, raw length={}, isStream={}", requestId, rawResponse.length(), isStream);
                        return;
                    }
                    log.info("[ResponseCaptureDecorator][{}] Writing cache entry: model={}, hash={}, textLen={}",
                        requestId, modelCode, promptHash.substring(0, Math.min(16, promptHash.length())), extractedText.length());
                    cacheService.saveEntryAsync(tenantId, projectId, clientId,
                        modelCode, promptHash, promptText, extractedText, config);
                });
            return getDelegate().writeWith(intercepted);
        }

        private String extractResponseText(String rawResponse) {
            if (rawResponse == null || rawResponse.isEmpty()) return "";
            if (!isStream) {
                try {
                    JsonNode root = objectMapper.readTree(rawResponse);
                    JsonNode contentNode = root.at("/choices/0/message/content");
                    if (!contentNode.isMissingNode()) {
                        String content = contentNode.asText();
                        return content.isEmpty() ? rawResponse : content;
                    }
                    JsonNode anthropicContent = root.path("content");
                    if (anthropicContent.isArray()) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode block : anthropicContent) {
                            if ("text".equals(block.path("type").asText(""))) {
                                sb.append(block.path("text").asText());
                            }
                        }
                        String result = sb.toString();
                        return result.isEmpty() ? rawResponse : result;
                    }
                    return rawResponse;
                } catch (Exception e) {
                    return rawResponse;
                }
            }
            StringBuilder text = new StringBuilder();
            for (String line : rawResponse.split("\n")) {
                if (line.startsWith("data: ") && !"data: [DONE]".equals(line.trim())) {
                    String jsonStr = line.substring(6);
                    try {
                        JsonNode node = objectMapper.readTree(jsonStr);
                        JsonNode content = node.at("/choices/0/delta/content");
                        if (!content.isMissingNode()) {
                            text.append(content.asText());
                            continue;
                        }
                        JsonNode msgContent = node.at("/choices/0/message/content");
                        if (!msgContent.isMissingNode()) {
                            text.append(msgContent.asText());
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (line.startsWith("data: ")) {
                    String jsonStr = line.substring(6);
                    try {
                        JsonNode node = objectMapper.readTree(jsonStr);
                        if ("content_block_delta".equals(node.path("type").asText(""))) {
                            String deltaText = node.at("/delta/text").asText(null);
                            if (deltaText != null) {
                                text.append(deltaText);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            String result = text.toString();
            return result.isEmpty() ? rawResponse : result;
        }
    }
}
