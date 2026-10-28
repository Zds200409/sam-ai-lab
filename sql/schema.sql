-- ==========================================
-- 山姆会员消费管理系统 - 数据库建表脚本
-- Database: sam_member_mgmt
-- ==========================================

CREATE DATABASE IF NOT EXISTS sam_member_mgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sam_member_mgmt;

-- ==================== 系统权限表 ====================

DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `store`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `operation_log`;

CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_name` (`role_name`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID 0=顶级',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `menu_type` CHAR(1) NOT NULL COMMENT '菜单类型 M=目录 C=菜单 F=按钮',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `visible` TINYINT DEFAULT 1 COMMENT '是否可见 0=隐藏 1=显示',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE `sys_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ==================== 会员管理表 ====================

DROP TABLE IF EXISTS `member_level_change_record`;
DROP TABLE IF EXISTS `member_points_record`;
DROP TABLE IF EXISTS `member_benefit`;
DROP TABLE IF EXISTS `member`;
DROP TABLE IF EXISTS `member_level`;

CREATE TABLE `member_level` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_code` INT NOT NULL COMMENT '等级编码 1=白金 2=卓越',
    `points_rate` DECIMAL(3,2) NOT NULL COMMENT '积分倍率',
    `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '折扣率 0.95=95折',
    `annual_fee` DECIMAL(10,2) NOT NULL COMMENT '年费',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级表';

CREATE TABLE `member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员ID',
    `member_no` VARCHAR(20) NOT NULL COMMENT '会员编号',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '会员端登录密码(BCrypt)，后台建档未设密码时为空',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0=未知 1=男 2=女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    `current_level_id` BIGINT NOT NULL COMMENT '当前等级ID',
    `total_consumption` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计消费金额',
    `total_points` INT DEFAULT 0 COMMENT '当前可用积分',
    `accumulated_points` INT DEFAULT 0 COMMENT '累计获得积分',
    `join_date` DATE DEFAULT NULL COMMENT '入会日期',
    `expiry_date` DATE DEFAULT NULL COMMENT '到期日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=冻结 1=正常 2=已过期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_no` (`member_no`),
    KEY `idx_phone` (`phone`),
    KEY `idx_current_level_id` (`current_level_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE `member_benefit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权益ID',
    `level_id` BIGINT NOT NULL COMMENT '等级ID',
    `benefit_name` VARCHAR(100) NOT NULL COMMENT '权益名称',
    `benefit_type` VARCHAR(50) NOT NULL COMMENT '权益类型 DISCOUNT/SERVICE/GIFT',
    `benefit_value` VARCHAR(200) DEFAULT NULL COMMENT '权益值',
    `points_cost` INT DEFAULT 0 COMMENT '兑换所需积分，0 表示不可积分兑换',
    `description` TEXT DEFAULT NULL COMMENT '权益描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员权益表';

CREATE TABLE `member_points_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `member_id` BIGINT NOT NULL COMMENT '会员ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `points` INT NOT NULL COMMENT '积分变动 正=获得 负=消耗',
    `type` TINYINT NOT NULL COMMENT '类型 1=消费获得 2=活动获得 3=积分兑换 4=过期扣除 5=人工调整',
    `balance_before` INT DEFAULT 0 COMMENT '变动前积分',
    `balance_after` INT DEFAULT 0 COMMENT '变动后积分',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动记录表';

CREATE TABLE `member_level_change_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `member_id` BIGINT NOT NULL COMMENT '会员ID',
    `from_level_id` BIGINT NOT NULL COMMENT '原等级ID',
    `to_level_id` BIGINT NOT NULL COMMENT '新等级ID',
    `change_reason` VARCHAR(500) NOT NULL COMMENT '变更原因',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID 空=系统自动',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级变更记录表';

-- ==================== 商品管理表 ====================

DROP TABLE IF EXISTS `stock_record`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `product_category`;

CREATE TABLE `product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID 0=顶级',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码(SKU)',
    `barcode` VARCHAR(50) DEFAULT NULL COMMENT '条形码',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `cost_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本价',
    `stock_quantity` INT DEFAULT 0 COMMENT '库存数量',
    `min_stock_alert` INT DEFAULT 10 COMMENT '低库存预警阈值',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
    `description` TEXT DEFAULT NULL COMMENT '商品描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=下架 1=上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_product_name` (`product_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE `stock_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity_change` INT NOT NULL COMMENT '库存变化量 正=入库 负=出库',
    `type` TINYINT NOT NULL COMMENT '类型 1=入库 2=销售出库 3=退货入库 4=盘点调整',
    `stock_before` INT DEFAULT 0 COMMENT '变动前库存',
    `stock_after` INT DEFAULT 0 COMMENT '变动后库存',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动记录表';

-- ==================== 订单表 ====================

DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `delivery`;
DROP TABLE IF EXISTS `return_record`;

CREATE TABLE `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(30) NOT NULL COMMENT '订单编号',
    `member_id` BIGINT NOT NULL COMMENT '会员ID',
    `member_name` VARCHAR(50) DEFAULT NULL COMMENT '会员姓名',
    `member_level_name` VARCHAR(50) DEFAULT NULL COMMENT '会员等级名称',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '商品总金额',
    `discount_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '优惠金额',
    `actual_amount` DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    `points_earned` INT DEFAULT 0 COMMENT '本次获得积分',
    `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式 CASH/WECHAT/ALIPAY/BANK_CARD',
    `operator_id` BIGINT DEFAULT NULL COMMENT '收银员ID 空=会员自助下单',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1=已完成 2=已退款 3=已取消',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
    `quantity` INT NOT NULL COMMENT '数量',
    `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ==================== 数据统计表 ====================

DROP TABLE IF EXISTS `daily_statistics`;

CREATE TABLE `daily_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_order_count` INT DEFAULT 0 COMMENT '订单总数',
    `total_amount` DECIMAL(14,2) DEFAULT 0.00 COMMENT '销售总额',
    `total_member_count` INT DEFAULT 0 COMMENT '消费会员数',
    `new_member_count` INT DEFAULT 0 COMMENT '新增会员数',
    `points_issued` INT DEFAULT 0 COMMENT '发放积分数',
    `points_consumed` INT DEFAULT 0 COMMENT '消耗积分数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- ==================== 会员升级支付流水表 ====================

DROP TABLE IF EXISTS `member_upgrade_payment`;

CREATE TABLE `member_upgrade_payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付单ID',
    `payment_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
    `member_id` BIGINT NOT NULL COMMENT '会员ID',
    `level_change_record_id` BIGINT NOT NULL COMMENT '关联的等级变更记录ID',
    `from_level_id` BIGINT NOT NULL COMMENT '升级前等级ID',
    `to_level_id` BIGINT NOT NULL COMMENT '升级后等级ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '补差价金额',
    `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `status` VARCHAR(20) NOT NULL COMMENT '支付状态 PAID',
    `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员升级支付流水表';

-- ==================== 配送表（补建表，实体已存在） ====================

CREATE TABLE `delivery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(30) DEFAULT NULL COMMENT '订单编号',
    `member_name` VARCHAR(50) DEFAULT NULL COMMENT '会员姓名',
    `member_phone` VARCHAR(20) DEFAULT NULL COMMENT '会员手机号',
    `delivery_address` VARCHAR(255) DEFAULT NULL COMMENT '配送地址',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1=待配送 2=配送中 3=已送达',
    `delivery_user_id` BIGINT DEFAULT NULL COMMENT '配送员ID',
    `delivery_time` DATETIME DEFAULT NULL COMMENT '送达时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送表';

-- ==================== 门店表 ====================

CREATE TABLE `store` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '门店ID',
    `store_code` VARCHAR(20) NOT NULL COMMENT '门店编码',
    `store_name` VARCHAR(100) NOT NULL COMMENT '门店名称',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '门店地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '店长姓名',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=停业 1=营业',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_code` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

-- ==================== 公告表 ====================

CREATE TABLE `announcement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT DEFAULT NULL COMMENT '内容',
    `type` TINYINT DEFAULT 1 COMMENT '类型 1=系统公告 2=活动促销 3=维护通知',
    `publisher` VARCHAR(50) DEFAULT NULL COMMENT '发布人',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=草稿 1=已发布',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ==================== 退换货表 ====================

CREATE TABLE `return_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '退换货ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(30) DEFAULT NULL COMMENT '订单编号',
    `member_id` BIGINT DEFAULT NULL COMMENT '会员ID',
    `member_name` VARCHAR(50) DEFAULT NULL COMMENT '会员姓名',
    `type` TINYINT DEFAULT 1 COMMENT '类型 1=退货 2=换货',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '原因',
    `amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '退款金额',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1=待审核 2=已通过 3=已拒绝',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退换货表';

-- ==================== 操作日志表 ====================

CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    `operation` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '方法名',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `duration` BIGINT DEFAULT 0 COMMENT '耗时(ms)',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=失败 1=成功',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
