const currentHost = 'YOUR_LAN_IP';
const currentPort = '8080';

const AppConfig = {
  appName: '药库查询系统',
  appVersion: 'v1.0.0',
  appDesc: '基于 AI 技术的智能药品信息查询平台',
  debugNetwork: false,
  apiProtocol: 'http',
  apiHost: currentHost,
  apiPort: currentPort,
  apiBaseUrl: 'http://' + currentHost + ':' + currentPort + '/api',
  searchPlaceholder: '输入药品名称、症状或自然语言描述',
  loginPlaceholders: {
    username: '请输入用户名',
    password: '请输入密码'
  }
};

module.exports = AppConfig;
