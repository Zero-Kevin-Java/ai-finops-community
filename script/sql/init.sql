-- AI-FinOps Community Edition — 数据库初始化脚本
-- 版本: 1.0.0
-- 数据库: PostgreSQL 17
-- 说明: 幂等脚本，使用 IF NOT EXISTS 保证重复执行安全
-- 来源: 从商业项目 SQL 脚本中提取的 L0 所需 26 张表

create extension if not exists btree_gist;

-- ==========================================
-- Part 1: 系统表（15 张）
-- 来源: D:/project/WL/AI-FinOps/script/sql/postgres/postgres_ry_vue_5.X.sql
--       D:/project/WL/AI-FinOps/script/sql/update/postgres/update_multi_tenant_user_switch_20260606.sql
-- ==========================================

-- 1. sys_tenant
-- 来源: postgres_ry_vue_5.X.sql L72-L105
create table if not exists sys_tenant
(
    id                int8,
    tenant_id         varchar(20)   not null,
    contact_user_name varchar(20)   default null::varchar,
    contact_phone     varchar(20)   default null::varchar,
    company_name      varchar(30)   default null::varchar,
    license_number    varchar(30)   default null::varchar,
    address           varchar(200)  default null::varchar,
    intro             varchar(200)  default null::varchar,
    domain            varchar(200)  default null::varchar,
    remark            varchar(200)  default null::varchar,
    package_id        int8,
    expire_time       timestamp,
    account_count     int4          default -1,
    status            char          default '0'::bpchar,
    del_flag          char          default '0'::bpchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    constraint "pk_sys_tenant" primary key (id)
);

comment on table   sys_tenant                    is '租户表';
comment on column  sys_tenant.tenant_id          is '租户编号';
comment on column  sys_tenant.contact_user_name  is '联系人';
comment on column  sys_tenant.contact_phone      is '联系电话';
comment on column  sys_tenant.company_name       is '企业名称';
comment on column  sys_tenant.license_number     is '统一社会信用代码';
comment on column  sys_tenant.address            is '地址';
comment on column  sys_tenant.intro              is '企业简介';
comment on column  sys_tenant.domain             is '域名';
comment on column  sys_tenant.remark             is '备注';
comment on column  sys_tenant.package_id         is '租户套餐编号';
comment on column  sys_tenant.expire_time        is '过期时间';
comment on column  sys_tenant.account_count      is '用户数量（-1不限制）';
comment on column  sys_tenant.status             is '租户状态（0正常 1停用）';
comment on column  sys_tenant.del_flag           is '删除标志（0代表存在 1代表删除）';
comment on column  sys_tenant.create_dept        is '创建部门';
comment on column  sys_tenant.create_by          is '创建者';
comment on column  sys_tenant.create_time        is '创建时间';
comment on column  sys_tenant.update_by          is '更新者';
comment on column  sys_tenant.update_time        is '更新时间';

-- 2. sys_tenant_package
-- 来源: postgres_ry_vue_5.X.sql L130-L145
create table if not exists sys_tenant_package
(
    package_id          int8,
    package_name        varchar(20)     default ''::varchar,
    menu_ids            varchar(3000)   default ''::varchar,
    remark              varchar(200)    default ''::varchar,
    menu_check_strictly bool            default true,
    status              char            default '0'::bpchar,
    del_flag            char            default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    constraint "pk_sys_tenant_package" primary key (package_id)
);

comment on table   sys_tenant_package                    is '租户套餐表';
comment on column  sys_tenant_package.package_id         is '租户套餐id';
comment on column  sys_tenant_package.package_name       is '套餐名称';
comment on column  sys_tenant_package.menu_ids           is '关联菜单id';
comment on column  sys_tenant_package.remark             is '备注';
comment on column  sys_tenant_package.status             is '状态（0正常 1停用）';
comment on column  sys_tenant_package.del_flag           is '删除标志（0代表存在 1代表删除）';
comment on column  sys_tenant_package.create_dept        is '创建部门';
comment on column  sys_tenant_package.create_by          is '创建者';
comment on column  sys_tenant_package.create_time        is '创建时间';
comment on column  sys_tenant_package.update_by          is '更新者';
comment on column  sys_tenant_package.update_time        is '更新时间';

-- 3. sys_dept
-- 来源: postgres_ry_vue_5.X.sql L165-L193
create table if not exists sys_dept
(
    dept_id     int8,
    tenant_id   varchar(20) default '000000'::varchar,
    parent_id   int8        default 0,
    ancestors   varchar(500)default ''::varchar,
    dept_name   varchar(30) default ''::varchar,
    dept_category varchar(100) default null::varchar,
    order_num   int4        default 0,
    leader      int8        default null,
    phone       varchar(11) default null::varchar,
    email       varchar(50) default null::varchar,
    status      char        default '0'::bpchar,
    del_flag    char        default '0'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    constraint "sys_dept_pk" primary key (dept_id)
);

comment on table sys_dept               is '部门表';
comment on column sys_dept.dept_id      is '部门ID';
comment on column sys_dept.tenant_id    is '租户编号';
comment on column sys_dept.parent_id    is '父部门ID';
comment on column sys_dept.ancestors    is '祖级列表';
comment on column sys_dept.dept_name    is '部门名称';
comment on column sys_dept.dept_category    is '部门类别编码';
comment on column sys_dept.order_num    is '显示顺序';
comment on column sys_dept.leader       is '负责人';
comment on column sys_dept.phone        is '联系电话';
comment on column sys_dept.email        is '邮箱';
comment on column sys_dept.status       is '部门状态（0正常 1停用）';
comment on column sys_dept.del_flag     is '删除标志（0代表存在 1代表删除）';
comment on column sys_dept.create_dept  is '创建部门';
comment on column sys_dept.create_by    is '创建者';
comment on column sys_dept.create_time  is '创建时间';
comment on column sys_dept.update_by    is '更新者';
comment on column sys_dept.update_time  is '更新时间';

-- 4. sys_user
-- 来源: postgres_ry_vue_5.X.sql L223-L273
create table if not exists sys_user
(
    user_id     int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dept_id     int8,
    user_name   varchar(30)  not null,
    nick_name   varchar(30)  not null,
    user_type   varchar(10)  default 'sys_user'::varchar,
    email       varchar(50)  default ''::varchar,
    phonenumber varchar(11)  default ''::varchar,
    sex         char         default '0'::bpchar,
    avatar      int8,
    password    varchar(100) default ''::varchar,
    status      char         default '0'::bpchar,
    del_flag    char         default '0'::bpchar,
    login_ip    varchar(128) default ''::varchar,
    login_date  timestamp,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint "sys_user_pk" primary key (user_id)
);

comment on table sys_user               is '用户信息表';
comment on column sys_user.user_id      is '用户ID';
comment on column sys_user.tenant_id    is '租户编号';
comment on column sys_user.dept_id      is '部门ID';
comment on column sys_user.user_name    is '用户账号';
comment on column sys_user.nick_name    is '用户昵称';
comment on column sys_user.user_type    is '用户类型（sys_user系统用户）';
comment on column sys_user.email        is '用户邮箱';
comment on column sys_user.phonenumber  is '手机号码';
comment on column sys_user.sex          is '用户性别（0男 1女 2未知）';
comment on column sys_user.avatar       is '头像地址';
comment on column sys_user.password     is '密码';
comment on column sys_user.status       is '账号状态（0正常 1停用）';
comment on column sys_user.del_flag     is '删除标志（0代表存在 1代表删除）';
comment on column sys_user.login_ip     is '最后登陆IP';
comment on column sys_user.login_date   is '最后登陆时间';
comment on column sys_user.create_dept  is '创建部门';
comment on column sys_user.create_by    is '创建者';
comment on column sys_user.create_time  is '创建时间';
comment on column sys_user.update_by    is '更新者';
comment on column sys_user.update_time  is '更新时间';
comment on column sys_user.remark       is '备注';

-- 5. sys_post
-- 来源: postgres_ry_vue_5.X.sql L283-L310
create table if not exists sys_post
(
    post_id     int8,
    tenant_id   varchar(20) default '000000'::varchar,
    dept_id     int8,
    post_code   varchar(64) not null,
    post_category   varchar(100) default null,
    post_name   varchar(50) not null,
    post_sort   int4        not null,
    status      char        not null,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint "sys_post_pk" primary key (post_id)
);

comment on table sys_post               is '岗位信息表';
comment on column sys_post.post_id      is '岗位ID';
comment on column sys_post.tenant_id    is '租户编号';
comment on column sys_post.dept_id      is '部门id';
comment on column sys_post.post_code    is '岗位编码';
comment on column sys_post.post_category is '岗位类别编码';
comment on column sys_post.post_name    is '岗位名称';
comment on column sys_post.post_sort    is '显示顺序';
comment on column sys_post.status       is '状态（0正常 1停用）';
comment on column sys_post.create_dept  is '创建部门';
comment on column sys_post.create_by    is '创建者';
comment on column sys_post.create_time  is '创建时间';
comment on column sys_post.update_by    is '更新者';
comment on column sys_post.update_time  is '更新时间';
comment on column sys_post.remark       is '备注';

-- 6. sys_role
-- 来源: postgres_ry_vue_5.X.sql L329-L374
create table if not exists sys_role
(
    role_id             int8,
    tenant_id           varchar(20)  default '000000'::varchar,
    role_name           varchar(30)  not null,
    role_key            varchar(100) not null,
    role_sort           int4         not null,
    data_scope          char         default '1'::bpchar,
    menu_check_strictly bool         default true,
    dept_check_strictly bool         default true,
    status              char         not null,
    del_flag            char         default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    remark              varchar(500) default null::varchar,
    constraint "sys_role_pk" primary key (role_id)
);

comment on table sys_role                       is '角色信息表';
comment on column sys_role.role_id              is '角色ID';
comment on column sys_role.tenant_id            is '租户编号';
comment on column sys_role.role_name            is '角色名称';
comment on column sys_role.role_key             is '角色权限字符串';
comment on column sys_role.role_sort            is '显示顺序';
comment on column sys_role.data_scope           is '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
comment on column sys_role.menu_check_strictly  is '菜单树选择项是否关联显示';
comment on column sys_role.dept_check_strictly  is '部门树选择项是否关联显示';
comment on column sys_role.status               is '角色状态（0正常 1停用）';
comment on column sys_role.del_flag             is '删除标志（0代表存在 1代表删除）';
comment on column sys_role.create_dept          is '创建部门';
comment on column sys_role.create_by            is '创建者';
comment on column sys_role.create_time          is '创建时间';
comment on column sys_role.update_by            is '更新者';
comment on column sys_role.update_time          is '更新时间';
comment on column sys_role.remark               is '备注';

-- 7. sys_menu
-- 来源: postgres_ry_vue_5.X.sql L378-L421
create table if not exists sys_menu
(
    menu_id     int8,
    menu_name   varchar(50) not null,
    parent_id   int8         default 0,
    order_num   int4         default 0,
    path        varchar(200) default ''::varchar,
    component   varchar(255) default null::varchar,
    query_param varchar(255) default null::varchar,
    is_frame    char         default '1'::bpchar,
    is_cache    char         default '0'::bpchar,
    menu_type   char         default ''::bpchar,
    visible     char         default '0'::bpchar,
    status      char         default '0'::bpchar,
    perms       varchar(100) default null::varchar,
    icon        varchar(100) default '#'::varchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default ''::varchar,
    constraint "sys_menu_pk" primary key (menu_id)
);

comment on table sys_menu               is '菜单权限表';
comment on column sys_menu.menu_id      is '菜单ID';
comment on column sys_menu.menu_name    is '菜单名称';
comment on column sys_menu.parent_id    is '父菜单ID';
comment on column sys_menu.order_num    is '显示顺序';
comment on column sys_menu.path         is '路由地址';
comment on column sys_menu.component    is '组件路径';
comment on column sys_menu.query_param  is '路由参数';
comment on column sys_menu.is_frame     is '是否为外链（0是 1否）';
comment on column sys_menu.is_cache     is '是否缓存（0缓存 1不缓存）';
comment on column sys_menu.menu_type    is '菜单类型（M目录 C菜单 F按钮）';
comment on column sys_menu.visible      is '显示状态（0显示 1隐藏）';
comment on column sys_menu.status       is '菜单状态（0正常 1停用）';
comment on column sys_menu.perms        is '权限标识';
comment on column sys_menu.icon         is '菜单图标';
comment on column sys_menu.create_dept  is '创建部门';
comment on column sys_menu.create_by    is '创建者';
comment on column sys_menu.create_time  is '创建时间';
comment on column sys_menu.update_by    is '更新者';
comment on column sys_menu.update_time  is '更新时间';
comment on column sys_menu.remark       is '备注';

-- 8. sys_user_role
-- 来源: postgres_ry_vue_5.X.sql L579-L586
create table if not exists sys_user_role
(
    user_id int8 not null,
    role_id int8 not null,
    constraint sys_user_role_pk primary key (user_id, role_id)
);

comment on table sys_user_role              is '用户和角色关联表';
comment on column sys_user_role.user_id     is '用户ID';
comment on column sys_user_role.role_id     is '角色ID';

-- 9. sys_role_menu
-- 来源: postgres_ry_vue_5.X.sql L600-L607
create table if not exists sys_role_menu
(
    role_id int8 not null,
    menu_id int8 not null,
    constraint sys_role_menu_pk primary key (role_id, menu_id)
);

comment on table sys_role_menu              is '角色和菜单关联表';
comment on column sys_role_menu.role_id     is '角色ID';
comment on column sys_role_menu.menu_id     is '菜单ID';

-- 10. sys_role_dept
-- 来源: postgres_ry_vue_5.X.sql L736-L745
create table if not exists sys_role_dept
(
    role_id int8 not null,
    dept_id int8 not null,
    constraint sys_role_dept_pk primary key (role_id, dept_id)
);

comment on table sys_role_dept              is '角色和部门关联表';
comment on column sys_role_dept.role_id     is '角色ID';
comment on column sys_role_dept.dept_id     is '部门ID';

-- 11. sys_user_post
-- 来源: postgres_ry_vue_5.X.sql L751-L760
create table if not exists sys_user_post
(
    user_id int8 not null,
    post_id int8 not null,
    constraint sys_user_post_pk primary key (user_id, post_id)
);

comment on table sys_user_post              is '用户与岗位关联表';
comment on column sys_user_post.user_id     is '用户ID';
comment on column sys_user_post.post_id     is '岗位ID';

-- 12. sys_user_tenant
-- 来源: update_multi_tenant_user_switch_20260606.sql L6-L21
create table if not exists sys_user_tenant
(
    id               int8         not null,
    user_id          int8         not null,
    tenant_id        varchar(20)  not null,
    dept_id          int8,
    status           char(1)      default '0',
    last_login_time  timestamp(6),
    create_dept      int8,
    create_by        int8,
    create_time      timestamp(6),
    update_by        int8,
    update_time      timestamp(6),
    del_flag         char(1)      default '0',
    remark           varchar(500),
    constraint sys_user_tenant_pk primary key (id)
);

create unique index if not exists idx_sys_user_tenant_user_tenant
    on sys_user_tenant (user_id, tenant_id) where del_flag = '0';

comment on table  sys_user_tenant                is '用户租户关系表';
comment on column sys_user_tenant.id             is '主键';
comment on column sys_user_tenant.user_id        is '用户ID';
comment on column sys_user_tenant.tenant_id      is '租户编号';
comment on column sys_user_tenant.dept_id        is '当前租户部门ID';
comment on column sys_user_tenant.status         is '状态（0正常 1停用）';
comment on column sys_user_tenant.last_login_time is '最后登录时间';
comment on column sys_user_tenant.create_dept    is '创建部门';
comment on column sys_user_tenant.create_by      is '创建者';
comment on column sys_user_tenant.create_time    is '创建时间';
comment on column sys_user_tenant.update_by      is '更新者';
comment on column sys_user_tenant.update_time    is '更新时间';
comment on column sys_user_tenant.del_flag       is '删除标志（0代表存在 1代表删除）';
comment on column sys_user_tenant.remark         is '备注';

-- 13. sys_dict_type
-- 来源: postgres_ry_vue_5.X.sql L820-L847
create table if not exists sys_dict_type
(
    dict_id     int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dict_name   varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_type_pk primary key (dict_id)
);

create unique index if not exists sys_dict_type_index1 ON sys_dict_type (tenant_id, dict_type);

comment on table sys_dict_type                  is '字典类型表';
comment on column sys_dict_type.dict_id         is '字典主键';
comment on column sys_dict_type.tenant_id       is '租户编号';
comment on column sys_dict_type.dict_name       is '字典名称';
comment on column sys_dict_type.dict_type       is '字典类型';
comment on column sys_dict_type.create_dept     is '创建部门';
comment on column sys_dict_type.create_by       is '创建者';
comment on column sys_dict_type.create_time     is '创建时间';
comment on column sys_dict_type.update_by       is '更新者';
comment on column sys_dict_type.update_time     is '更新时间';
comment on column sys_dict_type.remark          is '备注';

-- 14. sys_dict_data
-- 来源: postgres_ry_vue_5.X.sql L863-L898
create table if not exists sys_dict_data
(
    dict_code   int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dict_sort   int4         default 0,
    dict_label  varchar(100) default ''::varchar,
    dict_value  varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    css_class   varchar(100) default null::varchar,
    list_class  varchar(100) default null::varchar,
    is_default  char         default 'N'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_data_pk primary key (dict_code)
);

comment on table sys_dict_data                  is '字典数据表';
comment on column sys_dict_data.dict_code       is '字典编码';
comment on column sys_dict_data.tenant_id       is '租户编号';
comment on column sys_dict_data.dict_sort       is '字典排序';
comment on column sys_dict_data.dict_label      is '字典标签';
comment on column sys_dict_data.dict_value      is '字典键值';
comment on column sys_dict_data.dict_type       is '字典类型';
comment on column sys_dict_data.css_class       is '样式属性（其他样式扩展）';
comment on column sys_dict_data.list_class      is '表格回显样式';
comment on column sys_dict_data.is_default      is '是否默认（Y是 N否）';
comment on column sys_dict_data.create_dept     is '创建部门';
comment on column sys_dict_data.create_by       is '创建者';
comment on column sys_dict_data.create_time     is '创建时间';
comment on column sys_dict_data.update_by       is '更新者';
comment on column sys_dict_data.update_time     is '更新时间';
comment on column sys_dict_data.remark          is '备注';

-- 15. sys_config
-- 来源: postgres_ry_vue_5.X.sql L939-L968
create table if not exists sys_config
(
    config_id    int8,
    tenant_id    varchar(20)  default '000000'::varchar,
    config_name  varchar(100) default ''::varchar,
    config_key   varchar(100) default ''::varchar,
    config_value varchar(500) default ''::varchar,
    config_type  char         default 'N'::bpchar,
    create_dept  int8,
    create_by    int8,
    create_time  timestamp,
    update_by    int8,
    update_time  timestamp,
    remark       varchar(500) default null::varchar,
    constraint sys_config_pk primary key (config_id)
);

comment on table sys_config                 is '参数配置表';
comment on column sys_config.config_id      is '参数主键';
comment on column sys_config.tenant_id      is '租户编号';
comment on column sys_config.config_name    is '参数名称';
comment on column sys_config.config_key     is '参数键名';
comment on column sys_config.config_value   is '参数键值';
comment on column sys_config.config_type    is '系统内置（Y是 N否）';
comment on column sys_config.create_dept    is '创建部门';
comment on column sys_config.create_by      is '创建者';
comment on column sys_config.create_time    is '创建时间';
comment on column sys_config.update_by      is '更新者';
comment on column sys_config.update_time    is '更新时间';
comment on column sys_config.remark         is '备注';

-- ==========================================
-- Part 2: LLM 业务表（7 张）
-- 来源: D:/project/WL/AI-FinOps/afo-modules/afo-llm/src/main/resources/sql/postgres/afo_llm_init.sql
-- ==========================================

-- 16. afo_llm_tenant
-- 来源: afo_llm_init.sql L15-L59
create table if not exists afo_llm_tenant
(
    id               int8,
    tenant_id        varchar(20)  default '000000'::varchar,
    tenant_code      varchar(64)  not null,
    tenant_name      varchar(100) not null,
    contact_name     varchar(50)  default null::varchar,
    contact_phone    varchar(30)  default null::varchar,
    billing_currency varchar(10)  default 'USD'::varchar,
    quota_enabled    char         default '1'::bpchar,
    status           char         default '0'::bpchar,
    create_dept      int8,
    create_by        int8,
    create_time      timestamp,
    update_by        int8,
    update_time      timestamp,
    del_flag         char         default '0'::bpchar,
    remark           varchar(500) default null::varchar,
    constraint pk_afo_llm_tenant primary key (id)
);

comment on table afo_llm_tenant is 'LLM 租户业务档案表';
comment on column afo_llm_tenant.id is '主键';
comment on column afo_llm_tenant.tenant_id is '系统租户编号';
comment on column afo_llm_tenant.tenant_code is 'LLM 业务租户编码';
comment on column afo_llm_tenant.tenant_name is 'LLM 业务租户名称';
comment on column afo_llm_tenant.contact_name is '联系人姓名';
comment on column afo_llm_tenant.contact_phone is '联系人电话';
comment on column afo_llm_tenant.billing_currency is '默认计费币种';
comment on column afo_llm_tenant.quota_enabled is '是否启用额度控制（0否 1是）';
comment on column afo_llm_tenant.status is '状态（0正常 1停用）';
comment on column afo_llm_tenant.create_dept is '创建部门';
comment on column afo_llm_tenant.create_by is '创建者';
comment on column afo_llm_tenant.create_time is '创建时间';
comment on column afo_llm_tenant.update_by is '更新者';
comment on column afo_llm_tenant.update_time is '更新时间';
comment on column afo_llm_tenant.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_tenant.remark is '备注';

create unique index if not exists uk_afo_llm_tenant_tenant_id on afo_llm_tenant (tenant_id) where del_flag = '0';
comment on index uk_afo_llm_tenant_tenant_id is '唯一约束：未删除数据中一个系统租户只对应一条 LLM 业务档案';
create unique index if not exists uk_afo_llm_tenant_code on afo_llm_tenant (tenant_code) where del_flag = '0';
comment on index uk_afo_llm_tenant_code is '唯一约束：未删除数据中 LLM 业务租户编码唯一';
create index if not exists idx_afo_llm_tenant_status on afo_llm_tenant (status, del_flag);
comment on index idx_afo_llm_tenant_status is '查询场景：后台按状态筛选启用或停用的 LLM 租户';

-- 17. afo_llm_project
-- 来源: afo_llm_init.sql L64-L100
create table if not exists afo_llm_project
(
    project_id   int8,
    tenant_id    varchar(20)  default '000000'::varchar,
    project_code varchar(64)  not null,
    project_name varchar(100) not null,
    owner_user_id int8        default null,
    status       char         default '0'::bpchar,
    create_dept  int8,
    create_by    int8,
    create_time  timestamp,
    update_by    int8,
    update_time  timestamp,
    del_flag     char         default '0'::bpchar,
    remark       varchar(500) default null::varchar,
    constraint pk_afo_llm_project primary key (project_id)
);

comment on table afo_llm_project is 'LLM 项目表';
comment on column afo_llm_project.project_id is '项目ID';
comment on column afo_llm_project.tenant_id is '系统租户编号';
comment on column afo_llm_project.project_code is '项目编码';
comment on column afo_llm_project.project_name is '项目名称';
comment on column afo_llm_project.owner_user_id is '项目负责人用户ID';
comment on column afo_llm_project.status is '状态（0正常 1停用）';
comment on column afo_llm_project.create_dept is '创建部门';
comment on column afo_llm_project.create_by is '创建者';
comment on column afo_llm_project.create_time is '创建时间';
comment on column afo_llm_project.update_by is '更新者';
comment on column afo_llm_project.update_time is '更新时间';
comment on column afo_llm_project.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_project.remark is '备注';

create unique index if not exists uk_afo_llm_project_code on afo_llm_project (tenant_id, project_code) where del_flag = '0';
comment on index uk_afo_llm_project_code is '唯一约束：未删除数据中同一租户下项目编码唯一';
create index if not exists idx_afo_llm_project_tenant_status on afo_llm_project (tenant_id, status, del_flag);
comment on index idx_afo_llm_project_tenant_status is '查询场景：后台按租户和状态分页查询项目列表';

-- 18. afo_llm_app_client
-- 来源: afo_llm_init.sql L105-L143
create table if not exists afo_llm_app_client
(
    client_id       int8,
    tenant_id       varchar(20)   default '000000'::varchar,
    project_id      int8          not null,
    app_code        varchar(64)   not null,
    app_name        varchar(100)  not null,
    app_type        varchar(30)   default 'server'::varchar,
    status          char          default '0'::bpchar,
    create_dept     int8,
    create_by       int8,
    create_time     timestamp,
    update_by       int8,
    update_time     timestamp,
    del_flag        char          default '0'::bpchar,
    remark          varchar(500)  default null::varchar,
    constraint pk_afo_llm_app_client primary key (client_id)
);

comment on table afo_llm_app_client is 'LLM 应用客户端表';
comment on column afo_llm_app_client.client_id is '应用客户端ID';
comment on column afo_llm_app_client.tenant_id is '系统租户编号';
comment on column afo_llm_app_client.project_id is '项目ID';
comment on column afo_llm_app_client.app_code is '应用编码';
comment on column afo_llm_app_client.app_name is '应用名称';
comment on column afo_llm_app_client.app_type is '应用类型（server/web/mobile/internal）';
comment on column afo_llm_app_client.status is '状态（0正常 1停用）';
comment on column afo_llm_app_client.create_dept is '创建部门';
comment on column afo_llm_app_client.create_by is '创建者';
comment on column afo_llm_app_client.create_time is '创建时间';
comment on column afo_llm_app_client.update_by is '更新者';
comment on column afo_llm_app_client.update_time is '更新时间';
comment on column afo_llm_app_client.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_app_client.remark is '备注';

create unique index if not exists uk_afo_llm_app_client_code on afo_llm_app_client (tenant_id, app_code) where del_flag = '0';
comment on index uk_afo_llm_app_client_code is '唯一约束：未删除数据中同一租户下应用编码唯一';
create index if not exists idx_afo_llm_app_client_project_status on afo_llm_app_client (tenant_id, project_id, status, del_flag);
comment on index idx_afo_llm_app_client_project_status is '查询场景：按项目和状态分页查询应用客户端';

-- 19. afo_llm_api_key
-- 来源: afo_llm_init.sql L148-L198
create table if not exists afo_llm_api_key
(
    key_id         int8,
    tenant_id      varchar(20)  default '000000'::varchar,
    client_id      int8         not null,
    owner_user_id  int8         not null,
    key_name       varchar(100) not null,
    key_prefix     varchar(32)  not null,
    key_hash       varchar(128) not null,
    key_scope      varchar(500) default null::varchar,
    expire_time    timestamp    default null,
    last_used_time timestamp    default null,
    status         char         default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    del_flag       char         default '0'::bpchar,
    remark         varchar(500) default null::varchar,
    constraint pk_afo_llm_api_key primary key (key_id)
);

comment on table afo_llm_api_key is 'LLM 业务 API Key 表';
comment on column afo_llm_api_key.key_id is 'API Key ID';
comment on column afo_llm_api_key.tenant_id is '系统租户编号';
comment on column afo_llm_api_key.client_id is '应用客户端ID';
comment on column afo_llm_api_key.owner_user_id is '所属用户ID';
comment on column afo_llm_api_key.key_name is 'Key 名称';
comment on column afo_llm_api_key.key_prefix is 'API Key 定位前缀，用于检索，不对外明文返回';
comment on column afo_llm_api_key.key_hash is 'Key 哈希值，不存储明文';
comment on column afo_llm_api_key.key_scope is '授权范围，多个用逗号分隔';
comment on column afo_llm_api_key.expire_time is '过期时间，空值表示永不过期';
comment on column afo_llm_api_key.last_used_time is '最后使用时间';
comment on column afo_llm_api_key.status is '状态（0正常 1停用）';
comment on column afo_llm_api_key.create_dept is '创建部门';
comment on column afo_llm_api_key.create_by is '创建者';
comment on column afo_llm_api_key.create_time is '创建时间';
comment on column afo_llm_api_key.update_by is '更新者';
comment on column afo_llm_api_key.update_time is '更新时间';
comment on column afo_llm_api_key.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_api_key.remark is '备注';

create unique index if not exists uk_afo_llm_api_key_prefix on afo_llm_api_key (key_prefix);
comment on index uk_afo_llm_api_key_prefix is '唯一约束：API Key 定位前缀唯一，用于日志定位';
create unique index if not exists uk_afo_llm_api_key_hash on afo_llm_api_key (key_hash);
comment on index uk_afo_llm_api_key_hash is '唯一约束：Key 哈希唯一，用于鉴权查找';
create index if not exists idx_afo_llm_api_key_client_status on afo_llm_api_key (tenant_id, client_id, status, del_flag);
comment on index idx_afo_llm_api_key_client_status is '查询场景：按应用客户端和状态分页查询 API Key';
create index if not exists idx_afo_llm_api_key_owner_status on afo_llm_api_key (tenant_id, owner_user_id, status, del_flag);
comment on index idx_afo_llm_api_key_owner_status is '查询场景：按所属用户和状态分页查询 API Key';

-- 20. afo_llm_provider
-- 来源: afo_llm_init.sql L1044-L1062
create table if not exists afo_llm_provider
(
    provider_id       int8,
    provider_name     varchar(100) not null,
    logo_slug         varchar(100) default null::varchar,
    model_prefixes    varchar(500) default null::varchar,
    status            char         default '0'::bpchar,
    sort_order        int4         default 0,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    del_flag          char         default '0'::bpchar,
    remark            varchar(500) default null::varchar,
    constraint pk_afo_llm_provider primary key (provider_id)
);

create index if not exists idx_afo_llm_provider_status_sort on afo_llm_provider (status, sort_order, del_flag);

-- 21. afo_llm_model_catalog
-- 来源: afo_llm_init.sql L203-L281
create table if not exists afo_llm_model_catalog
(
    model_id          int8,
    tenant_id         varchar(20)  default '000000'::varchar,
    model_code        varchar(100) not null,
    display_name      varchar(100) not null,
    provider          varchar(50)  not null,
    supplier          varchar(100) default null::varchar,
    litellm_model     varchar(150) not null,
    model_type        varchar(30)  default 'chat'::varchar,
    context_window                  int4         default null,
    supports_stream                 char         default '1'::bpchar,
    supports_tool                   char         default '0'::bpchar,
    protocol                        varchar(20)  not null default 'openai',
    api_key                         text,
    api_base                        varchar(500),
    supports_vision                 char         default '0'::bpchar,
    supports_parallel_function_calling char      default '0'::bpchar,
    supports_reasoning              char         default '0'::bpchar,
    max_input_tokens                int4,
    max_output_tokens               int4,
    api_version                     varchar(50),
    rate_limit_tpm                  int4,
    rate_limit_rpm                  int4,
    request_timeout                 int4,
    model_tags                      text,
    routing_order                   int4,
    status                          char         default '0'::bpchar,
    sync_status                     varchar(20)  default 'synced'::varchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    del_flag          char         default '0'::bpchar,
    remark            varchar(500) default null::varchar,
    constraint pk_afo_llm_model_catalog primary key (model_id)
);

comment on table afo_llm_model_catalog is 'LLM 模型目录表';
comment on column afo_llm_model_catalog.model_id is '模型ID';
comment on column afo_llm_model_catalog.tenant_id is '系统租户编号';
comment on column afo_llm_model_catalog.model_code is '平台内部模型编码';
comment on column afo_llm_model_catalog.display_name is '模型展示名称';
comment on column afo_llm_model_catalog.provider is '模型厂商';
comment on column afo_llm_model_catalog.supplier is '供应商，表示模型的提供来源';
comment on column afo_llm_model_catalog.litellm_model is 'LiteLLM 模型名称';
comment on column afo_llm_model_catalog.model_type is '模型类型';
comment on column afo_llm_model_catalog.context_window is '上下文窗口 token 数';
comment on column afo_llm_model_catalog.supports_stream is '是否支持流式输出（0否 1是）';
comment on column afo_llm_model_catalog.supports_tool is '是否支持工具调用（0否 1是）';
comment on column afo_llm_model_catalog.protocol is '协议类型: openai / anthropic';
comment on column afo_llm_model_catalog.api_key is 'API Key（AES-256/GCM 加密存储）';
comment on column afo_llm_model_catalog.api_base is '上游 API 端点地址';
comment on column afo_llm_model_catalog.supports_vision is '是否支持视觉（0否 1是）';
comment on column afo_llm_model_catalog.supports_parallel_function_calling is '是否支持并行函数调用（0否 1是）';
comment on column afo_llm_model_catalog.supports_reasoning is '是否支持推理（0否 1是）';
comment on column afo_llm_model_catalog.max_input_tokens is '最大输入 token 数';
comment on column afo_llm_model_catalog.max_output_tokens is '最大输出 token 数';
comment on column afo_llm_model_catalog.api_version is 'API 版本';
comment on column afo_llm_model_catalog.rate_limit_tpm is 'TPM 限制';
comment on column afo_llm_model_catalog.rate_limit_rpm is 'RPM 限制';
comment on column afo_llm_model_catalog.request_timeout is '请求超时（秒）';
comment on column afo_llm_model_catalog.model_tags is '模型标签（JSON 数组，如 ["vision","fast"]）';
comment on column afo_llm_model_catalog.routing_order is '路由优先级（越低越优先，L2 使用）';
comment on column afo_llm_model_catalog.sync_status is '同步状态（synced/pending/failed）';
comment on column afo_llm_model_catalog.status is '状态（0正常 1停用）';
comment on column afo_llm_model_catalog.create_dept is '创建部门';
comment on column afo_llm_model_catalog.create_by is '创建者';
comment on column afo_llm_model_catalog.create_time is '创建时间';
comment on column afo_llm_model_catalog.update_by is '更新者';
comment on column afo_llm_model_catalog.update_time is '更新时间';
comment on column afo_llm_model_catalog.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column afo_llm_model_catalog.remark is '备注';

create unique index if not exists uk_afo_llm_model_catalog_code on afo_llm_model_catalog (tenant_id, model_code) where del_flag = '0';
comment on index uk_afo_llm_model_catalog_code is '唯一约束：未删除数据中同一租户下模型编码唯一';
create index if not exists idx_afo_llm_model_catalog_type_status on afo_llm_model_catalog (tenant_id, model_type, status, del_flag);
comment on index idx_afo_llm_model_catalog_type_status is '查询场景：按模型类型和状态筛选可用模型';

-- 22. afo_llm_request_log
-- 来源: afo_llm_init.sql L620-L683
create table if not exists afo_llm_request_log
(
    request_log_id int8,
    tenant_id      varchar(20)   default '000000'::varchar,
    request_id     varchar(64)   not null,
    project_id     int8          default null,
    client_id      int8          default null,
    key_id         int8          default null,
    model_code     varchar(100)  default null::varchar,
    request_path   varchar(200)  default null::varchar,
    stream         char          default '0'::bpchar,
    request_status varchar(30)   default 'pending'::varchar,
    http_status    int4          default null,
    latency_ms     int8          default null,
    error_code     varchar(100)  default null::varchar,
    error_message  varchar(1000) default null::varchar,
    client_ip      varchar(128)  default null::varchar,
    user_agent     varchar(500)  default null::varchar,
    trace_id       varchar(100)  default null::varchar,
    usage_id       int8          default null,
    usage_raw      text          default '{}'::text,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    remark         varchar(500)  default null::varchar,
    constraint pk_afo_llm_request_log primary key (request_log_id)
);

comment on table afo_llm_request_log is 'LLM 请求日志表';
comment on column afo_llm_request_log.request_log_id is '请求日志ID';
comment on column afo_llm_request_log.tenant_id is '系统租户编号';
comment on column afo_llm_request_log.request_id is '请求ID';
comment on column afo_llm_request_log.project_id is '项目ID';
comment on column afo_llm_request_log.client_id is '应用客户端ID';
comment on column afo_llm_request_log.key_id is 'API Key ID';
comment on column afo_llm_request_log.model_code is '模型编码';
comment on column afo_llm_request_log.request_path is '请求路径';
comment on column afo_llm_request_log.stream is '是否流式请求（0否 1是）';
comment on column afo_llm_request_log.request_status is '请求状态';
comment on column afo_llm_request_log.http_status is 'HTTP 状态码';
comment on column afo_llm_request_log.latency_ms is '请求耗时毫秒';
comment on column afo_llm_request_log.error_code is '错误编码';
comment on column afo_llm_request_log.error_message is '错误信息';
comment on column afo_llm_request_log.client_ip is '客户端 IP';
comment on column afo_llm_request_log.user_agent is 'User-Agent';
comment on column afo_llm_request_log.trace_id is '链路追踪ID';
comment on column afo_llm_request_log.usage_id is '用量记录ID';
comment on column afo_llm_request_log.usage_raw is '模型返回的原始 usage JSON 文本';
comment on column afo_llm_request_log.create_dept is '创建部门';
comment on column afo_llm_request_log.create_by is '创建者';
comment on column afo_llm_request_log.create_time is '创建时间';
comment on column afo_llm_request_log.update_by is '更新者';
comment on column afo_llm_request_log.update_time is '更新时间';
comment on column afo_llm_request_log.remark is '备注';

create unique index if not exists uk_afo_llm_request_log_tenant_request on afo_llm_request_log (tenant_id, request_id);
comment on index uk_afo_llm_request_log_tenant_request is '唯一约束：同一租户下 requestId 唯一，便于链路追踪和幂等记账';
create index if not exists idx_afo_llm_request_log_client_time on afo_llm_request_log (tenant_id, client_id, create_time);
comment on index idx_afo_llm_request_log_client_time is '查询场景：按应用和时间分页查询调用日志';
create index if not exists idx_afo_llm_request_log_status_time on afo_llm_request_log (tenant_id, request_status, create_time);
comment on index idx_afo_llm_request_log_status_time is '查询场景：按请求状态和时间统计成功率、失败率';

-- ==========================================
-- Part 3: 路由/策略表（3 张）
-- ==========================================

-- 23. afo_model_access_policy
-- 来源: D:/project/WL/AI-FinOps/afo-modules/afo-strategy/src/main/resources/sql/postgres/V1__model_access_policy.sql L4-L21
CREATE TABLE IF NOT EXISTS afo_model_access_policy (
    policy_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) DEFAULT '000000',
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

-- 24. afo_whitelist_rules
-- 来源: D:/project/WL/AI-FinOps/script/sql/V2_0__gateway_tables.sql L6-L21
CREATE TABLE IF NOT EXISTS afo_whitelist_rules (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) DEFAULT '000000',
    match_type VARCHAR(20) NOT NULL,
    pattern VARCHAR(500) NOT NULL,
    remark VARCHAR(200),
    enabled CHAR(1) DEFAULT '0',
    dry_run CHAR(1) DEFAULT '0',
    hit_count BIGINT DEFAULT 0,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX IF NOT EXISTS idx_whitelist_tenant ON afo_whitelist_rules(tenant_id, del_flag);

COMMENT ON TABLE afo_whitelist_rules IS '网关白名单规则';
COMMENT ON COLUMN afo_whitelist_rules.match_type IS '匹配类型: EXACT/PREFIX/REGEX';
COMMENT ON COLUMN afo_whitelist_rules.dry_run IS '试运行模式: 0=正式 1=试运行';

-- 25. afo_routing_config_rule
-- 来源: D:/project/WL/AI-FinOps/afo-modules/afo-strategy/src/main/resources/sql/postgres/V5__routing_config_rule.sql L4-L26
CREATE TABLE IF NOT EXISTS afo_routing_config_rule (
    rule_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(20) DEFAULT '000000',
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
COMMENT ON COLUMN afo_routing_config_rule.match_config IS '匹配条件 JSON';
COMMENT ON COLUMN afo_routing_config_rule.action_type IS '路由动作：ORIGINAL_MODEL、TARGET_MODEL';
COMMENT ON COLUMN afo_routing_config_rule.action_config IS '路由动作配置 JSON';
COMMENT ON COLUMN afo_routing_config_rule.fallback_config IS '兜底策略 JSON';
COMMENT ON COLUMN afo_routing_config_rule.execution_mode IS '执行模式：ENFORCE 执行';
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

-- ==========================================
-- Part 4: 日志表（1 张）
-- ==========================================

-- 26. afo_route_decision_log
-- 来源: D:/project/WL/AI-FinOps/afo-modules/afo-logs/src/main/resources/sql/postgres/V2__route_decision_log.sql
-- 仅建表保证 GatewayStatisticsController JdbcTemplate 查询不报错
-- 写入端（RouteDecisionLogConsumer）已在 D3 删除，表中数据始终为空
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
    policy_id BIGINT,
    deny_layer VARCHAR(64),
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT NOW(),
    update_by BIGINT,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

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
COMMENT ON COLUMN afo_route_decision_log.policy_id IS '拒绝策略 ID';
COMMENT ON COLUMN afo_route_decision_log.deny_layer IS '拒绝层级，如 ENTERPRISE_MODEL_ACCESS';
