package org.afo.llm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.common.excel.annotation.ExcelDictFormat;
import org.afo.common.excel.convert.ExcelDictConvert;
import org.afo.llm.domain.LlmModelCatalog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * LLM 模型目录视图对象 afo_llm_model_catalog。
 *
 * @author afo
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = LlmModelCatalog.class)
public class LlmModelCatalogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID。
     */
    @ExcelProperty(value = "模型ID")
    private Long modelId;

    /**
     * 平台内部模型编码。
     */
    @ExcelProperty(value = "模型编码")
    private String modelCode;

    /**
     * 模型展示名称。
     */
    @ExcelProperty(value = "模型名称")
    private String displayName;

    /**
     * 模型厂商。
     */
    @ExcelProperty(value = "模型厂商")
    private String provider;

    /**
     * 模型供应商，表示模型的提供来源。
     */
    @ExcelProperty(value = "供应商")
    private String supplier;

    /**
     * LiteLLM 模型名称。
     */
    @ExcelProperty(value = "LiteLLM模型名称")
    private String litellmModel;

    /**
     * 协议类型: openai / anthropic。
     */
    @ExcelProperty(value = "协议类型")
    private String protocol;

    /**
     * API Key（前端展示时脱敏）。
     */
    @ExcelProperty(value = "API Key")
    private String apiKey;

    /**
     * 上游 API 端点地址。
     */
    @ExcelProperty(value = "API Base")
    private String apiBase;

    /**
     * 模型类型。
     */
    @ExcelProperty(value = "模型类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "llm_model_type")
    private String modelType;

    /**
     * 状态（0正常 1停用）。
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
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
