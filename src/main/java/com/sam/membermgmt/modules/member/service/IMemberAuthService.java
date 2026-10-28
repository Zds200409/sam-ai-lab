package com.sam.membermgmt.modules.member.service;

import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeDTO;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginDTO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.dto.MemberPasswordDTO;
import com.sam.membermgmt.modules.member.dto.MemberProfileDTO;
import com.sam.membermgmt.modules.member.dto.MemberRegisterDTO;
import com.sam.membermgmt.modules.member.dto.MemberResetPasswordDTO;

public interface IMemberAuthService {

    MemberLoginVO register(MemberRegisterDTO dto);

    MemberLoginVO login(MemberLoginDTO dto);

    MemberLoginVO getProfile(Long memberId);

    void updateProfile(Long memberId, MemberProfileDTO dto);

    void changePassword(Long memberId, MemberPasswordDTO dto);

    void resetPassword(MemberResetPasswordDTO dto);

    /**
     * 会员升级（补差价）：从当前等级升级到更高一级，差额 = 目标年费 - 当前年费。
     */
    MemberLevelUpgradeVO upgradeLevel(Long memberId, MemberLevelUpgradeDTO dto);
}
