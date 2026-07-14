# Agent 指南

## 项目定位

`app` 是 AI-FinOps（简称 `afo`）前端项目，基于 Vue 3、Vite 7、TypeScript、Pinia、Vue Router、Vue I18n、Naive UI 和 UnoCSS 构建。

本目录下的变更只面向前端应用、前端工作区包、前端代码生成模板和前端菜单初始化资料。后端 Java 服务、数据库迁移和根目录 Maven 模块规范以仓库根目录 `Agent.md` 为准。

## 品牌与命名

- 对用户可见的项目名称统一使用 `AI-FinOps`。
- 项目简称、包名、命令展示名和轻量标识统一使用 `afo`。
- 浏览器标题、系统标题、关于页、页脚、水印、首页动态、主题预设和初始化菜单中的项目信息必须保持一致。
- 新增本地存储前缀、生成文件前缀或代码生成路径时优先使用 `AFO_`、`afo-` 或 `afo`。
- 不把旧项目名作为业务文案继续扩散。第三方依赖包名、上游工具注释和真实导入路径可以保留。

## 技术边界

- 包管理器使用 `pnpm`，Node.js 版本必须满足 `package.json` 中的 `>=20.19.0`。
- 默认开发命令在 `app` 目录运行：`pnpm dev`，Vite 默认端口为 `9527`。
- 构建命令：`pnpm build`、`pnpm build:dev`、`pnpm build:test`。
- 类型检查命令：`pnpm typecheck`。
- 代码检查和格式化命令：`pnpm lint`、`pnpm fmt`。
- 当前机器默认 Node 可能不满足 pnpm 要求时，使用已安装的 Node 24 路径：

```bash
PATH=/Users/wangbo/.nvm/versions/node/v24.15.0/bin:$PATH pnpm typecheck
```

## 目录职责

- `src/views`：页面级视图，按业务域组织。
- `src/components`：通用组件、业务组件和自定义基础组件。
- `src/layouts`：布局、顶部栏、侧边栏、页脚和全局框架。
- `src/service`：接口请求、API 封装和请求实例配置。
- `src/store`：Pinia 状态模块。
- `src/router`：路由守卫、路由转换和路由配置。
- `src/locales`：中英文文案。任何用户可见文案变更都需要同步对应语言文件。
- `src/theme`：主题变量、默认设置和主题预设。
- `src/assets`：前端静态资源。项目 logo 使用 `src/assets/svg-icon/logo.svg`。
- `packages/*`：前端 workspace 内部包，只承载前端通用能力。
- `docs/template`、`docs/java`、`docs/sql`：前端相关代码生成模板、生成路径规则和菜单初始化资料。

## 生成文件与自动化边界

- `src/router/elegant/routes.ts`、`src/router/elegant/imports.ts` 和相关 `typings` 中标注 generated 的内容是生成产物，优先通过 `pnpm gen-route` 更新。
- `src/typings/components.d.ts` 属于组件自动导入类型声明。仅在组件重命名或自动生成机制无法及时更新时做最小同步。
- 不修改 `node_modules`、`dist`、`.vite`、构建缓存或临时产物。
- 不手改 `pnpm-lock.yaml`，除非依赖版本确实发生变化并由 pnpm 生成。

## 前端实现规范

- 使用 Vue 3 Composition API 和 `<script setup lang="ts">`，保持严格 TypeScript 类型。
- 路径别名优先使用 `@/` 指向 `src`，使用 `~/` 指向 app 根目录。
- API 调用优先放在 `src/service/api`，请求行为通过 `src/service/request` 统一处理。
- 鉴权、token、加密、重复提交和错误提示不要在页面里重复实现，优先复用现有请求封装和 store。
- 页面状态优先局部化；跨页面或跨布局共享状态才放入 Pinia。
- 组件命名使用清晰业务名，和文件名保持可追踪关系。项目专属组件使用 `Afo` 前缀。
- UI 组件优先使用 Naive UI；图标优先使用 Iconify 或已有本地 svg 图标。
- 样式优先使用 UnoCSS utility、已有 CSS 变量和 `src/theme` token，避免局部硬编码大面积颜色体系。
- 新增可见文案时不要只写死中文，需同步 `zh-cn.ts` 和 `en-us.ts`，并通过 `$t` 使用。

## 路由与菜单

- 静态路由遵循 Elegant Router 现有命名和转换规则。
- 动态路由由后端菜单下发时，前端只维护常量路由、守卫和转换边界。
- 菜单初始化 SQL 中的项目链接、名称、图标和说明应与 `AI-FinOps` / `afo` 品牌保持一致。
- 新增页面时同步检查路由 i18n key、菜单名称和页面标题。

## 主题与品牌边界

- 默认水印文本使用 `AI-FinOps`。
- 主题预设中 AI-FinOps 预设文件使用 `afo.json`，i18n key 使用 `theme.appearance.preset.afo`。
- 旧项目资源或头像不应继续作为默认品牌视觉；默认头像优先使用 `src/assets/svg-icon/logo.svg`。
- 修改主题预设时同步检查 `src/theme/settings.ts`、`src/theme/preset/*.json` 和语言包中的预设描述。

## 验证要求

- 修改 TypeScript、Vue、路由、store、service 或主题配置后，至少运行 `pnpm typecheck`。
- 修改格式、lint 规则、导入、组件注册或大范围代码时，运行 `pnpm lint` 和 `pnpm fmt`。
- 修改构建配置、依赖、环境变量或资源路径时，运行对应模式的 `pnpm build`。
- 修改页面视觉或交互后，启动 `pnpm dev` 并用浏览器检查关键页面。
- 如果验证命令因为本地 Node、依赖或环境限制无法运行，需要在最终说明中明确原因。

## 协作规则

- 变更保持聚焦，不顺手重构无关模块。
- 遇到已有未提交变更时，先确认是否相关；无关变更不要回滚或格式化。
- 保留现有中文领域术语和业务语义。
- 不提交密钥、真实凭据、个人本地路径、日志和机器相关配置。
- 交付时说明修改范围、用户可见变化和已运行的验证命令。
