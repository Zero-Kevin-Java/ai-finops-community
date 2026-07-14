package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class RoutingConfigMatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModelHealthResolver healthResolver = new ModelHealthResolver();
    private final FallbackModelSelector fallbackSelector = new FallbackModelSelector(healthResolver);

    @Test
    void keywordRuleCanKeepOriginalModelWithoutReturningPromptText() {
        RoutingConfigMatcher matcher = matcherWith(rule(10, "r-original", "ORIGINAL_MODEL", null)
            .keyword("contract", "CONTAINS")
            .build());
        RuleMatchContext context = context("gpt-4o", "Please review this contract before signing");

        RuleDecision decision = matcher.evaluate("tenant-a", context);

        assertTrue(decision.isMatched());
        assertEquals("ORIGINAL_MODEL", decision.getActionType());
        assertEquals("gpt-4o", decision.getEffectiveTargetModel());
        assertTrue(decision.getMatchSummary().contains("keywords=1"));
        assertFalse(decision.getMatchSummary().contains("Please review this contract"));
    }

    @Test
    void keywordRuleCanRouteToTargetModel() {
        RoutingConfigMatcher matcher = matcherWith(rule(10, "r-target", "TARGET_MODEL", "deepseek-chat")
            .keyword("summarize", "CONTAINS")
            .build());

        RuleDecision decision = matcher.evaluate("tenant-a", context("gpt-4o", "summarize the meeting"));

        assertTrue(decision.isMatched());
        assertEquals("TARGET_MODEL", decision.getActionType());
        assertEquals("deepseek-chat", decision.getEffectiveTargetModel());
    }

    @Test
    void lowerPriorityNumberWinsFirstMatch() {
        RoutingConfigMatcher matcher = matcherWith(
            rule(20, "r-slow", "TARGET_MODEL", "slow-model").keyword("invoice", "CONTAINS").build(),
            rule(5, "r-fast", "TARGET_MODEL", "fast-model").keyword("invoice", "CONTAINS").build());

        RuleDecision decision = matcher.evaluate("tenant-a", context("gpt-4o", "invoice analysis"));

        assertTrue(decision.isMatched());
        assertEquals("r-fast", decision.getRuleName());
        assertEquals("fast-model", decision.getEffectiveTargetModel());
    }

    @Test
    void unhealthyTargetFallsBackToFirstHealthyFallbackModel() {
        RoutingConfigRuleCache rule = rule(10, "r-fallback", "TARGET_MODEL", "primary-model")
            .keyword("fallback", "CONTAINS")
            .fallbackModels("backup-one", "backup-two")
            .modelHealth(Map.of("primary-model", false, "backup-one", false, "backup-two", true))
            .build();
        RoutingConfigMatcher matcher = matcherWith(rule);

        RuleDecision decision = matcher.evaluate("tenant-a", context("gpt-4o", "fallback please"));

        assertTrue(decision.isFallbackApplied());
        assertEquals("backup-two", decision.getFallbackModel());
        assertEquals("backup-two", decision.getEffectiveTargetModel());
        assertEquals("TARGET_UNHEALTHY", decision.getFallbackReason());
    }

    @Test
    void allLogicMatchesDepartmentUserAndAppConditions() {
        RoutingConfigMatcher matcher = matcherWith(rule(10, "r-owner", "TARGET_MODEL", "deepseek-chat")
            .departments("20")
            .userIds("30")
            .appIds("40")
            .build());

        RuleMatchContext context = context("gpt-4o", "normal request");
        context.setDepartment("20");
        context.setUserId("30");
        context.setAppId("40");

        RuleDecision decision = matcher.evaluate("tenant-a", context);

        assertTrue(decision.isMatched());
        assertEquals("deepseek-chat", decision.getEffectiveTargetModel());
        assertTrue(decision.getMatchSummary().contains("departments=1"));
        assertTrue(decision.getMatchSummary().contains("userIds=1"));
        assertTrue(decision.getMatchSummary().contains("appIds=1"));
    }

    @Test
    void anyLogicMatchesWhenOneConfiguredConditionMatches() {
        RoutingConfigMatcher matcher = matcherWith(rule(10, "r-any", "TARGET_MODEL", "deepseek-chat")
            .logic("ANY")
            .departments("not-this-dept")
            .appIds("40")
            .build());

        RuleMatchContext context = context("gpt-4o", "normal request");
        context.setDepartment("20");
        context.setAppId("40");

        RuleDecision decision = matcher.evaluate("tenant-a", context);

        assertTrue(decision.isMatched());
        assertEquals("deepseek-chat", decision.getEffectiveTargetModel());
    }

    private RoutingConfigMatcher matcherWith(RoutingConfigRuleCache... rules) {
        RoutingConfigCache cache = RoutingConfigCache.builder()
            .present(true)
            .tenantId("tenant-a")
            .rules(List.of(rules))
            .build();
        RoutingConfigClient client = tenantId -> cache;
        return new RoutingConfigMatcher(client, fallbackSelector);
    }

    private RuleMatchContext context(String sourceModel, String prompt) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", sourceModel);
        request.put("prompt", prompt);
        return RuleMatchContext.builder()
            .tenantId("tenant-a")
            .apiKeyId("key-a")
            .path("/v1/chat/completions")
            .sourceModel(sourceModel)
            .modelType("chat")
            .headers(Map.of("x-env", "test"))
            .request(request)
            .build();
    }

    private RoutingConfigRuleCache.RuleBuilder rule(int priority, String name, String actionType, String targetModel) {
        return RoutingConfigRuleCache.ruleBuilder()
            .ruleId((long) priority)
            .ruleName(name)
            .priority(priority)
            .status("0")
            .actionType(actionType)
            .executionMode("ENFORCE")
            .targetModel(targetModel);
    }
}
