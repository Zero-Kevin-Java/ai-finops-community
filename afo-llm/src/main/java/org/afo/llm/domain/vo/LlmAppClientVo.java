package org.afo.llm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.llm.domain.LlmAppClient;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * LLM 应用客户端视图对象。
 *
 * @author afo
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = LlmAppClient.class)
public class LlmAppClientVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用客户端ID。
     */
    @ExcelProperty(value = "应用客户端ID")
    private Long clientId;

    /**
     * 项目ID。
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 项目名称。
     */
    @ExcelProperty(value = "项目名称")
    private String projectName;

    /**
     * 应用编码。
     */
    @ExcelProperty(value = "应用编码")
    private String appCode;

    /**
     * 应用名称。
     */
    @ExcelProperty(value = "应用名称")
    private String appName;

    /**
     * 应用类型。
     */
    @ExcelProperty(value = "应用类型")
    private String appType;

    /**
     * 状态。
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注。
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间。
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
