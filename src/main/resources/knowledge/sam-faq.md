# 山姆会员 FAQ 知识库

供 Step 3 向量化入库使用。共 40 条，分四类。

**数据可信度说明**

| 类别 | 条数 | 数据来源 |
|---|---|---|
| 会员等级与年费 | 10 | `member_level` 表真实数据，可直接用 |
| 积分规则 | 10 | `member_level` + `member_benefit` + `OrderServiceImpl` 代码逻辑，可直接用 |
| 退换货政策 | 12 | **行业通行做法，需要你确认或修改** |
| 门店与配送 | 8 | 门店信息来自 `sys_config`，配送政策**需要你确认** |

第 3 和第 4 类里标了 `TODO` 的条目，是系统里没有对应数据、我按零售行业惯例补的。这些内容进向量库之后模型会当事实引用，所以提交前你得过一遍，改成你认可的口径。

**格式约定**，每条一个二级标题，正文是答案，末尾三行元数据。切块时按标题切，一条一块。

---

## 山姆有几个会员等级

两个等级，白金会员和卓越会员。白金会员累计消费 0 元起，卓越会员需要累计消费满 50000 元。

category: 会员等级
source: member_level
points_rate: 1.2 / 1.5

## 白金会员的年费是多少

白金会员年费 260 元，会员卡有效期一年。

category: 会员等级
source: member_level.annual_fee

## 卓越会员的年费是多少

卓越会员年费 680 元，会员卡有效期一年。

category: 会员等级
source: member_level.annual_fee

## 白金会员怎么升级到卓越会员

累计消费满 50000 元即可升级。达到门槛后可以办理升级，需要补缴年费差额 420 元，也就是卓越 680 元减去白金 260 元。

category: 会员等级
source: member_level.min_consumption

## 升级卓越会员需要补交多少钱

需要补缴年费差额。白金会员年费 260 元，卓越会员年费 680 元，差额 420 元。如果当前卡还有剩余有效期，按剩余月份折算。

category: 会员等级
source: member_level.annual_fee

## 卓越会员和白金会员有什么区别

四个方面的差异。

积分倍率，卓越 1.5 倍、白金 1.2 倍。

折扣力度，卓越全场 88 折、白金全场 95 折。

停车权益，卓越全天免费、白金免费 4 小时。

专属服务，卓越享 VIP 一对一客服，白金享优先客服响应。卓越另有生日当月赠 200 元优惠券。

category: 会员等级
source: member_level + member_benefit

## 会员卡有效期是多久

一年。从开卡或续费之日起算，到期日在会员卡的到期日字段里。

category: 会员等级
source: member.expiry_date

## 会员快到期了会有提醒吗

有。系统会在到期前 7 天在首页展示提醒。你也可以在会员中心查看自己的到期日。

category: 会员等级
source: sys_config.member.expire.notice.days

## 会员到期后还能享受权益吗

到期后按普通会员处理，不再享受等级折扣和积分倍率。续费后恢复。

category: 会员等级
source: 业务规则推导

## 会员等级每年重新评定吗

等级依据累计消费金额判定。累计消费达到 50000 元即具备卓越会员资格，降到门槛以下不会自动降级，以实际办理状态为准。

category: 会员等级
source: member_level.min_consumption

---

## 积分是怎么计算的

公式是实付金额乘以等级积分倍率，结果向下取整。

注意是按照折后实付金额计算，不是按商品原价。白金会员倍率 1.2，卓越会员倍率 1.5。

category: 积分规则
source: sys_config.points.rule.desc + OrderServiceImpl

## 白金会员消费 1000 元能积多少分

如果 1000 元是商品原价，白金享受 95 折，实付 950 元，积分是 950 乘以 1.2，等于 1140 分。

如果 1000 元就是实付金额，积分是 1200 分。

category: 积分规则
source: OrderServiceImpl 计算逻辑

## 卓越会员消费 1000 元能积多少分

如果 1000 元是商品原价，卓越享受 88 折，实付 880 元，积分是 880 乘以 1.5，等于 1320 分。

如果 1000 元就是实付金额，积分是 1500 分。

category: 积分规则
source: OrderServiceImpl 计算逻辑

## 积分什么时候到账

订单结算完成后立即入账，可以在积分记录里看到变动明细。

category: 积分规则
source: PointsTypeEnum.EARN_PURCHASE

## 积分有什么用

积分可以兑换优惠券和停车月卡。白金会员 500 积分换 10 元无门槛券，1200 积分换免费停车月卡。卓越会员 2000 积分换 50 元无门槛券，1000 积分换免费停车月卡。

category: 积分规则
source: member_benefit.points_cost

## 积分会过期吗

积分记录里有过期扣除的类型，说明存在过期机制。具体有效期以会员中心展示为准，系统不会主动通知每一笔过期。

category: 积分规则
source: PointsTypeEnum.EXPIRE

## 退货以后积分会被扣回吗

会。退货产生的退款会触发积分调整，按原订单实际积分扣回。

category: 积分规则
source: PointsTypeEnum.MANUAL_ADJUST

## 积分可以转给别人吗

不可以。积分绑定会员账户，不支持转让。

category: 积分规则
source: 业务规则

## 在哪里查我的积分余额

在山姆会员小程序或者会员中心页面查看。积分余额和累计积分分开显示，前者是当前可用，后者是历史累计。

category: 积分规则
source: member.total_points / accumulated_points

## 积分和累计消费是两回事吗

是。积分是消费后按倍率产生的奖励，累计消费是用于判定等级的总金额。累计消费达到 50000 元可以升级卓越会员。

category: 积分规则
source: member.total_consumption

---

## 退货期限是多久

TODO 以下为行业通行做法，需要你确认。

生鲜食品、冷藏冷冻商品在签收后 24 小时内可退，需保留原包装。普通商品在签收后 7 天内可无理由退货，15 天内可因质量问题退货。大件家电家具按国家三包规定执行。

category: 退换货政策
source: TODO 待确认

## 哪些商品不支持退货

TODO 待确认。

以下通常不支持无理由退货。拆封的食品和生鲜、个人护理用品、定制商品、已激活的电子产品、明确标注不支持退换的特价商品。

category: 退换货政策
source: TODO 待确认

## 退货需要带什么凭证

TODO 待确认。

需要会员卡或手机号、订单号或购物小票。线上订单可以提供订单二维码。质量问题退货建议带上商品原包装和配件。

category: 退换货政策
source: TODO 待确认

## 拆封的商品还能退吗

TODO 待确认。

普通商品保持商品完好、不影响二次销售的可以退。食品类拆封后不支持无理由退货，质量问题除外。

category: 退换货政策
source: TODO 待确认

## 大件商品怎么退

TODO 待确认。

大件家电家具需要联系客服预约上门取件，不支持下门店退货。退货前需确认商品外观完好、配件齐全。

category: 退换货政策
source: TODO 待确认

## 退款多久能到账

TODO 待确认。

原路退回。微信支付宝通常 1 到 3 个工作日，银行卡 3 到 7 个工作日，具体以银行处理时间为准。

category: 退换货政策
source: TODO 待确认

## 可以换货吗

TODO 待确认。

同款同价可以换货。换不同商品按退货加重新购买处理。换货需要商品有库存。

category: 退换货政策
source: TODO 待确认

## 商品有质量问题怎么处理

TODO 待确认。

凭小票或订单记录到店或联系客服，经确认为质量问题的，按退货或换货处理，产生的运费由门店承担。

category: 退换货政策
source: TODO 待确认

## 用会员折扣买的商品退货怎么算

TODO 待确认。

按实付金额退款，即享受折扣后的金额。积分按原订单实际获得的积分扣回。

category: 退换货政策
source: TODO 待确认

## 线上订单和门店购买退货一样吗

TODO 待确认。

流程不同但政策一致。线上订单在 App 里提交退货申请，可以选择上门取件或门店退。门店购买的直接到店办理。

category: 退换货政策
source: TODO 待确认

## 退货影响会员等级吗

TODO 待确认。

退货会从累计消费中扣除对应金额。如果扣除后低于门槛，等级不会立即变化，续费时按实际累计消费重新判定。

category: 退换货政策
source: TODO 待确认

## 退换货需要预约吗

TODO 待确认。

普通商品不用预约，直接到服务台办理。大件商品和批量退货需要提前联系客服预约。

category: 退换货政策
source: TODO 待确认

---

## 门店地址在哪里

山姆会员商店恩施店，地址是湖北省恩施市金子坝路 1 号。

category: 门店与配送
source: sys_config.store.address

## 客服电话是多少

0718-8888888。

category: 门店与配送
source: sys_config.store.phone

## 门店营业时间是几点

TODO 待确认，系统里没有营业时间数据，需要你填。

category: 门店与配送
source: TODO 待确认

## 支持配送吗

TODO 待确认。

支持门店周边配送。具体范围以小程序下单时显示的可用地址为准。

category: 门店与配送
source: TODO 待确认

## 配送费怎么算

TODO 待确认。

category: 门店与配送
source: TODO 待确认

## 多久能送到

TODO 待确认。

category: 门店与配送
source: TODO 待确认

## 可以到店自提吗

TODO 待确认。

category: 门店与配送
source: TODO 待确认

## 卓越会员配送有优惠吗

TODO 待确认。当前系统里卓越会员的权益不包含配送优惠，只有积分、折扣、停车、客服和生日礼遇。如果实际有配送优惠，需要补充到 member_benefit 表。

category: 门店与配送
source: member_benefit 查询结果
