# 后端服务模块

基于 Spring Boot 的业务服务层，提供 REST API、WebSocket 实时推送、AI 智能查询、数据分析等功能。

## 📌 模块定位

本模块是系统的**业务服务层**，负责处理前端请求、调用数据处理模块、提供数据查询和分析服务。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **REST API** | 提供标准 RESTful 接口服务，支持 CRUD 操作 |
| **WebSocket 实时推送** | 实时数据推送，支持实时监控大屏 |
| **AI 智能查询** | 自然语言转 SQL、意图识别、图表生成、DSL 转换 |
| **AI 会话管理** | 多轮对话支持、查询收藏、计划任务 |
| **AI 数据库诊断** | SQL 安全性检测、数据倾斜诊断、Flink 反压分析 |
| **数据分析** | 多维分析、趋势分析、异常检测、预测分析 |
| **仪表盘** | KPI 指标汇总、实时数据、趋势展示 |
| **实时监控** | 实时订单量、热点区域、费用组成 |
| **热力地图** | 上下车热点可视化 |
| **数据质量** | 全链路质量监控、告警管理 |
| **数据导出** | 支持多种格式数据导出（Excel、CSV、JSON） |
| **认证授权** | 用户认证和权限管理 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.1.12 | 后端框架 |
| Spring Security | 6.2+ | 安全框架 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0+ | 主数据库 |
| WebSocket | - | 实时通信 |
| SpringDoc OpenAPI | 2.3.0 | API 文档 |
| Caffeine | 3.1.8 | 本地缓存 |
| Redis | - | 分布式缓存 |
| Java | 17 | 开发语言 |

## 📁 目录结构

```
taxi-analytics-backend/
├── src/main/java/com/taxi/analytics/
│   ├── TaxiAnalyticsApplication.java  # 应用入口
│   ├── common/                        # 公共模块
│   │   ├── config/                   # 配置类
│   │   │   ├── AsyncConfig.java      # 异步配置
│   │   │   ├── CorsConfig.java       # 跨域配置
│   │   │   ├── JdbcTemplateConfig.java # JDBC 配置
│   │   │   ├── MybatisPlusConfig.java  # MyBatis Plus 配置
│   │   │   ├── RestTemplateConfig.java # RestTemplate 配置
│   │   │   ├── SwaggerConfig.java     # Swagger 配置
│   │   │   ├── ThreadPoolConfig.java  # 线程池配置
│   │   │   └── WebMvcConfig.java      # Web MVC 配置
│   │   ├── controller/               # 公共控制器
│   │   │   └── HealthController.java  # 健康检查
│   │   ├── exception/               # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── interceptor/             # 拦截器
│   │   │   └── RateLimitInterceptor.java # 限流拦截器
│   │   ├── result/                  # 统一返回
│   │   │   ├── PageResult.java
│   │   │   ├── Result.java
│   │   │   └── ResultCode.java
│   │   └── validation/             # 参数校验
│   │       ├── DateRange.java
│   │       └── DateRangeValidatorImpl.java
│   └── modules/                      # 业务模块
│       ├── ai/                       # AI 智能查询模块
│       │   ├── client/              # LLM 客户端
│       │   │   ├── ChatMessage.java
│       │   │   ├── ChatResponse.java
│       │   │   ├── CozeClient.java  # Coze API 客户端
│       │   │   ├── DeepSeekClient.java # DeepSeek API 客户端
│       │   │   ├── LLMClient.java    # LLM 抽象接口
│       │   │   └── Message.java
│       │   ├── controller/
│       │   │   └── AiController.java  # AI 查询入口
│       │   ├── dsl/                 # DSL 转换
│       │   │   ├── Dsl.java
│       │   │   ├── DslConverter.java
│       │   │   └── DslValidator.java
│       │   ├── entity/             # 数据实体
│       │   ├── executor/           # 查询执行器
│       │   │   └── QueryExecutor.java
│       │   ├── guard/              # SQL 安全防护
│       │   │   └── SqlSecurityGuard.java
│       │   ├── intent/             # 意图识别
│       │   │   ├── Intent.java
│       │   │   ├── IntentClassifier.java
│       │   │   └── IntentType.java
│       │   ├── mapper/             # 数据访问
│       │   ├── metrics/            # 指标定义
│       │   │   ├── MetricDef.java
│       │   │   └── MetricRegistry.java
│       │   └── service/            # 业务服务
│       │       ├── AiService.java
│       │       ├── AiSessionService.java
│       │       ├── LlmService.java
│       │       ├── SchemaRetriever.java
│       │       ├── SqlGenerator.java
│       │       └── dto/             # 数据传输对象
│       ├── analysis/               # 数据分析模块
│       │   ├── controller/
│       │   │   └── AnalysisController.java
│       │   ├── dto/               # 数据传输对象
│       │   ├── mapper/
│       │   ├── service/
│       │   └── util/
│       ├── dashboard/             # 仪表盘模块
│       │   ├── controller/
│       │   │   └── DashboardController.java
│       │   ├── dto/
│       │   ├── entity/
│       │   │   └── DashboardKpi.java
│       │   ├── mapper/
│       │   └── service/
│       ├── export/                # 数据导出模块
│       │   ├── controller/
│       │   │   └── ExportController.java
│       │   ├── service/
│       │   └── task/
│       │       ├── ExportRequest.java
│       │       ├── ExportTask.java
│       │       ├── ExportTaskManager.java
│       │       ├── TaskStatus.java
│       │       └── TaskType.java
│       ├── map/                   # 热力地图模块
│       │   ├── controller/
│       │   │   └── MapController.java
│       │   ├── mapper/
│       │   └── service/
│       ├── quality/               # 质量监控模块
│       │   ├── controller/
│       │   │   └── QualityController.java
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── service/
│       │   └── task/
│       │       └── QualityCheckTask.java
│       └── realtime/              # 实时监控模块
│           ├── config/
│           │   ├── WebSocketConfig.java
│           │   └── ZoneMapping.java
│           ├── controller/
│           │   └── RealtimeController.java
│           ├── dto/
│           ├── handler/
│           │   └── RealtimeWebSocketHandler.java
│           ├── mapper/
│           └── service/
├── src/main/resources/
│   ├── application.yml           # 主配置
│   ├── application-dev.yml      # 开发环境
│   ├── application-prod.yml     # 生产环境
│   ├── mapper/                  # MyBatis Mapper XML
│   └── db/                      # 数据库脚本
├── docs/                        # 模块文档
├── dump.sql                     # 数据库初始化脚本
└── pom.xml                      # Maven 配置
```

## 🌐 API 接口

### 基础路径

```
http://localhost:8080/api/
```

### 接口分类

| 模块 | 路径 | 说明 |
|------|------|------|
| **仪表盘** | `/api/dashboard/*` | 仪表盘数据接口 |
| **实时监控** | `/api/realtime/*` | 实时指标接口 |
| **数据分析** | `/api/analysis/*` | 数据分析接口 |
| **热力地图** | `/api/map/*` | 热力图数据接口 |
| **数据质量** | `/api/quality/*` | 质量监控接口 |
| **AI 查询** | `/api/ai/*` | AI 智能查询接口 |
| **数据导出** | `/api/export/*` | 数据导出接口 |

### 核心接口

#### AI 模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/query` | POST | 统一 AI 查询入口 |
| `/api/ai/chat` | POST | 自然语言查询（兼容） |
| `/api/ai/gen-etl` | POST | 生成 ETL SQL |
| `/api/ai/sessions` | GET/POST | 会话管理 |
| `/api/ai/sessions/{id}/messages` | GET | 获取会话消息 |

#### Dashboard 模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/dashboard/kpi` | GET | KPI 指标汇总 |
| `/api/dashboard/trend` | GET | 趋势数据 |
| `/api/dashboard/hourly` | GET | 小时分布 |
| `/api/dashboard/vendor` | GET | 供应商表现 |

#### Realtime 模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/realtime/kpi` | GET | 实时 KPI |
| `/api/realtime/hotspots` | GET | 热点区域 |
| `/api/realtime/fee` | GET | 费用组成 |

#### Analysis 模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/analysis/trend` | GET | 趋势分析 |
| `/api/analysis/multi-dimension` | GET | 多维分析 |
| `/api/analysis/anomaly` | GET | 异常检测 |

## 🚀 运行方式

### 编译打包

```bash
cd taxi/taxi-analytics-backend
mvn clean package -DskipTests
```

### 启动服务

```bash
# 开发环境
mvn spring-boot:run

# 生产环境
java -jar target/taxi-analytics-backend-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 环境配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taxi_analytics
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

## 📝 文档链接

- [模块分析报告](后端服务模块-模块分析报告.md)
- [AI模块分析报告](后端服务模块-AI模块分析报告.md)
- [修复日志](后端服务模块-修复日志-20260516.md)
- [变更日志索引](后端服务模块-变更日志索引.md)
