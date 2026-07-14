CREATE TABLE IF NOT EXISTS "public"."afo_llm_bill_compare_item" (
  "item_id" int8 NOT NULL,
  "bill_id" int8 NOT NULL,
  "tenant_id" varchar(20) COLLATE "pg_catalog"."default" DEFAULT '000000'::character varying,
  "provider_template" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "supplier" varchar(100) COLLATE "pg_catalog"."default",
  "source_file_name" varchar(255) COLLATE "pg_catalog"."default",
  "source_row_no" int4,
  "vendor_account" varchar(120) COLLATE "pg_catalog"."default",
  "product_name" varchar(120) COLLATE "pg_catalog"."default",
  "vendor_model_name" varchar(200) COLLATE "pg_catalog"."default",
  "model_code" varchar(100) COLLATE "pg_catalog"."default",
  "usage_start_time" timestamp(6),
  "usage_end_time" timestamp(6),
  "bill_period" varchar(50) COLLATE "pg_catalog"."default",
  "usage_type" varchar(100) COLLATE "pg_catalog"."default",
  "pricing_mode" varchar(50) COLLATE "pg_catalog"."default",
  "billing_quantity" numeric(24,6),
  "billing_unit" varchar(50) COLLATE "pg_catalog"."default",
  "token_count" int8 DEFAULT 0,
  "currency" varchar(10) COLLATE "pg_catalog"."default",
  "unit_price" numeric(24,8),
  "original_amount" numeric(20,6),
  "discount_amount" numeric(20,6),
  "payable_amount" numeric(20,6),
  "parse_status" varchar(30) COLLATE "pg_catalog"."default" DEFAULT 'success'::character varying,
  "parse_message" varchar(500) COLLATE "pg_catalog"."default",
  "raw_payload" text COLLATE "pg_catalog"."default",
  "create_dept" int8,
  "create_by" int8,
  "create_time" timestamp(6),
  "update_by" int8,
  "update_time" timestamp(6),
  "del_flag" char(1) COLLATE "pg_catalog"."default" DEFAULT '0'::bpchar,
  CONSTRAINT "pk_afo_llm_bill_compare_item" PRIMARY KEY ("item_id")
);

COMMENT ON TABLE "public"."afo_llm_bill_compare_item" IS 'LLM 厂商账单对比明细表';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."item_id" IS '明细ID';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."bill_id" IS '账单对比ID';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."tenant_id" IS '系统租户编号';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."provider_template" IS '厂商账单模板';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."supplier" IS '供应商';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."source_file_name" IS '原始文件名';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."source_row_no" IS '原始行号';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."vendor_account" IS '厂商账号';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."product_name" IS '产品或接口名称';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."vendor_model_name" IS '厂商侧模型名称';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."model_code" IS '系统模型编码';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."usage_start_time" IS '用量开始时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."usage_end_time" IS '用量结束时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."bill_period" IS '账单周期';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."usage_type" IS '用量类型';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."pricing_mode" IS '计费模式';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."billing_quantity" IS '计费数量';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."billing_unit" IS '计费单位';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."token_count" IS 'Token 数';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."currency" IS '币种';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."unit_price" IS '单价';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."original_amount" IS '原始金额';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."discount_amount" IS '优惠金额';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."payable_amount" IS '应付金额';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."parse_status" IS '解析状态';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."parse_message" IS '解析信息';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."raw_payload" IS '原始行内容 JSON';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."create_dept" IS '创建部门';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."create_by" IS '创建者';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."update_by" IS '更新者';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."afo_llm_bill_compare_item"."del_flag" IS '删除标志（0代表存在 1代表删除）';

CREATE INDEX IF NOT EXISTS "idx_afo_llm_bill_compare_item_bill" ON "public"."afo_llm_bill_compare_item" USING btree (
  "bill_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE del_flag = '0'::bpchar;

CREATE INDEX IF NOT EXISTS "idx_afo_llm_bill_compare_item_match" ON "public"."afo_llm_bill_compare_item" USING btree (
  "tenant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "supplier" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "model_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "currency" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "usage_start_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE del_flag = '0'::bpchar;

CREATE INDEX IF NOT EXISTS "idx_afo_llm_bill_compare_item_template" ON "public"."afo_llm_bill_compare_item" USING btree (
  "tenant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "provider_template" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "bill_period" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE del_flag = '0'::bpchar;
