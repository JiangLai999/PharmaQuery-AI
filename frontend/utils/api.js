/**
 * 网络请求工具类
 * 封装wx.request, 自动携带JWT Token
 */
const AppConfig = require('../config/app');
const app = getApp();

const shouldLogNetwork = !!AppConfig.debugNetwork;

const request = (options) => {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data = {} } = options;
    const requestUrl = app.globalData.baseUrl + url;
    const startTime = Date.now();

    if (shouldLogNetwork) {
      console.log('[API request]', {
        method: method,
        url: requestUrl,
        data: data
      });
    }

    wx.request({
      url: requestUrl,
      method: method,
      data: data,
      timeout: 10000,
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
      },
      success(res) {
        const duration = Date.now() - startTime;
        if (shouldLogNetwork) {
          console.log('[API response]', {
            method: method,
            url: requestUrl,
            statusCode: res.statusCode,
            duration: duration + 'ms',
            data: res.data
          });
        }

        if (res.statusCode === 200) {
          resolve(res.data);
        } else if (res.statusCode === 401 || res.statusCode === 403) {
          if (shouldLogNetwork) {
            console.warn('请求需要认证:', url, res.statusCode);
          }
          resolve({ code: res.statusCode, data: [], message: '需要登录' });
        } else {
          const errMsg = (res.data && res.data.message) || '请求失败(' + res.statusCode + ')';
          if (shouldLogNetwork) {
            console.error('接口错误:', url, res.statusCode, errMsg);
          }
          resolve({ code: res.statusCode, data: [], message: errMsg });
        }
      },
      fail(err) {
        const duration = Date.now() - startTime;
        if (shouldLogNetwork) {
          console.error('[API fail]', {
            method: method,
            url: requestUrl,
            duration: duration + 'ms',
            errMsg: err.errMsg,
            error: err
          });
        }
        reject(err);
      }
    });
  });
};

/**
 * 药品搜索 (模糊)
 */
const searchDrugs = (keyword) => {
  return request({ url: '/drugs/fuzzy?keyword=' + encodeURIComponent(keyword) });
};

/**
 * NLP语义搜索
 */
const nlpSearch = (query) => {
  return request({ url: '/drugs/nlp-search?q=' + encodeURIComponent(query), method: 'POST' });
};

/**
 * 多条件组合查询
 */
const advancedSearch = (params) => {
  return request({ url: '/drugs/search', method: 'POST', data: params });
};

/**
 * 获取药品详情
 */
const getDrugDetail = (drugId) => {
  return request({ url: '/drugs/' + drugId });
};

/**
 * 新增药品
 */
const createDrug = (drug) => {
  return request({ url: '/drugs', method: 'POST', data: drug });
};

/**
 * 更新药品
 */
const updateDrug = (drugId, drug) => {
  return request({ url: '/drugs/' + drugId, method: 'PUT', data: drug });
};

/**
 * 删除药品
 */
const deleteDrug = (drugId) => {
  return request({ url: '/drugs/' + drugId, method: 'DELETE' });
};

/**
 * 更新库存
 */
const updateDrugStock = (drugId, quantity) => {
  return request({ url: '/drugs/' + drugId + '/stock?quantity=' + quantity, method: 'PUT' });
};

/**
 * 获取推荐列表
 */
const getRecommendations = (userId, topK = 8) => {
  return request({ url: '/recommend/' + userId + '?topK=' + topK });
};

/**
 * 记录查看行为
 */
const recordView = (drugId, userId) => {
  return request({ url: '/drugs/' + drugId + '/view?userId=' + userId, method: 'POST' });
};

/**
 * 登录
 */
const login = (username, password) => {
  return request({ url: '/auth/login', method: 'POST', data: { username, password } });
};

/**
 * 微信登录
 */
const wxLogin = (code) => {
  return request({ url: '/auth/wx-login', method: 'POST', data: { wxCode: code } });
};

/**
 * 获取近效期药品
 */
const getNearExpiry = () => {
  return request({ url: '/drugs/near-expiry' });
};

/**
 * 获取低库存药品
 */
const getLowStock = () => {
  return request({ url: '/drugs/low-stock' });
};

/**
 * 分类统计
 */
const getCategoryStats = () => {
  return request({ url: '/drugs/stats/category' });
};

module.exports = {
  request,
  searchDrugs,
  nlpSearch,
  advancedSearch,
  getDrugDetail,
  createDrug,
  updateDrug,
  deleteDrug,
  updateDrugStock,
  getRecommendations,
  recordView,
  login,
  wxLogin,
  getNearExpiry,
  getLowStock,
  getCategoryStats
};
