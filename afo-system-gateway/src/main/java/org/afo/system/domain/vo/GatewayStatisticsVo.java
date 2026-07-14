package org.afo.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关统计 VO — 按 API Key 或团队聚合
 * 
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Data
public class GatewayStatisticsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** API Key ID 或团队名称 */
    private String groupKey;

    /** 请求次数 */
    private Long requestCount;

    /** 总成本（账本 B：网关理论消耗） */
    private Double totalCost;
}
