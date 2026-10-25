import React, { useCallback, useEffect, useState } from 'react';
import {
  Plus,
  Search,
  Filter,
  UserCheck,
  Shield,
  Edit,
  KeyRound,
  UserX,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { LoadingState } from '../../../shared/components/LoadingState';
import { ErrorState } from '../../../shared/components/ErrorState';
import { EmptyState } from '../../../shared/components/EmptyState';
import { UnauthorizedState } from '../../../shared/components/UnauthorizedState';
import { authApi } from '../api/authApi';
import {
  RoleType,
  UserDto,
  UserCreateRequest,
  UserUpdateRequest,
  UserStatusUpdateRequest,
  AdminPasswordResetRequest,
} from '../types';
import { CreateUserModal } from '../components/CreateUserModal';
import { EditUserModal } from '../components/EditUserModal';
import { DeactivateConfirmModal } from '../components/DeactivateConfirmModal';
import { PasswordResetModal } from '../components/PasswordResetModal';

const ALL_ROLES: RoleType[] = ['ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER', 'VIEWER'];

export const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unauthorized, setUnauthorized] = useState(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'danger'; message: string } | null>(null);

  // Filters & Pagination
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<RoleType | ''>('');
  const [activeFilter, setActiveFilter] = useState<boolean | ''>('');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);
  const [statusUser, setStatusUser] = useState<UserDto | null>(null);
  const [resetPasswordUser, setResetPasswordUser] = useState<UserDto | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    setUnauthorized(false);

    try {
      const pageResult = await authApi.getUsers({
        page,
        size: pageSize,
        role: roleFilter,
        active: activeFilter,
        search: search.trim() || undefined,
      });
      setUsers(pageResult.content);
      setTotalPages(pageResult.totalPages || 1);
      setTotalElements(pageResult.totalElements || 0);
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 403 || apiErr.status === 401) {
        setUnauthorized(true);
      } else {
        setError(apiErr.message || 'Unable to retrieve user directory.');
      }
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, roleFilter, activeFilter, search]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const showNotification = (type: 'success' | 'danger', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 5000);
  };

  const handleCreateUser = async (data: UserCreateRequest) => {
    await authApi.createUser(data);
    showNotification('success', `User account '${data.email}' was created successfully.`);
    fetchUsers();
  };

  const handleUpdateUser = async (userId: number, data: UserUpdateRequest) => {
    await authApi.updateUser(userId, data);
    showNotification('success', `User account #${userId} updated successfully.`);
    fetchUsers();
  };

  const handleStatusUpdate = async (userId: number, data: UserStatusUpdateRequest) => {
    await authApi.updateUserStatus(userId, data);
    const actionWord = data.active ? 'reactivated' : 'deactivated';
    showNotification('success', `User account #${userId} has been ${actionWord}.`);
    fetchUsers();
  };

  const handlePasswordReset = async (userId: number, data: AdminPasswordResetRequest) => {
    await authApi.resetPassword(userId, data);
    showNotification('success', `Password for user #${userId} reset successfully.`);
  };

  if (unauthorized) {
    return (
      <UnauthorizedState
        message="You lack the required 'ADMIN' authority to view or modify user accounts and roles."
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Identity &amp; Role Management"
        subtitle="Manage administrative accounts, role-based authorities, credentials, and user lifecycle states."
      >
        <button
          className="btn fx-btn-gold btn-sm d-flex align-items-center gap-2 shadow"
          onClick={() => setIsCreateOpen(true)}
        >
          <Plus size={16} /> Provision User
        </button>
      </PageHeader>

      {/* Global alert notifications */}
      {notification && (
        <div
          className={`alert alert-${notification.type} d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm`}
          role="alert"
        >
          {notification.type === 'success' ? (
            <CheckCircle2 size={16} className="flex-shrink-0" />
          ) : (
            <AlertCircle size={16} className="flex-shrink-0" />
          )}
          <div>{notification.message}</div>
        </div>
      )}

      {/* Filters & Search Toolbar */}
      <div className="fx-card mb-4">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-5">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Search size={14} />
              </span>
              <input
                type="search"
                className="form-control"
                placeholder="Search by full name or email address..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>

          <div className="col-6 col-md-3">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Filter size={14} />
              </span>
              <select
                className="form-select"
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value as RoleType | '');
                  setPage(0);
                }}
                aria-label="Filter by assigned role"
              >
                <option value="">All Roles</option>
                {ALL_ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="col-6 col-md-3">
            <select
              className="form-select form-select-sm"
              value={activeFilter === '' ? '' : activeFilter ? 'true' : 'false'}
              onChange={(e) => {
                const val = e.target.value;
                setActiveFilter(val === '' ? '' : val === 'true');
                setPage(0);
              }}
              aria-label="Filter by account state"
            >
              <option value="">All Account Statuses</option>
              <option value="true">Active Only</option>
              <option value="false">Deactivated Only</option>
            </select>
          </div>

          <div className="col-12 col-md-1 text-md-end">
            <button
              className="btn btn-outline-secondary btn-sm p-1 px-2"
              onClick={() => fetchUsers()}
              title="Refresh Directory"
              aria-label="Refresh user list"
            >
              <RefreshCw size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Users Table / Content Area */}
      <div className="fx-card mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <UserCheck size={18} className="text-primary" />
            <span className="fw-semibold small">
              Directory Accounts ({totalElements})
            </span>
          </div>
          <span className="text-muted small">Spring Security RBAC Protected</span>
        </div>

        {loading ? (
          <LoadingState message="Loading directory accounts..." />
        ) : error ? (
          <ErrorState
            title="Unable to load user accounts"
            message={error}
            onRetry={fetchUsers}
          />
        ) : users.length === 0 ? (
          <EmptyState
            title="No Users Found"
            description="No user accounts match the selected search terms and filters."
            actionText="Reset Search Filters"
            onAction={() => {
              setSearch('');
              setRoleFilter('');
              setActiveFilter('');
              setPage(0);
            }}
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="fx-table align-middle">
                <thead>
                  <tr>
                    <th style={{ width: '80px' }}>User ID</th>
                    <th>Full Name</th>
                    <th>Email Address</th>
                    <th>Assigned Roles</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th className="text-end" style={{ width: '190px' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.userId}>
                      <td>
                        <code>#{u.userId}</code>
                      </td>
                      <td className="fw-medium text-slate-900">{u.fullName}</td>
                      <td>{u.email}</td>
                      <td>
                        <div className="d-flex flex-wrap gap-1">
                          {u.roles.map((role) => (
                            <span
                              key={role}
                              className="badge bg-light text-primary border border-primary-subtle px-2 py-1"
                              style={{ fontSize: '0.7rem' }}
                            >
                              <Shield size={10} className="me-1 inline" />
                              {role}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        {u.active ? (
                          <span className="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                            ACTIVE
                          </span>
                        ) : (
                          <span className="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1">
                            DEACTIVATED
                          </span>
                        )}
                      </td>
                      <td className="text-muted small">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                      </td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm" role="group">
                          <button
                            type="button"
                            className="btn btn-outline-secondary py-1 px-2"
                            onClick={() => setEditingUser(u)}
                            title="Edit User Details & Roles"
                          >
                            <Edit size={13} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary py-1 px-2"
                            onClick={() => setResetPasswordUser(u)}
                            title="Admin Password Reset"
                          >
                            <KeyRound size={13} />
                          </button>
                          <button
                            type="button"
                            className={`btn ${
                              u.active ? 'btn-outline-danger' : 'btn-outline-success'
                            } py-1 px-2`}
                            onClick={() => setStatusUser(u)}
                            title={u.active ? 'Deactivate Account' : 'Reactivate Account'}
                          >
                            {u.active ? <UserX size={13} /> : <UserCheck size={13} />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="d-flex align-items-center justify-content-between pt-3 border-top mt-2">
                <div className="small text-muted">
                  Showing page {page + 1} of {totalPages} ({totalElements} total users)
                </div>
                <div className="d-flex align-items-center gap-1">
                  <button
                    className="btn btn-outline-secondary btn-sm p-1 px-2"
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(p - 1, 0))}
                    aria-label="Previous page"
                  >
                    <ChevronLeft size={16} />
                  </button>
                  <span className="small px-2 fw-medium text-slate-800">
                    {page + 1}
                  </span>
                  <button
                    className="btn btn-outline-secondary btn-sm p-1 px-2"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                    aria-label="Next page"
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Modals */}
      <CreateUserModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={handleCreateUser}
      />

      <EditUserModal
        isOpen={Boolean(editingUser)}
        user={editingUser}
        onClose={() => setEditingUser(null)}
        onSubmit={handleUpdateUser}
      />

      <DeactivateConfirmModal
        isOpen={Boolean(statusUser)}
        user={statusUser}
        onClose={() => setStatusUser(null)}
        onConfirm={handleStatusUpdate}
      />

      <PasswordResetModal
        isOpen={Boolean(resetPasswordUser)}
        user={resetPasswordUser}
        onClose={() => setResetPasswordUser(null)}
        onSubmit={handlePasswordReset}
      />
    </div>
  );
};
