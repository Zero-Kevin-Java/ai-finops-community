package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * LLM 项目对象 afo_llm_project。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_project")
public class LlmProject extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID。
     */
    @TableId(value = "project_id")
    private Long projectId;

    /**
     * 项目编码。
     */
    private String projectCode;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 项目负责人用户ID。
     */
    private Long ownerUserId;

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
}
