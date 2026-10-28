package com.sam.membermgmt.modules.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.member.entity.Member;

public interface IMemberService extends IService<Member> {
    void saveMember(Member member);
    void updateMember(Member member);
    void freeze(Long id);
    void unfreeze(Long id);
    void renew(Long id);
    IPage<Member> pageWithLevel(Integer pageNum, Integer pageSize, String name, String phone, Long levelId, Integer status);
    /** 会员详情，附带当前等级名称 */
    Member getMemberDetail(Long id);
}
