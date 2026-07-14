-- 模型准入拒绝日志扩展

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS policy_id BIGINT;

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS deny_layer VARCHAR(64);

COMMENT ON COLUMN afo_route_decision_log.policy_id IS '拒绝策略 ID';
COMMENT ON COLUMN afo_route_decision_log.deny_layer IS '拒绝层级，如 ENTERPRISE_MODEL_ACCESS';

CREATE INDEX IF NOT EXISTS idx_route_log_deny_layer
    ON afo_route_decision_log (tenant_id, deny_layer, create_time DESC);
