/**
 * 工具函数库
 */
const Theme = require('../config/theme');

/**
 * 格式化日期
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的日期
 */
const formatDate = (dateStr) => {
  if (!dateStr) return '--';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '--';
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

/**
 * 计算距离过期天数
 * @param {string} expiryDate - 过期日期
 * @returns {number|null} 剩余天数
 */
const daysUntilExpiry = (expiryDate) => {
  if (!expiryDate) return null;
  const now = new Date();
  const expiry = new Date(expiryDate);
  if (isNaN(expiry.getTime())) return null;
  const diff = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));
  return diff;
};

/**
 * 获取风险等级配置
 * @param {string} level - 风险等级
 * @returns {Object} 风险等级配置
 */
const getRiskConfig = (level) => {
  return Theme.riskLevels[level] || Theme.riskLevels.normal;
};

/**
 * 获取风险等级文字
 * @param {string} level - 风险等级
 * @returns {string} 风险等级文字
 */
const getRiskText = (level) => {
  return getRiskConfig(level).label;
};

/**
 * 获取医保类型配置
 * @param {string} type - 医保类型
 * @returns {Object} 医保类型配置
 */
const getInsuranceConfig = (type) => {
  return Theme.insuranceTypes[type] || { 
    label: '自费', 
    color: Theme.colors.warning, 
    bgColor: Theme.colors.warningLight 
  };
};

/**
 * 获取医保类别文字
 * @param {string} type - 医保类型
 * @returns {string} 医保类别文字
 */
const getInsuranceText = (type) => {
  return getInsuranceConfig(type).label;
};

/**
 * 获取角色配置
 * @param {string} role - 角色代码
 * @returns {Object} 角色配置
 */
const getRoleConfig = (role) => {
  return Theme.roles[role] || { label: '用户', color: Theme.colors.text.secondary };
};

/**
 * 防抖函数
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 延迟时间(毫秒)
 * @returns {Function} 防抖后的函数
 */
const debounce = (fn, delay = 500) => {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
};

/**
 * 节流函数
 * @param {Function} fn - 要执行的函数
 * @param {number} interval - 间隔时间(毫秒)
 * @returns {Function} 节流后的函数
 */
const throttle = (fn, interval = 1000) => {
  let lastTime = 0;
  return function (...args) {
    const now = Date.now();
    if (now - lastTime >= interval) {
      lastTime = now;
      fn.apply(this, args);
    }
  };
};

/**
 * 格式化价格
 * @param {number} price - 价格
 * @returns {string} 格式化后的价格
 */
const formatPrice = (price) => {
  if (price === null || price === undefined) return '--';
  const num = parseFloat(price);
  if (isNaN(num)) return '--';
  return num.toFixed(2);
};

/**
 * 格式化数字
 * @param {number} num - 数字
 * @returns {string} 格式化后的数字
 */
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0';
  return num.toString();
};

/**
 * 从响应中提取列表数据
 * @param {Object} res - API响应
 * @returns {Array} 列表数据
 */
const extractList = (res) => {
  if (!res) return [];
  if (Array.isArray(res)) return res;
  if (Array.isArray(res.data)) return res.data;
  if (res.data && Array.isArray(res.data.records)) return res.data.records;
  return [];
};

/**
 * 从响应中提取总数
 * @param {Object} res - API响应
 * @returns {number} 总数
 */
const extractTotal = (res) => {
  if (!res) return 0;
  if (Array.isArray(res)) return res.length;
  if (res.data && typeof res.data.total === 'number') return res.data.total;
  return extractList(res).length;
};

/**
 * 检查权限
 * @param {string} role - 当前角色
 * @param {Array} permissions - 当前权限列表
 * @param {string} requiredPermission - 需要的权限
 * @returns {boolean} 是否有权限
 */
const hasPermission = (role, permissions, requiredPermission) => {
  if (!permissions || !Array.isArray(permissions)) return false;
  return permissions.includes(requiredPermission);
};

/**
 * 检查是否为管理员
 * @param {string} role - 当前角色
 * @returns {boolean} 是否为管理员
 */
const isAdmin = (role) => {
  return role === 'PHARMACY_ADMIN' || role === 'SYS_ADMIN';
};

/**
 * 检查是否为药师
 * @param {string} role - 当前角色
 * @returns {boolean} 是否为药师
 */
const isPharmacist = (role) => {
  return role === 'PHARMACIST';
};

/**
 * 检查是否为医生
 * @param {string} role - 当前角色
 * @returns {boolean} 是否为医生
 */
const isDoctor = (role) => {
  return role === 'DOCTOR';
};

/**
 * 获取NLP实体类型文字
 * @param {string} type - 实体类型
 * @returns {string} 类型文字
 */
const getNlpEntityTypeText = (type) => {
  const typeMap = {
    'SYMPTOM': '症状',
    'DRUG': '药品',
    'CATEGORY': '分类',
    'DOSAGE_FORM': '剂型',
    'POPULATION': '人群'
  };
  return typeMap[type] || type;
};

/**
 * 深拷贝对象
 * @param {Object} obj - 要拷贝的对象
 * @returns {Object} 拷贝后的对象
 */
const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime());
  if (Array.isArray(obj)) return obj.map(item => deepClone(item));
  const cloned = {};
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      cloned[key] = deepClone(obj[key]);
    }
  }
  return cloned;
};

/**
 * 合并对象
 * @param {Object} target - 目标对象
 * @param {Object} source - 源对象
 * @returns {Object} 合并后的对象
 */
const merge = (target, source) => {
  return Object.assign({}, target, source);
};

module.exports = {
  // 日期相关
  formatDate,
  daysUntilExpiry,
  
  // 配置相关
  getRiskConfig,
  getRiskText,
  getInsuranceConfig,
  getInsuranceText,
  getRoleConfig,
  
  // 函数工具
  debounce,
  throttle,
  
  // 格式化
  formatPrice,
  formatNumber,
  
  // 数据处理
  extractList,
  extractTotal,
  
  // 权限检查
  hasPermission,
  isAdmin,
  isPharmacist,
  isDoctor,
  
  // NLP相关
  getNlpEntityTypeText,
  
  // 对象操作
  deepClone,
  merge
};
