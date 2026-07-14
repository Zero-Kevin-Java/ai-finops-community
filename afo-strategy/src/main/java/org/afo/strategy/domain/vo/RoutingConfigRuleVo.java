package org.afo.strategy.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.strategy.domain.RoutingConfigRule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 路由配置规则视图对象。
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
@Data
@AutoMapper(target = RoutingConfigRule.class)
public class RoutingConfigRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private String tenantId;
    private String ruleName;
    private Integer priority;
    private String matchConfig;
    private String actionType;
    private String actionConfig;
    private String fallbackConfig;
    private String executionMode;
    private Date effectiveStart;
    private Date effectiveEnd;
    private String status;
    private Long hitCount;
    private Date lastHitTime;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    private Long updateBy;
    private String remark;
}
