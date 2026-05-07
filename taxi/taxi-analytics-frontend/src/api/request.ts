import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

interface RetryConfig {
  retries: number
  retryDelay: number
  retryCondition?: (error: any) => boolean
}

class Request {
  private instance: AxiosInstance
  private retryConfig: RetryConfig = {
    retries: 3,
    retryDelay: 1000,
    retryCondition: (error) => error.code === 'ECONNABORTED' || error.message?.includes('timeout')
  }

  constructor() {
    this.instance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    this.instance.interceptors.request.use(
      (config) => {
        if (config.method === 'get') {
          config.params = { ...config.params, _t: Date.now() }
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    this.instance.interceptors.response.use(
      (response: AxiosResponse<ResponseData>) => {
        const { code, message, data } = response.data

        if (code === 200) {
          return data
        }

        this.handleBusinessError(code, message)
        return Promise.reject(new Error(message))
      },
      async (error) => {
        if (this.shouldRetry(error)) {
          return this.retryRequest(error)
        }
        this.handleHttpError(error)
        return Promise.reject(error)
      }
    )
  }

  private handleBusinessError(code: number, message: string) {
    switch (code) {
      case 1001:
      case 1004:
      case 1005:
        ElMessage.warning(message)
        break
      case 2002:
      case 2005:
        ElMessage.warning(message)
        break
      default:
        ElMessage.error(message || '请求失败')
    }
  }

  private handleHttpError(error: any) {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 503:
          ElMessage.warning('服务暂时不可用，请稍后重试')
          break
        default:
          ElMessage.error(`请求失败: ${error.message}`)
      }
    } else if (error.request) {
      ElMessage.error('网络连接异常，请检查网络后重试')
    } else {
      ElMessage.error(error.message)
    }
  }

  private shouldRetry(error: any): boolean {
    return this.retryConfig.retryCondition?.(error) ?? false
  }

  private async retryRequest(error: any): Promise<any> {
    const { config } = error
    config.retryCount = config.retryCount || 0

    if (config.retryCount >= this.retryConfig.retries) {
      return Promise.reject(error)
    }

    config.retryCount += 1

    await new Promise(resolve => setTimeout(resolve, this.retryConfig.retryDelay * config.retryCount))

    return this.instance(config)
  }

  request<T = any>(config: any): Promise<T> {
    return this.instance.request(config)
  }

  get<T = any>(url: string, params?: any): Promise<T> {
    return this.instance.get(url, { params })
  }

  post<T = any>(url: string, data?: any): Promise<T> {
    return this.instance.post(url, data)
  }

  put<T = any>(url: string, data?: any): Promise<T> {
    return this.instance.put(url, data)
  }

  delete<T = any>(url: string, params?: any): Promise<T> {
    return this.instance.delete(url, { params })
  }
}

export default new Request()