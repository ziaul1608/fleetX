import React, { useState, useEffect } from 'react';
import { AlertCircle, UserCheck } from 'lucide-react';
import { RoleType, UserDto, UserUpdateRequest } from '../types';

interface EditUserModalProps {
  isOpen: boolean;
  user: UserDto | null;
  onClose: () => void;
  onSubmit: (userId: number, data: UserUpdateRequest) => Promise<void>;
}

const ALL_ROLES: RoleType[] = ['ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER', 'VIEWER'];

export const EditUserModal: React.FC<EditUserModalProps> = ({ isOpen, user, onClose, onSubmit }) => {
  const [fullName, setFullName] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<RoleType[]>([]);
  const [active, setActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setSelectedRoles([...user.roles]);
      setActive(user.active);
      setError(null);
    }
  }, [user]);

  if (!isOpen || !user) return null;

  const handleRoleToggle = (role: RoleType) => {
    setSelectedRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setError('Full Name is required.');
      return;
    }
    if (selectedRoles.length === 0) {
      setError('Please assign at least one role to this account.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(user.userId, {
        fullName: fullName.trim(),
        roles: selectedRoles,
        active,
      });
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string; validationErrors?: Array<{ message: string }> };
      if (apiErr.validationErrors && apiErr.validationErrors.length > 0) {
        setError(apiErr.validationErrors.map((v) => v.message).join(', '));
      } else {
        setError(apiErr.message || 'Failed to update user account.');
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
              <UserCheck size={18} className="text-primary" />
              Edit User Account: {user.email}
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

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary">
                  Account Email
                </label>
                <input
                  type="text"
                  className="form-control form-control-sm bg-light text-muted"
                  value={user.email}
                  disabled
                />
                <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                  Email addresses are immutable unique system identifiers.
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary" htmlFor="edit-full-name">
                  Full Name <span className="text-danger">*</span>
                </label>
                <input
                  id="edit-full-name"
                  type="text"
                  className="form-control form-control-sm"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary d-block">
                  Assigned Roles <span className="text-danger">*</span>
                </label>
                <div className="d-flex flex-wrap gap-2 pt-1">
                  {ALL_ROLES.map((role) => (
                    <div key={role} className="form-check form-check-inline">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id={`edit-role-${role}`}
                        checked={selectedRoles.includes(role)}
                        onChange={() => handleRoleToggle(role)}
                      />
                      <label className="form-check-label small" htmlFor={`edit-role-${role}`}>
                        {role}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              <div className="mb-3 pt-2 border-top">
                <div className="form-check form-switch">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="edit-user-active"
                    checked={active}
                    onChange={(e) => setActive(e.target.checked)}
                  />
                  <label className="form-check-label small fw-medium" htmlFor="edit-user-active">
                    Account Active ({active ? 'Enabled' : 'Deactivated'})
                  </label>
                </div>
                <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                  Deactivated accounts are immediately blocked from logging in.
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
                type="submit"
                className="btn btn-sm btn-primary"
                disabled={submitting}
              >
                {submitting ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
