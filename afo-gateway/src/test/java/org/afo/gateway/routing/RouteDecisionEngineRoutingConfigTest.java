package org.afo.gateway.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.afo.gateway.classifier.ClassifierClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("dev")
class RouteDecisionEngineRoutingConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void routingConfigRunsAfterApiKeyScopeWithoutEnterpriseModelAccessGate() {
        ModelAccessMatcher modelAccessMatcher = mock(ModelAccessMatcher.class);
        RoutingConfigMatcher routingConfigMatcher = mockRoutingMatcher("deepseek-chat");
        RouteDecisionEngine engine = new RouteDecisionEngine(
            modelAccessMatcher,
            new ApiKeyModelAccessValidator(),
            WebClient.builder().baseUrl("http://127.0.0.1:1").build(),
            mock(ClassifierClient.class),
            mock(ReactiveRedisTemplate.class),
            routingConfigMatcher);

        RouteResult result = engine.decideAfterPolicy(
            RuleMatchContext.builder()
                .tenantId("tenant-a")
                .apiKeyId("key-a")
                .sourceModel("gpt-4o")
                .request(request("gpt-4o", "summarize"))
                .build(),
            "gpt-4o",
            "gpt-4o,deepseek-chat",
            "STEADY",
            "summarize").block();

        assertEquals("deepseek-chat", result.getTargetModel());
        assertEquals("TARGET_MODEL", result.getActionType());
        verify(modelAccessMatcher, never()).evaluate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private RoutingConfigMatcher mockRoutingMatcher(String targetModel) {
        RoutingConfigClient client = tenantId -> RoutingConfigCache.builder()
            .present(true)
            .tenantId(tenantId)
            .rules(List.of(RoutingConfigRuleCache.ruleBuilder()
                .ruleId(1L)
                .ruleName("target")
                .priority(1)
                .status("0")
                .actionType("TARGET_MODEL")
                .executionMode("ENFORCE")
                .targetModel(targetModel)
                .keyword("summarize", "CONTAINS")
                .build()))
            .build();
        return new RoutingConfigMatcher(client, new FallbackModelSelector(new ModelHealthResolver()));
    }

    private ObjectNode request(String model, String prompt) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("prompt", prompt);
        return request;
    }
}
