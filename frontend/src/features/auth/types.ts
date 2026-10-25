export type RoleType = 'ADMIN' | 'FLEET_MANAGER' | 'DISPATCHER' | 'DRIVER' | 'VIEWER';

export interface UserDto {
  userId: number;
  email: string;
  fullName: string;
  roles: RoleType[];
  active: boolean;
  createdAt?: string;
  tenantId?: number;
  companyName?: string;
  warehouseName?: string;
}

export interface SendSignupOtpRequest {
  email: string;
  companyName: string;
  warehouseName: string;
  fullName: string;
}

export interface VerifySignupOtpRequest {
  email: string;
  otp: string;
  companyName: string;
  warehouseName: string;
  fullName: string;
}

export interface SendLoginOtpRequest {
  email: string;
}

export interface VerifyLoginOtpRequest {
  email: string;
  otp: string;
}

export interface OtpSentResponse {
  email: string;
  purpose: 'SIGNUP' | 'LOGIN';
  expiresInSeconds: number;
  cooldownSeconds: number;
  message: string;
}

export interface UserCreateRequest {
  email: string;
  password: string;
  fullName: string;
  roles: RoleType[];
}

export interface UserUpdateRequest {
  fullName: string;
  roles: RoleType[];
  active: boolean;
}

export interface UserStatusUpdateRequest {
  active: boolean;
  reason?: string;
}

export interface AdminPasswordResetRequest {
  newPassword: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AuditLogDto {
  id: number;
  userId: number;
  action: string;
  entityType?: string;
  entityId?: string;
  ipAddress?: string;
  detailsJson?: string;
  createdAt: string;
}

export interface CsrfResponseData {
  token: string;
  headerName: string;
  parameterName: string;
}
