const api = require('../../utils/api');

Page({
  data: {
    isAdminView: false,
    pageTitle: '个性化推荐',
    pageDesc: '结合您的查询历史，优先呈现更相关的药品资料',
    heroChip: 'AI Recommend',
    allRecommendations: [],
    recommendations: [],
    dashboardCards: [],
    focusAlerts: [],
    topCategories: [],
    loading: true,
    pageIndex: 0,
    pageSize: 4
  },

  onShow() {
    if (getApp().checkLogin()) {
      this.loadPageData();
    }
  },

  onPullDownRefresh() {
    this.loadPageData().then(() => wx.stopPullDownRefresh());
  },

  loadPageData() {
    const app = getApp();
    const permissions = (app.globalData.userInfo && app.globalData.userInfo.permissions) || [];
    const isAdminView = app.globalData.role === 'SYS_ADMIN';
    const canReadRecommend = permissions.includes('RECOMMEND_READ') && !isAdminView;

    this.setData({
      isAdminView: isAdminView,
      pageTitle: isAdminView ? '管理看板' : '个性化推荐',
      pageDesc: isAdminView ? '汇总分类分布、库存风险与重点预警，便于快速掌握系统运行状态' : '结合您的查询历史，优先呈现更相关的药品资料',
      heroChip: isAdminView ? 'System Overview' : 'AI Recommend'
    });

    wx.setNavigationBarTitle({ title: isAdminView ? '管理看板' : '个性化推荐' });
    if (isAdminView) return this.loadAdminDashboard();
    if (!canReadRecommend) {
      this.setData({ loading: false, allRecommendations: [], recommendations: [], pageIndex: 0 });
      return Promise.resolve();
    }
    return this.loadRecommendations();
  },

  getCurrentPageItems(list, pageIndex, pageSize) {
    if (!Array.isArray(list) || list.length === 0) return [];
    const totalPages = Math.ceil(list.length / pageSize);
    const safePageIndex = totalPages === 0 ? 0 : (pageIndex % totalPages);
    const start = safePageIndex * pageSize;
    const end = start + pageSize;
    return list.slice(start, end);
  },

  refreshBatch() {
    if (this.data.isAdminView) {
      this.loadAdminDashboard();
      return;
    }

    const allRecommendations = this.data.allRecommendations || [];
    const pageSize = this.data.pageSize;

    if (allRecommendations.length === 0) {
      this.setData({ recommendations: [], pageIndex: 0 });
      return;
    }

    const totalPages = Math.ceil(allRecommendations.length / pageSize);
    if (totalPages <= 1) {
      this.setData({
        recommendations: allRecommendations.slice(0, pageSize),
        pageIndex: 0
      });
      wx.showToast({ title: '暂无更多推荐', icon: 'none' });
      return;
    }

    const nextPageIndex = (this.data.pageIndex + 1) % totalPages;
    this.setData({
      pageIndex: nextPageIndex,
      recommendations: this.getCurrentPageItems(allRecommendations, nextPageIndex, pageSize)
    });
  },

  async loadAdminDashboard() {
    this.setData({ loading: true });
    try {
      const [categoryRes, nearExpiryRes, lowStockRes] = await Promise.all([
        api.getCategoryStats(),
        api.getNearExpiry(),
        api.getLowStock()
      ]);

      const categories = categoryRes.data || [];
      const nearExpiry = nearExpiryRes.data || [];
      const lowStock = lowStockRes.data || [];
      const mergedMap = {};

      nearExpiry.forEach(function (item) {
        mergedMap[item.drugId] = Object.assign({}, item, { reason: '近效期' });
      });

      lowStock.forEach(function (item) {
        if (mergedMap[item.drugId]) {
          mergedMap[item.drugId].reason = '效期+库存';
          mergedMap[item.drugId].riskLevel = 'danger';
        } else {
          mergedMap[item.drugId] = Object.assign({}, item, { reason: '低库存' });
        }
      });

      const focusAlerts = Object.values(mergedMap).slice(0, 5);
      const dashboardCards = [
        { key: 'category', title: '药品分类', value: categories.length, desc: '当前已覆盖分类数' },
        { key: 'expiry', title: '近效期', value: nearExpiry.length, desc: '需优先处理临期药品' },
        { key: 'stock', title: '低库存', value: lowStock.length, desc: '需关注补货与盘点' }
      ];

      this.setData({
        dashboardCards: dashboardCards,
        focusAlerts: focusAlerts,
        topCategories: categories.slice(0, 6),
        loading: false
      });
    } catch (e) {
      console.error('获取管理看板失败:', e);
      this.setData({
        dashboardCards: [],
        focusAlerts: [],
        topCategories: [],
        loading: false
      });
    }
  },

  async loadRecommendations() {
    const app = getApp();
    if (!app.globalData.userId) {
      this.setData({ loading: false, allRecommendations: [], recommendations: [], pageIndex: 0 });
      return;
    }

    this.setData({ loading: true });
    try {
      const res = await api.getRecommendations(app.globalData.userId, 12);
      const allRecommendations = res.data || [];
      this.setData({
        allRecommendations: allRecommendations,
        recommendations: this.getCurrentPageItems(allRecommendations, 0, this.data.pageSize),
        pageIndex: 0,
        loading: false
      });
    } catch (e) {
      console.error('获取推荐失败:', e);
      this.setData({ loading: false, allRecommendations: [], recommendations: [], pageIndex: 0 });
    }
  }
});
