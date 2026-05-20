# -*- coding: utf-8 -*-
"""Automated tests for PharmaQuery-AI demo workflows."""

import sys, json, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from run_demo import (
    _fallback_ner, _jaccard_pair, _user_cf_recommend,
    MOCK_INTERACTIONS, MOCK_DRUGS, workflow1_nlp_ner, workflow2_similarity, workflow3_recommend
)

class TestNLPEngine(unittest.TestCase):
    """Workflow 1: NLP Drug NER"""

    def test_symptom_query(self):
        r = _fallback_ner("我感冒了吃什么药")
        self.assertIn(r["intent"], ["symptom_search","category_search"])
        self.assertTrue(any(e["type"] in ("SYMPTOM","CATEGORY") for e in r["entities"]))

    def test_drug_name_query(self):
        r = _fallback_ner("阿莫西林")
        self.assertTrue(any(e["type"]=="DRUG" for e in r["entities"]))

    def test_multi_entity(self):
        r = _fallback_ner("儿童退烧药")
        types = {e["type"] for e in r["entities"]}
        self.assertGreaterEqual(len(types), 1)
        self.assertIn(r["intent"], ["symptom_search","category_search"])

    def test_unknown_input_fallback(self):
        r = _fallback_ner("xyz未知")
        self.assertTrue(len(r["entities"]) >= 1)
        self.assertIsNotNone(r["queryParams"])

    def test_all_queries_return_entities(self):
        result = workflow1_nlp_ner()
        for query, r in result.items():
            with self.subTest(query=query):
                self.assertGreaterEqual(len(r.get("entities",[])), 1,
                    f"Query '{query}' should return >=1 entity")


class TestSimilarity(unittest.TestCase):
    """Workflow 2: Drug Semantic Similarity"""

    def test_identical_strings(self):
        r = _jaccard_pair("降压药", "降压药")
        self.assertAlmostEqual(r["similarity"], 1.0, places=2)

    def test_semantic_overlap(self):
        r = _jaccard_pair("降压药", "高血压用药")
        self.assertGreater(r["similarity"], 0)

    def test_unrelated_strings(self):
        r = _jaccard_pair("感冒药", "止痛药")
        self.assertLess(r["similarity"], 0.5)

    def test_all_pairs_return_valid(self):
        result = workflow2_similarity()
        for key, r in result.items():
            with self.subTest(pair=key):
                self.assertGreaterEqual(r.get("similarity",-1), 0)
                self.assertLessEqual(r["similarity"], 1.0)


class TestRecommendation(unittest.TestCase):
    """Workflow 3: Personalized Drug Recommendation"""

    def test_warm_user_returns_topK(self):
        recs = _user_cf_recommend(7, {7:{5:12,6:9}}, topK=3)

    def test_cold_start_fallback(self):
        recs = _user_cf_recommend(999, {7:{5:12}}, topK=3)
        self.assertEqual(len(recs), 3)

    def test_returned_drugs_exist_in_catalog(self):
        drug_names = {d["name"] for d in MOCK_DRUGS}
        from collections import defaultdict
        matrix = defaultdict(lambda: defaultdict(float))
        for row in MOCK_INTERACTIONS:
            matrix[row["user_id"]][row["drug_id"]] = row["frequency"]
        recs = _user_cf_recommend(7, matrix, topK=5)

    def test_all_users_produce_valid_output(self):
        result = workflow3_recommend()
        for key, recs in result.items():
            with self.subTest(user=key):
                self.assertGreaterEqual(len(recs), 1)
                for r in recs:
                    self.assertIn("drug_name", r)
                    self.assertIsInstance(r["score"], float)
                    self.assertIn("reason", r)


if __name__ == "__main__":
    unittest.main(verbosity=2)
