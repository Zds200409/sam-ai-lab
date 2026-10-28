package com.sam.membermgmt.modules.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sam.membermgmt.modules.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
