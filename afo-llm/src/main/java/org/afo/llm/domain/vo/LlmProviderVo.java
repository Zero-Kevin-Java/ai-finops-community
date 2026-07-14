package org.afo.llm.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.afo.llm.domain.LlmProvider;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = LlmProvider.class)
public class LlmProviderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long providerId;
    private String providerName;
    private String logoSlug;
    private String modelPrefixes;
    private String status;
    private Integer sortOrder;
    private String remark;
    private Date createTime;
}
