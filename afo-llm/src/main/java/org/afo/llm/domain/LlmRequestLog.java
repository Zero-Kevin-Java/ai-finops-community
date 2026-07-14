package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * LLM 请求日志对象 afo_llm_request_log。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_request_log")
public class LlmRequestLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求日志ID。
     */
    @TableId(value = "request_log_id")
    private Long requestLogId;

    /**
     * 请求ID。
     */
    private String requestId;

    /**
     * 项目ID。
     */
    private Long projectId;

    /**
     * 应用客户端ID。
     */
    private Long clientId;

    /**
     * API Key ID。
     */
    private Long keyId;

    /**
     * 数字员工ID。
     */
    private Long digitalEmployeeId;

    /**
     * 模型编码。
     */
    private String modelCode;

    /**
     * 最终路由模型编码。
     */
    private String targetModelCode;

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * 是否流式请求（0否 1是）。
     */
    private String stream;

    /**
     * 请求状态。
     */
    private String requestStatus;

    /**
     * HTTP 状态码。
     */
    private Integer httpStatus;

    /**
     * 请求耗时毫秒。
     */
    private Long latencyMs;

    /**
     * 错误编码。
     */
    private String errorCode;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 客户端 IP。
     */
    private String clientIp;

    /**
     * User-Agent。
     */
    private String userAgent;

    /**
     * 链路追踪ID。
     */
    private String traceId;

    /**
     * 用量记录ID。
     */
    private Long usageId;

    /**
     * 模型返回的原始 usage JSON。
     */
    private String usageRaw;

    private String complexityTier;
    private String specificityCategory;
    private Double scoringConfidence;
    private Long scoringLatencyMs;
    private String scoringReason;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 影子对比相似度（0.0000~1.0000）。
     */
    private java.math.BigDecimal similarity;

    /**
     * 影子验证虚拟花费（便宜模型计价，用于预估节省计算）。
     */
    private java.math.BigDecimal costShadow;

    /**
     * 影子验证错误信息（NULL=正常，非空=嵌入失败原因）。
     */
    private String shadowError;

    /**
     * 便宜模型响应原文（错误列表预览用，前端截断100字符）。
     */
    private String cheapResponse;
}
