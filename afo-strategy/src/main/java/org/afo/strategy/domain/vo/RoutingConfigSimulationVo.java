package org.afo.strategy.domain.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 路由配置模拟结果。
 */
@Data
public class RoutingConfigSimulationVo {

    private boolean matched;
    private Long ruleId;
    private String ruleName;
    private String actionType;
    private String sourceModel;
    private String targetModel;
    private String executionMode;
    private String matchSummary;
    private boolean fallbackApplied;
    private String fallbackModel;
    private String fallbackReason;
    private List<String> fallbackModels = Collections.emptyList();
}
