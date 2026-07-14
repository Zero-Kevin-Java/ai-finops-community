package org.afo.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.constant.SystemConstants;
import org.afo.common.core.domain.R;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.satoken.utils.LoginHelper;
import org.afo.system.domain.bo.L0LoginBody;
import org.afo.system.domain.vo.SysUserVo;
import org.afo.system.domain.vo.auth.L0LoginVo;
import org.afo.system.service.ISysUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * L0 简化认证控制器
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ISysUserService userService;

    @PostMapping("/login")
    public R<L0LoginVo> login(@Validated @RequestBody L0LoginBody body) {
        SysUserVo user = userService.selectUserByUserName(body.getUsername());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (!SystemConstants.NORMAL.equals(user.getStatus())) {
            throw new ServiceException("用户已被停用");
        }
        if (!BCrypt.checkpw(body.getPassword(), user.getPassword())) {
            throw new ServiceException("密码错误");
        }

        SaLoginParameter param = new SaLoginParameter()
            .setExtra(LoginHelper.CLIENT_KEY, body.getClientId())
            .setExtra("userId", user.getUserId())
            .setExtra("userName", user.getUserName())
            .setExtra("tenantId", user.getTenantId());
        StpUtil.login(user.getUserId(), param);

        L0LoginVo vo = new L0LoginVo();
        vo.setAccessToken(StpUtil.getTokenValue());
        vo.setExpireIn(StpUtil.getTokenTimeout());
        vo.setClientId(body.getClientId());
        return R.ok(vo);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }
}
