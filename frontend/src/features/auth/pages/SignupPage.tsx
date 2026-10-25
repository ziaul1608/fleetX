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
      if (apiErr.errorCode === 'EMAIL_ALREADY_EXISTS') {
        setError('An account with this email already exists. Please log in.');
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
        setError(apiErr.message || 'Incorrect verification code. Please try again.');
      } else if (apiErr.errorCode === 'OTP_EXPIRED') {
        setError('Verification code has expired. Please request a new code.');
      } else if (apiErr.errorCode === 'MAX_ATTEMPTS_EXCEEDED') {
        setError('Maximum verification attempts exceeded. Please request a new code.');
      } else {
        setError(apiErr.message || 'Verification failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex align-items-center justify-content-center min-vh-100 bg-light p-3">
      <div className="card shadow-sm border-0 rounded-3 p-4" style={{ maxWidth: '480px', width: '100%', background: '#ffffff' }}>
        {/* Brand Header */}
        <div className="text-center mb-4">
          <Link to="/" className="text-decoration-none">
            <div className="d-inline-flex bg-primary text-white p-3 rounded-circle mb-2 shadow-sm">
              <Radio size={28} />
            </div>
          </Link>
          <h2 className="h4 fw-bold text-slate-900 mb-1">Launch Warehouse Workspace</h2>
          <p className="text-muted small">
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
              <label className="form-label small fw-medium text-secondary" htmlFor="signup-company">
                Company / Organization Name <span className="text-danger">*</span>
              </label>
              <div className="input-group input-group-sm">
                <span className="input-group-text bg-light text-muted">
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
              <label className="form-label small fw-medium text-secondary" htmlFor="signup-warehouse">
                Warehouse / Central Depot Name <span className="text-danger">*</span>
              </label>
              <div className="input-group input-group-sm">
                <span className="input-group-text bg-light text-muted">
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
              <label className="form-label small fw-medium text-secondary" htmlFor="signup-name">
                Owner / Manager Full Name <span className="text-danger">*</span>
              </label>
              <div className="input-group input-group-sm">
                <span className="input-group-text bg-light text-muted">
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
              <label className="form-label small fw-medium text-secondary" htmlFor="signup-email">
                Work Email Address <span className="text-danger">*</span>
              </label>
              <div className="input-group input-group-sm">
                <span className="input-group-text bg-light text-muted">
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
              <small className="text-muted" style={{ fontSize: '0.72rem' }}>
                We will email a 6-digit verification code to this address.
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
                  <span>Sending Verification Code...</span>
                </>
              ) : (
                <>
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
              <label className="form-label small fw-medium text-secondary d-block mb-2" htmlFor="signup-otp">
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
              <div className="d-flex align-items-center justify-content-between small text-muted px-2">
                <span>Code expires in 5 minutes</span>
                {cooldown > 0 ? (
                  <span>Resend in {cooldown}s</span>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    className="btn btn-link p-0 text-primary small text-decoration-none d-flex align-items-center gap-1"
                    disabled={loading}
                  >
                    <RefreshCw size={12} /> Resend Code
                  </button>
                )}
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100 btn-sm py-2 fw-medium d-flex align-items-center justify-content-center gap-2 mb-3"
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
              className="btn btn-outline-secondary w-100 btn-sm py-1 d-flex align-items-center justify-content-center gap-1"
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
        <div className="mt-4 pt-3 border-top text-center text-muted small" style={{ fontSize: '0.78rem' }}>
          <div>
            Already have an account?{' '}
            <Link to="/login" className="text-primary fw-medium text-decoration-none">
              Sign In
            </Link>
          </div>
          <div className="mt-2 d-flex align-items-center justify-content-center gap-1 text-secondary" style={{ fontSize: '0.72rem' }}>
            <ShieldCheck size={13} className="text-success" />
            <span>Cryptographic SHA-256 OTP • Zero-Leakage Tenancy</span>
          </div>
        </div>
      </div>
    </div>
  );
};
