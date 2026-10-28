package com.sam.membermgmt.modules.system.service;

import com.sam.membermgmt.modules.system.dto.LoginDTO;
import com.sam.membermgmt.modules.system.dto.LoginVO;

public interface IAuthService {
    LoginVO login(LoginDTO loginDTO);
    LoginVO getCurrentUser(Long userId);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
