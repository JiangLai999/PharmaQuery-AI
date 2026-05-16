const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    hotDrugs: [],
    alerts: [],
    alertSummary: [],
    categories: [],
    loading: false,
    roleName: '',
    realName: '',
    greeting: '欢迎使用药库工作台',
    canViewStock: false,
    isAdminView: false,
    canAddDrug: false,
    canEditDrug: false,
    canDeleteDrug: false,
    quickActions: [],
    primaryActions: [
      { key: 'nlp', icon: 'AI', title: '智能检索', desc: '自然语言查询', type: 'nlp', visible: true },
      { key: 'category', icon: '分', title: '分类浏览', desc: '按药理分类查看', action: 'goCategory', visible: true }
    ],
    secondaryActions: []
  },

  onLoad() {},

  onReady() {
    if (getApp().checkLogin()) {
      this.initPermissions();
      this.loadData();
    }
  },

  onShow() {
    if (getApp().checkLogin()) {
      this.initPermissions();
    }
  },

  initPermissions() {
    const app = getApp();
    const role = app.globalData.role || '';
    const userInfo = app.globalData.userInfo || {};
    const permissions = userInfo.permissions || [];
    const roleConfig = util.getRoleConfig(role);
    const isAdmin = util.isAdmin(role);
    const greeting = this.getGreeting(roleConfig.label, userInfo.realName || userInfo.username || '');
    const canViewStock = permissions.includes('STOCK_READ');
    const canAddDrug = permissions.includes('DRUG_INFO_WRITE');
    const canEditDrug = permissions.includes('DRUG_INFO_WRITE');
    const canDeleteDrug = permissions.includes('DRUG_INFO_DELETE');

    this.setData({
      roleName: roleConfig.label,
      realName: userInfo.realName || userInfo.username || '',
      greeting,
      canViewStock: canViewStock,
      isAdminView: isAdmin,
      canAddDrug: canAddDrug,
      canEditDrug: canEditDrug,
      canDeleteDrug: canDeleteDrug,
      secondaryActions: this.buildSecondaryActions(canViewStock, canAddDrug),
      quickActions: this.data.primaryActions.concat(this.buildSecondaryActions(canViewStock, canAddDrug))
    });
  },

  getGreeting(roleName, realName) {
    if (realName) {
      return realName + '，今天也来高效管理药品信息';
    }
    return roleName + '，欢迎回到药库工作台';
  },

  buildSecondaryActions(canViewStock, canAddDrug) {
    const actions = [
      { key: 'expiry', icon: '期', title: '近效期', desc: '查看临期药品', action: 'goNearExpiry', visible: canViewStock },
      { key: 'stock', icon: '库', title: '低库存', desc: '掌握库存预警', action: 'goLowStock', visible: canViewStock },
      { key: 'add', icon: '+', title: '新增药品', desc: '录入基础资料', action: 'goAddDrug', visible: canAddDrug }
    ];

    return actions.filter(function (item) {
      return item.visible;
    });
  },

  onPullDownRefresh() {
    this.loadData();
    wx.stopPullDownRefresh();
  },

  loadData() {
    this.setData({ loading: true });
    this.loadCategories();
    if (this.data.canViewStock) {
      this.loadAlerts();
    }
    this.loadHotDrugs();
  },

  loadCategories() {
    api.getCategoryStats().then(res => {
      const list = util.extractList(res).slice(0, 6).map(function (item) {
        return {
          category: item.category || '未分类',
          cnt: item.cnt || 0
        };
      });
      this.setData({ categories: list });
    }).catch(() => {});
  },

  loadAlerts() {
    Promise.all([
      api.getNearExpiry().catch(() => ({ data: [] })),
      api.getLowStock().catch(() => ({ data: [] }))
    ]).then(([nearExpiryRes, lowStockRes]) => {
      const nearExpiryList = util.extractList(nearExpiryRes);
      const lowStockList = util.extractList(lowStockRes);
      const alertMap = {};

      nearExpiryList.forEach(function (item) {
        const drugId = item.drugId;
        if (!alertMap[drugId]) {
          alertMap[drugId] = Object.assign({}, item, { alertReasons: [] });
        }
        alertMap[drugId].alertReasons.push('nearExpiry');
      });

      lowStockList.forEach(function (item) {
        const drugId = item.drugId;
        if (!alertMap[drugId]) {
          alertMap[drugId] = Object.assign({}, item, { alertReasons: [] });
        }
        alertMap[drugId].alertReasons.push('lowStock');
      });

      const mergedAlerts = Object.keys(alertMap).map(function (key) {
        const item = alertMap[key];
        const reasons = item.alertReasons || [];
        const riskLevel = reasons.length > 1 ? 'danger' : (item.riskLevel || 'warning');
        return Object.assign({}, item, {
          riskLevel: riskLevel,
          riskText: reasons.length > 1 ? '双重预警' : util.getRiskText(riskLevel),
          alertLabel: reasons.length > 1 ? '效期+库存' : (reasons[0] === 'lowStock' ? '低库存' : '近效期'),
          alertDesc: reasons.length > 1
            ? '同时存在近效期和低库存风险'
            : (reasons[0] === 'lowStock' ? '建议尽快补货并复核库存' : '建议优先处理临期药品')
        });
      }).sort(function (a, b) {
        const levelWeight = { danger: 3, warning: 2, normal: 1 };
        return (levelWeight[b.riskLevel] || 0) - (levelWeight[a.riskLevel] || 0);
      });

      const alertSummary = [
        { key: 'nearExpiry', title: '近效期', value: nearExpiryList.length, desc: '需优先处理临期药品' },
        { key: 'lowStock', title: '低库存', value: lowStockList.length, desc: '需关注补货与库存复核' },
        { key: 'critical', title: '双重预警', value: mergedAlerts.filter(function (item) { return item.alertReasons.length > 1; }).length, desc: '同时存在效期与库存风险' }
      ];

      this.setData({
        alerts: mergedAlerts.slice(0, 5),
        alertSummary: alertSummary
      });
    }).catch(() => {});
  },

  loadHotDrugs() {
    api.advancedSearch({ pageNum: 1, pageSize: 10 }).then(res => {
      const data = res.data || res;
      const list = (data.records || util.extractList(data)).slice(0, 10);
      this.setData({ hotDrugs: list, loading: false });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  goSearch(e) {
    const type = e.currentTarget.dataset.type || '';
    wx.navigateTo({ url: '/pages/search/search?mode=' + type });
  },

  onQuickActionTap(e) {
    const action = e.currentTarget.dataset.action;
    const type = e.currentTarget.dataset.type;

    if (type) {
      this.goSearch({ currentTarget: { dataset: { type: type } } });
      return;
    }

    if (action && typeof this[action] === 'function') {
      this[action]();
    }
  },

  goNearExpiry() {
    wx.navigateTo({ url: '/pages/search/search?mode=nearExpiry' });
  },

  goLowStock() {
    wx.navigateTo({ url: '/pages/search/search?mode=lowStock' });
  },

  goCategory() {
    wx.navigateTo({ url: '/pages/search/search?mode=category' });
  },

  goAddDrug() {
    wx.navigateTo({ url: '/pages/drug-form/drug-form?mode=create' });
  },

  searchByCategory(e) {
    const category = e.currentTarget.dataset.category;
    wx.navigateTo({ url: '/pages/search/search?category=' + encodeURIComponent(category) });
  }
});
