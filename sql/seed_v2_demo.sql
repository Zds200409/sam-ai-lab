-- ==========================================
-- v2 演示数据补齐（v2 修订版）
-- 目的：让论文中"有表无数据"的模块可用、可截图；并修复早期演示数据的缺陷
-- 执行方式：mysql -u root -p sam_member_mgmt < sql/seed_v2_demo.sql
-- 特点：可重复执行，幂等；所有数据均引用库中真实存在的会员/订单/等级/用户
-- 修订记录：v2 修复 (1) order.create_time 为空的 6 条订单按 order_no 还原时间戳
--                     (2) 幂等守卫改为关联子查询，避免只插入一条
--                     (3) daily_statistics 汇总排除 create_time 为空的行
-- ==========================================

USE sam_member_mgmt;

-- ==================== 0. 修复：还原 create_time 为空的订单时间 ====================
-- 6 条订单的 order_no 形如 SAM2026090216470001，其中 yyyyMMddHHmm 即为真实下单时间
UPDATE `order`
   SET create_time = STR_TO_DATE(SUBSTRING(order_no, 4, 12), '%Y%m%d%H%i')
 WHERE create_time IS NULL
   AND order_no REGEXP '^SAM[0-9]{12,}$'
   AND STR_TO_DATE(SUBSTRING(order_no, 4, 12), '%Y%m%d%H%i') IS NOT NULL;

-- ==================== 1. 门店基础数据（3 条） ====================
INSERT INTO `store` (`store_code`, `store_name`, `address`, `phone`, `manager_name`, `status`, `remark`)
SELECT * FROM (
    SELECT 'ES001' AS a, '山姆会员商店（恩施店）' AS b, '湖北省恩施市金子坝路1号' AS c, '0718-8888888' AS d, '店长老李' AS e, 1 AS f, '旗舰店' AS g
    UNION ALL SELECT 'ES002', '山姆会员商店（利川店）', '湖北省利川市清江大道88号', '0718-7777777', '王经理', 1, '分店'
    UNION ALL SELECT 'ES003', '山姆会员商店（来凤店）', '湖北省来凤县翔凤大道6号', '0718-6666666', '刘经理', 1, '分店'
) x
WHERE NOT EXISTS (SELECT 1 FROM `store` s WHERE s.store_code = x.a);

-- ==================== 2. 公告数据（4 条，已发布） ====================
INSERT INTO `announcement` (`title`, `content`, `type`, `publisher`, `status`, `create_time`)
SELECT * FROM (
    SELECT '中秋国庆双节会员专享礼遇' AS a,
           '9月28日至10月8日，会员到店单笔消费满500元赠积分礼包，卓越会员叠加95折上折。' AS b,
           1 AS c, '系统管理员' AS d, 1 AS e, NOW() - INTERVAL 12 DAY AS f
    UNION ALL SELECT '门店营业时间调整通知',
           '自10月1日起，门店营业时间调整为 08:00-22:30，会员专属通道提前30分钟开放。', 1, '店长老李', 1, NOW() - INTERVAL 9 DAY
    UNION ALL SELECT '积分兑换权益上新',
           '新增「免费停车3小时」「烘焙工坊体验券」两项积分兑换权益，白金会员1200积分起兑。', 2, '系统管理员', 1, NOW() - INTERVAL 5 DAY
    UNION ALL SELECT '会员卡到期续费提醒',
           '2026年10月到期的会员请及时续费，续费后可继续享受会员价与积分权益。', 3, '店长老李', 1, NOW() - INTERVAL 2 DAY
) x
WHERE NOT EXISTS (SELECT 1 FROM `announcement` a WHERE a.title = x.a);

-- ==================== 3. 退换货记录（前 6 笔已完成订单各一条） ====================
INSERT INTO `return_record` (`order_id`, `order_no`, `member_id`, `member_name`, `type`, `reason`, `amount`, `status`, `handler_id`, `handle_time`, `create_time`)
SELECT o.id, o.order_no, o.member_id, o.member_name,
       IF(o.id % 2 = 0, 1, 2),
       IF(o.id % 2 = 0, '商品与描述不符，申请退货', '规格不合适，申请换货'),
       o.actual_amount,
       IF(o.id % 3 = 0, 1, IF(o.id % 3 = 1, 2, 3)),
       1, NOW(), NOW() - INTERVAL (o.id % 20) DAY
FROM `order` o
WHERE o.status = 1
  AND o.id IN (SELECT z.id FROM (SELECT id FROM `order` WHERE status = 1 ORDER BY id LIMIT 6) z)
  AND NOT EXISTS (SELECT 1 FROM `return_record` r WHERE r.order_id = o.id)
ORDER BY o.id;

-- ==================== 4. 会员升级支付流水（对应真实的升级记录） ====================
-- 说明：member_level_change_record 中"升级"方向（to_level_id > from_level_id）的真实记录
--       有 2 条，故此处最多生成 2 条支付流水，不做任何虚构
INSERT INTO `member_upgrade_payment` (`payment_no`, `member_id`, `level_change_record_id`, `from_level_id`, `to_level_id`,
                                      `amount`, `payment_method`, `status`, `paid_time`, `create_time`)
SELECT CONCAT('PAY', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(r.id, 5, '0')),
       r.member_id, r.id, r.from_level_id, r.to_level_id,
       IFNULL(l.annual_fee, 260.00),
       IF(r.id % 3 = 0, 'WECHAT', IF(r.id % 3 = 1, 'ALIPAY', 'BANK_CARD')),
       1, r.create_time, r.create_time
FROM `member_level_change_record` r
LEFT JOIN `member_level` l ON l.id = r.to_level_id
WHERE r.to_level_id > r.from_level_id
  AND NOT EXISTS (SELECT 1 FROM `member_upgrade_payment` p WHERE p.level_change_record_id = r.id)
ORDER BY r.id;

-- ==================== 5. 操作日志历史数据（5 条） ====================
INSERT INTO `operation_log` (`user_id`, `username`, `operation`, `method`, `ip`, `params`, `duration`, `status`, `create_time`)
SELECT * FROM (
    SELECT 1 AS a, 'admin' AS b, '新增会员' AS c, 'MemberController.save' AS d, '127.0.0.1' AS e, '{"name":"演示会员"}' AS f, 46 AS g, 1 AS h, NOW() - INTERVAL 6 DAY AS i
    UNION ALL SELECT 2, 'cashier01', '收银结算', 'OrderController.checkout', '127.0.0.1', '{"memberId":1,"itemCount":3}', 128, 1, NOW() - INTERVAL 5 DAY
    UNION ALL SELECT 1, 'admin', '新增商品', 'ProductController.save', '127.0.0.1', '{"productName":"澳洲谷饲牛排"}', 52, 1, NOW() - INTERVAL 4 DAY
    UNION ALL SELECT 4, 'delivery01', '接单配送', 'DeliveryController.pickup', '127.0.0.1', '{"id":5}', 31, 1, NOW() - INTERVAL 3 DAY
    UNION ALL SELECT 3, 'manager01', '审核退换货', 'ReturnController.approve', '127.0.0.1', '{"id":2}', 27, 1, NOW() - INTERVAL 1 DAY
) x
WHERE NOT EXISTS (SELECT 1 FROM `operation_log` l WHERE l.method = x.d);

-- ==================== 6. 每日统计回填（按真实已完成订单按天汇总） ====================
DELETE FROM `daily_statistics`
 WHERE stat_date IN (SELECT DISTINCT DATE(create_time) FROM `order`
                      WHERE status = 1 AND create_time IS NOT NULL);

INSERT INTO `daily_statistics`
    (`stat_date`, `total_order_count`, `total_amount`, `total_member_count`,
     `new_member_count`, `points_issued`, `points_consumed`)
SELECT DATE(o.create_time),
       COUNT(*),
       IFNULL(SUM(o.actual_amount), 0),
       (SELECT COUNT(*) FROM `member` m WHERE DATE(m.create_time) <= DATE(o.create_time)),
       (SELECT COUNT(*) FROM `member` m2 WHERE DATE(m2.create_time) = DATE(o.create_time)),
       IFNULL((SELECT SUM(p.points) FROM `member_points_record` p
                WHERE DATE(p.create_time) = DATE(o.create_time) AND p.points > 0), 0),
       IFNULL((SELECT ABS(SUM(p2.points)) FROM `member_points_record` p2
                WHERE DATE(p2.create_time) = DATE(o.create_time) AND p2.points < 0), 0)
FROM `order` o
WHERE o.status = 1 AND o.create_time IS NOT NULL
GROUP BY DATE(o.create_time);

-- ==================== 7. 积分流水回填 order_id（消费类积分与订单建立关联） ====================
UPDATE `member_points_record` p
   SET p.order_id = (SELECT MIN(o.id) FROM `order` o
                      WHERE o.member_id = p.member_id AND o.points_earned = p.points)
 WHERE p.order_id IS NULL
   AND p.type = 1
   AND EXISTS (SELECT 1 FROM `order` o2
                WHERE o2.member_id = p.member_id AND o2.points_earned = p.points);

-- ==================== 8. 重建积分流水与会员汇总字段（以真实订单为唯一依据） ====================
-- 背景：早期 seed_demo_data.py 生成的 300 条积分记录与订单无任何关联（随机会员/随机积分、
--       balance_before 恒为 0），会员的 total_points / accumulated_points / total_consumption
--       亦为随机值。本节按真实订单重新推导，使"会员—订单—积分"三者可互相验证，
--       对应论文 4.2.2「会员累计消费额可由历史消费记录汇总得到」。

-- 8.1 清理未关联订单的"消费获得"记录（其余 type=2 活动获得 / type=5 人工调整本就不来自订单，保留）
DELETE FROM `member_points_record` WHERE `type` = 1 AND `order_id` IS NULL;

-- 8.2 每个已完成且有积分的订单补一条"消费获得"记录
INSERT INTO `member_points_record` (`member_id`, `order_id`, `points`, `type`,
                                    `balance_before`, `balance_after`, `description`, `create_time`)
SELECT o.member_id, o.id, o.points_earned, 1, 0, 0,
       CONCAT('消费获得积分，订单号: ', o.order_no), o.create_time
  FROM `order` o
 WHERE o.status = 1
   AND o.points_earned > 0
   AND NOT EXISTS (SELECT 1 FROM `member_points_record` p WHERE p.order_id = o.id);

-- 8.3 按会员 + 时间顺序重算每条记录的积分余额前后值
UPDATE `member_points_record` p
  JOIN (
        SELECT id,
               IFNULL(SUM(points) OVER (PARTITION BY member_id ORDER BY create_time, id
                        ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS bef,
               SUM(points) OVER (PARTITION BY member_id ORDER BY create_time, id
                        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)       AS aft
          FROM `member_points_record`
       ) t ON t.id = p.id
   SET p.balance_before = t.bef,
       p.balance_after  = t.aft;

-- 8.4 会员可用积分 / 累计积分 由积分流水回算
UPDATE `member` m
  JOIN (SELECT member_id,
               SUM(CASE WHEN points > 0 THEN points ELSE 0 END) AS earned,
               SUM(points)                                      AS net
          FROM `member_points_record` GROUP BY member_id) s ON s.member_id = m.id
   SET m.total_points       = GREATEST(s.net, 0),
       m.accumulated_points = s.earned;

UPDATE `member`
   SET total_points = 0, accumulated_points = 0
 WHERE id NOT IN (SELECT DISTINCT member_id FROM `member_points_record`);

-- 8.5 会员累计消费额 由已完成订单回算
UPDATE `member` m
  JOIN (SELECT member_id, SUM(actual_amount) AS amt
          FROM `order` WHERE status = 1 GROUP BY member_id) o ON o.member_id = m.id
   SET m.total_consumption = o.amt;

UPDATE `member`
   SET total_consumption = 0
 WHERE id NOT IN (SELECT DISTINCT member_id FROM `order` WHERE status = 1);

-- ==================== 结果自检 ====================
SELECT '演示数据补齐结果' AS info;
SELECT 'store' AS 表, COUNT(*) AS 条数 FROM `store`
UNION ALL SELECT 'announcement', COUNT(*) FROM `announcement`
UNION ALL SELECT 'return_record', COUNT(*) FROM `return_record`
UNION ALL SELECT 'member_upgrade_payment', COUNT(*) FROM `member_upgrade_payment`
UNION ALL SELECT 'operation_log', COUNT(*) FROM `operation_log`
UNION ALL SELECT 'daily_statistics', COUNT(*) FROM `daily_statistics`
UNION ALL SELECT 'sys_config', COUNT(*) FROM `sys_config`
UNION ALL SELECT 'member', COUNT(*) FROM `member`
UNION ALL SELECT 'order', COUNT(*) FROM `order`
UNION ALL SELECT 'delivery', COUNT(*) FROM `delivery`;

SELECT '订单 create_time 仍为空的数量（应为 0）' AS info,
       COUNT(*) AS 剩余空值 FROM `order` WHERE create_time IS NULL;

SELECT '积分流水 order_id 关联情况' AS info;
SELECT type,
       COUNT(*) AS 总条数,
       SUM(order_id IS NOT NULL) AS 已关联订单,
       SUM(order_id IS NULL) AS 未关联订单
  FROM `member_points_record`
 GROUP BY type;
