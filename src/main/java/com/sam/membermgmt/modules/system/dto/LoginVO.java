package com.sam.membermgmt.modules.system.dto;

import com.sam.membermgmt.modules.system.entity.SysMenu;
import com.sam.membermgmt.modules.system.entity.SysRole;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginVO {
    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String realName;
    private List<SysRole> roles;
    private List<SysMenu> menus;
}
