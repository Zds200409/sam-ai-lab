package com.sam.membermgmt.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sam.membermgmt.modules.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.* FROM sys_menu m INNER JOIN sys_role_menu rm ON m.id = rm.menu_id "
            + "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id WHERE ur.user_id = #{userId} "
            + "ORDER BY m.sort_order")
    List<SysMenu> selectByUserId(@Param("userId") Long userId);
}
