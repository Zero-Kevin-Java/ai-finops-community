# D2 Warning — 修剪 afo-gateway 核心

**日期:** 2026-07-07

## 遗留项

| 项目 | 状态 | 计划 |
|------|------|------|
| afo-gateway 模块 L0 修剪完成 | 已完成 | — |
| afo-gateway 编译零错误（`mvn clean compile -pl afo-gateway -am -q`）| 已验证 | — |
| afo-llm 模块编译错误（excel/idempotent/log 依赖 + 实体/VO/Mapper 残留引用）| 待处理 | D3 |
| afo-strategy 模块编译状态未知 | 待验证 | D3 |
| afo-logs 模块编译状态未知 | 待验证 | D3 |
| `scoring/` 和 `shadow/` 目录已清空并删除 | 已完成 | — |

## 验收标准核对

| # | 任务 | 检查项 | 状态 |
|---|------|--------|------|
| 1 | GatewayApplication | 删除 `org.afo.common.embedding` 扫描 | ✅ |
| 2 | pom.xml | 无 `afo-common-scoring`/`afo-common-embedding` 依赖 | ✅（D1 已完成） |
| 3 | 删除 10 个 L1/L2 文件 | 全部不存在；scoring/ / shadow/ 空目录已删除 | ✅ |
| 4 | RouteDecisionEngine | 无 recordOnly/scoringService/ScoringService/executeScoringThenAi；executeAiRouting 保留 | ✅ |
| 5 | ProxyForwardFilter | 无 ShadowRecordFilter/TrialKeyDetect/isRecordOnly/needsComparison/cheapConfigMono/callModelSync/sendMq；RequestLogQueueConfig 已切换；无 scoring Redis 写入 | ✅ |
| 6 | ModelAccessFilter | 无 ShadowRecordFilter；DataBufferUtils.join 路径保留 | ✅ |
| 7 | RouteResult | 无 executionMode/complexityTier/specificityCategory/scoringConfidence/scoringLatencyMs/scoringReason；无 scoringResult()；无 RULE_ENGINE | ✅ |
| 8 | 编译验证 | `mvn clean compile -pl afo-gateway -am -q` 零错误 | ✅ |

## 架构约束

- 商业项目 `D:/project/WL/AI-FinOps` 零改动
- 所有操作均在开源仓库 `D:/project/WL/ai-finops-community` 执行
- 修剪顺序：先删文件 → 再修剪方法体 → 最后清理 import（本次 Plan 的任务顺序正确执行）

## D3 准备

- D3 需要处理 afo-llm 模块的编译错误（见下方风险 #1 详情）
- D3 需要修剪 afo-strategy（删除评分文件 + RoutingConfigRuleServiceImpl）
- D3 需要修剪 afo-logs（删除 L1/L2 文件 + 修剪 LlmRequestLogConsumer + RouteDecisionLogConsumer）
- D3 需要修剪 RouteDecisionLogMessage（D2 已不再设置 recordOnly/scoring* 字段，D3 删除字段定义即可）

## 已发现的潜在风险

### 1. afo-llm 模块编译失败（风险等级：高）

**现象：** `mvn clean compile -pl afo-llm -am` 有大量编译错误，分为四类：

| 类别 | 缺失依赖/类 | 涉及文件 |
|------|------------|----------|
| Excel 注解 | `cn.idev.excel.*`（`ExcelProperty`、`ExcelIgnoreUnannotated`）| 全部 6 个 `*Vo.java` |
| Excel 工具 | `org.afo.common.excel.*`（`ExcelUtils`、`ExcelDictConvert`）| 4 个 Controller |
| 幂等/日志注解 | `org.afo.common.idempotent.*`（`RepeatSubmit`）；`org.afo.common.log.*`（`Log`、`BusinessType`）| 5 个 Controller |
| 实体/VO/Mapper 残留 | `LlmQuotaAccount`、`LlmUsageRecord`、`LlmCustomerModelPrice`、`LlmModelPriceTier` 及对应 Mapper/VO | `LlmAppClientServiceImpl`、`LlmModelCatalogServiceImpl`、`LlmProjectServiceImpl` |

**影响：** afo-llm 是 afo-strategy 的依赖模块，afo-llm 编译不通过则 afo-strategy 也无法编译。D3 必须解决此问题。

**建议修复策略：**
- Controller 层：删除 `@Log`、`@RepeatSubmit` 注解及对应 import（L0 开源版不需要操作审计和防重复提交）
- Vo 层：删除 `@ExcelProperty` 注解及对应 import（L0 开源版不需要 Excel 导出）
- Service Impl 层：删除对已删除的 Entity/Mapper/VO 的 import 和字段注入，删除依赖这些类的业务方法
- 或者：将 `afo-common-excel`、`afo-common-idempotent`、`afo-common-log` 复制到社区版（需评估是否偏离 L0 简洁原则）

### 2. afo-strategy / afo-logs 编译状态未验证（风险等级：中）

D2 仅编译了 afo-gateway 模块。afo-strategy 和 afo-logs 内部可能仍有对 L1/L2 类的 import 残留（如 ScoringService、ShadowStreamingComparator 等已被 D2 删除的类），编译时会暴露。这些属于 D3 范围。

### 3. ProxyForwardFilter 中 resolveConfidence 方法未被调用（风险等级：低）

`resolveConfidence()` 方法原用于影子对比 MQ 发送时获取置信度。删除 `sendMq()` 后，`resolveConfidence()` 不再被任何代码调用，成为死代码。由于该方法仅 5 行且可能被后续 D3 的 LlmRequestLogConsumer 使用，暂保留。

### 4. ObjectNode import 未被使用（风险等级：低）

`ProxyForwardFilter.java` 中 `import com.fasterxml.jackson.databind.node.ObjectNode;` 在删除 `callModelSync()` 方法后不再被使用。编译通过（未使用的 import 在 Java 中只是 warning），不影响功能，后续可清理。
