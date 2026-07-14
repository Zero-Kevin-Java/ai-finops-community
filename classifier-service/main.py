"""AI-FinOps 分类器服务 — 应用入口。

分类器服务是一个无状态 HTTP 服务，
网关 RouteDecisionEngine 调用其 /classify 端点判断 Prompt 是否简单任务。
"""
import uvicorn
from fastapi import FastAPI
from router import router
from config import Settings

# 网关 ClassifierClient 发送 POST /classify，ClassifyResponse 由 Pydantic 序列化后返回
app = FastAPI(title="AI-FinOps Classifier Service", version="1.0.0")
app.include_router(router)

if __name__ == "__main__":
    settings = Settings()
    # 端口 8000 与网关 application.yml 中 classifier.base-url: http://127.0.0.1:8000 对齐
    uvicorn.run(app, host=settings.host, port=settings.port)
