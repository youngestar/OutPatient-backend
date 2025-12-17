# OutPatient-backend 项目结构图（更细版，仿 BetterGenshinImpact 风格）
贡献者：基于仓库源码自动生成

### 分析结果
```
OutPatient-backend/
├── pom.xml: 父级 Maven 聚合 POM，定义模块、依赖版本和构建插件，负责统一构建与发布流程（例如 mvn -T 1C clean package）。
├── docker-compose.yml: 容器编排文件，定义数据库、Redis、RabbitMQ、MinIO、以及各微服务的容器、端口与环境变量，便于一键联调环境。
├── init.sql: 数据库初始化脚本，包含建表语句与基础种子数据（用户/角色/示例数据），用于首次部署和测试环境初始化。
├── build-and-run.sh: Unix 下的一键构建并运行脚本（通常执行 mvn package && docker-compose up），适用于 Bash/WSL 环境。
├── stop.sh: 停止/清理脚本，用于停止 docker-compose 或本地进程并执行清理动作。
├── wait-for-services.sh: 轮询依赖服务（例如 MySQL/Redis/Nacos）是否就绪的脚本，常用于容器化启动顺序控制。
├── README.md: 项目说明与运行指南，包含构建、运行、日志策略和常见问题；已更新说明 registration 模块不再向仓库 logs 写入文件日志。
├── logs/: 本地历史日志目录（包含 registration-service.log* 等历史文件），注意：已禁用 registration 模块的新文件写入，是否删除历史文件由你决定。

# OutPatient-backend-common/
├── src/main/java/com/std/cuit/common/config/CommonGlobalConfig.java: 通用模块全局配置，定义 env、timezone、charset、日期格式与全局开关（如 requestLog、sqlLog），被各模块以 @ConfigurationProperties 注入使用。
├── src/main/java/com/std/cuit/common/common/BaseResponse.java: 标准化 API 响应体的 POJO，用于统一接口返回结构（code/message/data）。
├── src/main/java/com/std/cuit/common/common/ResultUtils.java: 构造统一响应的工具方法（成功/失败/带数据/带错误码等），便于 Controller 统一返回格式。
├── src/main/java/com/std/cuit/common/exception/BusinessException.java: 自定义业务异常类型，包含错误码与描述，用于在 Service 层抛出并由全局处理器转换为 HTTP 响应。
├── src/main/java/com/std/cuit/common/exception/GlobalExceptionHandler.java: 全局异常处理器（@ControllerAdvice），捕获各种异常并返回标准化的 BaseResponse，包含日志记录与错误码映射。

# OutPatient-backend-model/
├── src/main/java/com/std/cuit/model/entity/Appointment.java: 预约实体映射（MyBatis-Plus 注解），定义 appointment_id、patientId、doctorId、scheduleId、appointmentDate、timeSlot、isRevisit、status 等字段，包含插入/更新时间自动填充配置。
├── src/main/java/com/std/cuit/model/entity/User.java: 用户实体，持久化用户基本信息（供鉴权与业务模块使用）。
├── src/main/java/com/std/cuit/model/DTO/UserRegisterRequest.java: 用户注册请求 DTO，包含前端提交的注册字段（如 username/password/email 等）用于参数校验与映射。
├── src/main/java/com/std/cuit/model/VO/AppointmentVO.java: 视图对象，用于组合返回预约/挂号详情给前端（含医生/科室/时间信息）。
├── src/main/java/com/std/cuit/model/query/ScheduleQuery.java: 排班查询参数封装类，便于 Controller 收集查询条件并传递到 Service/Mapper 层。

# OutPatient-backend-service/
├── src/main/java/com/std/cuit/service/mapper/AppointmentMapper.java: MyBatis-Plus 的 Mapper 接口，定义数据库访问的 CRUD 与自定义 SQL（预约相关查询与统计）。
├── src/main/java/com/std/cuit/service/service/AppointmentService.java: 预约服务接口，定义创建、取消、查询、医生/患者视图转换等业务方法的合同（如 createAppointment, cancelAppointment, getAppointmentDetail 等）。
├── src/main/java/com/std/cuit/service/service/serviceImpl/AppointmentServiceImpl.java: 预约服务实现，包含事务处理、并发检查、可重入/锁机制（可能使用 Redisson）以及与 Mapper 的持久化交互。
├── src/main/java/com/std/cuit/service/utils/minio/MinioUtils.java: MinIO 操作封装，提供上传/下载/签名 URL 生成的便捷方法，供多个模块复用。
├── src/main/java/com/std/cuit/service/utils/redis/RedissonService.java: Redisson 封装（分布式锁、延时队列等），用于解决并发下的排班/预约冲突问题。

# OutPatient-backend-registration/
├── src/main/java/com/std/cuit/registration/RegistrationApplication.java: 注册/挂号模块的 Spring Boot 启动类，加载模块配置并启动 Tomcat/Netty（取决于 Spring Boot 配置）。
├── src/main/java/com/std/cuit/registration/controller/RegistrationController.java: 提供用户注册、用户信息查询等接口（负责参数校验、调用 Service 并包装响应）。
├── src/main/java/com/std/cuit/registration/controller/AppointmentController.java: 预约相关的 REST 接口（创建预约、取消预约、查询患者预约列表等），将请求转换为 Service 调用并返回 VO。
├── src/main/java/com/std/cuit/registration/config/RegistrationGlobalConfig.java: 模块级全局配置（AI、预约策略、SSE、缓存、security），通过 @ConfigurationProperties(prefix = "registration.global") 注入并暴露给业务组件。
├── src/main/java/com/std/cuit/registration/config/RedisConfig.java: Redis 客户端配置（Lettuce 或 Jedis），包含连接信息、序列化策略与超时配置，用于缓存与会话。
├── src/main/java/com/std/cuit/registration/config/RedissonConfig.java: Redisson 客户端配置，用于分布式锁与延迟队列。
├── src/main/java/com/std/cuit/registration/config/RabbitMQConfig.java: RabbitMQ 连接与队列/交换机声明，支持异步任务（例如消息通知/邮件/日志处理）。
├── src/main/java/com/std/cuit/registration/config/MinioConfig.java: MinIO 客户端配置（endpoint/accessKey/secretKey/bucket），用于存储用户头像与附件。
├── src/main/java/com/std/cuit/registration/filter/TokenRefreshFilter.java: Servlet/Filter 实现，用于在请求中检测 token 并执行自动刷新以延长会话有效期（透传或返回刷新后的 token）。
├── src/main/java/com/std/cuit/registration/filter/SaTokenHeaderFilter.java: 处理 Sa-Token 相关头的过滤器，用于统一从请求头解析 token 并注入到上下文。
├── src/main/resources/application.yml: 注册模块配置（server.port:8206、datasource、redis、minio、sa-token、logging 等），已注释掉 `logging.file.name` 与 logback rollingpolicy，默认仅 console 输出。

# OutPatient-backend-gateway/
├── src/main/java/com/std/cuit/gateway/GatewayApplication.java: 网关模块启动类，负责引导 Spring Cloud Gateway 应用环境。
├── src/main/java/com/std/cuit/gateway/config/GatewayConfig.java: 路由规则配置类，定义服务转发规则、前缀剥离与全局过滤器。
├── src/main/java/com/std/cuit/gateway/config/SaTokenReactorConfig.java: WebFlux 版本的 Sa-Token 集成，定义白名单路径、鉴权逻辑与角色校验（如 /api/admin/** -> admin），并在认证通过后尝试刷新 token 活跃时间。
├── src/main/java/com/std/cuit/gateway/config/MyWebSocketHandler.java: 网关层的 WebSocket 处理器，负责代理/转发 WebSocket 请求到后端服务或处理会话。
├── src/main/java/com/std/cuit/gateway/controller/HealthController.java: 自定义健康检查端点（可用于容器编排中的就绪/存活探针补充）。
├── src/main/resources/application.yml: 网关配置（路由、服务发现、限流、日志等级），通常只配置 console 输出日志级别。

# OutPatient-backend-auth/
├── src/main/java/com/std/cuit/auth/AuthApplication.java: 鉴权模块启动类。
├── src/main/java/com/std/cuit/auth/controller/AuthController.java: 提供登录、注册、登出、邮箱验证和用户存在性检查等接口，负责生成/校验 token 并返回鉴权信息。
├── src/main/java/com/std/cuit/auth/config/SaTokenJwtConfig.java: Sa-Token + JWT 集成相关配置（token 名称、样式、jwt 签名密钥、自动续期等）。
├── src/main/java/com/std/cuit/auth/filter/SaTokenHeaderFilter.java: 在鉴权模块层面处理 token header 的过滤器，便于在网关或服务间透传鉴权信息。

# OutPatient-backend-medical/
├── src/main/java/com/std/cuit/medical/MedicalApplication.java: 医疗模块启动类。
├── src/main/java/com/std/cuit/medical/controller/DiagnosisController.java: 诊断相关的 REST 接口，处理病历、诊断记录的创建/查询/更新操作。
├── src/main/java/com/std/cuit/medical/controller/MessagesController.java: 医疗消息与沟通接口（医生-患者消息历史、通知等）。
├── src/main/java/com/std/cuit/medical/config/MedicalGlobalConfig.java: 医疗模块配置（模板路径、附件大小、患者数据脱敏开关等）。

# OutPatient-backend-data-analysis/
├── src/main/java/com/std/cuit/data/analysis/DataAnalysisApplication.java: 数据分析模块入口。
├── src/main/java/com/std/cuit/data/analysis/controller/DataAnalysisController.java: 提供报表查询、数据导出与统计接口，可能触发异步批处理或导出任务。
├── src/main/java/com/std/cuit/data/analysis/config/DataAnalysisGlobalConfig.java: 配置报表路径、导出行数上限、是否启用预测分析等业务设置。

# 其余文件（脚本/资源/模板等）
├── 各模块的 Dockerfile: 多阶段构建、设置运行用户、时区，并在镜像中创建 /home/appuser/logs 目录（容器内可写），但是否写日志由 `application.yml` 决定。
├── 各模块的 application-docker.yml: 容器化时覆盖的配置（端口、DB 地址、环境变量替换等）。
```