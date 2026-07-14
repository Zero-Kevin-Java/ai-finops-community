package org.afo.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 用户租户关系 sys_user_tenant
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_tenant")
public class SysUserTenant extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 当前租户部门ID
     */
    private Long deptId;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
