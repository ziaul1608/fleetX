import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import {
  Radio,
  Mail,
  AlertCircle,
  ShieldCheck,
  CheckCircle2,
  RefreshCw,
  ArrowRight,
  ArrowLeft,
  KeyRound,
} from 'lucide-react';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../shared/context/AuthContext';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginWithOtp, isAuthenticated } = useAuth();

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

  // Handle Send Login OTP (Email enumeration defense: uniform feedback)
  const handleSendLoginOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpEmail.trim()) {
      setError('Please enter your registered work email address.');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await authApi.sendLoginOtp({ email: otpEmail.trim().toLowerCase() });
      setSuccessMsg(res.message || `If an account is associated with this email, a verification code has been dispatched.`);
      setCooldown(res.cooldownSeconds || 60);
      setOtpSent(true);
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'OTP_COOLDOWN') {
        setError(apiErr.message || 'Please wait before requesting a new code.');
      } else if (apiErr.errorCode === 'IP_RATE_LIMIT_EXCEEDED' || apiErr.errorCode === 'EMAIL_RATE_LIMIT_EXCEEDED') {
        setError(apiErr.message || 'Rate limit exceeded. Please wait a few minutes before trying again.');
      } else {
        setError(apiErr.message || 'Failed to dispatch login code. Please verify email.');
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
      } else if (apiErr.errorCode === 'ACCOUNT_DEACTIVATED') {
        setError('Your account has been deactivated. Please contact your system administrator.');
      } else if (apiErr.errorCode === 'TENANT_INACTIVE') {
        setError('Organization account is suspended. Please contact support.');
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

        {/* Security Notice: Passwordless Email OTP Authentication */}
        <div className="p-2 mb-3 rounded-2 bg-light border d-flex align-items-center gap-2 small text-secondary">
          <KeyRound size={16} className="text-primary flex-shrink-0" />
          <span>Passwordless multi-tenant login via secure Email OTP</span>
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

        {/* PASSWORDLESS EMAIL OTP AUTHENTICATION */}
        <div>
          {!otpSent ? (
            <form onSubmit={handleSendLoginOtp}>
              <div className="mb-4">
                <label className="form-label small fw-medium text-secondary" htmlFor="login-email">
                  Registered Work Email <span className="text-danger">*</span>
                </label>
                <div className="input-group input-group-sm">
                  <span className="input-group-text bg-light text-muted">
                    <Mail size={14} />
                  </span>
                  <input
                    id="login-email"
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
                  We will send a 6-digit login verification code to your inbox.
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
