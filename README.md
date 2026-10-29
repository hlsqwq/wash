# 蜂窝盘模型云端化

把 Android 清洗设备 App 中写死的蜂窝盘模型参数（行数、每行管数、行首偏移、隐藏/堵塞孔位、横向隔断、中缝加宽、盘面图片）迁移到云端，由后台统一维护，Web 管理端可视化编辑，Android 端按云端数据通用渲染，不再为每个新型号新增一个 `类Model_xxx` / `类Fragment_xxx`。

仓库包含三部分：

| 目录 | 内容 | 技术栈 |
| --- | --- | --- |
| `src/`、`pom.xml`、`sql/`、`tools/` | 后端 REST API、MySQL 持久化、RustFS 图片存储、种子数据 | Spring Boot 4.0.8 / Java 21 / Hibernate / MySQL / AWS SDK v2（S3） |
| `ui/` | Web 管理端：列表、详情、新建、编辑、删除、图片上传与预览 | Vue 3 + TypeScript + Vite + Element Plus + Axios |
| `C:\code\android\code\wash`（独立仓库） | Android 设备端：Retrofit 拉取模型、通用渲染、版本缓存、离线回退 | Java 11 / AGP 9.3.2 / Glide / Retrofit |

---

## 1. 快速开始

### 1.1 环境要求

- JDK 21（后端）
- Maven（或使用仓库自带的 `mvnw`）
- MySQL 8.x，可用的 RustFS（S3 兼容）实例
- Node.js 18+（Web 管理端）
- Android Studio（含 JBR 25）或本机 JDK 25，用于编译 Android 工程

### 1.2 初始化数据库

```bash
mysql -h <mysql-host> -P 3306 -u<user> -p < sql/schema.sql
```

`schema.sql` 是幂等脚本：只创建缺失的库表，不删除、不清空已有数据。若之前存在 `media.backet` 拼写错误的旧库，再执行一次：

```bash
mysql -h <mysql-host> -P 3306 -u<user> -p wash < sql/migrate_media_bucket.sql
```

### 1.3 配置后端

所有敏感配置都通过环境变量或本地配置文件注入，**不要写进源码、示例配置或前端**。

方式一：环境变量（推荐）

```bash
export DB_URL='jdbc:mysql://<mysql-host>:3306/wash?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD='<db-password>'
export RUSTFS_ENDPOINT='http://<rustfs-host>:9000'
export RUSTFS_ACCESS_KEY='<access-key>'
export RUSTFS_SECRET_KEY='<secret-key>'
export RUSTFS_BUCKET=wash-model-images
```

方式二：本地配置文件

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 填写真实值后使用 --spring.profiles.active=local 启动
```

`application-local.yml` 已加入 `.gitignore`，不会被提交；`application-local.yml.example` 和 `.env.example` 只含占位值。

可用配置项：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | 开发库地址 | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / 空 | 数据库账号密码，密码只走环境变量 |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `RUSTFS_ENDPOINT` | 开发实例地址 | S3 端点（path-style） |
| `RUSTFS_ACCESS_KEY` / `RUSTFS_SECRET_KEY` | 空 | RustFS 凭证，只存于服务端 |
| `RUSTFS_BUCKET` | `wash-model-images` | 图片桶名 |
| `RUSTFS_REGION` | `us-east-1` | S3 region |
| `RUSTFS_AUTO_CREATE_BUCKET` | `true` | 启动时自动建桶 |
| `APP_PUBLIC_BASE_URL` | 空 | 图片绝对地址前缀；为空时按请求 Host 生成 |
| `APP_SEED_ENABLED` | `true` | 启动时导入/补齐内置模型 |

### 1.4 启动后端

```bash
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

启动日志中会显示种子数据结果（例如 `导入模型 e107：28 行 / 总管数 374`）。种子导入是幂等的：只插入不存在的 `code`，不覆盖管理端改过的数据；图片上传失败会如实跳过并记录，不会写入伪造地址。

### 1.5 启动 Web 管理端

```bash
npm --prefix ui install
npm --prefix ui run dev     # 开发调试，默认 http://localhost:5173
npm --prefix ui run build   # 类型检查 + 生产构建，产物在 ui/dist
```

开发模式下 Vite 将 `/api` 代理到 `VITE_DEV_PROXY_TARGET`（默认 `http://localhost:8080`），因此无需后端配置 CORS。所有 `VITE_*` 变量都会被打进前端产物，只能放公开地址，禁止放凭证。

### 1.6 编译 Android 工程

Android 工程在独立目录 `C:\code\android\code\wash`：

```bash
cd C:\code\android\code\wash
.\gradlew.bat :app:assembleDebug
```

模型服务地址通过 `BuildConfig.API_BASE_URL` 注入：

- 默认 `http://10.0.2.2:8080/`（模拟器访问宿主机）
- 真机联调时用电脑局域网 IP 覆盖：

```bash
.\gradlew.bat :app:assembleDebug -Pwash.apiBaseUrl=http://192.168.1.100:8080/
```

注意：

- 开发环境是 HTTP，清单中已开启 `android:usesCleartextTraffic="true"`；生产环境请改为 HTTPS 并关闭明文流量。
- AGP 9.3.2 需要 JDK 25 工具链。若命令行构建报 `Unable to download toolchain matching {languageVersion=25}`，请用 Android Studio 自带的 JBR 运行，例如：
  `.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr" "-Porg.gradle.java.installations.paths=C:\Program Files\Android\Android Studio\jbr" :app:assembleDebug`
- 若本机 `~/.gradle/gradle.properties` 配置的代理不可用，Gradle 下载依赖会失败；检查 `systemProp.http(s).proxyHost/Port` 或临时用 init script 清除代理后再构建。

---

## 2. 数据模型与内置模型

`model` 表保存模型参数，`media` 表保存图片元数据（图片本体在 RustFS）：

- `row_len`：每行管数 JSON 数组
- `row_offset`：每行首点偏移 JSON 数组，单位为管间距/行间距（可为小数）
- `hide_holes` / `block_holes`：隐藏/堵塞孔位，标签形如 `c_<行>_<列>`（0 基）
- `is_horizon_partition` + `horizon_partition`：横向隔断，`afterRow` 为 0 基行号，`heightDp` 为插入高度
- `wide_center_gap`：盘面中间加宽
- `version`：每次更新递增，Android 据此判断是否需要刷新缓存
- `enabled`：逻辑删除标记，删除后不再出现在列表与详情中

种子数据来自 Android 原始模型类，经 `tools/extract_models.py` 提取并人工核对特例：

| code | 行数 | 总管数 | 特例 |
| --- | --- | --- | --- |
| e107 | 28 | 374 | 隐藏 8 个孔、堵塞 2 个孔、第 13 行后插入 22dp 隔断、中缝加宽 |
| e221 | 50 | 1762 | 第 24 行后插入 16dp 隔断 |
| e222 | 23 | 371 | 无 |
| e302 | 13 | 127 | 无 |

如需重新生成种子数据：

```bash
python tools/extract_models.py --android-src C:\code\android\code\wash\app\src\main\java\com\hls\wash \
    --out src/main/resources/seed/models.json
```

---

## 3. API

统一前缀 `/api/v1`，统一响应结构：

```json
{ "code": 0, "message": "ok", "data": { } }
```

`code=0` 表示成功；失败时 HTTP 状态码与业务码同时返回。服务端重新计算 `rowCount` / `tubeCount`，客户端传入的值会被忽略；更新成功后 `version` 自动递增。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/models` | 模型列表（不含行数组） |
| GET | `/api/v1/models/{id}` | 模型详情（含 `rows`、`hideCells`、`blockCells`、`separators`） |
| POST | `/api/v1/models` | 新建模型 |
| PUT | `/api/v1/models/{id}` | 全量更新模型 |
| DELETE | `/api/v1/models/{id}` | 逻辑删除模型 |
| POST | `/api/v1/files/images` | 上传图片，multipart 字段名 `file` |
| GET | `/api/v1/files/images/{id}` | 后端代理读取图片（不暴露 RustFS 地址与凭证） |

### 3.1 新建模型示例

```bash
curl -X POST http://localhost:8080/api/v1/models \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "e999",
    "name": "联调测试模型",
    "description": "仅用于接口联调",
    "imageUrl": "",
    "dx": 1730,
    "dy": 1488,
    "wideCenterGap": false,
    "rows": [
      {"rowIndex": 0, "tubeCount": 4, "offsetX": 3.5, "offsetY": 0.5},
      {"rowIndex": 1, "tubeCount": 3, "offsetX": 4.0, "offsetY": 1.5}
    ],
    "hideCells": ["c_0_1"],
    "blockCells": [],
    "separators": [{"afterRow": 0, "heightDp": 16}]
  }'
```

返回的 `data` 为详情结构，`rowCount=2`、`tubeCount=7`、`version=1`、`description` 由客户端提供或为空。

校验规则（不满足返回 `400/40001`）：

- `code`：字母、数字、下划线、中划线，1–64 字符，全局唯一（重复返回 `409/40901`）
- `rows`：非空，`rowIndex` 必须从 0 开始连续；`tubeCount > 0`；最多 300 行
- `hideCells` / `blockCells`：`c_行_列` 格式，行列必须在模型范围内
- `separators[].afterRow`：必须在行范围内
- 删除的模型中不存在时返回 `404/40401`

### 3.2 图片上传

```bash
curl -X POST http://localhost:8080/api/v1/files/images \
  -F 'file=@model_e107.png'
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": "<内容 MD5>",
    "url": "http://<backend-host>/api/v1/files/images/<内容 MD5>",
    "filename": "model_e107.png"
  }
}
```

- 允许 PNG / JPEG / WebP；按文件头 + ImageIO 双重校验，SVG 等返回 `415/41501`
- 单文件上限 5MB，超出返回 `413/41301`
- 相同内容按 MD5 去重，不重复占用存储
- 新建/更新模型时把返回的 `url` 作为 `imageUrl` 传入即可关联

### 3.3 错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40001 | 400 | 参数校验失败 |
| 40401 | 404 | 模型/图片不存在 |
| 40901 | 409 | 模型编码重复 |
| 41501 | 415 | 不支持的图片类型 |
| 41301 | 413 | 图片超过 5MB |
| 50000 | 500 | 服务器内部错误（不返回堆栈细节） |

---

## 4. Web 管理端

- 模型列表：搜索、行数/管数概览、图片、版本、更新时间
- 详情：统计卡片、参数、盘面示意（隐藏/堵塞/隔断/中缝加宽）、逐行参数表
- 新建/编辑：逐行编辑、批量生成行、隐藏/堵塞孔位、横向隔断、客户端单元格校验
- 删除：二次确认，后端逻辑删除
- 图片：上传（5MB、PNG/JPEG/WebP）、预览、移除

图片始终通过后端 `/api/v1/files/images/{id}` 读取，前端不接触 RustFS 地址或凭证。

---

## 5. Android 端

新增 `com.hls.wash.network` 包：

- `ApiClient` / `ApiService`：Retrofit + Gson，超时 5/20/20 秒，基础地址来自 `BuildConfig.API_BASE_URL`
- `ModelRepository`：列表与详情缓存（`SharedPreferences: model_cloud_cache`）
  - 列表：启动时强制刷新；打开选择框时 5 分钟内直接用缓存
  - 详情：按 `code` 缓存并记录 `version`，摘要版本不比缓存新时直接用缓存，服务端 `version` 递增后自动重新拉取
  - 网络失败：先回落缓存，再回落四个本地模型
- `类Model_Dynamic`：按云端 `rows` 构造行数、每行管数、行首偏移
- `类Fragment_Dynamic`：按云端 `separators` 插入隔断、按 `hideCells`/`blockCells` 处理孔位、按 `wideCenterGap` 设置中缝；复用原有蓝牙、ESP8266、清洗分组与标定逻辑
- `ModelAdapter`：云端图片用 Glide 加载后端代理地址，失败时显示本地占位图

启动流程：先构造四个本地模型作为兜底 → 强制刷新云端列表 → 自动加载 `e222`（不存在则第一个）详情并替换显示。用户在选择框中选择云端模型时异步加载详情，按版本缓存；失败时提示并回退本地模型。全部模型（包括原有四个型号）都走同一套动态渲染，不再新增型号专用类。

---

## 6. 测试与验证结果

后端测试（H2 MySQL 兼容模式、种子关闭、存储服务 Mock）：

```bash
./mvnw test
```

| 测试类 | 数量 | 覆盖内容 |
| --- | --- | --- |
| `ModelApiTest` | 11 | 空列表、创建并重算行数/管数、详情 404、编码重复 409、行参数校验、行号不连续、孔位越界、编码非法、更新后 version=2 且行数据替换、逻辑删除、隔断越界 |
| `FileApiTest` | 6 | 上传 PNG 并代理读取、MD5 去重只写一次存储、SVG 拒绝、超 5MB 拒绝、模型关联图片、未知图片 404 |
| `WashApplicationTests` | 1 | 上下文加载 |

结果：`Tests run: 18, Failures: 0, Errors: 0`。

> 如果本机 Maven 配置了自定义本地仓库/镜像（例如 `conf/settings.xml` 中的 `alimaven`），
> `mvnw` 仍会使用默认的 `~/.m2`，可能出现重复下载或离线失败；此时改用已配置的 `mvn -o test`，
> 或显式传入 `-Dmaven.repo.local=<本地仓库路径>`。

真实环境联调（开发 MySQL + RustFS）：

- 种子导入四个模型，行数/管数与上表一致
- `GET /api/v1/models` 返回 4 个模型；e107 详情为 28 行 / 374 管 / 8 隐藏 / 2 堵塞 / `afterRow=13,heightDp=22` / `wideCenterGap=true`
- 图片代理返回 `200 image/png`
- 临时模型 `e999` 完成创建（2 行/7 管，v1）→ 更新（1 行/5 管，v2）→ 删除，列表恢复四个内置模型

前端：`npm --prefix ui run build`（`vue-tsc --noEmit` + `vite build`）通过。

Android：`.\gradlew.bat :app:assembleDebug` 构建通过，产物 `app/build/outputs/apk/debug/app-debug.apk`。本环境只做了编译验证，未在真机/模拟器上运行。

---

## 7. 安全说明

- 数据库密码、RustFS Access Key / Secret Key 只通过环境变量或已被忽略的 `application-local.yml` 注入
- 示例文件（`.env.example`、`application-local.yml.example`、`ui/.env.example`）只包含占位值
- 后端不向客户端返回数据库密码、存储凭证或异常堆栈
- 图片由后端代理输出，客户端（Web/Android）永远拿不到 RustFS 地址与密钥
- 所有 `VITE_*` 变量都可能公开，前端只使用后端 API 地址
- 本地 `application-local.yml` 与 UI 的 `node_modules/`、`dist/`、`.env` 已被 `.gitignore` 排除
- 开发环境使用 HTTP 明文；生产部署请启用 HTTPS，并把 `usesCleartextTraffic` 关闭

---

## 8. 常见问题

**列表为空 / 只有本地模型**

- 检查后端是否启动、`GET /api/v1/models` 是否有数据
- Android 真机需用电脑局域网 IP 构建（`-Pwash.apiBaseUrl=...`），并确保手机与电脑同网段
- 图片地址由请求 Host 生成；若部署在反向代理后，请配置 `APP_PUBLIC_BASE_URL`，否则可能生成内网地址

**图片 404**

- 确认 RustFS 桶存在（或 `RUSTFS_AUTO_CREATE_BUCKET=true`）
- 确认模型关联的 `media` 记录存在，且图片已经通过上传接口写入
- 确认 `APP_PUBLIC_BASE_URL` 与客户端访问地址一致

**前端跨域**

- 开发时使用 Vite 代理；生产环境由同域反向代理转发 `/api`，或按部署需要调整后端 CORS 配置

**种子数据没有导入**

- `APP_SEED_ENABLED` 是否为 `true`
- 数据库中是否已存在同 `code` 的模型（已存在时只补图片，不覆盖）
- 查看启动日志中图片上传失败的原因（RustFS 连接/凭证）

**Android 构建报 JDK 25 工具链错误**

- 使用 Android Studio 的 JBR 25（见 1.6 节命令），不要依赖自动下载工具链
- 若依赖下载失败，检查 Gradle 代理配置是否指向了未启动的本地代理

---

## 9. 已知限制

- 接口未做鉴权，仅适用于受控内网；对外部署需在网关层增加认证
- 删除为逻辑删除，记录仍保留在 `model` 表中
- 图片单文件上限 5MB，只支持 PNG/JPEG/WebP
- 时间字段统一按 UTC 存储与返回，展示层按本地时区格式化
- 盘面示意图为参数化示意（按行管数/孔位/隔断绘制），不是盘面照片
"# wash" 
