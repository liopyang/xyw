const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const developmentApi = 'http://127.0.0.1:8080/api'
const productionApi = 'https://hutbxyw.click/api'

export const API_BASE_URL = trimTrailingSlash(
  import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? developmentApi : productionApi),
)

export const FILE_BASE_URL = trimTrailingSlash(
  import.meta.env.VITE_FILE_BASE_URL || API_BASE_URL.replace(/\/api$/, ''),
)

export function apiUrl(path: string) {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

export function fileUrl(path?: string | null) {
  if (!path) return ''
  if (/^(https?:|data:|blob:|wxfile:|file:)/i.test(path)) return path
  return `${FILE_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}
