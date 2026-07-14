package org.afo.gateway.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路由规则决策结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDecision {

    private boolean matched;
    private Long ruleId;
    private String ruleName;
    private String sourceModel;
    private String targetModel;
    private String effectiveTargetModel;
    private String actionType;
    private String executionMode;
    private String matchSummary;
    private boolean fallbackApplied;
    private String fallbackModel;
    private String fallbackReason;
    private String classificationResult;
    private Double classifierConfidence;

    public static RuleDecision noMatch(String sourceModel) {
        return RuleDecision.builder()
            .matched(false)
            .sourceModel(sourceModel)
            .effectiveTargetModel(sourceModel)
            .actionType("ORIGINAL_MODEL")
            .executionMode("ENFORCE")
            .matchSummary("no_match")
            .build();
    }
}
