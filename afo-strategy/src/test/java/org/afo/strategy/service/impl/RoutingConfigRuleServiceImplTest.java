package org.afo.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.afo.strategy.domain.RoutingConfigRule;
import org.afo.strategy.domain.bo.RoutingConfigSimulateBo;
import org.afo.strategy.domain.vo.RoutingConfigSimulationVo;
import org.afo.strategy.domain.vo.RoutingConfigRuleVo;
import org.afo.strategy.mapper.RoutingConfigRuleMapper;
import org.afo.strategy.support.ModelCatalogLookup;
import org.afo.strategy.support.PolicyJsonCodec;
import org.afo.strategy.support.StrategyTenantResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class RoutingConfigRuleServiceImplTest {

    private final RoutingConfigRuleMapper ruleMapper = mock(RoutingConfigRuleMapper.class);
    private final LlmModelCatalogMapper modelCatalogMapper = mock(LlmModelCatalogMapper.class);
    private final RoutingConfigRuleServiceImpl service = new RoutingConfigRuleServiceImpl(
        ruleMapper,
        new ModelCatalogLookup(modelCatalogMapper),
        new StrategyTenantResolver(),
        new PolicyJsonCodec(new ObjectMapper())
    );

    @Test
    void rejectsTargetModelRuleWithoutTargetModel() {
        RoutingConfigRule rule = validTargetRule();
        rule.setActionConfig("{}");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insert(rule));

        assertTrue(ex.getMessage().contains("targetModel"));
    }

    @Test
    void updatesStatusToDisabled() {
        RoutingConfigRule rule = validTargetRule();
        rule.setRuleId(1L);
        when(ruleMapper.selectById(1L)).thenReturn(rule);

        service.updateStatus(1L, "1");

        assertEquals("1", rule.getStatus());
        verify(ruleMapper).updateById(rule);
    }

    @Test
    void rejectsInvalidStatusUpdate() {
        RoutingConfigRule rule = validTargetRule();
        rule.setRuleId(1L);
        when(ruleMapper.selectById(1L)).thenReturn(rule);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateStatus(1L, "invalid"));

        assertTrue(ex.getMessage().contains("状态"));
    }

    @Test
    void rejectsStatusUpdateWithoutRuleId() {
        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateStatus(null, "1"));

        assertTrue(ex.getMessage().contains("规则 ID"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryPageListSupportsExtendedSearchParams() {
        RoutingConfigRule query = new RoutingConfigRule();
        query.setTenantId("tenant-a");
        query.setRuleName("contract");
        query.setActionType("TARGET_MODEL");
        query.setExecutionMode("ENFORCE");
        query.setStatus("0");
        query.getParams().put("matchLogic", "ANY");
        query.getParams().put("fallbackMode", "TARGET_MODEL");
        query.getParams().put("beginTime", "2026-05-01 00:00:00");
        query.getParams().put("endTime", "2026-05-31 23:59:59");
        when(ruleMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>(1, 10));

        service.queryPageList(query, new PageQuery(10, 1));

        verify(ruleMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStatsAggregatesRoutingRuleData() {
        RoutingConfigRule fallbackRule = validTargetRule();
        fallbackRule.setHitCount(12L);
        fallbackRule.setFallbackConfig("{\"fallbackModels\":[\"gpt-4o-mini\"],\"onTargetUnavailable\":\"TARGET_MODEL\"}");
        RoutingConfigRule yesterdayRule = validTargetRule();
        yesterdayRule.setHitCount(10L);
        when(ruleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 3L, 1L);
        when(ruleMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(fallbackRule), List.of(yesterdayRule));

        var stats = service.getStats("tenant-a");

        assertEquals(5L, stats.getTotalRules());
        assertEquals(1L, stats.getTodayNewRules());
        assertEquals(3L, stats.getEnabledRules());
        assertEquals(60D, stats.getEnabledRate());
        assertEquals(12L, stats.getTodayHitCount());
        assertEquals(50D, stats.getTodayHitGrowthRate());
        assertEquals(12L, stats.getFallbackHitCount());
        verify(ruleMapper, times(2)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void rejectsInvalidMatchJsonArray() {
        RoutingConfigRule rule = validTargetRule();
        rule.setMatchConfig("[]");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insert(rule));

        assertTrue(ex.getMessage().contains("match_config"));
    }

    @Test
    void rejectsUncompilablePathRegex() {
        RoutingConfigRule rule = validTargetRule();
        rule.setMatchConfig("""
            {
              "paths": [
                { "type": "REGEX", "value": "[" }
              ]
            }
            """);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insert(rule));

        assertTrue(ex.getMessage().contains("正则"));
    }

    @Test
    void rejectsBlankFallbackModelEntry() {
        RoutingConfigRule rule = validTargetRule();
        rule.setFallbackConfig("""
            {
              "fallbackModels": ["gpt-4o-mini", ""]
            }
            """);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insert(rule));

        assertTrue(ex.getMessage().contains("fallbackModels"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void activeRulesUseTenantEffectiveWindowAndPriorityOrder() {
        RoutingConfigRuleVo highPriority = ruleVo(2L, "high", 10);
        RoutingConfigRuleVo lowPriority = ruleVo(1L, "low", 100);
        when(ruleMapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(highPriority, lowPriority));

        List<RoutingConfigRuleVo> result = service.getActiveRules("tenant-a");

        assertEquals(List.of(2L, 1L), result.stream().map(RoutingConfigRuleVo::getRuleId).toList());
        verify(ruleMapper).selectVoList(any(LambdaQueryWrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulateMatchesFirstActiveRuleWithoutIncrementingHitCount() {
        RoutingConfigRuleVo directLegalRule = ruleVo(10L, "legal-direct", 1);
        directLegalRule.setMatchConfig("""
            {
              "paths": [
                { "type": "PREFIX", "value": "/v1/chat" }
              ],
              "keywords": [
                { "field": "prompt", "type": "CONTAINS", "value": "contract" }
              ]
            }
            """);
        directLegalRule.setActionType("TARGET_MODEL");
        directLegalRule.setActionConfig("{\"targetModel\":\"deepseek-chat\"}");
        directLegalRule.setFallbackConfig("{\"fallbackModels\":[\"gpt-4o-mini\"]}");
        when(ruleMapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(directLegalRule));

        RoutingConfigSimulateBo request = new RoutingConfigSimulateBo();
        request.setTenantId("tenant-a");
        request.setPath("/v1/chat/completions");
        request.setSourceModel("gpt-4o");
        request.setPrompt("review this contract");
        request.setHeaders(Map.of("X-Trace", "demo"));

        RoutingConfigSimulationVo result = service.simulate(request);

        assertTrue(result.isMatched());
        assertEquals(10L, result.getRuleId());
        assertEquals("legal-direct", result.getRuleName());
        assertEquals("TARGET_MODEL", result.getActionType());
        assertEquals("gpt-4o", result.getSourceModel());
        assertEquals("deepseek-chat", result.getTargetModel());
        assertEquals("ENFORCE", result.getExecutionMode());
        assertEquals(List.of("gpt-4o-mini"), result.getFallbackModels());
        assertFalse(result.isFallbackApplied());
        verify(ruleMapper).selectVoList(any(LambdaQueryWrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulateSupportsAnyLogicAcrossDepartmentUserAndAppConditions() {
        RoutingConfigRuleVo rule = ruleVo(11L, "owner-any", 1);
        rule.setMatchConfig("""
            {
              "logic": "ANY",
              "departments": ["20"],
              "userIds": ["30"],
              "appIds": ["40"]
            }
            """);
        rule.setActionType("TARGET_MODEL");
        rule.setActionConfig("{\"targetModel\":\"deepseek-chat\"}");
        when(ruleMapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

        RoutingConfigSimulateBo request = new RoutingConfigSimulateBo();
        request.setTenantId("tenant-a");
        request.setDepartment("not-this-dept");
        request.setUserId("not-this-user");
        request.setAppId("40");
        request.setSourceModel("gpt-4o");

        RoutingConfigSimulationVo result = service.simulate(request);

        assertTrue(result.isMatched());
        assertEquals(11L, result.getRuleId());
        assertEquals("deepseek-chat", result.getTargetModel());
        assertTrue(result.getMatchSummary().contains("appId=40"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulateSupportsMultiSelectRequestContext() {
        RoutingConfigRuleVo rule = ruleVo(12L, "multi-context", 1);
        rule.setMatchConfig("""
            {
              "logic": "ALL",
              "departments": ["20"],
              "apiKeyIds": ["key-a"],
              "sourceModels": ["deepseek-chat"]
            }
            """);
        rule.setActionType("TARGET_MODEL");
        rule.setActionConfig("{\"targetModel\":\"gpt-4o-mini\"}");
        when(ruleMapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

        RoutingConfigSimulateBo request = new RoutingConfigSimulateBo();
        request.setTenantId("tenant-a");
        request.setDepartments(List.of("10", "20"));
        request.setApiKeyIds(List.of("key-b", "key-a"));
        request.setSourceModels(List.of("gpt-4o", "deepseek-chat"));
        request.setSourceModel("gpt-4o");

        RoutingConfigSimulationVo result = service.simulate(request);

        assertTrue(result.isMatched());
        assertEquals(12L, result.getRuleId());
        assertEquals("gpt-4o-mini", result.getTargetModel());
        assertTrue(result.getMatchSummary().contains("department=20"));
        assertTrue(result.getMatchSummary().contains("apiKeyId=key-a"));
        assertTrue(result.getMatchSummary().contains("sourceModel=deepseek-chat"));
    }

    private RoutingConfigRule validTargetRule() {
        RoutingConfigRule rule = new RoutingConfigRule();
        rule.setTenantId("tenant-a");
        rule.setRuleName("legal to target model");
        rule.setPriority(10);
        rule.setMatchConfig("{}");
        rule.setActionType("TARGET_MODEL");
        rule.setActionConfig("{\"targetModel\":\"deepseek-chat\"}");
        rule.setFallbackConfig("{}");
        rule.setExecutionMode("ENFORCE");
        rule.setStatus("0");
        when(modelCatalogMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        return rule;
    }

    private RoutingConfigRuleVo ruleVo(Long ruleId, String ruleName, Integer priority) {
        RoutingConfigRuleVo rule = new RoutingConfigRuleVo();
        rule.setRuleId(ruleId);
        rule.setTenantId("tenant-a");
        rule.setRuleName(ruleName);
        rule.setPriority(priority);
        rule.setMatchConfig("{}");
        rule.setActionType("ORIGINAL_MODEL");
        rule.setActionConfig("{}");
        rule.setFallbackConfig("{}");
        rule.setExecutionMode("ENFORCE");
        rule.setStatus("0");
        return rule;
    }
}
