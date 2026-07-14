package org.afo.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * L0 简化登录请求对象
 */
@Data
public class L0LoginBody {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String tenantId = "000000";

    private String clientId;

    private String grantType = "password";
}
