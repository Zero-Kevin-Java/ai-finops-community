-- LLM 额度批量分配：扩展额度账户对象维度、额度类型、执行动作、预警配置。

ALTER TABLE "public"."afo_llm_quota_account"
  ADD COLUMN IF NOT EXISTS "object_id" int8,
  ADD COLUMN IF NOT EXISTS "quota_unit" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'amount',
  ADD COLUMN IF NOT EXISTS "effective_time" timestamp(6),
  ADD COLUMN IF NOT EXISTS "reset_clock" time(6),
  ADD COLUMN IF NOT EXISTS "reset_day" int4,
  ADD COLUMN IF NOT EXISTS "limit_action" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'continue',
  ADD COLUMN IF NOT EXISTS "warning_enabled" char(1) COLLATE "pg_catalog"."default" DEFAULT '1',
  ADD COLUMN IF NOT EXISTS "warning_threshold" numeric(5,2) DEFAULT 85.00;

UPDATE "public"."afo_llm_quota_account"
SET
  "object_id" = CASE
    WHEN "object_id" IS NOT NULL THEN "object_id"
    WHEN "account_type" = 'project' THEN "project_id"
    WHEN "account_type" = 'app' THEN "client_id"
    ELSE "object_id"
  END,
  "quota_unit" = CASE
    WHEN "currency" = 'TOKEN' THEN 'token'
    ELSE COALESCE("quota_unit", 'amount')
  END,
  "limit_action" = COALESCE("limit_action", 'continue'),
  "warning_enabled" = COALESCE("warning_enabled", '1'),
  "warning_threshold" = COALESCE("warning_threshold", 85.00);

COMMENT ON COLUMN "public"."afo_llm_quota_account"."object_id" IS '额度对象ID：项目ID/应用ID/部门ID/用户ID/API Key ID';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."quota_unit" IS '额度类型（amount/token）';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."effective_time" IS '额度生效时间';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."reset_clock" IS '重置触发时间点';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."reset_day" IS '重置日期：weekly 为 1-7，monthly 为 1-28，daily 为空';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."limit_action" IS '额度耗尽后的执行动作（continue/block）';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."warning_enabled" IS '是否启用预警（0否 1是）';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."warning_threshold" IS '预警阈值百分比';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."account_type" IS '账户类型（tenant/project/app/department/user/api_key）';
COMMENT ON COLUMN "public"."afo_llm_quota_account"."reset_cycle" IS '重置周期（none/daily/weekly/monthly/yearly）';

DROP INDEX IF EXISTS "public"."uk_afo_llm_quota_account_scope";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_afo_llm_quota_account_scope" ON "public"."afo_llm_quota_account" USING btree (
  "tenant_id",
  "account_type",
  COALESCE("object_id", '-1'::bigint),
  "quota_unit",
  "currency"
) WHERE "del_flag" = '0'::bpchar;

CREATE INDEX IF NOT EXISTS "idx_afo_llm_quota_account_object"
ON "public"."afo_llm_quota_account" USING btree (
  "tenant_id", "account_type", "object_id", "quota_unit", "currency", "status", "del_flag"
);

INSERT INTO "public"."sys_dict_data"
("dict_code", "tenant_id", "dict_sort", "dict_label", "dict_value", "dict_type", "css_class", "list_class", "is_default", "create_dept", "create_by", "create_time", "remark")
VALUES
(20093, '000000', 4, 'dict.llm_quota_account_type.department', 'department', 'llm_quota_account_type', '', 'info', 'N', 103, 1, now(), '部门级额度账户'),
(20094, '000000', 5, 'dict.llm_quota_account_type.user', 'user', 'llm_quota_account_type', '', 'info', 'N', 103, 1, now(), '人员级额度账户'),
(20095, '000000', 6, 'dict.llm_quota_account_type.api_key', 'api_key', 'llm_quota_account_type', '', 'warning', 'N', 103, 1, now(), 'API Key 级额度账户'),
(20104, '000000', 5, 'dict.llm_quota_reset_cycle.weekly', 'weekly', 'llm_quota_reset_cycle', '', 'info', 'N', 103, 1, now(), '每周重置')
ON CONFLICT ("dict_code") DO NOTHING;
