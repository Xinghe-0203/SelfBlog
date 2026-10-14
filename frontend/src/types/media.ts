/**
 * 媒体文件相关类型定义
 */

/** 媒体类型 */
export type MediaType = 'image' | 'video' | 'file'

/** 上传类型 */
export type UploadType = 'article' | 'circle' | 'avatar' | 'cover'

/** 媒体信息 */
export interface Media {
  id: number
  userId: number
  fileName: string
  originalName: string
  filePath: string
  fileUrl: string
  fileType: string
  fileSize: number
  mediaType: MediaType
  createdAt: string
}

/** 上传进度 */
export interface UploadProgress {
  loaded: number
  total: number
  percent: number
}

/** 上传响应 */
export interface UploadResponse {
  id: number
  url: string
  fileUrl: string
  fileName: string
  fileSize: number
  fileType: string
}
