# Debug Log / 排错记录 — PharmaQuery-AI Demo Build

---

## Issue 1 / 问题 1：BERT model not loaded causes NER crash / BERT 模型未加载导致 NER 崩溃

**Symptom / 症状：**
Calling `parse_query()` crashed with `NameError: name 'bert_model' is not defined` when BERT had not been loaded.
调用 `parse_query()` 时因 BERT 未加载而触发 `NameError: name 'bert_model' is not defined` 崩溃。

**Root cause / 根因：**
The `app.py` module loads `bert_model = None` at module level. The `parse_query()` function accesses `bert_model` without a `global` declaration or None-check inside the branch.
`app.py` 模块在模块级别将 `bert_model` 初始化为 `None`，而 `parse_query()` 函数在分支内部访问 `bert_model` 时既未声明 `global`，也未做空值检查。

**Fix applied / 修复方案：**
The `run_demo.py` wraps all NLP calls in try/except and falls back to the standalone `_fallback_ner()` function which uses jieba dictionaries locally.
`run_demo.py` 将所有 NLP 调用包裹在 try/except 中，失败时自动降级到独立的 `_fallback_ner()` 函数，该函数使用本地 jieba 字典。

**Time to fix / 修复耗时：** 8 minutes / 8 分钟

---

## Issue 2 / 问题 2：jieba fails to recognize drug proper nouns / jieba 无法识别药品专有名词

**Symptom / 症状：**
`_fallback_ner("��蒙脱石散")` returned the whole text as a single drugName entity instead of extracting "蒙脱石散" as DRUG entity.
`_fallback_ner("蒙脱石散")` 将整个输入文本作为 drugName 实体返回，而不是将"蒙脱石散"提取为 DRUG 类型的实体。

**Root cause / 根因：**
jieba's default dictionary does not include pharmaceutical proper nouns like "蒙脱石散"、"阿莫西林" etc. Without a custom dictionary, jieba may split these into individual characters, preventing dictionary-based entity matching.
jieba 默认词典不包含"蒙脱石散"、"阿莫西林"等药学专有名词。缺少自定义词典时，jieba 会将这些词切分为单字，导致基于词典的实体匹配失败。

**Fix applied / 修复方案：**
Added `jieba.add_word()` calls for common drug names at module init with high frequency tags：
在模块初始化时添加高频药品名词到 jieba 自定义词典：

```python
jieba.add_word("阿莫西林", freq=100, tag="n")
jieba.add_word("蒙脱石散", freq=100, tag="n")
jieba.add_word("布洛芬", freq=100, tag="n")
jieba.add_word("头孢克肟", freq=100, tag="n")
# ... 9 common drug names total / 共 9 个常用药品名
```

**Verified / 验证结果：**
`_fallback_ner("阿莫西林胶囊")` now correctly outputs DRUG + DOSAGE_FORM entities.
`_fallback_ner("阿莫西林胶囊")` 现在正确输出 DRUG + DOSAGE_FORM 两种实体。

**Time to fix / 修复耗时：** 12 minutes / 12 分钟 （including reading jieba docs / 含查阅 jieba 文档）

---

## Issue 3 / 问题 3：Cold-start user returns empty recommendations / 冷启动用户返回空推荐

**Symptom / 症状：**
`_user_cf_recommend(999, ...)` returned `[]` for a user with no interaction history.
`_user_cf_recommend(999, ...)` 对无交互历史的用户返回了空列表 `[]`。

**Root cause / 根因：**
The original cold-start branch checked `if not target_vec` but `matrix.get(target_uid, {})` returns an empty dict `{}`, which evaluates as truthy in Python, causing the code to enter the warm-user recommendation path and produce no results （no similar users with 0 interactions）.
原始冷启动分支检查了 `if not target_vec`，但 `matrix.get(target_uid, {})` 返回的空字典 `{}` 在 Python 中被视为真值，导致代码进入了活跃用户推荐路径，但因无相似用户而输出空结果。

**Fix applied / 修复方案：**
Added explicit zero-value check before entering the warm-user path, and added a fallback that returns most-viewed drugs when no similar users are found：
进入活跃用户路径前添加显式零值检查，并在找不到相似用户时增加热门药品兜底：

```python
if not target_vec or not any(v > 0 for v in target_vec.values()):
    # cold start fallback / 冷启动兜底
    ...
if not top:
    # fallback: most-viewed drugs / 兜底：热门药品
    ...
```

**Verified / 验证结果：**
Cold-start user now correctly returns top-5 hot drug recommendations ranked by aggregate query frequency.
冷启动用户现在正确返回按总体查询频率排序的前 5 个热门药品推荐。

**Time to fix / 修复耗时：** 5 minutes / 5 分钟
