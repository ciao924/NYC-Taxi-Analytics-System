import request from './request'

export interface AiQueryRequest {
  question: string
}

export interface ChartData {
  x: string[]
  y: number[]
}

export interface AiQueryResponse {
  chart: string
  data: ChartData
}

export const aiApi = {
  query: (data: AiQueryRequest): Promise<AiQueryResponse> => {
    return request.post('/ai/query', data)
  },

  health: (): Promise<string> => {
    return request.get('/ai/health')
  },

  getSessions: (): Promise<any[]> => {
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

  getSessionMessages: (sessionId: string): Promise<any[]> => {
    return request.get(`/ai/sessions/${sessionId}/messages`)
  },

  getSuggestions: (): Promise<any[]> => {
    return request.get('/ai/suggestions')
  },

  getQueryHistory: (limit: number = 20): Promise<any[]> => {
    return request.get('/ai/history', { limit })
  },

  saveFeedback: (messageId: string, feedback: string): Promise<void> => {
    return request.post('/ai/feedback', { messageId, feedback })
  },

  addFavorite: (userId: string, queryText: string): Promise<void> => {
    return request.post('/ai/favorites', { userId, queryText })
  },

  getFavorites: (userId: string): Promise<any[]> => {
    return request.get('/ai/favorites', { userId })
  },

  removeFavorite: (favoriteId: string): Promise<void> => {
    return request.delete(`/ai/favorites/${favoriteId}`)
  },

  createScheduledTask: (userId: string, queryText: string, scheduleTime: string): Promise<{ taskId: string }> => {
    return request.post('/ai/schedule', { userId, queryText, scheduleTime })
  },

  getScheduledTasks: (userId: string): Promise<any[]> => {
    return request.get('/ai/schedule', { userId })
  },

  deleteScheduledTask: (taskId: string): Promise<void> => {
    return request.delete(`/ai/schedule/${taskId}`)
  }
}