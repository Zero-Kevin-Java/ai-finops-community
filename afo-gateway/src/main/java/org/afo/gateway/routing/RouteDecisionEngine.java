package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.rabbitmq.config.RequestLogQueueConfig;
import org.afo.common.rabbitmq.message.RouteDecisionLogMessage;
import org.afo.common.rabbitmq.utils.RabbitMqUtils;
import org.afo.gateway.classifier.ClassifierClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * 路由决策引擎（L0 开源版）。
 *
 * Layer 1: 企业模型准入 + API Key 范围 + 项目级策略。
 * Layer 2: routing-config 命中优先；未命中时走分类器路由。
 *
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Component
public class RouteDecisionEngine {

    private static final String DENY_LAYER_ENTERPRISE_MODEL_ACCESS = "ENTERPRISE_MODEL_ACCESS";
    private static final String DENY_LAYER_API_KEY_MODEL_ACCESS = "API_KEY_MODEL_ACCESS";
    private static final double CLASSIFIER_CONFIDENCE_THRESHOLD = 0.85;

    private final ModelAccessMatcher modelAccessMatcher;
    private final ApiKeyModelAccessValidator apiKeyModelAccessValidator;
    private final WebClient adminWebClient;
    private final ClassifierClient classifierClient;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RoutingConfigMatcher routingConfigMatcher;

    @Value("${afo.gateway.admin.internal-token:}")
    private String internalToken;

    @Autowired
    public RouteDecisionEngine(ModelAccessMatcher modelAccessMatcher,
                               ApiKeyModelAccessValidator apiKeyModelAccessValidator,
                               WebClient adminWebClient,
                               ClassifierClient classifierClient,
                               ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                               RoutingConfigMatcher routingConfigMatcher) {
        this.modelAccessMatcher = modelAccessMatcher;
        this.apiKeyModelAccessValidator = apiKeyModelAccessValidator;
        this.adminWebClient = adminWebClient;
        this.classifierClient = classifierClient;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.routingConfigMatcher = routingConfigMatcher;
        log.debug("[RouteDecisionEngine] CONSTRUCTED: cc={}, redis={}, routingMatcher={}",
            this.classifierClient != null, this.reactiveRedisTemplate != null,
            this.routingConfigMatcher != null);
    }

    public RouteResult decide(String tenantId, String model, String tenantMode,
                              String projectId, String clientId) {
        return decideWithRequestId(tenantId, model, tenantMode, projectId, clientId, null, null);
    }

    public RouteResult decideWithRequestId(String tenantId, String model, String tenantMode,
                                           String projectId, String clientId, String requestId,
                                           String apiKeyId) {
        return decideWithRequestId(tenantId, model, tenantMode, projectId, clientId, requestId, apiKeyId, null);
    }

    public RouteResult decideWithRequestId(String tenantId, String model, String tenantMode,
                                           String projectId, String clientId, String requestId,
                                           String apiKeyId, String keyScope) {
        return decideWithRequestId(tenantId, model, tenantMode, projectId, clientId, requestId, apiKeyId, keyScope, null)
            .block();
    }

    public Mono<RouteResult> decideWithRequestId(String tenantId, String model, String tenantMode,
                                                  String projectId, String clientId, String requestId,
                                                  String apiKeyId, String keyScope,
                                                  String prompt) {
        RuleMatchContext context = RuleMatchContext.builder()
            .tenantId(tenantId)
            .requestId(requestId)
            .apiKeyId(apiKeyId)
            .appId(clientId)
            .sourceModel(model)
            .build();
        return decideWithRequestId(context, tenantMode, projectId, clientId, requestId, apiKeyId, keyScope, prompt);
    }

    public Mono<RouteResult> decideWithRequestId(RuleMatchContext context, String tenantMode,
                                                  String projectId, String clientId, String requestId,
                                                  String apiKeyId, String keyScope,
                                                  String prompt) {
        final long startTime = System.currentTimeMillis();
        RuleMatchContext safeContext = context != null ? context : new RuleMatchContext();
        String tenantId = safeContext.getTenantId();
        String model = safeContext.getSourceModel();

        ModelAccessDecision modelAccessDecision = modelAccessMatcher.evaluate(tenantId, model);
        if (!modelAccessDecision.isAllowed()) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("[RouteDecisionEngine] Model {} denied by enterprise model access: tenantId={}, reason={}",
                model, tenantId, modelAccessDecision.getDenyReason());
            RouteResult result = RouteResult.denied(
                model,
                modelAccessDecision.getDenyReason(),
                modelAccessDecision.getPolicyId(),
                DENY_LAYER_ENTERPRISE_MODEL_ACCESS,
                latency);
            sendRouteDecisionLog(requestId, tenantId, model, result, apiKeyId);
            return Mono.just(result);
        }

        ModelAccessDecision apiKeyDecision = apiKeyModelAccessValidator.evaluate(keyScope, model);
        if (!apiKeyDecision.isAllowed()) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("[RouteDecisionEngine] Model {} denied by API Key scope: tenantId={}, apiKeyId={}, reason={}",
                model, tenantId, apiKeyId, apiKeyDecision.getDenyReason());
            RouteResult result = RouteResult.denied(
                model,
                apiKeyDecision.getDenyReason(),
                null,
                DENY_LAYER_API_KEY_MODEL_ACCESS,
                latency);
            sendRouteDecisionLog(requestId, tenantId, model, result, apiKeyId);
            return Mono.just(result);
        }

        return routeAfterPolicyOrMode(
            safeContext, tenantMode, model, requestId, tenantId, apiKeyId, startTime, prompt);
    }

    private Mono<RouteResult> routeAfterPolicyOrMode(RuleMatchContext context,
                                                     String tenantMode,
                                                     String model,
                                                     String requestId,
                                                     String tenantId,
                                                     String apiKeyId,
                                                     long startTime,
                                                     String prompt) {
        RouteResult routingResult = routeAfterPolicy(context, model, startTime);
        if (routingResult.getReason() != RouteResult.RouteReason.DEFAULT) {
            sendRouteDecisionLog(requestId, tenantId, model, routingResult, apiKeyId);
            return Mono.just(routingResult);
        }
        return executeAiRouting(tenantId, model, requestId, apiKeyId, startTime, prompt)
            .switchIfEmpty(Mono.just(routingResult));
    }

    private RouteResult routeAfterPolicy(RuleMatchContext context, String model, long startTime) {
        long latency = System.currentTimeMillis() - startTime;
        if (routingConfigMatcher == null || context == null) {
            log.debug("[RouteDecisionEngine] Routing config matcher unavailable, default route: model={}", model);
            RouteResult result = RouteResult.defaultToOriginal(model, latency);
            result.setSourceModel(model);
            result.setActionType("ORIGINAL_MODEL");
            return result;
        }
        RuleDecision decision = routingConfigMatcher.evaluate(context.getTenantId(), context);
        if (!decision.isMatched()) {
            log.debug("[RouteDecisionEngine] No routing config matched: tenantId={}, model={}", context.getTenantId(), model);
            RouteResult result = RouteResult.defaultToOriginal(model, latency);
            result.setSourceModel(model);
            result.setActionType("ORIGINAL_MODEL");
            result.setMatchSummary(decision.getMatchSummary());
            result.setTeamTag(context.getTeamTag());
            result.setPath(context.getPath());
            return result;
        }
        RouteResult result = RouteResult.fromRuleDecision(decision, context, latency);
        return result;
    }

    public Mono<RouteResult> decideAfterPolicy(RuleMatchContext context, String model, String keyScope,
                                          String tenantMode, String prompt) {
        final long startTime = System.currentTimeMillis();
        String tenantId = context != null ? context.getTenantId() : null;
        String requestId = context != null ? context.getRequestId() : null;
        String apiKeyId = context != null ? context.getApiKeyId() : null;
        ModelAccessDecision apiKeyDecision = apiKeyModelAccessValidator.evaluate(keyScope, model);
        if (!apiKeyDecision.isAllowed()) {
            long latency = System.currentTimeMillis() - startTime;
            RouteResult result = RouteResult.denied(
                model,
                apiKeyDecision.getDenyReason(),
                null,
                DENY_LAYER_API_KEY_MODEL_ACCESS,
                latency);
            sendRouteDecisionLog(requestId, tenantId, model, result, apiKeyId);
            return Mono.just(result);
        }
        RouteResult routingResult = routeAfterPolicy(context, model, startTime);
        if (routingResult.getReason() != RouteResult.RouteReason.DEFAULT) {
            sendRouteDecisionLog(requestId, tenantId, model, routingResult, apiKeyId);
            return Mono.just(routingResult);
        }
        return executeAiRouting(tenantId, model, requestId, apiKeyId, startTime, prompt);
    }

    private Mono<RouteResult> executeAiRouting(String tenantId, String model,
                                                String requestId, String apiKeyId,
                                                long startTime, String prompt) {
        return classifierClient.classify(prompt, tenantId)
            .flatMap(classifyResult -> {
                long latency = System.currentTimeMillis() - startTime;
                boolean isSimple = classifyResult.isSimple()
                    && classifyResult.getConfidence() >= CLASSIFIER_CONFIDENCE_THRESHOLD;

                log.debug("[RouteDecisionEngine] AI classify: isSimple={}, confidence={}, model={}",
                    classifyResult.isSimple(), classifyResult.getConfidence(), model);

                if (!isSimple) {
                    log.debug("[RouteDecisionEngine] AI routing: complex task, default to original model={}", model);
                    RouteResult result = RouteResult.defaultToOriginal(model, latency);
                    result.setSourceModel(model);
                    result.setClassificationResult(Boolean.toString(classifyResult.isSimple()));
                    result.setClassifierConfidence(classifyResult.getConfidence());
                    sendRouteDecisionLog(requestId, tenantId, model, result, apiKeyId);
                    return Mono.just(result);
                }

                return resolveTargetModel(tenantId, model)
                    .map(targetModel -> {
                        log.debug("[RouteDecisionEngine] AI routing: simple task, {} -> {}, confidence={}",
                            model, targetModel, classifyResult.getConfidence());
                        RouteResult result = RouteResult.aiDecision(model, targetModel, latency);
                        result.setSourceModel(model);
                        result.setClassificationResult(Boolean.toString(classifyResult.isSimple()));
                        result.setClassifierConfidence(classifyResult.getConfidence());
                        sendRouteDecisionLog(requestId, tenantId, model, result, apiKeyId);
                        return result;
                    });
            });
    }

    private Mono<String> resolveTargetModel(String tenantId, String originalModel) {
        if (tenantId == null || originalModel == null || reactiveRedisTemplate == null) {
            return Mono.justOrEmpty(originalModel);
        }
        String redisKey = "gateway:simple-route:" + tenantId;

        return reactiveRedisTemplate.opsForHash()
            .get(redisKey, originalModel)
            .map(obj -> (String) obj)
            .switchIfEmpty(Mono.defer(() -> fallbackTargetModel(tenantId, originalModel)));
    }

    private Mono<String> fallbackTargetModel(String tenantId, String originalModel) {
        return adminWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/simple-route/tenant/{tenantId}/model")
                .queryParam("model", originalModel)
                .build(tenantId))
            .header("X-Internal-Token", internalToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(jsonNode -> {
                JsonNode dataNode = jsonNode.get("data");
                String target = (dataNode != null && !dataNode.isNull()) ? dataNode.asText() : originalModel;
                if (!originalModel.equals(target)) {
                    log.debug("[RouteDecisionEngine] Fallback resolved: {} -> {} (not cached)",
                        originalModel, target);
                }
                return target;
            })
            .onErrorResume(e -> {
                log.warn("[RouteDecisionEngine] Fallback failed for {}/{}, use original: {}",
                    tenantId, originalModel, e.getMessage());
                return Mono.just(originalModel);
            });
    }

    public RouteResult decide(String tenantId, String model, String tenantMode) {
        return decide(tenantId, model, tenantMode, null, null);
    }

    private void sendRouteDecisionLog(String requestId, String tenantId, String model, RouteResult result,
                                       String apiKeyId) {
        try {
            RouteDecisionLogMessage message = new RouteDecisionLogMessage();
            message.setRequestId(requestId);
            message.setTenantId(tenantId);
            message.setModel(model);
            message.setTargetModel(result.getTargetModel());
            message.setRouteReason(result.getReason() != null ? result.getReason().name() : "UNKNOWN");
            message.setWhitelistHit(result.isWhitelistHit());
            message.setDecisionLatencyMs(result.getDecisionLatencyMs());
            message.setDenyReason(result.getDenyReason());
            message.setPolicyId(result.getPolicyId());
            message.setDenyLayer(result.getDenyLayer());
            message.setApiKeyId(apiKeyId);
            message.setRuleId(result.getRuleId());
            message.setRuleName(result.getRuleName());
            message.setSourceModel(result.getSourceModel());
            message.setActionType(result.getActionType());
            message.setMatchSummary(result.getMatchSummary());
            message.setFallbackApplied(result.isFallbackApplied());
            message.setFallbackModel(result.getFallbackModel());
            message.setFallbackReason(result.getFallbackReason());
            message.setClassificationResult(result.getClassificationResult());
            message.setClassifierConfidence(result.getClassifierConfidence());
            message.setTeamTag(result.getTeamTag());
            message.setPath(result.getPath());
            message.setTimestamp(Instant.now());

            RabbitMqUtils.convertAndSend(
                RequestLogQueueConfig.EXCHANGE_NAME,
                RequestLogQueueConfig.ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            log.warn("[RouteDecisionEngine] Failed to send route decision log: {}", e.getMessage());
        }
    }
}
