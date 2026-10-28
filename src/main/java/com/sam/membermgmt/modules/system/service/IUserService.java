package com.sam.membermgmt.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.system.entity.SysUser;

public interface IUserService extends IService<SysUser> {
    void saveUser(SysUser user);
    void updateUser(SysUser user);
    void deleteUser(Long id);
    void resetPassword(Long id);
    void assignRoles(Long userId, Long[] roleIds);
}
