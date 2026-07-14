package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmRequestLog;

/**
 * LLM 请求日志业务对象 afo_llm_request_log。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmRequestLog.class, reverseConvertGenerate = false)
public class LlmRequestLogBo extends BaseEntity {

    private Long requestLogId;
    private String requestId;
    private Long projectId;
    private Long clientId;
    private Long keyId;
    private String modelCode;
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
    private String remark;
}
