package org.afo.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.afo.common.core.constant.CacheNames;
import org.afo.common.core.constant.SystemConstants;
import org.afo.common.core.domain.dto.UserDTO;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.service.UserService;
import org.afo.common.core.utils.*;
import org.afo.common.mybatis.core.page.PageQuery;
import org.afo.common.mybatis.core.page.TableDataInfo;
import org.afo.common.mybatis.helper.DataPermissionHelper;
import org.afo.common.satoken.utils.LoginHelper;
import org.afo.system.domain.SysTenant;
import org.afo.system.domain.SysUser;
import org.afo.system.domain.SysUserPost;
import org.afo.system.domain.SysUserRole;
import org.afo.system.domain.SysUserTenant;
import org.afo.system.domain.SysPost;
import org.afo.system.domain.SysRole;
import org.afo.system.domain.bo.SysUserBo;
import org.afo.system.domain.vo.SysPostVo;
import org.afo.system.domain.vo.SysRoleVo;
import org.afo.system.domain.vo.SysUserExportVo;
import org.afo.system.domain.vo.SysUserVo;
import org.afo.system.mapper.*;
import org.afo.system.service.ISysUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.afo.common.tenant.helper.TenantHelper;

import java.util.*;

/**
 * 用户 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl implements ISysUserService, UserService {

    private final SysUserMapper baseMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;

    @Override
    public TableDataInfo<SysUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        String tenantId = TenantHelper.getTenantId();
        Page<SysUserVo> page = StringUtils.isBlank(tenantId)
            ? baseMapper.selectPageUserList(pageQuery.build(), this.buildQueryWrapper(user))
            : baseMapper.selectTenantPageUserList(pageQuery.build(), this.buildTenantQueryWrapper(user, tenantId));
        markTenantCreator(tenantId, page.getRecords());
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserExportVo> selectUserExportList(SysUserBo user) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            return baseMapper.selectTenantUserExportList(this.buildTenantQueryWrapper(user, tenantId));
        }
        Map<String, Object> params = user.getParams();
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u.nick_name", user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "u.create_time", params.get("beginTime"), params.get("endTime"))
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<Long> deptIds = deptMapper.selectDeptAndChildById(user.getDeptId());
                w.in("u.dept_id", deptIds);
            }).orderByAsc("u.user_id");
        return baseMapper.selectUserExportList(wrapper);
    }

    private Wrapper<SysUser> buildQueryWrapper(SysUserBo user) {
        Map<String, Object> params = user.getParams();
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId())
            .in(StringUtils.isNotBlank(user.getUserIds()), SysUser::getUserId, StringUtils.splitTo(user.getUserIds(), Convert::toLong))
            .like(StringUtils.isNotBlank(user.getUserName()), SysUser::getUserName, user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), SysUser::getNickName, user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), SysUser::getStatus, user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), SysUser::getPhonenumber, user.getPhonenumber())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysUser::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<Long> ids = deptMapper.selectDeptAndChildById(user.getDeptId());
                w.in(SysUser::getDeptId, ids);
            }).orderByAsc(SysUser::getUserId);
        if (StringUtils.isNotBlank(user.getExcludeUserIds())) {
            wrapper.notIn(SysUser::getUserId, StringUtils.splitTo(user.getExcludeUserIds(), Convert::toLong));
        }
        return wrapper;
    }

    private Wrapper<SysUser> buildTenantQueryWrapper(SysUserBo user, String tenantId) {
        Map<String, Object> params = user.getParams();
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .eq("ut.tenant_id", tenantId)
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u.nick_name", user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "u.create_time", params.get("beginTime"), params.get("endTime"))
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<Long> ids = deptMapper.selectDeptAndChildById(user.getDeptId());
                w.in("ut.dept_id", ids);
            }).orderByAsc("u.user_id");
        if (ObjectUtil.isNotNull(user.getUserId())) {
            wrapper.eq("u.user_id", user.getUserId());
        }
        if (StringUtils.isNotBlank(user.getUserIds())) {
            wrapper.in("u.user_id", StringUtils.splitTo(user.getUserIds(), Convert::toLong));
        }
        if (StringUtils.isNotBlank(user.getExcludeUserIds())) {
            wrapper.notIn("u.user_id", StringUtils.splitTo(user.getExcludeUserIds(), Convert::toLong));
        }
        return wrapper;
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public TableDataInfo<SysUserVo> selectAllocatedList(SysUserBo user, PageQuery pageQuery) {
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .eq(StringUtils.isNotBlank(TenantHelper.getTenantId()), "ut.tenant_id", TenantHelper.getTenantId())
            .eq(ObjectUtil.isNotNull(user.getRoleId()), "r.role_id", user.getRoleId())
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .orderByAsc("u.user_id");
        Page<SysUserVo> page = baseMapper.selectAllocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public TableDataInfo<SysUserVo> selectUnallocatedList(SysUserBo user, PageQuery pageQuery) {
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(user.getRoleId());
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .eq(StringUtils.isNotBlank(TenantHelper.getTenantId()), "ut.tenant_id", TenantHelper.getTenantId())
            .and(w -> w.ne("r.role_id", user.getRoleId()).or().isNull("r.role_id"))
            .notIn(CollUtil.isNotEmpty(userIds), "u.user_id", userIds)
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .orderByAsc("u.user_id");
        Page<SysUserVo> page = baseMapper.selectUnallocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByUserName(String userName) {
        SysUser user = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, userName));
        return user == null ? null : BeanUtil.toBean(user, SysUserVo.class);
    }

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByPhonenumber(String phonenumber) {
        SysUser user = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhonenumber, phonenumber));
        return user == null ? null : BeanUtil.toBean(user, SysUserVo.class);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserById(Long userId) {
        String tenantId = TenantHelper.getTenantId();
        SysUserVo user;
        if (StringUtils.isBlank(tenantId)) {
            SysUser entity = baseMapper.selectById(userId);
            user = entity == null ? null : BeanUtil.toBean(entity, SysUserVo.class);
        } else {
            QueryWrapper<SysUser> wrapper = Wrappers.query();
            wrapper.eq("u.user_id", userId);
            wrapper.eq("u.del_flag", SystemConstants.NORMAL);
            wrapper.eq("ut.tenant_id", tenantId);
            List<SysUserVo> users = TenantHelper.ignore(() -> baseMapper.selectTenantUserList(wrapper));
            user = CollUtil.isEmpty(users) ? null : users.get(0);
        }
        if (ObjectUtil.isNull(user)) {
            return user;
        }
        user.setRoles(roleMapper.selectRolesByUserId(user.getUserId()));
        return user;
    }

    /**
     * 通过用户ID串查询用户
     *
     * @param userIds 用户ID串
     * @param deptId  部门id
     * @return 用户列表信息
     */
    @Override
    public List<SysUserVo> selectUserByIds(List<Long> userIds, Long deptId) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            QueryWrapper<SysUser> wrapper = Wrappers.query();
            wrapper.eq("u.del_flag", SystemConstants.NORMAL)
                .eq("u.status", SystemConstants.NORMAL)
                .eq("ut.tenant_id", tenantId)
                .eq(ObjectUtil.isNotNull(deptId), "ut.dept_id", deptId)
                .in(CollUtil.isNotEmpty(userIds), "u.user_id", userIds)
                .orderByAsc("u.user_id");
            return TenantHelper.ignore(() -> baseMapper.selectTenantUserList(wrapper));
        }
        return baseMapper.selectUserList(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(deptId), SysUser::getDeptId, deptId)
            .in(CollUtil.isNotEmpty(userIds), SysUser::getUserId, userIds));
    }

    /**
     * 查询用户所属角色组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(Long userId) {
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysRoleVo::getRoleName);
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(Long userId) {
        List<SysPostVo> list = postMapper.selectPostsByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysPostVo::getPostName);
    }

    /**
     * 校验用户账号是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, user.getUserName())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkPhoneUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getPhonenumber, user.getPhonenumber())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkEmailUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getEmail, user.getEmail())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserAllowed(Long userId) {
        if (ObjectUtil.isNotNull(userId) && LoginHelper.isSuperAdmin(userId)) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            QueryWrapper<SysUser> wrapper = Wrappers.query();
            wrapper.eq("u.user_id", userId)
                .eq("u.del_flag", SystemConstants.NORMAL)
                .eq("ut.tenant_id", tenantId);
            if (CollUtil.isEmpty(TenantHelper.ignore(() -> baseMapper.selectTenantUserList(wrapper)))) {
                throw new ServiceException("没有权限访问用户数据！");
            }
            return;
        }
        if (baseMapper.countUserById(userId) == 0) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUserBo user) {
        validateTenantUserBindings(user);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        // 新增用户信息
        int rows = baseMapper.insert(sysUser);
        user.setUserId(sysUser.getUserId());
        String tenantId = TenantHelper.getTenantId();
        SysUserTenant sut = new SysUserTenant();
        sut.setUserId(sysUser.getUserId());
        sut.setTenantId(tenantId);
        sut.setDeptId(user.getDeptId());
        userTenantMapper.insert(sut);
        userTenantMapper.update(null, new LambdaUpdateWrapper<SysUserTenant>()
            .set(SysUserTenant::getLastLoginTime, new Date())
            .eq(SysUserTenant::getUserId, sysUser.getUserId())
            .eq(SysUserTenant::getTenantId, tenantId));
        // 新增用户岗位关联
        insertUserPost(user, false);
        // 新增用户与角色管理
        insertUserRole(user, false);
        return rows;
    }

    /**
     * 将已有用户加入当前租户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindUserToCurrentTenant(SysUserBo user) {
        if (ObjectUtil.isNull(user.getUserId())) {
            throw new ServiceException("用户ID不能为空");
        }
        validateTenantUserBindings(user);
        SysUserTenant sut = new SysUserTenant();
        sut.setUserId(user.getUserId());
        sut.setTenantId(TenantHelper.getTenantId());
        sut.setDeptId(user.getDeptId());
        userTenantMapper.insert(sut);
        insertUserPost(user, true);
        insertUserRole(user, true);
        return 1;
    }

    /**
     * 将用户移出当前租户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeUsersFromCurrentTenant(Long[] userIds) {
        if (ArrayUtil.isEmpty(userIds)) {
            return 0;
        }
        if (ArrayUtil.contains(userIds, LoginHelper.getUserId())) {
            throw new ServiceException("当前用户请使用退出企业");
        }
        String tenantId = TenantHelper.getTenantId();
        for (Long userId : userIds) {
            if (isTenantCreator(tenantId, userId)) {
                throw new ServiceException("租户创建者不允许移出企业");
            }
            removeUserFromCurrentTenant(userId);
        }
        return userIds.length;
    }

    private void markTenantCreator(String tenantId, List<SysUserVo> users) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(users)) {
            return;
        }
        SysTenant tenant = selectTenantByTenantId(tenantId);
        if (tenant == null) {
            return;
        }
        users.forEach(user -> user.setTenantCreator(isTenantCreator(tenant, user)));
    }

    private boolean isTenantCreator(String tenantId, Long userId) {
        if (StringUtils.isBlank(tenantId) || ObjectUtil.isNull(userId)) {
            return false;
        }
        SysTenant tenant = selectTenantByTenantId(tenantId);
        SysUser entity = baseMapper.selectById(userId);
        SysUserVo user = entity == null ? null : BeanUtil.toBean(entity, SysUserVo.class);
        return isTenantCreator(tenant, user);
    }

    private boolean isTenantCreator(SysTenant tenant, SysUserVo user) {
        if (tenant == null || user == null) {
            return false;
        }
        if (ObjectUtil.equals(tenant.getCreateBy(), user.getUserId())) {
            return true;
        }
        String contactPhone = tenant.getContactPhone();
        if (StringUtils.isNotBlank(contactPhone)
            && (contactPhone.equals(user.getPhonenumber()) || contactPhone.equals(user.getUserName()))) {
            return true;
        }
        String contactUserName = tenant.getContactUserName();
        return StringUtils.isNotBlank(contactUserName)
            && (contactUserName.equals(user.getUserName()) || contactUserName.equals(user.getNickName()));
    }

    private SysTenant selectTenantByTenantId(String tenantId) {
        return TenantHelper.ignore(() -> tenantMapper.selectOne(
            new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantId, tenantId)));
    }

    /**
     * 当前用户主动退出当前租户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCurrentTenant(Long userId) {
        leaveTenant(userId, TenantHelper.getTenantId());
    }

    /**
     * 当前用户主动退出指定租户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveTenant(Long userId, String tenantId) {
        if (LoginHelper.isSuperAdmin(userId)) {
            throw new ServiceException("超级管理员不允许退出企业");
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("企业不能为空");
        }
        removeTenantMembershipAndBindings(userId, tenantId);
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUserBo user, String tenantId) {
        user.setCreateBy(0L);
        user.setUpdateBy(0L);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        sysUser.setTenantId(tenantId);
        boolean success = baseMapper.insert(sysUser) > 0;
        if (success) {
            user.setUserId(sysUser.getUserId());
            SysUserTenant sut2 = new SysUserTenant();
            sut2.setUserId(sysUser.getUserId());
            sut2.setTenantId(tenantId);
            sut2.setDeptId(user.getDeptId());
            userTenantMapper.insert(sut2);
            userTenantMapper.update(null, new LambdaUpdateWrapper<SysUserTenant>()
                .set(SysUserTenant::getLastLoginTime, new Date())
                .eq(SysUserTenant::getUserId, sysUser.getUserId())
                .eq(SysUserTenant::getTenantId, tenantId));
        }
        return success;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUserBo user) {
        validateTenantUserBindings(user);
        // 新增用户与角色管理
        insertUserRole(user, true);
        // 新增用户与岗位管理
        insertUserPost(user, true);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        sysUser.setDeptId(null);
        // 防止错误更新后导致的数据误删除
        int flag = DataPermissionHelper.ignore(() -> TenantHelper.ignore(() -> baseMapper.updateById(sysUser)));
        if (flag < 1) {
            throw new ServiceException("修改用户{}信息失败", user.getUserName());
        }
        userTenantMapper.update(null, new LambdaUpdateWrapper<SysUserTenant>()
            .set(SysUserTenant::getDeptId, user.getDeptId())
            .eq(SysUserTenant::getUserId, user.getUserId())
            .eq(SysUserTenant::getTenantId, TenantHelper.getTenantId()));
        return flag;
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds) {
        validateTenantRoles(userId, roleIds);
        insertUserRole(userId, roleIds, true);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 账号状态
     * @return 结果
     */
    @Override
    public int updateUserStatus(Long userId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getStatus, status)
                .eq(SysUser::getUserId, userId));
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Override
    public int updateUserProfile(SysUserBo user) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(ObjectUtil.isNotNull(user.getNickName()), SysUser::getNickName, user.getNickName())
                .set(SysUser::getPhonenumber, user.getPhonenumber())
                .set(SysUser::getEmail, user.getEmail())
                .set(SysUser::getSex, user.getSex())
                .eq(SysUser::getUserId, user.getUserId()));
    }

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, Long avatar) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getAvatar, avatar)
                .eq(SysUser::getUserId, userId)) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, password)
                .eq(SysUser::getUserId, userId));
    }

    /**
     * 新增用户角色信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserRole(SysUserBo user, boolean clear) {
        this.insertUserRole(user.getUserId(), user.getRoleIds(), clear);
    }

    private void validateTenantUserBindings(SysUserBo user) {
        if (StringUtils.isBlank(TenantHelper.getTenantId())) {
            return;
        }
        validateTenantDept(user.getDeptId());
        validateTenantRoles(user.getUserId(), user.getRoleIds());
        validateTenantPosts(user.getPostIds());
    }

    private void validateTenantDept(Long deptId) {
        if (ObjectUtil.isNull(deptId)) {
            return;
        }
        if (deptMapper.countDeptById(deptId) == 0) {
            throw new ServiceException("没有权限访问部门数据！");
        }
    }

    private void validateTenantRoles(Long userId, Long[] roleIds) {
        if (ArrayUtil.isEmpty(roleIds)) {
            return;
        }
        List<Long> roleList = new ArrayList<>(Arrays.asList(roleIds));
        if (!LoginHelper.isSuperAdmin(userId)) {
            roleList.remove(SystemConstants.SUPER_ADMIN_ID);
        }
        if (roleList.isEmpty()) {
            throw new ServiceException("不允许为普通用户分配超级管理员角色，请至少选择一个其他角色");
        }
        if (roleMapper.selectRoleCount(roleList) != roleList.size()) {
            throw new ServiceException("没有权限访问角色的数据");
        }
    }

    private void validateTenantPosts(Long[] postIds) {
        if (ArrayUtil.isEmpty(postIds)) {
            return;
        }
        List<Long> postList = Arrays.asList(postIds);
        if (postMapper.selectPostCount(postList) != postList.size()) {
            throw new ServiceException("没有权限访问岗位的数据");
        }
    }

    /**
     * 新增用户岗位信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserPost(SysUserBo user, boolean clear) {
        Long[] postIdArr = user.getPostIds();
        if (ArrayUtil.isEmpty(postIdArr)) {
            return;
        }
        List<Long> postIds = Arrays.asList(postIdArr);

        // 校验是否有权限操作这些岗位（含数据权限控制）
        if (postMapper.selectPostCount(postIds) != postIds.size()) {
            throw new ServiceException("没有权限访问岗位的数据");
        }

        // 是否清除旧的用户岗位绑定
        if (clear) {
            deleteCurrentTenantUserPosts(user.getUserId());
        }

        // 构建用户岗位关联列表并批量插入
        List<SysUserPost> list = StreamUtils.toList(postIds,
            postId -> {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                return up;
            });
        userPostMapper.insertBatch(list);
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     * @param clear   清除已存在的关联数据
     */
    private void insertUserRole(Long userId, Long[] roleIds, boolean clear) {
        if (ArrayUtil.isEmpty(roleIds)) {
            return;
        }

        List<Long> roleList = new ArrayList<>(Arrays.asList(roleIds));

        // 非超级管理员，禁止包含超级管理员角色
        if (!LoginHelper.isSuperAdmin(userId)) {
            roleList.remove(SystemConstants.SUPER_ADMIN_ID);
        }

        // 移除超管角色后若无剩余角色，说明仅选了超管角色且不允许分配，显式报错
        if (roleList.isEmpty()) {
            throw new ServiceException("不允许为普通用户分配超级管理员角色，请至少选择一个其他角色");
        }

        // 校验是否有权限访问这些角色（含数据权限控制）
        if (roleMapper.selectRoleCount(roleList) != roleList.size()) {
            throw new ServiceException("没有权限访问角色的数据");
        }

        // 是否清除原有绑定
        if (clear) {
            deleteCurrentTenantUserRoles(userId);
        }

        // 批量插入用户-角色关联
        List<SysUserRole> list = StreamUtils.toList(roleList,
            roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            });
        userRoleMapper.insertBatch(list);
    }

    private void removeUserFromCurrentTenant(Long userId) {
        checkUserAllowed(userId);
        checkUserDataScope(userId);
        removeTenantMembershipAndBindings(userId, TenantHelper.getTenantId());
    }

    private void removeTenantMembershipAndBindings(Long userId, String tenantId) {
        if (ObjectUtil.hasNull(userId, tenantId)) {
            return;
        }
        userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
            .eq(SysUserTenant::getUserId, userId)
            .eq(SysUserTenant::getTenantId, tenantId));
        deleteTenantUserRoles(userId, tenantId);
        deleteTenantUserPosts(userId, tenantId);
        List<SysUserTenant> tenantList = userTenantMapper.selectList(new LambdaQueryWrapper<SysUserTenant>()
            .eq(SysUserTenant::getUserId, userId)
            .orderByDesc(SysUserTenant::getLastLoginTime));
        if (CollUtil.isNotEmpty(tenantList)) {
            SysUserTenant nextTenant = tenantList.get(0);
            userTenantMapper.update(null, new LambdaUpdateWrapper<SysUserTenant>()
                .set(SysUserTenant::getLastLoginTime, new Date())
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, nextTenant.getTenantId()));
        } else {
            userTenantMapper.update(null, new LambdaUpdateWrapper<SysUserTenant>()
                .set(SysUserTenant::getLastLoginTime, null)
                .eq(SysUserTenant::getUserId, userId));
        }
    }

    private void deleteCurrentTenantUserRoles(Long userId) {
        deleteTenantUserRoles(userId, TenantHelper.getTenantId());
    }

    private void deleteCurrentTenantUserPosts(Long userId) {
        deleteTenantUserPosts(userId, TenantHelper.getTenantId());
    }

    private void deleteTenantUserRoles(Long userId, String tenantId) {
        List<Long> tenantRoleIds = TenantHelper.dynamic(tenantId, () -> StreamUtils.toList(
            roleMapper.selectList(new LambdaQueryWrapper<SysRole>().select(SysRole::getRoleId)),
            SysRole::getRoleId));
        if (CollUtil.isNotEmpty(tenantRoleIds)) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .in(SysUserRole::getRoleId, tenantRoleIds));
        }
    }

    private void deleteTenantUserPosts(Long userId, String tenantId) {
        List<Long> tenantPostIds = TenantHelper.dynamic(tenantId, () -> StreamUtils.toList(
            postMapper.selectList(new LambdaQueryWrapper<SysPost>().select(SysPost::getPostId)),
            SysPost::getPostId));
        if (CollUtil.isNotEmpty(tenantPostIds)) {
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                .eq(SysUserPost::getUserId, userId)
                .in(SysUserPost::getPostId, tenantPostIds));
        }
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId) {
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        TenantHelper.ignore(() -> userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId)));
        // 防止更新失败导致的数据删除
        int flag = baseMapper.deleteById(userId);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(userId);
            checkUserDataScope(userId);
        }
        List<Long> ids = List.of(userIds);
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, ids));
        TenantHelper.ignore(() -> userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().in(SysUserTenant::getUserId, ids)));
        // 防止更新失败导致的数据删除
        int flag = baseMapper.deleteByIds(ids);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 通过部门id查询当前部门所有用户
     *
     * @param deptId 部门ID
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserVo> selectUserListByDept(Long deptId) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            QueryWrapper<SysUser> wrapper = Wrappers.query();
            wrapper.eq("u.del_flag", SystemConstants.NORMAL)
                .eq("u.status", SystemConstants.NORMAL)
                .eq("ut.tenant_id", tenantId)
                .eq("ut.dept_id", deptId)
                .orderByAsc("u.user_id");
            return TenantHelper.ignore(() -> baseMapper.selectTenantUserList(wrapper));
        }
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysUser::getDeptId, deptId);
        lqw.orderByAsc(SysUser::getUserId);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    @Cacheable(cacheNames = CacheNames.SYS_USER_NAME, key = "#userId")
    @Override
    public String selectUserNameById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserName).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getUserName);
    }

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    @Override
    @Cacheable(cacheNames = CacheNames.SYS_NICKNAME, key = "#userId")
    public String selectNicknameById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getNickName).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getNickName);
    }

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userIds 用户ID 多个用逗号隔开
     * @return 用户昵称
     */
    @Override
    public String selectNicknameByIds(String userIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(userIds, Convert::toLong)) {
            String nickname = SpringUtils.getAopProxy(this).selectNicknameById(id);
            if (StringUtils.isNotBlank(nickname)) {
                list.add(nickname);
            }
        }
        return StringUtils.joinComma(list);
    }

    /**
     * 通过用户ID查询用户手机号
     *
     * @param userId 用户id
     * @return 用户手机号
     */
    @Override
    public String selectPhonenumberById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getPhonenumber);
    }

    /**
     * 通过用户ID查询用户邮箱
     *
     * @param userId 用户id
     * @return 用户邮箱
     */
    @Override
    public String selectEmailById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getEmail).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getEmail);
    }

    /**
     * 通过用户ID查询用户列表
     *
     * @param userIds 用户ids
     * @return 用户列表
     */
    @Override
    public List<UserDTO> selectListByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<SysUserVo> list = baseMapper.selectVoList(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getUserType, SysUser::getEmail,
                SysUser::getPhonenumber, SysUser::getSex, SysUser::getStatus,
                SysUser::getCreateTime)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .in(SysUser::getUserId, userIds));
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    /**
     * 通过角色ID查询用户ID
     *
     * @param roleIds 角色ids
     * @return 用户ids
     */
    @Override
    public List<Long> selectUserIdsByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
        return StreamUtils.toList(userRoles, SysUserRole::getUserId);
    }

    /**
     * 通过角色ID查询用户
     *
     * @param roleIds 角色ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }

        // 通过角色ID获取用户角色信息
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));

        // 获取用户ID列表
        Set<Long> userIds = StreamUtils.toSet(userRoles, SysUserRole::getUserId);

        return this.selectListByIds(new ArrayList<>(userIds));
    }

    /**
     * 通过部门ID查询用户
     *
     * @param deptIds 部门ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByDeptIds(List<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return List.of();
        }
        List<SysUserVo> list = baseMapper.selectVoList(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName, SysUser::getEmail, SysUser::getPhonenumber)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .in(SysUser::getDeptId, deptIds));
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    /**
     * 通过岗位ID查询用户
     *
     * @param postIds 岗位ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByPostIds(List<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return List.of();
        }

        // 通过岗位ID获取用户岗位信息
        List<SysUserPost> userPosts = userPostMapper.selectList(
            new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getPostId, postIds));

        // 获取用户ID列表
        Set<Long> userIds = StreamUtils.toSet(userPosts, SysUserPost::getUserId);

        return this.selectListByIds(new ArrayList<>(userIds));
    }

    /**
     * 根据用户 ID 列表查询用户昵称映射关系
     *
     * @param userIds 用户 ID 列表
     * @return Map，其中 key 为用户 ID，value 为对应的用户昵称
     */
    @Override
    public Map<Long, String> selectUserNicksByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUser> list = baseMapper.selectList(
            new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getUserId, SysUser::getNickName)
                .in(SysUser::getUserId, userIds)
        );
        return StreamUtils.toMap(list, SysUser::getUserId, SysUser::getNickName);
    }

}
