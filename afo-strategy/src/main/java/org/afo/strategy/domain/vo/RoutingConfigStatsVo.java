package org.afo.strategy.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 路由配置统计视图。
 *
 * @author AI-FinOps Team
 * @since 2026-06-16
 */
@Data
public class RoutingConfigStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则总数。 */
    private Long totalRules;

    /** 今日新增规则数。 */
    private Long todayNewRules;

    /** 启用规则数。 */
    private Long enabledRules;

    /** 启用规则占比。 */
    private Double enabledRate;

    /** 今日命中次数。 */
    private Long todayHitCount;

    /** 今日命中较昨日增长占比。 */
    private Double todayHitGrowthRate;

    /** 今日兜底触发次数。 */
    private Long fallbackHitCount;
}
