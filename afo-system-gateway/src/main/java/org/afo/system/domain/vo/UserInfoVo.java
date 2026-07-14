package org.afo.system.domain.vo;

import lombok.Data;

import java.util.Set;

/**
 * 登录用户信息
 *
 * @author Michelle.Chung
 */
@Data
public class UserInfoVo {

    /**
     * 用户基本信息
     */
    private SysUserVo user;

    /**
     * 菜单权限
     */
    private Set<String> permissions;

    /**
     * 角色权限
     */
    private Set<String> roles;

    /**
     * 当前租户公司名称
     */
    private String companyName;

    /**
     * 当前租户套餐名称
     */
    private String packageName;

}
