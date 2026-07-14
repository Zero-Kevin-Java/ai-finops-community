import json
import logging
import os
import time
import uuid
from typing import AsyncGenerator, Union

from litellm import (
    APIConnectionError,
    APIError,
    AuthenticationError,
    BadRequestError,
    InternalServerError,
    RateLimitError,
    ServiceUnavailableError,
    Timeout,
    acompletion,
)
from litellm.types.utils import ModelResponse

logger = logging.getLogger(__name__)

# 流式请求超时（秒），大模型单chunk间隔可能>30s，需足够长
STREAM_TIMEOUT = int(os.getenv("LLM_ROUTER_STREAM_TIMEOUT", "120"))


def build_kwargs(headers: dict, body: dict) -> dict:
    """从 header + body 组装 litellm.acompletion() 参数"""
    kwargs = {
        "model": headers.get("x-litellm-model") or body.get("model"),
        "messages": body.get("messages", []),
        "stream": body.get("stream", False),
    }

    api_key = headers.get("x-api-key")
    if api_key:
        kwargs["api_key"] = api_key

    api_base = headers.get("x-api-base")
    if api_base:
        kwargs["api_base"] = api_base

    api_version = headers.get("x-api-version")
    if api_version:
        kwargs["api_version"] = api_version

    EXCLUDED = {"model", "messages", "stream", "api_key", "api_base", "api_version"}
    for key, value in body.items():
        if key not in EXCLUDED:
            kwargs[key] = value

    return kwargs


def _build_error(code: str, msg: str, exc: Exception) -> dict:
    """构建统一错误响应"""
    logger.error(f"Upstream error code={code}: {exc}")
    return {
        "error": {
            "message": msg,
            "type": "api_error",
            "code": code,
        }
    }


def _error_status_code(code: str) -> int:
    """错误码 → HTTP 状态码"""
    return {
        "upstream_auth_failed": 401,
        "upstream_rate_limited": 429,
        "upstream_bad_request": 400,
        "upstream_timeout": 504,
        "upstream_connection_closed": 502,
        "upstream_server_error": 500,
        "upstream_unavailable": 503,
        "upstream_unknown_error": 500,
    }.get(code, 500)


def _map_exception(e: Exception) -> tuple:
    """异常类型 → (error_code, message_prefix)"""
    if isinstance(e, AuthenticationError):
        return ("upstream_auth_failed", f"Authentication failed: {e}")
    if isinstance(e, RateLimitError):
        return ("upstream_rate_limited", f"Rate limit exceeded: {e}")
    if isinstance(e, BadRequestError):
        return ("upstream_bad_request", f"Bad request: {e}")
    if isinstance(e, Timeout):
        return ("upstream_timeout", f"Upstream timeout: {e}")
    if isinstance(e, APIConnectionError):
        return ("upstream_connection_closed", f"Upstream connection error: {e}")
    if isinstance(e, InternalServerError):
        return ("upstream_server_error", f"Upstream server error: {e}")
    if isinstance(e, ServiceUnavailableError):
        return ("upstream_unavailable", f"Upstream service unavailable: {e}")
    if isinstance(e, APIError):
        return ("upstream_connection_closed", f"Upstream stream interrupted: {e}")
    return ("upstream_unknown_error", f"Unexpected error: {e}")


async def chat_completions(
    headers: dict, body: dict
) -> Union[dict, tuple]:
    """非流式转发。正常返回 dict；出错返回 (error_dict, status_code) 以便调用方设置 HTTP 状态码。"""
    kwargs = build_kwargs(headers, body)
    kwargs.setdefault("timeout", STREAM_TIMEOUT)

    try:
        response: ModelResponse = await acompletion(**kwargs)
    except Exception as e:
        code, msg = _map_exception(e)
        return _build_error(code, msg, e), _error_status_code(code)

    return response.model_dump(exclude_none=True, mode="json")


async def stream_chat_completions(headers: dict, body: dict) -> AsyncGenerator[str, None]:
    """流式转发（SSE）"""
    kwargs = build_kwargs(headers, body)
    kwargs["stream"] = True
    kwargs.setdefault("timeout", STREAM_TIMEOUT)
    kwargs.setdefault("stream_options", {"include_usage": True})

    try:
        response = await acompletion(**kwargs)
        async for chunk in response:
            data = chunk.model_dump(exclude_none=True, mode="json")
            yield f"data: {json.dumps(data)}\n\n"
        yield "data: [DONE]\n\n"
    except Exception as e:
        code, msg = _map_exception(e)
        error_data = json.dumps({
            "error": {
                "message": msg,
                "type": "stream_error",
                "code": code,
            }
        })
        yield f"data: {error_data}\n\n"
        yield "data: [DONE]\n\n"


def build_anthropic_kwargs(headers: dict, body: dict) -> dict:
    model = headers.get("x-litellm-model") or body.get("model")
    api_key = headers.get("x-api-key")
    api_base = headers.get("x-api-base")
    api_version = headers.get("x-api-version")

    messages = []

    system = body.get("system")
    if system:
        if isinstance(system, str):
            messages.append({"role": "system", "content": system})
        elif isinstance(system, list):
            text_parts = []
            for block in system:
                if isinstance(block, dict) and block.get("type") == "text":
                    text_parts.append(block.get("text", ""))
            if text_parts:
                messages.append({"role": "system", "content": " ".join(text_parts)})

    for msg in body.get("messages", []):
        messages.append(msg)

    kwargs = {
        "model": model,
        "messages": messages,
        "stream": body.get("stream", False),
    }

    if api_key:
        kwargs["api_key"] = api_key
    if api_base:
        kwargs["api_base"] = api_base
    if api_version:
        kwargs["api_version"] = api_version

    EXCLUDED = {"model", "messages", "stream", "system", "api_key", "api_base", "api_version"}
    for key, value in body.items():
        if key in EXCLUDED:
            continue
        if key == "stop_sequences":
            kwargs["stop"] = value
        else:
            kwargs[key] = value

    return kwargs


def _openai_to_anthropic_response(openai_resp: dict, original_model: str) -> dict:
    choice = openai_resp.get("choices", [{}])[0]
    message = choice.get("message", {})
    finish_reason = choice.get("finish_reason", "stop")

    stop_reason_map = {
        "stop": "end_turn",
        "length": "max_tokens",
        "tool_calls": "tool_use",
        "content_filter": "end_turn",
    }
    stop_reason = stop_reason_map.get(finish_reason, "end_turn")

    usage = openai_resp.get("usage", {})
    input_tokens = usage.get("prompt_tokens", 0)
    output_tokens = usage.get("completion_tokens", 0)

    resp_id = openai_resp.get("id", f"msg_{uuid.uuid4().hex[:24]}")

    content_blocks = _convert_content_blocks(message)

    return {
        "id": resp_id,
        "type": "message",
        "role": "assistant",
        "model": original_model or openai_resp.get("model", ""),
        "content": content_blocks,
        "stop_reason": stop_reason,
        "stop_sequence": None,
        "usage": {
            "input_tokens": input_tokens,
            "output_tokens": output_tokens,
        },
    }


def _convert_content_blocks(message: dict) -> list:
    blocks = []
    thinking_blocks = message.get("thinking_blocks") or []
    for tb in thinking_blocks:
        if tb.get("type") == "thinking":
            block = {
                "type": "thinking",
                "thinking": tb.get("thinking", ""),
            }
            signature = tb.get("signature")
            if signature:
                block["signature"] = signature
            blocks.append(block)
        elif tb.get("type") == "redacted_thinking":
            blocks.append({
                "type": "redacted_thinking",
                "data": tb.get("data", ""),
            })
    if not thinking_blocks:
        reasoning = message.get("reasoning_content") or ""
        if reasoning:
            blocks.append({"type": "thinking", "thinking": reasoning})
    content_text = message.get("content", "") or ""
    if content_text:
        blocks.append({"type": "text", "text": content_text})
    tool_calls = message.get("tool_calls") or []
    for tc in tool_calls:
        fn = tc.get("function", {})
        input_data = _safe_parse_arguments(fn.get("arguments", ""))
        blocks.append({
            "type": "tool_use",
            "id": tc.get("id", ""),
            "name": fn.get("name", ""),
            "input": input_data,
        })
    if not blocks:
        blocks = [{"type": "text", "text": ""}]
    return blocks


def _safe_parse_arguments(arguments: str) -> dict:
    if not arguments:
        return {}
    try:
        return json.loads(arguments)
    except (json.JSONDecodeError, TypeError):
        return {"_raw": arguments}


async def anthropic_messages(
    headers: dict, body: dict
) -> Union[dict, tuple]:
    kwargs = build_anthropic_kwargs(headers, body)
    kwargs.setdefault("timeout", STREAM_TIMEOUT)

    original_model = body.get("model", "")

    try:
        response: ModelResponse = await acompletion(**kwargs)
    except Exception as e:
        code, msg = _map_exception(e)
        error_resp = {
            "type": "error",
            "error": {
                "type": "api_error",
                "message": msg,
            }
        }
        return error_resp, _error_status_code(code)

    openai_resp = response.model_dump(exclude_none=True, mode="json")
    return _openai_to_anthropic_response(openai_resp, original_model)


async def stream_anthropic_messages(
    headers: dict, body: dict
) -> AsyncGenerator[str, None]:
    kwargs = build_anthropic_kwargs(headers, body)
    kwargs["stream"] = True
    kwargs.setdefault("timeout", STREAM_TIMEOUT)
    kwargs.setdefault("stream_options", {"include_usage": True})

    original_model = body.get("model", "")
    msg_id = f"msg_{uuid.uuid4().hex[:24]}"
    content_index = 0
    output_tokens = 0
    finish_reason = "end_turn"

    active_block_type = None
    opened_tool_ids = set()
    input_tokens = 0

    try:
        response = await acompletion(**kwargs)

        yield (
            f"event: message_start\n"
            f"data: {json.dumps({'type': 'message_start', 'message': {'id': msg_id, 'type': 'message', 'role': 'assistant', 'model': original_model, 'content': [], 'usage': {'input_tokens': 0}}})}\n\n"
        )

        async for chunk in response:
            if chunk.choices:
                delta = chunk.choices[0].delta
                fn = chunk.choices[0].finish_reason

                reasoning = ""
                signature = ""
                thinking_blocks = getattr(delta, "thinking_blocks", None) or []
                for tb in thinking_blocks:
                    if isinstance(tb, dict):
                        reasoning += tb.get("thinking", "") or ""
                        if not signature:
                            signature = tb.get("signature", "") or ""
                if not reasoning:
                    reasoning = getattr(delta, "reasoning_content", None) or ""
                content = delta.content or ""
                tool_calls = getattr(delta, "tool_calls", None) or []

                if reasoning:
                    if active_block_type != "thinking":
                        if active_block_type:
                            yield _content_block_stop(content_index)
                            content_index += 1
                        thinking_start = {"thinking": reasoning}
                        if signature:
                            thinking_start["signature"] = signature
                        yield _content_block_start(
                            content_index, "thinking",
                            thinking_start,
                        )
                        active_block_type = "thinking"
                    else:
                        yield _content_block_delta(
                            content_index, "thinking_delta",
                            {"thinking": reasoning},
                        )

                if content:
                    if active_block_type != "text":
                        if active_block_type:
                            yield _content_block_stop(content_index)
                            content_index += 1
                        yield _content_block_start(
                            content_index, "text", {"text": ""},
                        )
                        active_block_type = "text"
                    yield _content_block_delta(
                        content_index, "text_delta",
                        {"text": content},
                    )

                for tc in tool_calls:
                    tc_id = getattr(tc, "id", None)
                    tc_fn = getattr(tc, "function", None)
                    tc_name = getattr(tc_fn, "name", None) if tc_fn else None
                    tc_args = getattr(tc_fn, "arguments", "") if tc_fn else ""

                    if tc_id and tc_id not in opened_tool_ids:
                        if active_block_type:
                            yield _content_block_stop(content_index)
                            content_index += 1
                        yield (
                            f"event: content_block_start\n"
                            f"data: {json.dumps({'type': 'content_block_start', 'index': content_index, 'content_block': {'type': 'tool_use', 'id': tc_id, 'name': tc_name, 'input': {}}})}\n\n"
                        )
                        active_block_type = "tool_use"
                        opened_tool_ids.add(tc_id)
                    if tc_args:
                        yield (
                            f"event: content_block_delta\n"
                            f"data: {json.dumps({'type': 'content_block_delta', 'index': content_index, 'delta': {'type': 'input_json_delta', 'partial_json': tc_args}})}\n\n"
                        )

                if fn:
                    finish_reason_map = {
                        "stop": "end_turn",
                        "length": "max_tokens",
                        "tool_calls": "tool_use",
                    }
                    finish_reason = finish_reason_map.get(fn, "end_turn")
            elif chunk.usage:
                output_tokens = chunk.usage.completion_tokens or 0
                input_tokens = getattr(chunk.usage, "prompt_tokens", 0) or 0

        if active_block_type:
            yield _content_block_stop(content_index)
            content_index += 1

        yield (
            f"event: message_delta\n"
            f"data: {json.dumps({'type': 'message_delta', 'delta': {'stop_reason': finish_reason, 'stop_sequence': None}, 'usage': {'input_tokens': input_tokens, 'output_tokens': output_tokens}})}\n\n"
        )

        yield (
            f"event: message_stop\n"
            f"data: {json.dumps({'type': 'message_stop'})}\n\n"
        )

    except Exception as e:
        code, msg_text = _map_exception(e)
        error_data = json.dumps({
            "type": "error",
            "error": {
                "type": "api_error",
                "message": msg_text,
            }
        })
        yield f"event: error\ndata: {error_data}\n\n"


def _content_block_start(index, block_type, extra):
    return (
        f"event: content_block_start\n"
        f"data: {json.dumps({'type': 'content_block_start', 'index': index, 'content_block': {'type': block_type, **extra}})}\n\n"
    )


def _content_block_delta(index, delta_type, delta_extra):
    return (
        f"event: content_block_delta\n"
        f"data: {json.dumps({'type': 'content_block_delta', 'index': index, 'delta': {'type': delta_type, **delta_extra}})}\n\n"
    )


def _content_block_stop(index):
    return (
        f"event: content_block_stop\n"
        f"data: {json.dumps({'type': 'content_block_stop', 'index': index})}\n\n"
    )
