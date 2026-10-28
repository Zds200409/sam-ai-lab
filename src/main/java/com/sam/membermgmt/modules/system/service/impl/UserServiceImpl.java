package com.sam.membermgmt.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.entity.SysUser;
import com.sam.membermgmt.modules.system.entity.SysUserRole;
import com.sam.membermgmt.modules.system.mapper.SysUserMapper;
import com.sam.membermgmt.modules.system.mapper.SysUserRoleMapper;
import com.sam.membermgmt.modules.system.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserService {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void saveUser(SysUser user) {
        SysUser exist = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);
        save(user);
    }

    @Override
    @Transactional
    public void updateUser(SysUser user) {
        SysUser exist = getById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        if (!exist.getUsername().equals(user.getUsername())) {
            SysUser duplicate = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
            if (duplicate != null) {
                throw new BusinessException("用户名已存在");
            }
        }
        user.setPassword(null);
        updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除超级管理员");
        }
        user.setStatus(0);
        updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        updateById(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, Long[] roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null && roleIds.length > 0) {
            Arrays.stream(roleIds).forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
    }
}
