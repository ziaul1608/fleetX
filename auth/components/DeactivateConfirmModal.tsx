import React, { useState } from 'react';
import { AlertTriangle, CheckCircle, ShieldAlert } from 'lucide-react';
import { UserDto, UserStatusUpdateRequest } from '../types';

interface DeactivateConfirmModalProps {
  isOpen: boolean;
  user: UserDto | null;
  onClose: () => void;
  onConfirm: (userId: number, data: UserStatusUpdateRequest) => Promise<void>;
}

export const DeactivateConfirmModal: React.FC<DeactivateConfirmModalProps> = ({
  isOpen,
  user,
  onClose,
  onConfirm,
}) => {
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !user) return null;

  const willDeactivate = user.active; // If currently active, this action will deactivate it

  const handleConfirm = async () => {
    setSubmitting(true);
    setError(null);

    try {
      await onConfirm(user.userId, {
        active: !willDeactivate,
        reason: reason.trim() || undefined,
      });
      setReason('');
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || `Failed to ${willDeactivate ? 'deactivate' : 'activate'} account.`);
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
              {willDeactivate ? (
                <>
                  <AlertTriangle size={18} className="text-warning" />
                  Confirm Account Deactivation
                </>
              ) : (
                <>
                  <CheckCircle size={18} className="text-success" />
                  Reactivate User Account
                </>
              )}
            </h5>
            <button
              type="button"
              className="btn-close"
              aria-label="Close"
              onClick={onClose}
              disabled={submitting}
            />
          </div>

          <div className="modal-body p-4">
            {error && (
              <div className="alert alert-danger small py-2 mb-3" role="alert">
                {error}
              </div>
            )}

            <p className="small mb-3">
              {willDeactivate ? (
                <>
                  Are you sure you want to <strong>deactivate</strong> the account for{' '}
                  <span className="fw-semibold text-slate-900">{user.fullName}</span> (
                  <code>{user.email}</code>)?
                </>
              ) : (
                <>
                  Are you sure you want to <strong>reactivate</strong> the account for{' '}
                  <span className="fw-semibold text-slate-900">{user.fullName}</span> (
                  <code>{user.email}</code>)?
                </>
              )}
            </p>

            {willDeactivate && user.roles.includes('ADMIN') && (
              <div className="alert alert-warning small d-flex align-items-start gap-2 py-2 mb-3">
                <ShieldAlert size={16} className="flex-shrink-0 mt-1" />
                <div>
                  <strong>Admin Account Notice:</strong> You cannot deactivate your own session or the sole remaining active administrator.
                </div>
              </div>
            )}

            <div className="mb-2">
              <label className="form-label small fw-medium text-secondary" htmlFor="status-reason">
                Audit Reason (Optional)
              </label>
              <input
                id="status-reason"
                type="text"
                className="form-control form-control-sm"
                placeholder={
                  willDeactivate
                    ? 'e.g. Employee resignation or security policy'
                    : 'e.g. Reinstated duties'
                }
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                This note will be recorded in the security audit trail.
              </div>
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
              type="button"
              className={`btn btn-sm ${willDeactivate ? 'btn-danger' : 'btn-success'}`}
              onClick={handleConfirm}
              disabled={submitting}
            >
              {submitting
                ? 'Processing...'
                : willDeactivate
                ? 'Deactivate Account'
                : 'Reactivate Account'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
