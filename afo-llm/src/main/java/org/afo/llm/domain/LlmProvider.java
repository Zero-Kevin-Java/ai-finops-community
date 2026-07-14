package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * LLM 厂商对象 afo_llm_provider。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_provider")
public class LlmProvider extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "provider_id")
    private Long providerId;

    private String providerName;
    private String logoSlug;
    private String modelPrefixes;
    private String status;
    private Integer sortOrder;
    private String remark;

    @TableLogic
    private String delFlag;
}
