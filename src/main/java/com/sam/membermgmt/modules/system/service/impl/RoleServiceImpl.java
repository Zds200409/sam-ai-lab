package com.sam.membermgmt.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.entity.SysRole;
import com.sam.membermgmt.modules.system.entity.SysRoleMenu;
import com.sam.membermgmt.modules.system.mapper.SysRoleMapper;
import com.sam.membermgmt.modules.system.mapper.SysRoleMenuMapper;
import com.sam.membermgmt.modules.system.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements IRoleService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    public List<SysRole> listByUserId(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper
                .selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, Long[] menuIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null && menuIds.length > 0) {
            Arrays.stream(menuIds).forEach(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            });
        }
    }
}
