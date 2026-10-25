import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Shield, Key, Calendar, LogOut, CheckCircle2, XCircle, Lock, AlertCircle } from 'lucide-react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { LoadingState } from '../../../shared/components/LoadingState';
import { ErrorState } from '../../../shared/components/ErrorState';
import { UnauthorizedState } from '../../../shared/components/UnauthorizedState';
import { authApi } from '../api/authApi';
import { UserDto } from '../types';

export const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUnauthenticated, setIsUnauthenticated] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  // Self-service password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdLoading, setPwdLoading] = useState(false);
  const [pwdError, setPwdError] = useState<string | null>(null);
  const [pwdSuccess, setPwdSuccess] = useState<string | null>(null);

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwdError(null);
    setPwdSuccess(null);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPwdError('All password fields are required.');
      return;
    }

    if (newPassword.length < 8) {
      setPwdError('New password must be at least 8 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPwdError('New password and confirmation do not match.');
      return;
    }

    if (newPassword === currentPassword) {
      setPwdError('New password cannot be identical to your current password.');
      return;
    }

    setPwdLoading(true);
    try {
      await authApi.changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });
      setPwdSuccess('Password changed successfully. Your account is secured with the updated credential.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'INVALID_CURRENT_PASSWORD' || apiErr.message?.includes('Current password is incorrect')) {
        setPwdError('The current password entered is incorrect. Please verify and retry.');
      } else if (apiErr.errorCode === 'PASSWORD_REUSE_PREVENTED') {
        setPwdError('New password cannot be identical to your current password.');
      } else {
        setPwdError(apiErr.message || 'Failed to update password. Please check your credentials.');
      }
    } finally {
      setPwdLoading(false);
    }
  };

  const fetchProfile = async () => {
    setLoading(true);
    setError(null);
    setIsUnauthenticated(false);

    try {
      const data = await authApi.getMe();
      setUser(data);
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 401) {
        setIsUnauthenticated(true);
      } else {
        setError(apiErr.message || 'Failed to load user profile session.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await authApi.logout();
    } catch {
      // Continue navigation even if network error on logout
    } finally {
      setLoggingOut(false);
      navigate('/login');
    }
  };

  if (loading) {
    return <LoadingState message="Retrieving authenticated profile session..." />;
  }

  if (isUnauthenticated) {
    return (
      <UnauthorizedState
        message="Your session has expired or you are unauthenticated. Please sign in to access your profile."
      />
    );
  }

  if (error || !user) {
    return (
      <ErrorState
        title="Profile Session Unavailable"
        message={error || 'Could not verify authenticated session details.'}
        onRetry={fetchProfile}
      />
    );
  }

  const isAdmin = user.roles.includes('ADMIN');

  return (
    <div>
      <PageHeader
        title="User Account & Profile"
        subtitle="Manage your identity credentials, assigned role authorities, and active session details."
      >
        <button
          className="btn btn-outline-danger btn-sm d-flex align-items-center gap-2"
          onClick={handleLogout}
          disabled={loggingOut}
        >
          <LogOut size={16} /> {loggingOut ? 'Ending Session...' : 'Sign Out'}
        </button>
      </PageHeader>

      <div className="row g-4">
        {/* Profile & Security Management */}
        <div className="col-12 col-lg-7 d-flex flex-column gap-4">
          <div className="fx-glass-panel p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="d-flex align-items-center gap-3 pb-3 border-bottom mb-4" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
              <div
                className="rounded-circle d-flex align-items-center justify-content-center shadow"
                style={{
                  width: '56px',
                  height: '56px',
                  fontSize: '1.4rem',
                  fontWeight: 700,
                  background: 'linear-gradient(135deg, #E8D3A2 0%, #CFB778 100%)',
                  color: '#201713',
                }}
              >
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div>
                <h3 className="h5 fw-bold mb-1" style={{ color: 'var(--fx-ivory-text)' }}>{user.fullName}</h3>
                <span className="small" style={{ color: 'var(--fx-muted-sand)' }}>{user.email}</span>
              </div>
            </div>

            <div className="row g-3">
              <div className="col-sm-6">
                <label className="small d-block mb-1" style={{ color: 'var(--fx-muted-sand)' }}>User Identifier</label>
                <div className="fw-semibold font-monospace" style={{ color: 'var(--fx-champagne-gold)' }}>
                  <code>#{user.userId}</code>
                </div>
              </div>

              <div className="col-sm-6">
                <label className="small d-block mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Account State</label>
                <div className="d-flex align-items-center gap-2">
                  {user.active ? (
                    <>
                      <CheckCircle2 size={16} style={{ color: '#8CE3A7' }} />
                      <span className="badge fx-badge-success px-2 py-1">
                        Active &amp; Verified
                      </span>
                    </>
                  ) : (
                    <>
                      <XCircle size={16} style={{ color: '#FFA4AC' }} />
                      <span className="badge fx-badge-danger px-2 py-1">
                        Deactivated
                      </span>
                    </>
                  )}
                </div>
              </div>

              <div className="col-12">
                <label className="small d-block mb-2" style={{ color: 'var(--fx-muted-sand)' }}>Assigned RBAC Authorities</label>
                <div className="d-flex flex-wrap gap-2">
                  {user.roles.map((role) => (
                    <span
                      key={role}
                      className="badge fx-badge-gold px-3 py-1.5 fw-medium d-inline-flex align-items-center gap-1"
                    >
                      <Shield size={13} /> {role}
                    </span>
                  ))}
                </div>
              </div>

              {user.createdAt && (
                <div className="col-12">
                  <label className="small d-block mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Account Provisioned</label>
                  <div className="d-flex align-items-center gap-2 small" style={{ color: 'var(--fx-muted-sand)' }}>
                    <Calendar size={14} style={{ color: 'var(--fx-soft-gold)' }} />
                    <span>{new Date(user.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              )}
            </div>

            {isAdmin && (
              <div className="mt-4 pt-3 border-top" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
                <Link to="/admin/users" className="btn btn-sm fx-btn-gold">
                  Go to Administrative User Console &rarr;
                </Link>
              </div>
            )}
          </div>

          {/* Password Change Card */}
          <div className="fx-glass-panel p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <h4 className="fs-6 fw-bold mb-1 d-flex align-items-center gap-2" style={{ color: 'var(--fx-ivory-text)' }}>
              <Lock size={18} style={{ color: 'var(--fx-champagne-gold)' }} /> Update Password
            </h4>
            <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
              Self-service password update. Enter your current password and choose a secure replacement (minimum 8 characters).
            </p>

            {pwdSuccess && (
              <div className="alert alert-success d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm" role="alert">
                <CheckCircle2 size={16} className="text-success flex-shrink-0" />
                <div>{pwdSuccess}</div>
              </div>
            )}

            {pwdError && (
              <div className="alert alert-danger d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm" role="alert">
                <AlertCircle size={16} className="text-danger flex-shrink-0" />
                <div>{pwdError}</div>
              </div>
            )}

            <form onSubmit={handlePasswordSubmit}>
              <div className="mb-3">
                <label className="form-label small fw-medium d-block mb-1" htmlFor="profile-current-password" style={{ color: 'var(--fx-ivory-text)' }}>
                  Current Password <span className="text-danger">*</span>
                </label>
                <div className="input-group input-group-sm">
                  <span
                    className="input-group-text border-end-0"
                    style={{
                      background: 'rgba(32, 23, 19, 0.65)',
                      borderColor: 'rgba(232, 211, 162, 0.22)',
                      color: 'var(--fx-muted-sand)',
                    }}
                  >
                    <Lock size={14} />
                  </span>
                  <input
                    id="profile-current-password"
                    type="password"
                    className="form-control"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter current password"
                    required
                    autoComplete="current-password"
                  />
                </div>
              </div>

              <div className="row g-2 mb-3">
                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-medium d-block mb-1" htmlFor="profile-new-password" style={{ color: 'var(--fx-ivory-text)' }}>
                    New Password <span className="text-danger">*</span>
                  </label>
                  <div className="input-group input-group-sm">
                    <span
                      className="input-group-text border-end-0"
                      style={{
                        background: 'rgba(32, 23, 19, 0.65)',
                        borderColor: 'rgba(232, 211, 162, 0.22)',
                        color: 'var(--fx-muted-sand)',
                      }}
                    >
                      <Lock size={14} />
                    </span>
                    <input
                      id="profile-new-password"
                      type="password"
                      className="form-control"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Min. 8 characters"
                      required
                      autoComplete="new-password"
                    />
                  </div>
                </div>

                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-medium d-block mb-1" htmlFor="profile-confirm-password" style={{ color: 'var(--fx-ivory-text)' }}>
                    Confirm New Password <span className="text-danger">*</span>
                  </label>
                  <div className="input-group input-group-sm">
                    <span
                      className="input-group-text border-end-0"
                      style={{
                        background: 'rgba(32, 23, 19, 0.65)',
                        borderColor: 'rgba(232, 211, 162, 0.22)',
                        color: 'var(--fx-muted-sand)',
                      }}
                    >
                      <Lock size={14} />
                    </span>
                    <input
                      id="profile-confirm-password"
                      type="password"
                      className="form-control"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repeat new password"
                      required
                      autoComplete="new-password"
                    />
                  </div>
                </div>
              </div>

              <button
                type="submit"
                className="btn fx-btn-gold btn-sm px-3 d-inline-flex align-items-center gap-2 shadow"
                disabled={pwdLoading}
              >
                {pwdLoading ? (
                  <>
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                    <span>Updating Password...</span>
                  </>
                ) : (
                  <span>Update Password</span>
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Security & Session Info */}
        <div className="col-12 col-lg-5">
          <div className="fx-glass-panel p-4 rounded-3 border h-100" style={{ borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <h4 className="fs-6 fw-bold mb-3 d-flex align-items-center gap-2" style={{ color: 'var(--fx-ivory-text)' }}>
              <Key size={18} style={{ color: 'var(--fx-champagne-gold)' }} /> Security &amp; Session Guarantees
            </h4>
            <ul className="list-unstyled small d-flex flex-column gap-3 mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
              <li className="d-flex align-items-start gap-2">
                <Shield size={16} style={{ color: '#8CE3A7' }} className="flex-shrink-0 mt-1" />
                <div>
                  <strong style={{ color: 'var(--fx-ivory-text)' }}>Server-Side Session:</strong> Authentication is maintained via Spring Security HTTP session cookies (<code>JSESSIONID</code>) with <code>HttpOnly</code> and <code>SameSite</code> protection.
                </div>
              </li>
              <li className="d-flex align-items-start gap-2">
                <Shield size={16} style={{ color: '#8CE3A7' }} className="flex-shrink-0 mt-1" />
                <div>
                  <strong style={{ color: 'var(--fx-ivory-text)' }}>CSRF Token Validation:</strong> Mutating POST/PUT/PATCH/DELETE calls strictly require synchronized CSRF token headers.
                </div>
              </li>
              <li className="d-flex align-items-start gap-2">
                <Shield size={16} style={{ color: '#8CE3A7' }} className="flex-shrink-0 mt-1" />
                <div>
                  <strong style={{ color: 'var(--fx-ivory-text)' }}>BCrypt Password Protection:</strong> Account credentials are encrypted with salted BCrypt hashing.
                </div>
              </li>
              <li className="d-flex align-items-start gap-2">
                <Shield size={16} style={{ color: '#8CE3A7' }} className="flex-shrink-0 mt-1" />
                <div>
                  <strong style={{ color: 'var(--fx-ivory-text)' }}>Immutable Audit Trail:</strong> All administrative adjustments, logins, and status toggles are recorded with IP addresses and timestamps.
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
