-- ==========================================
-- AI-FinOps Community Edition — L0 初始数据
-- 版本: 1.0.0
-- 说明: 幂等脚本，使用 ON CONFLICT DO NOTHING 保证可重复执行
-- 内容: 菜单(含按钮权限) + 角色-菜单绑定 + 字典 + 参数
-- ==========================================

-- ==========================================
-- Part 1: 菜单数据（仅 L0）
-- ==========================================

-- 1.1 一级目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1,    '系统管理', 0, 9,  'system',    NULL, '', '1', '0', 'M', '0', '0', '',               'local-icon-system',                     103, 1, now(), 1, now(), '系统管理目录'),
(4,    '组织架构', 0, 8,  'org',       NULL, '', '1', '0', 'M', '0', '0', '',               'local-icon-peoples',                    103, 1, now(), 1, now(), '组织架构目录'),
(6,    '租户管理', 0, 10, 'tenant',    NULL, '', '1', '0', 'M', '0', '0', '',               'carbon:user-multiple',                  103, 1, now(), 1, now(), '租户管理目录'),
(108,  '日志审计', 0, 7,  'log-audit', NULL, '', '1', '0', 'M', '0', '0', '',               'local-icon-log',                         103, 1, now(), 1, now(), '日志审计目录'),
(2,    '监控管理', 0, 11, 'monitor',   NULL, '', '1', '0', 'M', '0', '0', '',               'local-icon-monitor',                     103, 1, now(), 1, now(), '监控管理目录'),
(2000, '网关配置', 0, 4,  'gateway',   'Layout', '', '1', '0', 'M', '0', '0', '',           'carbon:api',                             103, 1, now(), 1, now(), '网关配置目录'),
(2067202336714969090, '基础信息', 0, 1, 'basicInfo', 'Layout', '', '1', '1', 'M', '0', '0', '', 'local-icon-list',                     103, 1, now(), 1, now(), '基础信息目录')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.2 系统管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(100, '用户管理', 4, 1, 'user',   'system/user/index',   '', '1', '0', 'C', '0', '0', 'system:user:list',    'carbon:user',             103, 1, now(), 1, now(), '用户管理菜单'),
(101, '角色管理', 4, 2, 'role',   'system/role/index',   '', '1', '0', 'C', '0', '0', 'system:role:list',    'carbon:user-role',         103, 1, now(), 1, now(), '角色管理菜单'),
(102, '菜单管理', 1, 1, 'menu',   'system/menu/index',   '', '1', '0', 'C', '0', '0', 'system:menu:list',    'carbon:tree-view',         103, 1, now(), 1, now(), '菜单管理菜单'),
(103, '部门管理', 4, 3, 'dept',   'system/dept/index',   '', '1', '0', 'C', '0', '0', 'system:dept:list',    'carbon:tree-view-alt',     103, 1, now(), 1, now(), '部门管理菜单'),
(104, '岗位管理', 4, 4, 'post',   'system/post/index',   '', '1', '0', 'C', '0', '0', 'system:post:list',    'carbon:badge',             103, 1, now(), 1, now(), '岗位管理菜单'),
(105, '字典管理', 1, 2, 'dict',   'system/dict/index',   '', '1', '0', 'C', '0', '0', 'system:dict:list',    'local-icon-dict',          103, 1, now(), 1, now(), '字典管理菜单'),
(106, '参数设置', 1, 3, 'config', 'system/config/index', '', '1', '0', 'C', '0', '0', 'system:config:list',  'carbon:settings-adjust',   103, 1, now(), 1, now(), '参数设置菜单'),
(107, '通知公告', 1, 4, 'notice', 'system/notice/index', '', '1', '0', 'C', '0', '0', 'system:notice:list',  'carbon:notification',      103, 1, now(), 1, now(), '通知公告菜单'),
(123, '客户端管理', 1, 6, 'client', 'system/client/index', '', '1', '0', 'C', '0', '0', 'system:client:list', 'carbon:application-web',  103, 1, now(), 1, now(), '客户端管理菜单')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.3 组织架构隐藏页面
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(130, '分配用户', 4, 5, 'role-auth/user/:roleId', 'system/role/authUser', '', '1', '1', 'C', '1', '0', 'system:role:edit',   'carbon:user-follow', 103, 1, now(), 1, now(), ''),
(131, '分配角色', 4, 6, 'user-auth/role/:userId', 'system/user/authRole', '', '1', '1', 'C', '1', '0', 'system:user:edit',   'carbon:user-role',   103, 1, now(), 1, now(), ''),
(132, '字典数据', 1, 9, 'dict-data/index/:dictId', 'system/dict/data', '', '1', '1', 'C', '1', '0', 'system:dict:list',   'carbon:data-structured', 103, 1, now(), 1, now(), '')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.4 租户管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(121, '租户管理',     6, 1, 'tenant',        'system/tenant/index',        '', '1', '0', 'C', '0', '0', 'system:tenant:list',        'carbon:enterprise', 103, 1, now(), 1, now(), '租户管理菜单'),
(122, '租户套餐管理', 6, 2, 'tenantPackage', 'system/tenantPackage/index', '', '1', '0', 'C', '0', '0', 'system:tenantPackage:list', 'carbon:package',    103, 1, now(), 1, now(), '租户套餐管理菜单')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.5 日志审计子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(500, '操作日志', 108, 1, 'operlog',    'monitor/operlog/index',    '', '1', '0', 'C', '0', '0', 'monitor:operlog:list',    'carbon:document', 103, 1, now(), 1, now(), '操作日志菜单'),
(501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '1', '0', 'C', '0', '0', 'monitor:logininfor:list', 'carbon:login',    103, 1, now(), 1, now(), '登录日志菜单')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.6 监控管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '1', '0', 'C', '0', '0', 'monitor:online:list', 'carbon:user-online', 103, 1, now(), 1, now(), '在线用户菜单'),
(113, '缓存监控', 2, 2, 'cache',  'monitor/cache/index',  '', '1', '0', 'C', '0', '0', 'monitor:cache:list',  'local-icon-redis',   103, 1, now(), 1, now(), '缓存监控菜单')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.7 LLM 业务菜单（基础信息子菜单）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(18001, '模型管理',   2067202336714969090, 1, 'model',      'llm/model/index',      '', '1', '0', 'C', '0', '0', 'llm:model:list',      'carbon:model',                      103, 1, now(), 1, now(), 'LLM 模型管理菜单'),
(18002, '项目管理',   2067202336714969090, 2, 'project',    'llm/project/index',    '', '1', '0', 'C', '0', '0', 'llm:project:list',    'carbon:folder-details',              103, 1, now(), 1, now(), 'LLM 项目管理菜单'),
(18003, '应用客户端', 2067202336714969090, 3, 'app-client', 'llm/app-client/index', '', '1', '0', 'C', '0', '0', 'llm:appClient:list',  'carbon:application',                 103, 1, now(), 1, now(), 'LLM 应用客户端管理菜单'),
(18004, 'API Key管理',2067202336714969090, 4, 'api-key',    'llm/api-key/index',    '', '1', '0', 'C', '0', '0', 'llm:apiKey:list',     'carbon:api-1',                       103, 1, now(), 1, now(), 'LLM API Key 管理菜单')
ON CONFLICT (menu_id) DO NOTHING;

-- 1.8 网关配置子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(18017, 'Provider 管理', 2000, 1, 'provider',          'llm/provider/index',                   '', '1', '0', 'C', '0', '0', 'llm:provider:list',             'carbon:cloud-service-management', 103, 1, now(), 1, now(), ''),
(2002,  '路由配置',      2000, 2, 'routing-config',    'gateway/routing-config/index',          '', '1', '0', 'C', '0', '0', 'gateway:whitelist:list',        'carbon:flow',                     103, 1, now(), 1, now(), ''),
(2004,  '缓存管理',      2000, 3, 'cache',             'gateway/cache/index',                   '', '1', '0', 'C', '0', '0', 'gateway:cache:refresh',         'local-icon-redis',                103, 1, now(), 1, now(), ''),
(2012,  '白名单推荐',    2000, 4, 'whitelist-recommend','gateway/whitelist-recommend/index',     '', '1', '0', 'C', '0', '0', 'gateway:whitelist:recommend',   'carbon:recommend',                103, 1, now(), 1, now(), '')
ON CONFLICT (menu_id) DO NOTHING;

-- ==========================================
-- 1.9 按钮级权限（F 类型）
-- ==========================================

-- 用户管理按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1001,'用户查询',100,1,'','','1','0','F','0','0','system:user:query',      'carbon:search',           103,1,now(),1,now(),''),
(1002,'用户新增',100,2,'','','1','0','F','0','0','system:user:add',        'carbon:add',              103,1,now(),1,now(),''),
(1003,'用户修改',100,3,'','','1','0','F','0','0','system:user:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1004,'用户删除',100,4,'','','1','0','F','0','0','system:user:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),
(1005,'用户导出',100,5,'','','1','0','F','0','0','system:user:export',     'carbon:download',         103,1,now(),1,now(),''),
(1006,'用户导入',100,6,'','','1','0','F','0','0','system:user:import',     'carbon:upload',           103,1,now(),1,now(),''),
(1007,'重置密码',100,7,'','','1','0','F','0','0','system:user:resetPwd',   'carbon:password',         103,1,now(),1,now(),''),

-- 角色管理按钮
(1008,'角色查询',101,1,'','','1','0','F','0','0','system:role:query',      'carbon:search',           103,1,now(),1,now(),''),
(1009,'角色新增',101,2,'','','1','0','F','0','0','system:role:add',        'carbon:add',              103,1,now(),1,now(),''),
(1010,'角色修改',101,3,'','','1','0','F','0','0','system:role:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1011,'角色删除',101,4,'','','1','0','F','0','0','system:role:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),
(1012,'角色导出',101,5,'','','1','0','F','0','0','system:role:export',     'carbon:download',         103,1,now(),1,now(),''),

-- 菜单管理按钮
(1013,'菜单查询',102,1,'','','1','0','F','0','0','system:menu:query',      'carbon:search',           103,1,now(),1,now(),''),
(1014,'菜单新增',102,2,'','','1','0','F','0','0','system:menu:add',        'carbon:add',              103,1,now(),1,now(),''),
(1015,'菜单修改',102,3,'','','1','0','F','0','0','system:menu:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1016,'菜单删除',102,4,'','','1','0','F','0','0','system:menu:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),

-- 部门管理按钮
(1017,'部门查询',103,1,'','','1','0','F','0','0','system:dept:query',      'carbon:search',           103,1,now(),1,now(),''),
(1018,'部门新增',103,2,'','','1','0','F','0','0','system:dept:add',        'carbon:add',              103,1,now(),1,now(),''),
(1019,'部门修改',103,3,'','','1','0','F','0','0','system:dept:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1020,'部门删除',103,4,'','','1','0','F','0','0','system:dept:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),

-- 岗位管理按钮
(1021,'岗位查询',104,1,'','','1','0','F','0','0','system:post:query',      'carbon:search',           103,1,now(),1,now(),''),
(1022,'岗位新增',104,2,'','','1','0','F','0','0','system:post:add',        'carbon:add',              103,1,now(),1,now(),''),
(1023,'岗位修改',104,3,'','','1','0','F','0','0','system:post:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1024,'岗位删除',104,4,'','','1','0','F','0','0','system:post:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),
(1025,'岗位导出',104,5,'','','1','0','F','0','0','system:post:export',     'carbon:download',         103,1,now(),1,now(),''),

-- 字典管理按钮
(1026,'字典查询',105,1,'','','1','0','F','0','0','system:dict:query',      'carbon:search',           103,1,now(),1,now(),''),
(1027,'字典新增',105,2,'','','1','0','F','0','0','system:dict:add',        'carbon:add',              103,1,now(),1,now(),''),
(1028,'字典修改',105,3,'','','1','0','F','0','0','system:dict:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1029,'字典删除',105,4,'','','1','0','F','0','0','system:dict:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),
(1030,'字典导出',105,5,'','','1','0','F','0','0','system:dict:export',     'carbon:download',         103,1,now(),1,now(),''),

-- 参数设置按钮
(1031,'参数查询',106,1,'','','1','0','F','0','0','system:config:query',    'carbon:search',           103,1,now(),1,now(),''),
(1032,'参数新增',106,2,'','','1','0','F','0','0','system:config:add',      'carbon:add',              103,1,now(),1,now(),''),
(1033,'参数修改',106,3,'','','1','0','F','0','0','system:config:edit',     'carbon:edit',             103,1,now(),1,now(),''),
(1034,'参数删除',106,4,'','','1','0','F','0','0','system:config:remove',   'carbon:trash-can',        103,1,now(),1,now(),''),
(1035,'参数导出',106,5,'','','1','0','F','0','0','system:config:export',   'carbon:download',         103,1,now(),1,now(),''),

-- 通知公告按钮
(1036,'公告查询',107,1,'','','1','0','F','0','0','system:notice:query',    'carbon:search',           103,1,now(),1,now(),''),
(1037,'公告新增',107,2,'','','1','0','F','0','0','system:notice:add',      'carbon:add',              103,1,now(),1,now(),''),
(1038,'公告修改',107,3,'','','1','0','F','0','0','system:notice:edit',     'carbon:edit',             103,1,now(),1,now(),''),
(1039,'公告删除',107,4,'','','1','0','F','0','0','system:notice:remove',   'carbon:trash-can',        103,1,now(),1,now(),''),

-- 操作日志按钮
(1040,'操作查询',500,1,'','','1','0','F','0','0','monitor:operlog:query',    'carbon:search',           103,1,now(),1,now(),''),
(1041,'操作删除',500,2,'','','1','0','F','0','0','monitor:operlog:remove',   'carbon:trash-can',        103,1,now(),1,now(),''),
(1042,'日志导出',500,4,'','','1','0','F','0','0','monitor:operlog:export',   'carbon:download',         103,1,now(),1,now(),''),

-- 登录日志按钮
(1043,'登录查询',501,1,'','','1','0','F','0','0','monitor:logininfor:query',  'carbon:search',                           103,1,now(),1,now(),''),
(1044,'登录删除',501,2,'','','1','0','F','0','0','monitor:logininfor:remove', 'carbon:trash-can',                        103,1,now(),1,now(),''),
(1045,'日志导出',501,3,'','','1','0','F','0','0','monitor:logininfor:export', 'carbon:download',                         103,1,now(),1,now(),''),
(1050,'账户解锁',501,4,'','','1','0','F','0','0','monitor:logininfor:unlock', 'material-symbols:buttons-alt-outline-rounded', 103,1,now(),1,now(),''),

-- 在线用户按钮
(1046,'在线查询',109,1,'','','1','0','F','0','0','monitor:online:query',       'carbon:search',                           103,1,now(),1,now(),''),
(1047,'批量强退',109,2,'','','1','0','F','0','0','monitor:online:batchLogout', 'material-symbols:buttons-alt-outline-rounded', 103,1,now(),1,now(),''),
(1048,'单条强退',109,3,'','','1','0','F','0','0','monitor:online:forceLogout', 'material-symbols:buttons-alt-outline-rounded', 103,1,now(),1,now(),''),

-- 客户端管理按钮
(1061,'客户端查询',123,1,'','','1','0','F','0','0','system:client:query',      'carbon:search',           103,1,now(),1,now(),''),
(1062,'客户端新增',123,2,'','','1','0','F','0','0','system:client:add',        'carbon:add',              103,1,now(),1,now(),''),
(1063,'客户端修改',123,3,'','','1','0','F','0','0','system:client:edit',       'carbon:edit',             103,1,now(),1,now(),''),
(1064,'客户端删除',123,4,'','','1','0','F','0','0','system:client:remove',     'carbon:trash-can',        103,1,now(),1,now(),''),
(1065,'客户端导出',123,5,'','','1','0','F','0','0','system:client:export',     'carbon:download',         103,1,now(),1,now(),''),

-- 租户管理按钮
(1606,'租户查询',121,1,'','','1','0','F','0','0','system:tenant:query',        'carbon:search',           103,1,now(),1,now(),''),
(1607,'租户新增',121,2,'','','1','0','F','0','0','system:tenant:add',          'carbon:add',              103,1,now(),1,now(),''),
(1608,'租户修改',121,3,'','','1','0','F','0','0','system:tenant:edit',         'carbon:edit',             103,1,now(),1,now(),''),
(1609,'租户删除',121,4,'','','1','0','F','0','0','system:tenant:remove',       'carbon:trash-can',        103,1,now(),1,now(),''),
(1610,'租户导出',121,5,'','','1','0','F','0','0','system:tenant:export',       'carbon:download',         103,1,now(),1,now(),''),

-- 租户套餐管理按钮
(1611,'套餐查询',122,1,'','','1','0','F','0','0','system:tenantPackage:query',  'carbon:search',           103,1,now(),1,now(),''),
(1612,'套餐新增',122,2,'','','1','0','F','0','0','system:tenantPackage:add',    'carbon:add',              103,1,now(),1,now(),''),
(1613,'套餐修改',122,3,'','','1','0','F','0','0','system:tenantPackage:edit',   'carbon:edit',             103,1,now(),1,now(),''),
(1614,'套餐删除',122,4,'','','1','0','F','0','0','system:tenantPackage:remove', 'carbon:trash-can',        103,1,now(),1,now(),''),
(1615,'套餐导出',122,5,'','','1','0','F','0','0','system:tenantPackage:export', 'carbon:download',         103,1,now(),1,now(),''),

-- 模型管理按钮
(18101,'模型查询',18001,1,'','','1','0','F','0','0','llm:model:query',       'carbon:search',           103,1,now(),1,now(),''),
(18102,'模型新增',18001,2,'','','1','0','F','0','0','llm:model:add',         'carbon:add',              103,1,now(),1,now(),''),
(18103,'模型修改',18001,3,'','','1','0','F','0','0','llm:model:edit',        'carbon:edit',             103,1,now(),1,now(),''),
(18104,'模型删除',18001,4,'','','1','0','F','0','0','llm:model:remove',      'carbon:trash-can',        103,1,now(),1,now(),''),
(18105,'模型导出',18001,5,'','','1','0','F','0','0','llm:model:export',      'carbon:download',         103,1,now(),1,now(),''),

-- 项目管理按钮
(18111,'项目查询',18002,1,'','','1','0','F','0','0','llm:project:query',     'carbon:search',           103,1,now(),1,now(),''),
(18112,'项目新增',18002,2,'','','1','0','F','0','0','llm:project:add',       'carbon:add',              103,1,now(),1,now(),''),
(18113,'项目修改',18002,3,'','','1','0','F','0','0','llm:project:edit',      'carbon:edit',             103,1,now(),1,now(),''),
(18114,'项目删除',18002,4,'','','1','0','F','0','0','llm:project:remove',    'carbon:trash-can',        103,1,now(),1,now(),''),
(18115,'项目导出',18002,5,'','','1','0','F','0','0','llm:project:export',    'carbon:download',         103,1,now(),1,now(),''),

-- 应用客户端按钮
(18121,'应用查询',18003,1,'','','1','0','F','0','0','llm:appClient:query',   'carbon:search',           103,1,now(),1,now(),''),
(18122,'应用新增',18003,2,'','','1','0','F','0','0','llm:appClient:add',     'carbon:add',              103,1,now(),1,now(),''),
(18123,'应用修改',18003,3,'','','1','0','F','0','0','llm:appClient:edit',    'carbon:edit',             103,1,now(),1,now(),''),
(18124,'应用删除',18003,4,'','','1','0','F','0','0','llm:appClient:remove',  'carbon:trash-can',        103,1,now(),1,now(),''),
(18125,'应用导出',18003,5,'','','1','0','F','0','0','llm:appClient:export',  'carbon:download',         103,1,now(),1,now(),''),

-- API Key 按钮
(18131,'Key查询', 18004,1,'','','1','0','F','0','0','llm:apiKey:query',     'carbon:search',           103,1,now(),1,now(),''),
(18132,'Key新增', 18004,2,'','','1','0','F','0','0','llm:apiKey:add',       'carbon:add',              103,1,now(),1,now(),''),
(18133,'Key修改', 18004,3,'','','1','0','F','0','0','llm:apiKey:edit',      'carbon:edit',             103,1,now(),1,now(),''),
(18134,'Key删除', 18004,4,'','','1','0','F','0','0','llm:apiKey:remove',    'carbon:trash-can',        103,1,now(),1,now(),''),
(18135,'Key导出', 18004,5,'','','1','0','F','0','0','llm:apiKey:export',    'carbon:download',         103,1,now(),1,now(),''),

-- Provider 管理按钮
(18701,'厂商查询',18017,1,'','','1','0','F','0','0','llm:provider:query',   'carbon:search',           103,1,now(),1,now(),''),
(18232,'厂商新增',18017,2,'','','1','0','F','0','0','llm:provider:add',     'carbon:add',              103,1,now(),1,now(),''),
(18233,'厂商修改',18017,3,'','','1','0','F','0','0','llm:provider:edit',    'carbon:edit',             103,1,now(),1,now(),''),
(18234,'厂商删除',18017,4,'','','1','0','F','0','0','llm:provider:remove',  'carbon:trash-can',        103,1,now(),1,now(),''),

-- 路由配置按钮
(20021,'路由新增',2002,1,'','','1','0','F','0','0','gateway:whitelist:add',    'carbon:add',              103,1,now(),1,now(),''),
(20022,'路由删除',2002,2,'','','1','0','F','0','0','gateway:whitelist:remove', 'carbon:trash-can',        103,1,now(),1,now(),''),
(20023,'路由修改',2002,3,'','','1','0','F','0','0','gateway:whitelist:edit',   'carbon:edit',             103,1,now(),1,now(),''),

-- 缓存管理按钮
(18615,'缓存列表',2004,1,'','','1','0','F','0','0','llm:cache:config:list',   'carbon:list',             103,1,now(),1,now(),''),
(18616,'缓存查询',2004,2,'','','1','0','F','0','0','llm:cache:config:query',  'carbon:search',           103,1,now(),1,now(),'')
ON CONFLICT (menu_id) DO NOTHING;

-- ==========================================
-- Part 2: 角色-菜单绑定（所有 L0 菜单赋予 super_admin）
-- ==========================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key = 'super_admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ==========================================
-- Part 3: 字典初始数据
-- ==========================================
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1, '系统开关', 'sys_normal_disable', 103, 1, now(), 1, now(), ''),
(2, '菜单状态', 'sys_show_hide',     103, 1, now(), 1, now(), ''),
(3, '系统是否', 'sys_yes_no',        103, 1, now(), 1, now(), ''),
(4, '用户性别', 'sys_user_sex',      103, 1, now(), 1, now(), ''),
(5, '通知类型', 'sys_notice_type',   103, 1, now(), 1, now(), ''),
(6, '通知状态', 'sys_notice_status', 103, 1, now(), 1, now(), ''),
(7, '操作类型', 'sys_oper_type',     103, 1, now(), 1, now(), ''),
(8, '系统状态', 'sys_common_status', 103, 1, now(), 1, now(), '')
ON CONFLICT (dict_id) DO NOTHING;

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
-- sys_normal_disable
(1,  0, '正常', '0', 'sys_normal_disable', 'default', 'Y', 103, 1, now(), 1, now(), ''),
(2,  1, '停用', '1', 'sys_normal_disable', 'warning', 'N', 103, 1, now(), 1, now(), ''),
-- sys_show_hide
(3,  0, '显示', '0', 'sys_show_hide', 'primary', 'Y', 103, 1, now(), 1, now(), ''),
(4,  1, '隐藏', '1', 'sys_show_hide', 'danger',  'N', 103, 1, now(), 1, now(), ''),
-- sys_yes_no
(5,  0, '是', 'Y', 'sys_yes_no', 'primary', 'Y', 103, 1, now(), 1, now(), ''),
(6,  1, '否', 'N', 'sys_yes_no', 'danger',  'N', 103, 1, now(), 1, now(), ''),
-- sys_user_sex
(7,  0, '男',   '0', 'sys_user_sex', '', 'Y', 103, 1, now(), 1, now(), ''),
(8,  1, '女',   '1', 'sys_user_sex', '', 'N', 103, 1, now(), 1, now(), ''),
(9,  2, '未知', '2', 'sys_user_sex', '', 'N', 103, 1, now(), 1, now(), ''),
-- sys_notice_type
(10, 0, '通知', '1', 'sys_notice_type', 'primary', 'Y', 103, 1, now(), 1, now(), ''),
(11, 1, '公告', '2', 'sys_notice_type', 'success', 'N', 103, 1, now(), 1, now(), ''),
-- sys_notice_status
(12, 0, '正常', '0', 'sys_notice_status', 'primary', 'Y', 103, 1, now(), 1, now(), ''),
(13, 1, '关闭', '1', 'sys_notice_status', 'danger',  'N', 103, 1, now(), 1, now(), ''),
-- sys_oper_type
(14, 0, '新增', '1', 'sys_oper_type', 'primary', 'N', 103, 1, now(), 1, now(), ''),
(15, 1, '修改', '2', 'sys_oper_type', 'info',    'N', 103, 1, now(), 1, now(), ''),
(16, 2, '删除', '3', 'sys_oper_type', 'danger',  'N', 103, 1, now(), 1, now(), ''),
(17, 3, '授权', '4', 'sys_oper_type', 'primary', 'N', 103, 1, now(), 1, now(), ''),
(18, 4, '导出', '5', 'sys_oper_type', 'warning', 'N', 103, 1, now(), 1, now(), ''),
(19, 5, '导入', '6', 'sys_oper_type', 'warning', 'N', 103, 1, now(), 1, now(), ''),
(20, 6, '强退', '7', 'sys_oper_type', 'danger',  'N', 103, 1, now(), 1, now(), ''),
(21, 7, '生成代码', '8', 'sys_oper_type', 'warning', 'N', 103, 1, now(), 1, now(), ''),
(22, 8, '清空数据', '9', 'sys_oper_type', 'danger',  'N', 103, 1, now(), 1, now(), ''),
-- sys_common_status
(23, 0, '成功', '0', 'sys_common_status', 'primary', 'N', 103, 1, now(), 1, now(), ''),
(24, 1, '失败', '1', 'sys_common_status', 'danger',  'N', 103, 1, now(), 1, now(), '')
ON CONFLICT (dict_code) DO NOTHING;

-- ==========================================
-- Part 4: 参数配置
-- ==========================================
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1, '主框架页-默认皮肤样式名称', 'sys.index.skinName',     'skin-blue',     'Y', 103, 1, now(), 1, now(), '蓝色'),
(2, '用户管理-账号初始密码',     'sys.user.initPassword',  'admin123',      'Y', 103, 1, now(), 1, now(), ''),
(3, '主框架页-侧边栏主题',       'sys.index.sideTheme',    'theme-dark',    'Y', 103, 1, now(), 1, now(), ''),
(4, '账号自助-是否开启注册功能', 'sys.account.registerUser', 'false',       'Y', 103, 1, now(), 1, now(), ''),
(5, '用户登录-黑名单列表',       'sys.login.blackIPList',  '',              'Y', 103, 1, now(), 1, now(), ''),
(6, '系统预览-是否开启水印',     'sys.watermark.enabled',  'false',         'Y', 103, 1, now(), 1, now(), ''),
(7, '系统预览-验证码开关',       'sys.account.captchaEnabled', 'false',     'Y', 103, 1, now(), 1, now(), ''),
(8, '系统预览-登录页标语',       'sys.account.loginSlogan', 'AI-FinOps Community Edition', 'Y', 103, 1, now(), 1, now(), '')
ON CONFLICT (config_id) DO NOTHING;
