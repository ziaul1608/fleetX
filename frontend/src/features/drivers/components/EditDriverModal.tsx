import React, { useState } from 'react';
import { Edit3, AlertCircle } from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Driver, DriverUpdateRequest } from '../types';

interface EditDriverModalProps {
  driver: Driver;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const EditDriverModal: React.FC<EditDriverModalProps> = ({ driver, isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState<DriverUpdateRequest>({
    fullName: driver.fullName,
    phoneNumber: driver.phoneNumber,
    email: driver.email || '',
    licenseCategory: driver.licenseCategory,
    licenseExpiryDate: driver.licenseExpiryDate,
    medicalCertExpiryDate: driver.medicalCertExpiryDate || '',
    experienceYears: driver.experienceYears,
    availabilityStatus: driver.availabilityStatus,
    active: driver.active,
    operationalNotes: driver.operationalNotes || '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await driverApi.updateDriver(driver.id, {
        ...formData,
        experienceYears: Number(formData.experienceYears) || 0,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update driver profile.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header border-bottom py-3">
            <h5 className="modal-title fw-bold d-flex align-items-center gap-2 text-slate-900">
              <Edit3 size={18} className="text-primary" /> Edit Driver Profile: {driver.fullName}
            </h5>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={loading} />
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body p-4">
              {error && (
                <div className="alert alert-danger d-flex align-items-center gap-2 py-2 px-3 small mb-3">
                  <AlertCircle size={16} /> {error}
                </div>
              )}

              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Full Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control form-control-sm"
                    value={formData.fullName}
                    onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Contact Phone Number *</label>
                  <input
                    type="tel"
                    required
                    className="form-control form-control-sm"
                    value={formData.phoneNumber}
                    onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Email Address</label>
                  <input
                    type="email"
                    className="form-control form-control-sm"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">License Number (Read-only)</label>
                  <input
                    type="text"
                    disabled
                    className="form-control form-control-sm bg-light font-monospace"
                    value={driver.licenseNumber}
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small fw-semibold text-slate-700">License Category</label>
                  <select
                    className="form-select form-select-sm"
                    value={formData.licenseCategory}
                    onChange={(e) => setFormData({ ...formData, licenseCategory: e.target.value })}
                  >
                    <option value="HEAVY">HEAVY</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LIGHT">LIGHT</option>
                  </select>
                </div>

                <div className="col-md-4">
                  <label className="form-label small fw-semibold text-slate-700">License Expiry Date *</label>
                  <input
                    type="date"
                    required
                    className="form-control form-control-sm"
                    value={formData.licenseExpiryDate}
                    onChange={(e) => setFormData({ ...formData, licenseExpiryDate: e.target.value })}
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small fw-semibold text-slate-700">Medical Fitness Expiry</label>
                  <input
                    type="date"
                    className="form-control form-control-sm"
                    value={formData.medicalCertExpiryDate}
                    onChange={(e) => setFormData({ ...formData, medicalCertExpiryDate: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Duty Availability Status</label>
                  <select
                    className="form-select form-select-sm"
                    value={formData.availabilityStatus}
                    onChange={(e) => setFormData({ ...formData, availabilityStatus: e.target.value })}
                  >
                    <option value="AVAILABLE">AVAILABLE</option>
                    <option value="ON_DUTY">ON_DUTY</option>
                    <option value="ON_LEAVE">ON_LEAVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Experience (Years)</label>
                  <input
                    type="number"
                    min={0}
                    max={50}
                    className="form-control form-control-sm"
                    value={formData.experienceYears}
                    onChange={(e) => setFormData({ ...formData, experienceYears: Number(e.target.value) })}
                  />
                </div>

                <div className="col-12">
                  <div className="form-check form-switch mt-2">
                    <input
                      type="checkbox"
                      className="form-check-input"
                      id="driverActiveSwitch"
                      checked={formData.active}
                      onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    />
                    <label className="form-check-label small fw-semibold text-slate-800" htmlFor="driverActiveSwitch">
                      Active Employment Record (Toggle off to deactivate without deletion)
                    </label>
                  </div>
                </div>

                <div className="col-12">
                  <label className="form-label small fw-semibold text-slate-700">Operational Notes</label>
                  <textarea
                    rows={2}
                    className="form-control form-control-sm"
                    value={formData.operationalNotes}
                    onChange={(e) => setFormData({ ...formData, operationalNotes: e.target.value })}
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer border-top py-2 px-4 bg-light">
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClose} disabled={loading}>
                Cancel
              </button>
              <button type="submit" className="btn btn-sm btn-primary" disabled={loading}>
                {loading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
