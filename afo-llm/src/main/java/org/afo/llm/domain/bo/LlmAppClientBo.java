package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmAppClient;

/**
 * LLM 应用客户端业务对象 afo_llm_app_client。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmAppClient.class, reverseConvertGenerate = false)
public class LlmAppClientBo extends BaseEntity {

    /**
     * 应用客户端ID。
     */
    @NotNull(message = "应用客户端ID不能为空", groups = { EditGroup.class })
    private Long clientId;

    /**
     * 项目ID。
     */
    @NotNull(message = "项目ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long projectId;

    /**
     * 应用编码。
     */
    @NotBlank(message = "应用编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appCode;

    /**
     * 应用名称。
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appName;

    /**
     * 应用类型（server/web/mobile/internal）。
     */
    private String appType;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;
}
