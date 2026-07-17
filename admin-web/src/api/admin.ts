import request from '../utils/request';
import type { ApiResponse, PageData } from '../types/api';
export const agentApi = {
  list: (params: object) => request.get<never, ApiResponse<PageData<any>>>('/agents', { params }),
  detail: (id: number) => request.get<never, ApiResponse<any>>(`/agents/${id}`),
  create: (data: object) => request.post('/agents', data),
  update: (id: number, data: object) => request.put(`/agents/${id}`, data),
  status: (id: number, status: number) => request.put(`/agents/${id}/status`, { status }),
};
export const issueApi = {
  list: (params: object) => request.get<never, ApiResponse<PageData<any>>>('/issues', { params }),
  detail: (id: number) => request.get<never, ApiResponse<any>>(`/issues/${id}`),
  status: (id: number, data: object) => request.put(`/issues/${id}/status`, data),
};
export const configApi = {
  list: () => request.get<never, ApiResponse<any[]>>('/configs'),
  update: (key: string, value: string) => request.put(`/configs/${key}`, { value }),
};
export const userApi = {
  list: (params: object) =>
    request.get<never, ApiResponse<PageData<any>>>('/system/users', { params }),
  create: (data: object) => request.post('/system/users', data),
  update: (id: number, data: object) => request.put(`/system/users/${id}`, data),
  status: (id: number, status: number) => request.put(`/system/users/${id}/status`, { status }),
};
export const logApi = {
  list: (params: object) =>
    request.get<never, ApiResponse<PageData<any>>>('/operation-logs', {
      params,
    }),
};
