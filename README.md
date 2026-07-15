<p align="center">
  <h1 align="center">AI-FinOps Community Edition</h1>
  <p align="center">LLM Multi-Model Intelligent Routing Gateway with Cost Management</p>
</p>

<p align="center">
  <a href="./README.zh-CN.md">中文</a> · <b>English</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/java-17-blue" alt="Java 17">
  <img src="https://img.shields.io/badge/python-3.11+-blue" alt="Python 3.11+">
  <img src="https://img.shields.io/badge/vue-3.x-green" alt="Vue 3">
</p>

---

## What is AI-FinOps?

AI-FinOps is a transparent proxy gateway that sits between your application and LLM providers. It uses a **rule engine + AI classifier** to intelligently route requests — simple queries go to cheap models, complex ones to premium models — cutting your LLM costs without sacrificing quality.

- **Transparent proxy**: Change one `base_url`, zero code changes
- **Smart routing**: Rule engine (multi-dimension matching) + ONNX classifier (real-time complexity scoring)
- **Cost control**: Route cheap tasks to cheap models automatically
- **Enterprise governance**: API Key management, tenant isolation, model allowlists/denylists
- **Full observability**: Request logs, route decision logs, usage dashboards
- **Privacy-first**: Metadata-only logging — never stores prompt or response content

## Architecture

```
                     ┌──────────────────────────────────┐
                     │     afo-system-gateway :8080      │
                     │   Control Plane (User/Role/Menu)   │
                     └──────────┬───────────────────────┘
                                │
  LLM Client ──→ afo-gateway :8081 ──→ llm-router :4000 ──→ Upstream LLM
                     │                        (LiteLLM proxy)
                     ├── classifier-service :8000
                     │   (prompt complexity → route hint)
                     │
                     └── PostgreSQL + Redis + RabbitMQ
```

### Route Decision Engine

Requests flow through a 3-layer decision chain inside the gateway filter pipeline:

| Priority | Layer | Description |
|----------|-------|-------------|
| 1 | **Model Access Control** | Tenant-level model allowlist/denylist |
| 2 | **API Key Scope** | Per-key model access restrictions |
| 3 | **Routing Rules** | Conditional rules (by team, path, model pattern) |
| 4 | **AI Classifier** (fallback) | Prompt complexity → auto-route simple tasks to cheaper models |

## Quick Start

### Prerequisites

- Docker & Docker Compose

### Launch

```bash
git clone https://github.com/<your-org>/ai-finops-community.git
cd ai-finops-community
docker-compose up -d
```

### Access

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:9080 | admin / admin123 |
| Gateway API | http://localhost:8081 | — |
| Control Plane | http://localhost:8080 | — |
| RabbitMQ UI | http://localhost:15672 | ai_finops / ai_finops |

### Send Your First Request

1. Login to the frontend, go to **LLM Management → Provider** and add an upstream provider (e.g., OpenAI, DeepSeek)
2. Create an API Key under **LLM Management → API Key**
3. Route through the gateway:

```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Authorization: Bearer <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o","messages":[{"role":"user","content":"Hello"}]}'
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Gateway | Java 17, Spring Boot 3.5, Spring WebFlux (Reactor) |
| Control Plane | Java 17, Spring Boot 3.5, MyBatis-Plus 3.5, Sa-Token 1.45 |
| Python Services | Python 3.11+, FastAPI, LiteLLM, ONNX Runtime |
| Frontend | Vue 3, Vite 7, TypeScript, Pinia, Naive UI, UnoCSS |
| Database | PostgreSQL 17 |
| Cache | Redis 7 |
| Messaging | RabbitMQ 3 |

## Project Structure

```
ai-finops-community/
├── afo-gateway/              # Reactive proxy gateway (WebFlux, port 8081)
├── afo-system-gateway/       # Control plane (RBAC, menus, API Keys, models)
├── afo-llm/                  # LLM business: providers, models, API keys, billing
├── afo-strategy/             # Routing rules engine & model access policies
├── afo-logs/                 # Async log persistence via RabbitMQ
├── afo-common/               # 13 shared Java libraries
├── classifier-service/       # Python: prompt complexity classifier
├── llm-router/               # Python: LiteLLM multi-protocol proxy (port 4000)
├── app-l0/                   # Vue 3 frontend
└── script/sql/               # Database init scripts (idempotent)
```

## Key Features

- **Rule Engine**: Multi-dimension matching — `team_tag`, `api_key`, `request_path`, `model_name` — with conditional routing rules
- **AI Classifier**: ONNX runtime with Qwen-0.5B for real-time prompt complexity assessment. Simple tasks (confidence ≥0.85) auto-route to cheaper models
- **Semantic Cache**: Response caching by normalized prompt hash. Configurable TTL per tenant. Supports SSE stream caching
- **Whitelist Bypass**: Pattern-based rules to force original model routing, skipping optimization
- **Multi-Protocol**: OpenAI-compatible (`/v1/chat/completions`) + Anthropic (`/v1/messages`) via LiteLLM
- **SSE Streaming**: Full support for server-sent events with streaming cost tracking

## Port Map

| Port | Service | Note |
|------|---------|------|
| 15432 | PostgreSQL | Mapped from container 5432 |
| 16379 | Redis | Mapped from container 6379 |
| 5672 | RabbitMQ AMQP | Standard AMQP |
| 15672 | RabbitMQ Management | Web UI |
| 8080 | afo-system-gateway | Control plane API |
| 8081 | afo-gateway | Gateway proxy |
| 8000 | classifier-service | Prompt classifier |
| 4000 | llm-router | LLM forward proxy |
| 9080 | app-l0 | Frontend (Nginx) |

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md).

This is the **L0 Community Edition** open-source branch. We welcome bug fixes, documentation improvements, and L0-scoped feature contributions.

## License

[Apache License 2.0](./LICENSE)
