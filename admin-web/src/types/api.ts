export type Role = 'OWNER' | 'ADMIN';
export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  role: Role;
}
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
export interface PageData<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}
export type BusinessType = 'CAMPUS_CARD' | 'CAMPUS_NETWORK' | 'DRIVING_SCHOOL' | 'RENEWAL';
export type AuditStatus = 'PENDING' | 'CONFIRMED';
export type SourceChannel = 'ONLINE' | 'AGENT' | 'STORE';
export interface Order {
  id: number;
  orderNo: string;
  businessType: BusinessType;
  name: string;
  phone: string;
  businessNumber?: string;
  sourceChannel: SourceChannel;
  agentId?: number;
  agentName?: string;
  auditStatus: AuditStatus;
  exportStatus?: 'NOT_EXPORTED' | 'EXPORTED';
  createdAt: string;
  remark?: string;
  deleted: boolean;
}
