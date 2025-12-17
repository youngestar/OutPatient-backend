# OutPatient-backend

本仓库是一个基于微服务的门诊后端系统（多模块 Maven 项目），包含若干服务模块、公共模块、数据库初始化脚本以及 Docker 配置，适用于本地开发、容器化运行和 CI/CD 场景。

---

## 主要功能与目标

- 按服务拆分（注册、挂号、医疗、鉴权、管理、数据分析、网关等）。
- 支持本地通过 Maven 打包运行，也支持使用 Docker / docker-compose 部署。
- 包含数据库初始化脚本 `init.sql` 和用于启动依赖服务的脚本 `wait-for-services.sh`。

---

## 仓库模块（大致）

- `OutPatient-backend-admin` — 管理后台服务
- `OutPatient-backend-auth` — 认证/鉴权服务
- `OutPatient-backend-common` — 公共库/工具
- `OutPatient-backend-data-analysis` — 数据分析服务
- `OutPatient-backend-gateway` — API 网关
- `OutPatient-backend-medical` — 医疗相关服务
- `OutPatient-backend-model` — 领域模型（POJO、DTO 等）
- `OutPatient-backend-registration` — 挂号/注册服务
- `OutPatient-backend-service` — 业务聚合或其他服务

(上述为仓库中已辨识的模块；具体模块可以参见各子目录的 `pom.xml`)

---

## 目录结构（概览）

- `docker-compose.yml` — 一键启动所有依赖（数据库、服务等）的配置
- `init.sql` — 数据库初始化脚本
- `build-and-run.sh`, `stop.sh`, `wait-for-services.sh` — 启动/停止与依赖等待脚本（Unix 环境）
- 各模块子目录含 `pom.xml`、`src/`、`target/` 等
- `logs/` — 本地运行时日志输出（示例日志已保存）

---

## 前置环境（假设）

注：仓库未显式声明 JDK 版本，这里给出常见的可用配置：

- JDK 17 或更高（建议使用 JDK 17）
- Apache Maven 3.6+
- Docker & Docker Compose（若使用容器化部署）
- 在 Windows 上运行 shell 脚本需要 Git Bash/WSL/其他类 Unix 环境，或使用等效 PowerShell 命令

如果你的机器已经安装以上工具，即可继续下面步骤。

---

## 快速开始 — 本地构建（使用 Maven）

在仓库根目录运行：

```bash
mvn -T 1C clean package
```

该命令会构建所有模块并在各自的 `target/` 目录生成可运行的 jar 文件。

示例：单独运行 `registration` 服务（替换为对应模块名与 jar 文件名）：

```powershell
java -jar OutPatient-backend-registration/target/OutPatient-backend-registration-1.0-SNAPSHOT.jar
```

注意：某些服务在启动时依赖数据库或其他服务，请先确保依赖项可用（参见“数据库初始化”与 `wait-for-services.sh`）。

---

## 使用 Docker / docker-compose（推荐用于整体联调）

仓库根目录包含 `docker-compose.yml`，可以用来启动数据库和所有服务（或按需的部分服务）。

在根目录运行：

```powershell
docker-compose up --build
# 或后台运行
docker-compose up --build -d
```

- 若你在 Windows 上使用 `build-and-run.sh` 等脚本，这些脚本为 Unix shell 脚本，需要在 Git Bash 或 WSL 下执行；在 PowerShell 下直接运行 `.sh` 脚本会失败。

---

## 数据库初始化

仓库根目录包含 `init.sql`，用于初始化数据库表结构与基础数据（可在容器化数据库首次启动时执行，或手动在你的 DB 管理工具中执行）。

---

## 启动依赖等待脚本

部分模块下存在 `wait-for-services.sh`，用于在启动服务前轮询其它依赖（例如数据库/注册中心）是否就绪。如果通过容器编排启动，大多数情况 docker-compose 会处理依赖，但在本地单体运行时建议使用这些脚本或手动检查。

---

## 日志

已将项目默认配置修改为不再把运行时日志写入仓库 `logs/` 目录，默认仅输出到控制台（stdout）。这样可以避免在开发环境中产生大量日志文件。如果你需要保留或轮转日志，可按下面说明恢复文件输出或在部署时通过容器日志收集方案（例如 Docker 日志驱动、ELK、Fluentd 等）集中保存日志。

- 禁用文件日志（当前状态）：各模块的 `application.yml` 中已注释掉 `logging.file` / logback 的文件滚动策略，服务只在控制台输出日志。
- 恢复文件日志：在对应模块的 `application.yml` 中恢复 `logging.file.name` 或 `logging.file.path` 配置，并配置 `logback` 的 rolling policy（例如最大历史、最大大小等）。
- 删除历史日志：如果你确定不需要保留现有的 `logs/` 目录下的日志文件，可以手动删除该目录下的文件或在命令行中执行删除命令（注意备份重要日志）。

示例：恢复文件输出（注册服务示例）：

```yaml
logging:
  file:
    name: logs/registration-service.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      total-size-cap: 1GB
```

---

## 开发与测试

- 单元测试：使用 Maven 执行 `mvn -DskipTests=false test`（默认会在 `package` 时执行测试，若 `mvn package` 跳过测试请去掉 `-DskipTests`）
- 调试：在 IDE（如 IntelliJ IDEA）中以模块为单位导入 Maven 项目，设置合适的 Run/Debug 配置。

示例运行测试：

```bash
mvn test
```

---

## 常见问题与排查（快速提示）

- 端口占用：检查 `application.yml`/`application.properties` 中配置的端口，或用 `netstat`/`ss` 查找占用并释放。
- 数据库连接失败：确认 `init.sql` 已执行且 `docker-compose` 中的 DB 服务已经完全启动。
- 脚本在 Windows 下无法运行：在 Windows 上请使用 Git Bash 或 WSL 执行 `.sh` 脚本，或将脚本转换为 PowerShell 版本。
- 构建失败：查看 Maven 输出的错误堆栈，通常是依赖下载失败或单元测试异常。
