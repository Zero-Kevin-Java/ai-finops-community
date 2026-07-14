package org.afo.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmRequestLog;
import org.afo.llm.domain.bo.LlmRequestLogBo;
import org.afo.llm.domain.vo.LlmRequestLogVo;
import org.afo.llm.mapper.LlmRequestLogMapper;
import org.afo.llm.service.ILlmRequestLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 请求日志服务实现。
 *
 * @author afo
 */
@RequiredArgsConstructor
@Service
public class LlmRequestLogServiceImpl implements ILlmRequestLogService {

    private final LlmRequestLogMapper baseMapper;

    @Override
    public LlmRequestLogVo queryById(Long requestLogId) {
        return baseMapper.selectVoById(requestLogId);
    }

    @Override
    public TableDataInfo<LlmRequestLogVo> queryPageList(LlmRequestLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LlmRequestLog> lqw = buildQueryWrapper(bo);
        Page<LlmRequestLogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<LlmRequestLogVo> queryList(LlmRequestLogBo bo) {
        LambdaQueryWrapper<LlmRequestLog> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LlmRequestLog> buildQueryWrapper(LlmRequestLogBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmRequestLog> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getRequestId()), LlmRequestLog::getRequestId, bo.getRequestId());
        lqw.eq(ObjectUtil.isNotNull(bo.getProjectId()), LlmRequestLog::getProjectId, bo.getProjectId());
        lqw.eq(ObjectUtil.isNotNull(bo.getClientId()), LlmRequestLog::getClientId, bo.getClientId());
        lqw.eq(ObjectUtil.isNotNull(bo.getKeyId()), LlmRequestLog::getKeyId, bo.getKeyId());
        lqw.like(StringUtils.isNotBlank(bo.getModelCode()), LlmRequestLog::getModelCode, bo.getModelCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStream()), LlmRequestLog::getStream, bo.getStream());
        lqw.eq(StringUtils.isNotBlank(bo.getRequestStatus()), LlmRequestLog::getRequestStatus, bo.getRequestStatus());
        lqw.eq(ObjectUtil.isNotNull(bo.getHttpStatus()), LlmRequestLog::getHttpStatus, bo.getHttpStatus());
        lqw.like(StringUtils.isNotBlank(bo.getTraceId()), LlmRequestLog::getTraceId, bo.getTraceId());
        lqw.eq(ObjectUtil.isNotNull(bo.getUsageId()), LlmRequestLog::getUsageId, bo.getUsageId());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmRequestLog::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(LlmRequestLog::getCreateTime);
        return lqw;
    }
}
