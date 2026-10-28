# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

山姆会员店会员消费管理系统 (Sam's Club Member Consumption Management System) — a full-stack Spring Boot + Vue 3 application.

---

## 🚀 一键启动

### 方式一：启动脚本（推荐）
```bash
# Windows 双击 start.bat
# 或在终端：
./start.sh
```

### 方式二：手动启动
```bash
# 后端 (端口 8080)
./mvnw spring-boot:run -DskipTests

# 前端 (端口 5173，另一个终端)
cd frontend && npm run dev
```

### 登录信息
- 地址: `http://localhost:5173`

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| `admin` | `123456` | ROLE_ADMIN | 系统管理员（全部功能） |
| `manager01` | `123456` | ROLE_MANAGER | 店长（会员/商品/积分/统计/订单） |
| `cashier01` | `123456` | ROLE_CASHIER | 收银员（POS收银 + 订单查询） |
| `delivery01` | `123456` | ROLE_DELIVERY | 配送员（配送管理） |

> 💡 DataInitializer (`@Profile("dev")`) 仅在 admin 不存在时创建（密码 `admin123`），data.sql 预置的 admin 密码为 `123456`。首次部署执行 `sql/schema.sql` + `sql/data.sql` 后密码统一为 `123456`。

### 🔧 常见问题

#### 1. 页面显示 403
- **原因**: 用户 Token 中无角色或角色不匹配。常见于 `zhaodashuai` 首次创建时未分配角色
- **修复**: 前端 403 会自动跳转登录页重新获取 Token；如果循环跳转，执行以下 SQL：
  ```sql
  -- 给用户分配角色
  INSERT IGNORE INTO sys_user_role (user_id, role_id)
  SELECT u.id, r.id FROM sys_user u, sys_role r
  WHERE u.username='zhaodashuai' AND r.role_code='ROLE_ADMIN';
  
  -- 重置密码为 123456
  UPDATE sys_user SET password='$2a$10$JtREEpm.k3bh/JRBL8MtSuTW.SUIbtC1OgSfZCQUXWg90E4MSESiG'
  WHERE username IN ('cashier01', 'manager01', 'zhaodashuai');
  ```

#### 2. 页面出现异常（error boundary）
- **原因**: 组件内 API 调用抛异常未被捕获，触发 `App.vue` 的 `onErrorCaptured`
- **预防**: `Home.vue` 已加 try-catch 降级；其他页面新增 API 调用时务必加错误处理

#### 3. 登录后首页白屏 / 无数据
- **原因**: 浏览器 localStorage 残留旧 Token（角色已变更但 Token 未过期）
- **修复**: F12 → Application → Local Storage → 清除 → 刷新页面重新登录

#### 4. 角色菜单不完整（只显示 1-2 个菜单）
- **原因**: `sys_role_menu` 只分配了子菜单，缺少父级菜单（如会员管理 parentId=2），`MenuServiceImpl.buildTree()` 从 parentId=0 开始构建树，父节点缺失导致子节点无法挂载
- **修复**: 确保每个角色同时拥有父级菜单和子菜单。`DataInitializer.ensureRoleMenus()` 已含自愈逻辑
- **SQL 参考**:
  ```sql
  -- 查看某角色的菜单树（含父子关系）
  SELECT m.id, m.menu_name, m.parent_id,
    CASE WHEN m2.id IS NULL AND m.parent_id > 0 THEN '❌ 缺父级' ELSE '✅' END AS 状态
  FROM sys_role_menu rm
  JOIN sys_menu m ON rm.menu_id=m.id
  LEFT JOIN sys_role_menu rm2 ON m.parent_id=rm2.menu_id AND rm2.role_id=rm.role_id
  WHERE rm.role_id=3;
  ```

#### 5. 已加固的前端页面（含 try-catch 降级）
| 页面 | 错误处理 |
|------|---------|
| `Home.vue` | ✅ API + 图表三重 try-catch |
| `StatisticsDashboard.vue` | ✅ API + 图表 try-catch |
| `MemberList.vue` | ✅ fetchData + onMounted try-catch |
| `OrderList.vue` | ✅ 退款/审批/详情 try-catch |
| `MemberDetail.vue` | ✅ error 状态展示空状态 |
| `request.js` | ✅ 401/403 自动跳转登录 |

#### 6. 浏览器 Console 调试技巧
- **localStorage Key**: Token 存在 `sam-token`，RefreshToken 存在 `sam-refresh-token`
- **403 权限拦截演示** (答辩用):
  ```javascript
  // 用收银员账号登录后，F12 Console 粘贴：
  fetch('/api/users/page', {
    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('sam-token') }
  }).then(r => r.json()).then(console.log)
  // 返回 {"code":403, "message":"权限不足，无法访问该资源"}
  ```

---

## 🎓 答辩指南

### RBAC 权限矩阵（核心卖点）

| 功能 | admin | cashier01 | manager01 | 拦截层 |
|------|-------|-----------|-----------|--------|
| 数据统计 | ✅ | ✅ | ✅ | - |
| POS收银 | ✅ | ✅ | ✅ | - |
| 订单查询 | ✅ | ✅ | ✅ | - |
| 会员管理 | ✅ | 🔒 403 | ✅ | `@RequireRole({"ROLE_ADMIN","ROLE_MANAGER"})` |
| 商品管理 | ✅ | 仅查看 | ✅ | 读开放 / 写拦截 |
| 退款审批 | ✅ | 仅申请 | ✅ | 状态机 + 角色判断 |
| 系统管理 | ✅ | 🔒 403 | 🔒 403 | `@RequireRole({"ROLE_ADMIN"})` |

### 答辩演示路径（5 分钟）
1. **admin 登录** → 首页仪表盘 → 展示8项数据指标
2. **新增会员** → 展示表单校验 + 重复手机号拦截
3. **POS收银** → 搜会员 → 扫商品 → 计算折扣 → F8结算
4. **订单退款** → 申请退款 → 切 manager01 → 审批通过
5. **权限拦截** → 切 cashier01 → F12 Console 粘贴403演示脚本
6. **数据统计** → 日期筛选 + 趋势图 + 分类占比

### 答辩口述要点
- **开场** (30s): "我的毕设是山姆会员店会员消费管理系统，Spring Boot + Vue 3 全栈项目"
- **亮点1** (1min): "RBAC做了三层：JWT认证 → @RequireRole授权 → @OperationLog审计"
- **亮点2** (1min): "POS收银在一个事务里完成扣库存、生成订单、累加积分，退款走状态机"
- **亮点3** (30s): "DataInitializer启动自愈 + Token双令牌自动续期 + 全局错误边界"
- **收尾** (30s): "最大收获不是代码量，是理解了企业级系统的分层思想"

### 环境依赖
- JDK 17 (`C:\Users\赵\.jdks\ms-17.0.18`)
- MySQL 8.0 (localhost:3306, 库名 `sam_member_mgmt`, root/root)
- Maven Wrapper 已安装 (`./mvnw`)，无需系统安装 Maven

---

## Development commands

### Backend (Spring Boot + Maven)

```bash
# Run the app (dev profile, port 8080)
./mvnw spring-boot:run -DskipTests

# Run tests
./mvnw test

# Package for production
./mvnw clean package -DskipTests
```

### Frontend (Vue 3 + Vite)

```bash
cd frontend

# Dev server (port 5173, proxies /api → localhost:8080)
npm run dev

# Build into ../src/main/resources/static
npm run build
```

---

## 📋 完善规划 (2026-06-06)

### 第一步 ✅ 端口对齐 — 已完成
- 修复 `application.yml` 端口从非法值 `801180` → `8080`
- 确认 Vite 代理 `localhost:8080` 与后端一致
- 安装 Maven Wrapper (`./mvnw`)
- Maven settings.xml 添加 `org.springframework.boot` pluginGroup
- 创建 `start.bat` / `start.sh` 一键启动脚本

### 第二步 ✅ 修复痛点 — 已完成
- [x] 前端 catch 块空实现 → Login.vue/POSCheckout.vue 添加 ElMessage 错误提示
- [x] 种子数据 BCrypt 密码哈希 → 重新生成正确哈希，三个用户密码统一为 123456
- [x] DataInitializer 优化 → 仅在 admin 用户不存在时创建，不再每次重置密码
- [x] 封装前端 API 层 → 新增 member.js / product.js / order.js / statistics.js / system.js / points.js
- [x] POSCheckout / StatisticsDashboard / Home / OrderList 已迁移至 API 模块

### 第三步 ✅ 功能补全 — 已完成
- [x] ECharts 图表响应窗口 resize（StatisticsDashboard + Home，含 onUnmounted 销毁）
- [x] 全部 18 个 Vue 页面迁移至 API 模块（零残留 `request({})` 裸调）
- [x] 退款功能前端已对接（OrderList.vue 退款按钮）

### 第四步 ✅ 继续打磨 — 全部完成 (2026-06-07)

#### 🔴 高优先级（安全/体验核心）— ✅ 全部完成 (2026-06-07)

| # | 任务 | 位置 | 说明 |
|---|------|------|------|
| 13 ✅ | 后端 API 权限校验 | `common/annotation/RequireRole.java` + `AuthInterceptor` | 自定义 `@RequireRole` 注解 + 拦截器校验。JWT 含角色编码，UserContext 存角色。13 个 Controller 全部标注角色权限 |
| 14 ✅ | Token 续期机制 | `utils/JwtUtil.java` + `AuthController.refresh` | 双 Token：access 30min（含角色） + refresh 7d（仅 userId）。前端 Axios 401 自动静默续期，并发请求排队防重复刷新 |
| 15 ✅ | 操作审计日志 | `common/aspect/OperationLogAspect.java` | `@OperationLog` 注解 + AOP 环绕切面。记录操作人/IP/参数/耗时/异常，输出到 `[AUDIT]` 日志 |

#### 🟡 中优先级（功能完整性）— ✅ 全部完成 (2026-06-07)

| # | 任务 | 位置 | 说明 |
|---|------|------|------|
| 16 ✅ | DataInitializer 作用域收窄 | `config/DataInitializer.java` | 添加 `@Profile("dev")`，确保仅在开发环境执行 |
| 17 ✅ | 前端全局错误边界 | `frontend/src/App.vue` | `onErrorCaptured` 捕获子组件异常，显示友好错误页（重试/回首页），不再白屏 |
| 18 ✅ | 订单退款审批流程 | `order/` 后端+前端 | 状态机：已完成→待审核退款→已退款/已拒绝。收银员可申请退款，管理员/经理审批 |
| 19 ✅ | 统计页日期选择器 | `StatisticsDashboard.vue` | `el-date-picker` 日期范围筛选 + 快捷选项，后端支持 `startDate/endDate` 按范围查询 |
| 20 ✅ | 会员生日/到期提醒 | `Home.vue` + 后端 | 首页新增"今日生日会员"+"7天内到期"两张卡片，后端 `selectTodayBirthdays`+`selectExpiringSoon` |

#### 🟢 低优先级（体验优化）— ✅ 全部完成 (2026-06-07)

| # | 任务 | 位置 | 说明 |
|---|------|------|------|
| 21 ✅ | 统一加载骨架屏 | `Home.vue` + `SkeletonCard.vue` | 首页卡片和图表区域使用 `<el-skeleton>` 骨架占位，加载完成后渐变显示 |
| 22 ✅ | 移动端适配 | `Layout.vue` + `responsive.css` | 768px 以下侧边栏自动收起+遮罩层，选中菜单自动关闭；表格横向滚动；对话框全宽适配 |
| 23 ✅ | 后端单元测试 | `src/test/` 3个测试类 | 15 个集成测试用例覆盖认证/会员/收银核心链路，全部通过 |

#### 项目现状评估

**优点：**
- 代码分层清晰（entity→mapper→service→controller），MyBatis-Plus 使用规范
- RBAC 权限模型完整（用户→角色→菜单 三级）
- 后端统一响应体 `Result<T>` + 全局异常拦截
- 前端 API 层封装完整，零裸调
- DataInitializer 自愈机制，启动即修复缺失数据
- ECharts 生命周期管理（resize + dispose）

**现状（2026-07-16 实际运行验证通过）：**
- `@RequireRole` + `AuthInterceptor` 权限拦截完整，所有 12 个 Controller 全部标注（07-16 补全 8 个）
- `@OperationLog` + `OperationLogAspect` 审计日志完整，所有写操作均已标注
- JWT 含角色编码（userId + username + roles），UserContext 完整存储
- 双令牌机制：access token 30min + refresh token 7d，`/api/auth/refresh` 端点已实现
- 统计模块：`selectTodayBirthdays` / `selectExpiringSoon` / `selectSalesTrendByRange` / `selectTotalProducts` 全部实现
- DashboardVO 含 totalProducts，仪表盘 8 项数据指标完整
- 配送管理模块：Delivery 实体 + Controller（分页/揽件/送达）+ DeliveryList.vue 前端页面

---

## 📋 修复记录

### 🟢 2026-07-16：权限补全 + 双令牌 + Bug 修复（运行验证）

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 35 | `ProductController.java` | 改 | 加 `@RequireRole`（类级 ADMIN/MANAGER，page/detail/search 加 CASHIER）+ `@OperationLog` |
| 36 | `CategoryController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN","ROLE_MANAGER"})` + `@OperationLog` |
| 37 | `OrderController.java` | 改 | 加 `@RequireRole`（三角色均可） + checkout/refund 加 `@OperationLog` |
| 38 | `MemberBenefitController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN","ROLE_MANAGER"})` + `@OperationLog` |
| 39 | `PointsController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN","ROLE_MANAGER"})` + `@OperationLog` |
| 40 | `UserController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN"})` + 全部写操作加 `@OperationLog` |
| 41 | `RoleController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN"})` + `@OperationLog` |
| 42 | `MenuController.java` | 改 | 加 `@RequireRole({"ROLE_ADMIN"})` + `@OperationLog` |
| 43 | `JwtUtil.java` | 改 | 新增 `generateRefreshToken()` / `getUserIdFromRefreshToken()` |
| 44 | `application.yml` | 改 | JWT expiration 改为 30min，新增 refresh-expiration 7d |
| 45 | `LoginVO.java` | 改 | 新增 `refreshToken` 字段 |
| 46 | `AuthServiceImpl.java` | 改 | 登录时同时生成 access token + refresh token |
| 47 | `AuthController.java` | 改 | 新增 `POST /api/auth/refresh` 端点（排除拦截器） |
| 48 | `WebMvcConfig.java` | 改 | `/api/auth/refresh` 加入 excludePathPatterns |
| 49 | `user.js` (store) | 改 | 登录时存储 refreshToken 到 localStorage |
| 50 | `Home.vue` | 改 | 生日/到期提醒改为 `.length` 取值 |
| 51 | `DashboardVO.java` | 改 | 新增 `totalProducts` 字段 |
| 52 | `StatisticsMapper.java` | 改 | 新增 `selectTotalProducts()` |
| 53 | `StatisticsMapper.xml` | 改 | 新增 `selectTotalProducts` SQL |
| 54 | `StatisticsController.java` | 改 | dashboard 接口填充 totalProducts |
| 55 | `CLAUDE.md` | 改 | 更新登录密码表、现状描述 |

> 运行验证：后端编译 `BUILD SUCCESS`，3 账号登录通过，权限矩阵全部正确（403 拦截生效），双令牌刷新通过。

### 🟡 2026-07-16 第二批：功能打磨 + Bug 修复

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 56 | `OrderStatusEnum.java` | 改 | 退款状态机重构：COMPLETED(1)→PENDING_REFUND(2)→REFUNDED(3)/REFUND_REJECTED(4)+CANCELLED(5) |
| 57 | `OrderController.java` | 改 | 新增 `/refund`(申请)、`/approve-refund`(审批)、`/reject-refund`(拒绝) 三个退款端点 |
| 58 | `IOrderService.java` / `OrderServiceImpl.java` | 改 | 审批通过时恢复库存+积分+会员消费额；拒绝时恢复订单状态 |
| 59 | `CategoryManage.vue` | 重写 | el-table 扁平展示 → el-tree 树形展示，支持行内新增/编辑/删除 |
| 60 | `data.sql` | 改 | 会员等级精简为 2 级：白金会员(260/年)、卓越会员(680/年)；角色中文名改为 系统管理员/店长/收银员/配送员 |
| 61 | `schema.sql` | 改 | member_level/member_benefit/product_category/order 表添加 `update_time` 列 |
| 62 | `Layout.vue` | 改 | 新增"修改密码"按钮 + 弹窗表单（旧密码/新密码/确认密码） |
| 63 | `AuthController.java` / `IAuthService.java` / `AuthServiceImpl.java` | 改 | 新增 `PUT /api/auth/changePwd` 修改密码端点 |
| 64 | `Delivery.java` / `DeliveryMapper.java` / `DeliveryController.java` | 新建 | 配送管理模块：实体+Mapper+Controller（分页/创建/揽件/完成送达） |
| 65 | `DeliveryList.vue` / `delivery.js` | 新建 | 配送订单页面 + 前端 API（待配送/配送中/已送达 状态标签） |
| 66 | `router/index.js` | 改 | 新增 `/delivery/list` 路由，配送员角色可访问 |
| 67 | `DataInitializer.java` | 改 | 新增 ROLE_DELIVERY 角色菜单自愈（仅 配送管理+配送订单，无首页） |
| 68 | `Login.vue` | 改 | 登录后根据角色菜单动态跳转：有首页→/home，无首页→第一个可用菜单 |
| 69 | `request.js` | 改 | 修复 `isRefreshing` 刷新后未重置 bug（导致后续401请求永久挂起） |
| 70 | `Home.vue` | 改 | 修复 reminderCards 取值：`d.todayBirthdays.length` → `(d.todayBirthdays && d.todayBirthdays.length) \|\| 0` |
| 71 | `seed_demo_data.py` | 新建 | 演示数据生成脚本：30会员+189订单+60积分+26配送记录 |
| 72 | `frontend/src/api/auth.js` | 改 | 新增 `changePassword()` API |
| 73 | `frontend/src/api/order.js` | 改 | 新增 `approveRefund()` / `rejectRefund()` API |
| 74 | `frontend/src/utils/auth.js` | 改 | 新增 `getRefreshToken()` / `setRefreshToken()` 工具函数 |
| 75 | `frontend/src/store/user.js` | 改 | 登录时存储 refreshToken |

> 运行验证：4 账号登录通过（含 delivery01），配送员直接进 /delivery/list 无 403，退款三态流转正常，分类树展开折叠正常，修改密码生效。

### 🟣 2026-06-16：补全注解 + 权限 + 审计（编译修复）

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 24 | `common/annotation/RequireRole.java` | 新建 | `@RequireRole({"ROLE_ADMIN","ROLE_MANAGER"})`，类级/方法级 |
| 25 | `common/annotation/OperationLog.java` | 新建 | `@OperationLog("描述")`，方法级 |
| 26 | `common/aspect/OperationLogAspect.java` | 新建 | AOP 环绕，记录 `[AUDIT]` 操作人+耗时+异常 |
| 27 | `utils/JwtUtil.java` | 改 | `generateToken(userId, username, roles)` 含角色；新增 `getRolesFromToken()` |
| 28 | `utils/UserContext.java` | 改 | 新增 `currentRoles` ThreadLocal |
| 29 | `config/AuthInterceptor.java` | 改 | 解析 JWT 角色→UserContext；检查 `@RequireRole`（方法级优先→类级） |
| 30 | `AuthServiceImpl.java` | 改 | 登录时 `roleService.listByUserId()` 提取 roleCode 写入 Token |
| 31 | `StatisticsMapper.java` | 改 | 新增 `selectTodayBirthdays()` / `selectExpiringSoon()` / `selectSalesTrendByRange()` |
| 32 | `StatisticsMapper.xml` | 改 | 新增 3 条对应 SQL |
| 33 | `DashboardVO.java` | 改 | 新增 `todayBirthdays` / `expiringSoon` 字段 |
| 34 | `pom.xml` | 改 | 新增 `spring-boot-starter-aop` 依赖 |

> 编译结果：`BUILD SUCCESS`，89 个源文件全部通过。

---

### 🔴 第一步：端口通路

### 🔴 第一步：端口通路

| # | 文件 | 位置 | 修复 | 原因 |
|---|------|------|------|------|
| 1 | `src/main/resources/application.yml:2` | `port: 801180 → 8080` | 端口 801180 超出 TCP 最大值 65535，且与 Vite 代理 8080 不一致 | 后端端口必须与 Vite proxy target 一致 |
| 2 | `.mvn/wrapper/` (新增) | Maven Wrapper 生成 | `mvn` 不在 PATH，每次需完整路径 | 项目自包含构建工具，`./mvnw` 即可 |
| 3 | `~/.m2/settings.xml` | 添加 `<pluginGroup>org.springframework.boot</pluginGroup>` | `spring-boot:run` 前缀解析失败 | 阿里云镜像缺少插件元数据 |
| 4 | `start.bat` + `start.sh` (新增) | 项目根目录 | 一键设置 JAVA_HOME、启动后端+前端、打开浏览器 | 消除手动配置的重复劳动 |

### 🟡 第二步：痛点修复

| # | 文件 | 位置 | 修复 | 原因 |
|---|------|------|------|------|
| 5 | `frontend/src/views/Login.vue:48` | catch 块 + import | 添加 `ElMessage` 错误/成功提示 | 原 `catch` 空实现，登录失败用户无感知 |
| 6 | `frontend/src/views/order/POSCheckout.vue:200` | handleCheckout catch | 同上 + 成功提示 | 收银是核心功能，失败必须告知原因 |
| 7 | `sql/data.sql:13-17` | BCrypt 哈希 | 重新生成 `123456` → `$2a$10$JtREEpm...` | 原哈希与明文不匹配，cashier01/manager01 无法登录 |
| 8 | `config/DataInitializer.java:22-31` | run() | `if (admin != null)` → `if (admin == null)` 仅首次创建 | 原逻辑每次重启覆盖密码，生产不可用 |
| 9 | `frontend/src/api/*.js`（6 新文件） | 新增 API 封装层 | member / product / order / statistics / system / points | 原代码在各页面 `request({})` 裸调，接口散落 |

### 🟢 第三步：质量补全

| # | 文件 | 位置 | 修复 | 原因 |
|---|------|------|------|------|
| 10 | `frontend/src/views/statistics/StatisticsDashboard.vue` | script setup | chartInstances + onUnmounted dispose + window resize | 图表不销毁会内存泄漏；不 resize 窗口缩放不跟随 |
| 11 | `frontend/src/views/Home.vue` | script setup | 同上 | 同上 |
| 12 | 11 个 Vue 页面 | import + 所有 fetch/save 函数 | `request({})` → API 模块调用 | 统一接口管理，便于维护 |

### 架构适配原因

| 原因 | 说明 |
|------|------|
| 前后端分离 | 端口一致是基础——Vite 代理和 Spring Boot 必须精确对齐 |
| 门店收银场景 | POS 结算不可靠 → 真实业务受影响 |
| 多角色系统 | 三个角色账号，2/3 密码不通 → 无法完整测试 |
| 生产就绪 | DataInitializer 暴力重置密码 → 演示/上线不可接受 |
| 数据可视化 | ECharts 不清理 → 长时间运行内存泄漏、仪表盘变卡 |
| 代码可维护性 | 18 页面 40+ 处裸调 → 改接口需全局搜索，API 层统一管理 |

## Architecture

### Backend layers (package: `com.sam.membermgmt`)

- **`common/`** — `Result<T>` (uniform API response: `code`, `message`, `data`, `timestamp`), `PageResult`, `BaseEntity` (provides `id`, `createTime`, `updateTime`), enums, `BusinessException`, `GlobalExceptionHandler` (`@RestControllerAdvice`)
- **`common/annotation/`** — `@RequireRole` (class-level/method-level RBAC role check), `@OperationLog` (AOP-triggered audit logging)
- **`common/aspect/`** — `OperationLogAspect` (surrounds `@OperationLog` methods, logs operator/user/IP/duration/exception to `[AUDIT]`)
- **`config/`** — MyBatis-Plus (pagination plugin, MapperScan), Swagger/Knife4j, CORS, BCrypt, `AuthInterceptor` + `WebMvcConfig` (interceptor on `/api/**`, extracts JWT→UserContext, enforces `@RequireRole` annotation, excludes login + Swagger paths)
- **`utils/`** — `JwtUtil` (HS256, configurable secret/expiration, stores userId+username+roles in claims), `UserContext` (ThreadLocal for current userId/username/roles)
- **`modules/<domain>/`** — each domain follows `entity → mapper → service → controller`:
  - `system` — auth (login, current user), user/role/menu CRUD, RBAC associations
  - `member` — members, levels, benefits, points records, level change records
  - `product` — categories, products, stock records
  - `order` — POS checkout (cart → order with points/discount calculation), order query
  - `statistics` — dashboard (uses custom Mapper XML at `resources/mapper/StatisticsMapper.xml`)

### Key backend patterns

- Services extend MyBatis-Plus `ServiceImpl<Mapper, Entity>`, controllers use `LambdaQueryWrapper` for queries
- MyBatis-Plus logical delete on `deleted` column (1=deleted, 0=not deleted)
- Controllers inject both services and mappers directly — mappers are used for simple list queries while services handle transactional business logic
- `@Transactional` on service methods that modify data across tables
- Auth: all `/api/**` requests (except login and Swagger) require `Authorization: Bearer <token>` header

### Frontend structure (`frontend/src/`)

- **`api/`** — `request.js` (Axios instance: baseURL `/api`, auto-attaches Bearer token, 401 redirects to login), domain API modules (e.g., `auth.js`)
- **`store/`** — Pinia stores: `user.js` (token, userInfo, menus), `app.js`
- **`router/`** — Vue Router with route guard: redirects to `/login` if no token, to `/home` if token present on login page
- **`views/`** — `Layout.vue` (sidebar shell), pages organized by domain: `member/`, `order/`, `product/`, `points/`, `statistics/`, `system/`
- **`components/`** — shared components
- **`utils/auth.js`** — token/user persistence helpers using localStorage

### Frontend tech stack

Vue 3 (`<script setup>`), Element Plus (Chinese locale), Pinia, Vue Router, Axios, ECharts via `vue-echarts`
Vite proxies `/api` to `localhost:8080` in dev; production build outputs to `../src/main/resources/static`

### Database

MySQL database `sam_member_mgmt`. Run `sql/schema.sql` to initialize tables and `sql/data.sql` for seed data. MyBatis-Plus `map-underscore-to-camel-case: true` handles column-to-field mapping.
