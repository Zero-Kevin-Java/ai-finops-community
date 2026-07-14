CREATE TABLE IF NOT EXISTS "public"."afo_llm_bill_compare" (
  "bill_id" int8 NOT NULL,
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default" DEFAULT '000000'::character varying,
  "bill_name" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "provider" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "supplier" varchar(100) COLLATE "pg_catalog"."default",
  "file_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "bill_period" varchar(50) COLLATE "pg_catalog"."default",
  "currency" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "parse_status" varchar(30) COLLATE "pg_catalog"."default" DEFAULT 'success'::character varying,
  "parsed_rows" int8 DEFAULT 0,
  "provider_total_tokens" int8 DEFAULT 0,
  "yuan_key_total_tokens" int8 DEFAULT 0,
  "provider_amount" numeric(20,6) DEFAULT 0,
  "yuan_key_amount" numeric(20,6) DEFAULT 0,
  "theoretical_amount" numeric(20,6),
  "theoretical_diff_amount" numeric(20,6),
  "imported_at" timestamp(6) NOT NULL,
  "create_dept" int8,
  "create_by" int8,
  "create_time" timestamp(6),
  "update_by" int8,
  "update_time" timestamp(6),
  "del_flag" char(1) COLLATE "pg_catalog"."default" DEFAULT '0'::bpchar,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  CONSTRAINT "pk_afo_llm_bill_compare" PRIMARY KEY ("bill_id")
);

COMMENT ON TABLE "public"."afo_llm_bill_compare" IS 'LLM 账单对比记录表';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."bill_id" IS '账单对比ID';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."tenant_id" IS '系统租户编号';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."bill_name" IS '账单名称';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."provider" IS '厂商';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."supplier" IS '供应商';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."file_name" IS '原始文件名';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."bill_period" IS '账单周期';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."currency" IS '币种';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."parse_status" IS '解析状态';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."parsed_rows" IS '解析行数';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."provider_total_tokens" IS '厂商总 Token 数';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."yuan_key_total_tokens" IS '元钥总 Token 数';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."provider_amount" IS '厂商账单金额';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."yuan_key_amount" IS '元钥账单金额';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."theoretical_amount" IS '理论账单金额，当前仅保留字段';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."theoretical_diff_amount" IS '理论差异金额，当前仅保留字段';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."imported_at" IS '导入时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."create_dept" IS '创建部门';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."create_by" IS '创建者';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."update_by" IS '更新者';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."del_flag" IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN "public"."afo_llm_bill_compare"."remark" IS '备注';

CREATE INDEX IF NOT EXISTS "idx_afo_llm_bill_compare_imported_at" ON "public"."afo_llm_bill_compare" USING btree (
  "tenant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "imported_at" "pg_catalog"."timestamp_ops" DESC NULLS LAST
) WHERE del_flag = '0'::bpchar;

CREATE INDEX IF NOT EXISTS "idx_afo_llm_bill_compare_filter" ON "public"."afo_llm_bill_compare" USING btree (
  "tenant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "provider" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "supplier" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "currency" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE del_flag = '0'::bpchar;
