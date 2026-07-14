package org.afo.common.rabbitmq.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestLogMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String tenantId;
    private Long projectId;
    private Long clientId;
    private Long keyId;
    private String modelCode;
    private String targetModelCode;
    private String requestPath;
    private String stream;
    private String requestStatus;
    private Integer httpStatus;
    private Long latencyMs;
    private String errorCode;
    private String errorMessage;
    private String clientIp;
    private String userAgent;
    private String traceId;
    private Long usageId;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long cachedTokens;
    private Long reasoningTokens;
    private String usageRaw;
    private Instant timestamp;
}
