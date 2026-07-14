from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, StreamingResponse

from .router import chat_completions, stream_chat_completions
from .router import anthropic_messages, stream_anthropic_messages


@asynccontextmanager
async def lifespan(app: FastAPI):
    import litellm
    litellm.drop_params = True
    litellm.set_verbose = False
    _patch_openai_reasoning_params()
    yield


def _patch_openai_reasoning_params():
    from litellm.llms.openai.chat.gpt_transformation import OpenAIGPTConfig
    _original = OpenAIGPTConfig.get_supported_openai_params

    def patched(self, model: str) -> list:
        params = _original(self, model)
        for p in ("thinking", "reasoning_effort", "max_completion_tokens"):
            if p not in params:
                params.append(p)
        return params

    OpenAIGPTConfig.get_supported_openai_params = patched

    import litellm.llms.openai.openai as oai_mod
    _original_map = oai_mod.OpenAIConfig.map_openai_params

    def patched_map(self, non_default_params, optional_params, model,
                    drop_params=None, **kwargs):
        result = _original_map(self, non_default_params, optional_params, model,
                               drop_params=drop_params, **kwargs)
        for p in ("thinking", "reasoning_effort"):
            if p in non_default_params and p in result:
                result.setdefault("extra_body", {})[p] = result.pop(p)
        return result

    oai_mod.OpenAIConfig.map_openai_params = patched_map


app = FastAPI(title="LLM Router", version="1.0.0", lifespan=lifespan)


@app.post("/v1/chat/completions")
async def proxy_chat(request: Request):
    body = await request.json()
    headers = {k.lower(): v for k, v in request.headers.items()}

    if body.get("stream", False):
        return StreamingResponse(
            stream_chat_completions(headers, body),
            media_type="text/event-stream",
        )

    result = await chat_completions(headers, body)
    if isinstance(result, tuple):
        return JSONResponse(result[0], status_code=result[1])
    return JSONResponse(result)


@app.post("/v1/completions")
async def proxy_completions(request: Request):
    """文本补全（兼容 OpenAI）"""
    body = await request.json()
    headers = {k.lower(): v for k, v in request.headers.items()}
    from litellm import acompletion
    kwargs = {
        "model": headers.get("x-litellm-model") or body.get("model"),
        "messages": [{"role": "user", "content": body.get("prompt", "")}],
    }
    api_key = headers.get("x-api-key")
    if api_key:
        kwargs["api_key"] = api_key
    api_base = headers.get("x-api-base")
    if api_base:
        kwargs["api_base"] = api_base
    response = await acompletion(**kwargs)
    return JSONResponse({
        "id": response.id,
        "object": "text_completion",
        "created": response.created,
        "model": response.model,
        "choices": [
            {"text": c.message.content, "index": c.index, "finish_reason": c.finish_reason}
            for c in response.choices
        ],
        "usage": {
            "prompt_tokens": response.usage.prompt_tokens,
            "completion_tokens": response.usage.completion_tokens,
            "total_tokens": response.usage.total_tokens,
        },
    })


@app.post("/v1/embeddings")
async def proxy_embeddings(request: Request):
    """Embedding（兼容 OpenAI）"""
    body = await request.json()
    headers = {k.lower(): v for k, v in request.headers.items()}
    from litellm import embedding
    kwargs = {
        "model": headers.get("x-litellm-model") or body.get("model"),
        "input": body.get("input", ""),
    }
    api_key = headers.get("x-api-key")
    if api_key:
        kwargs["api_key"] = api_key
    api_base = headers.get("x-api-base")
    if api_base:
        kwargs["api_base"] = api_base
    response = await embedding(**kwargs)
    return JSONResponse({
        "object": "list",
        "data": [
            {"object": "embedding", "index": d["index"], "embedding": d["embedding"]}
            for d in response["data"]
        ],
        "model": response["model"],
        "usage": response["usage"],
    })


@app.post("/v1/messages")
async def proxy_anthropic_messages(request: Request):
    body = await request.json()
    headers = {k.lower(): v for k, v in request.headers.items()}

    if body.get("stream", False):
        return StreamingResponse(
            stream_anthropic_messages(headers, body),
            media_type="text/event-stream",
        )

    result = await anthropic_messages(headers, body)
    if isinstance(result, tuple):
        return JSONResponse(result[0], status_code=result[1])
    return JSONResponse(result)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "llm-router"}
