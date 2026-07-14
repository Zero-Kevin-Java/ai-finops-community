package org.afo.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * API Key 验证结果 VO（AuthFilter 响应契约）
 * 查询 afo_llm_api_key 表后映射到此 VO
 *
 * @author AI-FinOps Team
 * @since 2026-05-05
 */
@Data
public class GatewayApiKeyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private String tenantId;

    /** API Key ID */
    private String apiKeyId;

    /** 脱敏展示 */
    private String keyMasked;

    /** 状态：0=正常 1=停用 2=过期 */
    private String status;

    /** 授权模型编码列表。 */
    private String keyScope;

    /** 项目 ID */
    private Long projectId;

    /** 客户端/应用 ID */
    private Long clientId;

    /** 所属用户 ID */
    private Long ownerUserId;

    /** 所属部门 ID */
    private Long deptId;
}
