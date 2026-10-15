import React, { useState } from 'react';
import { UserPlus, AlertCircle } from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { DriverCreateRequest } from '../types';

interface CreateDriverModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateDriverModal: React.FC<CreateDriverModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState<DriverCreateRequest>({
    fullName: '',
    phoneNumber: '',
    email: '',
    licenseNumber: '',
    licenseCategory: 'HEAVY',
    licenseExpiryDate: '',
    medicalCertExpiryDate: '',
    experienceYears: 3,
    availabilityStatus: 'AVAILABLE',
    operationalNotes: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await driverApi.createDriver({
        ...formData,
        experienceYears: Number(formData.experienceYears) || 0,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to onboard driver. Please check inputs.');
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
              <UserPlus size={20} className="text-primary" /> Onboard Commercial Driver
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
                    placeholder="e.g. Ramesh Kumar"
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
                    placeholder="e.g. +91 98765 43210"
                    value={formData.phoneNumber}
                    onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Email Address</label>
                  <input
                    type="email"
                    className="form-control form-control-sm"
                    placeholder="e.g. ramesh.kumar@fleetx.local"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">License Number *</label>
                  <input
                    type="text"
                    required
                    className="form-control form-control-sm font-monospace text-uppercase"
                    placeholder="e.g. DL-1420240098765"
                    value={formData.licenseNumber}
                    onChange={(e) => setFormData({ ...formData, licenseNumber: e.target.value.toUpperCase() })}
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small fw-semibold text-slate-700">License Category *</label>
                  <select
                    className="form-select form-select-sm"
                    value={formData.licenseCategory}
                    onChange={(e) => setFormData({ ...formData, licenseCategory: e.target.value })}
                  >
                    <option value="HEAVY">HEAVY (Heavy Commercial Trucks)</option>
                    <option value="MEDIUM">MEDIUM (Light/Medium Commercial)</option>
                    <option value="LIGHT">LIGHT (Vans & Small Couriers)</option>
                  </select>
                </div>

                <div className="col-md-4">
                  <label className="form-label small fw-semibold text-slate-700">License Expiry Date *</label>
                  <input
                    type="date"
                    required
                    aria-label="License Expiry Date"
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

                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Initial Availability Status</label>
                  <select
                    className="form-select form-select-sm"
                    value={formData.availabilityStatus}
                    onChange={(e) => setFormData({ ...formData, availabilityStatus: e.target.value })}
                  >
                    <option value="AVAILABLE">AVAILABLE (Eligible for immediate dispatch)</option>
                    <option value="ON_DUTY">ON_DUTY</option>
                    <option value="ON_LEAVE">ON_LEAVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                  </select>
                </div>

                <div className="col-12">
                  <label className="form-label small fw-semibold text-slate-700">Operational Notes / Special Certifications</label>
                  <textarea
                    rows={2}
                    className="form-control form-control-sm"
                    placeholder="e.g. Hazardous materials certified, long-haul interstate routes experience"
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
              <button type="submit" className="btn btn-sm btn-primary d-flex align-items-center gap-2" disabled={loading}>
                {loading ? 'Onboarding...' : 'Register Driver'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
