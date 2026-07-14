-- 路由配置规则表。
-- 独立承载请求路由规则，不复用企业模型准入表 afo_model_access_policy。

CREATE TABLE IF NOT EXISTS afo_routing_config_rule (
    rule_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) COLLATE "pg_catalog"."default" DEFAULT '000000'::character varying,
    rule_name VARCHAR(100) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    match_config TEXT NOT NULL DEFAULT '{}',
    action_type VARCHAR(40) NOT NULL,
    action_config TEXT NOT NULL DEFAULT '{}',
    fallback_config TEXT NOT NULL DEFAULT '{}',
    execution_mode VARCHAR(30) NOT NULL DEFAULT 'ENFORCE',
    effective_start TIMESTAMP(6) DEFAULT NULL,
    effective_end TIMESTAMP(6) DEFAULT NULL,
    status CHAR(1) DEFAULT '0',
    hit_count BIGINT DEFAULT 0,
    last_hit_time TIMESTAMP(6) DEFAULT NULL,
    create_dept INT8,
    create_by INT8,
    create_time TIMESTAMP(6),
    update_by INT8,
    update_time TIMESTAMP(6),
    del_flag CHAR(1) DEFAULT '0',
    remark VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_afo_routing_config_rule_tenant_status
    ON afo_routing_config_rule (tenant_id, status, del_flag, priority);

CREATE INDEX IF NOT EXISTS idx_afo_routing_config_rule_effective
    ON afo_routing_config_rule (tenant_id, effective_start, effective_end);

COMMENT ON TABLE afo_routing_config_rule IS '路由配置规则';
COMMENT ON COLUMN afo_routing_config_rule.rule_id IS '规则 ID';
COMMENT ON COLUMN afo_routing_config_rule.tenant_id IS '租户 ID';
COMMENT ON COLUMN afo_routing_config_rule.rule_name IS '规则名称';
COMMENT ON COLUMN afo_routing_config_rule.priority IS '优先级，数字越小越先匹配';
COMMENT ON COLUMN afo_routing_config_rule.match_config IS '匹配条件 JSON：apiKeyIds、teamTags、paths、sourceModels、modelTypes、keywords、tools、headers';
COMMENT ON COLUMN afo_routing_config_rule.action_type IS '路由动作：ORIGINAL_MODEL、TARGET_MODEL、MODEL_GROUP、CLASSIFIER、DENY';
COMMENT ON COLUMN afo_routing_config_rule.action_config IS '路由动作配置 JSON';
COMMENT ON COLUMN afo_routing_config_rule.fallback_config IS '兜底策略 JSON';
COMMENT ON COLUMN afo_routing_config_rule.execution_mode IS '执行模式：ENFORCE 执行，RECORD_ONLY 只记录不改写';
COMMENT ON COLUMN afo_routing_config_rule.effective_start IS '生效开始时间，为空表示立即生效';
COMMENT ON COLUMN afo_routing_config_rule.effective_end IS '生效结束时间，为空表示长期有效';
COMMENT ON COLUMN afo_routing_config_rule.status IS '状态：0 启用，1 停用';
COMMENT ON COLUMN afo_routing_config_rule.hit_count IS '规则命中次数';
COMMENT ON COLUMN afo_routing_config_rule.last_hit_time IS '最近命中时间';
COMMENT ON COLUMN afo_routing_config_rule.create_dept IS '创建部门';
COMMENT ON COLUMN afo_routing_config_rule.create_by IS '创建者';
COMMENT ON COLUMN afo_routing_config_rule.create_time IS '创建时间';
COMMENT ON COLUMN afo_routing_config_rule.update_by IS '更新者';
COMMENT ON COLUMN afo_routing_config_rule.update_time IS '更新时间';
COMMENT ON COLUMN afo_routing_config_rule.del_flag IS '删除标志：0 存在，1 删除';
COMMENT ON COLUMN afo_routing_config_rule.remark IS '备注';

-- 菜单文案迁移：保留历史权限编码 gateway:whitelist:*，只切换业务口径和组件路径。
UPDATE sys_menu
SET menu_name = '路由配置',
    path = 'routing-config',
    component = 'gateway/routing-config/index',
    icon = 'route',
    remark = '路由配置规则'
WHERE menu_id = 2002;

UPDATE sys_menu
SET menu_name = '路由配置新增'
WHERE menu_id = 20021;

UPDATE sys_menu
SET menu_name = '路由配置删除'
WHERE menu_id = 20022;

UPDATE sys_menu
SET menu_name = '路由配置修改'
WHERE menu_id = 20023;
