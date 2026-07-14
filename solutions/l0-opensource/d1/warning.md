# D1 Warning — 搭建骨架 + afo-llm 精简

**日期:** 2026-07-07

## 遗留项

| 项目 | 状态 | 计划 |
|------|------|------|
| afo-gateway 含 L1/L2 文件（ScoringService, ShadowStreamingComparator, QuotaGuardFilter 等）| 待删除 | D2 |
| afo-gateway 含 scoring/shadow/quota 相关引用 | 待修剪 | D2 |
| afo-strategy 含评分文件（Scoring*, 需要在 D2/D3 修剪）| 待删除 | D3 |
| afo-logs 含 QuotaGuardEventConsumer | 待删除 | D3 |
| afo-llm 的 bo/vo 已按计划精简（仅保留 6 个实体相关）| 已完成 | — |
| afo-system 完整复制（含 V1.5 + OSS 代码），不在模块列表中 | 待剪切 | D4 |
| afo-common-core 的 service 接口未精简（含 workflow/process 等）| 待处理 | D2 根据编译错误决定 |
| afo-llm/aflo-strategy/aflo-logs 编译状态未知（被 afo-gateway 阻塞）| 待验证 | D2 |

## 验收标准核对

| # | 任务 | 检查项 | 状态 |
|---|------|--------|------|
| 1 | 骨架 | 5 根文件 + git init | ✅ |
| 2 | 根 pom | mvn validate 零错误；13 模块声明正确；无 scoring/embedding | ✅ |
| 3 | common 复制 | 9 模块就位；parent 改为 1.0.0；afo-common/pom.xml 正确 | ✅ |
| 4 | rabbitmq 修剪 | 10 文件已删；@Import 仅 3 个 L0 队列；demo/ 不存在 | ✅ |
| 5 | 业务模块复制 | 4 模块就位；gateway 无 scoring/embedding 依赖 | ✅ |
| 6 | afo-llm 精简 | 6 Controller/6 Mapper/6 Entity；pricing/job/billcompare 已删 | ✅ |
| 7 | 编译验证 | 9 common 编译通过；afo-gateway 29 错误（预期）；错误日志已保存 | ✅ |
| 8 | warning.md | 遗留项/架构约束/D2 准备/潜在风险 四部分完整 | ✅ |
| 9 | 审核修复 | 6 项修复已验证 | ✅ |

## 架构约束

- 开源仓库 commit 历史独立，不与商业项目共享
- 所有 `pom.xml` 的版本使用硬编码 `1.0.0`，不保留 `${revision}` 变量
- 根 POM 中 `<packaging>pom</packaging>`，`<skipTests>true</skipTests>`
- `ip2region` 和 `dynamic-datasource` 已重新加入 root pom 依赖管理（afo-common-core 和 afo-common-mybatis 实际依赖它们）

## D2 修剪准备

- D2 需要先读取 `compile-errors-d1.txt`，定位所有编译错误
- 修剪顺序：先删 L1/L2 文件 → 再修剪方法体 → 最后清理 import
- 关键文件：`GateApplication.java`、`RouteDecisionEngine.java`、`ProxyForwardFilter.java`、`RouteResult.java`、`ScoringService.java`、`ShadowStreamingComparator.java`、`QuotaGuardFilter.java`
- `afo-gateway/scoring/` 目录整体删除（ScoringService.java）
- `afo-gateway/shadow/` 目录中删除 ShadowStreamingComparator.java
- `afo-gateway/filter/QuotaGuardFilter.java` 整体删除
- `afo-gateway/routing/QuotaGuardEventPublisher.java` 整体删除
- `afo-gateway/routing/RabbitQuotaGuardEventPublisher.java` 整体删除
- `RouteDecisionEngine.java` 需移除所有 scoring 相关 import 和方法调用
- `ProxyForwardFilter.java` 需移除 ShadowCompareQueueConfig/RequestLogInsertQueueConfig/ShadowCompareMessage 引用

## 已发现的潜在风险

1. **afo-common-core 的 ip2region 依赖**：计划中删除但代码仍在使用，已恢复至根 POM 依赖管理
2. **afo-common-mybatis 的 dynamic-datasource 依赖**：计划中删除但代码仍在使用，已恢复至根 POM 依赖管理
3. **afo-llm/aflo-strategy/aflo-logs 未编译验证**：被 afo-gateway 阻塞，D2 修复 afo-gateway 后需验证这三个模块
4. **afo-llm 内部可能仍有 import 残留**：删除 Service/Controller/Mapper 后，保留的文件中可能有对其他已删除类的 import（如 LiteLlmClient 等），编译时会暴露
5. **afo-strategy 依赖 afo-llm**：afo-llm 编译通过后 afo-strategy 才可编译
6. **afo-strategy 依赖 afo-common-log/idempotent/excel**：这些模块不在社区版，但 pom.xml 中依赖声明已删除，代码引用将在 D2/D3 处理
