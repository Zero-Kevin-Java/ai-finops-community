package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * LLM 应用客户端对象 afo_llm_app_client。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_app_client")
public class LlmAppClient extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用客户端ID。
     */
    @TableId(value = "client_id")
    private Long clientId;

    /**
     * 项目ID。
     */
    private Long projectId;

    /**
     * 应用编码。
     */
    private String appCode;

    /**
     * 应用名称。
     */
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
     * 删除标志（0代表存在 1代表删除）。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注。
     */
    private String remark;
}
