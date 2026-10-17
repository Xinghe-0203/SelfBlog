import api from './index'
import type { ApiResponse } from '@/types'
import type { PageResult } from './site'

export interface AlbumPhoto {
  id: number
  imageUrl: string
  caption?: string
  sortOrder: number
}

export interface PhotoAlbum {
  id: number
  title: string
  description?: string
  privateAlbum: boolean
  locked: boolean
  coverUrl?: string
  photoCount: number
  sortOrder: number
  createTime: string
  photos?: AlbumPhoto[]
}

export interface AlbumForm {
  title: string
  description: string
  privateAlbum: boolean
  password: string
  sortOrder: number
}

export const galleryApi = {
  getAlbums(): Promise<ApiResponse<PhotoAlbum[]>> {
    return api.get('/gallery/albums')
  },
  getAlbum(id: number, token?: string): Promise<ApiResponse<PhotoAlbum>> {
    return api.get(`/gallery/albums/${id}`, { params: token ? { token } : undefined })
  },
  unlock(id: number, password: string): Promise<ApiResponse<{ accessToken: string }>> {
    return api.post(`/gallery/albums/${id}/unlock`, { password })
  },
  getAdminAlbums(): Promise<ApiResponse<PhotoAlbum[]>> {
    return api.get('/admin/albums')
  },
  createAlbum(data: AlbumForm): Promise<ApiResponse<number>> {
    return api.post('/admin/albums', data)
  },
  updateAlbum(id: number, data: AlbumForm): Promise<ApiResponse> {
    return api.put(`/admin/albums/${id}`, data)
  },
  deleteAlbum(id: number): Promise<ApiResponse> {
    return api.delete(`/admin/albums/${id}`)
  },
  uploadPhotos(id: number, files: File[]): Promise<ApiResponse<PhotoAlbum[]>> {
    const body = new FormData()
    files.forEach((file) => body.append('files', file))
    return api.post(`/admin/albums/${id}/photos`, body, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deletePhoto(albumId: number, photoId: number): Promise<ApiResponse> {
    return api.delete(`/admin/albums/${albumId}/photos/${photoId}`)
  },
  getCommentPage<T>(params: {
    pageNum?: number
    pageSize?: number
  }): Promise<ApiResponse<PageResult<T>>> {
    return api.get('/admin/guest-comments', { params })
  }
}
