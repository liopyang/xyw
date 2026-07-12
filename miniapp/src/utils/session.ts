export interface SessionUser {
  id: number
  username: string
  realName: string
  role: 'AGENT' | 'USER' | 'OWNER' | 'ADMIN' | string
}

export function currentUser(): SessionUser {
  return (uni.getStorageSync('user') || {}) as SessionUser
}

export function isAgent() {
  return currentUser().role === 'AGENT'
}

export function requireAgent() {
  if (isAgent()) return true
  uni.showToast({ title: '仅代理账号可使用订单功能', icon: 'none' })
  setTimeout(() => uni.switchTab({ url: '/pages/home/index' }), 300)
  return false
}
