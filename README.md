# 山姆门店会员消费管理系统

基于 Spring Boot + Vue 3 的会员消费管理后台，覆盖会员建档、消费结算、积分累计、订单查询、数据统计、用户与权限管理等核心场景。

## 技术栈

| 层级 | 选型 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18、JDK 17 |
| 持久层 | MyBatis-Plus 3.5.3.1、MySQL 8.x |
| 安全 | JWT（jjwt 0.11.5）、BCrypt 散列、HandlerInterceptor |
| 接口文档 | Knife4j 4.1.0（启动后访问 /doc.html） |
| 前端 | Vue 3、Vite 8、Element Plus、Pinia、Vue Router、ECharts |
| 构建 | Maven Wrapper、npm |

## 环境准备

1. JDK 17（项目自带 `mvnw`，推荐路径 `C:\Users\赵\.jdks\ms-17.0.18`）
2. MySQL 8.x，本地 root 账号可访问，默认配置见 `src/main/resources/application.yml`
3. Node.js ≥ 20.19 或 ≥ 22.12（Vite 8 要求，老版 Node 18 装不上依赖）

## 数据库初始化

```bash
mysql -u root -p
source sql/schema.sql      # 建表
source sql/data.sql        # 种子数据（3 个会员 + 角色 + 菜单）
python sql/seed_demo_data.py   # 可选：再补 30 个会员与近 31 天约 200 单演示数据
```

`seed_demo_data.py` 会先 `DELETE` 掉原会员与订单再灌入演示数据。

已有数据库（不想重建）升级会员端，直接执行 `source sql/migrate_member_portal.sql`，可重复执行、幂等，不会破坏已有数据。

## 启动

双击根目录 `start.bat`，会自动：
- 启动 Spring Boot（端口 8080）
- 启动 Vite 开发服务器（端口 5173）

浏览器访问 `http://localhost:5173`。

### 两个端口的分工

| 端口 | 角色 | 读取来源 |
| --- | --- | --- |
| 5173 | Vite 开发服务器，改 `frontend/src` 实时热重载 | `frontend/src/` 源码 |
| 8080 | Spring Boot 后端 + 打包后的前端静态资源 | `src/main/resources/static/` 打包产物 |

`vite.config.js` 中的 `server.proxy` 会把 5173 收到的 `/api/**` 请求转发到 `http://localhost:8080`，因此从 5173 访问时前后端协同工作，无需配置跨域。

改完 `frontend/src` 后：

- 从 5173 访问：Vite 自动热重载，立即生效
- 从 8080 访问：需先执行 `npm run build`。该命令的 `build.outDir` 已配置为 `../src/main/resources/static`，产物会直接覆盖后端静态资源目录

答辩演示建议直接用 5173；若需从 8080 单端口访问（例如打包成 jar 部署），记得先跑一次 `npm run build`。

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | ROLE_ADMIN |
| manager01 | 123456 | ROLE_MANAGER |
| cashier01 | 123456 | ROLE_CASHIER |
| delivery01 | 123456 | ROLE_DELIVERY |

`DataInitializer` 的 `ensureAdminUser()` 采用「不存在才创建」策略：`sql/data.sql` 已插入 admin 时它会跳过创建，因此**默认密码保持 data.sql 中的 123456**，不会被重置为 admin123。仅当数据库为空、admin 记录不存在时，才会创建 admin/admin123。

## 会员端

系统提供面向顾客的会员自助端（与后台管理端独立）：

- 访问入口：`http://localhost:5173/portal/login`（需先注册或使用已开通密码的会员手机号登录）
- 注册：手机号 + 姓名 + 密码，注册后默认开通「白金会员」等级，会员编号自动生成
- 功能：会员首页（积分余额 / 累计消费 / 等级 / 到期日）、我的订单、我的积分流水、积分兑换权益、个人信息维护与修改密码
- 会员端使用独立的 JWT（`userType=MEMBER`）与后台用户 token 隔离，两套 ID 空间互不串扰

### 会员端涉及的数据变更

会员端依赖三处数据变更（`sql/migrate_member_portal.sql` 已覆盖，可重复执行、幂等）：

1. `member` 表新增 `password` 列（BCrypt 存储，后台建档未设密码时为空，需到后台重置或会员自行注册）
2. `member_benefit` 表新增 `points_cost` 列（0 表示不可积分兑换），并补充可兑换的 GIFT 权益
3. `sys_role` 补充 `ROLE_MEMBER`，`sys_role_menu` 补充配送菜单，`sys_user_role` 增加唯一键防止重复关联

已有数据库请执行一次 `source sql/migrate_member_portal.sql`，无需重建库。

## 目录结构

```
sam-member-mgmt/
├── sql/                      # 建表与种子脚本
├── src/main/java/com/sam/membermgmt/
│   ├── common/               # 统一返回、异常、注解、枚举、分页
│   ├── config/               # WebMvc、MyBatis-Plus、Auth、BCrypt、Swagger
│   ├── modules/
│   │   ├── member/           # 会员、积分、等级
│   │   ├── order/            # 收银、订单明细、退款
│   │   ├── product/          # 商品、分类、库存流水
│   │   ├── statistics/       # 仪表盘统计
│   │   └── system/           # 用户、角色、菜单、认证
│   └── utils/                # JWT、编号生成、用户上下文
├── src/main/resources/
│   ├── application.yml       # 多环境配置（dev/prod）
│   ├── mapper/               # 自定义 XML 映射
│   ├── static/               # 打包后的前端静态资源（开发期由前端 dev server 提供）
│   └── templates/            # SPA 入口页（默认首页跳转用）
└── frontend/                 # Vue 3 源码
    ├── src/views/            # 业务页面
    ├── src/api/              # axios 请求与各模块 API
    └── src/router/           # 路由与登录守卫
```

## 接口文档

启动后端后访问 `http://localhost:8080/doc.html`（Knife4j），可在线浏览所有 REST 接口、请求参数与示例响应。

## 关键业务规则

- 积分公式：P = A × r，其中 A 为实付金额，r 为会员等级 `points_rate`，向下取整
- 折扣公式：D = T × (1 - d)，其中 T 为商品总额，d 为等级 `discount_rate`
- 等级自动升级：累计消费额达到更高等级 `min_consumption` 时自动升级并写入变更记录
- 事务边界：结算 / 退款 / 积分调整三类操作均 `@Transactional(rollbackFor = Exception.class)`，涉及订单、订单明细、库存、会员余额五张表的原子写入