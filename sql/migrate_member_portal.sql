-- ==========================================
-- 会员端 + 配送员 增量迁移脚本
-- 适用对象：已经执行过 schema.sql 与 data.sql 的存量数据库
-- 执行方式：mysql -u root -p sam_member_mgmt < sql/migrate_member_portal.sql
-- 特点：可重复执行，幂等，不会破坏已有会员与订单数据
-- ==========================================

USE sam_member_mgmt;

-- ==================== 1. member 表增加登录密码字段 ====================
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，改用动态 SQL 判断是否已存在
SET @dbName := DATABASE();
SET @colExists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbName
      AND TABLE_NAME = 'member'
      AND COLUMN_NAME = 'password'
);
SET @alterSql := IF(@colExists = 0,
    'ALTER TABLE `member` ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT ''会员端登录密码(BCrypt)，后台建档未设密码时为空'' AFTER `phone`',
    'SELECT ''member.password 已存在，跳过'' AS info');
PREPARE stmt FROM @alterSql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 2. 补充角色：配送员 与 会员 ====================
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`) VALUES
(4, '配送员', 'ROLE_DELIVERY', '负责订单配送与配送状态更新'),
(5, '会员',   'ROLE_MEMBER',   '门店会员，可查询个人消费记录与积分并兑换权益')
ON DUPLICATE KEY UPDATE
    `role_name`   = VALUES(`role_name`),
    `role_code`   = VALUES(`role_code`),
    `description` = VALUES(`description`);

-- ==================== 3. 补充配送菜单（父目录 8 与 子菜单 81） ====================
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(8,  0, '配送管理', 'M', '/delivery',       '',                        '',              'Van', 8),
(81, 8, '配送订单', 'C', '/delivery/list',  'delivery/DeliveryList',  'delivery:list', '',    1)
ON DUPLICATE KEY UPDATE
    `parent_id`  = VALUES(`parent_id`),
    `menu_name`  = VALUES(`menu_name`),
    `menu_type`  = VALUES(`menu_type`),
    `path`       = VALUES(`path`),
    `component`  = VALUES(`component`),
    `perms`      = VALUES(`perms`),
    `icon`       = VALUES(`icon`),
    `sort_order` = VALUES(`sort_order`);

-- ==================== 4. 补充配送员账号 ====================
-- 密码与 admin 一致，均为 123456 的 BCrypt 散列（强度 10）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `status`) VALUES
(4, 'delivery01', '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG', '配送员小赵', '13800000003', 1)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ==================== 5. 账号与角色关联 ====================
-- 先清理重复关联（历史版本无唯一键可能产生重复）
DELETE t1 FROM `sys_user_role` t1
JOIN `sys_user_role` t2
  ON t1.user_id = t2.user_id AND t1.role_id = t2.role_id AND t1.id > t2.id;

-- 补充唯一键（幂等，已存在则跳过）
SET @ukExists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @dbName AND TABLE_NAME = 'sys_user_role' AND INDEX_NAME = 'uk_user_role'
);
SET @ukSql := IF(@ukExists = 0,
    'ALTER TABLE `sys_user_role` ADD UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)',
    'SELECT ''sys_user_role.uk_user_role 已存在，跳过'' AS info');
PREPARE stmtUk FROM @ukSql;
EXECUTE stmtUk;
DEALLOCATE PREPARE stmtUk;

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(4, 4)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ==================== 6. 菜单权限分配 ====================
-- 先清理 DataInitializer 可能已写入的孤儿记录（角色或菜单曾不存在时产生的脏数据）
DELETE FROM `sys_role_menu`
WHERE (`role_id` = 4 AND `menu_id` NOT IN (1, 8, 81))
   OR (`role_id` NOT IN (SELECT `id` FROM `sys_role`) )
   OR (`menu_id` NOT IN (SELECT `id` FROM `sys_menu`));

-- 管理员：补齐配送管理
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 8), (1, 81)
ON DUPLICATE KEY UPDATE `menu_id` = VALUES(`menu_id`);

-- 配送员：配送管理(父) + 配送订单（不分配首页，首页依赖全局统计，配送员无权限）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 8), (4, 81)
ON DUPLICATE KEY UPDATE `menu_id` = VALUES(`menu_id`);

-- ==================== 7. 为已有会员补全会员端登录密码（可选） ====================
-- 存量会员 password 为 NULL 时无法登录会员端，需各自注册或在后台重置。
-- 如需批量给所有存量会员设置初始密码 123456，取消下面这段注释后再执行一次：
--
-- UPDATE `member`
-- SET `password` = '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG'
-- WHERE `password` IS NULL OR `password` = '';

-- ==================== 8. 权益表增加兑换积分字段 ====================
SET @colExists2 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbName
      AND TABLE_NAME = 'member_benefit'
      AND COLUMN_NAME = 'points_cost'
);
SET @alterSql2 := IF(@colExists2 = 0,
    'ALTER TABLE `member_benefit` ADD COLUMN `points_cost` INT DEFAULT 0 COMMENT ''兑换所需积分，0 表示不可积分兑换'' AFTER `benefit_value`',
    'SELECT ''member_benefit.points_cost 已存在，跳过'' AS info');
PREPARE stmt2 FROM @alterSql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ==================== 9. 补充可积分兑换的权益（幂等，按名称去重） ====================
INSERT INTO `member_benefit` (`level_id`, `benefit_name`, `benefit_type`, `benefit_value`, `points_cost`, `description`) VALUES
(2, '10元无门槛券', 'GIFT', '10', 500, '积分兑换10元无门槛优惠券'),
(2, '免费停车月卡', 'GIFT', 'parking-month', 1200, '积分兑换免费停车月卡'),
(3, '50元无门槛券', 'GIFT', '50', 2000, '积分兑换50元无门槛优惠券'),
(3, '免费停车月卡', 'GIFT', 'parking-month', 1000, '积分兑换免费停车月卡')
ON DUPLICATE KEY UPDATE
    `points_cost` = VALUES(`points_cost`),
    `description` = VALUES(`description`);

-- ==================== 10. 订单表 operator_id 改可空（会员自助下单无收银员） ====================
ALTER TABLE `order`
    MODIFY COLUMN `operator_id` BIGINT DEFAULT NULL COMMENT '收银员ID 空=会员自助下单';

-- ==================== 执行结果自检 ====================
SELECT '角色' AS 检查项, `id`, `role_code`, `role_name` FROM `sys_role`
UNION ALL
SELECT '菜单', `id`, `menu_type`, `menu_name` FROM `sys_menu` WHERE `id` IN (8, 81)
UNION ALL
SELECT '账号', `id`, `username`, `real_name` FROM `sys_user` WHERE `username` = 'delivery01';
