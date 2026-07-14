package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmProject;

/**
 * LLM 项目业务对象 afo_llm_project。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmProject.class, reverseConvertGenerate = false)
public class LlmProjectBo extends BaseEntity {

    /**
     * 项目ID。
     */
    @NotNull(message = "项目ID不能为空", groups = { EditGroup.class })
    private Long projectId;

    /**
     * 项目编码。
     */
    @NotBlank(message = "项目编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String projectCode;

    /**
     * 项目名称。
     */
    @NotBlank(message = "项目名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String projectName;

    /**
     * 项目负责人用户ID。
     */
    @NotNull(message = "负责人不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long ownerUserId;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 备注。
     */
    private String remark;
}
