"""路由定义：/health 和 /classify 两个端点。

这两个端点的路径、请求/响应格式必须与网关 ClassifierClient 的契约严格一致，
否则 Java 端 Jackson 反序列化会失败。
"""
from fastapi import APIRouter
from fastapi.responses import PlainTextResponse
from schemas import ClassifyRequest, ClassifyResponse
from classifier import Classifier
from config import Settings

# 模块级单例：Settings 从 .env 读取，Classifier 持有一个 OpenAI 客户端
router = APIRouter()
_settings = Settings()
_classifier = Classifier(_settings)


@router.get("/health", response_class=PlainTextResponse)
async def health():
    """网关 ClassifierHealthChecker 每 30s 调用此端点。
    期望响应中包含 "status":"ok" 或 "healthy":true 字符串。
    """
    return '{"status":"ok"}'


@router.post("/classify")
async def classify(req: ClassifyRequest) -> ClassifyResponse:
    """网关 ClassifierClient.classify() 调用的分类端点。
    服务层再做一层 512 字符截断（防御层：即使网关忘记截断也不会拖垮 LLM）。
    所有异常由 classifier.py 内部兜底为 is_simple=false，不会抛到路由层。
    """
    truncated = req.prompt[:512] if req.prompt else ""
    result = _classifier.classify(truncated)
    return result
