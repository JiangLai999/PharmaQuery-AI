const AppConfig = require('./config/app');

/**
 * 药库药品基础信息查询系统 - 小程序入口
 */
App({
  globalData: {
    baseUrl: AppConfig.apiBaseUrl,
    token: '',
    userInfo: null,
    userId: null,
    role: ''
  },

  updateRoleBasedTabBar() {
    const role = this.globalData.role || '';
    const text = role === 'SYS_ADMIN' ? '看板' : '推荐';

    try {
      wx.setTabBarItem({
        index: 1,
        text: text
      });
    } catch (e) {
      console.warn('更新 TabBar 文案失败:', e);
    }
  },

  onLaunch() {
    // 从本地缓存读取登录信息
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.userId = userInfo.userId || null;
      this.globalData.role = userInfo.role || '';
      this.updateRoleBasedTabBar();
    } else {
      // 未登录，跳转登录页
      wx.reLaunch({ url: '/pages/login/login' });
    }
  },

  /**
   * 检查登录状态，未登录则跳转登录页
   * 供各页面在 onShow 中调用
   */
  checkLogin() {
    if (!this.globalData.token) {
      wx.reLaunch({ url: '/pages/login/login' });
      return false;
    }
    return true;
  },

  /**
   * 保存登录信息
   */
  setLoginInfo(data) {
    this.globalData.token = data.token;
    this.globalData.userId = data.userId;
    this.globalData.role = data.role;
    this.globalData.userInfo = data;
    wx.setStorageSync('token', data.token);
    wx.setStorageSync('userInfo', data);
    this.updateRoleBasedTabBar();
  },

  /**
   * 清除登录信息
   */
  clearLoginInfo() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    this.globalData.userId = null;
    this.globalData.role = '';
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    this.updateRoleBasedTabBar();
  }
});
