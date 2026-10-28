package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 会员忘记密码（重置密码）请求对象
 */
@Data
public class MemberResetPasswordDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "密码至少8位，且需包含字母和数字")
    private String newPassword;
}
