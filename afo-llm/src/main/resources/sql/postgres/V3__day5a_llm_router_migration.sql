-- Day5a: 替换 LiteLLM Proxy 为 llm-router
-- 1. 删除不再需要的同步状态字段（DB 中可能不存在，使用 IF EXISTS 安全跳过）
ALTER TABLE afo_llm_model_catalog DROP COLUMN IF EXISTS sync_status;

-- 2. 新增模型配置字段
ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS protocol varchar(20) NOT NULL DEFAULT 'openai';
COMMENT ON COLUMN afo_llm_model_catalog.protocol IS '协议类型: openai / anthropic';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS api_key text;
COMMENT ON COLUMN afo_llm_model_catalog.api_key IS 'API Key（AES-256/GCM 加密存储）';

ALTER TABLE afo_llm_model_catalog ADD COLUMN IF NOT EXISTS api_base varchar(500);
COMMENT ON COLUMN afo_llm_model_catalog.api_base IS '上游 API 端点地址';
