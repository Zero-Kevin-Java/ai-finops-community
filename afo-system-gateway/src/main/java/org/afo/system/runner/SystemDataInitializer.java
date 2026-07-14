package org.afo.system.runner;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.constant.SystemConstants;
import org.afo.common.tenant.helper.TenantHelper;
import org.afo.system.domain.SysDept;
import org.afo.system.domain.SysDictData;
import org.afo.system.domain.SysDictType;
import org.afo.system.domain.SysMenu;
import org.afo.system.domain.SysRole;
import org.afo.system.domain.SysRoleMenu;
import org.afo.system.domain.SysTenant;
import org.afo.system.domain.SysUser;
import org.afo.system.domain.SysUserRole;
import org.afo.system.mapper.SysDeptMapper;
import org.afo.system.mapper.SysDictDataMapper;
import org.afo.system.mapper.SysDictTypeMapper;
import org.afo.system.mapper.SysMenuMapper;
import org.afo.system.mapper.SysRoleMapper;
import org.afo.system.mapper.SysRoleMenuMapper;
import org.afo.system.mapper.SysTenantMapper;
import org.afo.system.mapper.SysUserMapper;
import org.afo.system.mapper.SysUserRoleMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * L0 系统数据初始化器。
 * <p>
 * 在应用启动时检查并初始化 L0 开源版必需的系统数据：
 * 默认租户、部门、角色、管理员用户、菜单、字典数据。
 * 幂等设计：如果 {@code sys_user} 表中已存在 admin 用户，跳过所有初始化。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class SystemDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String SUPER_ADMIN_ROLE_KEY = "super_admin";
    private static final Long CREATOR_ID = 1L;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    @Override
    public void run(String... args) {
        TenantHelper.dynamic(DEFAULT_TENANT_ID, () -> {
            if (adminExists()) {
                log.info("System data already initialized, skipping.");
                return;
            }
            log.info("Initializing L0 system data...");
            initTenant();
            Long deptId = initDept();
            Long roleId = initRole();
            Long userId = initAdminUser(deptId);
            initUserRole(userId, roleId);
            initMenus(roleId);
            initDictData();
            log.info("L0 system data initialization completed.");
        });
    }

    private boolean adminExists() {
        return userMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, ADMIN_USERNAME)
        ) > 0;
    }

    private void initTenant() {
        if (tenantMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantId, DEFAULT_TENANT_ID)
        ) > 0) {
            return;
        }
        SysTenant tenant = new SysTenant();
        tenant.setTenantId(DEFAULT_TENANT_ID);
        tenant.setCompanyName("Default Tenant");
        tenant.setContactUserName("Admin");
        tenant.setStatus(SystemConstants.NORMAL);
        tenant.setAccountCount(-1L);
        tenant.setCreateBy(CREATOR_ID);
        tenant.setCreateTime(new Date());
        tenant.setUpdateBy(CREATOR_ID);
        tenant.setUpdateTime(new Date());
        tenant.setDelFlag(SystemConstants.NORMAL);
        tenantMapper.insert(tenant);
    }

    private Long initDept() {
        if (deptMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, "AIFinOps")
        ) > 0) {
            return deptMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getDeptName, "AIFinOps")
            ).getDeptId();
        }
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setDeptName("AIFinOps");
        dept.setOrderNum(0);
        dept.setStatus(SystemConstants.NORMAL);
        dept.setAncestors(SystemConstants.ROOT_DEPT_ANCESTORS);
        dept.setCreateBy(CREATOR_ID);
        dept.setCreateDept(SystemConstants.DEFAULT_DEPT_ID);
        dept.setCreateTime(new Date());
        dept.setUpdateBy(CREATOR_ID);
        dept.setUpdateTime(new Date());
        dept.setDelFlag(SystemConstants.NORMAL);
        deptMapper.insert(dept);
        return dept.getDeptId();
    }

    private Long initRole() {
        if (roleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SUPER_ADMIN_ROLE_KEY)
        ) > 0) {
            return roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleKey, SUPER_ADMIN_ROLE_KEY)
            ).getRoleId();
        }
        SysRole role = new SysRole();
        role.setRoleName("超级管理员");
        role.setRoleKey(SUPER_ADMIN_ROLE_KEY);
        role.setRoleSort(1);
        role.setDataScope("1");
        role.setMenuCheckStrictly(false);
        role.setDeptCheckStrictly(false);
        role.setStatus(SystemConstants.NORMAL);
        role.setCreateBy(CREATOR_ID);
        role.setCreateDept(SystemConstants.DEFAULT_DEPT_ID);
        role.setCreateTime(new Date());
        role.setUpdateBy(CREATOR_ID);
        role.setUpdateTime(new Date());
        role.setDelFlag(SystemConstants.NORMAL);
        roleMapper.insert(role);
        return role.getRoleId();
    }

    private Long initAdminUser(Long deptId) {
        SysUser user = new SysUser();
        user.setUserName(ADMIN_USERNAME);
        user.setNickName("Admin");
        user.setUserType("sys_user");
        user.setPassword(BCrypt.hashpw(ADMIN_PASSWORD));
        user.setDeptId(deptId);
        user.setStatus(SystemConstants.NORMAL);
        user.setCreateBy(CREATOR_ID);
    user.setCreateDept(deptId);
        user.setCreateTime(new Date());
        user.setUpdateBy(CREATOR_ID);
        user.setUpdateTime(new Date());
        user.setDelFlag(SystemConstants.NORMAL);
        userMapper.insert(user);
        return user.getUserId();
    }

    private void initUserRole(Long userId, Long roleId) {
        if (userRoleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId)
        ) > 0) {
            return;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    private void initMenus(Long roleId) {
        if (menuMapper.selectCount(null) > 0) {
            bindAllMenusToRole(roleId);
            return;
        }

        // L0 一级目录
        Long systemDirId = createMenu(null, 1L, 0, "M", "系统管理", "system", "system", "", "system:all", "ep:setting", 0);
        Long gatewayDirId = createMenu(null, 2L, 0, "M", "网关管理", "gateway", "gateway", "", "gateway:all", "ep:connection", 1);
        Long llmDirId = createMenu(null, 3L, 0, "M", "LLM配置", "llm", "llm", "", "llm:all", "ep:data-board", 2);

        // 系统管理子菜单
        Long userMenuId = createMenu(systemDirId, 100L, 1, "C", "用户管理", "user", "system/user/index", "", "system:user:list", "", 0);
        Long roleMenuId = createMenu(systemDirId, 101L, 1, "C", "角色管理", "role", "system/role/index", "", "system:role:list", "", 1);
        Long menuMenuId = createMenu(systemDirId, 102L, 1, "C", "菜单管理", "menu", "system/menu/index", "", "system:menu:list", "", 2);
        Long deptMenuId = createMenu(systemDirId, 103L, 1, "C", "部门管理", "dept", "system/dept/index", "", "system:dept:list", "", 3);

        // 网关管理子菜单
        createMenu(gatewayDirId, 200L, 1, "C", "API Key", "apikey", "gateway/apikey/index", "", "gateway:apikey:list", "", 0);
        createMenu(gatewayDirId, 201L, 1, "C", "路由规则", "routing", "gateway/routing/index", "", "gateway:routing:list", "", 1);
        createMenu(gatewayDirId, 202L, 1, "C", "白名单", "whitelist", "gateway/whitelist/index", "", "gateway:whitelist:list", "", 2);
        createMenu(gatewayDirId, 203L, 1, "C", "模型准入", "model-access", "gateway/model-access/index", "", "gateway:model-access:list", "", 3);

        // LLM配置子菜单
        createMenu(llmDirId, 300L, 1, "C", "模型目录", "model-catalog", "llm/model-catalog/index", "", "llm:model-catalog:list", "", 0);
        createMenu(llmDirId, 301L, 1, "C", "Provider", "provider", "llm/provider/index", "", "llm:provider:list", "", 1);

        bindAllMenusToRole(roleId);
    }

    private Long createMenu(Long parentId, Long menuId, int orderNum, String menuType,
                            String menuName, String path, String component, String queryParam,
                            String perms, String icon, int sortIndex) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.setMenuName(menuName);
        menu.setOrderNum(orderNum);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setQueryParam(queryParam);
        menu.setIsFrame(SystemConstants.NO_FRAME);
        menu.setIsCache("0");
        menu.setMenuType(menuType);
        menu.setVisible(SystemConstants.NORMAL);
        menu.setStatus(SystemConstants.NORMAL);
        menu.setPerms(perms);
        menu.setIcon(icon);
        menu.setCreateBy(CREATOR_ID);
        menu.setCreateTime(new Date());
        menu.setUpdateBy(CREATOR_ID);
        menu.setUpdateTime(new Date());
        menuMapper.insert(menu);
        return menuId;
    }

    private void bindAllMenusToRole(Long roleId) {
        var menus = menuMapper.selectList(null);
        if (menus == null || menus.isEmpty()) {
            return;
        }
        for (SysMenu menu : menus) {
            if (roleMenuMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, roleId)
                    .eq(SysRoleMenu::getMenuId, menu.getMenuId())
            ) == 0) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menu.getMenuId());
                roleMenuMapper.insert(rm);
            }
        }
    }

    private void initDictData() {
        if (dictTypeMapper.selectCount(null) > 0) {
            return;
        }

        // 系统内置字典：sys_normal_disable
        SysDictType normalDisable = createDictType("sys_normal_disable", "系统开关", 0);
        createDictData(normalDisable.getDictId(), "sys_normal_disable", "正常", "0", "default", 0);
        createDictData(normalDisable.getDictId(), "sys_normal_disable", "停用", "1", "warning", 1);

        // 系统内置字典：sys_show_hide
        SysDictType showHide = createDictType("sys_show_hide", "菜单状态", 1);
        createDictData(showHide.getDictId(), "sys_show_hide", "显示", "0", "primary", 0);
        createDictData(showHide.getDictId(), "sys_show_hide", "隐藏", "1", "danger", 1);

        // 系统内置字典：sys_yes_no
        SysDictType yesNo = createDictType("sys_yes_no", "系统是否", 2);
        createDictData(yesNo.getDictId(), "sys_yes_no", "是", "Y", "primary", 0);
        createDictData(yesNo.getDictId(), "sys_yes_no", "否", "N", "danger", 1);

        // 业务字典：sys_user_sex
        SysDictType userSex = createDictType("sys_user_sex", "用户性别", 3);
        createDictData(userSex.getDictId(), "sys_user_sex", "男", "0", "", 0);
        createDictData(userSex.getDictId(), "sys_user_sex", "女", "1", "", 1);
        createDictData(userSex.getDictId(), "sys_user_sex", "未知", "2", "", 2);
    }

    private SysDictType createDictType(String dictType, String dictName, int sort) {
        SysDictType entity = new SysDictType();
        entity.setDictName(dictName);
        entity.setDictType(dictType);
        entity.setCreateBy(CREATOR_ID);
        entity.setCreateTime(new Date());
        entity.setUpdateBy(CREATOR_ID);
        entity.setUpdateTime(new Date());
        dictTypeMapper.insert(entity);
        return entity;
    }

    private void createDictData(Long dictTypeId, String dictType, String label, String value,
                                String cssClass, int sort) {
        SysDictData entity = new SysDictData();
        entity.setDictSort(sort);
        entity.setDictLabel(label);
        entity.setDictValue(value);
        entity.setDictType(dictType);
        entity.setCssClass(cssClass);
        entity.setIsDefault(SystemConstants.YES);
        entity.setCreateBy(CREATOR_ID);
        entity.setCreateTime(new Date());
        entity.setUpdateBy(CREATOR_ID);
        entity.setUpdateTime(new Date());
        dictDataMapper.insert(entity);
    }
}
