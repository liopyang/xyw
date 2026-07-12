import axios, { type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { mockAdapter } from '../mock'

const request=axios.create({baseURL:import.meta.env.VITE_API_BASE_URL||'/api',timeout:15000,adapter:import.meta.env.VITE_USE_MOCK==='true'?mockAdapter:undefined})
request.interceptors.request.use(config=>{const token=localStorage.getItem('campus_token');if(token)config.headers.Authorization=`Bearer ${token}`;return config})

async function errorMessage(error: AxiosError<{ message?: string } | Blob>) {
  const data = error.response?.data
  if (data instanceof Blob) {
    try {
      const text = await data.text()
      if (text) {
        try {
          const parsed = JSON.parse(text) as { message?: string }
          if (parsed.message) return parsed.message
        } catch {
          return text.length <= 200 ? text : '导出失败，服务器返回了无效错误信息'
        }
      }
    } catch {
      // Blob 解析失败时继续使用通用错误信息。
    }
    return '导出失败，服务器未返回可识别的错误信息'
  } else if (data?.message) {
    return data.message
  }
  if (!error.response) return '无法连接服务器，请检查网络后重试'
  return error.message || '请求失败，请稍后重试'
}

request.interceptors.response.use(
  response => response.data,
  async (error: AxiosError<{ message?: string } | Blob>) => {
    const status = error.response?.status
    const message = await errorMessage(error)
    if (status === 401) {
      localStorage.removeItem('campus_token')
      localStorage.removeItem('campus_user')
      if (location.pathname !== '/login') location.href = '/login'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)
export default request
