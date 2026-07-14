-- 模型准入管理表结构
-- 执行方式：由数据库管理员在 PostgreSQL 中手动执行。

CREATE TABLE IF NOT EXISTS afo_model_access_policy (
    policy_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) COLLATE "pg_catalog"."default" DEFAULT '000000'::character varying,
    policy_name VARCHAR(100) NOT NULL,
    default_mode VARCHAR(30) NOT NULL DEFAULT 'ALLOW_UNLISTED',
    allowed_models TEXT DEFAULT '[]',
    denied_models TEXT DEFAULT '[]',
    effective_start TIMESTAMP(6) DEFAULT NULL,
    effective_end TIMESTAMP(6) DEFAULT NULL,
    status CHAR(1) DEFAULT '0',
    create_dept INT8,
    create_by INT8,
    create_time TIMESTAMP(6),
    update_by INT8,
    update_time TIMESTAMP(6),
    del_flag CHAR(1) DEFAULT '0',
    remark VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_afo_model_access_policy_tenant_status
    ON afo_model_access_policy (tenant_id, status, del_flag);

CREATE INDEX IF NOT EXISTS idx_afo_model_access_policy_effective
    ON afo_model_access_policy (tenant_id, effective_start, effective_end);

COMMENT ON TABLE afo_model_access_policy IS '企业模型准入策略';
COMMENT ON COLUMN afo_model_access_policy.policy_id IS '策略 ID';
COMMENT ON COLUMN afo_model_access_policy.tenant_id IS '租户 ID';
COMMENT ON COLUMN afo_model_access_policy.policy_name IS '策略名称';
COMMENT ON COLUMN afo_model_access_policy.default_mode IS '默认准入模式：ALLOW_UNLISTED 允许未配置模型，DENY_UNLISTED 拒绝未配置模型';
COMMENT ON COLUMN afo_model_access_policy.allowed_models IS '允许模型编码 JSON 数组';
COMMENT ON COLUMN afo_model_access_policy.denied_models IS '禁止模型编码 JSON 数组';
COMMENT ON COLUMN afo_model_access_policy.effective_start IS '生效开始时间，为空表示立即生效';
COMMENT ON COLUMN afo_model_access_policy.effective_end IS '生效结束时间，为空表示长期有效';
COMMENT ON COLUMN afo_model_access_policy.status IS '状态：0 启用，1 停用';
COMMENT ON COLUMN afo_model_access_policy.create_dept IS '创建部门';
COMMENT ON COLUMN afo_model_access_policy.create_by IS '创建者';
COMMENT ON COLUMN afo_model_access_policy.create_time IS '创建时间';
COMMENT ON COLUMN afo_model_access_policy.update_by IS '更新者';
COMMENT ON COLUMN afo_model_access_policy.update_time IS '更新时间';
COMMENT ON COLUMN afo_model_access_policy.del_flag IS '删除标志：0 存在，1 删除';
COMMENT ON COLUMN afo_model_access_policy.remark IS '备注';

-- 菜单文案迁移：保留历史路由与权限编码，只更新显示名称。
UPDATE sys_menu
SET menu_name = '模型准入管理',
    remark = '企业模型准入配置'
WHERE menu_id = 2002;

UPDATE sys_menu
SET menu_name = '模型准入新增'
WHERE menu_id = 20021;

UPDATE sys_menu
SET menu_name = '模型准入删除'
WHERE menu_id = 20022;

UPDATE sys_menu
SET menu_name = '模型准入修改'
WHERE menu_id = 20023;
