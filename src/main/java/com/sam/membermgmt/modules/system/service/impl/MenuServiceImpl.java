package com.sam.membermgmt.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.modules.system.entity.SysMenu;
import com.sam.membermgmt.modules.system.mapper.SysMenuMapper;
import com.sam.membermgmt.modules.system.service.IMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements IMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
        return buildTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        List<SysMenu> userMenus = menuMapper.selectByUserId(userId);
        return buildTree(userMenus, 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .sorted(Comparator.comparing(SysMenu::getSortOrder))
                .map(menu -> {
                    List<SysMenu> children = buildTree(menus, menu.getId());
                    menu.setChildren(children.isEmpty() ? null : children);
                    return menu;
                })
                .collect(Collectors.toList());
    }
}
