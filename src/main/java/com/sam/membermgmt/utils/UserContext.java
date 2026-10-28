package com.sam.membermgmt.utils;

import java.util.Collections;
import java.util.List;

public class UserContext {
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_MEMBER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> CURRENT_ROLES = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 会员端登录者身份。与后台 sys_user.id 属于不同 ID 空间，
     * 单独存放可避免 admin(id=1) 与 member(id=1) 混淆。
     */
    public static void setCurrentMemberId(Long memberId) {
        CURRENT_MEMBER_ID.set(memberId);
    }

    public static Long getCurrentMemberId() {
        return CURRENT_MEMBER_ID.get();
    }

    public static void setCurrentUsername(String username) {
        CURRENT_USERNAME.set(username);
    }

    public static String getCurrentUsername() {
        return CURRENT_USERNAME.get();
    }

    public static void setCurrentRoles(List<String> roles) {
        CURRENT_ROLES.set(roles);
    }

    public static List<String> getCurrentRoles() {
        List<String> roles = CURRENT_ROLES.get();
        return roles != null ? roles : Collections.emptyList();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_MEMBER_ID.remove();
        CURRENT_USERNAME.remove();
        CURRENT_ROLES.remove();
    }
}
