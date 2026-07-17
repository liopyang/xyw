export const businessTypeLabels: Record<string, string> = {
  CAMPUS_CARD: '校园卡',
  CAMPUS_NETWORK: '校园网',
  DRIVING_SCHOOL: '驾校',
  RENEWAL: '续费',
};

export const sourceChannelLabels: Record<string, string> = {
  AGENT: '代理',
  ONLINE: '线上',
  STORE: '门店',
};

export const issueTypeLabels: Record<string, string> = {
  CAMPUS_CARD: '校园卡问题',
  CAMPUS_NETWORK: '校园网问题',
  DRIVING_SCHOOL: '驾校问题',
  RENEWAL: '续费问题',
  ACCOUNT: '账号问题',
  OTHER: '其他问题',
};

export const issueStatusLabels: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
};

export const auditStatusLabels: Record<string, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
};

export const exportStatusLabels: Record<string, string> = {
  NOT_EXPORTED: '未导出',
  EXPORTED: '已导出',
};
