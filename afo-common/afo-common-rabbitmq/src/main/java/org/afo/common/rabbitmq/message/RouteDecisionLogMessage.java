package org.afo.common.rabbitmq.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 路由决策日志 MQ 消息。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteDecisionLogMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String tenantId;
    private String model;
    private String targetModel;
    private String routeReason;
    private boolean whitelistHit;
    private long decisionLatencyMs;
    private String denyReason;
    private Long policyId;
    private String denyLayer;
    private String apiKeyId;
    private Long ruleId;
    private String ruleName;
    private String sourceModel;
    private String actionType;
    private String matchSummary;
    private boolean fallbackApplied;
    private String fallbackModel;
    private String fallbackReason;
    private String classificationResult;
    private Double classifierConfidence;
    private String teamTag;
    private String path;
    private Instant timestamp;
}
