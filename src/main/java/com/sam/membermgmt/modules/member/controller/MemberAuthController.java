package com.sam.membermgmt.modules.member.controller;

import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeDTO;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginDTO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.dto.MemberPasswordDTO;
import com.sam.membermgmt.modules.member.dto.MemberProfileDTO;
import com.sam.membermgmt.modules.member.dto.MemberRegisterDTO;
import com.sam.membermgmt.modules.member.dto.MemberResetPasswordDTO;
import com.sam.membermgmt.modules.member.service.IMemberAuthService;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Tag(name = "会员认证")
@RestController
@RequestMapping("/api/member-auth")
public class MemberAuthController {

    @Autowired
    private IMemberAuthService memberAuthService;

    @Operation(summary = "会员注册")
    @PostMapping("/register")
    public Result<MemberLoginVO> register(@Valid @RequestBody MemberRegisterDTO dto) {
        return Result.success("注册成功", memberAuthService.register(dto));
    }

    @Operation(summary = "会员登录")
    @PostMapping("/login")
    public Result<MemberLoginVO> login(@Valid @RequestBody MemberLoginDTO dto) {
        return Result.success("登录成功", memberAuthService.login(dto));
    }

    @Operation(summary = "获取我的资料")
    @GetMapping("/profile")
    public Result<MemberLoginVO> profile() {
        return Result.success(memberAuthService.getProfile(UserContext.getCurrentMemberId()));
    }

    @Operation(summary = "修改我的资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody MemberProfileDTO dto) {
        memberAuthService.updateProfile(UserContext.getCurrentMemberId(), dto);
        return Result.success("资料已更新", null);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody MemberPasswordDTO dto) {
        memberAuthService.changePassword(UserContext.getCurrentMemberId(), dto);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody MemberResetPasswordDTO dto) {
        memberAuthService.resetPassword(dto);
        return Result.success("密码重置成功，请使用新密码登录", null);
    }

    @Operation(summary = "会员升级（补差价）")
    @PostMapping("/upgrade")
    public Result<MemberLevelUpgradeVO> upgrade(@Valid @RequestBody MemberLevelUpgradeDTO dto) {
        return Result.success("升级成功", memberAuthService.upgradeLevel(UserContext.getCurrentMemberId(), dto));
    }
}
