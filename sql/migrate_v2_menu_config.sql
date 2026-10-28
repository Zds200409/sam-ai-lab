-- ==========================================
-- v2 菜单迁移（补充）：参数配置菜单与角色授权
-- 执行方式：mysql -u root -p sam_member_mgmt < sql/migrate_v2_menu_config.sql
-- 特点：可重复执行，幂等
-- ==========================================
USE sam_member_mgmt;

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`)
SELECT 76, 7, '参数配置', 'C', '/system/configs', 'system/ConfigManage', 'system:config', '', 6, 1
 WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.id = 76);

UPDATE `sys_menu` SET `path` = '/system/configs', `component` = 'system/ConfigManage', `menu_name` = '参数配置'
 WHERE `id` = 76;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT * FROM (SELECT 1 AS a, 76 AS b UNION ALL SELECT 3, 76) x
 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = x.a AND rm.menu_id = x.b);

SELECT m.id, m.menu_name, m.path, m.component FROM `sys_menu` m WHERE m.id = 76;
SELECT r.role_name, COUNT(rm.menu_id) AS 菜单数
  FROM `sys_role` r LEFT JOIN `sys_role_menu` rm ON rm.role_id = r.id
 GROUP BY r.id, r.role_name ORDER BY r.id;
