package org.afo.strategy.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.strategy.domain.ModelAccessPolicy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 企业模型准入策略视图对象。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
@AutoMapper(target = ModelAccessPolicy.class)
public class ModelAccessPolicyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 策略 ID。 */
    private Long policyId;

    /** 租户 ID。 */
    private String tenantId;

    /** 策略名称。 */
    private String policyName;

    /** 默认准入模式。 */
    private String defaultMode;

    /** 允许模型编码 JSON 数组。 */
    private String allowedModels;

    /** 禁止模型编码 JSON 数组。 */
    private String deniedModels;

    /** 允许模型数量。 */
    private Integer allowedModelCount;

    /** 禁止模型数量。 */
    private Integer deniedModelCount;

    /** 允许模型明细。 */
    private List<ModelAccessModelVo> allowedModelDetails;

    /** 禁止模型明细。 */
    private List<ModelAccessModelVo> deniedModelDetails;

    /** 生效开始时间。 */
    private Date effectiveStart;

    /** 生效结束时间。 */
    private Date effectiveEnd;

    /** 状态。 */
    private String status;

    /** 创建时间。 */
    private Date createTime;

    /** 更新时间。 */
    private Date updateTime;

    /** 更新人。 */
    private Long updateBy;

    /** 备注。 */
    private String remark;
}
