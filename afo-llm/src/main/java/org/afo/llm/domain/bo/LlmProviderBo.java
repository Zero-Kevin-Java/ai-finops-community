package org.afo.llm.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.core.validate.AddGroup;
import org.afo.common.core.validate.EditGroup;
import org.afo.common.mybatis.core.domain.BaseEntity;
import org.afo.llm.domain.LlmProvider;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = LlmProvider.class, reverseConvertGenerate = false)
public class LlmProviderBo extends BaseEntity {

    @NotNull(message = "厂商ID不能为空", groups = { EditGroup.class })
    private Long providerId;

    @NotBlank(message = "厂商名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String providerName;

    private String logoSlug;
    private String modelPrefixes;
    private String status;
    private Integer sortOrder;
    private String remark;
}
