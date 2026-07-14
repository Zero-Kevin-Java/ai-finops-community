package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.rabbitmq.config.RequestLogQueueConfig;
import org.afo.common.rabbitmq.message.RequestLogMessage;
import org.afo.common.rabbitmq.utils.RabbitMqUtils;
import org.afo.gateway.protocol.ProtocolAwareRequest;
import org.afo.gateway.config.LiteLLMProperties;
import org.afo.gateway.util.ProxyPathUtils;
import org.afo.gateway.config.ModelRouteConfig;
import org.afo.gateway.routing.RouteDecisionEngine;
import org.afo.gateway.routing.RouteResult;
import org.afo.gateway.routing.RuleMatchContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 代理转发过滤器（OpenAI 兼容接口 / L0 开源版）
 *
 * 从 Redis 读取模型配置，改写 body.model 并通过 header 传递 api_key/api_base 到 llm-router。
 * 模型准入协作：优先使用 ModelAccessFilter 写入的 ATTR_ROUTE_RESULT，避免重复决策。
 *
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Order(-68)
@Component
public class ProxyForwardFilter implements WebFilter {

    public static final String ATTR_USAGE_RAW = "llmUsageRaw";
    public static final String ATTR_PROMPT_TOKENS = "llmPromptTokens";
    public static final String ATTR_COMPLETION_TOKENS = "llmCompletionTokens";
    public static final String ATTR_TOTAL_TOKENS = "llmTotalTokens";
    public static final String ATTR_CACHED_TOKENS = "llmCachedTokens";
    public static final String ATTR_REASONING_TOKENS = "llmReasoningTokens";

    private final WebClient litellmWebClient;
    private final WebClient adminWebClient;
    private final RouteDecisionEngine routeDecisionEngine;
    private final ObjectMapper objectMapper;
    private final LiteLLMProperties liteLLMProperties;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final ModelRouteConfig NO_CONFIG = new ModelRouteConfig();

    @Value("${afo.gateway.admin.internal-token:}")
    private String adminInternalToken;

    public ProxyForwardFilter(WebClient litellmWebClient,
                              @Qualifier("adminWebClient") WebClient adminWebClient,
                              RouteDecisionEngine routeDecisionEngine,
                              ObjectMapper objectMapper,
                              LiteLLMProperties liteLLMProperties,
                              ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.litellmWebClient = litellmWebClient;
        this.adminWebClient = adminWebClient;
        this.routeDecisionEngine = routeDecisionEngine;
        this.objectMapper = objectMapper;
        this.liteLLMProperties = liteLLMProperties;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!ProxyPathUtils.isProxyPath(path)) {
            return chain.filter(exchange);
        }

        String requestId = exchange.getAttribute("requestId");
        String tenantId = exchange.getAttribute("tenantId");
        String tenantMode = exchange.getAttribute("tenantMode");
        String apiKeyId = exchange.getAttribute("apiKeyId");
        String keyScope = exchange.getAttribute("keyScope");
        String teamTag = exchange.getAttribute("teamTag");
        String department = exchange.getAttribute("department");
        String userId = exchange.getAttribute("userId");
        String appId = exchange.getAttribute("appId");

        final String finalTenantId = (tenantId != null) ? tenantId : "0";
        final String finalTenantMode = (tenantMode != null) ? tenantMode : "STEADY";

        final Mono<Void> resultChain = DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bodyBytes);
                DataBufferUtils.release(dataBuffer);
                return bodyBytes;
            })
            .flatMap(bodyBytes -> {
                String body = new String(bodyBytes, StandardCharsets.UTF_8);

                ProtocolAwareRequest protoReq = ProtocolAwareRequest.from(path, bodyBytes, objectMapper);
                final String model = protoReq.getModel();
                final String prompt = protoReq.getPromptText();

                exchange.getAttributes().put("reqModelCode", model != null ? model : "");
                exchange.getAttributes().put("reqStream", protoReq.isStream() ? "1" : "0");
                exchange.getAttributes().put("reqProtocol", protoReq.getProtocol());
                exchange.getAttributes().put("reqCanonicalHashSource", protoReq.getCanonicalHashSource());

                log.debug("[ProxyForwardFilter][{}] Forwarding to llm-router, mode={}, model={}",
                    requestId, finalTenantMode, model);

                RouteResult existingResult = exchange.getAttribute(ModelAccessFilter.ATTR_ROUTE_RESULT);
                final Mono<RouteResult> resultMono;
                if (existingResult != null) {
                    resultMono = Mono.just(existingResult);
                } else {
                    RuleMatchContext routingContext = RuleMatchContext.builder()
                        .tenantId(finalTenantId)
                        .requestId(requestId)
                        .apiKeyId(apiKeyId)
                        .teamTag(teamTag)
                        .department(department)
                        .userId(userId)
                        .appId(appId)
                        .path(path)
                        .sourceModel(model)
                        .modelType(resolveModelType(path))
                        .headers(flattenHeaders(exchange.getRequest().getHeaders()))
                        .request(parseRequest(body, requestId))
                        .build();

                    resultMono = routeDecisionEngine.decideWithRequestId(
                        routingContext, finalTenantMode, null, null, requestId, apiKeyId, keyScope,
                        prompt);
                }

                return resultMono.flatMap(result -> {
                exchange.getAttributes().put(ModelAccessFilter.ATTR_ROUTE_RESULT, result);

                log.debug("[ProxyForwardFilter][{}] Route decision: model={} -> target={}, reason={}, latency={}ms",
                    requestId, model, result.getTargetModel(), result.getReason(), result.getDecisionLatencyMs());

                if (result.getReason() == RouteResult.RouteReason.DENIED) {
                    if (!exchange.getResponse().isCommitted()) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    }
                    String errorBody = String.format(
                        "{\"code\":403,\"msg\":\"Model access denied: %s\"}",
                        result.getDenyReason() != null ? result.getDenyReason() : "Unknown reason");
                    byte[] errBytes = errorBody.getBytes(StandardCharsets.UTF_8);
                    return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap(errBytes)));
                }

                final String finalModel = (result.getTargetModel() != null && !"null".equals(result.getTargetModel()))
                    ? result.getTargetModel() : model;

                final String effectiveModel;
                if (finalModel != null) {
                    effectiveModel = finalModel;
                } else if (result.getSourceModel() != null) {
                    log.warn("[ProxyForwardFilter][{}] Model is null from body, falling back to routeSourceModel={}",
                        requestId, result.getSourceModel());
                    effectiveModel = result.getSourceModel();
                } else {
                    log.warn("[ProxyForwardFilter][{}] Model is null from both body and routeResult, returning 400", requestId);
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    String errBody = "{\"code\":400,\"msg\":\"Missing model in request\"}";
                    byte[] errBytes = errBody.getBytes(StandardCharsets.UTF_8);
                    return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(errBytes)));
                }

                final String fModel = effectiveModel;

                return resolveModelConfig(finalTenantId, effectiveModel)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.warn("[ProxyForwardFilter][{}] resolveModelConfig failed, using NO_CONFIG: {}",
                            requestId, e.getMessage());
                        return Mono.just(NO_CONFIG);
                    })
                    .flatMap(routeConfig -> {
                        String litellmModel = (routeConfig != null && routeConfig.getLitellmModel() != null)
                            ? routeConfig.getLitellmModel() : fModel;
                        String apiKey = (routeConfig != null) ? routeConfig.getApiKey() : null;
                        String apiBase = (routeConfig != null) ? routeConfig.getApiBase() : null;
                        String apiVersion = (routeConfig != null) ? routeConfig.getApiVersion() : null;

                        if (routeConfig == null || routeConfig == NO_CONFIG) {
                            log.warn("[ProxyForwardFilter][{}] Model config not found: tenant={}, model={}",
                                requestId, finalTenantId, fModel);
                        }

                        byte[] modifiedBodyBytes = protoReq.rewriteModel(litellmModel, objectMapper);
                        String modifiedBody = new String(modifiedBodyBytes, StandardCharsets.UTF_8);
                        modifiedBody = ensureStreamUsageIncluded(objectMapper, modifiedBody);

                        final String finalLitellmModel = litellmModel;
                        final String finalApiKey = apiKey;
                        final String finalApiBase = apiBase;
                        final String finalApiVersion = apiVersion;
                        final String finalModifiedBody = modifiedBody;

                        return litellmWebClient.post()
                            .uri(path)
                            .headers(headers -> {
                                exchange.getRequest().getHeaders().forEach((key, values) -> {
                                    if (!key.equalsIgnoreCase("X-API-Key")) {
                                        headers.put(key, values);
                                    }
                                });
                                if (finalApiKey != null && !finalApiKey.isEmpty()) {
                                    headers.set("X-API-Key", finalApiKey);
                                }
                                if (finalApiBase != null && !finalApiBase.isEmpty()) {
                                    headers.set("X-API-Base", finalApiBase);
                                }
                                if (finalApiVersion != null && !finalApiVersion.isEmpty()) {
                                    headers.set("X-API-Version", finalApiVersion);
                                }
                                headers.set("X-LiteLLM-Model", finalLitellmModel);
                                headers.setContentType(MediaType.APPLICATION_JSON);
                            })
                            .bodyValue(finalModifiedBody)
                            .exchangeToMono(response -> {
                                if (!exchange.getResponse().isCommitted()) {
                                    org.springframework.http.HttpHeaders respHeaders = exchange.getResponse().getHeaders();
                                    response.headers().asHttpHeaders().forEach((key, values) -> {
                                        if (!key.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)
                                            && !key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                                            try {
                                                respHeaders.put(key, values);
                                            } catch (UnsupportedOperationException e) {
                                                log.warn("[ProxyForwardFilter][{}] Cannot modify committed response headers: key={}", requestId, key);
                                            }
                                        }
                                    });
                                }

                                MediaType contentType = response.headers().contentType().orElse(null);
                                if (contentType != null && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                                    AtomicReference<StringBuilder> accRef = new AtomicReference<>(new StringBuilder());
                                    Flux<DataBuffer> responseFlux = response.bodyToFlux(DataBuffer.class)
                                        .doOnNext(buffer -> {
                                            ByteBuffer bb = buffer.asByteBuffer();
                                            byte[] bytes = new byte[bb.remaining()];
                                            bb.get(bytes);
                                            String chunk = new String(bytes, StandardCharsets.UTF_8);
                                            accRef.get().append(chunk);
                                        })
                                        .doFinally(signalType -> {
                                            if (signalType != SignalType.CANCEL) {
                                                captureUsage(exchange, accRef.get().toString());
                                            }
                                        });

                                    if (!exchange.getResponse().isCommitted()) {
                                        exchange.getResponse().setStatusCode(response.statusCode());
                                    }
                                    return exchange.getResponse().writeWith(responseFlux);
                                }

                                return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(responseBody -> {
                                        captureUsage(exchange, responseBody);
                                        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

                                        if (!exchange.getResponse().isCommitted()) {
                                            exchange.getResponse().setStatusCode(response.statusCode());
                                            if (contentType != null) {
                                                exchange.getResponse().getHeaders().setContentType(contentType);
                                            }
                                            exchange.getResponse().getHeaders().setContentLength(responseBytes.length);
                                        }

                                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                            .bufferFactory()
                                            .wrap(responseBytes)));
                                    });
                            })
                            .timeout(Duration.ofMillis(liteLLMProperties.getTimeout()));
                    });
            });
            })
            .onErrorResume(e -> {
                log.error("[ProxyForwardFilter][{}] llm-router request failed: {}", requestId, e.getMessage(), e);
                if (exchange.getResponse().isCommitted()) {
                    log.warn("[ProxyForwardFilter][{}] Response already committed, cannot send error body", requestId);
                    HttpStatusCode committedStatus = exchange.getResponse().getStatusCode();
                    if (committedStatus != null && committedStatus.is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    exchange.getAttributes().put("reqErrorCode", "502");
                    exchange.getAttributes().put("reqErrorMessage", "LLM router proxy unavailable (response already committed)");
                    return Mono.empty();
                }
                exchange.getAttributes().put("reqErrorCode", "502");
                exchange.getAttributes().put("reqErrorMessage", "LLM router proxy unavailable");
                exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                String errorBody = "{\"code\":502,\"msg\":\"LLM router proxy unavailable\"}";
                byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
                return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes)));
            });
        return resultChain.doFinally(signalType -> sendRequestLog(exchange, signalType));
    }

    private JsonNode parseRequest(String body, String requestId) {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("[ProxyForwardFilter][{}] Failed to parse request body for routing config: {}", requestId, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> result = new HashMap<>();
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, values.get(0));
            }
        });
        return result;
    }

    private String resolveModelType(String path) {
        if (path == null) {
            return "chat";
        }
        if (path.contains("embeddings")) {
            return "embedding";
        }
        if (path.contains("images")) {
            return "image";
        }
        if (path.contains("audio")) {
            return "audio";
        }
        return "chat";
    }

    private Mono<ModelRouteConfig> resolveModelConfig(String tenantId, String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            log.info("[ProxyForwardFilter] resolveModelConfig: modelCode is null/empty, returning NO_CONFIG");
            return Mono.just(NO_CONFIG);
        }
        String key = "tenant:" + tenantId + ":model:" + modelCode;

        return resolveFromRedis(key)
            .switchIfEmpty(Mono.defer(() -> {
                log.info("[ProxyForwardFilter] redis miss for key={}, falling back to Admin API", key);
                return fallbackToAdminWithWriteBack(key, tenantId, modelCode);
            }));
    }

    private Mono<ModelRouteConfig> resolveFromRedis(String key) {
        return reactiveRedisTemplate.opsForHash().entries(key)
            .collectMap(e -> (String) e.getKey(), e -> (String) e.getValue())
            .timeout(Duration.ofSeconds(3))
            .flatMap(map -> {
                if (map.isEmpty()) {
                    return Mono.empty();
                }
                String litellmModel = map.getOrDefault("litellm_model", map.get("litellmModel"));
                if (litellmModel == null || litellmModel.isEmpty()) {
                    return Mono.empty();
                }
                ModelRouteConfig config = new ModelRouteConfig();
                config.setLitellmModel(litellmModel);
                config.setApiKey(map.getOrDefault("api_key", map.get("apiKey")));
                config.setApiBase(map.getOrDefault("api_base", map.get("apiBase")));
                config.setProtocol(map.getOrDefault("protocol", map.get("protocol")));
                config.setApiVersion(map.getOrDefault("api_version", map.get("apiVersion")));
                log.info("[ProxyForwardFilter] redis hit for key={}, model={}", key, litellmModel);
                return Mono.just(config);
            })
            .onErrorResume(e -> {
                log.warn("[ProxyForwardFilter] redis read failed for key={}: {}", key, e.getMessage());
                return Mono.empty();
            });
    }

    private Mono<ModelRouteConfig> fallbackToAdminWithWriteBack(String key, String tenantId, String modelCode) {
        return fallbackToAdmin(tenantId, modelCode)
            .flatMap(config -> {
                if (config != null && config != NO_CONFIG) {
                    return writeBackToRedis(key, config)
                        .then(Mono.just(config));
                }
                return Mono.just(config);
            });
    }

    private Mono<ModelRouteConfig> fallbackToAdmin(String tenantId, String modelCode) {
        WebClient.RequestHeadersSpec<?> spec = adminWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/llm/model/config")
                .queryParam("tenantId", tenantId)
                .queryParam("modelCode", modelCode)
                .build());
        if (adminInternalToken != null && !adminInternalToken.isEmpty()) {
            spec.header("X-Internal-Token", adminInternalToken);
        }
        return spec.retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnNext(response -> {
                Object code = response.get("code");
                Object msg = response.get("msg");
                log.info("[ProxyForwardFilter] Admin response for {}:{}: code={}, msg={}",
                    tenantId, modelCode, code, msg);
            })
            .map(response -> {
                Object data = response.get("data");
                if (data == null) {
                    log.warn("[ProxyForwardFilter] Admin returned null data for {}:{}, full response: {}",
                        tenantId, modelCode, response);
                    return NO_CONFIG;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                ModelRouteConfig config = new ModelRouteConfig();
                config.setLitellmModel(Objects.toString(dataMap.get("litellmModel"), null));
                config.setApiKey(Objects.toString(dataMap.get("apiKey"), null));
                config.setApiBase(Objects.toString(dataMap.get("apiBase"), null));
                config.setProtocol(Objects.toString(dataMap.get("protocol"), null));
                config.setApiVersion(Objects.toString(dataMap.get("apiVersion"), null));
                return config;
            })
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                log.error("[ProxyForwardFilter] Fallback failed for {}:{}: {}",
                    tenantId, modelCode, e.getMessage());
                return Mono.just(NO_CONFIG);
            });
    }

    private Mono<Void> writeBackToRedis(String key, ModelRouteConfig config) {
        Map<String, String> map = new HashMap<>();
        map.put("litellm_model", config.getLitellmModel());
        if (config.getApiKey() != null)    map.put("api_key", config.getApiKey());
        if (config.getApiBase() != null)   map.put("api_base", config.getApiBase());
        if (config.getProtocol() != null)  map.put("protocol", config.getProtocol());
        if (config.getApiVersion() != null) map.put("api_version", config.getApiVersion());
        String csRaw = Objects.toString(config.getLitellmModel(), "")
            + "|" + Objects.toString(config.getApiKey(), "")
            + "|" + Objects.toString(config.getApiBase(), "")
            + "|" + Objects.toString(config.getProtocol(), "")
            + "|" + Objects.toString(config.getApiVersion(), "");
        map.put("_checksum", Integer.toHexString(csRaw.hashCode()));
        return reactiveRedisTemplate.opsForHash().putAll(key, map)
            .doOnSuccess(v -> log.info("[ProxyForwardFilter] Self-heal: write-back to Redis key={}", key))
            .doOnError(e -> log.warn("[ProxyForwardFilter] Self-heal write-back failed for key={}: {}", key, e.getMessage()))
            .then();
    }

    static String ensureStreamUsageIncluded(ObjectMapper objectMapper, String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            ensureStreamUsageIncluded(objectMapper, node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return body;
        }
    }

    private static void ensureStreamUsageIncluded(ObjectMapper objectMapper, JsonNode node) {
        if (!(node instanceof ObjectNode objectNode) || !node.path("stream").asBoolean(false)) {
            return;
        }
        JsonNode existing = objectNode.get("stream_options");
        ObjectNode streamOptions = existing instanceof ObjectNode existingObject
            ? existingObject
            : objectMapper.createObjectNode();
        streamOptions.put("include_usage", true);
        objectNode.set("stream_options", streamOptions);
    }


    private void sendRequestLog(ServerWebExchange exchange, SignalType signalType) {
        try {
            String requestId = exchange.getAttribute("requestId");
            String path = exchange.getRequest().getPath().value();
            if (!ProxyPathUtils.isProxyPath(path) || requestId == null) return;

            if (signalType == SignalType.CANCEL) {
                log.warn("[ProxyForwardFilter][{}] sendRequestLog: signalType=CANCEL, skipping log entirely", requestId);
                return;
            }

            if (signalType == SignalType.ON_ERROR) {
                if (exchange.getAttribute("reqErrorCode") == null) {
                    exchange.getAttributes().put("reqErrorCode", "500");
                    exchange.getAttributes().put("reqErrorMessage", "Unhandled proxy error");
                }
            }

            RequestLogMessage msg = new RequestLogMessage();
            msg.setRequestId(requestId);
            msg.setTenantId(exchange.getAttribute("tenantId"));
            msg.setProjectId(toLong(exchange.getAttribute("projectId")));
            msg.setClientId(toLong(exchange.getAttribute("clientId")));
            msg.setKeyId(toLong(exchange.getAttribute("apiKeyId")));
            msg.setModelCode(exchange.getAttribute("reqModelCode"));

            RouteResult routeResult = exchange.getAttribute(ModelAccessFilter.ATTR_ROUTE_RESULT);
            if (routeResult != null && routeResult.getTargetModel() != null) {
                msg.setTargetModelCode(routeResult.getTargetModel());
            }

            msg.setRequestPath(path);
            msg.setStream(Objects.toString(exchange.getAttribute("reqStream"), "0"));
            msg.setHttpStatus(exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value() : null);
            msg.setClientIp(exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : null);
            msg.setUserAgent(exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT));
            msg.setTraceId(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"));
            msg.setUsageRaw(exchange.getAttribute(ATTR_USAGE_RAW));
            msg.setPromptTokens(toLong(exchange.getAttribute(ATTR_PROMPT_TOKENS)));
            msg.setCompletionTokens(toLong(exchange.getAttribute(ATTR_COMPLETION_TOKENS)));
            msg.setTotalTokens(toLong(exchange.getAttribute(ATTR_TOTAL_TOKENS)));
            msg.setCachedTokens(toLong(exchange.getAttribute(ATTR_CACHED_TOKENS)));
            msg.setReasoningTokens(toLong(exchange.getAttribute(ATTR_REASONING_TOKENS)));
            msg.setTimestamp(Instant.now());

            Instant startTime = exchange.getAttribute("startTime");
            if (startTime != null) {
                msg.setLatencyMs(Duration.between(startTime, Instant.now()).toMillis());
            }

            String errorCode = exchange.getAttribute("reqErrorCode");
            String errorMessage = exchange.getAttribute("reqErrorMessage");

            if (errorCode != null || errorMessage != null) {
                int httpStatus = msg.getHttpStatus() != null ? msg.getHttpStatus() : 0;
                if (httpStatus >= 400 && httpStatus < 500) {
                    msg.setRequestStatus("client_error");
                } else {
                    msg.setRequestStatus("server_error");
                }
            } else if (msg.getHttpStatus() != null) {
                if (msg.getHttpStatus() >= 200 && msg.getHttpStatus() < 300) {
                    msg.setRequestStatus("success");
                } else if (msg.getHttpStatus() >= 400 && msg.getHttpStatus() < 500) {
                    msg.setRequestStatus("client_error");
                } else {
                    msg.setRequestStatus("server_error");
                }
            } else {
                msg.setRequestStatus("error");
            }

            if (errorCode != null) msg.setErrorCode(errorCode);
            if (errorMessage != null) msg.setErrorMessage(errorMessage);

            log.info("[ProxyForwardFilter][{}] sendRequestLog: modelCode={} status={} httpStatus={} promptTokens={} completionTokens={} totalTokens={} usageRawLen={}",
                requestId, msg.getModelCode(), msg.getRequestStatus(), msg.getHttpStatus(),
                msg.getPromptTokens(), msg.getCompletionTokens(), msg.getTotalTokens(),
                msg.getUsageRaw() != null ? msg.getUsageRaw().length() : 0);

            RabbitMqUtils.convertAndSend(
                RequestLogQueueConfig.EXCHANGE_NAME,
                RequestLogQueueConfig.ROUTING_KEY,
                msg);

            log.info("[ProxyForwardFilter][{}] Request log MQ sent: model={}, status={}, http={}, latency={}ms",
                requestId, msg.getModelCode(), msg.getRequestStatus(), msg.getHttpStatus(), msg.getLatencyMs());
        } catch (Exception e) {
            log.warn("[ProxyForwardFilter] Failed to send request log MQ: {}", e.getMessage());
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void captureUsage(ServerWebExchange exchange, String responseBody) {
        UsageStats usage = extractUsage(responseBody);
        if (usage == null || usage.usageRaw() == null || usage.usageRaw().isBlank()) {
            String requestId = exchange.getAttribute("requestId");
            log.warn("[ProxyForwardFilter][{}] captureUsage: no usage extracted, bodyLen={}, bodyPreview={}",
                requestId, responseBody != null ? responseBody.length() : 0,
                responseBody != null ? responseBody.substring(0, Math.min(200, responseBody.length())) : "null");
            return;
        }
        exchange.getAttributes().put(ATTR_USAGE_RAW, usage.usageRaw());
        exchange.getAttributes().put(ATTR_PROMPT_TOKENS, usage.promptTokens());
        exchange.getAttributes().put(ATTR_COMPLETION_TOKENS, usage.completionTokens());
        exchange.getAttributes().put(ATTR_TOTAL_TOKENS, usage.totalTokens());
        exchange.getAttributes().put(ATTR_CACHED_TOKENS, usage.cachedTokens());
        exchange.getAttributes().put(ATTR_REASONING_TOKENS, usage.reasoningTokens());
        String requestId = exchange.getAttribute("requestId");
        log.info("[ProxyForwardFilter][{}] captureUsage: OK promptTokens={} completionTokens={} totalTokens={}",
            requestId, usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
    }

    private UsageStats extractUsage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        String trimmed = responseBody.trim();
        if (trimmed.startsWith("{")) {
            return extractUsageFromJson(trimmed);
        }
        return extractUsageFromSse(trimmed);
    }

    private UsageStats extractUsageFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode usageNode = root.get("usage");
            return usageNode == null || usageNode.isNull() ? null : toUsageStats(usageNode);
        } catch (Exception e) {
            return null;
        }
    }

    private UsageStats extractUsageFromSse(String sseBody) {
        UsageStats latest = null;
        String[] lines = sseBody.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String data = trimmed.substring("data:".length()).trim();
            if (data.isBlank() || "[DONE]".equals(data)) {
                continue;
            }
            UsageStats candidate = extractUsageFromJson(data);
            if (candidate != null) {
                latest = candidate;
            }
        }
        return latest;
    }

    private UsageStats toUsageStats(JsonNode usageNode) {
        try {
            String usageRaw = objectMapper.writeValueAsString(usageNode);
            long promptTokens = firstLong(usageNode, "prompt_tokens", "input_tokens");
            long completionTokens = firstLong(usageNode, "completion_tokens", "output_tokens");
            long totalTokens = firstLong(usageNode, "total_tokens");
            if (totalTokens == 0L) {
                totalTokens = promptTokens + completionTokens;
            }
            long cachedTokens = nestedLong(usageNode, "prompt_tokens_details", "cached_tokens")
                + nestedLong(usageNode, "input_tokens_details", "cached_tokens");
            long reasoningTokens = nestedLong(usageNode, "completion_tokens_details", "reasoning_tokens")
                + nestedLong(usageNode, "output_tokens_details", "reasoning_tokens");
            return new UsageStats(usageRaw, promptTokens, completionTokens, totalTokens, cachedTokens, reasoningTokens);
        } catch (Exception e) {
            return null;
        }
    }

    private long firstLong(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) {
                return Math.max(value.asLong(), 0L);
            }
        }
        return 0L;
    }

    private long nestedLong(JsonNode node, String objectName, String fieldName) {
        JsonNode objectNode = node.get(objectName);
        if (objectNode == null || !objectNode.isObject()) {
            return 0L;
        }
        JsonNode value = objectNode.get(fieldName);
        return value != null && value.isNumber() ? Math.max(value.asLong(), 0L) : 0L;
    }

    private BigDecimal resolveConfidence(RouteResult result) {
        if (result.getClassifierConfidence() != null) {
            return BigDecimal.valueOf(result.getClassifierConfidence());
        }
        return null;
    }

    private record UsageStats(String usageRaw,
                              long promptTokens,
                              long completionTokens,
                              long totalTokens,
                              long cachedTokens,
                              long reasoningTokens) {
    }

}
