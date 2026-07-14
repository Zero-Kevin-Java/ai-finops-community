package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.bo.LlmProjectBo;
import org.afo.llm.domain.vo.LlmProjectVo;

import java.util.Collection;
import java.util.List;

/**
 * LLM 项目服务。
 *
 * @author afo
 */
public interface ILlmProjectService {

    /**
     * 查询项目详情。
     *
     * @param projectId 项目ID
     * @return 项目详情
     */
    LlmProjectVo queryById(Long projectId);

    /**
     * 分页查询项目。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 项目分页
     */
    TableDataInfo<LlmProjectVo> queryPageList(LlmProjectBo bo, PageQuery pageQuery);

    /**
     * 查询项目列表。
     *
     * @param bo 查询条件
     * @return 项目列表
     */
    List<LlmProjectVo> queryList(LlmProjectBo bo);

    /**
     * 新增项目。
     *
     * @param bo 项目
     * @return 是否成功
     */
    Boolean insertByBo(LlmProjectBo bo);

    /**
     * 修改项目。
     *
     * @param bo 项目
     * @return 是否成功
     */
    Boolean updateByBo(LlmProjectBo bo);

    /**
     * 批量删除项目。
     *
     * @param ids 项目ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 修改项目状态。
     *
     * @param projectId 项目ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateStatus(Long projectId, String status);

    /**
     * 校验项目编码唯一。
     *
     * @param bo 项目
     * @return true 唯一
     */
    boolean checkProjectCodeUnique(LlmProjectBo bo);
}
