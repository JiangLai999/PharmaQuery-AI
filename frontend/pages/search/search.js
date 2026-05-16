const api = require('../../utils/api');
const util = require('../../utils/util');
const Theme = require('../../config/theme');
const AppConfig = require('../../config/app');

Page({
  data: {
    keyword: '',
    mode: 'fuzzy',
    isCategoryMode: false,
    selectedCategoryName: '',
    pageTitle: '药品检索中心',
    pageDesc: '从关键词、语义理解到组合条件，一次覆盖日常查询场景',
    results: [],
    total: 0,
    searched: false,
    loading: false,
    autoFocus: true,
    searchTime: 0,
    nlpEntities: [],
    nlpEntityTags: [],
    modeTabs: [
      { key: 'fuzzy', label: '关键词', desc: '药名或症状' },
      { key: 'nlp', label: 'AI 语义', desc: '自然语言理解' },
      { key: 'advanced', label: '高级筛选', desc: '组合条件检索' }
    ],
    categories: Theme.categoryOptions,
    dosageForms: Theme.dosageForms,
    insuranceTypes: Theme.insuranceOptions,
    stockOptions: Theme.stockOptions,
    categoryIndex: 0,
    dosageIndex: 0,
    insuranceIndex: 0,
    stockIndex: 0,
    manufacturer: '',
    pageNum: 1,
    pageSize: 20,
    hasMore: true,
    searchPlaceholder: AppConfig.searchPlaceholder,
    canEditDrug: false,
    canAdjustStock: false,
    stockAdjustSummary: [],
    stockAdjustFilter: 'all',
    stockAdjustAllDrugs: [],
    stockAdjustLowIds: [],
    stockAdjustExpiryIds: [],
    stockAdjustDoubleIds: []
  },

  onLoad(options) {
    const app = getApp();
    const permissions = (app.globalData.userInfo && app.globalData.userInfo.permissions) || [];
    this.setData({
      canEditDrug: permissions.includes('DRUG_INFO_WRITE'),
      canAdjustStock: permissions.includes('STOCK_WRITE')
    });

    if (options.mode) {
      this.setData({ mode: options.mode });
      if (options.mode === 'nearExpiry') {
        this.loadNearExpiry();
      } else if (options.mode === 'lowStock') {
        this.loadLowStock();
      } else if (options.mode === 'stockAdjust') {
        this.loadStockAdjustList();
      } else if (options.mode === 'category') {
        this.setData({
          mode: 'advanced',
          isCategoryMode: true,
          autoFocus: false,
          pageTitle: '分类浏览',
          pageDesc: '按药理分类快速筛选药品，并结合剂型、医保和库存条件进一步过滤'
        });
        wx.setNavigationBarTitle({ title: '分类浏览' });
      }
    }
    if (options.category) {
      const category = decodeURIComponent(options.category);
      const categoryIndex = Math.max(0, this.data.categories.indexOf(category));
      this.setData({
        mode: 'advanced',
        isCategoryMode: true,
        autoFocus: false,
        selectedCategoryName: category,
        pageTitle: category,
        pageDesc: '已为你展示该分类下的药品，可继续结合剂型、医保和库存条件筛选',
        categoryIndex: categoryIndex
      });
      wx.setNavigationBarTitle({ title: category });
      this.doSearch();
    }
  },

  onInput: util.debounce(function (e) {
    const value = e.detail.value;
    this.setData({ keyword: value });
    if (this.data.mode === 'stockAdjust') {
      this.applyStockAdjustFilter();
      return;
    }
    if (e.detail.value.length >= 2) {
      this.doSearch();
    }
  }, 400),

  onShow() {
    if (this.data.mode === 'lowStock' && this.data.searched) {
      this.loadLowStock();
    } else if (this.data.mode === 'stockAdjust' && this.data.searched) {
      this.loadStockAdjustList();
    }
  },

  applyStockAdjustFilter() {
    const filter = this.data.stockAdjustFilter;
    const allDrugs = this.data.stockAdjustAllDrugs || [];
    const lowIds = this.data.stockAdjustLowIds || [];
    const expiryIds = this.data.stockAdjustExpiryIds || [];
    const doubleIds = this.data.stockAdjustDoubleIds || [];
    const keyword = (this.data.keyword || '').trim().toLowerCase();

    const filtered = this.filterDrugsSync(allDrugs, filter, keyword, lowIds, expiryIds, doubleIds);

    this.setData({
      results: filtered,
      total: filtered.length
    });
  },

  filterDrugsSync(allDrugs, filter, keyword, lowIds, expiryIds, doubleIds) {
    let filtered = allDrugs;

    if (filter === 'low') {
      filtered = allDrugs.filter(function (item) {
        return lowIds.indexOf(item.drugId) > -1;
      });
    } else if (filter === 'expiry') {
      filtered = allDrugs.filter(function (item) {
        return expiryIds.indexOf(item.drugId) > -1;
      });
    } else if (filter === 'double') {
      filtered = allDrugs.filter(function (item) {
        return doubleIds.indexOf(item.drugId) > -1;
      });
    }

    if (keyword) {
      filtered = filtered.filter(function (item) {
        const genericName = (item.genericName || '').toLowerCase();
        const tradeName = (item.tradeName || '').toLowerCase();
        const manufacturer = (item.manufacturer || '').toLowerCase();
        return genericName.indexOf(keyword) > -1 || tradeName.indexOf(keyword) > -1 || manufacturer.indexOf(keyword) > -1;
      });
    }

    return filtered;
  },

  onStockAdjustSummaryTap(e) {
    const filter = e.currentTarget.dataset.filter;
    this.setData({ stockAdjustFilter: filter }, () => {
      this.applyStockAdjustFilter();
    });
  },

  onSearchAction() {
    if (this.data.mode === 'stockAdjust') {
      this.applyStockAdjustFilter();
      return;
    }
    this.doSearch();
  },

  clearKeyword() {
    if (this.data.mode === 'stockAdjust') {
      this.setData({ keyword: '' }, () => {
        this.applyStockAdjustFilter();
      });
      return;
    }
    this.setData({ keyword: '', results: [], searched: false, nlpEntities: [], nlpEntityTags: [] });
  },

  switchMode(e) {
    const nextMode = e.currentTarget.dataset.mode;
    this.setData({
      mode: nextMode,
      isCategoryMode: false,
      selectedCategoryName: '',
      autoFocus: true,
      pageTitle: '药品检索中心',
      pageDesc: '从关键词、语义理解到组合条件，一次覆盖日常查询场景',
      results: [],
      searched: false,
      nlpEntities: [],
      nlpEntityTags: []
    });
    wx.setNavigationBarTitle({ title: nextMode === 'advanced' ? '高级筛选' : '药品检索' });
  },

  async doSearch() {
    const { keyword, mode } = this.data;
    if (!keyword && mode !== 'advanced') return;

    this.setData({ loading: true, searched: true, nlpEntities: [], nlpEntityTags: [] });
    const startTime = Date.now();

    try {
      let res;
      if (mode === 'nlp') {
        res = await api.nlpSearch(keyword);
        if (res.data && res.data.entities) {
          const entities = res.data.entities || [];
          this.setData({
            nlpEntities: entities,
            nlpEntityTags: entities.map(function (item) {
              return {
                text: item.text,
                label: util.getNlpEntityTypeText(item.type) + ' · ' + item.text
              };
            })
          });
        }
      } else if (mode === 'advanced') {
        const params = this.buildAdvancedParams();
        res = await api.advancedSearch(params);
      } else {
        res = await api.searchDrugs(keyword);
      }

      const searchTime = Date.now() - startTime;
      const data = res.data || res;
      const results = Array.isArray(data) ? data : (data.records || []);

      this.setData({
        results: results,
        total: Array.isArray(data) ? data.length : (data.total || results.length),
        searchTime: searchTime,
        loading: false
      });

      this.recordSearch(keyword);
    } catch (e) {
      console.error('搜索失败:', e);
      this.setData({ loading: false, results: [] });
      wx.showToast({ title: '搜索失败', icon: 'none' });
    }
  },

  buildAdvancedParams() {
    const { keyword, categories, categoryIndex, dosageIndex, dosageForms, insuranceIndex, insuranceTypes, stockIndex, manufacturer } = this.data;
    return {
      genericName: keyword || undefined,
      category: categoryIndex > 0 ? categories[categoryIndex] : undefined,
      dosageForm: dosageIndex > 0 ? dosageForms[dosageIndex] : undefined,
      insuranceType: insuranceIndex > 0 ? insuranceTypes[insuranceIndex] : undefined,
      stockStatus: Theme.stockStatusMap[stockIndex] || undefined,
      manufacturer: manufacturer || undefined,
      pageNum: this.data.pageNum,
      pageSize: this.data.pageSize
    };
  },

  async loadNearExpiry() {
    this.setData({ loading: true, searched: true });
    wx.setNavigationBarTitle({ title: '近效期药品' });
    try {
      const res = await api.getNearExpiry();
      this.setData({
        results: res.data || [],
        total: (res.data || []).length,
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async loadLowStock() {
    this.setData({ loading: true, searched: true });
    wx.setNavigationBarTitle({ title: '低库存药品' });
    try {
      const [lowStockRes, nearExpiryRes] = await Promise.all([
        api.getLowStock(),
        api.getNearExpiry()
      ]);
      const lowStockList = lowStockRes.data || [];
      const expiryIds = (nearExpiryRes.data || []).map(function (item) { return item.drugId; });
      const lowIds = lowStockList.map(function (item) { return item.drugId; });
      this.setData({
        results: this.decorateStockRiskFlags(lowStockList, lowIds, expiryIds),
        total: lowStockList.length,
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async loadStockAdjustList() {
    this.setData({
      loading: true,
      searched: true,
      pageTitle: '库存调整',
      pageDesc: '集中管理药品库存与补货阈值，可按风险状态和关键词快速筛选',
      searchPlaceholder: '搜索药品名称或厂家'
    });
    wx.setNavigationBarTitle({ title: '库存调整' });
    try {
      const [allDrugRes, lowStockRes, nearExpiryRes] = await Promise.all([
        api.advancedSearch({ pageNum: 1, pageSize: 100 }),
        api.getLowStock(),
        api.getNearExpiry()
      ]);
      const allDrugData = allDrugRes.data || allDrugRes;
      const allDrugs = Array.isArray(allDrugData) ? allDrugData : (allDrugData.records || []);
      const lowStockList = lowStockRes.data || [];
      const nearExpiryList = nearExpiryRes.data || [];
      const lowIds = lowStockList.map(function (item) { return item.drugId; });
      const expiryIds = nearExpiryList.map(function (item) { return item.drugId; });
      const doubleIds = lowIds.filter(function (id) { return expiryIds.indexOf(id) > -1; });
      const decoratedDrugs = this.decorateStockRiskFlags(allDrugs, lowIds, expiryIds);
      const stockAdjustSummary = [
        { key: 'all', filter: 'all', title: '药品总数', value: decoratedDrugs.length, desc: '当前可管理药品数量' },
        { key: 'low', filter: 'low', title: '低库存', value: lowStockList.length, desc: '优先补货的库存项目' },
        { key: 'expiry', filter: 'expiry', title: '近效期', value: nearExpiryList.length, desc: '需同步关注效期风险' },
        { key: 'double', filter: 'double', title: '双重风险', value: doubleIds.length, desc: '同时存在低库存和近效期' }
      ];
      const filteredResults = this.filterDrugsSync(decoratedDrugs, 'all', '', lowIds, expiryIds, doubleIds);
      this.setData({
        stockAdjustAllDrugs: decoratedDrugs,
        stockAdjustLowIds: lowIds,
        stockAdjustExpiryIds: expiryIds,
        stockAdjustDoubleIds: doubleIds,
        stockAdjustFilter: 'all',
        results: filteredResults,
        total: filteredResults.length,
        loading: false,
        canAdjustStock: true,
        stockAdjustSummary: stockAdjustSummary
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  recordSearch(keyword) {
    const app = getApp();
    if (app.globalData.userId && this.data.results.length > 0) {
      const firstDrug = this.data.results[0];
      api.recordView(firstDrug.drugId, app.globalData.userId).catch(() => {});
    }
  },

  onDosageChange(e) { this.setData({ dosageIndex: e.detail.value }); },
  onCategoryChange(e) {
    const categoryIndex = Number(e.detail.value);
    this.setData({
      categoryIndex: categoryIndex,
      selectedCategoryName: categoryIndex > 0 ? this.data.categories[categoryIndex] : ''
    });
  },
  onInsuranceChange(e) { this.setData({ insuranceIndex: e.detail.value }); },
  onStockChange(e) { this.setData({ stockIndex: e.detail.value }); },
  onManufacturerInput(e) { this.setData({ manufacturer: e.detail.value }); }
});
