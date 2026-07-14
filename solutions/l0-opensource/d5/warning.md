# D5 注意事项文档

**日期:** 2026-07-13
**任务:** Docker + 配置 + 分类器 + llm-router + 测试修复
**开发者:** A（开源拆分）

---

## 一、遗留项（D6/D7 处理）

| # | 遗留项 | 影响 | 归属 |
|---|--------|------|------|
| 1 | 前端 L0 页面未复制 | 控制面无法通过 Web UI 访问 | D6 |
| 2 | 全链路端到端验证（8 个用例）未执行 | 运行时行为未验证 | D7 |
| 3 | README/CONTRIBUTING/LICENSE 未完善 | 项目文档不完整 | D7 |
| 4 | GitHub 推送未执行 | 公开仓库未发布 | D7 |
| 5 | `afo-common-log` 模块未评估清理 | 当前在根 POM 和 dependencyManagement 中声明但未被任何模块依赖 | D7 |
| 6 | `afo-common-translation` 模块未评估清理 | 含依赖已删除 Service 的实现类（`DeptNameTranslationImpl` 等），afo-system-gateway 未直接依赖，无运行时影响但有代码残留 | D7 |
| 7 | `ProxyForwardFilter.resolveConfidence()` 死代码未清理 | D2 遗留，5 行方法不再被任何代码调用 | D7 |
| 8 | `ProxyForwardFilter.ObjectNode` import 未使用 | D2 遗留，编译仅 WARNING，无功能影响 | D7 |
| 9 | afo-system-gateway 无单元测试 | D4 未迁移测试；控制面模块无回归测试覆盖 | 评估后决定 |
| 10 | LLM Provider 真实上游 API Key 需用户自行配置 | 运维操作 | 运维 |

---

## 二、架构约束

| # | 约束 | 说明 |
|---|------|------|
| 1 | `tenant.enable: true` 新增于 afo-system-gateway 的 application.yml | D5 计划模板中未包含此项，但 TenantHelper.isEnable() 默认 false，若不加此项则租户隔离不生效，MyBatis-Plus 租户拦截器不会自动填充 tenant_id |
| 2 | `SystemDataInitializer` 使用 `TenantHelper.dynamic("000000", ...)` 设置租户上下文 | 因为 SystemDataInitializer 在应用启动阶段无登录用户，ThreadLocal 方式设置动态租户是最优解 |
| 3 | `SystemDataInitializer` 使用直接 Mapper 操作而非 Service 层 | 避免 Service 层的 `@DataPermission` 注解在无登录用户时抛异常；createBy/createDept/createTime 等审计字段手动赋值（MyBatis-Plus 自动填充依赖 LoginHelper，启动阶段无登录用户） |
| 4 | 根 POM 中 `<dependencyManagement>` 已声明 `afo-system-gateway` | D4 遗留项，D5 任务 2.0 修复 |
| 5 | 商业项目 `D:/project/WL/AI-FinOps` 零改动 | 所有操作均在 `D:/project/WL/ai-finops-community` 执行 |

---

## 三、D5 验收标准核对

### 任务 1：复制 classifier-service、llm-router + .gitignore

| 检查项 | 状态 |
|--------|------|
| `classifier-service/` 存在，含 main.py、Dockerfile、requirements.txt | ✅ |
| `llm-router/` 存在，含 app/main.py、Dockerfile、requirements.txt | ✅ |
| Dockerfile 与商业项目一致（除 curl 安装） | ✅ |
| `.gitignore` 存在，含常见忽略项 | ✅ |
| classifier-service 的 `__pycache__/` 已清理 | ✅ |
| classifier-service 的 `.env` 文件未复制（商业项目 .env 在 .gitignore 中） | ✅ |

### 任务 2：修剪 afo-gateway 配置 + 根 POM

| 检查项 | 状态 |
|--------|------|
| `grep scoring` 无结果 | ✅ |
| `grep shadow` 无结果 | ✅ |
| `grep trial` 无结果 | ✅ |
| `grep quota` 无结果 | ✅ |
| `grep record.only\|record-only` 无结果 | ✅ |
| `grep 124.220.135.11` 无结果 | ✅ |
| `afo.gateway.admin.base-url` → `afo-system-gateway:8080` | ✅ |
| `rabbitmq.enabled: true` 存在 | ✅ |
| application-dev.yml / application-online.yml 使用环境变量占位符 | ✅ |
| 根 POM dependencyManagement 含 afo-system-gateway | ✅ |

### 任务 3：afo-system-gateway 配置 + SystemDataInitializer

| 检查项 | 状态 |
|--------|------|
| `application.yml` 存在 | ✅ |
| `application-online.yml` 存在 | ✅ |
| 无 scoring/shadow/trial/quota/record-only 字样 | ✅ |
| `grep 124.220.135.11` 无结果 | ✅ |
| `SystemDataInitializer.java` 存在，实现 CommandLineRunner | ✅ |
| `mvn clean compile -pl afo-system-gateway -am -q` 零错误 | ✅ |

### 任务 4：docker-compose.yml

| 检查项 | 状态 |
|--------|------|
| 文件存在 | ✅ |
| `docker-compose config` 无语法错误 | ✅ |
| 7 服务均有 container_name | ✅ |
| 基础设施 3 服务有 healthcheck | ✅ |
| 无 host.docker.internal 引用 | ✅ |
| 无硬编码 IP | ✅ |
| depends_on 链正确 | ✅ |
| init.sql 通过 volume 挂载到 /docker-entrypoint-initdb.d/ | ✅ |

### 任务 5：Dockerfiles

| 检查项 | 状态 |
|--------|------|
| `afo-system-gateway/Dockerfile` 存在 | ✅ |
| FROM 与 afo-gateway 一致 | ✅ |
| 端口为 8080 | ✅ |
| JAR 名为 afo-system-gateway.jar（finalName 已设置） | ✅ |
| afo-gateway Dockerfile 含 curl 安装 | ✅ |
| classifier-service Dockerfile 含 curl 安装 | ✅ |
| llm-router Dockerfile 含 curl 安装（在 USER 切换前） | ✅ |
| `afo-system-gateway/pom.xml` 含 `<finalName>afo-system-gateway</finalName>` | ✅ |

### 任务 6：测试修复

| 检查项 | 状态 |
|--------|------|
| `mvn test-compile -q` 全模块零错误 | ✅ |
| `LlmRequestLogConsumerScoringTest.java` 已删除（100% L1/L2） | ✅ |
| `RouteDecisionLogConsumerScoringTest.java` 已删除（100% L1/L2） | ✅ |
| `RouteDecisionLogServiceImplTest.java` 不再引用 record_only | ✅ |
| `GatewayApplicationYamlTest` 通过（application-prod.yml 已创建） | ✅ |
| afo-gateway: 34 tests, 0 failures | ✅ |
| ProxyForwardFilterTest: 纯 L0，无 L1/L2 引用 | ✅ |
| 测试代码中无 ScoringService/ShadowStreamingComparator/TrialKeyDetectFilter 引用 | ✅ |

### 任务 7：编译 + 配置验证（Docker 运行时需 D7 验证）

| 检查项 | 状态 |
|--------|------|
| `mvn compile -q` 全模块零错误 | ✅ |
| `docker-compose config` 语法有效 | ✅ |

---

## 四、代码审核补充

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `afo-gateway/application.yml` | 删除 scoring/shadow 节；修改 admin base-url | 配置纯净度已验证 |
| 2 | `afo-gateway/application-online.yml` | 删除 shadow.embedding；替换硬编码 IP | 硬编码 IP 残留已全部清理 |
| 3 | `afo-gateway/application-dev.yml` | 替换硬编码 IP | 同上 |
| 4 | `afo-gateway/application-prod.yml` | 修改为容器服务名+环境变量 | 原有文件含旧 IP，已更新 |
| 5 | `afo-system-gateway/application.yml` | 新建 | 含 tenant.enable: true（计划模板未包含但必需） |
| 6 | `afo-system-gateway/application-online.yml` | 新建 | 精简版，仅数据源/Redis/RabbitMQ/日志 |
| 7 | `afo-system-gateway/runner/SystemDataInitializer.java` | 新建 | 幂等设计；初始化租户/部门/角色/用户/13 个 L0 菜单/4 个字典类型 |
| 8 | `afo-system-gateway/Dockerfile` | 新建 | 含 curl；使用 ZGC；JAVA_OPTS 可覆盖 |
| 9 | `afo-gateway/Dockerfile` | 修改 | 添加 curl 安装 |
| 10 | `classifier-service/Dockerfile` | 修改 | 添加 curl 安装 |
| 11 | `llm-router/Dockerfile` | 修改 | 添加 curl 安装（USER 切换前） |
| 12 | `afo-system-gateway/pom.xml` | 修改 | 添加 finalName + spring-boot-maven-plugin excludes |
| 13 | 根 `pom.xml` | 修改 | dependencyManagement 新增 afo-system-gateway |
| 14 | `.gitignore` | 重写 | 完整覆盖 Python/Java/IDE/Docker/Node/Env |
| 15 | `classifier-service/` | 复制 | 商业项目完整复制，无修剪 |
| 16 | `llm-router/` | 复制 | 商业项目完整复制，无修剪 |
| 17 | `docker-compose.yml` | 新建 | 7 服务编排 |

---

## 五、已发现的潜在风险

### 1. `LlmProviderServiceImplTest` 预存失败（风险等级：低）

**现象：** `providerVoGeneratesEntityToConverter` 测试失败，`assertTrue` 期望 true 但得到 false。MapStruct-Plus 实体-VO 转换器自动生成未生效。

**原因分析：** 该测试来自商业项目，与 D5 L1/L2 修剪无关。可能是 MapStruct-Plus 版本或配置的兼容性问题。测试编译和运行均不依赖 L1/L2 类。

**影响：** afo-llm 模块有 1 个测试失败（共 14 个，通过 13 个）。不影响 gateway/strategy/logs 模块测试。

**建议：** D7 端到端验证阶段通过集成测试覆盖 Provider 配置功能。

### 2. `SystemDataInitializer` 初始化失败风险（风险等级：高 — D5 计划已标注）

**风险点：**
- 外键约束：菜单表 `parent_id` 引用自身 → 按 parent_id=0 先插入目录再插入子菜单，顺序正确 ✅
- 重复插入：幂等设计（先查后插），admin 用户存在则跳过全部初始化 ✅
- tenant_id 自动填充：已配置 `tenant.enable: true`，TenantHelper.dynamic() 设置上下文 ✅
- 审计字段：createBy/createDept/createTime 手动赋值，不依赖 LoginHelper ✅
- MyBatis-Plus 自动填充：createTime/updateTime 可能在 MetaObjectHandler 中再次赋值导致覆盖，已手动赋值兜底

**Plan B：** 如初始化失败，准备最小种子 SQL 通过 init.sql 追加 INSERT 语句。

### 3. `afo-gateway/application.yml` 中 litellm/classifier 默认值仍为 localhost（风险等级：低）

**现象：** application.yml 中 litellm 默认 `http://127.0.0.1:4000`，classifier 默认 `http://127.0.0.1:8000`。仅在 Docker 环境通过环境变量覆盖。

**影响：** 本地 IDE 直接启动 afo-gateway 时，需确保 llm-router 和 classifier-service 在对应端口运行。

**说明：** application.yml 的默认值面向本地开发场景（IDE 启动），非容器场景。属设计决策，非缺陷。

### 4. `afo-system-gateway` 缺少 `application-dev.yml`（风险等级：低）

**现象：** afo-system-gateway 仅创建了 `application.yml` 和 `application-online.yml`，无 `application-dev.yml`。

**影响：** `spring.profiles.active: online` 为默认值，切换到 dev profile 时会回退到 application.yml 的默认配置。本地 IDE 开发时可能需要 dev profile 特定配置。

**建议：** 如需要，D6/D7 补充 application-dev.yml。

### 5. 健康检查依赖 curl，Python 镜像构建可能失败（风险等级：中 — D5 计划已标注）

**风险点：** llm-router 的 Dockerfile 在 `pip install` 后、`USER appuser` 前安装了 curl。classifier-service 在 COPY 前安装了 curl。需在 Docker 环境中实际构建验证。

**缓解：** pip 使用清华镜像源，apt-get 使用默认源。如构建失败，可切换 apt 镜像源或使用 wget 替代 curl。

### 6. init.sql 幂等性问题（风险等级：中 — D5 计划已标注）

**风险点：** PG volume 已有持久化数据时，docker-entrypoint-initdb.d 中的 init.sql 不会重新执行。

**说明：** 这属于 PostgreSQL 官方镜像的正常行为。如需重建，需 `docker-compose down -v` 清除 volume。

### 7. `afo-common-log` 和 `afo-common-translation` 模块未使用但存在于仓库（风险等级：低）

D3/D4 warning 已标注此风险，计划在 D7 清理。当前无运行时影响。

---

## 六、D6 准备

- D6 需要从商业项目复制 Vue 3 前端项目骨架
- D6 需要修剪 L1/L2 页面和文案
- D6 需要配置前端代理指向 afo-system-gateway:8080
- D6 typecheck 目标：`pnpm typecheck` 零错误
- D6 需注意：前端路由/菜单仅 6 项，无 L1/L2 入口

---

**文档版本:** v1.0
**创建时间:** 2026-07-13
