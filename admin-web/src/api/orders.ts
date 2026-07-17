import request from '../utils/request';
import type { ApiResponse, Order, PageData } from '../types/api';

export interface OrderQuery {
  page: number;
  pageSize: number;
  businessType?: string;
  sourceChannel?: string;
  agentId?: number;
  auditStatus?: string;
  exportStatus?: string;
  startTime?: string;
  endTime?: string;
  includeDeleted?: boolean;
  orderId?: number;
  keyword?: string;
}

export interface OrderPayload {
  businessType: string;
  name: string;
  phone: string;
  businessNumber?: string;
  sourceChannel: string;
  agentId?: number;
  auditStatus?: string;
  remark?: string;
  studentNo?: string;
  idCardLastSix?: string;
  licenseType?: string;
  classType?: string;
  paymentAmount?: number;
  renewalAmount?: number;
}

export type NetworkExportFilters = Pick<
  OrderQuery,
  'sourceChannel' | 'agentId' | 'exportStatus' | 'startTime' | 'endTime'
>;

export const orderApi = {
  list: (params: OrderQuery) =>
    request.get<never, ApiResponse<PageData<Order>>>('/orders', { params }),
  detail: (id: number) => request.get<never, ApiResponse<OrderPayload & Order>>(`/orders/${id}`),
  create: (data: OrderPayload) => request.post<never, ApiResponse<{ id: number }>>('/orders', data),
  update: (id: number, data: OrderPayload) => request.put(`/orders/${id}`, data),
  confirm: (id: number) => request.post(`/orders/${id}/confirm`),
  toggleAuditStatus: (id: number) => request.post(`/orders/${id}/audit-status/toggle`),
  void: (id: number) => request.delete(`/orders/${id}`),
  restore: (id: number) => request.post(`/orders/${id}/restore`),
  exportableNetworkCount: (params: Partial<NetworkExportFilters>) =>
    request.get<never, ApiResponse<PageData<Order>>>('/orders', {
      params: {
        page: 1,
        pageSize: 1,
        businessType: 'CAMPUS_NETWORK',
        auditStatus: 'CONFIRMED',
        includeDeleted: false,
        ...params,
      },
    }),
  exportNetwork: (params: Partial<OrderQuery>) =>
    request.get<never, Blob>('/orders/campus-network/export', {
      params,
      responseType: 'blob',
    }),
};
