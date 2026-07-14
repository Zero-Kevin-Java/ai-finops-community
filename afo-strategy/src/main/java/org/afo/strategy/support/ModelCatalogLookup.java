package org.afo.strategy.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.StringUtils;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.mapper.LlmModelCatalogMapper;
import org.afo.strategy.domain.vo.ModelAccessModelVo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy-facing model catalog lookup and projection rules.
 */
@Component
@RequiredArgsConstructor
public class ModelCatalogLookup {

    private final LlmModelCatalogMapper modelCatalogMapper;

    public LlmModelCatalog find(String modelCode) {
        return modelCatalogMapper.selectOne(
            Wrappers.lambdaQuery(LlmModelCatalog.class)
                .eq(LlmModelCatalog::getModelCode, modelCode)
                .eq(LlmModelCatalog::getDelFlag, "0")
                .last("LIMIT 1"));
    }

    public void requireModelCodesExist(List<String> modelCodes) {
        for (String modelCode : modelCodes) {
            boolean exists = modelCatalogMapper.exists(
                Wrappers.lambdaQuery(LlmModelCatalog.class)
                    .eq(LlmModelCatalog::getModelCode, modelCode)
                    .eq(LlmModelCatalog::getDelFlag, "0"));
            if (!exists) {
                throw new ServiceException("模型编码不存在：{}", modelCode);
            }
        }
    }

    public List<ModelAccessModelVo> accessModelDetails(List<String> modelCodes) {
        if (modelCodes == null || modelCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, ModelAccessModelVo> detailMap = accessModelDetailMap(modelCodes);
        return modelCodes.stream()
            .map(modelCode -> detailMap.getOrDefault(modelCode, missingModelDetail(modelCode)))
            .toList();
    }

    private Map<String, ModelAccessModelVo> accessModelDetailMap(Collection<String> modelCodes) {
        Set<String> distinctCodes = modelCodes.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        if (distinctCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return modelCatalogMapper.selectList(
                Wrappers.lambdaQuery(LlmModelCatalog.class)
                    .in(LlmModelCatalog::getModelCode, distinctCodes)
                    .eq(LlmModelCatalog::getDelFlag, "0"))
            .stream()
            .map(this::toModelDetail)
            .collect(Collectors.toMap(ModelAccessModelVo::getModelCode, Function.identity(), (left, right) -> left));
    }

    private ModelAccessModelVo toModelDetail(LlmModelCatalog catalog) {
        ModelAccessModelVo vo = new ModelAccessModelVo();
        vo.setModelCode(catalog.getModelCode());
        vo.setDisplayName(catalog.getDisplayName());
        vo.setProvider(catalog.getProvider());
        vo.setModelType(catalog.getModelType());
        vo.setStatus(catalog.getStatus());
        return vo;
    }

    private ModelAccessModelVo missingModelDetail(String modelCode) {
        ModelAccessModelVo vo = new ModelAccessModelVo();
        vo.setModelCode(modelCode);
        vo.setDisplayName(modelCode);
        return vo;
    }
}
