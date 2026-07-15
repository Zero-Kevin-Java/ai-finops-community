<p align="center">
  <h1 align="center">AI-FinOps Community Edition</h1>
  <p align="center">LLM 多模型智能路由网关 · 成本精细化管控</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/java-17-blue" alt="Java 17">
  <img src="https://img.shields.io/badge/python-3.11+-blue" alt="Python 3.11+">
  <img src="https://img.shields.io/badge/vue-3.x-green" alt="Vue 3">
</p>

<p align="center">
  <a href="./README.md">English</a> · <b>中文</b>
</p>

---

## 什么是 AI-FinOps？

AI-FinOps 是一个位于你的应用与 LLM 供应商之间的透明代理网关。它通过**规则引擎 + AI 分类器**智能路由请求——简单问题自动分发到便宜模型，复杂任务路由到高级模型——在不牺牲回复质量的前提下大幅降低 LLM 调用成本。

- **透明代理**：改一个 `base_url`，零代码侵入
- **智能路由**：规则引擎（多维匹配） + ONNX 分类器（实时复杂度评分）
- **成本管控**：简单任务自动走低成本模型
- **企业治理**：API Key 管理、租户隔离、模型黑白名单
- **全链路可观测**：请求日志、路由决策日志、用量看板
- **隐私优先**：仅记录元数据，不存储 prompt / response 原文

## 架构

```
                     ┌──────────────────────────────────┐
                     │     afo-system-gateway :8080      │
                     │    控制面（用户 / 角色 / 菜单）      │
                     └──────────┬───────────────────────┘
                                │
  LLM Client ──→ afo-gateway :8081 ──→ llm-router :4000 ──→ 上游 LLM
                     │                  （LiteLLM 代理）
                     ├── classifier-service :8000
                     │   （任务复杂度 → 路由建议）
                     │
                     └── PostgreSQL + Redis + RabbitMQ
```

### 路由决策引擎

请求在网关过滤链中依次经过 3 层决策：

| 优先级 | 层级 | 说明 |
|--------|------|------|
| 1 | **模型准入控制** | 租户级别模型白名单/黑名单 |
| 2 | **API Key 范围** | 按 Key 限制可访问的模型 |
| 3 | **路由规则** | 条件路由（按团队、路径、模型模式） |
| 4 | **AI 分类器**（兜底） | 任务复杂度评估 → 简单任务走便宜模型 |

## 快速开始

### 前置条件

- Docker & Docker Compose

### 一键启动

```bash
git clone https://github.com/<your-org>/ai-finops-community.git
cd ai-finops-community
docker-compose up -d
```

### 访问地址

| 服务 | URL | 账号 |
|------|-----|------|
| 前端 | http://localhost:9080 | admin / admin123 |
| 网关 API | http://localhost:8081 | — |
| 控制面 API | http://localhost:8080 | — |
| RabbitMQ 管理 | http://localhost:15672 | ai_finops / ai_finops |

### 发送第一个请求

1. 登录前端，进入 **LLM 管理 → Provider**，添加上游供应商（如 OpenAI、DeepSeek）
2. 进入 **LLM 管理 → API Key**，创建 API Key
3. 通过网关发送请求：

```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Authorization: Bearer <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o","messages":[{"role":"user","content":"你好"}]}'
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 网关 | Java 17, Spring Boot 3.5, Spring WebFlux (Reactor) |
| 控制面 | Java 17, Spring Boot 3.5, MyBatis-Plus 3.5, Sa-Token 1.45 |
| Python 服务 | Python 3.11+, FastAPI, LiteLLM, ONNX Runtime |
| 前端 | Vue 3, Vite 7, TypeScript, Pinia, Naive UI, UnoCSS |
| 数据库 | PostgreSQL 17 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3 |

## 项目结构

```
ai-finops-community/
├── afo-gateway/              # 响应式代理网关（WebFlux，端口 8081）
├── afo-system-gateway/       # 控制面（RBAC、菜单、API Key、模型管理）
├── afo-llm/                  # LLM 业务：供应商、模型、API Key、计费
├── afo-strategy/             # 路由规则引擎与模型准入策略
├── afo-logs/                 # 基于 RabbitMQ 的异步日志持久化
├── afo-common/               # 13 个共享 Java 库
├── classifier-service/       # Python：任务复杂度分类器
├── llm-router/               # Python：LiteLLM 多协议代理（端口 4000）
├── app-l0/                   # Vue 3 前端
└── script/sql/               # 数据库初始化脚本（幂等）
```

## 核心特性

- **规则引擎**：多维匹配——`team_tag`、`api_key`、`request_path`、`model_name`——支持条件路由规则
- **AI 分类器**：ONNX 运行 Qwen-0.5B，实时评估任务复杂度。简单任务（置信度 ≥0.85）自动路由到低成本模型
- **语义缓存**：按归一化 prompt 哈希缓存响应，按租户配置 TTL，支持 SSE 流式缓存
- **白名单豁免**：基于 pattern 的规则，命中后强制走原模型，不参与路由优化
- **多协议支持**：OpenAI 兼容（`/v1/chat/completions`）+ Anthropic（`/v1/messages`），通过 LiteLLM 统一转发
- **SSE 流式**：完整支持 Server-Sent Events，含流式成本追踪

## 端口规划

| 端口 | 服务 | 说明 |
|------|------|------|
| 15432 | PostgreSQL | 映射容器 5432 |
| 16379 | Redis | 映射容器 6379 |
| 5672 | RabbitMQ AMQP | 标准 AMQP |
| 15672 | RabbitMQ Management | Web 管理界面 |
| 8080 | afo-system-gateway | 控制面 API |
| 8081 | afo-gateway | 网关代理 |
| 8000 | classifier-service | 任务分类器 |
| 4000 | llm-router | LLM 转发代理 |
| 9080 | app-l0 | 前端（Nginx） |

## 参与贡献

详见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

本项目为 **L0 社区版**开源分支。欢迎提交 Bug 修复、文档改进和 L0 范围内的功能贡献。

## 开源协议

[Apache License 2.0](./LICENSE)
