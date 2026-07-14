package org.afo.strategy.service;

import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.strategy.domain.ModelAccessPolicy;
import org.afo.strategy.domain.vo.ModelAccessPolicyVo;

import java.util.List;

/**
 * 企业模型准入策略服务接口。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
public interface IModelAccessPolicyService {

    /** 查询当前有效的企业模型准入策略。 */
    ModelAccessPolicyVo getActivePolicy(String tenantId);

    /** 分页查询企业模型准入策略。 */
    TableDataInfo<ModelAccessPolicyVo> queryPageList(ModelAccessPolicy policy, PageQuery pageQuery);

    /** 查询企业模型准入策略详情。 */
    ModelAccessPolicyVo queryById(Long policyId);

    /** 新增企业模型准入策略。 */
    void insert(ModelAccessPolicy policy);

    /** 修改企业模型准入策略。 */
    void update(ModelAccessPolicy policy);

    /** 修改企业模型准入策略状态。 */
    void updateStatus(Long policyId, String status);

    /** 批量删除企业模型准入策略。 */
    void deleteByIds(List<Long> policyIds);

    /** 查询企业模型准入策略列表，用于导出。 */
    List<ModelAccessPolicyVo> queryList(ModelAccessPolicy policy);
}
