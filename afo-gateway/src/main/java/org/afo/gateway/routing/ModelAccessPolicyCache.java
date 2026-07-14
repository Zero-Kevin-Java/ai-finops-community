package org.afo.gateway.routing;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业模型准入策略缓存 DTO。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
public class ModelAccessPolicyCache {

    /** 策略 ID。 */
    private Long policyId;

    /** 租户 ID。 */
    private String tenantId;

    /** 默认准入模式。 */
    private String defaultMode = "ALLOW_UNLISTED";

    /** 允许模型编码。 */
    private List<String> allowedModels = new ArrayList<>();

    /** 禁止模型编码。 */
    private List<String> deniedModels = new ArrayList<>();

    /** 策略状态。 */
    private String status;

    /** 是否存在有效策略。 */
    private boolean present;

    /** 空策略：控制面没有配置时兼容放行。 */
    public static ModelAccessPolicyCache empty() {
        ModelAccessPolicyCache cache = new ModelAccessPolicyCache();
        cache.setPresent(false);
        return cache;
    }
}
