# D4 注意事项文档

**日期:** 2026-07-08  
**任务:** afo-system-gateway 新建 + 数据库 DDL  
**开发者:** A（开源拆分）

---

## 一、遗留项（D5 处理）

| # | 遗留项 | 影响 | 归属 |
|---|--------|------|------|
| 1 | afo-system-gateway 缺少 application.yml（数据源/Sa-Token/Redis/RabbitMQ 配置） | 无法运行时启动 | D5 |
| 2 | docker-compose.yml 未编写 | 无法一键启动全栈 | D5 |
| 3 | classifier-service / llm-router 未复制 | 分类器和转发代理不可用 | D5 |
| 4 | 单元测试未迁移（afo-system-gateway 无测试目录） | 无回归测试覆盖 | D5 |
| 5 | 前端 L0 页面未复制 | 控制面无法通过 Web UI 访问 | D6 |
| 6 | 根 POM 的 `<dependencyManagement>` 未声明 afo-system-gateway | 其他模块无法通过版本管理引用 | D5 |

---

## 二、架构约束

| # | 约束 | 说明 |
|---|------|------|
| 1 | afo-system-gateway 是独立 Spring Boot 模块，有自己的 `@SpringBootApplication` 主类 | 不依赖已删除的 afo-system |
| 2 | `@ComponentScan` 扫描 3 个包：`org.afo.system`、`org.afo.common`、`org.afo.llm` | 确保 MyBatis Mapper 和 Spring Bean 正确发现 |
| 3 | `ISysUserTenantService` 已删除，相关调用已改为直接使用 `SysUserTenantMapper` | 对 `ensureMembership`/`recordLastLoginTenant`/`removeMembership` 等调用使用直接 mapper 操作 |
| 4 | `SysSensitiveServiceImpl` 已删除（依赖不存在的 `afo-common-sensitive` 模块） | SysUserVo 和 SysUserProfileBo 中的 `@Sensitive` 注解已移除 |
| 5 | `KeywordExtractor.java` 已删除（依赖不存在的 jieba-analysis 库） | 白名单离线挖掘功能暂不可用 |
| 6 | `SystemApplicationRunner.java` 已删除（依赖 ISysOssConfigService 等非 L0 服务） | 应用启动时的初始化逻辑需在 D5 重新实现 |
| 7 | `SimpleTaskRouteMapper.java` 已删除（依赖 L1 的 SimpleTaskRouteEntity） | GatewayStatisticsController 不依赖此 Mapper |

---

## 三、数据库注意事项

| # | 注意 | 详情 |
|---|------|------|
| 1 | init.sql 包含 26 张表（15 系统 + 7 LLM + 3 路由 + 1 日志），比计划多 7 张 | 新增了 sys_tenant_package、sys_config、sys_dict_type、sys_dict_data、sys_role_dept、sys_user_post、sys_user_tenant — 这些是实体层引用的必需表 |
| 2 | `afo_route_decision_log` 表已移除 `record_only` 列 | L1 字段，L0 版本不需要 |
| 3 | `sys_tenant` 表不含 `shadow_validation_start_at` 和 `smart_config` 字段 | L1 字段，L0 版本不需要 |
| 4 | init.sql 不包含任何 INSERT/UPDATE/DELETE 语句 | 仅 DDL，初始化数据由 D5 处理 |
| 5 | `afo_route_decision_log` 表仅建表（保证 GatewayStatisticsController JdbcTemplate 查询不报错） | 写入端（RouteDecisionLogConsumer）已在 D3 删除 |

---

## 四、代码审核要点

| # | 文件 | 修改类型 | 说明 |
|---|------|---------|------|
| 1 | `SysTenantServiceImpl.java` | 修改 | 删除 ISysUserTenantService 注入，替换为 SysUserTenantMapper 直接操作；删除 getShadowValidationStartAt |
| 2 | `SysUserServiceImpl.java` | 修改 | 删除 ISysUserTenantService 注入，所有 userTenantService 调用替换为直接 mapper 操作 |
| 3 | `ISysTenantService.java` | 修改 | 删除 getShadowValidationStartAt 方法声明 |
| 4 | `SysUserVo.java` | 修改 | 移除 @Sensitive 注解（afo-common-sensitive 模块不存在） |
| 5 | `SysUserProfileBo.java` | 修改 | 移除 @Sensitive 注解 |
| 6 | `SysUserTenant.java` + `SysUserTenantMapper.java` | 新增 | 原计划删除的实体和 Mapper，因 SysUserServiceImpl 直接依赖而保留 |

---

## 五、潜在风险

| 风险 | 严重程度 | 说明 |
|------|---------|------|
| `GatewayStatisticsController` 的 JdbcTemplate SQL 直接操作 afo_route_decision_log 和 afo_llm_api_key 表 | 中 | 依赖数据库中的表，需确保 init.sql 执行成功且表存在 |
| `SysTenantServiceImpl` 中 userTenant 相关操作已内联 | 低 | 直接使用 Mapper 替代 Service，行为与商业版略有差异但 L0 可接受 |
| afo-system-gateway 缺少 Redis 配置可能导致 GatewayCachePublisher 失败 | 低 | GatewayCachePublisher 依赖 StringRedisTemplate，需在 application.yml 中配置（D5 处理） |
| Mapper XML 文件可能不完整 | 中 | 仅复制了 15 个 Mapper XML，非 L0 的 mapper 无 XML 也能编译通过 |
| afo-common 模块中有 `afo-common-log` 等非核心模块存在 | 低 | D1 阶段复制了所有 common 模块但部分未使用 |

---

**文档版本:** v1.0  
**创建时间:** 2026-07-08
