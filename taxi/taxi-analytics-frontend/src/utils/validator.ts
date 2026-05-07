// 日期格式校验
export const isValidDate = (date: string): boolean => {
  const regex = /^\d{4}-\d{2}-\d{2}$/
  if (!regex.test(date)) return false
  const d = new Date(date)
  return d instanceof Date && !isNaN(d.getTime())
}

// 日期范围校验
export const validateDateRange = (
  startDate: string,
  endDate: string,
  maxDays: number = 90,
  minDate: string = '2025-01-01',
  maxDate: string = '2025-03-31'
): { valid: boolean; message?: string } => {
  if (!startDate || !endDate) {
    return { valid: false, message: '请选择日期范围' }
  }
  
  if (!isValidDate(startDate) || !isValidDate(endDate)) {
    return { valid: false, message: '日期格式错误' }
  }
  
  const start = new Date(startDate)
  const end = new Date(endDate)
  
  if (start > end) {
    return { valid: false, message: '开始日期不能大于结束日期' }
  }
  
  const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1
  if (days > maxDays) {
    return { valid: false, message: `日期范围不能超过${maxDays}天` }
  }
  
  const min = new Date(minDate)
  const max = new Date(maxDate)
  if (start < min || end > max) {
    return { valid: false, message: `日期范围超出数据范围(${minDate} ~ ${maxDate})` }
  }
  
  return { valid: true }
}

// 分页参数校验
export const validatePageParams = (pageNum: number, pageSize: number): { valid: boolean; message?: string } => {
  if (pageNum < 1) {
    return { valid: false, message: '页码必须大于0' }
  }
  if (pageSize < 1 || pageSize > 500) {
    return { valid: false, message: '每页条数必须在1-500之间' }
  }
  return { valid: true }
}

// TOP N校验
export const validateTopN = (topN: number): { valid: boolean; message?: string } => {
  if (topN < 1 || topN > 100) {
    return { valid: false, message: 'TOP N必须在1-100之间' }
  }
  return { valid: true }
}