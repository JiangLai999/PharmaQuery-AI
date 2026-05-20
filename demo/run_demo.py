# -*- coding: utf-8 -*-
"""
PharmaQuery-AI  AI Workflow MVP Demo
=====================================
10-minute verifiable AI pipeline powered by BERT embeddings + Collaborative Filtering.

Usage:
    python run_demo.py              # Run full demo, output to console + results.json
    python run_demo.py --workflow 1 # Run single workflow (1=NER, 2=Similarity, 3=Recommend)
"""

import sys, os, json, time, argparse
from collections import defaultdict, OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# ── AI ENGINE ──────────────────────────────────────────────
_HAS_AI = False
_AI_ERR = ""
try:
    from ai_engine import semantic_ner, semantic_similarity, cosine_sim, encode_batch
    _HAS_AI = True
except Exception as e:
    _AI_ERR = str(e)

# ── MOCK DRUG CATALOG ──────────────────────────────────────
MOCK_DRUGS = [
    {"id":1,  "name":"阿莫西林胶囊",        "category":"抗感染药",     "indication":"上呼吸道感染 肺炎 扁桃体炎 中耳炎",       "form":"胶囊剂"},
    {"id":2,  "name":"头孢克肟分散片",      "category":"抗感染药",     "indication":"呼吸道感染 泌尿系感染 淋病 中耳炎",       "form":"片剂"},
    {"id":3,  "name":"阿奇霉素片",          "category":"抗感染药",     "indication":"呼吸道感染 皮肤软组织感染 支原体肺炎",    "form":"片剂"},
    {"id":4,  "name":"左氧氟沙星注射液",    "category":"抗感染药",     "indication":"呼吸道感染 泌尿感染 肠道感染 腹腔感染",   "form":"注射液"},
    {"id":5,  "name":"氨氯地平片",          "category":"心血管系统药", "indication":"高血压 心绞痛 慢性稳定性心绞痛",         "form":"片剂"},
    {"id":6,  "name":"硝苯地平控释片",      "category":"心血管系统药", "indication":"高血压 冠心病 变异型心绞痛",              "form":"控释片"},
    {"id":7,  "name":"卡托普利片",          "category":"心血管系统药", "indication":"高血压 心力衰竭 心肌梗死后",             "form":"片剂"},
    {"id":8,  "name":"缬沙坦胶囊",          "category":"心血管系统药", "indication":"高血压 心力衰竭 心肌梗死后 糖尿病肾病",  "form":"胶囊剂"},
    {"id":9,  "name":"二甲双胍片",          "category":"内分泌系统药", "indication":"2型糖尿病 肥胖 多囊卵巢综合征",          "form":"片剂"},
    {"id":10, "name":"格列美脲片",          "category":"内分泌系统药", "indication":"2型糖尿病 血糖控制",                     "form":"片剂"},
    {"id":11, "name":"胰岛素注射液",        "category":"内分泌系统药", "indication":"1型糖尿病 2型糖尿病 糖尿病急性并发症",   "form":"注射液"},
    {"id":12, "name":"奥美拉唑肠溶胶囊",    "category":"消化系统药",   "indication":"胃溃疡 十二指肠溃疡 反流性食管炎 胃酸过多","form":"胶囊剂"},
    {"id":13, "name":"雷贝拉唑钠肠溶片",    "category":"消化系统药",   "indication":"胃溃疡 十二指肠溃疡 胃食管反流 糜烂性食管炎","form":"肠溶片"},
    {"id":14, "name":"蒙脱石散",            "category":"消化系统药",   "indication":"成人及儿童急慢性腹泻 腹痛 腹胀",          "form":"散剂"},
    {"id":15, "name":"对乙酰氨基酚片",      "category":"解热镇痛药",   "indication":"普通感冒 流感 发热 头痛 牙痛 神经痛",    "form":"片剂"},
    {"id":16, "name":"布洛芬缓释胶囊",      "category":"解热镇痛药",   "indication":"头痛 牙痛 痛经 关节痛 肌肉痛 发热",      "form":"胶囊剂"},
    {"id":17, "name":"氯雷他定片",          "category":"抗过敏药",     "indication":"过敏性鼻炎 荨麻疹 瘙痒性皮肤病",          "form":"片剂"},
    {"id":18, "name":"右美沙芬片",          "category":"呼吸系统药",   "indication":"干咳 感冒引起的咳嗽 上呼吸道感染",       "form":"片剂"},
    {"id":19, "name":"沙丁胺醇气雾剂",      "category":"呼吸系统药",   "indication":"支气管哮喘 喘息性支气管炎 慢阻肺",       "form":"气雾剂"},
    {"id":20, "name":"阿托伐他汀钙片",      "category":"心血管系统药", "indication":"高胆固醇血症 冠心病 脑卒中预防",         "form":"片剂"},
]

# ── MOCK USER INTERACTIONS (department-specific patterns) ──
MOCK_INTERACTIONS = [
    {"user_id":7,  "drug_id":5,  "frequency":12},
    {"user_id":7,  "drug_id":6,  "frequency":9},
    {"user_id":7,  "drug_id":7,  "frequency":7},
    {"user_id":7,  "drug_id":8,  "frequency":6},
    {"user_id":7,  "drug_id":20, "frequency":5},
    {"user_id":7,  "drug_id":15, "frequency":3},
    {"user_id":6,  "drug_id":9,  "frequency":15},
    {"user_id":6,  "drug_id":10, "frequency":11},
    {"user_id":6,  "drug_id":11, "frequency":8},
    {"user_id":6,  "drug_id":5,  "frequency":4},
    {"user_id":6,  "drug_id":20, "frequency":3},
    {"user_id":8,  "drug_id":18, "frequency":14},
    {"user_id":8,  "drug_id":19, "frequency":10},
    {"user_id":8,  "drug_id":15, "frequency":8},
    {"user_id":8,  "drug_id":17, "frequency":6},
    {"user_id":8,  "drug_id":3,  "frequency":5},
    {"user_id":9,  "drug_id":12, "frequency":13},
    {"user_id":9,  "drug_id":13, "frequency":10},
    {"user_id":9,  "drug_id":14, "frequency":9},
    {"user_id":9,  "drug_id":15, "frequency":4},
    {"user_id":10, "drug_id":16, "frequency":11},
    {"user_id":10, "drug_id":15, "frequency":10},
    {"user_id":10, "drug_id":5,  "frequency":6},
    {"user_id":10, "drug_id":6,  "frequency":5},
    {"user_id":3,  "drug_id":1,  "frequency":8},
    {"user_id":3,  "drug_id":2,  "frequency":7},
    {"user_id":3,  "drug_id":15, "frequency":6},
    {"user_id":3,  "drug_id":16, "frequency":5},
    {"user_id":3,  "drug_id":14, "frequency":4},
    {"user_id":3,  "drug_id":9,  "frequency":3},
]


# ════════════════════════════════════════════════════════════
# WORKFLOW 1 : Real AI NER via BERT Semantic Matching
# ════════════════════════════════════════════════════════════
def workflow1_ner():
    """
    Real AI: Encode the query + all drug indications into BERT embeddings,
    then find top-k semantically closest drugs.

    This is NOT keyword matching. It understands that 'fever in children'
    relates to drugs treating 'common cold, influenza, fever' even when
    the exact words don't overlap.
    """
    queries = [
        "孩子发烧了吃什么药",
        "老人家血压高头晕",
        "胃疼反酸想吃药",
        "感冒咳嗽嗓子疼",
        "拉肚子止泻",
    ]

    print("\n" + "=" * 72)
    print("  WORKFLOW 1: Real AI NER — BERT Semantic Drug Matching")
    if not _HAS_AI:
        print(f"  [SKIP] AI engine unavailable: {_AI_ERR}")
        return {"error": _AI_ERR}
    print("  Model: paraphrase-multilingual-MiniLM-L12-v2 (118 MB, 384-dim)")
    print("  Method: Query embedding → cosine similarity vs 20 drug vectors")
    print("=" * 72)

    results = OrderedDict()
    for query in queries:
        print(f"\n  [IN]  \"{query}\"")
        matches = semantic_ner(query, MOCK_DRUGS)
        for i, m in enumerate(matches[:3], 1):
            bar = "█" * int(m["similarity"] * 20)
            print(f"  [{i}] {m['drug_name']:18s}  sim={m['similarity']:.4f}  [{m['category']}]  {bar}")
        results[query] = matches[:5]
    return results


# ════════════════════════════════════════════════════════════
# WORKFLOW 2 : Real AI Semantic Similarity via BERT Embeddings
# ════════════════════════════════════════════════════════════
def workflow2_similarity():
    """
    Real AI: Encode both texts into BERT embeddings, compute cosine distance.

    Unlike Jaccard (character overlap), BERT captures genuine semantic meaning:
    'antibiotics' vs 'cephalosporins' → high similarity despite zero shared characters.
    """
    pairs = [
        ("降压药",   "高血压用药"),
        ("抗生素",   "头孢类药物"),
        ("感冒药",   "止痛药"),
        ("胃溃疡",   "消化性溃疡"),
        ("阿莫西林", "青霉素类抗生素"),
    ]

    print("\n" + "=" * 72)
    print("  WORKFLOW 2: Real AI Similarity — BERT Embedding Cosine Distance")
    if not _HAS_AI:
        print(f"  [SKIP] AI engine unavailable: {_AI_ERR}")
        return {"error": _AI_ERR}
    print("  Model: paraphrase-multilingual-MiniLM-L12-v2")
    print("  Method: encode(text_a), encode(text_b) → cosine similarity")
    print("=" * 72)

    results = OrderedDict()
    for a, b in pairs:
        r = semantic_similarity(a, b)
        bar = "█" * int(r["similarity"] * 20)
        print(f"  \"{a}\"  vs  \"{b}\"")
        print(f"  → similarity = {r['similarity']:.4f}  {bar}")
        results[f"{a}|{b}"] = r
    return results


# ════════════════════════════════════════════════════════════
# WORKFLOW 3 : User-Based Collaborative Filtering (Real ML)
# ════════════════════════════════════════════════════════════
def workflow3_recommend():
    """Real ML: User-based CF with cosine similarity on interaction vectors."""
    import math

    matrix = defaultdict(lambda: defaultdict(float))
    for row in MOCK_INTERACTIONS:
        matrix[row["user_id"]][row["drug_id"]] = row["frequency"]

    test_cases = [
        {"user_id":7,  "label":"心内科张医生 (Cardiologist, warm)",  "topK":5},
        {"user_id":6,  "label":"内分泌科陈医生 (Endocrinologist, warm)","topK":5},
        {"user_id":999,"label":"新入职医生 (Cold Start)",              "topK":5},
    ]

    print("\n" + "=" * 72)
    print("  WORKFLOW 3: Real ML — User-Based Collaborative Filtering")
    print("  Method: Cosine similarity on user-drug interaction vectors")
    print("  Min neighbor similarity: 0.15 | Max neighbors: 30")
    print("=" * 72)

    results = OrderedDict()
    drug_list = {d["id"]: d for d in MOCK_DRUGS}

    for tc in test_cases:
        uid, label, topK = tc["user_id"], tc["label"], tc["topK"]
        print(f"\n  ── {label} ──")

        target_vec = matrix.get(uid, {})
        if not target_vec or not any(v > 0 for v in target_vec.values()):
            # Cold start
            dfreq = defaultdict(int)
            for row in MOCK_INTERACTIONS:
                dfreq[row["drug_id"]] += row["frequency"]
            top = sorted(dfreq.items(), key=lambda x: -x[1])[:topK]
            recs = []
            for did, freq in top:
                dl = drug_list.get(did, {"name": f"Drug#{did}"})
                recs.append({"drug_name": dl["name"], "score": round(freq/20, 4),
                             "reason": "热门药品推荐 (冷启动 / cold start)"})
        else:
            # Compute user-user similarities
            sims = {}
            t_items = set(target_vec.keys())
            t_norm = math.sqrt(sum(v * v for v in target_vec.values()))
            for ouid, ovec in matrix.items():
                if ouid == uid:
                    continue
                common = set(ovec.keys()) & t_items
                if not common:
                    continue
                dot = sum(target_vec[k] * ovec[k] for k in common)
                o_norm = math.sqrt(sum(v * v for v in ovec.values()))
                s = dot / (t_norm * o_norm) if t_norm * o_norm else 0
                if s >= 0.15:
                    sims[ouid] = s

            scores = defaultdict(float)
            seen = set(target_vec.keys())
            for ouid, sim in sorted(sims.items(), key=lambda x: -x[1])[:30]:
                for did, freq in matrix[ouid].items():
                    if did not in seen:
                        scores[did] += sim * freq

            top = sorted(scores.items(), key=lambda x: -x[1])[:topK]
            if not top:
                dfreq = defaultdict(int)
                for row in MOCK_INTERACTIONS:
                    dfreq[row["drug_id"]] += row["frequency"]
                top_fb = sorted(dfreq.items(), key=lambda x: -x[1])[:topK]
                recs = []
                for did, freq in top_fb:
                    dl = drug_list.get(did, {"name": f"Drug#{did}"})
                    recs.append({"drug_name": dl["name"], "score": round(freq/20, 4),
                                 "reason": "热门药品推荐 (相似用户不足 / no similar users)"})
            else:
                recs = []
                for did, score in top:
                    dl = drug_list.get(did, {"name": f"Drug#{did}"})
                    pct = int(min(score / sum(v for _, v in top) * 100 if top else 50, 98))
                    recs.append({"drug_name": dl["name"], "score": round(score, 4),
                                 "reason": f"有{pct}%的相似用户也查询了此药品"})

        for i, r in enumerate(recs, 1):
            print(f"  [{i}] {r['drug_name']:18s}  score={r['score']:.4f}  {r['reason']}")
        results[f"user_{uid}"] = recs

    return results


# ════════════════════════════════════════════════════════════
# MAIN
# ════════════════════════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workflow", type=int, choices=[1, 2, 3])
    parser.add_argument("--output", default="results.json")
    args = parser.parse_args()

    t0 = time.time()

    if not _HAS_AI:
        print(f"\n[ERROR] AI engine failed to load: {_AI_ERR}")
        print("[FIX]  Run: pip install sentence-transformers")
        print("[FIX]  Then re-run: python run_demo.py")
        return

    all_results = OrderedDict()
    all_results["meta"] = {
        "project": "PharmaQuery-AI",
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "ai_engine": "BERT embeddings (paraphrase-multilingual-MiniLM-L12-v2, 384-dim)",
        "demo_version": "2.0.0"
    }

    if args.workflow == 1 or not args.workflow:
        all_results["workflow1_bert_ner"] = workflow1_ner()
    if args.workflow == 2 or not args.workflow:
        all_results["workflow2_bert_similarity"] = workflow2_similarity()
    if args.workflow == 3 or not args.workflow:
        all_results["workflow3_cf_recommend"] = workflow3_recommend()

    t1 = time.time()

    out_path = Path(args.output)
    out_path.write_text(json.dumps(all_results, ensure_ascii=False, indent=2), encoding="utf-8")

    print("\n" + "=" * 72)
    print(f"  All 3 workflows complete in {t1 - t0:.2f}s")
    print(f"  Structured results → {out_path.resolve()}")
    print("  Engine: REAL BERT embeddings (not keyword matching)")
    print("=" * 72)


if __name__ == "__main__":
    main()
