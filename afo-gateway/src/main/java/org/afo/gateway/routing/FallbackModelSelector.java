package org.afo.gateway.routing;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 兜底模型选择器。
 */
@Component
public class FallbackModelSelector {

    private final ModelHealthResolver healthResolver;

    public FallbackModelSelector(ModelHealthResolver healthResolver) {
        this.healthResolver = healthResolver;
    }

    public String selectFirstHealthy(List<String> fallbackModels, Map<String, Boolean> modelHealth) {
        if (fallbackModels == null || fallbackModels.isEmpty()) {
            return null;
        }
        for (String model : fallbackModels) {
            if (model != null && !model.isBlank() && healthResolver.isHealthy(model, modelHealth)) {
                return model;
            }
        }
        return null;
    }
}
