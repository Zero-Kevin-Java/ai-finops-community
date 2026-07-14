package org.afo.strategy.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.utils.StringUtils;
import org.afo.strategy.domain.ModelAccessPolicy;

import java.util.Collections;
import java.util.List;

/**
 * 企业模型准入判定器。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Slf4j
public class ModelAccessValidator {

    /** 企业明确禁止模型。 */
    public static final String ENTERPRISE_MODEL_DENIED = "ENTERPRISE_MODEL_DENIED";

    /** 模型不在企业允许范围。 */
    public static final String ENTERPRISE_MODEL_NOT_ALLOWED = "ENTERPRISE_MODEL_NOT_ALLOWED";

    /** 默认模式拒绝未配置模型。 */
    public static final String ENTERPRISE_MODEL_UNLISTED_DENIED = "ENTERPRISE_MODEL_UNLISTED_DENIED";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判定请求模型是否允许进入下一层。
     *
     * @param policy 当前有效策略，为空时兼容放行
     * @param modelCode 请求模型编码
     * @return 准入判定结果
     */
    public ModelAccessDecision evaluate(ModelAccessPolicy policy, String modelCode) {
        if (policy == null || !"0".equals(policy.getStatus())) {
            return ModelAccessDecision.allowed();
        }
        List<String> deniedModels = parseModelCodes(policy.getDeniedModels());
        if (deniedModels.contains(modelCode)) {
            return ModelAccessDecision.denied(ENTERPRISE_MODEL_DENIED);
        }

        List<String> allowedModels = parseModelCodes(policy.getAllowedModels());
        if (!allowedModels.isEmpty()) {
            return allowedModels.contains(modelCode)
                ? ModelAccessDecision.allowed()
                : ModelAccessDecision.denied(ENTERPRISE_MODEL_NOT_ALLOWED);
        }

        if ("DENY_UNLISTED".equals(policy.getDefaultMode())) {
            return ModelAccessDecision.denied(ENTERPRISE_MODEL_UNLISTED_DENIED);
        }
        return ModelAccessDecision.allowed();
    }

    List<String> parseModelCodes(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        } catch (Exception e) {
            log.warn("[ModelAccess] Invalid model list JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
