-- LLM 额度重置日期：weekly 使用 1-7，monthly 使用 1-28，daily 为空。

ALTER TABLE "public"."afo_llm_quota_account"
  ADD COLUMN IF NOT EXISTS "reset_day" int4;

COMMENT ON COLUMN "public"."afo_llm_quota_account"."reset_day" IS '重置日期：weekly 为 1-7，monthly 为 1-28，daily 为空';
