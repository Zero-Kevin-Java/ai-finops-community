package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * LLM 模型目录对象 afo_llm_model_catalog。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_model_catalog")
public class LlmModelCatalog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID。
     */
    @TableId(value = "model_id")
    private Long modelId;

    /**
     * 平台内部模型编码。
     */
    private String modelCode;

    /**
     * 模型展示名称。
     */
    private String displayName;

    /**
     * 模型厂商。
     */
    private String provider;

    /**
     * 模型供应商，表示模型的提供来源。
     */
    private String supplier;

    /**
     * LiteLLM 模型名称。
     */
    private String litellmModel;

    /**
     * 模型类型。
     */
    private String modelType;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 协议类型: openai / anthropic。
     */
    private String protocol;

    /**
     * API Key（AES-256/GCM 加密存储）。
     */
    private String apiKey;

    /**
     * 上游 API 端点地址。
     */
    private String apiBase;
}
