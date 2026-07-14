package org.afo.strategy.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 企业模型准入策略对象 afo_model_access_policy。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_model_access_policy")
public class ModelAccessPolicy extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 策略 ID。 */
    @TableId(value = "policy_id")
    private Long policyId;

    /** 租户 ID。 */
    private String tenantId;

    /** 策略名称。 */
    private String policyName;

    /** 默认准入模式：ALLOW_UNLISTED / DENY_UNLISTED。 */
    private String defaultMode;

    /** 允许模型编码 JSON 数组。 */
    private String allowedModels;

    /** 禁止模型编码 JSON 数组。 */
    private String deniedModels;

    /** 生效开始时间，为空表示立即生效。 */
    private Date effectiveStart;

    /** 生效结束时间，为空表示长期有效。 */
    private Date effectiveEnd;

    /** 状态：0 启用，1 停用。 */
    private String status;

    /** 删除标志：0 存在，1 删除。 */
    @TableLogic
    private String delFlag;

    /** 备注。 */
    private String remark;
}
