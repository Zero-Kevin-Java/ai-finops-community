package org.afo.system.domain.vo.auth;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * L0 简化登录令牌响应
 */
@Data
public class L0LoginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;

    private String refreshToken;

    private Long expireIn;

    private Long refreshExpireIn;

    private String clientId;
}
