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
  Sparkles,
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
    <div
      className="d-flex align-items-center justify-content-center min-vh-100 p-3 position-relative overflow-hidden"
      style={{ background: 'var(--fx-espresso-bg)', color: 'var(--fx-ivory-text)' }}
    >
      {/* Ambient background glows */}
      <div className="fx-glow-orb fx-glow-top-left" aria-hidden="true" />
      <div className="fx-glow-orb fx-glow-bottom-right" aria-hidden="true" />

      <div
        className="fx-glass-panel p-4 p-sm-5 rounded-4 border shadow-lg position-relative"
        style={{
          maxWidth: '460px',
          width: '100%',
          borderColor: 'rgba(232, 211, 162, 0.28)',
          zIndex: 1,
        }}
      >
        <div className="text-center mb-4">
          <Link to="/" className="text-decoration-none">
            <div
              className="d-inline-flex p-3 rounded-circle mb-3 shadow"
              style={{
                background: 'linear-gradient(135deg, #E8D3A2 0%, #C8753D 100%)',
                color: '#201713',
              }}
            >
              <Radio size={28} strokeWidth={2.5} />
            </div>
          </Link>
          <h2 className="h4 fw-extrabold mb-1" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
            FleetX Control Tower
          </h2>
          <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
            Intelligent Fleet &amp; Logistics Management System
          </p>
        </div>

        {/* Security Notice: Passwordless Email OTP Authentication */}
        <div
          className="p-2.5 mb-3 rounded-3 border d-flex align-items-center gap-2 small"
          style={{
            background: 'rgba(70, 49, 40, 0.45)',
            borderColor: 'rgba(232, 211, 162, 0.2)',
            color: 'var(--fx-muted-sand)',
          }}
        >
          <KeyRound size={16} style={{ color: 'var(--fx-champagne-gold)' }} className="flex-shrink-0" />
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
                <label className="form-label small fw-medium d-block mb-1" htmlFor="login-email" style={{ color: 'var(--fx-ivory-text)' }}>
                  Registered Work Email <span className="text-danger">*</span>
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
                <small className="d-block mt-1" style={{ fontSize: '0.72rem', color: 'var(--fx-muted-sand)' }}>
                  We will send a 6-digit login verification code to your inbox.
                </small>
              </div>

              <button
                type="submit"
                className="btn fx-btn-gold w-100 btn-sm py-2.5 fw-bold d-flex align-items-center justify-content-center gap-2 shadow"
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
                <label className="form-label small fw-medium d-block mb-2" htmlFor="otp-login-code" style={{ color: 'var(--fx-ivory-text)' }}>
                  Enter 6-Digit Code sent to <span style={{ color: 'var(--fx-champagne-gold)' }}>{otpEmail}</span>
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
                <div className="d-flex align-items-center justify-content-between small px-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <span>Valid for 5 minutes</span>
                  {cooldown > 0 ? (
                    <span>Resend in {cooldown}s</span>
                  ) : (
                    <button
                      type="button"
                      onClick={handleSendLoginOtp}
                      className="btn btn-link p-0 small text-decoration-none d-flex align-items-center gap-1"
                      style={{ color: 'var(--fx-champagne-gold)' }}
                      disabled={loading}
                    >
                      <RefreshCw size={12} /> Resend
                    </button>
                  )}
                </div>
              </div>

              <button
                type="submit"
                className="btn fx-btn-gold w-100 btn-sm py-2.5 fw-bold d-flex align-items-center justify-content-center gap-2 mb-2 shadow"
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
                className="btn fx-btn-glass w-100 btn-sm py-1.5 d-flex align-items-center justify-content-center gap-1"
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

        <div
          className="mt-4 pt-3 border-top text-center small"
          style={{ borderColor: 'rgba(232, 211, 162, 0.16)', fontSize: '0.8rem', color: 'var(--fx-muted-sand)' }}
        >
          <div>
            Don't have a warehouse account?{' '}
            <Link to="/signup" className="fw-semibold text-decoration-none" style={{ color: 'var(--fx-champagne-gold)' }}>
              Register Private Warehouse
            </Link>
          </div>
          <div className="mt-2 d-flex align-items-center justify-content-center gap-1" style={{ fontSize: '0.72rem' }}>
            <ShieldCheck size={13} style={{ color: '#8CE3A7' }} />
            <span>Spring Security RBAC &bull; Multi-Tenant Scoped Session</span>
          </div>
        </div>
      </div>
    </div>
  );
};
