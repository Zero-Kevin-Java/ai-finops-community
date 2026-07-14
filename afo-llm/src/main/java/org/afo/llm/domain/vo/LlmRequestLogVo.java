package org.afo.llm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.common.excel.annotation.ExcelDictFormat;
import org.afo.common.excel.convert.ExcelDictConvert;
import org.afo.llm.domain.LlmRequestLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * LLM 请求日志视图对象。
 *
 * @author afo
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = LlmRequestLog.class)
public class LlmRequestLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "请求日志ID")
    private Long requestLogId;

    @ExcelProperty(value = "请求ID")
    private String requestId;

    @ExcelProperty(value = "项目ID")
    private Long projectId;

    @ExcelProperty(value = "应用客户端ID")
    private Long clientId;

    @ExcelProperty(value = "API Key ID")
    private Long keyId;

    @ExcelProperty(value = "模型编码")
    private String modelCode;

    @ExcelProperty(value = "路由模型编码")
    private String targetModelCode;

    @ExcelProperty(value = "请求路径")
    private String requestPath;

    @ExcelProperty(value = "是否流式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String stream;

    @ExcelProperty(value = "请求状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "llm_request_status")
    private String requestStatus;

    @ExcelProperty(value = "HTTP状态码")
    private Integer httpStatus;

    @ExcelProperty(value = "耗时毫秒")
    private Long latencyMs;

    @ExcelProperty(value = "错误编码")
    private String errorCode;

    @ExcelProperty(value = "错误信息")
    private String errorMessage;

    @ExcelProperty(value = "客户端IP")
    private String clientIp;

    private String userAgent;
    private String traceId;
    private Long usageId;
    private String usageRaw;
    private String remark;

    private java.math.BigDecimal similarity;
    private java.math.BigDecimal costShadow;

    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
