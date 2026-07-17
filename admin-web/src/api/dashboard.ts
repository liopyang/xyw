import request from '../utils/request';
import type { ApiResponse, BusinessType } from '../types/api';

export interface CardStat {
  businessType: BusinessType;
  today: number;
  month: number;
}
export interface TrendPoint {
  date: string;
  count: number;
}
export interface RankingItem {
  agentId: number;
  agentName: string;
  count: number;
}
export interface Todos {
  pendingOrders: number;
  unexportedNetworks: number;
  pendingIssues: number;
  processingIssues: number;
  monthlyAgentOrders: number;
}
export interface TrendQuery {
  businessType: string;
  range: string;
  start?: string;
  end?: string;
}

export const dashboardApi = {
  cards: () => request.get<never, ApiResponse<CardStat[]>>('/dashboard/cards'),
  trend: (params: TrendQuery) =>
    request.get<never, ApiResponse<TrendPoint[]>>('/dashboard/trend', {
      params,
    }),
  ranking: (params: Record<string, string>) =>
    request.get<never, ApiResponse<RankingItem[]>>('/dashboard/agent-ranking', {
      params,
    }),
  todos: () => request.get<never, ApiResponse<Todos>>('/dashboard/todos'),
};
