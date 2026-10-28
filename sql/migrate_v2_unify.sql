-- ==========================================
-- v2 统一迁移：约束与字段补齐
-- 目的：使数据库结构与毕业论文的功能描述一致（四方一致改造 - 数据库侧）
-- 适用对象：已执行 schema.sql / data.sql / migrate_member_portal.sql 的存量数据库
-- 执行方式：mysql -u root -p sam_member_mgmt < sql/migrate_v2_unify.sql
-- 特点：可重复执行，幂等，不破坏已有业务数据
-- 前置校验：member.phone / delivery.order_id / member.member_no 均无重复（已实测为 0）
-- ==========================================

USE sam_member_mgmt;

-- ==================== 1. member.phone 唯一约束 ====================
-- 对应论文 5.1.1「系统对手机号设置唯一约束」；手机号同时是会员端登录账号
SET @ukExists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND INDEX_NAME = 'uk_phone');
SET @idxExists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND INDEX_NAME = 'idx_phone');
SET @alterSql := IF(@ukExists = 0 AND @idxExists > 0,
    'ALTER TABLE `member` DROP INDEX `idx_phone`, ADD UNIQUE KEY `uk_phone` (`phone`)',
    IF(@ukExists = 0,
       'ALTER TABLE `member` ADD UNIQUE KEY `uk_phone` (`phone`)',
       'SELECT ''member.uk_phone 已存在，跳过'' AS info'));
PREPARE stmt FROM @alterSql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 2. delivery.order_id 唯一约束 ====================
-- 对应论文 4.2.2「配送表通过 order_id 与订单表一对一关联」
SET @ukExists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'delivery' AND INDEX_NAME = 'uk_order_id');
SET @idxExists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'delivery' AND INDEX_NAME = 'idx_order_id');
SET @alterSql := IF(@ukExists = 0 AND @idxExists > 0,
    'ALTER TABLE `delivery` DROP INDEX `idx_order_id`, ADD UNIQUE KEY `uk_order_id` (`order_id`)',
    IF(@ukExists = 0,
       'ALTER TABLE `delivery` ADD UNIQUE KEY `uk_order_id` (`order_id`)',
       'SELECT ''delivery.uk_order_id 已存在，跳过'' AS info'));
PREPARE stmt FROM @alterSql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 3. member.deleted 逻辑删除字段 ====================
-- 对应论文 5.1.1「会员注销后仍可追溯原始消费记录」
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND COLUMN_NAME = 'deleted');
SET @alterSql := IF(@colExists = 0,
    'ALTER TABLE `member` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0未删除 1已删除'' AFTER `status`',
    'SELECT ''member.deleted 已存在，跳过'' AS info');
PREPARE stmt FROM @alterSql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 4. product.deleted 逻辑删除字段 ====================
-- 对应论文 5.1.4「商品的新增、修改、删除和查询」
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'deleted');
SET @alterSql := IF(@colExists = 0,
    'ALTER TABLE `product` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0未删除 1已删除'' AFTER `status`',
    'SELECT ''product.deleted 已存在，跳过'' AS info');
PREPARE stmt FROM @alterSql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 5. sys_config 系统参数配置表 ====================
-- 对应论文 3.2.3「基础配置则应覆盖门店信息、积分规则、消费等级、公告参数」与 4.3.3「基础配置」
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(100) NOT NULL                COMMENT '参数键',
  `config_value` VARCHAR(500) NOT NULL                COMMENT '参数值',
  `config_name`  VARCHAR(100) DEFAULT NULL            COMMENT '参数名称',
  `remark`       VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

INSERT INTO `sys_config` (`config_key`, `config_value`, `config_name`, `remark`) VALUES
('store.name',              '山姆会员商店（恩施店）', '门店名称',       '用于页头与报表抬头'),
('store.phone',             '0718-8888888',           '门店电话',       '对外客服电话'),
('store.address',           '湖北省恩施市金子坝路1号','门店地址',       '用于配送与小票打印'),
('points.rule.desc',        '积分 = 实付金额 × 等级积分倍率，向下取整', '积分计算规则', '与 member_level.points_rate 联动'),
('stock.alert.default',     '10',                     '默认库存预警阈值', '商品未单独设置 min_stock_alert 时使用'),
('member.expire.notice.days','7',                     '会员到期提醒天数', '首页展示 N 天内到期的会员'),
('order.auto.delivery',     '1',                      '线上订单自动生成配送单', '0关闭 1开启'),
('page.size.default',       '10',                     '默认分页条数',   '列表页默认每页记录数')
ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `config_name`  = VALUES(`config_name`),
    `remark`       = VALUES(`remark`);

-- ==================== 6. daily_statistics.stat_date 唯一约束 ====================
-- 支撑每日定时汇总的幂等写入（同一天只允许一条汇总记录）
SET @ukExists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_statistics' AND INDEX_NAME = 'uk_stat_date');
SET @alterSql := IF(@ukExists = 0,
    'ALTER TABLE `daily_statistics` ADD UNIQUE KEY `uk_stat_date` (`stat_date`)',
    'SELECT ''daily_statistics.uk_stat_date 已存在，跳过'' AS info');
PREPARE stmt FROM @alterSql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 迁移结果自检 ====================
SELECT '迁移完成，自检结果如下' AS info;
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME) AS cols
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME IN ('uk_phone','uk_order_id')
 GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'deleted';
SELECT COUNT(*) AS sys_config_条数 FROM `sys_config`;
