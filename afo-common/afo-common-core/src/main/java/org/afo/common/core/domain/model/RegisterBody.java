package org.afo.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterBody extends LoginBody {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = 2, max = 30, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Length(max = 30, message = "昵称长度不能超过30个字符")
    private String nickName;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = 5, max = 30, message = "{user.password.length.valid}")
//    @Pattern(regexp = RegexConstants.PASSWORD, message = "{user.password.format.valid}")
    private String password;

    /**
     * 公司名称
     */
    @Length(max = 30, message = "公司名称长度不能超过30个字符")
    private String companyName;

    /**
     * 用户类型
     */
    private String userType;

}
