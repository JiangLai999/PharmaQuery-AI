// ============================================================
// 药库药品基础信息查询系统 - MongoDB 初始化脚本
// 使用方法: mongosh < init_mongo.js
// ============================================================

db = db.getSiblingDB('pharmacy_db');

// -----------------------------------------------------------
// 1. 操作日志集合
// -----------------------------------------------------------
db.createCollection('operation_logs');
db.operation_logs.createIndex({ userId: 1, timestamp: -1 });
db.operation_logs.createIndex({ action: 1 });
db.operation_logs.createIndex({ timestamp: -1 }, { expireAfterSeconds: 7776000 }); // 90天过期

// -----------------------------------------------------------
// 2. 查询日志集合 (用于协同过滤)
// -----------------------------------------------------------
db.createCollection('drug_logs');
db.drug_logs.createIndex({ userId: 1, timestamp: -1 });
db.drug_logs.createIndex({ query: 'text' });

// -----------------------------------------------------------
// 3. 药品说明书文档集合
// -----------------------------------------------------------
db.createCollection('drug_documents');
db.drug_documents.createIndex({ drugId: 1 }, { unique: true });
db.drug_documents.createIndex({ '$**': 'text' });

// -----------------------------------------------------------
// 4. NLP语义向量集合
// -----------------------------------------------------------
db.createCollection('nlp_vectors');
db.nlp_vectors.createIndex({ drugId: 1 }, { unique: true });

// -----------------------------------------------------------
// 5. 插入示例说明书文档
// -----------------------------------------------------------
db.drug_documents.insertMany([
  {
    drugId: 1,
    genericName: '阿莫西林胶囊',
    fullText: '【药品名称】阿莫西林胶囊\n【成份】本品主要成份为阿莫西林。\n【适应症】阿莫西林适用于敏感菌（不产β-内酰胺酶菌株）所致的下列感染：1.溶血链球菌、肺炎链球菌、葡萄球菌或流感嗜血杆菌所致中耳炎、鼻窦炎、咽炎、扁桃体炎等上呼吸道感染。2.大肠埃希菌、奇异变形杆菌或粪肠球菌所致的泌尿生殖道感染。3.溶血链球菌、葡萄球菌或大肠埃希菌所致的皮肤软组织感染。4.溶血链球菌、肺炎链球菌、葡萄球菌或流感嗜血杆菌所致急性支气管炎、肺炎等下呼吸道感染。\n【用法用量】口服。成人一次0.5g，每6～8小时1次，一日剂量不超过4g。\n【不良反应】恶心、呕吐、腹泻及假膜性肠炎等胃肠道反应。\n【禁忌】青霉素过敏及青霉素皮肤试验阳性患者禁用。',
    pdfUrl: null,
    imageUrls: [],
    updatedAt: new Date()
  },
  {
    drugId: 2,
    genericName: '苯磺酸氨氯地平片',
    fullText: '【药品名称】苯磺酸氨氯地平片\n【成份】本品主要成份为苯磺酸氨氯地平。\n【适应症】1.高血压（可单独使用或与其他抗高血压药物合用）。2.慢性稳定性心绞痛及变异型心绞痛（可单独使用或与其他抗心绞痛药物合用）。\n【用法用量】通常口服起始剂量为5mg，每日一次，最大剂量为10mg，每日一次。\n【不良反应】头痛、水肿、疲劳、失眠、恶心、腹痛、面红、心悸和头晕。\n【禁忌】对本品过敏者禁用。',
    pdfUrl: null,
    imageUrls: [],
    updatedAt: new Date()
  }
]);

// -----------------------------------------------------------
// 6. 插入示例操作日志
// -----------------------------------------------------------
db.operation_logs.insertMany([
  { userId: 'doctor01', role: 'DOCTOR', action: 'DRUG_QUERY', resource: '阿莫西林', ip: '192.168.1.100', timestamp: new Date() },
  { userId: 'pharmacist', role: 'PHARMACIST', action: 'STOCK_CHECK', resource: 'drug_id:1', ip: '192.168.1.101', timestamp: new Date() }
]);

print('MongoDB 初始化完成');
