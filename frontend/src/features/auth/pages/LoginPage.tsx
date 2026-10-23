import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import {
  Radio,
  Lock,
  Mail,
  AlertCircle,
  ShieldCheck,
  CheckCircle2,
  RefreshCw,
  ArrowRight,
  ArrowLeft,
} from 'lucide-react';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../shared/context/AuthContext';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loginWithOtp, isAuthenticated } = useAuth();

  // Mode: 'PASSWORD' or 'OTP'
  const [authMode, setAuthMode] = useState<'PASSWORD' | 'OTP'>('PASSWORD');

  // Password Login State
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // OTP Login State
  const [otpEmail, setOtpEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  // Common UI State
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // If already authenticated, redirect to target or dashboard
  useEffect(() => {
    if (isAuthenticated) {
      const target = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/dashboard';
      navigate(target, { replace: true });
    }
  }, [isAuthenticated, location.state, navigate]);

  // Bootstrap CSRF token on component mount
  useEffect(() => {
    authApi.fetchCsrf().catch(() => {});
  }, []);

  // Cooldown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  // Handle Standard Password Login
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please provide both your registered email and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await login(email.trim(), password);
      const target = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/dashboard';
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

  // Handle Send Login OTP
  const handleSendLoginOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpEmail.trim()) {
      setError('Please enter your registered email address.');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await authApi.sendLoginOtp({ email: otpEmail.trim().toLowerCase() });
      setSuccessMsg(res.message || `Login code sent to ${otpEmail.trim()}`);
      setCooldown(res.cooldownSeconds || 60);
      setOtpSent(true);
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.status === 404 || apiErr.errorCode === 'RESOURCE_NOT_FOUND') {
        setError('No account found with this email. Please register your warehouse first.');
      } else if (apiErr.errorCode === 'RATE_LIMIT_COOLDOWN') {
        setError(apiErr.message || 'Please wait before requesting a new code.');
      } else {
        setError(apiErr.message || 'Failed to send login code. Please verify email.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Handle Verify Login OTP
  const handleVerifyLoginOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanOtp = otpCode.trim();
    if (cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
      setError('Please enter the 6-digit login verification code.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (loginWithOtp) {
        await loginWithOtp(otpEmail.trim().toLowerCase(), cleanOtp);
      } else {
        await authApi.verifyLoginOtp({ email: otpEmail.trim().toLowerCase(), otp: cleanOtp });
      }
      const target = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/dashboard';
      navigate(target, { replace: true });
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'INCORRECT_OTP') {
        setError(apiErr.message || 'Incorrect verification code. Please check your email.');
      } else if (apiErr.errorCode === 'OTP_EXPIRED') {
        setError('Verification code has expired. Please request a new code.');
      } else if (apiErr.errorCode === 'MAX_ATTEMPTS_EXCEEDED') {
        setError('Maximum verification attempts exceeded. Please request a new code.');
      } else {
        setError(apiErr.message || 'Login verification failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex align-items-center justify-content-center min-vh-100 bg-light p-3">
      <div className="fx-card shadow-sm" style={{ maxWidth: '440px', width: '100%', background: '#ffffff' }}>
        <div className="text-center mb-3">
          <Link to="/" className="text-decoration-none">
            <div className="d-inline-flex bg-primary text-white p-3 rounded-circle mb-2 shadow-sm">
              <Radio size={28} />
            </div>
          </Link>
          <h2 className="h4 fw-bold text-slate-900 mb-1">FleetX Control Tower</h2>
          <p className="text-muted small">Intelligent Fleet &amp; Logistics Management System</p>
        </div>

        {/* Method Toggle Buttons */}
        <div className="d-flex bg-light p-1 rounded-2 mb-3 border">
          <button
            type="button"
            className={`btn btn-sm flex-fill fw-medium border-0 ${authMode === 'PASSWORD' ? 'bg-white shadow-sm text-primary' : 'text-muted'}`}
            onClick={() => {
              setAuthMode('PASSWORD');
              setError(null);
              setSuccessMsg(null);
            }}
          >
            Password Login
          </button>
          <button
            type="button"
            className={`btn btn-sm flex-fill fw-medium border-0 ${authMode === 'OTP' ? 'bg-white shadow-sm text-primary' : 'text-muted'}`}
            onClick={() => {
              setAuthMode('OTP');
              setError(null);
              setSuccessMsg(null);
              if (!otpEmail && email) setOtpEmail(email);
            }}
          >
            Email Code (OTP)
          </button>
        </div>

        {error && (
          <div className="alert alert-danger d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm" role="alert">
            <AlertCircle size={16} className="flex-shrink-0" />
            <div>{error}</div>
          </div>
        )}

        {successMsg && (
          <div className="alert alert-success d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm" role="alert">
            <CheckCircle2 size={16} className="flex-shrink-0" />
            <div>{successMsg}</div>
          </div>
        )}

        {authMode === 'PASSWORD' ? (
          /* FORM A: PASSWORD AUTHENTICATION (Backwards Compatible) */
          <form onSubmit={handlePasswordSubmit}>
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
        ) : (
          /* FORM B: PASSWORDLESS EMAIL OTP AUTHENTICATION */
          <div>
            {!otpSent ? (
              <form onSubmit={handleSendLoginOtp}>
                <div className="mb-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="otp-login-email">
                    Registered Work Email <span className="text-danger">*</span>
                  </label>
                  <div className="input-group input-group-sm">
                    <span className="input-group-text bg-light text-muted">
                      <Mail size={14} />
                    </span>
                    <input
                      id="otp-login-email"
                      type="email"
                      className="form-control"
                      value={otpEmail}
                      onChange={(e) => setOtpEmail(e.target.value)}
                      placeholder="owner@yourwarehouse.com"
                      required
                      autoComplete="email"
                    />
                  </div>
                  <small className="text-muted" style={{ fontSize: '0.72rem' }}>
                    We will send a 6-digit login code to your inbox.
                  </small>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary w-100 btn-sm py-2 fw-medium d-flex align-items-center justify-content-center gap-2"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                      <span>Sending Login Code...</span>
                    </>
                  ) : (
                    <>
                      <span>Send Login Code</span>
                      <ArrowRight size={14} />
                    </>
                  )}
                </button>
              </form>
            ) : (
              <form onSubmit={handleVerifyLoginOtp}>
                <div className="mb-4 text-center">
                  <label className="form-label small fw-medium text-secondary d-block mb-2" htmlFor="otp-login-code">
                    Enter 6-Digit Code sent to {otpEmail}
                  </label>
                  <div className="d-flex justify-content-center mb-2">
                    <input
                      id="otp-login-code"
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      className="form-control text-center font-monospace fs-4 fw-bold letter-spacing-lg"
                      style={{ maxWidth: '240px', letterSpacing: '0.35em' }}
                      placeholder="123456"
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                      autoFocus
                      required
                    />
                  </div>
                  <div className="d-flex align-items-center justify-content-between small text-muted px-2">
                    <span>Valid for 5 minutes</span>
                    {cooldown > 0 ? (
                      <span>Resend in {cooldown}s</span>
                    ) : (
                      <button
                        type="button"
                        onClick={handleSendLoginOtp}
                        className="btn btn-link p-0 text-primary small text-decoration-none d-flex align-items-center gap-1"
                        disabled={loading}
                      >
                        <RefreshCw size={12} /> Resend
                      </button>
                    )}
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary w-100 btn-sm py-2 fw-medium d-flex align-items-center justify-content-center gap-2 mb-2"
                  disabled={loading || otpCode.length !== 6}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                      <span>Verifying...</span>
                    </>
                  ) : (
                    <span>Verify &amp; Enter Warehouse</span>
                  )}
                </button>

                <button
                  type="button"
                  className="btn btn-outline-secondary w-100 btn-sm py-1 d-flex align-items-center justify-content-center gap-1"
                  onClick={() => {
                    setOtpSent(false);
                    setOtpCode('');
                    setError(null);
                    setSuccessMsg(null);
                  }}
                  disabled={loading}
                >
                  <ArrowLeft size={13} />
                  <span>Use different email</span>
                </button>
              </form>
            )}
          </div>
        )}

        <div className="mt-4 pt-3 border-top text-center text-muted small" style={{ fontSize: '0.78rem' }}>
          <div>
            Don't have a warehouse account?{' '}
            <Link to="/signup" className="text-primary fw-medium text-decoration-none">
              Register Private Warehouse
            </Link>
          </div>
          <div className="mt-2 d-flex align-items-center justify-content-center gap-1 text-secondary" style={{ fontSize: '0.72rem' }}>
            <ShieldCheck size={13} className="text-success" />
            <span>Spring Security RBAC &bull; Multi-Tenant Scoped Session</span>
          </div>
        </div>
      </div>
    </div>
  );
};
