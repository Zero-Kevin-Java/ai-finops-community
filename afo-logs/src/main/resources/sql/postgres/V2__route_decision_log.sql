-- 路由决策日志表归口到 afo-logs

CREATE TABLE IF NOT EXISTS afo_route_decision_log (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) DEFAULT '000000',
    request_id VARCHAR(64),
    api_key_id VARCHAR(64),
    model VARCHAR(200),
    target_model VARCHAR(200),
    route_reason VARCHAR(50),
    whitelist_hit BOOLEAN DEFAULT FALSE,
    decision_latency_ms BIGINT,
    deny_reason VARCHAR(500),
    record_only BOOLEAN DEFAULT FALSE,
    policy_id BIGINT,
    deny_layer VARCHAR(64),
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS record_only BOOLEAN DEFAULT FALSE;

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS policy_id BIGINT;

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS deny_layer VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_route_log_tenant
    ON afo_route_decision_log (tenant_id, del_flag);

CREATE INDEX IF NOT EXISTS idx_route_log_request
    ON afo_route_decision_log (request_id);

CREATE INDEX IF NOT EXISTS idx_route_log_time
    ON afo_route_decision_log (create_time DESC);

CREATE INDEX IF NOT EXISTS idx_route_log_deny_layer
    ON afo_route_decision_log (tenant_id, deny_layer, create_time DESC);

COMMENT ON TABLE afo_route_decision_log IS '路由决策日志';
COMMENT ON COLUMN afo_route_decision_log.route_reason IS '路由原因: DEFAULT/WHITELIST_HIT/DENIED/RULE_HIT/AI_DECISION';
COMMENT ON COLUMN afo_route_decision_log.whitelist_hit IS '是否命中白名单';
COMMENT ON COLUMN afo_route_decision_log.decision_latency_ms IS '决策延迟毫秒';
COMMENT ON COLUMN afo_route_decision_log.record_only IS '只记录不拦截标记：true=仅记录，false=按策略执行';
COMMENT ON COLUMN afo_route_decision_log.policy_id IS '拒绝策略 ID';
COMMENT ON COLUMN afo_route_decision_log.deny_layer IS '拒绝层级，如 ENTERPRISE_MODEL_ACCESS';
