package org.afo.llm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.common.excel.annotation.ExcelDictFormat;
import org.afo.common.excel.convert.ExcelDictConvert;
import org.afo.common.translation.annotation.Translation;
import org.afo.common.translation.constant.TransConstant;
import org.afo.llm.domain.LlmApiKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * LLM 业务 API Key 视图对象。
 *
 * @author afo
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = LlmApiKey.class)
public class LlmApiKeyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API Key ID。
     */
    @ExcelProperty(value = "API Key ID")
    private Long keyId;

    /**
     * 应用客户端ID。
     */
    @ExcelProperty(value = "应用客户端ID")
    private Long clientId;

    /**
     * 应用名称。
     */
    @ExcelProperty(value = "应用名称")
    private String appName;

    /**
     * 所属用户ID。
     */
    @ExcelProperty(value = "所属用户ID")
    private Long ownerUserId;

    /**
     * 所属用户。
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "ownerUserId")
    @ExcelProperty(value = "所属用户")
    private String ownerUserName;

    /**
     * Key 名称。
     */
    @ExcelProperty(value = "Key名称")
    private String keyName;

    /**
     * API Key（后端脱敏）。
     */
    @ExcelProperty(value = "API Key")
    private String keyPrefix;

    /**
     * 授权模型编码。
     */
    @ExcelProperty(value = "授权模型")
    private String keyScope;

    /**
     * 过期时间。
     */
    @ExcelProperty(value = "过期时间")
    private Date expireTime;

    /**
     * 最后使用时间。
     */
    @ExcelProperty(value = "最后使用时间")
    private Date lastUsedTime;

    /**
     * 状态。
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "llm_key_status")
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
