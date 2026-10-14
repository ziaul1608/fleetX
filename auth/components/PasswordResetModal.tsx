import React, { useState } from 'react';
import { AlertCircle, KeyRound } from 'lucide-react';
import { AdminPasswordResetRequest, UserDto } from '../types';

interface PasswordResetModalProps {
  isOpen: boolean;
  user: UserDto | null;
  onClose: () => void;
  onSubmit: (userId: number, data: AdminPasswordResetRequest) => Promise<void>;
}

export const PasswordResetModal: React.FC<PasswordResetModalProps> = ({
  isOpen,
  user,
  onClose,
  onSubmit,
}) => {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !user) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters in length.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(user.userId, { newPassword });
      setNewPassword('');
      setConfirmPassword('');
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string; validationErrors?: Array<{ message: string }> };
      if (apiErr.validationErrors && apiErr.validationErrors.length > 0) {
        setError(apiErr.validationErrors.map((v) => v.message).join(', '));
      } else {
        setError(apiErr.message || 'Failed to reset password.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal fade show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.45)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content shadow border-0">
          <div className="modal-header border-bottom py-3">
            <h5 className="modal-title fs-6 fw-bold text-slate-900 d-flex align-items-center gap-2">
              <KeyRound size={18} className="text-primary" />
              Reset Password for {user.email}
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={submitting}
            />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              {error && (
                <div className="alert alert-danger d-flex align-items-center gap-2 small py-2 mb-3" role="alert">
                  <AlertCircle size={16} className="flex-shrink-0" />
                  <div>{error}</div>
                </div>
              )}

              <p className="small text-muted mb-3">
                Enter a temporary or replacement password for{' '}
                <span className="fw-semibold text-slate-900">{user.fullName}</span>.
              </p>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary" htmlFor="reset-new-password">
                  New Password <span className="text-danger">*</span>
                </label>
                <input
                  id="reset-new-password"
                  type="password"
                  className="form-control form-control-sm"
                  placeholder="Min 8 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary" htmlFor="reset-confirm-password">
                  Confirm Password <span className="text-danger">*</span>
                </label>
                <input
                  id="reset-confirm-password"
                  type="password"
                  className="form-control form-control-sm"
                  placeholder="Re-enter password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
            </div>

            <div className="modal-footer border-top py-2 bg-light">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={onClose}
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-sm btn-primary"
                disabled={submitting}
              >
                {submitting ? 'Resetting...' : 'Update Password'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
