import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 实例：统一携带 JWT 令牌、统一处理响应与错误
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 请求拦截器：注入 JWT 令牌
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cls_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 { code, message, data }
request.interceptors.response.use(
  (response) => {
    // 二进制流（文件下载）直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      handleUnauthorized(res.message)
      return Promise.reject(new Error(res.message || '登录已失效'))
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized('登录已失效，请重新登录')
    } else if (status === 403) {
      ElMessage.error('无权限访问该功能')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试（AI 接口响应较慢，请耐心等待）')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请检查后端服务是否已启动')
    }
    return Promise.reject(error)
  }
)

let redirecting = false

/** 登录失效统一处理：清除本地信息并跳转登录页 */
function handleUnauthorized(message) {
  if (redirecting) return
  redirecting = true
  localStorage.removeItem('cls_token')
  localStorage.removeItem('cls_user')
  ElMessage.error(message || '登录已失效，请重新登录')
  setTimeout(() => {
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login'
    }
    redirecting = false
  }, 300)
}

export default request
