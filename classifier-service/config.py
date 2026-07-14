"""环境变量配置（pydantic-settings）。

所有配置从 .env 文件读取，默认值与网关 application.yml 中的 classifier 配置段对齐。
新增配置项只需在此类中声明字段，pydantic-settings 自动从环境变量注入。
"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    host: str = "0.0.0.0"
    port: int = 8000

    # OpenAI 兼容 API 配置（可指向 LiteLLM Proxy / 阿里云 DashScope / 任意 OpenAI 兼容服务）
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"  # 分类本身用便宜模型，避免分类成本倒挂
    openai_timeout: int = 10  # 应大于网关超时（5s），否则网关先超时触发降级

    # {prompt} 占位符由 classifier.py 中 format 替换
    # {{ 和 }} 是 Python str.format() 中转义写法，运行时输出为单花括号
    classify_prompt_template: str = (
        "You are a task classifier. Determine if the following user request is a "
        "'simple task' or 'complex task'.\n\n"
        "Simple tasks are: basic Q&A, math calculations, translations, "
        "summarization of short text, simple code snippets, definitions.\n\n"
        "Complex tasks are: multi-step reasoning, creative writing, "
        "long document analysis, complex code generation, strategic planning.\n\n"
        "Respond with a JSON object ONLY (no markdown, no code block):\n"
        '{{"is_simple": true/false, "confidence": 0.0~1.0}}\n\n'
        "User request: {prompt}"
    )

    # pydantic-settings v2 配置：自动加载 .env 文件
    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}
