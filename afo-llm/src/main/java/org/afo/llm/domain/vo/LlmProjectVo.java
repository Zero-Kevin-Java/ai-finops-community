package org.afo.llm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.common.excel.annotation.ExcelDictFormat;
import org.afo.common.excel.convert.ExcelDictConvert;
import org.afo.common.translation.annotation.Translation;
import org.afo.common.translation.constant.TransConstant;
import org.afo.llm.domain.LlmProject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * LLM 项目视图对象 afo_llm_project。
 *
 * @author afo
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = LlmProject.class)
public class LlmProjectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID。
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 项目编码。
     */
    @ExcelProperty(value = "项目编码")
    private String projectCode;

    /**
     * 项目名称。
     */
    @ExcelProperty(value = "项目名称")
    private String projectName;

    /**
     * 项目负责人用户ID。
     */
    @ExcelProperty(value = "负责人用户ID")
    private Long ownerUserId;

    /**
     * 项目负责人。
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "ownerUserId")
    @ExcelProperty(value = "负责人")
    private String ownerUserName;

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
