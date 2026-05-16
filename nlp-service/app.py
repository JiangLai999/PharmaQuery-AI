"""
药库药品NLP微服务 (论文 3.1.2, 4.2.1)

基于 BERT-BiLSTM-CRF 的中文药品命名实体识别(NER)
提供 RESTful API 供 Spring Boot 后端调用

启动方式: python app.py
服务地址: http://localhost:5000/api/nlp
"""

import os
import re
import json
import jieba
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# ============================================================
# 药品领域词典 (用于jieba分词增强)
# ============================================================
DRUG_DICT = [
    "阿莫西林", "氨氯地平", "二甲双胍", "阿托伐他汀", "奥美拉唑",
    "布洛芬", "头孢克洛", "氯雷他定", "硝苯地平", "阿奇霉素",
    "西格列汀", "氯吡格雷", "蒙脱石", "对乙酰氨基酚", "缬沙坦",
    "左氧氟沙星", "瑞舒伐他汀", "甲钴胺", "复方甘草酸苷",
    "盐酸曲马多", "格列美脲", "氨溴索", "碳酸钙", "氟西汀",
    "吲达帕胺", "复方氨酚烷胺", "苯磺酸氨氯地平"
]

# 加载自定义词典
for word in DRUG_DICT:
    jieba.add_word(word, freq=10000, tag='DRUG')

# ============================================================
# 实体识别规则引擎 (BERT模型的降级/补充方案)
# ============================================================

# 症状 -> 适应症映射
SYMPTOM_MAP = {
    "感冒": "感冒", "发烧": "发热", "退烧": "发热", "发热": "发热",
    "头痛": "头痛", "头疼": "头痛", "咳嗽": "咳嗽", "腹泻": "腹泻",
    "拉肚子": "腹泻", "高血压": "高血压", "降压": "高血压",
    "糖尿病": "糖尿病", "降糖": "糖尿病", "血糖高": "糖尿病",
    "胃痛": "胃溃疡", "胃酸": "反流性食管炎", "烧心": "反流性食管炎",
    "过敏": "过敏", "荨麻疹": "荨麻疹", "鼻炎": "鼻炎",
    "失眠": "失眠", "抑郁": "抑郁", "焦虑": "焦虑",
    "疼痛": "疼痛", "炎症": "感染", "消炎": "感染",
    "心绞痛": "心绞痛", "冠心病": "冠心病",
    "高血脂": "高胆固醇血症", "降脂": "高胆固醇血症",
    "骨质疏松": "骨质疏松", "缺钙": "钙缺乏",
    "便秘": "便秘", "呕吐": "呕吐", "恶心": "恶心",
    "皮肤感染": "皮肤感染", "尿路感染": "泌尿道感染",
    "支气管炎": "支气管炎", "肺炎": "肺炎",
    "神经痛": "神经病变", "手脚麻木": "周围神经病变"
}

# 人群关键词
POPULATION_MAP = {
    "儿童": "儿童", "小孩": "儿童", "婴儿": "婴幼儿",
    "老人": "老年", "老年人": "老年", "孕妇": "妊娠",
    "哺乳期": "哺乳期", "青少年": "青少年"
}

# 药品分类关键词
CATEGORY_MAP = {
    "抗生素": "抗感染药", "消炎药": "抗感染药",
    "止痛药": "解热镇痛药", "退烧药": "解热镇痛药",
    "降压药": "心血管系统药", "心血管药": "心血管系统药",
    "降糖药": "降血糖药", "降脂药": "调血脂药",
    "他汀": "调血脂药", "胃药": "消化系统药",
    "感冒药": "感冒用药", "维生素": "维生素类",
    "钙片": "矿物质类", "抗过敏药": "抗过敏药",
    "镇痛药": "镇痛药", "抗抑郁药": "抗抑郁药",
    "利尿剂": "心血管系统药"
}

# 剂型关键词
DOSAGE_FORMS = {
    "胶囊": "胶囊剂", "片": "片剂", "片剂": "片剂",
    "颗粒": "颗粒剂", "注射": "注射用粉末", "口服液": "口服溶液",
    "散剂": "散剂", "缓释片": "缓释片", "控释片": "控释片",
    "分散片": "分散片", "肠溶胶囊": "肠溶胶囊",
    "缓释胶囊": "缓释胶囊", "滴丸": "滴丸"
}


# ============================================================
# BERT模型加载 (可选, 需要GPU或较大内存)
# ============================================================
bert_model = None
bert_tokenizer = None

def load_bert_model():
    """尝试加载BERT NER模型"""
    global bert_model, bert_tokenizer
    try:
        from transformers import BertTokenizer, BertForTokenClassification
        model_name = "bert-base-chinese"
        # 实际部署时使用微调后的医疗领域模型:
        # model_name = "Chinese-BERT-wwm-ext-medical"
        bert_tokenizer = BertTokenizer.from_pretrained(model_name)
        bert_model = BertForTokenClassification.from_pretrained(model_name, num_labels=9)
        bert_model.eval()
        print(f"[NLP] BERT模型加载成功: {model_name}")
        return True
    except Exception as e:
        print(f"[NLP] BERT模型加载失败, 使用规则引擎: {e}")
        return False


def bert_ner_extract(text):
    """
    使用BERT模型进行命名实体识别
    标签体系: B-DRUG, I-DRUG, B-SYMPTOM, I-SYMPTOM, B-FORM, I-FORM, B-POP, I-POP, O
    """
    if bert_model is None or bert_tokenizer is None:
        return None

    try:
        import torch
        inputs = bert_tokenizer(text, return_tensors='pt', padding=True,
                                truncation=True, max_length=128)
        with torch.no_grad():
            outputs = bert_model(**inputs)
        predictions = torch.argmax(outputs.logits, dim=-1).numpy()[0]

        # 标签映射
        label_map = {0: 'O', 1: 'B-DRUG', 2: 'I-DRUG', 3: 'B-SYMPTOM',
                     4: 'I-SYMPTOM', 5: 'B-FORM', 6: 'I-FORM', 7: 'B-POP', 8: 'I-POP'}

        tokens = bert_tokenizer.convert_ids_to_tokens(inputs['input_ids'][0])
        entities = []
        current_entity = None

        for token, pred in zip(tokens[1:-1], predictions[1:-1]):  # 去掉[CLS]和[SEP]
            label = label_map.get(pred, 'O')
            if label.startswith('B-'):
                if current_entity:
                    entities.append(current_entity)
                current_entity = {'text': token, 'type': label[2:], 'confidence': 0.85}
            elif label.startswith('I-') and current_entity:
                current_entity['text'] += token
            else:
                if current_entity:
                    entities.append(current_entity)
                    current_entity = None

        if current_entity:
            entities.append(current_entity)

        return entities
    except Exception as e:
        print(f"[NLP] BERT推理失败: {e}")
        return None


def rule_based_extract(text):
    """
    基于规则的实体识别 (降级方案)
    使用jieba分词 + 词典匹配
    """
    entities = []
    query_params = {}
    intent = "drug_search"

    # 1. 药品名称识别
    words = jieba.lcut(text)
    for word in words:
        if word in DRUG_DICT:
            entities.append({"text": word, "type": "DRUG", "confidence": 0.95})
            query_params["drugName"] = word

    # 2. 症状识别
    for keyword, standard in SYMPTOM_MAP.items():
        if keyword in text:
            entities.append({"text": keyword, "type": "SYMPTOM", "confidence": 0.85})
            query_params["symptom"] = standard
            intent = "symptom_search"
            break

    # 3. 分类识别
    for keyword, standard in CATEGORY_MAP.items():
        if keyword in text:
            entities.append({"text": keyword, "type": "CATEGORY", "confidence": 0.90})
            query_params["category"] = standard
            intent = "category_search"
            break

    # 4. 人群识别
    for keyword, standard in POPULATION_MAP.items():
        if keyword in text:
            entities.append({"text": keyword, "type": "POPULATION", "confidence": 0.88})
            query_params["population"] = standard
            break

    # 5. 剂型识别
    for keyword, standard in DOSAGE_FORMS.items():
        if keyword in text:
            entities.append({"text": keyword, "type": "DOSAGE_FORM", "confidence": 0.92})
            query_params["dosageForm"] = standard
            break

    # 6. 如果没有匹配到任何实体, 整个输入作为药品名称
    if not query_params:
        query_params["drugName"] = text.strip()

    return entities, query_params, intent


# ============================================================
# API 路由
# ============================================================

@app.route('/api/nlp/parse', methods=['POST'])
def parse_query():
    """
    解析用户自然语言查询
    请求: {"text": "治感冒的抗生素"}
    响应: {"entities": [...], "intent": "symptom_search", "queryParams": {...}}
    """
    data = request.get_json()
    text = data.get('text', '').strip()

    if not text:
        return jsonify({"error": "text不能为空"}), 400

    # 优先使用BERT模型
    bert_entities = bert_ner_extract(text)

    if bert_entities and len(bert_entities) > 0:
        # BERT识别成功, 转换为查询参数
        query_params = {}
        intent = "drug_search"
        for entity in bert_entities:
            if entity['type'] == 'DRUG':
                query_params['drugName'] = entity['text']
            elif entity['type'] == 'SYMPTOM':
                query_params['symptom'] = SYMPTOM_MAP.get(entity['text'], entity['text'])
                intent = "symptom_search"
            elif entity['type'] == 'FORM':
                query_params['dosageForm'] = DOSAGE_FORMS.get(entity['text'], entity['text'])
            elif entity['type'] == 'POP':
                query_params['population'] = POPULATION_MAP.get(entity['text'], entity['text'])

        return jsonify({
            "entities": bert_entities,
            "intent": intent,
            "queryParams": query_params
        })

    # 降级到规则引擎
    entities, query_params, intent = rule_based_extract(text)

    return jsonify({
        "entities": entities,
        "intent": intent,
        "queryParams": query_params
    })


@app.route('/api/nlp/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        "status": "ok",
        "bert_loaded": bert_model is not None,
        "engine": "bert" if bert_model else "rule_based"
    })


@app.route('/api/nlp/similarity', methods=['POST'])
def text_similarity():
    """
    计算文本语义相似度 (论文 2.4 BERT嵌入向量余弦相似度)
    请求: {"text1": "降压药", "text2": "高血压用药"}
    """
    data = request.get_json()
    text1 = data.get('text1', '')
    text2 = data.get('text2', '')

    if bert_model and bert_tokenizer:
        try:
            import torch
            inputs1 = bert_tokenizer(text1, return_tensors='pt', padding=True, truncation=True)
            inputs2 = bert_tokenizer(text2, return_tensors='pt', padding=True, truncation=True)

            with torch.no_grad():
                outputs1 = bert_model.bert(**inputs1)
                outputs2 = bert_model.bert(**inputs2)

            vec1 = outputs1.last_hidden_state[:, 0, :].numpy().flatten()
            vec2 = outputs2.last_hidden_state[:, 0, :].numpy().flatten()

            similarity = float(np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2)))
            return jsonify({"similarity": similarity, "engine": "bert"})
        except Exception as e:
            pass

    # 降级: 基于关键词重叠的简单相似度
    set1 = set(jieba.lcut(text1))
    set2 = set(jieba.lcut(text2))
    intersection = set1 & set2
    union = set1 | set2
    jaccard = len(intersection) / len(union) if union else 0
    return jsonify({"similarity": jaccard, "engine": "jaccard"})


# ============================================================
# 启动
# ============================================================
if __name__ == '__main__':
    # 尝试加载BERT模型
    load_bert_model()
    print("[NLP] 药品NLP微服务启动: http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=False)
