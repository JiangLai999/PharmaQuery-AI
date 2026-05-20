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

---

## 24h Build Challenge：AI Workflow MVP / AI 工作流 MVP（10 min verifiable / 10 分钟可验证）

> **Goal / 目标**：No MySQL / Java / WeChat needed. **Only Python + 3 commands** to verify the full AI pipeline.
无需 MySQL / Java / 微信小程序，**仅需 Python + 3 条命令**即可验证全部 AI 流程。

### Quick Start / 快速启动（< 3 min / < 3 分钟）

```bash
git clone https://github.com/JiangLai999/PharmaQuery-AI.git
cd PharmaQuery-AI
pip install -r demo/requirements.txt
python demo/run_demo.py
```

Output / 输出：`results.json`（structured JSON containing all 3 workflow results / 结构化 JSON，包含全部 3 个工作流结果）

---

### AI Workflow 1 / AI 工作流 1：NLP Drug NER / NLP 药品命名实体识别

**Scenario / 用户场景**：Clinicians describe patient symptoms in natural language during ward rounds and need to quickly locate corresponding drugs.
临床医生在查房时用自然语言描述患者症状，需要快速定位对应药品。

**Pain Point / 痛点**：Traditional HIS systems only support exact keyword matching, unable to understand colloquial queries like "antibiotics for colds" or "fever medicine for children".
传统 HIS 系统仅支持关键词精确匹配，无法理解"治感冒的抗生素""儿童退烧药"等口语化查询。

| Input / 输入 | Output / 输出（Entity + Type + Confidence / 实体 + 类型 + 置信度） | Intent / 意图 |
|---|---|---|
| "阿莫西林胶囊" | 阿莫西林(DRUG, 0.95) + 胶囊(DOSAGE_FORM, 0.92) | drug_search |
| "儿童退烧药有哪些" | 儿童(POPULATION, 0.85) + 退烧药(CATEGORY, 0.90) | category_search |
| "老年人高血压用药" | 老年人(POPULATION, 0.85) + 高血压(SYMPTOM, 0.88) | symptom_search |

**Engine / 引擎**：BERT-BiLSTM-CRF（primary / 优先） / jieba rule engine（fallback / 降级兜底）

---

### AI Workflow 2 / AI 工作流 2：Drug Semantic Similarity / 药品语义相似度

**Scenario / 用户场景**：Pharmacists need to find alternative drugs when reviewing prescriptions.
药师在审核处方时需要查找某药品的可替代品种。

**Pain Point / 痛点**：Different manufacturers use different brand names for pharmacologically similar drugs （e.g. "降压药" vs "高血压用药"），keyword search cannot capture semantic relationships.
不同厂家的商品名不同但药理相似（如"降压药" vs "高血压用药"），关键词查询无法捕获语义关联。

| Input A / 输入 A | Input B / 输入 B | Similarity / 相似度 | Engine / 引擎 |
|---|---|---|---|
| "降压药" | "高血压用药" | 0.33 | jaccard |
| "胃溃疡" | "消化性溃疡" | 0.33 | jaccard |
| "感冒药" | "止痛药" | 0.20 | jaccard |

> **Note / 说明**：Jaccard is character-level fallback. With BERT enabled, cosine similarity on embeddings produces results like "降压药" vs "高血压用药" ≈ 0.75+.
Jaccard 值为字符级兜底。启用 BERT 后相似度由余弦相似度计算（如"降压药" vs "高血压用药"预期 ≥ 0.75）。

---

### AI Workflow 3 / AI 工作流 3：Personalized Drug Recommendation / 个性化药品推荐（CF / 协同过滤）

**Scenario / 用户场景**：Doctors from different departments need to see department-specific drugs first when querying.
不同科室医生在药品查询时需要优先看到本科室常用药品。

**Pain Point / 痛点**：Traditional systems display a uniform drug list for all users, ignoring departmental differences, leading to low search efficiency.
传统系统统一展示药品列表，忽略科室差异，导致查询效率低下。

| Input / 输入 | Top-3 Recommendations / 推荐结果 | Basis / 推荐依据 |
|---|---|---|
| Cardiologist Zhang / 心内科张医生 (user_7) | Ibuprofen 布洛芬(5.19)、Metformin 二甲双胍(2.44)、Glimepiride 格列美脲(1.79) | 34% similar users also queried / 34% 相似用户也查询过 |
| Endocrinologist Chen / 内分泌科陈医生 (user_6) | Nifedipine 硝苯地平(1.47)、Paracetamol 对乙酰氨基酚(1.41)、Amoxicillin 阿莫西林(1.22) | 9% similar users also queried / 9% 相似用户也查询过 |
| New doctor / 新入职医生 (cold-start) | Paracetamol 对乙酰氨基酚(1.55)、Amlodipine 氨氯地平(1.10)、Metformin 二甲双胍(0.90) | Hot drugs / 热门药品（冷启动） |

**Method / 方法**：User-Based Collaborative Filtering / 基于用户的协同过滤，cosine similarity on interaction vectors / 余弦相似度计算用户交互向量，weighted scoring for unseen drugs / 加权评分推荐未交互药品。

---

### Next Validation / 下一步验证计划

1. **Deploy to hospital test environment / 部署医院测试环境**，compare traditional search vs NLP search avg query time / 对比传统搜索 vs NLP 搜索的平均查询时间
2. **A/B test recommendation CTR / A/B 测试推荐点击率**：CF vs random recommendation，target CTR +30% / CF 推荐 vs 随机推荐，目标 CTR 提升 30%+
3. **Collect real doctor query logs / 收集真实医生查询日志**，expand NER dictionary （currently 50 words / 当前仅 50 词），train domain-specific BERT model / 训练领域 BERT 模型
4. **Integrate with real HIS system API / 接入真实 HIS 系统 API**，enable smart drug substitution prompts during prescription review / 实现处方审核时的替代药品智能提示

---

### Key Files / 关键文件

| File / 文件 | Description / 说明 |
|---|---|
| `demo/run_demo.py` | AI workflow MVP main script / 主脚本（360 lines / 行，standalone runnable / 独立可运行） |
| `demo/test_demo.py` | Automated tests / 自动化测试（13 test cases / 13 个测试用例） |
| `demo/results.json` | Structured output after run / 运行后的结构化输出 |
| `demo/DEBUG_LOG.md` | Real debugging log / 真实排错记录（3 bugs + fixes / 3 个 Bug + 修复） |
| `demo/AI_COLLABORATION.md` | AI collaboration record / AI 协作记录（Agent usage / Agent 使用说明） |
| `nlp-service/app.py` | BERT-BiLSTM-CRF + jieba rule engine / jieba 规则引擎（319 lines / 行） |
| `backend/src/main/java/.../RecommendServiceImpl.java` | Production CF implementation / 生产环境协同过滤实现 |
| `sql/init_mysql.sql` | Database schema / 数据库表结构（50 drugs / 50 药品 + RBAC tables / RBAC 表） |

---

### Test Cases / 测试用例（2+）

Run automated tests / 运行自动化测试：

```bash
python -m unittest demo/test_demo.py -v
```

Coverage / 覆盖范围：

| Test Class / 测试类 | Cases / 用例数 | Coverage / 覆盖内容 |
|---|---|---|
| TestNLPEngine | 5 | Symptom/Drug name/Multi-entity/Unknown/Fallback / 症状/药品名/多实体/未知输入/降级 |
| TestSimilarity | 4 | Identical/Semantic overlap/Unrelated/Validity / 相同/语义重叠/无关/有效性 |
| TestRecommendation | 4 | Warm user/Cold-start/Catalog check/Completeness / 活跃用户/冷启动/药品库/完整性 |

---

### Real Debugging Log / 真实排错记录

详见 `demo/DEBUG_LOG.md`, 3 real bugs from development / 收录 3 条开发过程的真实 Bug：

1. **BERT not loaded → NER crash / BERT 未加载导致 NER 崩溃** → add try/except + jieba fallback / 增加 try/except + jieba 降级
2. **jieba missing drug proper nouns / jieba 未识别药品专有名词** → manually add high-frequency drug word list / 手动添加 jieba 高频词表
3. **Cold-start user returns empty recommendations / 冷启动用户返回空推荐** → add hot-drug fallback strategy / 增加热门药品兜底策略

---

### AI Collaboration Record / AI 协作记录

详见 `demo/AI_COLLABORATION.md`, full record of AI Agent usage in this Build Challenge / 完整记录本次 Build Challenge 的 AI Agent 使用过程：

- Platform / 平台：OpenCode（opencode.ai）
- Model / 模型：deepseek-v4-pro
- Total time / 总耗时：~45 min / 约 45 分钟
- Complete loop / 包含：Code exploration / 代码探索 → MVP design / MVP 设计 → Implementation / 编码实现 → Debug & verification / 调试验证

---

## License / 开源协议

本项目基于 [MIT License](LICENSE) 开源。
