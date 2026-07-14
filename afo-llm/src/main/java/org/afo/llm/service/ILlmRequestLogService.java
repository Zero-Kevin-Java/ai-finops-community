package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.bo.LlmRequestLogBo;
import org.afo.llm.domain.vo.LlmRequestLogVo;

import java.util.List;

/**
 * LLM 请求日志 Service 接口。
 *
 * @author afo
 */
public interface ILlmRequestLogService {

    LlmRequestLogVo queryById(Long requestLogId);

    TableDataInfo<LlmRequestLogVo> queryPageList(LlmRequestLogBo bo, PageQuery pageQuery);

    List<LlmRequestLogVo> queryList(LlmRequestLogBo bo);
}
