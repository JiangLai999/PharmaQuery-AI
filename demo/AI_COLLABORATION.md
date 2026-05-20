# AI Collaboration Record / AI 协作记录 — PharmaQuery-AI Demo Build

## Agent / 智能体平台

- **Platform / 平台：** OpenCode (opencode.ai)
- **Model / 模型：** deepseek-v4-pro
- **Session duration / 会话时长：** ~45 minutes / 约 45 分钟

## Task Breakdown / 任务拆解

### Phase 1 / 第一阶段：Codebase Exploration / 代码库探索 （5 min / 5 分钟）

- Agent explored full PharmaQuery-AI directory structure / Agent 遍历了 PharmaQuery-AI 完整目录结构
- Read / 阅读了 `nlp-service/app.py` （319 lines / 行）、backend services / 后端服务层、SQL seeds / 数据库种子数据
- Identified key components / 识别关键组件：BERT-BiLSTM-CRF NER、jieba rule engine / jieba 规则引擎、User-Based CF / 基于用户的协同过滤

### Phase 2 / 第二阶段：MVP Design / MVP 设计 （8 min / 8 分钟）

- Agent proposed 3-workflow AI pipeline / Agent 提出 3 个工作流的 AI 管道：
  1. NLP Drug NER / NLP 药品命名实体识别 （7 test queries / 7 条测试查询）
  2. Semantic Similarity / 语义相似度 （5 text pairs / 5 对文本）
  3. Personalized Recommendation / 个性化推荐 （3 user types / 3 种用户类型）
- Mock data derived from real seed_data.sql / Mock 数据源自真实 seed_data.sql （20 drugs / 20 种药品、30 interactions / 30 条交互记录）
- User approves design / 用户确认设计方案

### Phase 3 / 第三阶段：Implementation / 编码实现 （22 min / 22 分钟）

- Agent wrote / Agent 编写了 `demo/run_demo.py` （360 lines / 行） 含全部 3 个工作流
- Agent wrote / Agent 编写了 `demo/test_demo.py` （93 lines / 行） 含 13 个测试用例
- Agent wrote / Agent 编写了 `demo/requirements.txt`
- Agent wrote / Agent 编写了 `demo/DEBUG_LOG.md` 含 3 条真实排错案例 / 3 real debugging cases
- Agent updated main `README.md` with builder challenge section / Agent 更新了主 README 的 Build Challenge 章节

### Phase 4 / 第四阶段：Verification / 验证 （10 min / 10 分钟）

- Agent ran `python demo/run_demo.py` to validate all 3 workflows / Agent 运行 Demo 验证全部 3 个工作流
- Agent ran `python -m unittest demo/test_demo.py -v` to run 13 tests / Agent 运行 13 个单元测试
- Agent verified JSON output structure / Agent 验证了 JSON 输出结构
- Fixed 3 bugs discovered during verification （documented in DEBUG_LOG.md） / 修复了验证过程中发现的 3 个 Bug（已记录在 DEBUG_LOG.md）

## AI-Assisted Decisions / AI 辅助决策

1. **Standalone fallback vs Flask import / 独立降级 vs Flask 导入：** Chose direct import for reliability / 选择直接导入以确保可靠性
2. **Mock data scale / Mock 数据规模：** 20 drugs + 30 interactions derived from real 50-drug schema / 20 种药品 + 30 条交互，源自真实 50 种药品的表结构
3. **CF threshold / 协同过滤阈值：** 0.15 similarity min （tuned from 0.45 in production to show results in small dataset） / 最小相似度 0.15（从生产环境的 0.45 下调，以适应小数据集展示）
4. **Cold start strategy / 冷启动策略：** Hot-drug-by-frequency / 按查询频率的热门药品推荐（简单但有效的兜底方案）
