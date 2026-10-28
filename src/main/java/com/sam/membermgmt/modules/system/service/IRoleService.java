package com.sam.membermgmt.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.system.entity.SysRole;

import java.util.List;

public interface IRoleService extends IService<SysRole> {
    void assignMenus(Long roleId, Long[] menuIds);

    /** 查询某角色已分配的菜单ID集合，用于前端回显 */
    List<Long> getMenuIds(Long roleId);

    List<SysRole> listByUserId(Long userId);
}
