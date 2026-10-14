import api from './index'
import type { ApiResponse } from '@/types'
import type { SiteSettings } from '@/stores/site'
import { getVisitorId } from '@/utils/visitor'

export interface VisitorSummary {
  totalVisitors: number
  todayVisitors: number
  totalPageViews: number
  todayPageViews: number
  articleViews: number
  daily: Array<{ date: string; visitors: number; pageViews: number }>
  topPages: Array<{ path: string; views: number }>
}

export interface VisitorRecord {
  id: number
  visitor: string
  network: string
  userAgent: string
  path: string
  referrer?: string
  time: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const siteApi = {
  getPublicSettings(): Promise<ApiResponse<SiteSettings>> {
    return api.get('/site/public')
  },
  getSettings(): Promise<ApiResponse<SiteSettings>> {
    return api.get('/admin/site-settings')
  },
  updateSettings(data: Partial<SiteSettings>): Promise<ApiResponse<SiteSettings>> {
    return api.put('/admin/site-settings', data)
  },
  trackVisitor(path: string, referrer = ''): Promise<ApiResponse> {
    return api.post('/visitor/track', { visitorId: getVisitorId(), path, referrer })
  },
  getVisitorSummary(): Promise<ApiResponse<VisitorSummary>> {
    return api.get('/admin/visitors/summary')
  },
  getVisitorRecords(params: { pageNum?: number; pageSize?: number }): Promise<ApiResponse<PageResult<VisitorRecord>>> {
    return api.get('/admin/visitors', { params })
  }
}
