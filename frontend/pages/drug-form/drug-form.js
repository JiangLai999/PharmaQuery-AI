const api = require('../../utils/api');
const Theme = require('../../config/theme');

const createDefaultForm = () => ({
  genericName: '',
  tradeName: '',
  specification: '',
  dosageForm: '',
  manufacturer: '',
  approvalNumber: '',
  barcode: '',
  category: '',
  insuranceType: '甲',
  indication: '',
  contraindication: '',
  interaction: '',
  stockQuantity: '',
  stockThreshold: '',
  unitPrice: '',
  expiryDate: '',
  shelfLifeDays: '',
  storageCondition: '',
  status: 1
});

Page({
  data: {
    mode: 'create',
    drugId: null,
    loading: false,
    submitting: false,
    form: createDefaultForm(),
    insuranceOptions: Theme.insuranceOptions.slice(1),
    dosageOptions: Theme.dosageForms.slice(1),
    categoryOptions: Theme.categoryOptions.slice(1),
    insuranceIndex: 0,
    dosageIndex: 0,
    categoryIndex: 0,
    statusOptions: [
      { label: '正常', value: 1 },
      { label: '停用', value: 0 }
    ],
    statusIndex: 0
  },

  onLoad(options) {
    const mode = options.mode === 'edit' ? 'edit' : 'create';
    const title = mode === 'edit' ? '编辑药品' : '新增药品';
    this.setData({ mode: mode, drugId: options.id ? Number(options.id) : null });
    wx.setNavigationBarTitle({ title: title });

    if (mode === 'edit' && options.id) {
      this.loadDrugDetail(options.id);
    }
  },

  async loadDrugDetail(drugId) {
    this.setData({ loading: true });
    try {
      const res = await api.getDrugDetail(drugId);
      const drug = res.data || res;
      this.setData({
        form: Object.assign({}, createDefaultForm(), {
          genericName: drug.genericName || '',
          tradeName: drug.tradeName || '',
          specification: drug.specification || '',
          dosageForm: drug.dosageForm || '',
          manufacturer: drug.manufacturer || '',
          approvalNumber: drug.approvalNumber || '',
          barcode: drug.barcode || '',
          category: drug.category || '',
          insuranceType: drug.insuranceType || '甲',
          indication: drug.indication || '',
          contraindication: drug.contraindication || '',
          interaction: drug.interaction || '',
          stockQuantity: drug.stockQuantity !== undefined && drug.stockQuantity !== null ? String(drug.stockQuantity) : '',
          stockThreshold: drug.stockThreshold !== undefined && drug.stockThreshold !== null ? String(drug.stockThreshold) : '',
          unitPrice: drug.unitPrice !== undefined && drug.unitPrice !== null ? String(drug.unitPrice) : '',
          expiryDate: drug.expiryDate || '',
          shelfLifeDays: drug.shelfLifeDays !== undefined && drug.shelfLifeDays !== null ? String(drug.shelfLifeDays) : '',
          storageCondition: drug.storageCondition || '',
          status: typeof drug.status === 'number' ? drug.status : 1
        }),
        insuranceIndex: Math.max(0, this.data.insuranceOptions.indexOf(drug.insuranceType || '甲')),
        dosageIndex: Math.max(0, this.data.dosageOptions.indexOf(drug.dosageForm || '')),
        categoryIndex: Math.max(0, this.data.categoryOptions.indexOf(drug.category || '')),
        statusIndex: typeof drug.status === 'number' && drug.status === 0 ? 1 : 0,
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载药品失败', icon: 'none' });
    }
  },

  onFieldInput(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      form: Object.assign({}, this.data.form, { [field]: value })
    });
  },

  onInsuranceChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      insuranceIndex: index,
      form: Object.assign({}, this.data.form, { insuranceType: this.data.insuranceOptions[index] })
    });
  },

  onDosageChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      dosageIndex: index,
      form: Object.assign({}, this.data.form, { dosageForm: this.data.dosageOptions[index] })
    });
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      categoryIndex: index,
      form: Object.assign({}, this.data.form, { category: this.data.categoryOptions[index] })
    });
  },

  onStatusChange(e) {
    const index = Number(e.detail.value);
    this.setData({
      statusIndex: index,
      form: Object.assign({}, this.data.form, { status: this.data.statusOptions[index].value })
    });
  },

  onExpiryDateChange(e) {
    this.setData({
      form: Object.assign({}, this.data.form, { expiryDate: e.detail.value })
    });
  },

  validateForm() {
    const form = this.data.form;
    if (!form.genericName) return '请输入通用名';
    if (form.genericName.trim().length < 2) return '通用名至少 2 个字符';
    if (!form.specification) return '请输入规格';
    if (!form.dosageForm) return '请选择剂型';
    if (!form.manufacturer) return '请输入生产厂家';
    if (!form.category) return '请选择药理分类';
    if (!form.insuranceType) return '请选择医保类别';
    if (form.stockQuantity === '') return '请输入当前库存';
    if (form.stockThreshold === '') return '请输入补货阈值';
    if (form.unitPrice === '') return '请输入单价';

    const stockQuantity = Number(form.stockQuantity);
    const stockThreshold = Number(form.stockThreshold);
    const unitPrice = Number(form.unitPrice);
    const shelfLifeDays = form.shelfLifeDays === '' ? null : Number(form.shelfLifeDays);

    if (!Number.isInteger(stockQuantity) || stockQuantity < 0) return '当前库存必须为大于等于 0 的整数';
    if (!Number.isInteger(stockThreshold) || stockThreshold < 0) return '补货阈值必须为大于等于 0 的整数';
    if (Number.isNaN(unitPrice) || unitPrice < 0) return '单价必须为大于等于 0 的数字';
    if (shelfLifeDays !== null && (!Number.isInteger(shelfLifeDays) || shelfLifeDays <= 0)) return '总有效期必须为正整数';

    if (form.approvalNumber && form.approvalNumber.trim().length < 6) return '批准文号格式过短';
    if (form.barcode && !/^\d{8,20}$/.test(form.barcode.trim())) return '条形码应为 8 到 20 位数字';
    if (form.expiryDate && shelfLifeDays !== null) {
      const expiryTime = new Date(form.expiryDate).getTime();
      if (Number.isNaN(expiryTime)) return '有效期日期格式不正确';
    }

    return '';
  },

  buildSubmitPayload() {
    const form = this.data.form;
    return {
      genericName: form.genericName.trim(),
      tradeName: form.tradeName.trim(),
      specification: form.specification.trim(),
      dosageForm: form.dosageForm,
      manufacturer: form.manufacturer.trim(),
      approvalNumber: form.approvalNumber.trim(),
      barcode: form.barcode.trim(),
      category: form.category,
      insuranceType: form.insuranceType,
      indication: form.indication.trim(),
      contraindication: form.contraindication.trim(),
      interaction: form.interaction.trim(),
      stockQuantity: Number(form.stockQuantity || 0),
      stockThreshold: Number(form.stockThreshold || 0),
      unitPrice: Number(form.unitPrice || 0),
      expiryDate: form.expiryDate || null,
      shelfLifeDays: form.shelfLifeDays ? Number(form.shelfLifeDays) : null,
      storageCondition: form.storageCondition.trim(),
      status: form.status
    };
  },

  async submitForm() {
    const errorText = this.validateForm();
    if (errorText) {
      wx.showToast({ title: errorText, icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    const payload = this.buildSubmitPayload();

    try {
      if (this.data.mode === 'edit' && this.data.drugId) {
        const res = await api.updateDrug(this.data.drugId, payload);
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || '药品更新失败');
        }
        wx.showToast({ title: '药品已更新', icon: 'success' });
      } else {
        const res = await api.createDrug(payload);
        if (!res || res.code !== 200) {
          throw new Error((res && res.message) || '药品新增失败');
        }
        wx.showToast({ title: '药品已新增', icon: 'success' });
      }

      setTimeout(() => {
        wx.navigateBack({ delta: 1 });
      }, 600);
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
