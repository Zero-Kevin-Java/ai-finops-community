# afo-llm Agent 指南

## 模块定位

`afo-llm` 是 AI-FinOps 的 LLM 业务模块，位于 `afo-modules`。它负责 Spring Boot 业务平台层的 LLM 管理、准入、用量、审计和计费。

LiteLLM 只作为模型网关层存在，具体调用通过 `afo-extend/afo-litellm` 完成。

## 架构边界

调用方向：

```text
afo-admin -> afo-llm -> afo-litellm -> LiteLLM Proxy
```

`afo-llm` 负责：

- 租户、项目、应用、API Key。
- 模型目录和模型访问策略。
- 企业价格、额度、余额、账单、计费流水。
- 请求日志、usage 落库、审计事件。
- 面向后台的 CRUD、分页、导出、状态切换。
- 面向业务调用方的受控 OpenAI-compatible API。
- PostgreSQL DDL、初始化数据、菜单、权限、字典、参数。
- 前端 LLM 后台页面和 i18n。

`afo-llm` 不负责：

- LiteLLM provider adapter。
- LiteLLM routing、fallback、load balancing。
- OpenAI 协议兼容实现。
- 修改 LiteLLM 源码。
- 新建独立后台权限系统。

## 代码组织

固定包名：

```text
org.afo.llm
  /controller
  /service
  /service/impl
  /mapper
  /domain
  /domain/bo
  /domain/vo
  /enums
  /billing
  /policy
  /audit
```

当前已落地：

- `LlmModelCatalogController`
- `ILlmModelCatalogService`
- `LlmModelCatalogServiceImpl`
- `LlmModelCatalogMapper`
- `LlmModelCatalog`
- `LlmModelCatalogBo`
- `LlmModelCatalogVo`
- `LiteLlmChatController`：后台受控调用验证入口，不作为业务应用正式入口。

## RuoYi-Vue-Plus 约束

- 复用现有用户、角色、部门、菜单、字典、参数、日志体系。
- Controller 继承现有 `BaseController` 或复用项目已有模式。
- 权限使用 `@SaCheckPermission`。
- 操作日志使用 `@Log` 和 `BusinessType`。
- 防重复提交使用 `@RepeatSubmit`。
- 分页使用 `PageQuery` 和 `TableDataInfo`。
- 返回体使用 `R`，OpenAI-compatible 透传接口除外。
- 导出使用 `ExcelUtil`。
- Mapper 使用 `BaseMapperPlus`。
- 租户隔离优先复用项目租户体系和 `TenantEntity`。

## 数据库生成规则

数据库固定 PostgreSQL。

SQL 放置目录：

```text
afo-modules/afo-llm/src/main/resources/sql/postgres/
```

命名要求：

- LLM 表统一使用 `afo_llm_*`。
- 不生成裸 `llm_*` 表。
- 每张业务表必须有明确主键。
- 每张业务表必须包含 `create_dept`、`create_by`、`create_time`、`update_by`、`update_time`。
- 需要租户隔离的表必须包含 `tenant_id`。
- 业务字段必须写 `COMMENT ON COLUMN`。
- 表必须写 `COMMENT ON TABLE`。
- 索引必须基于真实查询场景，并用 `COMMENT ON INDEX` 说明用途。
- 价格表必须支持生效时间或版本控制。
- usage 必须保留 `prompt_tokens`、`completion_tokens`、`total_tokens` 和原始 usage 扩展字段。

生成 SQL 时同步考虑：

- 初始化数据。
- 字典类型和字典数据。
- 参数配置。
- 菜单和按钮权限。
- 最小可回滚策略。

字典数据如果需要前端多语言，`dict_label` 使用 `dict.<dict_type>.<key>`，并同步维护前端 `dict` i18n。

## 前端生成规则

前端固定在 `app` 目录。

LLM 页面目录：

```text
app/src/views/llm/<feature>/index.vue
app/src/views/llm/<feature>/modules/*.vue
```

接口和类型目录：

```text
app/src/service/api/llm/<feature>.ts
app/src/service/api/llm/index.ts
app/src/typings/api/llm.api.d.ts
```

i18n：

```text
app/src/locales/langs/zh-cn.ts
app/src/locales/langs/en-us.ts
app/src/typings/app.d.ts
```

页面要求：

- 使用 Vue 3、Vite、TypeScript、Naive UI、Pinia、Vue Router、UnoCSS。
- 页面用 `<script setup lang="ts">` 或现有 TSX 风格。
- 参考 `app/src/views/system/*` 现有页面模式。
- 必须包含列表查询、分页、重置、刷新、列显隐、创建、编辑、删除、状态切换等目标业务自然需要的交互。
- API 调用统一放在 `app/src/service/api/llm`。
- 类型统一放在 `Api.Llm` 命名空间。
- 用户可见文案必须通过 `$t`，同步中英文。
- 菜单、权限标识、路由标题和图标同步生成 SQL。
- 路由生成产物不要手写，新增页面后通过构建或项目命令生成。

前端验证：

```bash
cd app
pnpm typecheck
```

涉及路由或构建配置时补充：

```bash
cd app
pnpm build:dev
```

## 子 agents 开发模式

任务较大、可并行或涉及多条独立线索时，优先拆分为主 agent + 子 agents：

- 主 agent：理解目标、拆边界、集成结果、最终验证和汇报。
- explorer：并行阅读代码、确认现有模式、依赖关系和风险点。
- worker：实现边界清晰且文件范围不重叠的具体改动。
- reviewer：只审查，不改文件，输出具体问题、文件和行号。

分配 worker 时必须明确：

- 文件所有权。
- 输出物。
- 验收标准。
- 不要回滚其他人的改动。

主 agent 不应把下一步立即依赖的阻塞任务交给子 agent 后空等，应继续处理不重叠工作。

## 验证命令

后端最小验证：

```bash
mvn -pl afo-modules/afo-llm -am compile
mvn -pl afo-admin -am compile
```

前端最小验证：

```bash
cd app
pnpm typecheck
```

SQL 最小验证：

- 检查 PostgreSQL 语法。
- 检查菜单 component 是否有对应前端页面。
- 检查权限标识和 Controller 注解一致。
- 检查字典 key 是否有中英文 i18n。

业务网关 smoke：

```bash
cd /Users/wangbo/codex/litellm
uv run litellm --config dev_config.yaml --port 4000
```

另开终端执行：

```bash
scripts/apply-llm-smoke-sql.sh

AFO_BASE_URL=http://127.0.0.1:8080 \
AFO_LLM_API_KEY=sk_smoke_dev_only_000000 \
AFO_LLM_MODEL=gpt-4o-mini \
scripts/smoke-llm-gateway.sh
```

如果主机没有 `psql`，默认会通过 `ai-finops-postgres` 容器执行 SQL。也可以显式指定：

```bash
AFO_DB_CONTAINER=ai-finops-postgres scripts/apply-llm-smoke-sql.sh
```

如果要同时验证落库记录，设置 `AFO_DB_URL` 或 `AFO_DB_CONTAINER`：

```bash
AFO_DB_CONTAINER=ai-finops-postgres scripts/smoke-llm-gateway.sh
```

smoke seed 只用于本地或测试环境。默认业务请求模型仍是 `gpt-4o-mini`，但 `afo_llm_model_catalog.litellm_model` 映射到本机 `/Users/wangbo/codex/litellm/dev_config.yaml` 中的 `fake-openai-endpoint`。该 seed 还会在 `llm.gateway.apiKey` 为空时填入 LiteLLM 本地 `master_key`：`sk-1234`。如果本地 LiteLLM Proxy 使用其他别名或密钥，只更新 smoke seed 对应字段。
