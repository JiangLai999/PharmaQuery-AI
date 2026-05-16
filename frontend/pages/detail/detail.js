const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    drug: {},
    expiryDateStr: '--',
    expiryDays: null,
    stockStatusText: '--',
    isLowStock: false,
    showIndication: true,
    showContra: true,
    showInteraction: false,
    canEdit: false,
    canAdjustStock: false,
    canDelete: false,
    isDoctor: false,
    insuranceText: '--',
    riskText: '正常'
  },

  onLoad(options) {
    this.initPermissions();
    if (options.id) {
      this.drugId = options.id;
      this.loadDrugDetail(options.id);
    }
  },

  onShow() {
    if (this.drugId) {
      this.loadDrugDetail(this.drugId);
    }
  },

  initPermissions() {
    const app = getApp();
    const role = app.globalData.role || '';
    const permissions = (app.globalData.userInfo && app.globalData.userInfo.permissions) || [];

    this.setData({
      canEdit: permissions.includes('DRUG_INFO_WRITE'),
      canDelete: permissions.includes('DRUG_INFO_DELETE'),
      canAdjustStock: permissions.includes('STOCK_WRITE'),
      isDoctor: util.isDoctor(role)
    });
  },

  async loadDrugDetail(drugId) {
    wx.showLoading({ title: '加载中' });
    try {
      const res = await api.getDrugDetail(drugId);
      const drug = res.data || res;

      const expiryDays = util.daysUntilExpiry(drug.expiryDate);
      const expiryDateStr = util.formatDate(drug.expiryDate);
      const insuranceText = util.getInsuranceText(drug.insuranceType);
      const riskText = util.getRiskText(drug.riskLevel || 'normal');
      const stockQuantity = Number(drug.stockQuantity || 0);
      const stockThreshold = Number(drug.stockThreshold || 0);
      const isLowStock = stockQuantity <= stockThreshold;
      const stockStatusText = stockThreshold > 0
        ? (isLowStock ? '低于补货阈值，当前属于低库存' : '高于补货阈值，当前库存正常')
        : '未设置补货阈值';

      this.setData({
        drug,
        expiryDateStr,
        expiryDays,
        insuranceText,
        riskText,
        isLowStock,
        stockStatusText
      });
      wx.setNavigationBarTitle({ title: drug.genericName || '药品详情' });

      const app = getApp();
      if (app.globalData.userId) {
        api.recordView(drugId, app.globalData.userId).catch(() => {});
      }
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  toggleIndication() {
    this.setData({ showIndication: !this.data.showIndication });
  },

  toggleContra() {
    this.setData({ showContra: !this.data.showContra });
  },

  toggleInteraction() {
    this.setData({ showInteraction: !this.data.showInteraction });
  },

  onEditDrug() {
    if (!this.data.drug.drugId) return;
    wx.navigateTo({
      url: '/pages/drug-form/drug-form?mode=edit&id=' + this.data.drug.drugId
    });
  },

  onAdjustStock() {
    if (!this.data.drug.drugId) return;
    wx.navigateTo({
      url: '/pages/stock-adjust/stock-adjust?id=' + this.data.drug.drugId
    });
  },

  onDeleteDrug() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除药品"' + this.data.drug.genericName + '"吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteDrug(this.data.drug.drugId);
            wx.showToast({ title: '删除成功', icon: 'success' });
            setTimeout(() => {
              wx.navigateBack({ delta: 1 });
            }, 500);
          } catch (e) {
            wx.showToast({ title: '删除失败', icon: 'none' });
          }
        }
      }
    });
  }
});
