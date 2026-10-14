export type RoleType = 'ADMIN' | 'FLEET_MANAGER' | 'DISPATCHER' | 'DRIVER' | 'VIEWER';

export interface UserDto {
  userId: number;
  email: string;
  fullName: string;
  roles: RoleType[];
  active: boolean;
  createdAt?: string;
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
