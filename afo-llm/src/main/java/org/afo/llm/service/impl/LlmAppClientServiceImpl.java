package org.afo.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.MapstructUtils;
import org.afo.common.core.utils.StringUtils;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmApiKey;
import org.afo.llm.domain.LlmAppClient;
import org.afo.llm.domain.LlmProject;
import org.afo.llm.domain.bo.LlmAppClientBo;
import org.afo.llm.domain.vo.LlmAppClientVo;
import org.afo.llm.mapper.LlmApiKeyMapper;
import org.afo.llm.mapper.LlmAppClientMapper;
import org.afo.llm.mapper.LlmProjectMapper;
import org.afo.llm.service.ILlmAppClientService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 应用客户端服务实现。
 *
 * @author afo
 */
@RequiredArgsConstructor
@Service
public class LlmAppClientServiceImpl implements ILlmAppClientService {

    private final LlmAppClientMapper baseMapper;
    private final LlmApiKeyMapper apiKeyMapper;
    private final LlmProjectMapper projectMapper;

    /**
     * 查询应用客户端详情。
     *
     * @param clientId 应用客户端ID
     * @return 应用客户端详情
     */
    @Override
    public LlmAppClientVo queryById(Long clientId) {
        LlmAppClientVo vo = baseMapper.selectVoById(clientId);
        if (vo != null) {
            enrichNames(Collections.singletonList(vo));
        }
        return vo;
    }

    /**
     * 分页查询应用客户端。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 应用客户端分页
     */
    @Override
    public TableDataInfo<LlmAppClientVo> queryPageList(LlmAppClientBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LlmAppClient> lqw = buildQueryWrapper(bo);
        Page<LlmAppClientVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        enrichNames(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询应用客户端列表。
     *
     * @param bo 查询条件
     * @return 应用客户端列表
     */
    @Override
    public List<LlmAppClientVo> queryList(LlmAppClientBo bo) {
        LambdaQueryWrapper<LlmAppClient> lqw = buildQueryWrapper(bo);
        List<LlmAppClientVo> list = baseMapper.selectVoList(lqw);
        enrichNames(list);
        return list;
    }

    /**
     * 新增应用客户端。
     *
     * @param bo 应用客户端
     * @return 是否成功
     */
    @Override
    public Boolean insertByBo(LlmAppClientBo bo) {
        LlmAppClient add = MapstructUtils.convert(bo, LlmAppClient.class);
        fillDefaultValues(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setClientId(add.getClientId());
        }
        return flag;
    }

    /**
     * 修改应用客户端。
     *
     * @param bo 应用客户端
     * @return 是否成功
     */
    @Override
    public Boolean updateByBo(LlmAppClientBo bo) {
        LlmAppClient update = MapstructUtils.convert(bo, LlmAppClient.class);
        fillDefaultValues(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量删除应用客户端。
     *
     * @param ids 应用客户端ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (apiKeyMapper.exists(new LambdaQueryWrapper<LlmApiKey>().in(LlmApiKey::getClientId, ids))) {
                throw new ServiceException("应用客户端存在 API Key，不能删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 修改应用客户端状态。
     *
     * @param clientId 应用客户端ID
     * @param status 状态
     * @return 是否成功
     */
    @Override
    public Boolean updateStatus(Long clientId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<LlmAppClient>()
                .set(LlmAppClient::getStatus, status)
                .eq(LlmAppClient::getClientId, clientId)) > 0;
    }

    /**
     * 校验应用编码唯一。
     *
     * @param bo 应用客户端
     * @return true 唯一
     */
    @Override
    public boolean checkAppCodeUnique(LlmAppClientBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<LlmAppClient>()
            .eq(LlmAppClient::getAppCode, bo.getAppCode())
            .ne(ObjectUtil.isNotNull(bo.getClientId()), LlmAppClient::getClientId, bo.getClientId()));
        return !exist;
    }

    private LambdaQueryWrapper<LlmAppClient> buildQueryWrapper(LlmAppClientBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmAppClient> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getProjectId()), LlmAppClient::getProjectId, bo.getProjectId());
        lqw.like(StringUtils.isNotBlank(bo.getAppCode()), LlmAppClient::getAppCode, bo.getAppCode());
        lqw.like(StringUtils.isNotBlank(bo.getAppName()), LlmAppClient::getAppName, bo.getAppName());
        lqw.eq(StringUtils.isNotBlank(bo.getAppType()), LlmAppClient::getAppType, bo.getAppType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), LlmAppClient::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmAppClient::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(LlmAppClient::getCreateTime);
        return lqw;
    }

    private void fillDefaultValues(LlmAppClient client) {
        if (StringUtils.isBlank(client.getAppType())) {
            client.setAppType("server");
        }
        if (StringUtils.isBlank(client.getStatus())) {
            client.setStatus("0");
        }
    }

    private void enrichNames(List<LlmAppClientVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }

        Set<Long> projectIds = vos.stream()
            .map(LlmAppClientVo::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> projectNameMap = Collections.emptyMap();
        if (!projectIds.isEmpty()) {
            projectNameMap = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(LlmProject::getProjectId, LlmProject::getProjectName, (a, b) -> a));
        }

        for (LlmAppClientVo vo : vos) {
            vo.setProjectName(projectNameMap.get(vo.getProjectId()));
        }
    }
}
