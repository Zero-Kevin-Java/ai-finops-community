# 贡献指南

感谢您对 AI-FinOps Community Edition 的关注！

## 开发环境

- Java 17
- Maven 3.9+
- Node.js 20 + pnpm 10
- Docker + Docker Compose
- Python 3.11+（如需修改分类器/llm-router）

## 代码规范

- Java：遵循阿里巴巴 Java 开发手册，使用 4 空格缩进
- 前端：遵循项目内 oxlint + oxfmt 规则
- 提交信息：遵循 Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:` / `test:`）

## 提交 PR 流程

1. Fork 仓库
2. 创建 feature 分支：`git checkout -b feat/xxx`
3. 本地验证：`mvn test -q` 和 `pnpm typecheck`
4. 提交 PR，描述变更原因和验证方式

## 后端开发

```bash
mvn clean compile -q
mvn test -q
```

## 前端开发

```bash
cd app-l0
pnpm i
pnpm dev
pnpm typecheck
pnpm build
```

## 测试要求

- 新功能必须包含单元测试
- 修改后必须保证 `mvn test -q` 和 `pnpm typecheck` 通过

## 与商业版的关系

本项目是 AI-FinOps 商业产品的 L0 稳态版开源分支。L1/L2 功能不在本仓库开发；本仓库仅接受 L0 范围内的改进、bug 修复和文档更新。

## 许可证

Apache License 2.0
