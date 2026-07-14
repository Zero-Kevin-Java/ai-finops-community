-- Day5b: 模型目录扩展字段 — 补齐所有 Entity 期望的列（部分建表时即遗漏）

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS context_window int4;
COMMENT ON COLUMN afo_llm_model_catalog.context_window IS '上下文窗口 token 数';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS supports_stream char DEFAULT '1';
COMMENT ON COLUMN afo_llm_model_catalog.supports_stream IS '是否支持流式输出（0否 1是）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS supports_tool char DEFAULT '0';
COMMENT ON COLUMN afo_llm_model_catalog.supports_tool IS '是否支持工具调用（0否 1是）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS status char DEFAULT '0';
COMMENT ON COLUMN afo_llm_model_catalog.status IS '状态（0正常 1停用）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS protocol varchar(20) NOT NULL DEFAULT 'openai';
COMMENT ON COLUMN afo_llm_model_catalog.protocol IS '协议类型: openai / anthropic';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS api_key text;
COMMENT ON COLUMN afo_llm_model_catalog.api_key IS 'API Key（AES-256/GCM 加密存储）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS api_base varchar(500);
COMMENT ON COLUMN afo_llm_model_catalog.api_base IS '上游 API 端点地址';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS supports_vision char DEFAULT '0';
COMMENT ON COLUMN afo_llm_model_catalog.supports_vision IS '是否支持视觉（0否 1是）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS supports_parallel_function_calling char DEFAULT '0';
COMMENT ON COLUMN afo_llm_model_catalog.supports_parallel_function_calling IS '是否支持并行函数调用（0否 1是）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS supports_reasoning char DEFAULT '0';
COMMENT ON COLUMN afo_llm_model_catalog.supports_reasoning IS '是否支持推理（0否 1是）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS max_input_tokens int4;
COMMENT ON COLUMN afo_llm_model_catalog.max_input_tokens IS '最大输入 token 数';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS max_output_tokens int4;
COMMENT ON COLUMN afo_llm_model_catalog.max_output_tokens IS '最大输出 token 数';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS api_version varchar(50);
COMMENT ON COLUMN afo_llm_model_catalog.api_version IS 'API 版本';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS rate_limit_tpm int4;
COMMENT ON COLUMN afo_llm_model_catalog.rate_limit_tpm IS 'TPM 限制';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS rate_limit_rpm int4;
COMMENT ON COLUMN afo_llm_model_catalog.rate_limit_rpm IS 'RPM 限制';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS request_timeout int4;
COMMENT ON COLUMN afo_llm_model_catalog.request_timeout IS '请求超时（秒）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS model_tags text;
COMMENT ON COLUMN afo_llm_model_catalog.model_tags IS '模型标签（JSON 数组，如 ["vision","fast"]）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS routing_order int4;
COMMENT ON COLUMN afo_llm_model_catalog.routing_order IS '路由优先级（越低越优先，L2 使用）';
