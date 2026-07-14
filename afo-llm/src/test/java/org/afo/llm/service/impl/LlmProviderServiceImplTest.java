package org.afo.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmProvider;
import org.afo.llm.domain.bo.LlmProviderBo;
import org.afo.llm.domain.vo.LlmProviderVo;
import org.afo.llm.mapper.LlmProviderMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class LlmProviderServiceImplTest {

    private final LlmProviderMapper providerMapper = mock(LlmProviderMapper.class);
    private final LlmProviderServiceImpl service = new LlmProviderServiceImpl(providerMapper);

    @Test
    void normalizePrefixesBeforeSave() {
        assertEquals("deepseek,DeepSeek/", LlmProviderServiceImpl.normalizeModelPrefixes(" deepseek, DeepSeek/ \n deepseek ; "));
    }

    @Test
    void providerVoGeneratesEntityToVoConverter() throws Exception {
        assertTrue(hasGeneratedEntityToVoMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryListMapsProvidersWithoutMapstructConverter() {
        when(providerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            provider(1L, "OpenAI", "gpt-,o1")
        ));

        List<LlmProviderVo> providers = service.queryList(new LlmProviderBo());

        assertEquals(1, providers.size());
        assertEquals("OpenAI", providers.get(0).getProviderName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryPageListMapsProvidersWithoutMapstructConverter() {
        when(providerMapper.selectList(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<LlmProvider> page = invocation.getArgument(0);
            page.setTotal(1);
            return List.of(provider(1L, "OpenAI", "gpt-,o1"));
        });

        TableDataInfo<LlmProviderVo> providers = service.queryPageList(new LlmProviderBo(), new PageQuery(10, 1));

        assertEquals(1, providers.getTotal());
        assertEquals(1, providers.getRows().size());
        assertEquals("OpenAI", providers.getRows().get(0).getProviderName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchProviderByModelNameUsesLongestConfiguredPrefix() {
        when(providerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            provider(1L, "Generic DeepSeek", "deepseek"),
            provider(2L, "DeepSeek Chat", "deepseek-chat,deepseek-reasoner")
        ));

        LlmProviderVo matched = service.matchByModelName("DeepSeek-Chat");

        assertEquals(2L, matched.getProviderId());
        assertEquals("DeepSeek Chat", matched.getProviderName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchProviderByModelNameReturnsNullWhenNoPrefixMatches() {
        when(providerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            provider(1L, "OpenAI", "gpt-,o1")
        ));

        assertNull(service.matchByModelName("claude-3-5-sonnet"));
    }

    private LlmProvider provider(Long id, String name, String prefixes) {
        LlmProvider provider = new LlmProvider();
        provider.setProviderId(id);
        provider.setProviderName(name);
        provider.setModelPrefixes(prefixes);
        provider.setStatus("0");
        return provider;
    }

    private boolean hasGeneratedEntityToVoMapper() throws Exception {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("org/afo/llm/domain");
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }
            Path domainPath = Path.of(resource.toURI());
            try (var files = Files.list(domainPath)) {
                if (files.anyMatch(file -> file.getFileName().toString().matches("LlmProviderToLlmProviderVoMapper__\\d+\\.class"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
