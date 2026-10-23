import { apiClient } from '../../../shared/api/client';
import { ApiResponse, PageResponse } from '../../../shared/api/types';
import {
  UserDto,
  UserCreateRequest,
  UserUpdateRequest,
  UserStatusUpdateRequest,
  AdminPasswordResetRequest,
  PasswordChangeRequest,
  AuditLogDto,
  CsrfResponseData,
  RoleType,
  SendSignupOtpRequest,
  VerifySignupOtpRequest,
  SendLoginOtpRequest,
  VerifyLoginOtpRequest,
  OtpSentResponse,
} from '../types';

export const authApi = {
  /**
   * Initializes CSRF token and sets headers on apiClient
   */
  async fetchCsrf(): Promise<CsrfResponseData> {
    const response = await apiClient.get<ApiResponse<CsrfResponseData>>('/api/v1/auth/csrf');
    const data = response.data.data;
    if (data?.token) {
      if (data.headerName) {
        apiClient.defaults.headers.common[data.headerName] = data.token;
      }
      apiClient.defaults.headers.common['X-XSRF-TOKEN'] = data.token;
      apiClient.defaults.headers.common['X-CSRF-TOKEN'] = data.token;
    }
    return data;
  },

  /**
   * Request email verification code for new warehouse owner signup
   */
  async sendSignupOtp(payload: SendSignupOtpRequest): Promise<OtpSentResponse> {
    const response = await apiClient.post<ApiResponse<OtpSentResponse>>('/api/v1/auth/otp/send-signup', payload);
    return response.data.data;
  },

  /**
   * Verify email verification code and provision new warehouse tenant + admin account
   */
  async verifySignupOtp(payload: VerifySignupOtpRequest): Promise<UserDto> {
    const response = await apiClient.post<ApiResponse<UserDto>>('/api/v1/auth/otp/verify-signup', payload);
    return response.data.data;
  },

  /**
   * Request passwordless email login OTP challenge
   */
  async sendLoginOtp(payload: SendLoginOtpRequest): Promise<OtpSentResponse> {
    const response = await apiClient.post<ApiResponse<OtpSentResponse>>('/api/v1/auth/otp/send-login', payload);
    return response.data.data;
  },

  /**
   * Verify passwordless login OTP and establish session
   */
  async verifyLoginOtp(payload: VerifyLoginOtpRequest): Promise<UserDto> {
    const response = await apiClient.post<ApiResponse<UserDto>>('/api/v1/auth/otp/verify-login', payload);
    return response.data.data;
  },

  /**
   * Performs user login
   */
  async login(payload: { email: string; password: string }): Promise<UserDto> {
    const response = await apiClient.post<ApiResponse<UserDto>>('/api/v1/auth/login', payload);
    return response.data.data;
  },

  /**
   * Logs out the current user and invalidates session
   */
  async logout(): Promise<void> {
    await apiClient.post<ApiResponse<void>>('/api/v1/auth/logout');
  },

  /**
   * Fetches currently authenticated user
   */
  async getMe(): Promise<UserDto> {
    const response = await apiClient.get<ApiResponse<UserDto>>('/api/v1/auth/me');
    return response.data.data;
  },

  /**
   * Self-service password change for authenticated user
   */
  async changePassword(payload: PasswordChangeRequest): Promise<void> {
    await apiClient.put<ApiResponse<void>>('/api/v1/auth/password', payload);
  },

  /**
   * Fetches paginated user list for admin
   */
  async getUsers(params: {
    page?: number;
    size?: number;
    role?: RoleType | '';
    active?: boolean | '';
    search?: string;
  }): Promise<PageResponse<UserDto>> {
    const queryParams: Record<string, string | number | boolean> = {};
    if (params.page !== undefined) queryParams.page = params.page;
    if (params.size !== undefined) queryParams.size = params.size;
    if (params.role) queryParams.role = params.role;
    if (params.active !== '' && params.active !== undefined) queryParams.active = params.active;
    if (params.search) queryParams.search = params.search;

    const response = await apiClient.get<ApiResponse<PageResponse<UserDto>>>('/api/v1/admin/users', {
      params: queryParams,
    });
    return response.data.data;
  },

  /**
   * Fetches single user by ID
   */
  async getUserById(id: number): Promise<UserDto> {
    const response = await apiClient.get<ApiResponse<UserDto>>(`/api/v1/admin/users/${id}`);
    return response.data.data;
  },

  /**
   * Creates a new user account
   */
  async createUser(payload: UserCreateRequest): Promise<UserDto> {
    const response = await apiClient.post<ApiResponse<UserDto>>('/api/v1/admin/users', payload);
    return response.data.data;
  },

  /**
   * Updates an existing user
   */
  async updateUser(id: number, payload: UserUpdateRequest): Promise<UserDto> {
    const response = await apiClient.put<ApiResponse<UserDto>>(`/api/v1/admin/users/${id}`, payload);
    return response.data.data;
  },

  /**
   * Activates or deactivates user status
   */
  async updateUserStatus(id: number, payload: UserStatusUpdateRequest): Promise<UserDto> {
    const response = await apiClient.patch<ApiResponse<UserDto>>(`/api/v1/admin/users/${id}/status`, payload);
    return response.data.data;
  },

  /**
   * Admin-triggered password reset
   */
  async resetPassword(id: number, payload: AdminPasswordResetRequest): Promise<void> {
    await apiClient.post<ApiResponse<void>>(`/api/v1/admin/users/${id}/password-reset`, payload);
  },

  /**
   * Fetches paginated security audit logs
   */
  async getAuditLogs(params: {
    page?: number;
    size?: number;
    action?: string;
    userId?: number;
  }): Promise<PageResponse<AuditLogDto>> {
    const queryParams: Record<string, string | number | boolean> = {};
    if (params.page !== undefined) queryParams.page = params.page;
    if (params.size !== undefined) queryParams.size = params.size;
    if (params.action) queryParams.action = params.action;
    if (params.userId !== undefined) queryParams.userId = params.userId;

    const response = await apiClient.get<ApiResponse<PageResponse<AuditLogDto>>>('/api/v1/admin/audit-logs', {
      params: queryParams,
    });
    return response.data.data;
  },
};
