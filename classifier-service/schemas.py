"""Pydantic 请求/响应模型。

字段名与 Java 端 ClassifyRequest / ClassifyResponse 的 @JsonProperty 严格对齐：
- Python is_simple → Java @JsonProperty("is_simple") private boolean isSimple
- Python model_used → Java @JsonProperty("model_used") private String modelUsed

Pydantic 蛇形命名默认序列化为蛇形 JSON 字段名，恰好与 Java 端 Jackson 配置一致。
"""
from pydantic import BaseModel


class ClassifyRequest(BaseModel):
    """对应 Java org.afo.gateway.classifier.ClassifyRequest。"""
    prompt: str = ""
    tenantId: str = "0"


class ClassifyResponse(BaseModel):
    """对应 Java org.afo.gateway.classifier.ClassifyResponse。
    经 FastAPI 序列化后为 {"is_simple":..., "confidence":..., "model_used":...}。
    """
    is_simple: bool = False
    confidence: float = 0.0
    model_used: str = "none"
