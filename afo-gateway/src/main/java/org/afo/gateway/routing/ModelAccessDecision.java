package org.afo.gateway.routing;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 企业模型准入判定结果。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
@AllArgsConstructor
public class ModelAccessDecision {

    /** 是否允许进入下一层。 */
    private boolean allowed;

    /** 拒绝原因编码。 */
    private String denyReason;

    /** 命中的企业模型准入策略 ID。 */
    private Long policyId;

    public static ModelAccessDecision allowed() {
        return new ModelAccessDecision(true, null, null);
    }

    public static ModelAccessDecision denied(String denyReason, Long policyId) {
        return new ModelAccessDecision(false, denyReason, policyId);
    }
}
