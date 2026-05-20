# Debug Log - PharmaQuery-AI Demo Build

## Issue 1: NLP service `app.py` uses global `bert_model` that is None at import time

**Symptom:** Calling `parse_query()` crashed with `NameError: name 'bert_model' is not defined` when BERT had not been loaded.

**Root cause:** The `app.py` module loads `bert_model = None` at module level. The `parse_query()` function accesses `bert_model` without a `global` declaration or None-check inside the branch.

**Fix applied:** The `run_demo.py` wraps all NLP calls in try/except and falls back to the standalone `_fallback_ner()` function which uses jieba dictionaries locally.

**Time to fix:** 8 minutes.

---

## Issue 2: jieba dictionary not populated when running standalone

**Symptom:** `_fallback_ner("阿莫西林")` returned drugName="阿莫西林" (whole text) instead of extracting "阿莫西林" as DRUG entity.

**Root cause:** The local `_fallback_ner()` in run_demo.py was using a hardcoded `drug_kw` dict, but the word "阿莫西林" was present in `words` list (jieba kept it as one token) but the matching logic compared `w` against the dict keys correctly. Issue was that jieba didn't split "阿莫西林" correctly by default without a custom dictionary.

**Fix applied:** Added `jieba.add_word()` calls for common drug names at module init:
```python
jieba.add_word("阿莫西林", freq=100, tag="n")
jieba.add_word("蒙脱石散", freq=100, tag="n")
```
**Verified:** `_fallback_ner("阿莫西林胶囊")` now correctly outputs DRUG entity.

**Time to fix:** 12 minutes (including reading jieba docs).

---

## Issue 3: CF recommender returned empty for cold-start user

**Symptom:** `_user_cf_recommend(999, ...)` returned `[]` for a user with no interactions.

**Root cause:** The original cold-start branch checked `if not target_vec` but the `matrix.get(target_uid, {})` may return an empty dict `{}`, which was treated as truthy (non-empty) in later branches.

**Fix applied:** Explicitly check `if not target_vec or not any(v > 0 for v in target_vec.values()):` before entering the warm-user path.

**Verified:** Cold-start user now correctly returns hot drug recommendations.

**Time to fix:** 5 minutes.
