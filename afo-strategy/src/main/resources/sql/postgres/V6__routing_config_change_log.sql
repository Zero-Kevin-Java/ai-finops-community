-- 路由配置修改日志表。

CREATE TABLE IF NOT EXISTS afo_routing_config_change_log (
    log_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) COLLATE "pg_catalog"."default" DEFAULT '000000'::character varying,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(100),
    operator_name VARCHAR(100),
    operation_type VARCHAR(32) NOT NULL,
    change_content TEXT,
    before_content TEXT,
    after_content TEXT,
    change_reason VARCHAR(500),
    effective_start TIMESTAMP(6) DEFAULT NULL,
    effective_end TIMESTAMP(6) DEFAULT NULL,
    runtime_start TIMESTAMP(6) DEFAULT NULL,
    runtime_end TIMESTAMP(6) DEFAULT NULL,
    create_dept INT8,
    create_by INT8,
    create_time TIMESTAMP(6),
    update_by INT8,
    update_time TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_afo_routing_config_change_log_rule
    ON afo_routing_config_change_log (tenant_id, rule_id, update_time DESC);

CREATE INDEX IF NOT EXISTS idx_afo_routing_config_change_log_type
    ON afo_routing_config_change_log (tenant_id, operation_type, update_time DESC);

COMMENT ON TABLE afo_routing_config_change_log IS '路由配置修改日志';
COMMENT ON COLUMN afo_routing_config_change_log.log_id IS '日志 ID';
COMMENT ON COLUMN afo_routing_config_change_log.tenant_id IS '租户 ID';
COMMENT ON COLUMN afo_routing_config_change_log.rule_id IS '规则 ID';
COMMENT ON COLUMN afo_routing_config_change_log.rule_name IS '规则名称';
COMMENT ON COLUMN afo_routing_config_change_log.operator_name IS '操作人名称';
COMMENT ON COLUMN afo_routing_config_change_log.operation_type IS '操作类型：CREATE/UPDATE/STATUS/DELETE/COPY';
COMMENT ON COLUMN afo_routing_config_change_log.change_content IS '修改内容，多项使用换行分隔';
COMMENT ON COLUMN afo_routing_config_change_log.before_content IS '修改前，多项使用换行分隔';
COMMENT ON COLUMN afo_routing_config_change_log.after_content IS '修改后，多项使用换行分隔';
COMMENT ON COLUMN afo_routing_config_change_log.change_reason IS '修改原因';
COMMENT ON COLUMN afo_routing_config_change_log.effective_start IS '生效开始时间';
COMMENT ON COLUMN afo_routing_config_change_log.effective_end IS '生效结束时间';
COMMENT ON COLUMN afo_routing_config_change_log.runtime_start IS '运行开始时间';
COMMENT ON COLUMN afo_routing_config_change_log.runtime_end IS '运行截止时间';
