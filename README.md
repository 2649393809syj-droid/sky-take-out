# 苍穹外卖（sky-take-out）

餐饮外卖平台：用户端微信小程序 + 管理端 Web，覆盖完整外卖业务闭环（浏览菜品 → 购物车 → 下单 → 微信支付 → 订单流转 → 来单提醒）。

## 技术栈

Java · Spring Boot · MyBatis · MySQL · Redis · WebSocket · 微信支付 · Spring Task · Nginx

## 核心实现

- **用户端核心链路**：微信登录（JWT 双端拦截器）、菜品/套餐浏览（Redis + Spring Cache 缓存）、购物车、下单、微信支付回调验签、订单状态流转；Spring Task 定时取消超时未支付订单
- **管理端与统计报表**：员工管理（BCrypt 密码 + JWT 鉴权）、菜品/分类/套餐管理、订单管理；营业额/用户/订单/销量 Top10 报表与 Excel 导出；AOP 公共字段自动填充（`@AutoFill` 切面）
- **工程化与性能优化**：
  - `@Idempotent` 注解 + 拦截器 + Redis **Lua 原子 GET/DEL** 做订单提交幂等防重
  - `@Async` 自定义线程池（核心 10 / 最大 20 / 队列 50，CallerRuns 拒绝策略）异步 WebSocket 推送来单/催单
  - orders 表 **复合索引 `(status, order_time)`** 优化分页查询
  - OSS 图片上传、全局异常处理器、Nginx 部署

## 目录结构

```
sky-server/     Spring Boot 后端（controller / service / mapper / interceptor / config / websocket）
sky-common/     公共模块（结果封装、JWT、OSS、微信支付、状态常量等）
sky-pojo/       实体、DTO / VO
db_optimization.sql   索引优化脚本
```

## 启动方式

1. MySQL 初始化库表（`sky.sql`），Redis 启动
2. `sky-server` 配置 `application.yml` / `application-dev.yml`（数据库、Redis、OSS、微信支付配置）
3. `mvn spring-boot:run` 启动后端；前端用微信开发者工具打开用户端，浏览器打开管理端

## 说明

- 本仓库基于经典教学项目二次开发，重点沉淀了幂等防重、异步推送、索引优化等工程实践。
- 密钥（阿里云 OSS、微信支付、百度地图）一律通过环境变量注入（`${SKY_ALIOSS_ACCESS_KEY_ID:}` 等），仓库不提交任何真实凭证。
