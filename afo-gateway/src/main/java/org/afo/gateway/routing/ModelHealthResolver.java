package org.afo.gateway.routing;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模型健康状态解析器。
 *
 * <p>第一阶段仅使用规则内携带的健康快照；未配置时默认健康。</p>
 */
@Component
public class ModelHealthResolver {

    public boolean isHealthy(String model, Map<String, Boolean> modelHealth) {
        if (model == null || model.isBlank()) {
            return false;
        }
        if (modelHealth == null || modelHealth.isEmpty()) {
            return true;
        }
        return modelHealth.getOrDefault(model, true);
    }
}
