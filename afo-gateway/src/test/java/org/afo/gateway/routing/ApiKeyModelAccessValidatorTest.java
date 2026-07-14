package org.afo.gateway.routing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
public class ApiKeyModelAccessValidatorTest {

    private final ApiKeyModelAccessValidator validator = new ApiKeyModelAccessValidator();

    @Test
    void allowsModelIncludedInCommaSeparatedScope() {
        ModelAccessDecision decision = validator.evaluate("gpt-4o,gpt-4o-mini", "gpt-4o-mini");

        assertTrue(decision.isAllowed());
    }

    @Test
    void deniesModelMissingFromScope() {
        ModelAccessDecision decision = validator.evaluate("gpt-4o,gpt-4o-mini", "deepseek-chat");

        assertFalse(decision.isAllowed());
    }

    @Test
    void allowsAnyModelWhenScopeContainsWildcard() {
        ModelAccessDecision decision = validator.evaluate("*", "deepseek-chat");

        assertTrue(decision.isAllowed());
    }

    @Test
    void supportsJsonArrayScopeForCompatibility() {
        ModelAccessDecision decision = validator.evaluate("[\"gpt-4o\",\"deepseek-chat\"]", "deepseek-chat");

        assertTrue(decision.isAllowed());
    }

    @Test
    void deniesInvalidJsonArrayScopeWithInvalidReason() {
        ModelAccessDecision decision = validator.evaluate("[\"gpt-4o\"", "gpt-4o");

        assertFalse(decision.isAllowed());
        assertEquals("API_KEY_MODEL_SCOPE_INVALID", decision.getDenyReason());
    }

    @Test
    void deniesEmptyScopeWithEmptyReason() {
        ModelAccessDecision decision = validator.evaluate(" ", "gpt-4o");

        assertFalse(decision.isAllowed());
        assertEquals("API_KEY_MODEL_SCOPE_EMPTY", decision.getDenyReason());
    }
}
