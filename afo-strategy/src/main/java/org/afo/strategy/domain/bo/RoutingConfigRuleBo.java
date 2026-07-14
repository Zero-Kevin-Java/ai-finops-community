package org.afo.strategy.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.strategy.domain.RoutingConfigRule;

import java.util.Date;

/**
 * 路由配置规则业务对象。
 *
 * @author AI-FinOps Team
 * @since 2026-05-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = RoutingConfigRule.class, reverseConvertGenerate = false)
public class RoutingConfigRuleBo extends BaseEntity {

    @NotNull(message = "规则 ID 不能为空", groups = { EditGroup.class })
    private Long ruleId;

    private String tenantId;

    @NotBlank(message = "规则名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String ruleName;

    @NotNull(message = "优先级不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer priority;

    private String matchConfig;

    @NotBlank(message = "路由动作不能为空", groups = { AddGroup.class, EditGroup.class })
    private String actionType;

    private String actionConfig;
    private String fallbackConfig;
    private String executionMode;
    private Date effectiveStart;
    private Date effectiveEnd;
    private String status;
    private String remark;
}
