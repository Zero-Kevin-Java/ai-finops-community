ALTER TABLE afo_llm_model_price_tier ADD COLUMN IF NOT EXISTS usage_basis varchar(30) DEFAULT 'total_tokens'::varchar;

COMMENT ON COLUMN afo_llm_model_price_tier.usage_basis IS '阶梯匹配用量口径：total_tokens 总用量，prompt_tokens 输入用量，completion_tokens 输出用量';
