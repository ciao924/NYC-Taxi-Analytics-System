# 后端服务模块

基于 Spring Boot 的业务服务层，提供 REST API、WebSocket 实时推送、AI 智能查询等功能。

## 📌 模块定位

本模块是系统的**业务服务层**，负责处理前端请求、调用数据处理模块、提供数据查询和分析服务。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **REST API** | 提供标准 RESTful 接口服务 |
| **WebSocket** | 实时数据推送，支持实时监控 |
| **AI 智能查询** | 自然语言转 SQL、图表生成 |
| **数据导出** | 支持多种格式数据导出 |
| **质量监控** | 全链路数据质量监控和告警 |
| **认证授权** | 用户认证和权限管理 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2+ | 后端框架 |
| Spring Security | 6.2+ | 安全框架 |
| MyBatis Plus | 3.5+ | ORM 框架 |
| MySQL | 8.0+ | 主数据库 |
| WebSocket | - | 实时通信 |
| Java | 21 | 开发语言 |

## 📁 目录结构

```
taxi-analytics-backend/
├── src/main/java/com/taxi/analytics/
│   ├── controller/           # REST 控制层
│   ├── service/              # 业务逻辑层
│   ├── mapper/               # 数据访问层
│   ├── entity/               # 数据库实体
│   ├── dto/                  # 数据传输对象
│   ├── config/               # 配置类
│   ├── util/                 # 工具类
│   └── TaxiAnalyticsApplication.java
├── src/main/resources/       # 配置文件
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── mapper/               # MyBatis Mapper XML
│   └── db/                   # 数据库脚本
├── docs/                     # 模块文档
└── pom.xml                   # Maven 配置
```

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
| **数据质量** | `/api/quality/*` | 质量监控接口 |
| **AI 查询** | `/api/ai/*` | AI 智能查询接口 |
| **导出任务** | `/api/export/*` | 数据导出接口 |

## 📝 文档链接

- [模块分析报告](后端服务模块-模块分析报告.md)
- [AI模块分析报告](后端服务模块-AI模块分析报告.md)
- [修复日志索引](后端服务模块-变更日志索引.md)