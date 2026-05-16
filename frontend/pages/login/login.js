const api = require('../../utils/api');
const AppConfig = require('../../config/app');

Page({
  data: {
    username: '',
    password: '',
    appName: AppConfig.appName,
    appDesc: AppConfig.appDesc,
    appVersion: AppConfig.appVersion,
    placeholders: AppConfig.loginPlaceholders
  },

  completeLogin(loginData) {
    const app = getApp();
    app.setLoginInfo(loginData);
    wx.switchTab({
      url: '/pages/index/index',
      fail(err) {
        console.error('[login switchTab fail]', err);
        wx.reLaunch({ url: '/pages/index/index' });
      }
    });
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  async doLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }

    let loadingVisible = true;
    wx.showLoading({ title: '登录中' });
    try {
      const res = await api.login(username, password);
      if (res.code === 200 && res.data) {
        if (loadingVisible) {
          wx.hideLoading();
          loadingVisible = false;
        }
        this.completeLogin(res.data);
      } else {
        if (loadingVisible) {
          wx.hideLoading();
          loadingVisible = false;
        }
        wx.showToast({ title: res.message || '登录失败', icon: 'none' });
      }
    } catch (e) {
      if (loadingVisible) {
        wx.hideLoading();
        loadingVisible = false;
      }
      const errMsg = e && e.errMsg ? e.errMsg : '';
      wx.showToast({ title: errMsg.indexOf('timeout') > -1 ? '连接超时，请检查后端地址' : '登录失败', icon: 'none' });
    } finally {
      if (loadingVisible) {
        wx.hideLoading();
      }
    }
  }
});
