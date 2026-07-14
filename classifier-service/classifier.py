"""分类核心逻辑：调用远程 LLM 判断 Prompt 是否为简单任务。

单一职责：接收文本 → 调用 LLM → 返回结构化分类结果。
所有异常出口都返回 is_simple=false，确保网关不因分类器报错而阻断主流量。
"""
import json
import re
from openai import OpenAI, APITimeoutError, APIError

from config import Settings
from schemas import ClassifyResponse


class Classifier:
    def __init__(self, settings: Settings):
        self.settings = settings
        # 使用 OpenAI 兼容 SDK，base_url 可指向任何 OpenAI 兼容 API（LiteLLM / 阿里云 DashScope / 智谱等）
        self.client = OpenAI(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            timeout=settings.openai_timeout,
        )

    def classify(self, prompt: str) -> ClassifyResponse:
        """对 prompt 做简单/复杂二分类。返回结果与 Java ClassifyResponse @JsonProperty 字段对齐。"""
        # 空 prompt 直接降级，避免空字符串送到 LLM 产生无意义调用
        if not prompt or not prompt.strip():
            return ClassifyResponse(is_simple=False, confidence=0.0, model_used="none")

        # 防御截断：512 字符是产品文档定义的推理层输入上限（见 plans/product.md 五层纵深防御 L2）
        truncated = prompt[:512]

        # 用预置模板引导 LLM 输出结构化 JSON，而非自然语言
        user_prompt = self.settings.classify_prompt_template.format(prompt=truncated)

        try:
            # temperature=0.0 保证同一 prompt 多次调用的分类结果稳定；
            # max_tokens=128 限制回复长度，LLM 只需要输出 {"is_simple":...,"confidence":...}
            resp = self.client.chat.completions.create(
                model=self.settings.openai_model,
                messages=[{"role": "user", "content": user_prompt}],
                temperature=0.0,
                max_tokens=128,
            )

            content = resp.choices[0].message.content.strip()
            model_used = resp.model if resp.model else self.settings.openai_model

            parsed = self._parse_response(content)
            if parsed:
                return ClassifyResponse(
                    is_simple=parsed.get("is_simple", False),
                    confidence=parsed.get("confidence", 0.0),
                    model_used=model_used,
                )

            # LLM 响应格式不符合预期时降级，不抛异常
            return ClassifyResponse(is_simple=False, confidence=0.0, model_used=model_used)

        except APITimeoutError:
            # 超时降级：网关也有 5s 超时，服务层 10s 超时应晚于网关触发
            return ClassifyResponse(is_simple=False, confidence=0.0, model_used="timeout")
        except APIError as e:
            return ClassifyResponse(is_simple=False, confidence=0.0, model_used="error")
        except Exception as e:
            # 兜底捕获（如 JSONDecodeError、网络断开等），确保从不抛异常到路由层
            return ClassifyResponse(is_simple=False, confidence=0.0, model_used="error")

    @staticmethod
    def _parse_response(content: str) -> dict | None:
        """从 LLM 响应中解析 JSON。

        不同模型输出风格不同：
        - GPT-4o/mini 倾向于输出纯 JSON
        - 其他模型可能用 markdown 代码块包裹（```json ... ```）
        此方法同时兼容两种格式，避免因 LLM 输出风格差异导致解析失败。
        """
        # 移除 markdown 代码块标记
        if "```" in content:
            match = re.search(r"```(?:json)?\s*([\s\S]*?)```", content)
            if match:
                content = match.group(1).strip()

        # 提取第一个 JSON 对象
        match = re.search(r"\{[\s\S]*\}", content)
        if match:
            try:
                return json.loads(match.group())
            except json.JSONDecodeError:
                return None
        return None
