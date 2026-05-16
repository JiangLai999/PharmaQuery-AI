-- ============================================================
-- 药库药品基础信息查询系统 - MySQL 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS pharmacy_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmacy_db;

-- -----------------------------------------------------------
-- 1. 药品基础信息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS drug_info (
    drug_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '药品ID',
    generic_name    VARCHAR(200) NOT NULL COMMENT '通用名',
    trade_name      VARCHAR(200) DEFAULT NULL COMMENT '商品名',
    specification   VARCHAR(100) DEFAULT NULL COMMENT '规格',
    dosage_form     VARCHAR(50)  DEFAULT NULL COMMENT '剂型',
    manufacturer    VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
    approval_number VARCHAR(50)  DEFAULT NULL COMMENT '批准文号',
    barcode         VARCHAR(50)  DEFAULT NULL COMMENT '条形码',
    category        VARCHAR(50)  DEFAULT NULL COMMENT '药理分类',
    insurance_type  VARCHAR(20)  DEFAULT NULL COMMENT '医保类别(甲/乙/丙)',
    indication      TEXT         DEFAULT NULL COMMENT '适应症',
    contraindication TEXT        DEFAULT NULL COMMENT '禁忌症',
    interaction     TEXT         DEFAULT NULL COMMENT '药物相互作用',
    stock_quantity  INT          DEFAULT 0    COMMENT '当前库存量',
    stock_threshold INT          DEFAULT 50   COMMENT '补货阈值',
    unit_price      DECIMAL(10,2) DEFAULT 0   COMMENT '单价(元)',
    expiry_date     DATE         DEFAULT NULL COMMENT '最近批次有效期',
    shelf_life_days INT          DEFAULT 730  COMMENT '总有效期(天)',
    storage_condition VARCHAR(100) DEFAULT NULL COMMENT '储存条件',
    status          TINYINT      DEFAULT 1    COMMENT '状态(1正常 0停用)',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_generic_name (generic_name),
    INDEX idx_trade_name (trade_name),
    INDEX idx_approval_number (approval_number),
    INDEX idx_barcode (barcode),
    INDEX idx_category (category),
    INDEX idx_dosage_form (dosage_form),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品基础信息表';

-- -----------------------------------------------------------
-- 2. 用户信息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_info (
    user_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    openid      VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    department  VARCHAR(100) DEFAULT NULL COMMENT '科室',
    role_id     BIGINT       NOT NULL COMMENT '角色ID',
    avatar_url  VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      DEFAULT 1 COMMENT '状态(1正常 0禁用)',
    last_login  DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- -----------------------------------------------------------
-- 3. 角色表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    role_code   VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- -----------------------------------------------------------
-- 4. 权限表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS permission (
    perm_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name   VARCHAR(100) NOT NULL COMMENT '权限名称',
    perm_code   VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    resource    VARCHAR(200) DEFAULT NULL COMMENT '资源路径',
    action      VARCHAR(20)  DEFAULT NULL COMMENT '操作类型(READ/WRITE/DELETE)',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- -----------------------------------------------------------
-- 5. 角色-权限关联表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_permission (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id),
    FOREIGN KEY (perm_id) REFERENCES permission(perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- -----------------------------------------------------------
-- 6. 用户-药品交互记录表 (用于协同过滤)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_drug_interaction (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    drug_id     BIGINT NOT NULL,
    frequency   INT DEFAULT 1 COMMENT '查询次数(隐式评分)',
    last_query  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_drug_id (drug_id),
    UNIQUE KEY uk_user_drug (user_id, drug_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户药品交互记录';

-- -----------------------------------------------------------
-- 7. 初始角色数据
-- -----------------------------------------------------------
INSERT INTO role (role_name, role_code, description) VALUES
('药房管理员', 'PHARMACY_ADMIN', '药房管理员，拥有全部药品管理权限'),
('临床医生',   'DOCTOR',         '临床医生，可查询药品信息和获取推荐'),
('药师',       'PHARMACIST',     '药师，可查询和管理药品库存'),
('系统维护员', 'SYS_ADMIN',      '系统管理员，拥有全部系统权限');

-- -----------------------------------------------------------
-- 8. 初始权限数据
-- -----------------------------------------------------------
INSERT INTO permission (perm_name, perm_code, resource, action) VALUES
('查看药品信息',   'DRUG_INFO_READ',    '/api/drugs/**',     'READ'),
('编辑药品信息',   'DRUG_INFO_WRITE',   '/api/drugs/**',     'WRITE'),
('删除药品信息',   'DRUG_INFO_DELETE',  '/api/drugs/**',     'DELETE'),
('查看推荐结果',   'RECOMMEND_READ',    '/api/recommend/**', 'READ'),
('查看操作日志',   'LOG_READ',          '/api/logs/**',      'READ'),
('用户管理',       'USER_MANAGE',       '/api/users/**',     'WRITE'),
('查看库存信息',   'STOCK_READ',        '/api/stock/**',     'READ'),
('修改库存信息',   'STOCK_WRITE',       '/api/stock/**',     'WRITE');

-- -----------------------------------------------------------
-- 9. 角色-权限分配
-- -----------------------------------------------------------
-- 药房管理员: 全部权限
INSERT INTO role_permission (role_id, perm_id) SELECT 1, perm_id FROM permission;
-- 临床医生: 查看药品 + 推荐
INSERT INTO role_permission (role_id, perm_id) VALUES (2, 1), (2, 4);
-- 药师: 查看药品 + 推荐 + 库存
INSERT INTO role_permission (role_id, perm_id) VALUES (3, 1), (3, 4), (3, 7), (3, 8);
-- 系统维护员: 全部权限
INSERT INTO role_permission (role_id, perm_id) SELECT 4, perm_id FROM permission;

-- -----------------------------------------------------------
-- 10. 测试用户 (密码均为 BCrypt 加密的 "123456")
-- -----------------------------------------------------------
INSERT INTO user_info (username, password, real_name, department, role_id) VALUES
('admin',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '信息科',   4),
('pharmacist', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张药师',     '药剂科',   3),
('doctor01',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李医生',     '心内科',   2);

-- -----------------------------------------------------------
-- 11. 测试药品数据 (30条常见药品)
-- -----------------------------------------------------------
INSERT INTO drug_info (generic_name, trade_name, specification, dosage_form, manufacturer, approval_number, category, insurance_type, indication, contraindication, stock_quantity, stock_threshold, unit_price, expiry_date, shelf_life_days, storage_condition) VALUES
('阿莫西林胶囊', '阿莫仙', '0.5g*24粒', '胶囊剂', '珠海联邦制药', '国药准字H10983045', '抗感染药', '甲', '敏感菌所致的感染', '青霉素过敏者禁用', 500, 50, 12.50, '2026-06-15', 730, '密封，阴凉处保存'),
('苯磺酸氨氯地平片', '络活喜', '5mg*7片', '片剂', '辉瑞制药', '国药准字H10950224', '心血管系统药', '甲', '高血压、冠心病', '对本品过敏者禁用', 300, 30, 28.60, '2027-03-20', 1095, '遮光，密封保存'),
('二甲双胍缓释片', '格华止', '0.5g*20片', '缓释片', '中美上海施贵宝', '国药准字H20023370', '降血糖药', '甲', '2型糖尿病', '肾功能不全者禁用', 450, 40, 32.00, '2026-12-01', 730, '密封保存'),
('阿托伐他汀钙片', '立普妥', '20mg*7片', '片剂', '辉瑞制药', '国药准字H20051408', '调血脂药', '甲', '高胆固醇血症', '活动性肝病禁用', 200, 20, 42.50, '2027-01-15', 1095, '密封，30°C以下保存'),
('奥美拉唑肠溶胶囊', '洛赛克', '20mg*14粒', '肠溶胶囊', '阿斯利康', '国药准字H20030945', '消化系统药', '甲', '胃溃疡、反流性食管炎', '对本品过敏者禁用', 600, 60, 25.80, '2026-09-30', 730, '密封，阴凉处保存'),
('布洛芬缓释胶囊', '芬必得', '0.3g*20粒', '缓释胶囊', '中美天津史克', '国药准字H10900089', '解热镇痛药', '乙', '轻至中度疼痛、发热', '消化道溃疡活动期禁用', 800, 80, 15.60, '2026-08-20', 730, '密封保存'),
('头孢克洛缓释片', '可福乐', '0.375g*6片', '缓释片', '礼来制药', '国药准字H20040598', '抗感染药', '甲', '呼吸道及泌尿道感染', '头孢过敏者禁用', 350, 35, 38.00, '2027-02-28', 730, '密封保存'),
('氯雷他定片', '开瑞坦', '10mg*6片', '片剂', '拜耳医药', '国药准字H10970410', '抗过敏药', '乙', '过敏性鼻炎、荨麻疹', '对本品过敏者禁用', 700, 70, 18.90, '2026-11-15', 730, '遮光，密封保存'),
('硝苯地平控释片', '拜新同', '30mg*7片', '控释片', '拜耳医药', '国药准字H10950218', '心血管系统药', '甲', '高血压、心绞痛', '心源性休克禁用', 250, 25, 35.20, '2027-04-10', 1095, '遮光，密封保存'),
('盐酸二甲双胍片', '美迪康', '0.25g*48片', '片剂', '北京利龄恒泰', '国药准字H11021560', '降血糖药', '甲', '2型糖尿病', '酮症酸中毒禁用', 550, 55, 8.50, '2026-07-25', 730, '密封保存'),
('阿奇霉素分散片', '希舒美', '0.25g*6片', '分散片', '辉瑞制药', '国药准字H10960112', '抗感染药', '甲', '呼吸道感染、皮肤感染', '对大环内酯类过敏者禁用', 400, 40, 22.30, '2026-10-20', 730, '密封保存'),
('西格列汀片', '捷诺维', '100mg*7片', '片剂', '默沙东', '国药准字J20140095', '降血糖药', '乙', '2型糖尿病', '1型糖尿病禁用', 180, 18, 56.80, '2027-05-01', 1095, '密封，25°C以下保存'),
('氯吡格雷片', '波立维', '75mg*7片', '片剂', '赛诺菲', '国药准字J20180029', '抗血栓药', '甲', '动脉粥样硬化血栓', '活动性出血禁用', 220, 22, 68.50, '2027-06-15', 1095, '密封保存'),
('蒙脱石散', '思密达', '3g*10袋', '散剂', '博福-益普生', '国药准字H20000690', '消化系统药', '乙', '急慢性腹泻', '对本品过敏者禁用', 900, 90, 22.00, '2026-12-31', 730, '密封保存'),
('对乙酰氨基酚片', '泰诺林', '0.5g*20片', '片剂', '上海强生', '国药准字H31020800', '解热镇痛药', '甲', '发热、头痛', '严重肝肾功能不全禁用', 1000, 100, 9.80, '2026-08-15', 730, '密封保存'),
('缬沙坦胶囊', '代文', '80mg*7粒', '胶囊剂', '诺华制药', '国药准字H20040217', '心血管系统药', '甲', '高血压', '妊娠期禁用', 280, 28, 45.00, '2027-03-01', 1095, '密封保存'),
('左氧氟沙星片', '可乐必妥', '0.5g*6片', '片剂', '第一三共', '国药准字H20060093', '抗感染药', '甲', '呼吸道及泌尿道感染', '18岁以下禁用', 380, 38, 28.50, '2026-11-30', 730, '遮光，密封保存'),
('瑞舒伐他汀钙片', '可定', '10mg*7片', '片剂', '阿斯利康', '国药准字J20170008', '调血脂药', '甲', '高胆固醇血症', '活动性肝病禁用', 190, 19, 38.60, '2027-02-15', 1095, '密封保存'),
('甲钴胺片', '弥可保', '0.5mg*20片', '片剂', '卫材制药', '国药准字H20143107', '维生素类', '乙', '周围神经病变', '对本品过敏者禁用', 650, 65, 26.00, '2026-09-20', 730, '遮光，密封保存'),
('复方甘草酸苷片', '美能', '25mg*100片', '片剂', '卫材制药', '国药准字J20130077', '肝病辅助药', '乙', '慢性肝病', '醛固酮增多症禁用', 320, 32, 48.00, '2027-01-20', 1095, '密封保存'),
('盐酸曲马多缓释片', '奇曼丁', '100mg*10片', '缓释片', '萌蒂制药', '国药准字H20020530', '镇痛药', '乙', '中度至重度疼痛', '癫痫患者禁用', 100, 10, 35.00, '2026-10-15', 730, '密封保存'),
('格列美脲片', '亚莫利', '2mg*15片', '片剂', '赛诺菲', '国药准字H20010561', '降血糖药', '甲', '2型糖尿病', '1型糖尿病禁用', 260, 26, 30.50, '2027-04-20', 1095, '密封保存'),
('氨溴索口服溶液', '沐舒坦', '100ml:0.6g', '口服溶液', '勃林格殷格翰', '国药准字H20031314', '呼吸系统药', '乙', '痰液粘稠不易咳出', '对本品过敏者禁用', 500, 50, 22.80, '2026-07-30', 730, '密封保存'),
('碳酸钙D3片', '钙尔奇', '600mg*60片', '片剂', '惠氏制药', '国药准字H10950029', '矿物质类', '乙', '骨质疏松、钙缺乏', '高钙血症禁用', 750, 75, 45.00, '2027-06-01', 1095, '密封保存'),
('枸橼酸西地那非片', '万艾可', '100mg*1片', '片剂', '辉瑞制药', '国药准字H20020528', '泌尿系统药', '丙', '勃起功能障碍', '使用硝酸酯类药物者禁用', 80, 8, 128.00, '2027-08-15', 1095, '密封保存'),
('注射用头孢曲松钠', '罗氏芬', '1g/瓶', '注射用粉末', '罗氏制药', '国药准字H10983036', '抗感染药', '甲', '敏感菌所致严重感染', '头孢过敏者禁用', 400, 40, 18.50, '2026-06-30', 730, '遮光，密封保存'),
('盐酸氟西汀胶囊', '百忧解', '20mg*14粒', '胶囊剂', '礼来制药', '国药准字J20160029', '抗抑郁药', '乙', '抑郁症、强迫症', '与MAO抑制剂合用禁忌', 150, 15, 85.00, '2027-05-20', 1095, '密封保存'),
('吲达帕胺缓释片', '纳催离', '1.5mg*30片', '缓释片', '施维雅', '国药准字H20090387', '心血管系统药', '甲', '高血压', '严重肝肾功能不全禁用', 300, 30, 25.60, '2026-12-20', 730, '密封保存'),
('复方氨酚烷胺片', '感康', '12片/盒', '片剂', '吉林吴太感康', '国药准字H22026193', '感冒用药', '乙', '普通感冒及流行性感冒', '消化道溃疡者慎用', 1200, 120, 8.90, '2026-05-10', 730, '密封保存'),
('磷酸西格列汀二甲双胍片', '捷诺达', '50mg/500mg*14片', '片剂', '默沙东', '国药准字J20171025', '降血糖药', '乙', '2型糖尿病', '肾功能不全者禁用', 120, 12, 78.00, '2027-07-01', 1095, '密封，25°C以下保存');
