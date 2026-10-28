package com.sam.membermgmt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberStatusEnum {
    FROZEN(0, "已冻结"),
    ACTIVE(1, "正常"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String desc;

    public static String getDesc(int code) {
        for (MemberStatusEnum e : values()) {
            if (e.code == code) return e.desc;
        }
        return "未知";
    }
}
