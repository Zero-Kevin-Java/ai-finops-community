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
 * 路由配置规则对象 afo_routing_config_rule。
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_routing_config_rule")
public class RoutingConfigRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则 ID。 */
    @TableId(value = "rule_id")
    private Long ruleId;

    /** 租户 ID。 */
    private String tenantId;

    /** 规则名称。 */
    private String ruleName;

    /** 优先级，数字越小越先匹配。 */
    private Integer priority;

    /** 匹配条件 JSON。 */
    private String matchConfig;

    /** 路由动作。 */
    private String actionType;

    /** 路由动作配置 JSON。 */
    private String actionConfig;

    /** 兜底策略 JSON。 */
    private String fallbackConfig;

    /** 执行模式：ENFORCE / RECORD_ONLY。 */
    private String executionMode;

    /** 生效开始时间。 */
    private Date effectiveStart;

    /** 生效结束时间。 */
    private Date effectiveEnd;

    /** 状态：0 启用，1 停用。 */
    private String status;

    /** 命中次数。 */
    private Long hitCount;

    /** 最近命中时间。 */
    private Date lastHitTime;

    /** 删除标志：0 存在，1 删除。 */
    @TableLogic
    private String delFlag;

    /** 备注。 */
    private String remark;
}
