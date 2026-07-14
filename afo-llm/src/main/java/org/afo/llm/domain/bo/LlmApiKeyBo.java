package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmApiKey;

import java.util.Date;

/**
 * LLM 业务 API Key 业务对象 afo_llm_api_key。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmApiKey.class, reverseConvertGenerate = false)
public class LlmApiKeyBo extends BaseEntity {

    /**
     * API Key ID。
     */
    @NotNull(message = "API Key ID不能为空", groups = { EditGroup.class })
    private Long keyId;

    /**
     * 应用客户端ID。
     */
    @NotNull(message = "应用客户端ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long clientId;

    /**
     * 所属用户ID。
     */
    @NotNull(message = "所属用户不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long ownerUserId;

    /**
     * Key 名称。
     */
    @NotBlank(message = "Key名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String keyName;

    /**
     * API Key 查询条件，对应已保存的定位前缀。
     */
    private String keyPrefix;

    /**
     * 授权模型编码，多个用逗号分隔，* 表示全部模型。
     */
    @NotBlank(message = "授权模型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String keyScope;

    /**
     * 过期时间。
     */
    private Date expireTime;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;
}
