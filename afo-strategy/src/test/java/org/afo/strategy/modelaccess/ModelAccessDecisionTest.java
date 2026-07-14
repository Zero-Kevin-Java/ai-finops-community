package org.afo.strategy.modelaccess;

import org.afo.strategy.domain.ModelAccessPolicy;
import org.afo.strategy.service.impl.ModelAccessDecision;
import org.afo.strategy.service.impl.ModelAccessValidator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ModelAccessDecisionTest {

    private final ModelAccessValidator validator = new ModelAccessValidator();

    @Test
    void permitsRequestWhenNoActivePolicyExists() {
        ModelAccessDecision decision = validator.evaluate(null, "gpt-4o");

        assertTrue(decision.isAllowed());
    }

    @Test
    void deniesModelWhenItIsExplicitlyDenied() {
        ModelAccessPolicy policy = activePolicy("ALLOW_UNLISTED", "[\"gpt-4o\"]", "[\"gpt-4o-mini\"]");

        ModelAccessDecision decision = validator.evaluate(policy, "gpt-4o-mini");

        assertEquals("ENTERPRISE_MODEL_DENIED", decision.getDenyReason());
    }

    @Test
    void deniesModelOutsideNonEmptyAllowedList() {
        ModelAccessPolicy policy = activePolicy("ALLOW_UNLISTED", "[\"gpt-4o\"]", "[]");

        ModelAccessDecision decision = validator.evaluate(policy, "deepseek-chat");

        assertEquals("ENTERPRISE_MODEL_NOT_ALLOWED", decision.getDenyReason());
    }

    @Test
    void deniesUnlistedModelWhenDefaultModeIsDenyUnlisted() {
        ModelAccessPolicy policy = activePolicy("DENY_UNLISTED", "[]", "[]");

        ModelAccessDecision decision = validator.evaluate(policy, "deepseek-chat");

        assertEquals("ENTERPRISE_MODEL_UNLISTED_DENIED", decision.getDenyReason());
    }

    private ModelAccessPolicy activePolicy(String defaultMode, String allowedModels, String deniedModels) {
        ModelAccessPolicy policy = new ModelAccessPolicy();
        policy.setStatus("0");
        policy.setDefaultMode(defaultMode);
        policy.setAllowedModels(allowedModels);
        policy.setDeniedModels(deniedModels);
        return policy;
    }
}
