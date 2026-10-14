import React, { useState } from 'react';
import { AlertCircle, UserPlus } from 'lucide-react';
import { RoleType, UserCreateRequest } from '../types';

interface CreateUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: UserCreateRequest) => Promise<void>;
}

const ALL_ROLES: RoleType[] = ['ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER', 'VIEWER'];

export const CreateUserModal: React.FC<CreateUserModalProps> = ({ isOpen, onClose, onSubmit }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<RoleType[]>(['VIEWER']);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleRoleToggle = (role: RoleType) => {
    setSelectedRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || !email.trim() || !password) {
      setError('Please fill in all mandatory fields.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }
    if (selectedRoles.length === 0) {
      setError('Please select at least one role for the user.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        roles: selectedRoles,
      });
      // Reset form on success
      setFullName('');
      setEmail('');
      setPassword('');
      setSelectedRoles(['VIEWER']);
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string; validationErrors?: Array<{ message: string }> };
      if (apiErr.validationErrors && apiErr.validationErrors.length > 0) {
        setError(apiErr.validationErrors.map((v) => v.message).join(', '));
      } else {
        setError(apiErr.message || 'Failed to create user account.');
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
              <UserPlus size={18} className="text-primary" />
              Provision New User Account
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
                <label className="form-label small fw-medium text-secondary" htmlFor="create-full-name">
                  Full Name <span className="text-danger">*</span>
                </label>
                <input
                  id="create-full-name"
                  type="text"
                  className="form-control form-control-sm"
                  placeholder="e.g. Jane Doe"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary" htmlFor="create-email">
                  Email Address <span className="text-danger">*</span>
                </label>
                <input
                  id="create-email"
                  type="email"
                  className="form-control form-control-sm"
                  placeholder="e.g. jane.doe@fleetx.local"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-secondary" htmlFor="create-password">
                  Initial Password <span className="text-danger">*</span>
                </label>
                <input
                  id="create-password"
                  type="password"
                  className="form-control form-control-sm"
                  placeholder="Min 8 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
                />
                <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                  Must be at least 8 characters. The user can change this later.
                </div>
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
                        id={`role-check-${role}`}
                        checked={selectedRoles.includes(role)}
                        onChange={() => handleRoleToggle(role)}
                      />
                      <label className="form-check-label small" htmlFor={`role-check-${role}`}>
                        {role}
                      </label>
                    </div>
                  ))}
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
                {submitting ? 'Creating...' : 'Create Account'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
