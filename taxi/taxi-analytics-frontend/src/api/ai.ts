import request from './request'

export interface AiQueryRequest {
  query: string
  sessionId?: string
  database?: string
}

/**
 * 图表配置接口
 * 严格遵循API契约，字段名使用下划线格式
 */
export interface ChartConfig {
  /**
   * 图表类型
   * 取值仅限: line, bar, pie, stacked_bar, table, horizontal_bar
   */
  chart_type: string
  /**
   * 标题
   */
  title: string
  /**
   * X轴字段名（对应data数组中对象的字段名）
   */
  x_field: string
  /**
   * Y轴字段名（或字段名数组，对应data数组中对象的字段名）
   */
  y_field: string | string[]
  /**
   * 是否显示图例
   */
  legend?: boolean
  /**
   * 是否显示百分比
   */
  percentage?: boolean
  /**
   * 图表数据（后端执行SQL后得到的实际数据数组）
   */
  data: Array<Record<string, any>>
}

/**
 * 响应数据内部结构
 */
export interface ResponseData {
  /**
   * 执行的SQL语句
   */
  sql: string
  /**
   * 图表配置信息
   */
  chartConfig: ChartConfig | null
  /**
   * SQL执行耗时（毫秒）
   */
  executionTime: number
  /**
   * 业务解释文字（可选）
   */
  explanation?: string
  /**
   * 查询结果数量
   */
  count?: number
}

/**
 * AI查询响应接口
 * 严格遵循API契约格式
 */
export interface AiQueryResponse {
  /**
   * 响应码
   * 200: 成功
   * 400: 请求参数错误 / 智能体解析失败
   * 500: 服务器内部错误 / SQL执行失败
   */
  code: number
  /**
   * 响应消息
   */
  message: string
  /**
   * 响应数据
   */
  data: ResponseData | null
}

export interface ChatMessage {
  messageId: string
  role: 'user' | 'assistant'
  content: string
  explanation?: string
  sql?: string
  hasChart?: boolean
  chartConfig?: ChartConfig
  chartHeight?: number
}

export interface Session {
  sessionId: string
  sessionName: string
}

export interface Suggestion {
  id: string
  text: string
}

export interface HistoryItem {
  messageId: string
  question: string
  createTime: string
}

export interface Favorite {
  id: string
  queryText: string
}

export interface ScheduledTask {
  taskId: string
  queryText: string
  scheduleTime: string
}

export const aiApi = {
  chat: (data: AiQueryRequest): Promise<AiQueryResponse> => {
    return request.post('/ai/chat', data)
  },

  query: (data: AiQueryRequest): Promise<AiQueryResponse> => {
    return request.post('/ai/query', data)
  },

  health: (): Promise<string> => {
    return request.get('/ai/health')
  },

  getSessions: (): Promise<Session[]> => {
    return request.get('/ai/sessions')
  },

  createSession: (sessionName: string): Promise<{ sessionId: string }> => {
    return request.post('/ai/sessions', { sessionName })
  },

  deleteSession: (sessionId: string): Promise<void> => {
    return request.delete(`/ai/sessions/${sessionId}`)
  },

  updateSession: (sessionId: string, sessionName: string): Promise<void> => {
    return request.put(`/ai/sessions/${sessionId}`, { sessionName })
  },

  getSessionMessages: (sessionId: string): Promise<ChatMessage[]> => {
    return request.get(`/ai/sessions/${sessionId}/messages`)
  },

  getSuggestions: (): Promise<Suggestion[]> => {
    return request.get('/ai/suggestions')
  },

  getQueryHistory: (limit: number = 20): Promise<HistoryItem[]> => {
    return request.get('/ai/history', { params: { limit } })
  },

  saveFeedback: (messageId: string, feedback: string): Promise<void> => {
    return request.post('/ai/feedback', { messageId, feedback })
  },

  addFavorite: (userId: string, queryText: string): Promise<void> => {
    return request.post('/ai/favorites', { userId, queryText })
  },

  getFavorites: (userId: string): Promise<Favorite[]> => {
    return request.get('/ai/favorites', { params: { userId } })
  },

  removeFavorite: (favoriteId: string): Promise<void> => {
    return request.delete(`/ai/favorites/${favoriteId}`)
  },

  createScheduledTask: (userId: string, queryText: string, scheduleTime: string): Promise<{ taskId: string }> => {
    return request.post('/ai/schedule', { userId, queryText, scheduleTime })
  },

  getScheduledTasks: (userId: string): Promise<ScheduledTask[]> => {
    return request.get('/ai/schedule', { params: { userId } })
  },

  deleteScheduledTask: (taskId: string): Promise<void> => {
    return request.delete(`/ai/schedule/${taskId}`)
  }
}