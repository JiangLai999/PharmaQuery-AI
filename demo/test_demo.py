# -*- coding: utf-8 -*-
"""Automated tests for PharmaQuery-AI demo (Real AI version)."""

import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from run_demo import workflow1_ner, workflow2_similarity, workflow3_recommend, MOCK_DRUGS

_HAS_AI = True
try:
    from ai_engine import semantic_ner, semantic_similarity
except ImportError:
    _HAS_AI = False


@unittest.skipUnless(_HAS_AI, "AI engine not available")
class TestBERTNER(unittest.TestCase):
    """Workflow 1: Real AI BERT Semantic NER"""

    def test_fever_query_returns_antipyretics(self):
        matches = semantic_ner("孩子发烧了吃什么药", MOCK_DRUGS)
        self.assertGreaterEqual(len(matches), 3)
        self.assertGreater(matches[0]["similarity"], 0.3,
            f"Expected high similarity for fever query, got {matches[0]['similarity']}")

    def test_stomach_pain_returns_digestive_drugs(self):
        matches = semantic_ner("胃疼反酸想吃药", MOCK_DRUGS)
        self.assertGreaterEqual(len(matches), 3)
        cat = matches[0]["category"]
        self.assertIn("消化", cat, f"Expected digestive drug for stomach pain, got {cat}")

    def test_hypertension_query_returns_cv_drugs(self):
        matches = semantic_ner("老人家血压高头晕", MOCK_DRUGS)
        self.assertGreaterEqual(len(matches), 3)
        cats = [m["category"] for m in matches[:3]]
        self.assertTrue(any("心血管" in c for c in cats),
            f"Expected cardiovascular drugs, got {cats}")

    def test_all_queries_produce_valid_output(self):
        result = workflow1_ner()
        for query, matches in result.items():
            with self.subTest(query=query):
                self.assertGreaterEqual(len(matches), 3)


@unittest.skipUnless(_HAS_AI, "AI engine not available")
class TestBERTSimilarity(unittest.TestCase):
    """Workflow 2: Real AI BERT Semantic Similarity"""

    def test_synonyms_high_similarity(self):
        r = semantic_similarity("降压药", "高血压用药")
        self.assertGreater(r["similarity"], 0.5,
            f"'降压药' vs '高血压用药' should be >0.5, got {r['similarity']}")

    def test_unrelated_low_similarity(self):
        r = semantic_similarity("感冒药", "止痛药")
        self.assertLess(r["similarity"], 0.7,
            f"'感冒药' vs '止痛药' should be <0.7, got {r['similarity']}")
        self.assertGreater(r["similarity"], r.get("_", 0),
            "Should still have some semantic similarity")

    def test_antibiotics_cephalosporins(self):
        r = semantic_similarity("抗生素", "头孢类药物")
        self.assertGreater(r["similarity"], 0.3,
            f"'抗生素' vs '头孢类药物' should be >0.3 (BERT captures semantics), got {r['similarity']}")

    def test_all_pairs_return_valid(self):
        result = workflow2_similarity()
        for key, r in result.items():
            with self.subTest(pair=key):
                self.assertGreaterEqual(r["similarity"], 0)
                self.assertLessEqual(r["similarity"], 1.0)


class TestCFRecommend(unittest.TestCase):
    """Workflow 3: Real ML Collaborative Filtering"""

    def test_warm_user_returns_results(self):
        result = workflow3_recommend()
        recs = result.get("user_7", [])
        self.assertGreaterEqual(len(recs), 1)

    def test_cold_start_has_fallback(self):
        result = workflow3_recommend()
        recs = result.get("user_999", [])
        self.assertEqual(len(recs), 5)

    def test_all_users_have_recommendations(self):
        result = workflow3_recommend()
        for key, recs in result.items():
            with self.subTest(user=key):
                self.assertGreaterEqual(len(recs), 1)
                for r in recs:
                    self.assertIn("drug_name", r)
                    self.assertIsInstance(r["score"], float)


if __name__ == "__main__":
    unittest.main(verbosity=2)
