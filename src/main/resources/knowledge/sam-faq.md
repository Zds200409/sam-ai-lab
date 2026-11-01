# 山姆会员 FAQ 知识库

供 RAG 向量化入库使用。共 22 条，全部来自系统真实数据与代码逻辑。

**数据来源**

| 类别 | 条数 | 数据来源 |
|---|---|---|
| 会员等级与年费 | 10 | `member_level` 表、`MemberExpireTask`、`MemberAuthServiceImpl` |
| 积分规则 | 10 | `OrderServiceImpl` 计算逻辑、`member_benefit`、`PointsServiceImpl` |
| 门店信息 | 2 | `sys_config` |

**归档说明**：退换货政策与配送规则已移出本文件，统一由 `sam-charter.md` 会员章程的第五章、第六章承载。一份政策只保留一个出处，避免两处措辞不一致被同时检索出来。

**格式约定**，每条一个二级标题，正文是答案，末尾两行元数据。切块时按标题切，一条一块。

## 山姆有几个会员等级

两个等级，白金会员和卓越会员。山姆是会员制商店，两个等级都是付费购买，没有消费金额门槛，缴年费即可开通。白金会员年费 260 元，卓越会员年费 680 元。

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

不需要消费满多少，随时可以办理。补缴年费差额 420 元即可升级，也就是卓越 680 元减去白金 260 元。卡内还有剩余有效期的，按剩余月份折算差额。

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

一年。从开卡或续费之日起算，到期日在会员卡卡面和会员中心都能看到。

category: 会员等级
source: member.expiry_date

## 会员快到期了会有提醒吗

有。会员中心会在到期前 7 天在首页展示续费提醒。你也可以在会员中心查看自己的到期日。

category: 会员等级
source: sys_config.member.expire.notice.days

## 会员到期后还能享受权益吗

到期后会员卡被标记为已过期，无法下单结算，也不再享受等级折扣和积分倍率。会员等级保留不变，续费后状态恢复正常，原等级继续有效。

category: 会员等级
source: 业务规则推导

## 会员等级每年重新评定吗

不重新评定。等级由办卡时选择的卡种决定，之后不会因为消费多少而升降。卓越会员续费时仍按卓越续费。

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

积分本身长期有效，日常使用不会过期。但会员卡失效后，已累积的积分只保留 90 天，逾期未续费的自动作废，作废后不可恢复。

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

是。积分是消费后按倍率产生的奖励，可以用来兑换优惠券和停车月卡。累计消费是历史消费总额，只用于消费统计，不参与等级判定，等级由购买的卡种决定。

category: 积分规则
source: member.total_consumption

---

## 门店地址在哪里

山姆会员商店恩施店，地址是湖北省恩施市金子坝路 1 号。

category: 门店与配送
source: sys_config.store.address

## 客服电话是多少

0718-8888888。

category: 门店与配送
source: sys_config.store.phone
