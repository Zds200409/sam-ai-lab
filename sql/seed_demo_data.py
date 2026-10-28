"""
山姆会员系统 - 演示数据生成脚本
生成: 300个会员 + 近31天约1000笔订单 + 300条积分记录
"""
import mysql.connector
import random
import datetime

conn = mysql.connector.connect(
    host='localhost', port=3306, user='root', password='root',
    database='sam_member_mgmt', charset='utf8mb4', use_pure=True
)
cur = conn.cursor()

# --- 清理 seed 自己生成的旧数据，保留手动注册会员 ---
# 只清 phone 在 13900000000~13900000299 范围内的会员（seed 自己的数据），
# 这样再跑 seed 不会覆盖你手动注册或导入的会员。
cur.execute("SELECT id FROM member WHERE phone BETWEEN '13900000000' AND '13900000299'")
seed_ids = [r[0] for r in cur.fetchall()]
cur.execute("SELECT COUNT(*) FROM member")
total_before = cur.fetchone()[0]
if seed_ids:
    ph = ','.join(['%s'] * len(seed_ids))
    cur.execute(f"DELETE FROM order_item WHERE order_id IN (SELECT id FROM `order` WHERE member_id IN ({ph}))", seed_ids)
    cur.execute(f"DELETE FROM `order` WHERE member_id IN ({ph})", seed_ids)
    cur.execute(f"DELETE FROM member_points_record WHERE member_id IN ({ph})", seed_ids)
    cur.execute(f"DELETE FROM member_level_change_record WHERE member_id IN ({ph})", seed_ids)
    cur.execute(f"DELETE FROM member WHERE id IN ({ph})", seed_ids)
cur.execute("DELETE FROM stock_record")
cur.execute("ALTER TABLE `order` AUTO_INCREMENT = 1")
cur.execute("ALTER TABLE member AUTO_INCREMENT = 1")
preserved = total_before - len(seed_ids)
print(f"已清理 seed 旧数据 {len(seed_ids)} 人，保留手动注册会员 {preserved} 人")

# ==================== 1. 会员 ====================
surnames = ['张','李','王','刘','陈','杨','赵','黄','周','吴','徐','孙','胡','朱','高','林','何','郭','马','罗','梁','宋','郑','谢','韩','唐','冯','董','萧','程']
genders = [1, 2]
level_ids = [2, 3]  # 2=白金会员, 3=卓越会员

members = []
today = datetime.date.today()

BCRYPT_123456 = '$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG'

for i in range(300):
    name = random.choice(surnames) + random.choice(['伟','芳','娜','敏','静','强','磊','洋','勇','军','杰','欣','婷','雪','峰','涛','超','艳','鑫','宇'])
    gender = random.choice(genders)
    level_id = random.choice(level_ids)
    # 消费额: 白金(0-5万), 卓越(5万+)
    if level_id == 2:
        consumption = round(random.uniform(1000, 48000), 2)
    else:
        consumption = round(random.uniform(50000, 150000), 2)
    points = int(consumption * 1.2) if level_id == 2 else int(consumption * 1.5)
    accumulated = points + random.randint(0, 5000)

    join_days_ago = random.randint(10, 700)
    join_date = today - datetime.timedelta(days=join_days_ago)
    expiry_date = join_date + datetime.timedelta(days=365)

    # 约20%的人生日在今天
    birthday = None
    if i < 6:
        birthday = today.replace(year=random.randint(1985, 2000))
    elif i < 12:
        birthday = datetime.date(random.randint(1980, 2002), random.randint(1, 12), random.randint(1, 28))

    member_no = f'M{today.strftime("%Y%m%d")}{i+10001:04d}'
    phone = f'139{i:08d}'

    cur.execute("""
        INSERT INTO member (member_no, name, phone, password, gender, current_level_id,
            total_consumption, total_points, accumulated_points, birthday,
            join_date, expiry_date, status, create_time, update_time)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,NOW(),NOW())
    """, (member_no, name, phone, BCRYPT_123456, gender, level_id, consumption, points, accumulated, birthday, join_date, expiry_date, 1))
    members.append((cur.lastrowid, name, phone))

# 让2个人7天内到期
cur.execute("UPDATE member SET expiry_date = %s WHERE id = %s", (today + datetime.timedelta(days=3), members[0][0]))
cur.execute("UPDATE member SET expiry_date = %s WHERE id = %s", (today + datetime.timedelta(days=5), members[2][0]))
print(f"会员: 300人 (含 {today.month}月{today.day}日 生日6人, 7天内到期2人)")

# ==================== 2. 订单 ====================
products = []
cur.execute("SELECT id, product_name, price, stock_quantity FROM product WHERE status=1")
for row in cur.fetchall():
    products.append({'id': row[0], 'name': row[1], 'price': float(row[2]), 'stock': row[3]})

order_ids = []
for day_offset in range(30, -1, -1):
    order_date = today - datetime.timedelta(days=day_offset)
    # 每天约32笔，31天约1000笔
    daily_count = random.randint(30, 34)
    for _ in range(daily_count):
        member = random.choice(members)
        # 选1-5个商品
        item_count = random.randint(1, 5)
        items = random.sample(products, item_count)
        total = sum(it['price'] * random.randint(1, 3) for it in items)
        discount = round(total * random.choice([0, 0.05, 0.12]), 2)
        actual = round(total - discount, 2)
        points_earned = int(actual * 1.2) if member[0] % 3 != 0 else int(actual * 1.5)

        cur.execute("""
            INSERT INTO `order` (order_no, member_id, member_name, member_level_name,
                total_amount, discount_amount, actual_amount, points_earned,
                payment_method, operator_id, status, create_time, update_time)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,NOW())
        """, ('SAM' + order_date.strftime('%Y%m%d') + f'{day_offset:02d}{_:02d}{random.randint(10,99):02d}',
              member[0], member[1],
              '卓越会员' if member[0] % 3 == 0 else '白金会员',
              total, discount, actual, points_earned,
              random.choice(['WECHAT', 'ALIPAY', 'CASH', 'BANK_CARD']),
              1, 1, order_date.strftime('%Y-%m-%d %H:%M:%S')))
        order_id = cur.lastrowid
        order_ids.append(order_id)

        for it in items:
            qty = random.randint(1, 3)
            cur.execute("""
                INSERT INTO order_item (order_id, product_id, product_name, product_code,
                    unit_price, quantity, subtotal, create_time)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
            """, (order_id, it['id'], it['name'], f'SAM-{it["id"]:05d}',
                  it['price'], qty, round(it['price'] * qty, 2),
                  order_date.strftime('%Y-%m-%d %H:%M:%S')))

print(f"订单: {len(order_ids)}笔 (近30天)")

# ==================== 3. 积分记录（按真实订单生成，保证可追溯） ====================
# 3.1 消费获得：每个已完成且有积分的订单生成一条，携带 order_id
cur.execute("""
    INSERT INTO member_points_record (member_id, order_id, points, type,
        balance_before, balance_after, description, create_time)
    SELECT member_id, id, points_earned, 1, 0, 0,
           CONCAT('消费获得积分，订单号: ', order_no), create_time
      FROM `order` WHERE status = 1 AND points_earned > 0
""")
print(f"积分记录(消费获得): {cur.rowcount}条")

# 3.2 活动获得 / 人工调整：不来自订单，order_id 留空（符合业务语义）
for _ in range(200):
    m = random.choice(members)
    pts = random.randint(50, 500)
    ptype = random.choice([2, 5])
    cur.execute("""
        INSERT INTO member_points_record (member_id, order_id, points, type,
            balance_before, balance_after, description, create_time)
        VALUES (%s, NULL, %s, %s, 0, 0, %s, DATE_SUB(NOW(), INTERVAL %s DAY))
    """, (m[0], pts, ptype,
          '活动奖励积分' if ptype == 2 else '人工调整积分',
          random.randint(1, 60)))
print("积分记录(活动/人工): 200条")

# 3.3 按会员 + 时间顺序重算每条记录的积分余额前后值
cur.execute("""
    UPDATE member_points_record p
    JOIN (SELECT id,
                 IFNULL(SUM(points) OVER (PARTITION BY member_id ORDER BY create_time, id
                          ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS bef,
                 SUM(points) OVER (PARTITION BY member_id ORDER BY create_time, id
                          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)       AS aft
            FROM member_points_record) t ON t.id = p.id
       SET p.balance_before = t.bef, p.balance_after = t.aft
""")

# 3.4 会员积分与累计消费额由流水、订单回算（不再使用随机值）
cur.execute("""
    UPDATE member m
    JOIN (SELECT member_id,
                 SUM(CASE WHEN points > 0 THEN points ELSE 0 END) AS earned,
                 SUM(points)                                      AS net
            FROM member_points_record GROUP BY member_id) s ON s.member_id = m.id
       SET m.total_points = GREATEST(s.net, 0), m.accumulated_points = s.earned
""")
cur.execute("""
    UPDATE member m
    JOIN (SELECT member_id, SUM(actual_amount) AS amt FROM `order` WHERE status = 1 GROUP BY member_id) o
      ON o.member_id = m.id
       SET m.total_consumption = o.amt
""")
print("会员积分与累计消费额已按真实数据回算")

conn.commit()
cur.close()
conn.close()
print("\n✅ 演示数据生成完成!")
