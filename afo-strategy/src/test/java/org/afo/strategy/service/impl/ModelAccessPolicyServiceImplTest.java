package org.afo.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.afo.common.core.exception.ServiceException;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.afo.strategy.domain.ModelAccessPolicy;
import org.afo.strategy.domain.vo.ModelAccessPolicyVo;
import org.afo.strategy.mapper.ModelAccessPolicyMapper;
import org.afo.strategy.support.ModelCatalogLookup;
import org.afo.strategy.support.PolicyJsonCodec;
import org.afo.strategy.support.StrategyTenantResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelAccessPolicyServiceImplTest {

    private final ModelAccessPolicyMapper policyMapper = mock(ModelAccessPolicyMapper.class);
    private final LlmModelCatalogMapper modelCatalogMapper = mock(LlmModelCatalogMapper.class);
    private final ModelAccessPolicyServiceImpl service = new ModelAccessPolicyServiceImpl(
        policyMapper,
        new ModelCatalogLookup(modelCatalogMapper),
        new StrategyTenantResolver(),
        new PolicyJsonCodec(new ObjectMapper())
    );

    @Test
    void rejectsModelAppearingInAllowedAndDeniedLists() {
        ModelAccessPolicy policy = new ModelAccessPolicy();
        policy.setTenantId("000000");
        policy.setPolicyName("企业默认模型准入规则");
        policy.setDefaultMode("ALLOW_UNLISTED");
        policy.setAllowedModels("[\"gpt-4o\"]");
        policy.setDeniedModels("[\"gpt-4o\"]");
        policy.setStatus("0");

        assertThrows(ServiceException.class, () -> service.insert(policy));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsUnknownModelCode() {
        ModelAccessPolicy policy = new ModelAccessPolicy();
        policy.setTenantId("000000");
        policy.setPolicyName("企业默认模型准入规则");
        policy.setDefaultMode("ALLOW_UNLISTED");
        policy.setAllowedModels("[\"missing-model\"]");
        policy.setDeniedModels("[]");
        policy.setStatus("0");
        when(modelCatalogMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

        assertThrows(ServiceException.class, () -> service.insert(policy));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryByIdIncludesAllowedAndDeniedModelDetails() {
        ModelAccessPolicyVo policy = new ModelAccessPolicyVo();
        policy.setPolicyId(1L);
        policy.setTenantId("000000");
        policy.setAllowedModels("[\"gpt-4o\"]");
        policy.setDeniedModels("[\"gpt-4o-mini\"]");
        when(policyMapper.selectVoById(1L)).thenReturn(policy);

        LlmModelCatalog allowed = new LlmModelCatalog();
        allowed.setModelCode("gpt-4o");
        allowed.setDisplayName("GPT-4o");
        allowed.setProvider("openai");
        allowed.setModelType("chat");
        allowed.setStatus("0");

        LlmModelCatalog denied = new LlmModelCatalog();
        denied.setModelCode("gpt-4o-mini");
        denied.setDisplayName("GPT-4o mini");
        denied.setProvider("openai");
        denied.setModelType("chat");
        denied.setStatus("1");
        when(modelCatalogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(allowed, denied));

        ModelAccessPolicyVo result = service.queryById(1L);

        assertEquals(1, result.getAllowedModelDetails().size());
        assertEquals("GPT-4o", result.getAllowedModelDetails().get(0).getDisplayName());
        assertEquals(1, result.getDeniedModelDetails().size());
        assertEquals("gpt-4o-mini", result.getDeniedModelDetails().get(0).getModelCode());
    }
}
