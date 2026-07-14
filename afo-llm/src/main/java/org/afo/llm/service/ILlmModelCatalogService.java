package org.afo.llm.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.llm.domain.LlmModelCatalog;
import org.afo.llm.domain.bo.LlmModelCatalogBo;
import org.afo.llm.domain.vo.LlmModelCatalogVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * LLM 模型目录服务。
 *
 * @author afo
 */
public interface ILlmModelCatalogService {

    /**
     * 查询模型目录详情。
     *
     * @param modelId 模型ID
     * @return 模型目录详情
     */
    LlmModelCatalogVo queryById(Long modelId);

    /**
     * 分页查询模型目录。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 模型目录分页
     */
    TableDataInfo<LlmModelCatalogVo> queryPageList(LlmModelCatalogBo bo, PageQuery pageQuery);

    /**
     * 查询模型目录列表。
     *
     * @param bo 查询条件
     * @return 模型目录列表
     */
    List<LlmModelCatalogVo> queryList(LlmModelCatalogBo bo);

    /**
     * 新增模型目录。
     *
     * @param bo 模型目录
     * @return 是否成功
     */
    Boolean insertByBo(LlmModelCatalogBo bo);

    /**
     * 修改模型目录。
     *
     * @param bo 模型目录
     * @return 是否成功
     */
    Boolean updateByBo(LlmModelCatalogBo bo);

    /**
     * 批量删除模型目录。
     *
     * @param ids 模型ID集合
     * @param isValid 是否校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 修改模型状态。
     *
     * @param modelId 模型ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean updateStatus(Long modelId, String status);

    /**
     * 校验模型编码唯一。
     *
     * @param bo 模型目录
     * @return true 唯一
     */
    boolean checkModelCodeUnique(LlmModelCatalogBo bo);

    /**
     * 获取模型下拉选项（用于价格管理、策略管理等表单）。
     *
     * @return 模型选项列表 [{label, value}]
     */
    List<Map<String, Object>> listOptions();

    /**
     * 根据租户和模型编码查询模型配置（供网关回源）。
     *
     * @param tenantId 租户ID
     * @param modelCode 模型编码
     * @return 模型实体
     */
    LlmModelCatalog getModelConfig(String tenantId, String modelCode);
}
