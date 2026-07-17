import request from '../utils/request';
import type { ApiResponse } from '../types/api';

export interface MiniCategory {
  id: number;
  categoryCode: string;
  categoryName: string;
  description?: string;
  sortOrder: number;
  status: number;
}
export interface MiniArticle {
  id: number;
  articleNo: string;
  categoryId: number;
  categoryName?: string;
  title: string;
  subtitle?: string;
  summary?: string;
  coverMediaId?: number;
  contentBlocksJson?: string;
  publishStatus: string;
  sortOrder: number;
}
export interface MiniPlace {
  id: number;
  placeNo: string;
  categoryId: number;
  categoryName?: string;
  placeName: string;
  longitude: number;
  latitude: number;
  address: string;
  summary?: string;
  contactPhone?: string;
  businessHours?: string;
  detailBlocksJson?: string;
  sortOrder: number;
  status: number;
}

export const miniContentApi = {
  home: () => request.get<never, ApiResponse<any[]>>('/admin/mini/home-config'),
  saveHome: (configs: Record<string, unknown>) =>
    request.put('/admin/mini/home-config', { configs }),
  categories: () => request.get<never, ApiResponse<MiniCategory[]>>('/admin/mini/categories'),
  createCategory: (data: Partial<MiniCategory>) => request.post('/admin/mini/categories', data),
  updateCategory: (id: number, data: Partial<MiniCategory>) =>
    request.put(`/admin/mini/categories/${id}`, data),
  deleteCategory: (id: number) => request.delete(`/admin/mini/categories/${id}`),
  articles: () => request.get<never, ApiResponse<MiniArticle[]>>('/admin/mini/articles'),
  article: (id: number) =>
    request.get<never, ApiResponse<MiniArticle>>(`/admin/mini/articles/${id}`),
  createArticle: (data: Partial<MiniArticle>) => request.post('/admin/mini/articles', data),
  updateArticle: (id: number, data: Partial<MiniArticle>) =>
    request.put(`/admin/mini/articles/${id}`, data),
  publish: (id: number) => request.post(`/admin/mini/articles/${id}/publish`),
  offline: (id: number) => request.post(`/admin/mini/articles/${id}/offline`),
  records: () => request.get<never, ApiResponse<any[]>>('/admin/mini/publish-records'),
  media: () => request.get<never, ApiResponse<any[]>>('/admin/mini/media'),
  uploadMedia: (file: File) => {
    const data = new FormData();
    data.append('file', file);
    return request.post('/admin/mini/media', data);
  },
  deleteMedia: (id: number) => request.delete(`/admin/mini/media/${id}`),
  placeCategories: () => request.get<never, ApiResponse<any[]>>('/admin/mini/places/categories'),
  places: () => request.get<never, ApiResponse<MiniPlace[]>>('/admin/mini/places'),
  place: (id: number) => request.get<never, ApiResponse<MiniPlace>>(`/admin/mini/places/${id}`),
  createPlace: (data: Partial<MiniPlace>) => request.post('/admin/mini/places', data),
  updatePlace: (id: number, data: Partial<MiniPlace>) =>
    request.put(`/admin/mini/places/${id}`, data),
  deletePlace: (id: number) => request.delete(`/admin/mini/places/${id}`),
  users: (keyword = '') =>
    request.get<never, ApiResponse<any[]>>('/admin/mini/users', {
      params: { keyword },
    }),
  setUserRole: (id: number, data: object) => request.put(`/admin/mini/users/${id}/role`, data),
};
