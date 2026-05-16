const api = require('../../utils/api');

Page({
  data: {
    loading: true,
    submitting: false,
    drugId: null,
    drug: {},
    quantity: '',
    stockThreshold: '',
    adjustmentNote: '',
    suggestedFixedQuantity: 100,
    currentStockStatusText: '--',
    currentIsLowStock: false
  },

  updateStockStatusPreview() {
    const quantity = Number(this.data.quantity || 0);
    const stockThreshold = Number(this.data.stockThreshold || 0);
    const currentIsLowStock = stockThreshold > 0 ? quantity <= stockThreshold : false;
    const currentStockStatusText = stockThreshold > 0
      ? (currentIsLowStock ? '调整后仍将处于低库存状态' : '调整后将恢复到正常库存状态')
      : '设置补货阈值后可启用低库存判断';

    this.setData({ currentIsLowStock, currentStockStatusText });
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ drugId: Number(options.id) });
      this.loadDrugDetail(options.id);
    } else {
      this.setData({ loading: false });
      wx.showToast({ title: '缺少药品信息', icon: 'none' });
    }
  },

  async loadDrugDetail(drugId) {
    this.setData({ loading: true });
    try {
      const res = await api.getDrugDetail(drugId);
      const drug = res.data || res;
      this.setData({
        drug: drug,
        quantity: drug.stockQuantity !== undefined && drug.stockQuantity !== null ? String(drug.stockQuantity) : '',
        stockThreshold: drug.stockThreshold !== undefined && drug.stockThreshold !== null ? String(drug.stockThreshold) : '',
        loading: false
      }, () => {
        this.updateStockStatusPreview();
      });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载药品失败', icon: 'none' });
    }
  },

  onQuantityInput(e) {
    this.setData({ quantity: e.detail.value }, () => {
      this.updateStockStatusPreview();
    });
  },

  onNoteInput(e) {
    this.setData({ adjustmentNote: e.detail.value });
  },

  onThresholdInput(e) {
    this.setData({ stockThreshold: e.detail.value }, () => {
      this.updateStockStatusPreview();
    });
  },

  fillSuggestedQuantity(e) {
    const type = e.currentTarget.dataset.type;
    const threshold = Number(this.data.stockThreshold || 0);
    let value = Number(e.currentTarget.dataset.value || 0);

    if (type === 'threshold') {
      value = threshold;
    } else if (type === 'doubleThreshold') {
      value = threshold * 2;
    }

    this.setData({ quantity: String(value) }, () => {
      this.updateStockStatusPreview();
    });
  },

  validateForm() {
    if (this.data.quantity === '') return '请输入库存数量';
    const quantity = Number(this.data.quantity);
    if (!Number.isInteger(quantity) || quantity < 0) return '库存数量必须为大于等于 0 的整数';

    if (this.data.stockThreshold === '') return '请输入补货阈值';
    const stockThreshold = Number(this.data.stockThreshold);
    if (!Number.isInteger(stockThreshold) || stockThreshold < 0) return '补货阈值必须为大于等于 0 的整数';

    return '';
  },

  async submitStockAdjust() {
    const errorText = this.validateForm();
    if (errorText) {
      wx.showToast({ title: errorText, icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const quantity = Number(this.data.quantity);
      const stockThreshold = Number(this.data.stockThreshold);

      const stockRes = await api.updateDrugStock(this.data.drugId, quantity);
      if (!stockRes || stockRes.code !== 200) {
        throw new Error((stockRes && stockRes.message) || '库存更新失败');
      }

      const updateRes = await api.updateDrug(this.data.drugId, {
        drugId: this.data.drugId,
        genericName: this.data.drug.genericName,
        tradeName: this.data.drug.tradeName,
        specification: this.data.drug.specification,
        dosageForm: this.data.drug.dosageForm,
        manufacturer: this.data.drug.manufacturer,
        approvalNumber: this.data.drug.approvalNumber,
        barcode: this.data.drug.barcode,
        category: this.data.drug.category,
        insuranceType: this.data.drug.insuranceType,
        indication: this.data.drug.indication,
        contraindication: this.data.drug.contraindication,
        interaction: this.data.drug.interaction,
        stockQuantity: quantity,
        stockThreshold: stockThreshold,
        unitPrice: this.data.drug.unitPrice,
        expiryDate: this.data.drug.expiryDate,
        shelfLifeDays: this.data.drug.shelfLifeDays,
        storageCondition: this.data.drug.storageCondition,
        status: this.data.drug.status
      });

      if (!updateRes || updateRes.code !== 200) {
        throw new Error((updateRes && updateRes.message) || '补货阈值保存失败');
      }

      const detailRes = await api.getDrugDetail(this.data.drugId);
      const latestDrug = detailRes.data || detailRes;
      if (Number(latestDrug.stockThreshold || 0) !== stockThreshold) {
        throw new Error('补货阈值未成功保存');
      }

      wx.showToast({ title: '库存与阈值已更新', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack({ delta: 1 });
      }, 600);
    } catch (e) {
      console.error('[stock adjust submit fail]', e);
      wx.showToast({ title: e.message || '更新库存失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
