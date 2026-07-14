-- 路由配置决策日志扩展字段

ALTER TABLE afo_route_decision_log
    ADD COLUMN IF NOT EXISTS rule_id BIGINT,
    ADD COLUMN IF NOT EXISTS rule_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS source_model VARCHAR(200),
    ADD COLUMN IF NOT EXISTS action_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS match_summary TEXT,
    ADD COLUMN IF NOT EXISTS fallback_applied BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fallback_model VARCHAR(200),
    ADD COLUMN IF NOT EXISTS fallback_reason VARCHAR(64),
    ADD COLUMN IF NOT EXISTS classification_result VARCHAR(64),
    ADD COLUMN IF NOT EXISTS classifier_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS team_tag VARCHAR(128),
    ADD COLUMN IF NOT EXISTS path VARCHAR(256);

CREATE INDEX IF NOT EXISTS idx_route_log_rule
    ON afo_route_decision_log (tenant_id, rule_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_route_log_action
    ON afo_route_decision_log (tenant_id, action_type, execution_mode, create_time DESC);

COMMENT ON COLUMN afo_route_decision_log.rule_id IS '命中的路由配置规则 ID';
COMMENT ON COLUMN afo_route_decision_log.rule_name IS '命中的路由配置规则名称';
COMMENT ON COLUMN afo_route_decision_log.source_model IS '请求原始模型';
COMMENT ON COLUMN afo_route_decision_log.action_type IS '路由动作：ORIGINAL_MODEL/TARGET_MODEL/MODEL_GROUP/CLASSIFIER/DENY';
COMMENT ON COLUMN afo_route_decision_log.execution_mode IS '执行模式：ENFORCE/RECORD_ONLY';
COMMENT ON COLUMN afo_route_decision_log.match_summary IS '路由配置匹配摘要，不记录完整 prompt';
COMMENT ON COLUMN afo_route_decision_log.fallback_applied IS '是否应用兜底模型';
COMMENT ON COLUMN afo_route_decision_log.fallback_model IS '实际选中的兜底模型';
COMMENT ON COLUMN afo_route_decision_log.fallback_reason IS '兜底原因';
COMMENT ON COLUMN afo_route_decision_log.classification_result IS '分类器结果';
COMMENT ON COLUMN afo_route_decision_log.classifier_confidence IS '分类器置信度';
COMMENT ON COLUMN afo_route_decision_log.team_tag IS '请求团队标签';
COMMENT ON COLUMN afo_route_decision_log.path IS '请求路径';

CREATE OR REPLACE FUNCTION afo_touch_routing_config_rule_hit()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.rule_id IS NOT NULL AND to_regclass('public.afo_routing_config_rule') IS NOT NULL THEN
        UPDATE afo_routing_config_rule
        SET hit_count = COALESCE(hit_count, 0) + 1,
            last_hit_time = COALESCE(NEW.create_time, NOW()),
            update_time = NOW()
        WHERE rule_id = NEW.rule_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_route_decision_log_rule_hit ON afo_route_decision_log;

CREATE TRIGGER trg_route_decision_log_rule_hit
AFTER INSERT ON afo_route_decision_log
FOR EACH ROW
EXECUTE FUNCTION afo_touch_routing_config_rule_hit();
