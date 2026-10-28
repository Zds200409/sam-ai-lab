package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Data
public class MemberRegisterDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "密码至少8位，且需包含字母和数字")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private Integer gender;

    private LocalDate birthday;

    private String email;
}
