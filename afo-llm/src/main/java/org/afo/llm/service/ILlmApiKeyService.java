package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.bo.LlmApiKeyBo;
import org.afo.llm.domain.vo.LlmApiKeyVo;

import java.util.Collection;
import java.util.List;

/**
 * LLM 业务 API Key 服务接口。
 *
 * @author afo
 */
public interface ILlmApiKeyService {

    /**
     * 查询 API Key 详情。
     *
     * @param keyId API Key ID
     * @return API Key 详情
     */
    LlmApiKeyVo queryById(Long keyId);

    /**
     * 分页查询 API Key。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return API Key 分页
     */
    TableDataInfo<LlmApiKeyVo> queryPageList(LlmApiKeyBo bo, PageQuery pageQuery);

    /**
     * 查询 API Key 列表。
     *
     * @param bo 查询条件
     * @return API Key 列表
     */
    List<LlmApiKeyVo> queryList(LlmApiKeyBo bo);

    /**
     * 新增 API Key。
     *
     * @param bo API Key
     * @return 仅返回一次的明文 Key
     */
    String insertByBo(LlmApiKeyBo bo);

    /**
     * 修改 API Key。
     *
     * @param bo API Key
     * @return 是否成功
     */
    Boolean updateByBo(LlmApiKeyBo bo);

    /**
     * 批量删除 API Key。
     *
     * @param ids API Key ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 修改 API Key 状态。
     *
     * @param keyId API Key ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateStatus(Long keyId, String status);
}
