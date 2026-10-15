import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Radio, Lock, Mail, AlertCircle, ShieldCheck } from 'lucide-react';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../shared/context/AuthContext';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const [email, setEmail] = useState('admin@fleetx.local');
  const [password, setPassword] = useState('DevAdmin123!');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // If already authenticated, redirect to target or home
  useEffect(() => {
    if (isAuthenticated) {
      const target = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/';
      navigate(target, { replace: true });
    }
  }, [isAuthenticated, location.state, navigate]);

  // Bootstrap CSRF token on component mount
  useEffect(() => {
    authApi
      .fetchCsrf()
      .catch(() => {
        // Even if CSRF bootstrap fails, allow attempt since login is CSRF-exempted
      });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please provide both your registered email and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await login(email.trim(), password);
      const target = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/';
      navigate(target, { replace: true });
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'ACCOUNT_DEACTIVATED') {
        setError('Your account has been deactivated. Please contact your system administrator.');
      } else if (apiErr.status === 401 || apiErr.errorCode === 'UNAUTHORIZED' || apiErr.errorCode === 'BAD_CREDENTIALS') {
        setError('Invalid email or password. Please verify your credentials.');
      } else {
        setError(apiErr.message || 'Authentication failed. Please verify credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex align-items-center justify-content-center min-vh-100 bg-light p-3">
      <div className="fx-card shadow-sm" style={{ maxWidth: '440px', width: '100%' }}>
        <div className="text-center mb-4">
          <div className="d-inline-flex bg-primary text-white p-3 rounded-circle mb-2 shadow-sm">
            <Radio size={28} />
          </div>
          <h2 className="h4 fw-bold text-slate-900 mb-1">FleetX Control Tower</h2>
          <p className="text-muted small">Intelligent Fleet &amp; Logistics Management System</p>
        </div>

        {error && (
          <div className="alert alert-danger d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm" role="alert">
            <AlertCircle size={16} className="flex-shrink-0" />
            <div>{error}</div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label small fw-medium text-secondary" htmlFor="login-email">
              Email Address <span className="text-danger">*</span>
            </label>
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-light text-muted">
                <Mail size={14} />
              </span>
              <input
                id="login-email"
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@fleetx.local"
                required
                autoComplete="email"
              />
            </div>
          </div>

          <div className="mb-4">
            <label className="form-label small fw-medium text-secondary" htmlFor="login-password">
              Password <span className="text-danger">*</span>
            </label>
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-light text-muted">
                <Lock size={14} />
              </span>
              <input
                id="login-password"
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                autoComplete="current-password"
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary w-100 btn-sm py-2 fw-medium d-flex align-items-center justify-content-center gap-2"
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                <span>Authenticating Session...</span>
              </>
            ) : (
              <span>Sign In to Control Tower</span>
            )}
          </button>
        </form>

        <div className="mt-4 pt-3 border-top text-center text-muted small" style={{ fontSize: '0.75rem' }}>
          <div className="d-flex align-items-center justify-content-center gap-1 mb-1">
            <ShieldCheck size={14} className="text-success" />
            <span>Spring Security RBAC &bull; Server-Side Session</span>
          </div>
          <div>Development Admin: <code>admin@fleetx.local</code></div>
        </div>
      </div>
    </div>
  );
};
