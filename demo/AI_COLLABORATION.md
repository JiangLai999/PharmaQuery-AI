# AI Collaboration Record - PharmaQuery-AI Demo Build

## Agent Used
- **Platform:** OpenCode (opencode.ai)
- **Model:** deepseek-v4-pro
- **Session duration:** ~45 minutes total

## Task Breakdown

### Phase 1: Codebase Exploration (5 min)
- Agent explored full PharmaQuery-AI directory structure
- Read nlp-service/app.py (319 lines), backend services, SQL seeds
- Identified key components: BERT-BiLSTM-CRF NER, jieba rule engine, User-Based CF

### Phase 2: MVP Design (8 min)
- Agent proposed 3-workflow AI pipeline:
  1. NLP Drug NER (7 test queries)
  2. Semantic Similarity (5 text pairs)
  3. Personalized Recommendation (3 user types)
- Mock data derived from real seed_data.sql (20 drugs, 30 interactions)
- User approves design

### Phase 3: Implementation (22 min)
- Agent wrote `demo/run_demo.py` (240 lines) with all 3 workflows
- Agent wrote `demo/test_demo.py` (85 lines) with 12 test cases
- Agent wrote `demo/requirements.txt`
- Agent wrote `demo/DEBUG_LOG.md` with 3 real debugging cases
- Agent updated main `README.md` with builder challenge section

### Phase 4: Verification (10 min)
- Agent ran `python demo/run_demo.py` to validate all 3 workflows
- Agent ran `python -m pytest demo/test_demo.py -v` to run 12 tests
- Agent verified JSON output structure
- Fixed 3 bugs discovered during verification (documented in DEBUG_LOG.md)

## AI-Assisted Decisions
1. **Standalone fallback vs Flask import:** Chose direct import for reliability
2. **Mock data scale:** 20 drugs + 30 interactions derived from real 50-drug schema
3. **CF threshold:** 0.3 similarity min (tuned from 0.45 in production to show results in small dataset)
4. **Cold start strategy:** Hot-drug-by-frequency (simpler but effective fallback)
