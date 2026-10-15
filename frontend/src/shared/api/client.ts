import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { ApiErrorResponse } from './types';

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match ? decodeURIComponent(match[3]) : null;
}

export const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Normalize /v1/ prefix to /api/v1/ and attach CSRF Token for mutating requests
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (config.url && config.url.startsWith('/v1/')) {
      config.url = '/api' + config.url;
    }
    const method = config.method?.toUpperCase();
    if (method && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      const csrfToken = getCookie('XSRF-TOKEN');
      if (csrfToken && config.headers) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Normalize server errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.data) {
      return Promise.reject(error.response.data);
    }
    const fallbackError: ApiErrorResponse = {
      success: false,
      status: error.response?.status || 500,
      errorCode: 'NETWORK_ERROR',
      message: error.message || 'Unable to communicate with FleetX backend.',
      path: error.config?.url || '',
      timestamp: new Date().toISOString(),
    };
    return Promise.reject(fallbackError);
  }
);
