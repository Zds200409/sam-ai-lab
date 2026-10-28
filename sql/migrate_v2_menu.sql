-- ==========================================
-- v2 菜单迁移：为新增的后台页面补充菜单与角色授权
-- 新增页面：退换货管理、公告管理、操作日志
-- 执行方式：mysql -u root -p sam_member_mgmt < sql/migrate_v2_menu.sql
-- 特点：可重复执行，幂等
-- ==========================================

USE sam_member_mgmt;

-- ==================== 1. 新增菜单 ====================
-- 43 挂在「消费管理(4)」下，74/75 挂在「系统管理(7)」下
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`)
SELECT * FROM (
    SELECT 43 AS a, 4 AS b, '退换货管理' AS c, 'C' AS d, '/order/returns' AS e, 'order/ReturnList' AS f, 'order:return' AS g, '' AS h, 3 AS i, 1 AS j
    UNION ALL SELECT 74, 7, '公告管理', 'C', '/system/announcements', 'system/AnnouncementManage', 'system:announcement', '', 4, 1
    UNION ALL SELECT 75, 7, '操作日志', 'C', '/system/logs', 'system/OperationLogList', 'system:log', '', 5, 1
) x
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.id = x.a);

-- 更新已存在菜单的路径与组件（保证与前端路由一致）
UPDATE `sys_menu` SET `path` = '/order/returns', `component` = 'order/ReturnList', `menu_name` = '退换货管理'
 WHERE `id` = 43;
UPDATE `sys_menu` SET `path` = '/system/announcements', `component` = 'system/AnnouncementManage', `menu_name` = '公告管理'
 WHERE `id` = 74;
UPDATE `sys_menu` SET `path` = '/system/logs', `component` = 'system/OperationLogList', `menu_name` = '操作日志'
 WHERE `id` = 75;

-- ==================== 2. 角色授权 ====================
-- 系统管理员(1)：全部；店长(3)：全部；收银员(2)：退换货管理
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT * FROM (
    SELECT 1 AS a, 43 AS b
    UNION ALL SELECT 1, 74
    UNION ALL SELECT 1, 75
    UNION ALL SELECT 3, 43
    UNION ALL SELECT 3, 74
    UNION ALL SELECT 3, 75
    UNION ALL SELECT 2, 43
) x
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = x.a AND rm.menu_id = x.b);

-- ==================== 结果自检 ====================
SELECT '新增菜单' AS info;
SELECT m.id, m.parent_id, p.menu_name AS 父菜单, m.menu_name, m.path, m.component
  FROM `sys_menu` m LEFT JOIN `sys_menu` p ON p.id = m.parent_id
 WHERE m.id IN (43, 74, 75);

SELECT '角色授权情况' AS info;
SELECT r.role_name, COUNT(rm.menu_id) AS 菜单数
  FROM `sys_role` r LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.id
 GROUP BY r.id, r.role_name ORDER BY r.id;
