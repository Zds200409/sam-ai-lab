package com.sam.membermgmt.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.modules.system.entity.SysMenu;
import com.sam.membermgmt.modules.system.entity.SysRole;
import com.sam.membermgmt.modules.system.entity.SysRoleMenu;
import com.sam.membermgmt.modules.system.entity.SysUser;
import com.sam.membermgmt.modules.system.entity.SysUserRole;
import com.sam.membermgmt.modules.system.mapper.SysMenuMapper;
import com.sam.membermgmt.modules.system.mapper.SysRoleMapper;
import com.sam.membermgmt.modules.system.mapper.SysRoleMenuMapper;
import com.sam.membermgmt.modules.system.mapper.SysUserMapper;
import com.sam.membermgmt.modules.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. 确保管理员用户存在
        SysUser admin = ensureAdminUser();

        // 2. 确保 ROLE_ADMIN 角色分配给 admin
        ensureAdminRole(admin.getId());

        // 3. 确保 ROLE_ADMIN 拥有全部菜单（自愈 data.sql 部分执行的问题）
        ensureAdminMenus();

        // 4. 确保其他角色菜单完整（含父级菜单）
        ensureRoleMenus();
    }

    private SysUser ensureAdminUser() {
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRealName("系统管理员");
            admin.setPhone("13800000000");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("默认管理员账号已创建: admin / 123456");
        } else {
            // 强制统一密码为 123456，避免与 data.sql 中管理员密码不一致
            if (!passwordEncoder.matches("123456", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("123456"));
                userMapper.updateById(admin);
                log.info("管理员密码已统一为 123456");
            } else {
                log.info("管理员账号已存在且密码一致，跳过处理");
            }
        }
        return admin;
    }

    private void ensureAdminRole(Long userId) {
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        if (adminRole == null) {
            log.warn("ROLE_ADMIN 角色不存在，请先执行 sql/data.sql");
            return;
        }
        // 检查是否已分配 ROLE_ADMIN（而非检查任意角色）
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, adminRole.getId()));
        if (count == 0) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
            log.info("已为 admin 分配角色: ROLE_ADMIN");
        } else {
            log.info("admin 已有 ROLE_ADMIN 角色，跳过分配");
        }
    }

    private void ensureAdminMenus() {
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        if (adminRole == null) return;

        List<SysMenu> allMenus = menuMapper.selectList(null);
        int added = 0;
        for (SysMenu menu : allMenus) {
            Long exists = roleMenuMapper.selectCount(
                    new LambdaQueryWrapper<SysRoleMenu>()
                            .eq(SysRoleMenu::getRoleId, adminRole.getId())
                            .eq(SysRoleMenu::getMenuId, menu.getId()));
            if (exists == 0) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(adminRole.getId());
                rm.setMenuId(menu.getId());
                roleMenuMapper.insert(rm);
                added++;
            }
        }
        if (added > 0) {
            log.info("已为 ROLE_ADMIN 补全 {} 个菜单权限", added);
        } else {
            log.info("ROLE_ADMIN 菜单权限完整，无需补全");
        }
    }

    /**
     * 确保所有角色拥有正确的菜单权限（含父级菜单，否则树形菜单无法展开）
     */
    private void ensureRoleMenus() {
        // ROLE_CASHIER: 仅收银结算（POS收银 + 订单查询 + 退换货），不含首页
        Long[][] cashierMenus = {
            {2L, 4L}, {2L, 41L}, {2L, 411L}, {2L, 42L}, {2L, 421L}
        };
        batchInsertRoleMenus(cashierMenus);

        // ROLE_MANAGER: 父级 + 首页 + 会员 + 商品 + 积分 + 统计 + 订单
        Long[][] managerMenus = {
            {3L, 1L},
            {3L, 2L}, {3L, 21L}, {3L, 211L}, {3L, 212L}, {3L, 213L}, {3L, 214L}, {3L, 22L},
            {3L, 3L}, {3L, 31L}, {3L, 32L}, {3L, 321L}, {3L, 322L}, {3L, 323L}, {3L, 33L}, {3L, 331L},
            {3L, 5L}, {3L, 51L}, {3L, 52L}, {3L, 521L},
            {3L, 6L},
            {3L, 4L}, {3L, 41L}, {3L, 411L}, {3L, 42L}, {3L, 421L}
        };
        batchInsertRoleMenus(managerMenus);

        // ROLE_DELIVERY: 仅配送管理（按 role_code 与 menu path 解析，避免硬编码 ID 产生孤儿数据）
        ensureDeliveryMenus();
    }

    private void batchInsertRoleMenus(Long[][] roleMenus) {
        for (Long[] rm : roleMenus) {
            Long exists = roleMenuMapper.selectCount(
                    new LambdaQueryWrapper<SysRoleMenu>()
                            .eq(SysRoleMenu::getRoleId, rm[0])
                            .eq(SysRoleMenu::getMenuId, rm[1]));
            if (exists == 0) {
                SysRoleMenu entity = new SysRoleMenu();
                entity.setRoleId(rm[0]);
                entity.setMenuId(rm[1]);
                roleMenuMapper.insert(entity);
            }
        }
    }

    /**
     * 按 role_code 与 menu path 解析配送角色菜单，角色或菜单不存在时跳过，
     * 避免向 sys_role_menu 写入指向不存在记录的孤儿数据。
     */
    private void ensureDeliveryMenus() {
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_DELIVERY"));
        if (role == null) {
            log.warn("ROLE_DELIVERY 角色不存在，跳过配送菜单初始化");
            return;
        }
        String[] paths = {"/delivery", "/delivery/list"};   // 配送员不含首页
        for (String path : paths) {
            SysMenu menu = menuMapper.selectOne(
                    new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, path));
            if (menu == null) {
                log.warn("配送菜单 path={} 不存在，跳过为 ROLE_DELIVERY 分配", path);
                continue;
            }
            Long exists = roleMenuMapper.selectCount(
                    new LambdaQueryWrapper<SysRoleMenu>()
                            .eq(SysRoleMenu::getRoleId, role.getId())
                            .eq(SysRoleMenu::getMenuId, menu.getId()));
            if (exists == 0) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menu.getId());
                roleMenuMapper.insert(rm);
            }
        }
    }
}
