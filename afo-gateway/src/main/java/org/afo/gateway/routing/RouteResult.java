package org.afo.gateway.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路由决策结果（L0 开源版）
 *
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {

    /** 目标模型 */
    private String targetModel;

    /** 路由原因 */
    private RouteReason reason;

    /** 历史路由保护命中标记；模型准入拒绝使用 denyReason 承载。 */
    private boolean whitelistHit;

    /** 路由延迟（毫秒） */
    @Builder.Default
    private long decisionLatencyMs = 0;

    /** 拒绝原因（当 reason=DENIED 时） */
    private String denyReason;

    /** 拒绝策略 ID。 */
    private Long policyId;

    /** 拒绝层级。 */
    private String denyLayer;

    private Long ruleId;
    private String ruleName;
    private String sourceModel;
    private String actionType;
    private String matchSummary;
    private boolean fallbackApplied;
    private String fallbackModel;
    private String fallbackReason;
    private String classificationResult;
    private Double classifierConfidence;
    private String teamTag;
    private String path;

    public static RouteResult defaultToOriginal(String originalModel) {
        return defaultToOriginal(originalModel, 0);
    }

    public static RouteResult defaultToOriginal(String originalModel, long decisionLatencyMs) {
        return RouteResult.builder()
            .targetModel(originalModel)
            .reason(RouteReason.DEFAULT)
            .whitelistHit(false)
            .decisionLatencyMs(decisionLatencyMs)
            .build();
    }

    public static RouteResult fromRuleDecision(RuleDecision decision, RuleMatchContext context, long decisionLatencyMs) {
        boolean enforcedDeny = "DENY".equals(decision.getActionType());
        RouteReason reason = enforcedDeny ? RouteReason.DENIED : RouteReason.RULE_HIT;
        return RouteResult.builder()
            .targetModel(decision.getEffectiveTargetModel())
            .reason(reason)
            .whitelistHit(false)
            .decisionLatencyMs(decisionLatencyMs)
            .denyReason(enforcedDeny ? "ROUTING_CONFIG_DENY" : null)
            .denyLayer(enforcedDeny ? "ROUTING_CONFIG" : null)
            .ruleId(decision.getRuleId())
            .ruleName(decision.getRuleName())
            .sourceModel(decision.getSourceModel())
            .actionType(decision.getActionType())
            .matchSummary(decision.getMatchSummary())
            .fallbackApplied(decision.isFallbackApplied())
            .fallbackModel(decision.getFallbackModel())
            .fallbackReason(decision.getFallbackReason())
            .classificationResult(decision.getClassificationResult())
            .classifierConfidence(decision.getClassifierConfidence())
            .teamTag(context != null ? context.getTeamTag() : null)
            .path(context != null ? context.getPath() : null)
            .build();
    }

    public static RouteResult denied(String model, String denyReason, long decisionLatencyMs) {
        return denied(model, denyReason, null, null, decisionLatencyMs);
    }

    public static RouteResult denied(String model, String denyReason, Long policyId, String denyLayer, long decisionLatencyMs) {
        return RouteResult.builder()
            .targetModel(model)
            .reason(RouteReason.DENIED)
            .whitelistHit(false)
            .denyReason(denyReason)
            .policyId(policyId)
            .denyLayer(denyLayer)
            .decisionLatencyMs(decisionLatencyMs)
            .build();
    }

    public static RouteResult aiDecision(String originalModel, String targetModel, long latencyMs) {
        return RouteResult.builder()
            .targetModel(targetModel)
            .reason(RouteReason.AI_DECISION)
            .whitelistHit(false)
            .decisionLatencyMs(latencyMs)
            .build();
    }

    public enum RouteReason {
        /** 默认原模型 */
        DEFAULT,
        /** 历史白名单命中。 */
        WHITELIST_HIT,
        /** 手动规则命中（L1） */
        RULE_HIT,
        /** AI 分类器决策 */
        AI_DECISION,
        /** 模型策略拒绝（准入控制） */
        DENIED
    }

}
