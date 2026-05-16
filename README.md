# PharmaQuery AI

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen?logo=springboot" alt="Spring Boot 2.7">
  <img src="https://img.shields.io/badge/Python-3.10+-blue?logo=python&logoColor=white" alt="Python 3.10">
  <img src="https://img.shields.io/badge/Flask-3.x-lightgrey?logo=flask" alt="Flask">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479a1?logo=mysql&logoColor=white" alt="MySQL 8">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License MIT">
</p>

<p align="center">
  <strong>基于 AI 技术的智能药库药品查询平台</strong>
</p>

<p align="center">
  集成 Spring Boot 后端 · 微信小程序前端 · NLP 语义微服务
</p>

---

## 项目简介

PharmaQuery AI 是一款面向医疗机构的智能药品信息查询系统。通过自然语言处理（NLP）技术与协同过滤推荐算法，帮助医护人员快速、准确地获取药品信息，提升药房管理效率。

## 核心特性

- **智能搜索**：支持药品名称模糊搜索与 NLP 自然语言语义解析
- **个性化推荐**：基于协同过滤算法，按科室与查询习惯推荐药品
- **库存管理**：近效期预警、低库存提醒、库存调整记录
- **RBAC 权限控制**：细粒度角色权限管理，保障数据安全
- **多端适配**：微信小程序原生开发，随时随地使用

## 技术架构

```
┌─────────────────────────────────────────────────────┐
│                  微信小程序前端                       │
│              (WeChat Mini Program)                   │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / REST API
┌──────────────────────▼──────────────────────────────┐
│               Spring Boot 后端服务                    │
│  Java 17 · Spring Security · MyBatis-Plus · JWT     │
└──┬───────────────────────────────┬──────────────────┘
   │                               │
   ▼                               ▼
┌──────────────┐          ┌──────────────────┐
│   MySQL 8    │          │  NLP 微服务       │
│  MongoDB     │          │  Flask + BERT    │
│  Redis       │          │  jieba + torch   │
└──────────────┘          └──────────────────┘
```

## 快速开始

### 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x |
| Python | 3.10+ |
| 微信开发者工具 | 最新稳定版 |

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE pharmacy_db DEFAULT CHARACTER SET utf8mb4;"

# 执行初始化脚本
mysql -u root -p pharmacy_db < sql/init_mysql.sql
mysql -u root -p pharmacy_db < sql/seed_data.sql
```

### 2. 配置后端

复制配置模板并修改：

```bash
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application.yml
```

编辑 `application.yml`，设置数据库密码与 JWT 密钥：

```yaml
spring:
  datasource:
    username: your_db_username
    password: your_db_password

jwt:
  secret: your_secure_jwt_secret_key
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080/api`

### 3. 启动 NLP 微服务（可选）

```bash
cd nlp-service
pip install -r requirements.txt
python app.py
```

NLP 服务默认运行在 `http://localhost:5000`

### 4. 配置微信小程序

1. 使用微信开发者工具打开 `frontend` 目录
2. 编辑 `frontend/config/app.js`，将 `apiHost` 改为你的局域网 IP
3. 勾选"不校验合法域名、web-view、TLS 版本以及 HTTPS 证书"

```js
apiHost: '192.168.x.x'  // 替换为实际 IP
```

## 默认测试账号

> **安全提示**：以下账号仅用于开发测试，请勿用于生产环境。

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `123456` | 系统管理员 | 全部权限 |
| `pharmacist` | `123456` | 药师 | 药品查询与库存管理 |
| `doctor01` | `123456` | 临床医生 | 药品查询与推荐 |

更多测试账号见 `sql/seed_data.sql`。

## RBAC 权限矩阵

| 功能模块 | 临床医生 | 药师 | 药房管理员 | 系统管理员 |
|----------|:--------:|:----:|:----------:|:----------:|
| 药品查询 | ✅ | ✅ | ✅ | ✅ |
| 药品详情 | ✅ | ✅ | ✅ | ✅ |
| 个性化推荐 | ✅ | ✅ | ❌ | ❌ (管理看板) |
| 新增/编辑药品 | ❌ | ❌ | ✅ | ✅ |
| 删除药品 | ❌ | ❌ | ✅ | ✅ |
| 库存查看 | ❌ | ✅ | ✅ | ✅ |
| 库存调整 | ❌ | ✅ | ✅ | ✅ |
| 低库存/近效期 | ❌ | ✅ | ✅ | ✅ |
| 操作日志 | ❌ | ❌ | ✅ | ✅ |

## 项目结构

```text
pharma-query-ai/
├── backend/              # Spring Boot 后端服务
│   ├── src/main/java/
│   │   └── com/pharmacy/
│   │       ├── config/   # 配置类 (Security, Redis, WebSocket)
│   │       ├── controller/ # REST API 控制器
│   │       ├── entity/   # 数据库实体
│   │       ├── service/  # 业务逻辑层
│   │       ├── mapper/   # MyBatis-Plus Mapper
│   │       └── security/ # JWT 认证与授权
│   └── src/main/resources/
│       └── application.yml # 应用配置
├── frontend/             # 微信小程序前端
│   ├── pages/            # 页面 (首页/搜索/详情/推荐/个人中心)
│   ├── components/       # 自定义组件
│   ├── utils/            # 工具函数
│   └── config/           # 应用配置
├── nlp-service/          # Python NLP 微服务
│   ├── app.py            # Flask 主程序
│   └── requirements.txt  # Python 依赖
└── sql/                  # 数据库脚本
    ├── init_mysql.sql    # 表结构初始化
    └── seed_data.sql     # 测试数据
```

## API 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| GET | `/api/drugs` | 药品列表（分页/搜索） | DRUG_INFO_READ |
| GET | `/api/drugs/{id}` | 药品详情 | DRUG_INFO_READ |
| POST | `/api/drugs` | 新增药品 | DRUG_INFO_WRITE |
| PUT | `/api/drugs/{id}` | 编辑药品 | DRUG_INFO_WRITE |
| DELETE | `/api/drugs/{id}` | 删除药品 | DRUG_INFO_DELETE |
| GET | `/api/recommend` | 个性化推荐 | RECOMMEND_READ |
| POST | `/api/nlp/parse` | NLP 语义解析 | 内部调用 |

## 常见问题

### 小程序请求超时

- 确认后端服务已启动
- `frontend/config/app.js` 中的 `apiHost` 是否为当前电脑的局域网 IP
- 手机与电脑需在同一局域网
- 微信开发者工具中关闭域名校验

### NLP 搜索无结果

- 确认 `nlp-service` 已启动 (`python app.py`)
- 检查后端 `nlp.service.url` 配置是否正确
- NLP 服务为可选组件，未启动时自动降级为规则引擎

### 推荐结果不变化

- 推荐算法依赖用户查询历史，新用户需积累一定查询记录
- 可切换不同角色账号体验差异化推荐结果

## 安全注意事项

- 生产环境务必修改默认数据库密码与 JWT Secret
- 启用 HTTPS 与 WSS（WebSocket 安全协议）
- 定期更新依赖版本，修复已知安全漏洞
- 测试账号密码哈希请勿直接用于生产环境

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
