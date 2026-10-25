import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Radio,
  Building2,
  Warehouse,
  User,
  Mail,
  ArrowRight,
  AlertCircle,
  CheckCircle2,
  ShieldCheck,
  RefreshCw,
  ArrowLeft,
  Sparkles,
} from 'lucide-react';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../shared/context/AuthContext';

export const SignupPage: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated, signupWithOtp } = useAuth();

  // Step 1: DETAILS, Step 2: VERIFY_OTP
  const [step, setStep] = useState<'DETAILS' | 'VERIFY_OTP'>('DETAILS');

  // Form Fields
  const [companyName, setCompanyName] = useState('');
  const [warehouseName, setWarehouseName] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // Bootstrap CSRF token on mount
  useEffect(() => {
    authApi.fetchCsrf().catch(() => {});
  }, []);

  // Cooldown timer ticker
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!companyName.trim() || !warehouseName.trim() || !fullName.trim() || !email.trim()) {
      setError('Please fill out all required fields.');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await authApi.sendSignupOtp({
        companyName: companyName.trim(),
        warehouseName: warehouseName.trim(),
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
      });

      setSuccessMsg(res.message || `Verification code sent to ${email.trim()}`);
      setCooldown(res.cooldownSeconds || 60);
      setStep('VERIFY_OTP');
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'EMAIL_ALREADY_EXISTS' || apiErr.errorCode === 'RESOURCE_CONFLICT' || apiErr.errorCode === 'DATA_CONFLICT') {
        setError('An account with this email address already exists. Please sign in.');
      } else if (apiErr.errorCode === 'OTP_COOLDOWN') {
        setError(apiErr.message || 'Please wait before requesting another verification code.');
      } else if (apiErr.errorCode === 'EMAIL_SERVICE_UNAVAILABLE') {
        setError('Email delivery service is temporarily unavailable. Please try again shortly.');
      } else if (apiErr.errorCode === 'NETWORK_ERROR') {
        setError('Unable to reach the server. Please check your network connection.');
      } else {
        setError(apiErr.message || 'Failed to dispatch verification code. Please check your email.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (cooldown > 0 || loading) return;
    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await authApi.sendSignupOtp({
        companyName: companyName.trim(),
        warehouseName: warehouseName.trim(),
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
      });
      setSuccessMsg(res.message || `New code sent to ${email.trim()}`);
      setCooldown(res.cooldownSeconds || 60);
      setOtp('');
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to resend code. Please wait for cooldown.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanOtp = otp.trim();
    if (cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
      setError('Please enter the 6-digit verification code.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (signupWithOtp) {
        await signupWithOtp({
          companyName: companyName.trim(),
          warehouseName: warehouseName.trim(),
          fullName: fullName.trim(),
          email: email.trim().toLowerCase(),
          otp: cleanOtp,
        });
      } else {
        await authApi.verifySignupOtp({
          companyName: companyName.trim(),
          warehouseName: warehouseName.trim(),
          fullName: fullName.trim(),
          email: email.trim().toLowerCase(),
          otp: cleanOtp,
        });
      }

      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.errorCode === 'INCORRECT_OTP') {
        setError(apiErr.message || 'Incorrect verification code. Attempts remaining: 3');
      } else if (apiErr.errorCode === 'OTP_EXPIRED') {
        setError('Verification code has expired. Please request a new code below.');
      } else if (apiErr.errorCode === 'MAX_ATTEMPTS_EXCEEDED') {
        setError('Maximum verification attempts exceeded. Please request a new code.');
      } else if (apiErr.errorCode === 'NO_ACTIVE_OTP') {
        setError('No active verification code found for this email. Please request a new code.');
      } else if (apiErr.errorCode === 'OTP_ALREADY_USED') {
        setError('This verification code has already been consumed. Please request a fresh code.');
      } else if (apiErr.errorCode === 'EMAIL_ALREADY_EXISTS' || apiErr.errorCode === 'RESOURCE_CONFLICT' || apiErr.errorCode === 'DATA_CONFLICT') {
        setError('An account with this email address already exists. Please sign in instead.');
      } else if (apiErr.errorCode === 'EMAIL_SERVICE_UNAVAILABLE') {
        setError('Email service is temporarily unavailable. Please try again shortly.');
      } else if (apiErr.errorCode === 'TENANT_INACTIVE') {
        setError('Organization account is suspended. Please contact system support.');
      } else if (apiErr.errorCode === 'NETWORK_ERROR') {
        setError('Unable to reach the server. Please check your network connection and try again.');
      } else {
        setError(apiErr.message || 'Verification failed. Please check your code or request a new code.');
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
          maxWidth: '520px',
          width: '100%',
          borderColor: 'rgba(232, 211, 162, 0.28)',
          zIndex: 1,
        }}
      >
        {/* Brand Header */}
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
            Launch Warehouse Workspace
          </h2>
          <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
            {step === 'DETAILS'
              ? 'Multi-Tenant Logistics SaaS • Dedicated Private Partition'
              : `Enter 6-digit code sent to ${email}`}
          </p>
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

        {step === 'DETAILS' ? (
          /* STEP 1: Registration Form */
          <form onSubmit={handleSendOtp}>
            <div className="mb-3">
              <label className="form-label small fw-medium d-block mb-1" htmlFor="signup-company" style={{ color: 'var(--fx-ivory-text)' }}>
                Company / Organization Name <span className="text-danger">*</span>
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
                  <Building2 size={14} />
                </span>
                <input
                  id="signup-company"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Apex Global Logistics"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label small fw-medium d-block mb-1" htmlFor="signup-warehouse" style={{ color: 'var(--fx-ivory-text)' }}>
                Warehouse / Central Depot Name <span className="text-danger">*</span>
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
                  <Warehouse size={14} />
                </span>
                <input
                  id="signup-warehouse"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Mumbai Port Gateway Hub"
                  value={warehouseName}
                  onChange={(e) => setWarehouseName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label small fw-medium d-block mb-1" htmlFor="signup-name" style={{ color: 'var(--fx-ivory-text)' }}>
                Owner / Manager Full Name <span className="text-danger">*</span>
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
                  <User size={14} />
                </span>
                <input
                  id="signup-name"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Alice Alpha"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="mb-4">
              <label className="form-label small fw-medium d-block mb-1" htmlFor="signup-email" style={{ color: 'var(--fx-ivory-text)' }}>
                Work Email Address <span className="text-danger">*</span>
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
                  id="signup-email"
                  type="email"
                  className="form-control"
                  placeholder="owner@company.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>
              <small className="d-block mt-1" style={{ fontSize: '0.72rem', color: 'var(--fx-muted-sand)' }}>
                We will email a 6-digit verification code to this address.
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
                  <span>Sending Verification Code...</span>
                </>
              ) : (
                <>
                  <Sparkles size={15} />
                  <span>Send Verification Code</span>
                  <ArrowRight size={14} />
                </>
              )}
            </button>
          </form>
        ) : (
          /* STEP 2: OTP Verification Form */
          <form onSubmit={handleVerifyOtp}>
            <div className="mb-4 text-center">
              <label className="form-label small fw-medium d-block mb-2" htmlFor="signup-otp" style={{ color: 'var(--fx-ivory-text)' }}>
                Enter 6-Digit Verification Code
              </label>
              <div className="d-flex justify-content-center mb-2">
                <input
                  id="signup-otp"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  className="form-control text-center font-monospace fs-4 fw-bold letter-spacing-lg"
                  style={{ maxWidth: '240px', letterSpacing: '0.35em' }}
                  placeholder="123456"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                  autoFocus
                  required
                />
              </div>
              <div className="d-flex align-items-center justify-content-between small px-2" style={{ color: 'var(--fx-muted-sand)' }}>
                <span>Code expires in 5 minutes</span>
                {cooldown > 0 ? (
                  <span>Resend in {cooldown}s</span>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    className="btn btn-link p-0 small text-decoration-none d-flex align-items-center gap-1"
                    style={{ color: 'var(--fx-champagne-gold)' }}
                    disabled={loading}
                  >
                    <RefreshCw size={12} /> Resend Code
                  </button>
                )}
              </div>
            </div>

            <button
              type="submit"
              className="btn fx-btn-gold w-100 btn-sm py-2.5 fw-bold d-flex align-items-center justify-content-center gap-2 mb-3 shadow"
              disabled={loading || otp.length !== 6}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                  <span>Verifying &amp; Provisioning...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 size={16} />
                  <span>Verify &amp; Launch Warehouse</span>
                </>
              )}
            </button>

            <button
              type="button"
              className="btn fx-btn-glass w-100 btn-sm py-1.5 d-flex align-items-center justify-content-center gap-1"
              onClick={() => {
                setStep('DETAILS');
                setOtp('');
                setError(null);
                setSuccessMsg(null);
              }}
              disabled={loading}
            >
              <ArrowLeft size={13} />
              <span>Change Email or Details</span>
            </button>
          </form>
        )}

        {/* Footer links */}
        <div
          className="mt-4 pt-3 border-top text-center small"
          style={{ borderColor: 'rgba(232, 211, 162, 0.16)', fontSize: '0.8rem', color: 'var(--fx-muted-sand)' }}
        >
          <div>
            Already have an account?{' '}
            <Link to="/login" className="fw-semibold text-decoration-none" style={{ color: 'var(--fx-champagne-gold)' }}>
              Sign In
            </Link>
          </div>
          <div className="mt-2 d-flex align-items-center justify-content-center gap-1" style={{ fontSize: '0.72rem' }}>
            <ShieldCheck size={13} style={{ color: '#8CE3A7' }} />
            <span>Cryptographic SHA-256 OTP • Zero-Leakage Tenancy</span>
          </div>
        </div>
      </div>
    </div>
  );
};
