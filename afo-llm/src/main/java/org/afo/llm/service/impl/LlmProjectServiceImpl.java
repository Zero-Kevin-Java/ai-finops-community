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
import org.afo.llm.domain.LlmAppClient;
import org.afo.llm.domain.LlmProject;
import org.afo.llm.domain.bo.LlmProjectBo;
import org.afo.llm.domain.vo.LlmProjectVo;
import org.afo.llm.mapper.LlmAppClientMapper;
import org.afo.llm.mapper.LlmProjectMapper;
import org.afo.llm.service.ILlmProjectService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * LLM 项目服务实现。
 *
 * @author afo
 */
@RequiredArgsConstructor
@Service
public class LlmProjectServiceImpl implements ILlmProjectService {

    private final LlmProjectMapper baseMapper;
    private final LlmAppClientMapper appClientMapper;

    /**
     * 查询项目详情。
     *
     * @param projectId 项目ID
     * @return 项目详情
     */
    @Override
    public LlmProjectVo queryById(Long projectId) {
        return baseMapper.selectVoById(projectId);
    }

    /**
     * 分页查询项目。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 项目分页
     */
    @Override
    public TableDataInfo<LlmProjectVo> queryPageList(LlmProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LlmProject> lqw = buildQueryWrapper(bo);
        Page<LlmProjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询项目列表。
     *
     * @param bo 查询条件
     * @return 项目列表
     */
    @Override
    public List<LlmProjectVo> queryList(LlmProjectBo bo) {
        LambdaQueryWrapper<LlmProject> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 新增项目。
     *
     * @param bo 项目
     * @return 是否成功
     */
    @Override
    public Boolean insertByBo(LlmProjectBo bo) {
        LlmProject add = MapstructUtils.convert(bo, LlmProject.class);
        fillDefaultValues(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setProjectId(add.getProjectId());
        }
        return flag;
    }

    /**
     * 修改项目。
     *
     * @param bo 项目
     * @return 是否成功
     */
    @Override
    public Boolean updateByBo(LlmProjectBo bo) {
        LlmProject update = MapstructUtils.convert(bo, LlmProject.class);
        fillDefaultValues(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量删除项目。
     *
     * @param ids 项目ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (appClientMapper.exists(new LambdaQueryWrapper<LlmAppClient>().in(LlmAppClient::getProjectId, ids))) {
                throw new ServiceException("项目存在应用客户端，不能删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 修改项目状态。
     *
     * @param projectId 项目ID
     * @param status 状态
     * @return 是否成功
     */
    @Override
    public Boolean updateStatus(Long projectId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<LlmProject>()
                .set(LlmProject::getStatus, status)
                .eq(LlmProject::getProjectId, projectId)) > 0;
    }

    /**
     * 校验项目编码唯一。
     *
     * @param bo 项目
     * @return true 唯一
     */
    @Override
    public boolean checkProjectCodeUnique(LlmProjectBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<LlmProject>()
            .eq(LlmProject::getProjectCode, bo.getProjectCode())
            .ne(ObjectUtil.isNotNull(bo.getProjectId()), LlmProject::getProjectId, bo.getProjectId()));
        return !exist;
    }

    private LambdaQueryWrapper<LlmProject> buildQueryWrapper(LlmProjectBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LlmProject> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getProjectCode()), LlmProject::getProjectCode, bo.getProjectCode());
        lqw.like(StringUtils.isNotBlank(bo.getProjectName()), LlmProject::getProjectName, bo.getProjectName());
        lqw.eq(ObjectUtil.isNotNull(bo.getOwnerUserId()), LlmProject::getOwnerUserId, bo.getOwnerUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), LlmProject::getStatus, bo.getStatus());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            LlmProject::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(LlmProject::getCreateTime);
        return lqw;
    }

    private void fillDefaultValues(LlmProject project) {
        if (StringUtils.isBlank(project.getStatus())) {
            project.setStatus("0");
        }
    }
}
