/**
 * 举报接口
 */

import api from './index'
import type { ApiResponse, PaginatedResponse, Report, CreateReportRequest } from '@/types'

export const reportApi = {
  /**
   * 提交举报
   */
  createReport(data: CreateReportRequest): Promise<ApiResponse<Report>> {
    return api.post('/report', data)
  },

  /**
   * 获取我的举报
   */
  getMyReports(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<PaginatedResponse<Report>> {
    return api.get('/report/my', { params })
  }
}
