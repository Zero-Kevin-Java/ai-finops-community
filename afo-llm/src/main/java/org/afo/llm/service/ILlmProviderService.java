package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.bo.LlmProviderBo;
import org.afo.llm.domain.vo.LlmProviderVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ILlmProviderService {

    LlmProviderVo queryById(Long providerId);

    TableDataInfo<LlmProviderVo> queryPageList(LlmProviderBo bo, PageQuery pageQuery);

    List<LlmProviderVo> queryList(LlmProviderBo bo);

    Boolean insertByBo(LlmProviderBo bo);

    Boolean updateByBo(LlmProviderBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    Boolean updateStatus(Long providerId, String status);

    List<Map<String, Object>> listOptions();

    LlmProviderVo matchByModelName(String modelName);
}
