package org.afo.strategy.service.impl;

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

    /** 是否允许进入下一层校验。 */
    private boolean allowed;

    /** 拒绝原因编码。 */
    private String denyReason;

    /** 允许结果。 */
    public static ModelAccessDecision allowed() {
        return new ModelAccessDecision(true, null);
    }

    /** 拒绝结果。 */
    public static ModelAccessDecision denied(String denyReason) {
        return new ModelAccessDecision(false, denyReason);
    }
}
