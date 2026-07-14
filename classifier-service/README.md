# AI-FinOps Classifier Service

Classifier service that determines whether a user prompt is a "simple task", used by the AI-FinOps gateway for routing decisions.

## Quick Start

```bash
pip install -r requirements.txt
cp .env.example .env
# Edit .env and fill in OPENAI_API_KEY
python main.py
```

## API

### `GET /health`
Health check. Returns `{"status": "ok"}`.

### `POST /classify`
Classification request.

**Request body:**
```json
{"prompt": "What is 2+2?", "tenantId": "000000"}
```

**Response:**
```json
{"is_simple": true, "confidence": 0.95, "model_used": "gpt-4o-mini-2024-07-18"}
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `HOST` | `0.0.0.0` | Listen address |
| `PORT` | `8000` | Listen port |
| `OPENAI_API_KEY` | — | API Key (required) |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Compatible endpoint |
| `OPENAI_MODEL` | `gpt-4o-mini` | Classification model |
| `OPENAI_TIMEOUT` | `10` | Timeout in seconds |

## Docker

```bash
docker build -t afo-classifier .
docker run -p 8000:8000 --env-file .env afo-classifier
```
