import { apiUrl } from '../config/env';

interface ApiEnvelope<T> {
  code: number;
  message?: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}

function clearSession() {
  uni.removeStorageSync('token');
  uni.removeStorageSync('user');
}

function handleUnauthorized() {
  clearSession();
  uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' });
  setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 300);
}

function messageOf(value: unknown, fallback: string) {
  if (value && typeof value === 'object' && 'message' in value) {
    const message = (value as { message?: unknown }).message;
    if (typeof message === 'string' && message) return message;
  }
  return fallback;
}

function parseUploadBody(value: string): ApiEnvelope<unknown> | null {
  try {
    return JSON.parse(value) as ApiEnvelope<unknown>;
  } catch {
    return null;
  }
}

export function request<T = unknown>(
  url: string,
  method: UniApp.RequestOptions['method'] = 'GET',
  data?: UniApp.RequestOptions['data'],
): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: apiUrl(url),
      method,
      data,
      header: { Authorization: `Bearer ${uni.getStorageSync('token') || ''}` },
      success: (response) => {
        const body = response.data as ApiEnvelope<T>;
        if (response.statusCode === 401) {
          handleUnauthorized();
          reject(body);
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || !body || body.code !== 200) {
          uni.showToast({
            title: messageOf(body, `请求失败（${response.statusCode}）`),
            icon: 'none',
          });
          reject(body || response);
          return;
        }
        resolve(body.data);
      },
      fail: (error) => {
        uni.showToast({ title: '网络连接失败，请稍后重试', icon: 'none' });
        reject(error);
      },
    });
  });
}

export function uploadFile<T = unknown>(url: string, filePath: string, name = 'file'): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: apiUrl(url),
      filePath,
      name,
      header: { Authorization: `Bearer ${uni.getStorageSync('token') || ''}` },
      success: (response) => {
        const body = parseUploadBody(response.data) as ApiEnvelope<T> | null;
        if (response.statusCode === 401) {
          handleUnauthorized();
          reject(body || response);
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || !body || body.code !== 200) {
          uni.showToast({
            title: messageOf(body, `图片上传失败（${response.statusCode}）`),
            icon: 'none',
          });
          reject(body || response);
          return;
        }
        resolve(body.data);
      },
      fail: (error) => {
        uni.showToast({ title: '图片上传失败，请检查网络', icon: 'none' });
        reject(error);
      },
    });
  });
}
