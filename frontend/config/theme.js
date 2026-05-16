/**
 * UI主题配置 - 集中管理所有样式常量
 * 避免硬编码，便于维护和主题切换
 */

const Theme = {
  // 颜色系统
  colors: {
    primary: '#2C7EF8',
    primaryDark: '#1A6FE0',
    primaryLight: '#E8F0FE',
    
    success: '#52C41A',
    successLight: '#E8F8E8',
    
    warning: '#FAAD14',
    warningLight: '#FFF7E6',
    
    danger: '#FF4D4F',
    dangerLight: '#FFF1F0',
    dangerOrange: '#FF6B35',
    
    info: '#2C7EF8',
    infoLight: '#F0F7FF',
    
    text: {
      primary: '#1a1a1a',
      secondary: '#666666',
      tertiary: '#999999',
      quaternary: '#bbbbbb',
      white: '#ffffff'
    },
    
    bg: {
      page: '#F5F6FA',
      card: '#ffffff',
      hover: '#f5f5f5'
    },
    
    border: '#f0f0f0',
    divider: '#e8e8e8',
    shadow: 'rgba(0, 0, 0, 0.06)'
  },
  
  // 字体大小
  fontSize: {
    xs: '22rpx',
    sm: '24rpx',
    base: '28rpx',
    md: '30rpx',
    lg: '32rpx',
    xl: '36rpx',
    xxl: '40rpx',
    title: '48rpx'
  },
  
  // 间距
  spacing: {
    xs: '8rpx',
    sm: '12rpx',
    md: '16rpx',
    lg: '20rpx',
    xl: '24rpx',
    xxl: '30rpx',
    xxxl: '40rpx'
  },
  
  // 圆角
  radius: {
    sm: '8rpx',
    md: '12rpx',
    lg: '16rpx',
    xl: '20rpx',
    xxl: '40rpx',
    full: '50%'
  },
  
  // 阴影
  shadow: {
    sm: '0 2rpx 8rpx rgba(0, 0, 0, 0.04)',
    md: '0 2rpx 12rpx rgba(0, 0, 0, 0.06)',
    lg: '0 4rpx 20rpx rgba(0, 0, 0, 0.08)'
  },
  
  // 渐变
  gradient: {
    primary: 'linear-gradient(135deg, #2C7EF8, #667eea)',
    primaryReverse: 'linear-gradient(135deg, #667eea, #2C7EF8)',
    login: 'linear-gradient(180deg, #2C7EF8 0%, #F5F6FA 50%)'
  },
  
  // 动画
  animation: {
    duration: '0.2s',
    easing: 'ease'
  },
  
  // 图标
  icons: {
    search: '/images/icons/search.png',
    ai: '/images/icons/ai.png',
    expiry: '/images/icons/expiry.png',
    stock: '/images/icons/stock.png',
    category: '/images/icons/category.png',
    add: '/images/icons/add.png',
    home: '/images/icons/home.png',
    recommend: '/images/icons/recommend.png',
    profile: '/images/icons/profile.png',
    warning: '/images/icons/warning.png',
    arrow: '/images/icons/arrow.png',
    clear: '/images/icons/clear.png',
    refresh: '/images/icons/refresh.png',
    empty: '/images/icons/empty.png',
    drug: '/images/icons/drug.png',
    logo: '/images/logo.png'
  },
  
  // 角色配置
  roles: {
    PHARMACY_ADMIN: { label: '药房管理员', color: '#2C7EF8' },
    DOCTOR: { label: '临床医生', color: '#52C41A' },
    PHARMACIST: { label: '药师', color: '#FAAD14' },
    SYS_ADMIN: { label: '系统管理员', color: '#FF4D4F' }
  },
  
  // 权限标签映射
  permissions: {
    'DRUG_INFO_READ': '查看药品',
    'DRUG_INFO_WRITE': '编辑药品',
    'DRUG_INFO_DELETE': '删除药品',
    'RECOMMEND_READ': '智能推荐',
    'LOG_READ': '查看日志',
    'USER_MANAGE': '用户管理',
    'STOCK_READ': '查看库存',
    'STOCK_WRITE': '管理库存'
  },
  
  // 风险等级
  riskLevels: {
    normal: { label: '正常', color: '#52C41A', bgColor: '#E8F8E8' },
    warning: { label: '注意', color: '#FAAD14', bgColor: '#FFF7E6' },
    danger: { label: '警告', color: '#FF4D4F', bgColor: '#FFF1F0' }
  },
  
  // 医保类型
  insuranceTypes: {
    '甲': { label: '医保甲类', color: '#52C41A', bgColor: '#E8F8E8' },
    '乙': { label: '医保乙类', color: '#2C7EF8', bgColor: '#E8F0FE' },
    '丙': { label: '自费', color: '#FAAD14', bgColor: '#FFF7E6' }
  },
  
  // 剂型选项
  dosageForms: ['全部', '片剂', '胶囊剂', '缓释片', '控释片', '分散片', '散剂', '口服溶液', '注射用粉末', '肠溶胶囊'],

  // 分类选项
  categoryOptions: ['全部', '心血管系统药', '抗感染药', '降血糖药', '呼吸系统药', '消化系统药', '抗血栓药', '维生素类', '解热镇痛药', '抗过敏药', '泌尿系统药', '调血脂药', '感冒用药', '抗抑郁药', '矿物质类', '肝病辅助药', '镇痛药'],
  
  // 医保选项
  insuranceOptions: ['全部', '甲', '乙', '丙'],
  
  // 库存选项
  stockOptions: ['全部', '有库存', '低库存', '缺货'],
  
  // 库存状态映射
  stockStatusMap: {
    1: 'inStock',
    2: 'lowStock', 
    3: 'outOfStock'
  }
};

module.exports = Theme;
