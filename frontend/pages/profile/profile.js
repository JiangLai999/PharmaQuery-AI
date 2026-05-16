const api = require('../../utils/api');
const AppConfig = require('../../config/app');
const Theme = require('../../config/theme');
const util = require('../../utils/util');

Page({
  data: {
    userInfo: {},
    userInitial: 'U',
    roleText: '',
    drugCount: 0,
    permLabels: [],
    canViewStock: false,
    canViewLog: false,
    menuItems: [],
    appVersion: AppConfig.appVersion
  },

  onShow() {
    const app = getApp();
    if (!app.checkLogin()) return;

    const userInfo = app.globalData.userInfo || {};
    const role = app.globalData.role || '';

    const permissions = userInfo.permissions || [];
    const permLabels = permissions.map(function (p) {
      return Theme.permissions[p] || p;
    }).filter(Boolean);

    const isAdmin = util.isAdmin(role);
    const roleText = util.getRoleConfig(role).label;
    const canViewStock = permissions.includes('STOCK_READ');
    const canViewLog = permissions.includes('LOG_READ');

    this.setData({
      userInfo: userInfo,
      userInitial: this.getUserInitial(userInfo),
      roleText: roleText,
      permLabels: permLabels,
      canViewStock: canViewStock,
      canViewLog: canViewLog,
      menuItems: this.buildMenuItems(canViewStock, canViewLog)
    });

    this.loadStats();
  },

  async loadStats() {
    try {
      const res = await api.getCategoryStats();
      const list = Array.isArray(res) ? res : (res.data || []);
      let total = 0;
      list.forEach(item => { total += (item.cnt || 0); });
      this.setData({ drugCount: total });
    } catch (e) {}
  },

  buildMenuItems(canViewStock, canViewLog) {
    const items = [
      { key: 'search', icon: '检', title: '药品查询', desc: '检索药品基础资料', action: 'goSearch', visible: true },
      { key: 'expiry', icon: '期', title: '近效期药品', desc: '查看临期库存', action: 'goNearExpiry', visible: canViewStock },
      { key: 'stock', icon: '库', title: '低库存预警', desc: '关注补货风险', action: 'goLowStock', visible: canViewStock },
      { key: 'stockAdjust', icon: '调', title: '库存调整', desc: '快速修改库存与阈值', action: 'goStockAdjust', visible: canViewStock },
      { key: 'history', icon: '记', title: '操作日志', desc: '查看系统操作记录', action: 'goHistory', visible: canViewLog }
    ];

    return items.filter(function (item) {
      return item.visible;
    });
  },

  getUserInitial(userInfo) {
    const name = userInfo.realName || userInfo.username || 'U';
    return String(name).slice(0, 1);
  },

  onMenuTap(e) {
    const action = e.currentTarget.dataset.action;
    if (action && typeof this[action] === 'function') {
      this[action]();
    }
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/search' });
  },

  goNearExpiry() {
    wx.navigateTo({ url: '/pages/search/search?mode=nearExpiry' });
  },

  goLowStock() {
    wx.navigateTo({ url: '/pages/search/search?mode=lowStock' });
  },

  goStockAdjust() {
    wx.navigateTo({ url: '/pages/search/search?mode=stockAdjust' });
  },

  goHistory() {
    wx.showToast({ title: '日志查看功能开发中', icon: 'none' });
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (res.confirm) {
          const app = getApp();
          app.clearLoginInfo();
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
