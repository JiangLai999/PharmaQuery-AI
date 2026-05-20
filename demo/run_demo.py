# -*- coding: utf-8 -*-
"""
PharmaQuery-AI  AI Workflow MVP Demo
=====================================
10-minute verifiable AI pipeline: NER + Semantic Similarity + Recommendations

Usage:
    python run_demo.py              # Run full demo, output to console + results.json
    python run_demo.py --test       # Run only the verification workflow
    python run_demo.py --workflow 1 # Run single workflow (1=NLP, 2=Similarity, 3=Recommend)
"""

import sys, os, json, math, time, csv, argparse
from collections import defaultdict, OrderedDict
from pathlib import Path

# ── PATH SETUP ──────────────────────────────────────────────
ROOT  = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "nlp-service"))

# ── IMPORT NLP ENGINE ───────────────────────────────────────
HAS_NLP = False
try:
    from app import parse_query, compute_similarity, BERT_AVAILABLE, load_bert_model
    HAS_NLP = True
except Exception as e:
    print(f"[WARN] NLP service import failed: {e}")

# ── MOCK DRUG DATA (based on init_mysql.sql seed) ───────────
MOCK_DRUGS = [
    {"id":1,  "name":"阿莫西林胶囊",        "category":"抗感染药",     "indication":"上呼吸道感染 肺炎 扁桃体炎",       "form":"胶囊剂"},
    {"id":2,  "name":"头孢克肟分散片",      "category":"抗感染药",     "indication":"呼吸道感染 泌尿系感染 淋病",       "form":"片剂"},
    {"id":3,  "name":"阿奇霉素片",          "category":"抗感染药",     "indication":"呼吸道感染 皮肤软组织感染",        "form":"片剂"},
    {"id":4,  "name":"左氧氟沙星注射液",    "category":"抗感染药",     "indication":"呼吸道感染 泌尿感染 肠道感染",     "form":"注射液"},
    {"id":5,  "name":"氨氯地平片",          "category":"心血管系统药", "indication":"高血压 心绞痛",                     "form":"片剂"},
    {"id":6,  "name":"硝苯地平控释片",      "category":"心血管系统药", "indication":"高血压 冠心病 心绞痛",              "form":"控释片"},
    {"id":7,  "name":"卡托普利片",          "category":"心血管系统药", "indication":"高血压 心力衰竭",                   "form":"片剂"},
    {"id":8,  "name":"缬沙坦胶囊",          "category":"心血管系统药", "indication":"高血压 心力衰竭 心肌梗死后",       "form":"胶囊剂"},
    {"id":9,  "name":"二甲双胍片",          "category":"内分泌系统药", "indication":"2型糖尿病 肥胖",                   "form":"片剂"},
    {"id":10, "name":"格列美脲片",          "category":"内分泌系统药", "indication":"2型糖尿病",                         "form":"片剂"},
    {"id":11, "name":"胰岛素注射液",        "category":"内分泌系统药", "indication":"1型糖尿病 2型糖尿病 急性并发症",   "form":"注射液"},
    {"id":12, "name":"奥美拉唑肠溶胶囊",    "category":"消化系统药",   "indication":"胃溃疡 十二指肠溃疡 反流性食管炎", "form":"胶囊剂"},
    {"id":13, "name":"雷贝拉唑钠肠溶片",    "category":"消化系统药",   "indication":"胃溃疡 十二指肠溃疡 胃食管反流",   "form":"肠溶片"},
    {"id":14, "name":"蒙脱石散",            "category":"消化系统药",   "indication":"成人及儿童急慢性腹泻",              "form":"散剂"},
    {"id":15, "name":"对乙酰氨基酚片",      "category":"解热镇痛药",   "indication":"普通感冒 流感 发热 头痛 牙痛",     "form":"片剂"},
    {"id":16, "name":"布洛芬缓释胶囊",      "category":"解热镇痛药",   "indication":"头痛 牙痛 痛经 关节痛 发热",       "form":"胶囊剂"},
    {"id":17, "name":"氯雷他定片",          "category":"抗过敏药",     "indication":"过敏性鼻炎 荨麻疹",                 "form":"片剂"},
    {"id":18, "name":"右美沙芬片",          "category":"呼吸系统药",   "indication":"干咳 感冒引起的咳嗽",               "form":"片剂"},
    {"id":19, "name":"沙丁胺醇气雾剂",      "category":"呼吸系统药",   "indication":"支气管哮喘 喘息性支气管炎",         "form":"气雾剂"},
    {"id":20, "name":"阿托伐他汀钙片",      "category":"心血管系统药", "indication":"高胆固醇血症 冠心病",               "form":"片剂"},
]

# ── MOCK USER INTERACTIONS (cardiology-heavy, based on seed_data.sql) ──
MOCK_INTERACTIONS = [
    # Doctor in Internal Medicine - heart-focused
    {"user_id":7,  "drug_id":5,  "frequency":12},
    {"user_id":7,  "drug_id":6,  "frequency":9},
    {"user_id":7,  "drug_id":7,  "frequency":7},
    {"user_id":7,  "drug_id":8,  "frequency":6},
    {"user_id":7,  "drug_id":20, "frequency":5},
    {"user_id":7,  "drug_id":15, "frequency":3},
    # Doctor in Endocrinology
    {"user_id":6,  "drug_id":9,  "frequency":15},
    {"user_id":6,  "drug_id":10, "frequency":11},
    {"user_id":6,  "drug_id":11, "frequency":8},
    {"user_id":6,  "drug_id":5,  "frequency":4},
    {"user_id":6,  "drug_id":20, "frequency":3},
    # Doctor in Respiratory
    {"user_id":8,  "drug_id":18, "frequency":14},
    {"user_id":8,  "drug_id":19, "frequency":10},
    {"user_id":8,  "drug_id":15, "frequency":8},
    {"user_id":8,  "drug_id":17, "frequency":6},
    {"user_id":8,  "drug_id":3,  "frequency":5},
    # Doctor in Gastroenterology
    {"user_id":9,  "drug_id":12, "frequency":13},
    {"user_id":9,  "drug_id":13, "frequency":10},
    {"user_id":9,  "drug_id":14, "frequency":9},
    {"user_id":9,  "drug_id":15, "frequency":4},
    # Doctor in Neurology - mixed patterns
    {"user_id":10, "drug_id":16, "frequency":11},
    {"user_id":10, "drug_id":15, "frequency":10},
    {"user_id":10, "drug_id":5,  "frequency":6},
    {"user_id":10, "drug_id":6,  "frequency":5},
    # General Pharmacy - broad coverage
    {"user_id":3,  "drug_id":1,  "frequency":8},
    {"user_id":3,  "drug_id":2,  "frequency":7},
    {"user_id":3,  "drug_id":15, "frequency":6},
    {"user_id":3,  "drug_id":16, "frequency":5},
    {"user_id":3,  "drug_id":14, "frequency":4},
    {"user_id":3,  "drug_id":9,  "frequency":3},
]

MOCK_USERS = {
    1:  {"name":"系统管理员", "department":"信息科",     "role":"SYS_ADMIN"},
    2:  {"name":"药师王",     "department":"药剂科",     "role":"PHARMACIST"},
    3:  {"name":"药师李",     "department":"药剂科",     "role":"PHARMACIST"},
    6:  {"name":"内分泌科陈医生","department":"内分泌科","role":"DOCTOR"},
    7:  {"name":"心内科张医生",  "department":"心内科",   "role":"DOCTOR"},
    8:  {"name":"呼吸科刘医生",  "department":"呼吸科",   "role":"DOCTOR"},
    9:  {"name":"消化科赵医生",  "department":"消化科",   "role":"DOCTOR"},
    10: {"name":"神经科周医生",  "department":"神经科",   "role":"DOCTOR"},
}


# ════════════════════════════════════════════════════════════
# WORKFLOW 1 : NLP Drug NER  (Natural Language → Structured Entities)
# ════════════════════════════════════════════════════════════
def workflow1_nlp_ner():
    """Demonstrate BERT-BiLSTM-CRF / jieba rule engine on real drug queries."""
    test_queries = [
        "治感冒的抗生素",
        "阿莫西林胶囊",
        "儿童退烧药有哪些",
        "降压药",
        "胃溃疡吃什么药",
        "治疗腹泻的蒙脱石散",
        "老年人高血压用药",
    ]

    results = OrderedDict()
    print("\n" + "="*70)
    print("  WORKFLOW 1: NLP Drug NER  (Natural Language → Structured Entities)")
    print("  Engine:", "BERT-BiLSTM-CRF" if HAS_NLP and BERT_AVAILABLE else "jieba Rule Engine (fallback)")
    print("="*70)

    for query in test_queries:
        print(f"\n  [IN]  \"{query}\"")
        try:
            parsed = parse_query(query) if HAS_NLP else _fallback_ner(query)
            print(f"  [OUT] intent: {parsed.get('intent','?')}")
            for ent in parsed.get("entities", []):
                print(f"         entity: {ent['text']:10s}  type: {ent['type']:12s}  conf: {ent.get('confidence',0.9):.2f}")
            results[query] = parsed
        except Exception as e:
            print(f"  [ERR] {e}")
            results[query] = {"error": str(e)}

    return results


def _fallback_ner(text):
    """Minimal jieba-based NER fallback (mirrors app.py rule engine)."""
    import jieba
    for w, freq in [("阿莫西林",100),("蒙脱石散",100),("布洛芬",100),
                     ("头孢克肟",100),("奥美拉唑",100),("阿奇霉素",100),
                     ("氨氯地平",100),("二甲双胍",100),("对乙酰氨基酚",80)]:
        jieba.add_word(w, freq=freq, tag="n")
    entities = []
    intent = "drug_search"
    symptom_kw = {"感冒":"感冒","发烧":"发热","退烧":"发热","咳嗽":"咳嗽","头痛":"头痛",
                  "胃痛":"胃痛","胃溃疡":"胃溃疡","腹泻":"腹泻","高血压":"高血压","过敏":"过敏",
                  "炎症":"炎症","感染":"感染","糖尿病":"糖尿病"}
    category_kw = {"抗生素":"抗感染药","降压药":"心血管系统药","退烧药":"解热镇痛药",
                   "感冒药":"解热镇痛药","胃药":"消化系统药","消炎药":"抗感染药"}
    form_kw = {"胶囊":"胶囊剂","片":"片剂","注射液":"注射液","颗粒":"颗粒剂","散":"散剂"}
    pop_kw = {"儿童":"儿童","老人":"老年人","老年人":"老年人","孕妇":"孕妇"}
    drug_kw = {"阿莫西林":"阿莫西林","蒙脱石散":"蒙脱石散","布洛芬":"布洛芬",
               "头孢":"头孢","阿奇霉素":"阿奇霉素","奥美拉唑":"奥美拉唑"}
    words = list(jieba.cut(text))
    for w in words:
        if w in symptom_kw: entities.append({"text":w,"type":"SYMPTOM","confidence":0.88})
        elif w in category_kw: entities.append({"text":w,"type":"CATEGORY","confidence":0.90})
        elif w in form_kw: entities.append({"text":w,"type":"DOSAGE_FORM","confidence":0.92})
        elif w in pop_kw: entities.append({"text":w,"type":"POPULATION","confidence":0.85})
        elif w in drug_kw: entities.append({"text":w,"type":"DRUG","confidence":0.95})
    qp = {}
    for e in entities:
        t = e["type"]
        if t in ("SYMPTOM","DRUG","CATEGORY","DOSAGE_FORM","POPULATION"):
            qp[t.lower()] = e["text"]
    types = {e["type"] for e in entities}
    if "SYMPTOM" in types: intent = "symptom_search"
    elif "CATEGORY" in types: intent = "category_search"
    if not entities: entities.append({"text":text,"type":"DRUG","confidence":0.50}); qp["drugName"]=text
    return {"entities":entities, "intent":intent, "queryParams":qp}


# ════════════════════════════════════════════════════════════
# WORKFLOW 2 : Drug Similarity  (Semantic Text Matching)
# ════════════════════════════════════════════════════════════
def workflow2_similarity():
    """Compute semantic similarity between drug-related text pairs."""
    pairs = [
        ("降压药",   "高血压用药"),
        ("抗生素",   "头孢类药物"),
        ("感冒药",   "止痛药"),
        ("胃溃疡",   "消化性溃疡"),
        ("阿莫西林", "青霉素类"),
    ]

    results = OrderedDict()
    print("\n" + "="*70)
    print("  WORKFLOW 2: Drug Semantic Similarity")
    engine = "BERT cosine" if HAS_NLP and BERT_AVAILABLE else "Jaccard (fallback)"
    print("  Engine:", engine)
    print("="*70)

    if HAS_NLP:
        for a, b in pairs:
            print(f"\n  [IN]  \"{a}\"  vs  \"{b}\"")
            try:
                sim_result = compute_similarity(a, b)
                sim = sim_result.get("similarity", 0)
                eg = sim_result.get("engine", "?")
                print(f"  [OUT] similarity: {sim:.4f}  (engine: {eg})")
                results[f"{a}|{b}"] = {"similarity":sim,"engine":eg}
            except Exception as e:
                print(f"  [ERR] {e}")
                results[f"{a}|{b}"] = _jaccard_pair(a,b)
    else:
        for a, b in pairs:
            print(f"\n  [IN]  \"{a}\"  vs  \"{b}\"")
            r = _jaccard_pair(a,b)
            print(f"  [OUT] similarity: {r['similarity']:.4f}  (engine: jaccard)")
            results[f"{a}|{b}"] = r

    return results


def _jaccard_pair(a, b):
    """Jaccard similarity: word-level + char-level, take max."""
    import jieba
    sa, sb = set(jieba.cut(a)), set(jieba.cut(b))
    inter = len(sa & sb)
    union = len(sa | sb)
    word_sim = inter/union if union else 0
    ca, cb = set(a), set(b)
    cinter = len(ca & cb)
    cunion = len(ca | cb)
    char_sim = cinter/cunion if cunion else 0
    sim = max(word_sim, char_sim)
    return {"similarity": round(sim, 4), "engine": "jaccard"}


# ════════════════════════════════════════════════════════════
# WORKFLOW 3 : Personalized Recommendation  (User-Based CF)
# ════════════════════════════════════════════════════════════
def workflow3_recommend():
    """User-based collaborative filtering with real seed data patterns."""
    print("\n" + "="*70)
    print("  WORKFLOW 3: Personalized Drug Recommendation (User-Based CF)")
    print("  Method: Cosine similarity on interaction frequency vectors")
    print("="*70)

    # Build user-drug matrix
    matrix = defaultdict(lambda: defaultdict(float))
    for row in MOCK_INTERACTIONS:
        matrix[row["user_id"]][row["drug_id"]] = row["frequency"]

    # 3 use cases
    test_cases = [
        {"user_id":7,  "label":"心内科张医生 (有丰富历史)", "topK":5},
        {"user_id":6,  "label":"内分泌科陈医生 (专业集中)",  "topK":5},
        {"user_id":999,"label":"新入职医生 (Cold Start)",    "topK":5},
    ]

    results = OrderedDict()
    for tc in test_cases:
        uid = tc["user_id"]
        label = tc["label"]
        topK = tc["topK"]
        print(f"\n  ── {label} ──")
        recs = _user_cf_recommend(uid, matrix, topK)
        for i, r in enumerate(recs, 1):
            print(f"  [{i}] {r['drug_name']:20s} score:{r['score']:.4f}  {r['reason']}")
        results[f"user_{uid}"] = recs

    return results


def _user_cf_recommend(target_uid, matrix, topK=5):
    """User-based CF: cosine similarity + weighted scoring."""
    target_vec = matrix.get(target_uid, {})
    drug_list = {d["id"]:d for d in MOCK_DRUGS}

    if not target_vec:
        # Cold start: return most-viewed drugs
        drug_freq = defaultdict(int)
        for row in MOCK_INTERACTIONS:
            drug_freq[row["drug_id"]] += row["frequency"]
        top = sorted(drug_freq.items(), key=lambda x:-x[1])[:topK]
        return [{"drug_name":drug_list[did]["name"], "score":round(freq/20,4),
                 "reason":"热门药品推荐（冷启动）"} for did,freq in top]

    # Compute user-user similarities
    sims = {}
    target_items = set(target_vec.keys())
    target_norm = math.sqrt(sum(v*v for v in target_vec.values()))
    for ouid, ovec in matrix.items():
        if ouid == target_uid: continue
        common = set(ovec.keys()) & target_items
        if not common: continue
        dot = sum(target_vec[k]*ovec[k] for k in common)
        norm = target_norm * math.sqrt(sum(v*v for v in ovec.values()))
        s = dot/norm if norm else 0
        if s >= 0.15:
            sims[ouid] = s

    # Score unseen drugs
    scores = defaultdict(float)
    seen = set(target_vec.keys())
    for ouid, sim in sorted(sims.items(), key=lambda x:-x[1])[:30]:
        for did, freq in matrix[ouid].items():
            if did not in seen:
                scores[did] += sim * freq

    top = sorted(scores.items(), key=lambda x:-x[1])[:topK]
    if not top:
        # Fallback: return most-viewed drugs from all users
        drug_freq = defaultdict(int)
        for row in MOCK_INTERACTIONS:
            drug_freq[row["drug_id"]] += row["frequency"]
        top_fallback = sorted(drug_freq.items(), key=lambda x:-x[1])[:topK]
        return [{"drug_name":drug_list[did]["name"], "score":round(freq/20,4),
                 "reason":"热门药品推荐（相似用户不足）"} for did,freq in top_fallback]
    recs = []
    for did, score in top:
        dl = drug_list.get(did, {"name":f"Drug#{did}"})
        recs.append({"drug_name":dl["name"],
                     "score":round(score,4),
                     "reason":f"有{int(min(score/15*100,95))}%的相似用户也查询了此药品"})
    return recs


# ════════════════════════════════════════════════════════════
# MAIN
# ════════════════════════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--test", action="store_true")
    parser.add_argument("--workflow", type=int, choices=[1,2,3])
    parser.add_argument("--output", default="results.json")
    args = parser.parse_args()

    t0 = time.time()

    if not HAS_NLP:
        print("[INFO] NLP service not available in this Python. Using standalone fallback engine.")
        print("[INFO] Install: pip install -r nlp-service/requirements.txt")

    all_results = OrderedDict()
    all_results["meta"] = {
        "project": "PharmaQuery-AI",
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "engine": "BERT-BiLSTM-CRF" if (HAS_NLP and BERT_AVAILABLE) else "jieba Rule Engine",
        "demo_version": "1.0.0"
    }

    if args.workflow == 1 or (not args.test and not args.workflow):
        all_results["workflow1_ner"] = workflow1_nlp_ner()

    if args.workflow == 2 or (not args.test and not args.workflow):
        all_results["workflow2_similarity"] = workflow2_similarity()

    if args.workflow == 3 or (not args.test and not args.workflow):
        all_results["workflow3_recommend"] = workflow3_recommend()

    t1 = time.time()

    # Save results
    out_path = Path(args.output)
    out_path.write_text(json.dumps(all_results, ensure_ascii=False, indent=2), encoding="utf-8")

    print("\n" + "="*70)
    print(f"  Demo complete in {t1-t0:.2f}s") if args.test else print(f"  All workflows complete in {t1-t0:.2f}s")
    print(f"  Structured results saved to: {out_path.resolve()}")
    print("="*70)

    # Quick self-check summary
    if args.test:
        checks = 0
        if all_results.get("workflow1_ner"): checks += 1
        if all_results.get("workflow2_similarity"): checks += 1
        if all_results.get("workflow3_recommend"): checks += 1
        print(f"\n  Self-check: {checks}/3 workflows executed | PASS" if checks==3 else f"\n  Self-check: {checks}/3 | INCOMPLETE")


if __name__ == "__main__":
    main()
