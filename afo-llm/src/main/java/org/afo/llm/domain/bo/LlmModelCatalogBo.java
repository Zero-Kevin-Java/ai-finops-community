package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmModelCatalog;

/**
 * LLM 模型目录业务对象 afo_llm_model_catalog。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmModelCatalog.class, reverseConvertGenerate = false)
public class LlmModelCatalogBo extends BaseEntity {

    /**
     * 模型ID。
     */
    @NotNull(message = "模型ID不能为空", groups = { EditGroup.class })
    private Long modelId;

    /**
     * 平台内部模型编码。
     */
    @NotBlank(message = "模型编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelCode;

    /**
     * 模型展示名称。
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String displayName;

    /**
     * 模型厂商（按模型名称自动匹配）。
     */
    private String provider;

    /**
     * 模型供应商，表示模型的提供来源。
     */
    private String supplier;

    /**
     * LiteLLM 模型名称；为空时根据 modelCode 和 protocol 幂等生成。
     */
    private String litellmModel;

    /**
     * 协议类型: openai / anthropic。
     */
    private String protocol;

    /**
     * API Key（AES-256/GCM 加密）。
     */
    private String apiKey;

    /**
     * 上游 API 端点地址。
     */
    private String apiBase;

    /**
     * 模型类型。
     */
    @NotBlank(message = "模型类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelType;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;
}
