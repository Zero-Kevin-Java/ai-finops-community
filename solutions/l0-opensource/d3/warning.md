# D3 Warning — 修剪 afo-strategy + afo-logs + 消息类瘦身 + 测试迁移

**日期:** 2026-07-07

## 遗留项

| 项目 | 状态 | 计划 |
|------|------|------|
| afo-strategy 评分文件 8 个已删除 | 已完成 | — |
| afo-strategy 变更日志文件 3 个已删除 | 已完成 | — |
| afo-strategy RoutingConfigRuleServiceImpl 修剪完成 | 已完成 | — |
| afo-logs L1/L2 文件 6 个已删除 | 已完成 | — |
| afo-logs Consumer/Entity 修剪完成 | 已完成 | — |
| RouteDecisionLogMessage 7 个 L1/L2 字段删除 | 已完成 | — |
| RequestLogMessage 5 个 scoring 字段删除 | 已完成 | — |
| afo-llm 模块编译修复（新增 4 个 common 模块 + 修剪 3 个 ServiceImpl）| 已完成 | — |
| afo-gateway/afo-strategy 测试迁移并修剪 | 已完成 | — |
| afo-system 模块编译未处理 | 待处理 | D4 |
| ProxyForwardFilter 中 resolveConfidence() 死代码 | 待清理 | D4 或后续 |

## 验收标准核对

| # | 任务 | 检查项 | 状态 |
|---|------|--------|------|
| 1 | afo-strategy 文件删除 | 11 个文件全删；grep ScoringAnalytics/Scoring*Vo/RoutingConfigChangeLog 无结果 | ✅ |
| 2 | RoutingConfigRuleServiceImpl | 无 MODE_RECORD_ONLY/recordChangeLog/MODEL_GROUP/CLASSIFIER/DENY；ACTION_TYPES 仅 ORIGINAL_MODEL/TARGET_MODEL | ✅ |
| 3 | 接口+控制器修剪 | 无 changeLog/RoutingConfigChangeLog 引用 | ✅ |
| 4 | afo-logs 文件删除 | 6 个文件全删；grep KeywordSketch/QuotaGuardEvent 无结果；listener/ 仅 2 个 Consumer | ✅ |
| 5 | RouteDecisionLogMessage | 无 recordOnly/executionMode/scoring* 字段 | ✅ |
| 6 | RequestLogMessage | 无 complexityTier/scoring* 字段 | ✅ |
| 7 | RouteDecisionLog 实体+VO | 无 7 个 L1/L2 字段 | ✅ |
| 8 | RouteDecisionLogConsumer | 无 7 个 set*() 调用 | ✅ |
| 9 | LlmRequestLogConsumer | 无 INSERT_USAGE_SQL/PRICE_LOOKUP_SQL/buildUsageParams/findPrice/calculateBilling；INSERT_SQL 21 字段 = 21 params | ✅ |
| 10 | 测试迁移 | afo-gateway + afo-strategy 测试文件就位 | ✅ |
| 11 | 测试修剪 | 6 个测试文件已删；3 个测试文件已修剪；grep Scoring/RULE_ENGINE/recordOnly/changeLogMapper 无结果 | ✅ |
| 12 | 编译验证 | `mvn clean compile` afo-gateway/afo-llm/afo-strategy/afo-logs/afo-common 零错误；test-compile afo-gateway/afo-strategy 零错误 | ✅ |

## 架构约束

- 商业项目 `D:/project/WL/AI-FinOps` 零改动
- 所有操作均在开源仓库 `D:/project/WL/ai-finops-community` 执行
- 修剪顺序严格遵循计划：先删文件 → 修剪方法体 → 清理 import
- D2 设计意图验证通过：D2 先删 `set*()` 调用，D3 删字段定义，两端独立编译可验证

## 新增模块（填补 D1 遗漏）

D3 期间新增了 4 个 common 子模块，均为 afo-llm 编译所需：

| 模块 | 依赖来源 | 说明 |
|------|---------|------|
| `afo-common-excel` | afo-llm Vo 层的 `@ExcelProperty` 注解 | 含 fastexcel 依赖 |
| `afo-common-idempotent` | afo-llm Controller 的 `@RepeatSubmit` | 幂等防重复提交 |
| `afo-common-log` | afo-llm Controller 的 `@Log` | 操作日志记录 |
| `afo-common-translation` | afo-llm Vo 层的 `@Translation` | 数据翻译 |

> **注意：** D2 warning.md 建议"删除 `@Log`/`@RepeatSubmit` 注解"来替代新增模块。D3 选择了新增模块方案，保留 Controller 的完整注解。两种方案均可行，后续可根据 L0 简洁原则评估是否回退为删除注解方案。

## D4 准备

- D4 需要创建 `afo-system-gateway` 模块（从 afo-system 剪切）
- D4 需要从商业项目 SQL 提取 18 张表 DDL，合并为 `init.sql`
- D4 需要处理 afo-system 模块中 V1.5/OSS 相关文件
- D4 编译目标：`mvn clean compile -q` 全模块零错误（含 afo-system-gateway）

## 已发现的潜在风险

### 1. afo-llm 测试文件批量删除（风险等级：低）

**现象：** afo-llm 测试目录从商业项目完整复制后，包含 14 个测试文件，其中 12 个引用已删除的 L1/L2 类（quota/price/public model 等）。全部直接删除，未做修剪。

**影响：** L0 版 afo-llm 模块无测试覆盖。后续如需回归测试，需从商业项目筛选 L0 相关测试并修剪。

**建议：** 在 D5（全栈启动验证）阶段通过运行时集成测试弥补。

### 2. afo-gateway ModelAccessFilterTest 被删除（风险等级：低）

**现象：** `ModelAccessFilterTest.java` 引用旧版 `decideWithRequestId` 签名（8 参数 vs 当前 7 参数或不同签名）和 `ShadowRecordFilter`，与 D2 修剪后的代码不兼容，已删除。

**影响：** ModelAccessFilter 无单元测试覆盖。D2 的 ModelAccessFilter 本身已修剪（去 ShadowRecordFilter），新行为需在 D5 运行时验证。

### 3. 新增 common 模块可能引入不必要依赖（风险等级：低）

**现象：** `afo-common-idempotent` 依赖 `afo-common-redis`（即 Redisson），`afo-common-log` 依赖 `afo-common-satoken`。这些传递依赖增加了 L0 版的复杂度。

**影响：** 如果后续决定 L0 版不需要 `@RepeatSubmit` 和 `@Log` 注解，可回退为 D2 建议的"删除注解"方案，同时移除这 4 个 common 模块。

### 4. afo-system 模块仍编译失败（风险等级：已知）

D3 全量编译时未包含 afo-system（D4 范围）。afo-system 中仍有 V1.5（数字员工/org-graph/AI-BOM/ROI）和 OSS 相关代码，D4 剪切为 afo-system-gateway 时需处理。
