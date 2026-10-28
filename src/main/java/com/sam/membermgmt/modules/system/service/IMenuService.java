package com.sam.membermgmt.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.system.entity.SysMenu;

import java.util.List;

public interface IMenuService extends IService<SysMenu> {
    List<SysMenu> getMenuTree();
    List<SysMenu> getMenuTreeByUserId(Long userId);
}
