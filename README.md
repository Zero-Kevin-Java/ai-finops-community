# AI-FinOps Community Edition

AI 算力多模型智能路由网关 — 规则引擎 + AI 分类器驱动的异构路由，透明代理，纯转发不落地。

## 功能特性

- **透明代理**：修改 base_url 即可接入，无需改动业务代码
- **规则引擎**：team_tag / api_key / path / model 多维匹配，支持异构路由
- **AI 分类器路由**：Qwen-0.5B + ONNX 实时评估任务复杂度，简单任务走便宜模型
- **模型准入控制**：企业级模型访问策略，支持按 API Key 拒绝特定模型
- **白名单豁免**：命中 pattern 强制走原模型，不参与路由优化
- **纯转发不落地**：request_logs 仅存元数据，不存储 prompt/response 原文
- **基础看板**：请求数统计、模型占比、冷启动引导
- **llm-router**：Python + litellm 无状态多协议转发

## 架构

```
[Client] → [afo-gateway:8081] → [llm-router:4000] → [Upstream LLM]
                ↓
       [classifier-service:8000]
                ↓
       [afo-system-gateway:8080] (控制面)
                ↓
       [PostgreSQL + Redis + RabbitMQ]
```

## 快速开始

### 前置条件

- Docker + Docker Compose
- Git

### 启动

```bash
git clone <repo-url>
cd ai-finops-community
docker-compose up -d
```

### 访问

- 前端：`http://localhost:9527`（admin / admin123）
- 网关：`http://localhost:8081`
- 控制面 API：`http://localhost:8080`
- RabbitMQ 管理：`http://localhost:15672`（ai_finops / ai_finops）

### 配置上游 Provider

登录后进入「LLM 管理 → Provider」，添加 OpenAI/阿里云等上游 base_url 和 api_key。

### 发送第一个请求

```bash
# 1. 在控制面创建 API Key 并绑定 Provider
# 2. 使用 API Key 发送请求
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Authorization: Bearer <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o","messages":[{"role":"user","content":"Hello"}]}'
```

## 目录结构

```
ai-finops-community/
├── afo-gateway/            # WebFlux 响应式网关
├── afo-system-gateway/     # 控制面（用户/角色/菜单/API Key/模型管理）
├── afo-llm/                # LLM 模型目录/API Key/Provider/项目/应用客户端
├── afo-strategy/           # 规则引擎 + 模型准入策略
├── afo-logs/               # 路由日志消费（运行于网关容器）
├── afo-common/             # 公共模块
├── classifier-service/     # Python 分类器
├── llm-router/             # Python 转发代理
├── app-l0/                 # Vue 3 前端
└── script/sql/init.sql     # 数据库初始化脚本
```

## 端口规划

| 端口 | 服务 | 说明 |
|------|------|------|
| 15432 | PostgreSQL | 映射容器 5432 |
| 16379 | Redis | 映射容器 6379 |
| 5672 | RabbitMQ AMQP | 标准 AMQP 端口 |
| 15672 | RabbitMQ Management | 管理 UI |
| 8080 | afo-system-gateway | 控制面 API |
| 8081 | afo-gateway | 网关代理 |
| 8000 | classifier-service | 分类器 |
| 4000 | llm-router | LLM 转发代理 |
| 9527 | app-l0 | 前端 Nginx |

## 与商业版的关系

本项目是 AI-FinOps 商业产品的 **L0 稳态版**开源分支。L1/L2 功能不在本仓库开发；本仓库仅接受 L0 范围内的改进、bug 修复和文档更新。

## 贡献

见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 许可证

Apache License 2.0
