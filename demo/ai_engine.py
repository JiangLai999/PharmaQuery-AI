# -*- coding: utf-8 -*-
"""Real AI engine for PharmaQuery-AI demo. Uses BERT embeddings for NER + similarity."""

import numpy as np

_MODEL = None

def _load_model():
    """Lazy-load a multilingual BERT model for sentence embeddings."""
    global _MODEL
    if _MODEL is None:
        from sentence_transformers import SentenceTransformer
        _MODEL = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")
    return _MODEL


def encode_batch(texts: list) -> np.ndarray:
    """Encode a batch of texts into BERT embeddings (384-dim)."""
    model = _load_model()
    return model.encode(texts, convert_to_numpy=True, show_progress_bar=False)


def cosine_sim(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine similarity between two embedding vectors."""
    return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-9))


def semantic_ner(query: str, drug_catalog: list) -> list:
    """
    Real AI NER: Match natural language query to drug catalog using BERT embeddings.
    Returns top-k semantically closest drugs with confidence scores.
    """
    if not drug_catalog:
        return []

    texts = [query] + [d["name"] + " " + d["indication"] for d in drug_catalog]
    embeddings = encode_batch(texts)
    query_emb = embeddings[0]
    drug_embs = embeddings[1:]

    results = []
    for i, d in enumerate(drug_catalog):
        sim = cosine_sim(query_emb, drug_embs[i])
        results.append({"drug_name": d["name"], "category": d["category"],
                        "similarity": round(sim, 4),
                        "form": d.get("form","")})
    return sorted(results, key=lambda x: -x["similarity"])


def semantic_similarity(text_a: str, text_b: str) -> dict:
    """Real AI similarity: BERT embedding cosine similarity."""
    embeddings = encode_batch([text_a, text_b])
    sim = cosine_sim(embeddings[0], embeddings[1])
    return {"similarity": round(sim, 4), "engine": "BERT embeddings (MiniLM-L12-v2)"}
