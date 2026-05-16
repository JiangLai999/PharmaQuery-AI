const util = require('../../utils/util');

Component({
  properties: {
    drug: {
      type: Object,
      value: {},
      observer(drug) {
        this.setData({
          display: this.buildDisplay(drug || {})
        });
      }
    },
    showEditAction: {
      type: Boolean,
      value: false
    },
    showStockAction: {
      type: Boolean,
      value: false
    }
  },

  data: {
    display: {}
  },

  methods: {
    buildDisplay(drug) {
      const riskLevel = drug.riskLevel || 'normal';
      const riskConfig = util.getRiskConfig(riskLevel);
      const insuranceConfig = util.getInsuranceConfig(drug.insuranceType);
      const stockQuantity = Number(drug.stockQuantity || 0);
      const stockThreshold = Number(drug.stockThreshold || 0);
      const lowStock = stockThreshold > 0 ? stockQuantity <= stockThreshold : stockQuantity <= 50;
      const riskTags = [];

      if (drug.isDoubleRisk) {
        riskTags.push({ key: 'double', text: '双重风险', className: 'risk-tag-double' });
      } else {
        if (drug.isLowStock || lowStock) {
          riskTags.push({ key: 'low', text: '低库存', className: 'risk-tag-low' });
        }
        if (drug.isNearExpiry) {
          riskTags.push({ key: 'expiry', text: '近效期', className: 'risk-tag-expiry' });
        }
      }

      return {
        genericName: drug.genericName || '未命名药品',
        tradeName: drug.tradeName || '',
        specification: drug.specification || '--',
        dosageForm: drug.dosageForm || '--',
        manufacturer: drug.manufacturer || '--',
        stockQuantity: util.formatNumber(stockQuantity),
        stockThreshold: util.formatNumber(stockThreshold),
        unitPrice: util.formatPrice(drug.unitPrice),
        insuranceText: insuranceConfig.label,
        insuranceClass: this.getInsuranceClass(drug.insuranceType),
        riskText: riskConfig.label,
        recommendation: drug.recommendation || '',
        riskLevel,
        lowStock,
        stockStatusText: stockThreshold > 0 ? (lowStock ? '低库存' : '库存正常') : '未设阈值',
        riskTags,
        showRiskBadge: riskLevel !== 'normal'
      };
    },

    getInsuranceClass(type) {
      if (type === '甲') return 'jia';
      if (type === '乙') return 'yi';
      return 'bing';
    },

    onTap() {
      const drugId = this.properties.drug.drugId;
      if (drugId) {
        wx.navigateTo({
          url: '/pages/detail/detail?id=' + drugId
        });
      }
    },

    onEditTap() {
      const drugId = this.properties.drug.drugId;
      if (drugId) {
        wx.navigateTo({
          url: '/pages/drug-form/drug-form?mode=edit&id=' + drugId
        });
      }
    },

    onStockTap() {
      const drugId = this.properties.drug.drugId;
      if (drugId) {
        wx.navigateTo({
          url: '/pages/stock-adjust/stock-adjust?id=' + drugId
        });
      }
    }
  }
});
