package com.sam.membermgmt.modules.system.dto;

import lombok.Data;

/**
 * 图形验证码返回对象
 */
@Data
public class CaptchaVO {

    /** 验证码唯一标识，登录时与验证码一并提交 */
    private String captchaKey;

    /** 验证码图片（Base64 编码，前端可直接用于 img 的 src） */
    private String captchaImage;
}
