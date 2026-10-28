-- ==========================================
-- 山姆会员消费管理系统 - 种子数据
-- ==========================================

USE sam_member_mgmt;

-- ==================== 角色数据 ====================
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '系统管理员', 'ROLE_ADMIN', '系统超级管理员，拥有所有权限'),
(2, '收银员', 'ROLE_CASHIER', '负责前台收银结算'),
(3, '店长', 'ROLE_MANAGER', '负责会员管理、商品管理、退款审批和数据查看'),
(4, '配送员', 'ROLE_DELIVERY', '负责订单配送与配送状态更新'),
(5, '会员', 'ROLE_MEMBER', '门店会员，可查询个人消费记录与积分并兑换权益');

-- ==================== 用户数据 (密码均为 123456，BCrypt加密) ====================
-- BCrypt哈希由 BCryptPasswordEncoder.encode("123456") 生成，强度10
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `status`) VALUES
(1, 'admin',     '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG', '系统管理员',   '13800000000', 1),
(2, 'cashier01', '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG', '收银员小王',   '13800000001', 1),
(3, 'manager01', '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG', '店长老李',   '13800000002', 1),
(4, 'delivery01','$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG', '配送员小赵', '13800000003', 1);
-- 注意: DataInitializer 仅在 admin 记录不存在时创建 admin/admin123，
--       此处 admin 已存在则跳过创建，故实际登录密码为 123456（与各账号一致）

-- ==================== 用户角色关联 ====================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4);

-- ==================== 菜单数据 ====================
-- 顶级目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(1, 0, '首页', 'C', '/home', 'Home', '', 'HomeFilled', 1),
(2, 0, '会员管理', 'M', '/member', '', '', 'UserFilled', 2),
(3, 0, '商品管理', 'M', '/product', '', '', 'Goods', 3),
(4, 0, '收银结算', 'M', '/order', '', '', 'Sell', 4),
(5, 0, '积分权益', 'M', '/points', '', '', 'Star', 5),
(6, 0, '数据统计', 'C', '/statistics', 'Statistics', '', 'DataAnalysis', 6),
(7, 0, '系统管理', 'M', '/system', '', '', 'Setting', 7),
(8, 0, '配送管理', 'M', '/delivery', '', '', 'Van', 8);

-- 会员管理子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(21, 2, '会员列表', 'C', '/member/list', 'member/MemberList', 'member:list', '', 1),
(22, 2, '等级管理', 'C', '/member/levels', 'member/LevelManage', 'member:level', '', 2);

-- 会员管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `sort_order`) VALUES
(211, 21, '新增会员', 'F', '', '', 'member:add', 1),
(212, 21, '编辑会员', 'F', '', '', 'member:edit', 2),
(213, 21, '冻结/解冻', 'F', '', '', 'member:freeze', 3),
(214, 21, '续费', 'F', '', '', 'member:renew', 4);

-- 商品管理子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(31, 3, '商品分类', 'C', '/product/categories', 'product/CategoryManage', 'product:category', '', 1),
(32, 3, '商品列表', 'C', '/product/list', 'product/ProductList', 'product:list', '', 2),
(33, 3, '库存管理', 'C', '/product/stock', 'product/StockManage', 'product:stock', '', 3);

-- 商品管理按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `sort_order`) VALUES
(321, 32, '新增商品', 'F', '', '', 'product:add', 1),
(322, 32, '编辑商品', 'F', '', '', 'product:edit', 2),
(323, 32, '上架/下架', 'F', '', '', 'product:status', 3),
(331, 33, '商品入库', 'F', '', '', 'product:stock:in', 1);

-- 收银结算子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(41, 4, 'POS收银', 'C', '/order/checkout', 'order/POSCheckout', 'order:checkout', '', 1),
(42, 4, '订单查询', 'C', '/order/list', 'order/OrderList', 'order:list', '', 2);

-- 收银按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `sort_order`) VALUES
(411, 41, '结算', 'F', '', '', 'order:checkout', 1),
(421, 42, '退款', 'F', '', '', 'order:refund', 1);

-- 积分权益子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(51, 5, '权益管理', 'C', '/points/benefits', 'points/BenefitManage', 'points:benefit', '', 1),
(52, 5, '积分管理', 'C', '/points/manage', 'points/PointsManage', 'points:manage', '', 2);

-- 积分按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `sort_order`) VALUES
(521, 52, '积分调整', 'F', '', '', 'points:adjust', 1);

-- 系统管理子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(71, 7, '用户管理', 'C', '/system/users', 'system/UserManage', 'system:user', '', 1),
(72, 7, '角色管理', 'C', '/system/roles', 'system/RoleManage', 'system:role', '', 2),
(73, 7, '菜单管理', 'C', '/system/menus', 'system/MenuManage', 'system:menu', '', 3);

-- 配送管理子菜单（对应 DataInitializer 中 ROLE_DELIVERY 分配的菜单 8 / 81）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`) VALUES
(81, 8, '配送订单', 'C', '/delivery/list', 'delivery/DeliveryList', 'delivery:list', '', 1);

-- 将超管可访问的菜单权限分配给 ROLE_ADMIN (角色ID=1)
-- 所有菜单ID: 1,21,211,212,213,214,22,31,32,321,322,323,33,331,41,411,42,421,51,52,521,6,7,71,72,73,8,81
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1,1), (1,21),(1,211),(1,212),(1,213),(1,214),(1,22),
(1,31),(1,32),(1,321),(1,322),(1,323),(1,33),(1,331),
(1,41),(1,411),(1,42),(1,421),
(1,51),(1,52),(1,521),
(1,6),
(1,7),(1,71),(1,72),(1,73),
(1,8),(1,81);

-- ROLE_CASHIER (角色ID=2) 只有收银结算（不含首页）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2,41),(2,411),(2,42),(2,421);

-- ROLE_DELIVERY (角色ID=4) 配送管理(父目录8) + 配送订单(81)（不分配首页，首页依赖全局统计）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4,8), (4,81);

-- ROLE_MANAGER (角色ID=3) 有首页+会员+商品+积分+统计
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3,1),
(3,21),(3,211),(3,212),(3,213),(3,214),(3,22),
(3,31),(3,32),(3,321),(3,322),(3,323),(3,33),(3,331),
(3,51),(3,52),(3,521),
(3,6);

-- ==================== 会员等级数据 ====================
INSERT INTO `member_level` (`id`, `level_name`, `level_code`, `points_rate`, `discount_rate`, `annual_fee`, `sort_order`) VALUES
(2, '白金会员', 1, 1.20, 0.95, 260.00, 1),
(3, '卓越会员', 2, 1.50, 0.88, 680.00, 2);

-- ==================== 等级权益数据 ====================
INSERT INTO `member_benefit` (`level_id`, `benefit_name`, `benefit_type`, `benefit_value`, `points_cost`, `description`) VALUES
-- 白金会员权益
(2, '购物积分', 'SERVICE', '1.2', 0, '消费1元积1.2分'),
(2, '全场95折', 'DISCOUNT', '0.95', 0, '全场商品享受95折优惠'),
(2, '免费停车4小时', 'SERVICE', '4h', 0, '不限消费金额免费停车4小时'),
(2, '专属客服', 'SERVICE', 'priority', 0, '享受优先客服响应'),
(2, '10元无门槛券', 'GIFT', '10', 500, '积分兑换10元无门槛优惠券'),
(2, '免费停车月卡', 'GIFT', 'parking-month', 1200, '积分兑换免费停车月卡'),
-- 卓越会员权益
(3, '购物积分', 'SERVICE', '1.5', 0, '消费1元积1.5分'),
(3, '全场88折', 'DISCOUNT', '0.88', 0, '全场商品享受88折优惠'),
(3, '免费停车全天', 'SERVICE', 'all-day', 0, '不限消费金额免费停车全天'),
(3, '专属客服', 'SERVICE', 'vip', 0, '享受VIP一对一客服'),
(3, '生日礼遇', 'GIFT', '200', 0, '生日当月赠送200元优惠券'),
(3, '50元无门槛券', 'GIFT', '50', 2000, '积分兑换50元无门槛优惠券'),
(3, '免费停车月卡', 'GIFT', 'parking-month', 1000, '积分兑换免费停车月卡');

-- ==================== 商品分类数据 ====================
INSERT INTO `product_category` (`id`, `parent_id`, `category_name`, `sort_order`) VALUES
(1, 0, '生鲜食品', 1),
(2, 0, '零食饮料', 2),
(3, 0, '日用百货', 3),
(4, 0, '个人护理', 4),
(5, 0, '家居家电', 5),
(6, 1, '水果蔬菜', 1),
(7, 1, '肉禽蛋奶', 2),
(8, 1, '海鲜水产', 3),
(9, 2, '膨化食品', 1),
(10, 2, '坚果糖果', 2),
(11, 2, '饮料冲调', 3),
(12, 3, '厨房用品', 1),
(13, 3, '清洁用品', 2),
(14, 4, '洗发护发', 1),
(15, 4, '口腔护理', 2),
(16, 5, '床上用品', 1),
(17, 5, '厨房电器', 2);

-- ==================== 商品数据 ====================
INSERT INTO `product` (`category_id`, `product_name`, `product_code`, `barcode`, `unit`, `price`, `cost_price`, `stock_quantity`, `description`, `status`) VALUES
(6, '新疆阿克苏苹果5kg', 'SAM-00001', '6901234560001', '箱', 79.90, 55.00, 200, '新疆阿克苏冰糖心苹果，5kg装', 1),
(6, '进口香蕉1.5kg', 'SAM-00002', '6901234560002', '袋', 19.90, 12.00, 300, '菲律宾进口香蕉，1.5kg装', 1),
(6, '有机草莓500g', 'SAM-00003', '6901234560003', '盒', 39.90, 28.00, 150, '有机种植草莓，500g装', 1),
(7, '澳洲谷饲牛排250g', 'SAM-00004', '6901234560004', '份', 89.90, 62.00, 100, '澳洲进口谷饲牛排，250g/份', 1),
(7, '五花肉500g', 'SAM-00005', '6901234560005', '盒', 29.90, 20.00, 180, '精选五花肉，500g装', 1),
(7, '有机鸡蛋30枚', 'SAM-00006', '6901234560006', '盒', 49.90, 35.00, 250, '有机散养鸡蛋，30枚装', 1),
(7, '蒙牛纯牛奶1L*12', 'SAM-00007', '6901234560007', '箱', 69.90, 50.00, 300, '蒙牛纯牛奶，1L*12盒', 1),
(8, '波士顿龙虾500g', 'SAM-00008', '6901234560008', '只', 199.00, 140.00, 50, '鲜活波士顿龙虾，约500g', 1),
(8, '三文鱼刺身200g', 'SAM-00009', '6901234560009', '盒', 69.90, 48.00, 80, '挪威进口三文鱼刺身，200g', 1),
(9, '乐事薯片大礼包', 'SAM-00010', '6901234560010', '袋', 29.90, 18.00, 400, '乐事薯片混合口味大礼包', 1),
(10, '三只松鼠坚果礼盒', 'SAM-00011', '6901234560011', '盒', 128.00, 88.00, 120, '三只松鼠坚果礼盒，8种坚果', 1),
(11, '农夫山泉矿泉水550ml*24', 'SAM-00012', '6901234560012', '箱', 39.90, 25.00, 500, '农夫山泉饮用天然水，550ml*24瓶', 1),
(11, '雀巢咖啡礼盒', 'SAM-00013', '6901234560013', '盒', 99.00, 65.00, 200, '雀巢金牌咖啡礼盒装', 1),
(12, '双立人不粘锅', 'SAM-00014', '6901234560014', '个', 299.00, 200.00, 60, '双立人不粘炒锅30cm', 1),
(13, '蓝月亮洗衣液3kg', 'SAM-00015', '6901234560015', '桶', 49.90, 32.00, 350, '蓝月亮深层洁净洗衣液3kg', 1),
(14, '海飞丝洗发水500ml', 'SAM-00016', '6901234560016', '瓶', 69.90, 45.00, 280, '海飞丝去屑洗发水500ml', 1),
(15, '云南白药牙膏180g', 'SAM-00017', '6901234560017', '支', 39.90, 25.00, 400, '云南白药牙膏180g', 1),
(16, '富安娜四件套', 'SAM-00018', '6901234560018', '套', 399.00, 260.00, 80, '富安娜纯棉印花四件套1.8m', 1),
(17, '美的电饭煲5L', 'SAM-00019', '6901234560019', '台', 299.00, 210.00, 50, '美的智能IH电饭煲5L', 1),
(17, '小米空气炸锅', 'SAM-00020', '6901234560020', '台', 199.00, 140.00, 70, '小米智能空气炸锅4.5L', 1);

-- 按品类给商品配图（5 张品类占位图循环复用）
UPDATE `product` SET `image_url` = CASE
    WHEN `category_id` IN (6, 7, 8)   THEN '/img/products/fresh.png'
    WHEN `category_id` IN (9, 10, 11) THEN '/img/products/snack.png'
    WHEN `category_id` IN (12, 13)    THEN '/img/products/daily.png'
    WHEN `category_id` IN (14, 15)    THEN '/img/products/care.png'
    WHEN `category_id` IN (16, 17)    THEN '/img/products/home.png'
    ELSE '/img/products/fresh.png'
END;

-- ==================== 测试会员数据 ====================
INSERT INTO `member` (`member_no`, `name`, `phone`, `gender`, `current_level_id`, `total_consumption`, `total_points`, `accumulated_points`, `join_date`, `expiry_date`, `status`) VALUES
('M202601010001', '张伟', '13900000001', 1, 3, 85600.00, 12840, 128400, '2025-01-15', '2027-01-15', 1),
('M202601010002', '李娜', '13900000002', 2, 2, 25600.00, 2560, 30720, '2025-03-20', '2027-03-20', 1),
('M202601010003', '王强', '13900000003', 1, 2, 3200.00, 320, 3200, '2025-08-10', '2027-08-10', 1);
