package com.sam.membermgmt.modules.system.controller;

import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.dto.CaptchaVO;
import com.sam.membermgmt.modules.system.dto.LoginDTO;
import com.sam.membermgmt.modules.system.dto.LoginVO;
import com.sam.membermgmt.modules.system.entity.SysRole;
import com.sam.membermgmt.modules.system.entity.SysUser;
import com.sam.membermgmt.modules.system.mapper.SysUserMapper;
import com.sam.membermgmt.modules.system.service.CaptchaService;
import com.sam.membermgmt.modules.system.service.IAuthService;
import com.sam.membermgmt.modules.system.service.IRoleService;
import com.sam.membermgmt.utils.JwtUtil;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IRoleService roleService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.generate());
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        if (!captchaService.validate(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        LoginVO vo = authService.login(loginDTO);
        return Result.success("登录成功", vo);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/currentUser")
    public Result<LoginVO> currentUser() {
        LoginVO vo = authService.getCurrentUser(UserContext.getCurrentUserId());
        return Result.success(vo);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        try {
            Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
            if (!jwtUtil.validateToken(refreshToken)) {
                return Result.error(401, "Refresh Token 已过期，请重新登录");
            }
            SysUser user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == 0) {
                return Result.error(401, "用户不存在或已被禁用");
            }
            List<String> roleCodes = roleService.listByUserId(userId)
                    .stream().map(SysRole::getRoleCode).collect(Collectors.toList());
            String newToken = jwtUtil.generateToken(userId, user.getUsername(), roleCodes);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId);
            Map<String, String> result = new java.util.HashMap<>();
            result.put("token", newToken);
            result.put("refreshToken", newRefreshToken);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(401, "Refresh Token 无效，请重新登录");
        }
    }

    @Operation(summary = "修改密码")
    @PutMapping("/changePwd")
    public Result<?> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContext.getCurrentUserId(),
                body.get("oldPassword"), body.get("newPassword"));
        return Result.success("密码修改成功");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
