package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.bo.LlmAppClientBo;
import org.afo.llm.domain.vo.LlmAppClientVo;

import java.util.Collection;
import java.util.List;

/**
 * LLM 应用客户端服务接口。
 *
 * @author afo
 */
public interface ILlmAppClientService {

    /**
     * 查询应用客户端详情。
     *
     * @param clientId 应用客户端ID
     * @return 应用客户端详情
     */
    LlmAppClientVo queryById(Long clientId);

    /**
     * 分页查询应用客户端。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 应用客户端分页
     */
    TableDataInfo<LlmAppClientVo> queryPageList(LlmAppClientBo bo, PageQuery pageQuery);

    /**
     * 查询应用客户端列表。
     *
     * @param bo 查询条件
     * @return 应用客户端列表
     */
    List<LlmAppClientVo> queryList(LlmAppClientBo bo);

    /**
     * 新增应用客户端。
     *
     * @param bo 应用客户端
     * @return 是否成功
     */
    Boolean insertByBo(LlmAppClientBo bo);

    /**
     * 修改应用客户端。
     *
     * @param bo 应用客户端
     * @return 是否成功
     */
    Boolean updateByBo(LlmAppClientBo bo);

    /**
     * 批量删除应用客户端。
     *
     * @param ids 应用客户端ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 修改应用客户端状态。
     *
     * @param clientId 应用客户端ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateStatus(Long clientId, String status);

    /**
     * 校验应用编码唯一。
     *
     * @param bo 应用客户端
     * @return true 唯一
     */
    boolean checkAppCodeUnique(LlmAppClientBo bo);
}
