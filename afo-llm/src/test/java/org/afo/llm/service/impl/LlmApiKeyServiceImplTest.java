package org.afo.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.core.exception.ServiceException;
import org.afo.llm.domain.LlmAppClient;
import org.afo.llm.domain.LlmApiKey;
import org.afo.llm.domain.bo.LlmApiKeyBo;
import org.afo.llm.domain.vo.LlmApiKeyVo;
import org.afo.llm.mapper.LlmAppClientMapper;
import org.afo.llm.mapper.LlmApiKeyMapper;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class LlmApiKeyServiceImplTest {

    private final LlmApiKeyMapper apiKeyMapper = mock(LlmApiKeyMapper.class);
    private final LlmModelCatalogMapper modelCatalogMapper = mock(LlmModelCatalogMapper.class);
    private final LlmAppClientMapper appClientMapper = mock(LlmAppClientMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final LlmApiKeyServiceImpl service = new LlmApiKeyServiceImpl(
        apiKeyMapper,
        modelCatalogMapper,
        appClientMapper,
        stringRedisTemplate
    );

    @Test
    @SuppressWarnings("unchecked")
    void generateUniquePlainKeyUsesSkPrefix() {
        when(apiKeyMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

        String plainKey = ReflectionTestUtils.invokeMethod(service, "generateUniquePlainKey");

        assertTrue(plainKey.startsWith("sk_"));
        assertFalse(plainKey.startsWith("afo_"));
        assertEquals(51, plainKey.length());
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryPageListMasksKeyPrefixBeforeReturning() {
        LlmApiKeyVo vo = new LlmApiKeyVo();
        vo.setKeyPrefix("sk_1234567890abcdef12345");
        Page<LlmApiKeyVo> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(apiKeyMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        TableDataInfo<LlmApiKeyVo> result = service.queryPageList(new LlmApiKeyBo(), new PageQuery(10, 1));

        assertEquals("sk_1234567890abcdef1****", result.getRows().get(0).getKeyPrefix());
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryPageListEnrichesAppNameOnly() {
        LlmApiKeyVo vo = new LlmApiKeyVo();
        vo.setClientId(20L);
        vo.setKeyPrefix("sk_1234567890abcdef12345");
        Page<LlmApiKeyVo> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(apiKeyMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        LlmAppClient appClient = new LlmAppClient();
        appClient.setClientId(20L);
        appClient.setAppName("控制台");
        when(appClientMapper.selectBatchIds(any())).thenReturn(List.of(appClient));

        TableDataInfo<LlmApiKeyVo> result = service.queryPageList(new LlmApiKeyBo(), new PageQuery(10, 1));

        assertEquals("控制台", result.getRows().get(0).getAppName());
    }

    @Test
    void apiKeyTypesDoNotExposeProjectFields() {
        assertThrows(NoSuchFieldException.class, () -> LlmApiKey.class.getDeclaredField("projectId"));
        assertThrows(NoSuchFieldException.class, () -> LlmApiKeyBo.class.getDeclaredField("projectId"));
        assertThrows(NoSuchFieldException.class, () -> LlmApiKeyVo.class.getDeclaredField("projectId"));
        assertThrows(NoSuchFieldException.class, () -> LlmApiKeyVo.class.getDeclaredField("projectName"));
    }

    @Test
    void updateStatusRejectsExpiredStatus() {
        LlmApiKeyBo bo = new LlmApiKeyBo();
        bo.setKeyId(1001L);
        bo.setStatus("2");

        assertThrows(ServiceException.class, () -> service.updateStatus(bo.getKeyId(), bo.getStatus()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateKeepsStoredKeyPrefixAndHashImmutable() {
        LlmApiKey existing = new LlmApiKey();
        existing.setKeyId(1001L);
        existing.setKeyPrefix("afo_sk_existing_prefix");
        existing.setKeyHash("stored-hash");
        when(apiKeyMapper.selectById(1001L)).thenReturn(existing);
        when(modelCatalogMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(apiKeyMapper.updateById(any(LlmApiKey.class))).thenReturn(1);

        LlmApiKeyBo bo = new LlmApiKeyBo();
        bo.setKeyId(1001L);
        bo.setClientId(20L);
        bo.setOwnerUserId(30L);
        bo.setKeyName("Updated Key");
        bo.setKeyPrefix("afo_sk_malicious_prefix");
        bo.setKeyScope("gpt-4o");
        bo.setExpireTime(null);
        bo.setStatus("0");

        service.updateByBo(bo);

        ArgumentCaptor<LlmApiKey> captor = ArgumentCaptor.forClass(LlmApiKey.class);
        verify(apiKeyMapper).updateById(captor.capture());
        assertEquals("afo_sk_existing_prefix", captor.getValue().getKeyPrefix());
        assertEquals("stored-hash", captor.getValue().getKeyHash());
        assertEquals(30L, captor.getValue().getOwnerUserId());
        assertNull(captor.getValue().getExpireTime());
        verify(stringRedisTemplate).convertAndSend("gateway:cache:refresh", "apikey:stored-hash");
    }
}
