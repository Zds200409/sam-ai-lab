package com.sam.membermgmt.modules.system.service.impl;

import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.dto.LoginDTO;
import com.sam.membermgmt.modules.system.dto.LoginVO;
import com.sam.membermgmt.modules.system.entity.SysRole;
import com.sam.membermgmt.modules.system.entity.SysUser;
import com.sam.membermgmt.modules.system.mapper.SysUserMapper;
import com.sam.membermgmt.modules.system.service.IAuthService;
import com.sam.membermgmt.modules.system.service.IMenuService;
import com.sam.membermgmt.modules.system.service.IRoleService;
import com.sam.membermgmt.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IMenuService menuService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        List<String> roleCodes = roleService.listByUserId(user.getId())
                .stream().map(SysRole::getRoleCode).collect(java.util.stream.Collectors.toList());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roleService.listByUserId(user.getId()))
                .menus(menuService.getMenuTreeByUserId(user.getId()))
                .build();
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public LoginVO getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return LoginVO.builder()
                .token(null)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roleService.listByUserId(userId))
                .menus(menuService.getMenuTreeByUserId(userId))
                .build();
    }
}
