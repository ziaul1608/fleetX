import React, { useEffect, useState } from 'react';
import { User, ShieldAlert, AlertTriangle, Phone, Mail, Award as Star } from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Driver, DriverPerformance } from '../types';
import { Badge } from '../../../shared/components/Badge';

interface DriverDetailsModalProps {
  driver: Driver;
  isOpen: boolean;
  onClose: () => void;
}

export const DriverDetailsModal: React.FC<DriverDetailsModalProps> = ({ driver, isOpen, onClose }) => {
  const [performance, setPerformance] = useState<DriverPerformance | null>(null);
  const [loadingPerf, setLoadingPerf] = useState(false);

  useEffect(() => {
    if (isOpen && driver) {
      setLoadingPerf(true);
      driverApi
        .getDriverPerformance(driver.id)
        .then((data) => setPerformance(data))
        .catch(() => setPerformance(null))
        .finally(() => setLoadingPerf(false));
    }
  }, [isOpen, driver]);

  if (!isOpen) return null;

  const isExpired = new Date(driver.licenseExpiryDate) < new Date();
  const isExpiringSoon =
    !isExpired &&
    new Date(driver.licenseExpiryDate).getTime() - new Date().getTime() < 30 * 24 * 60 * 60 * 1000;

  return (
    <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header border-bottom py-3">
            <div className="d-flex align-items-center gap-2">
              <div className="p-2 bg-primary-subtle text-primary rounded">
                <User size={20} />
              </div>
              <div>
                <h5 className="modal-title fw-bold text-slate-900 mb-0">{driver.fullName}</h5>
                <small className="text-muted">License: <code className="fw-semibold">{driver.licenseNumber}</code></small>
              </div>
            </div>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
          </div>

          <div className="modal-body p-4">
            {/* Status alerts */}
            {isExpired && (
              <div className="alert alert-danger d-flex align-items-center gap-2 py-2 px-3 small mb-3">
                <ShieldAlert size={16} />
                <strong>License Expired!</strong> This commercial license expired on {driver.licenseExpiryDate}. Asset allocation is strictly blocked.
              </div>
            )}
            {isExpiringSoon && (
              <div className="alert alert-warning d-flex align-items-center gap-2 py-2 px-3 small mb-3">
                <AlertTriangle size={16} />
                <strong>License Expiring Soon!</strong> Valid until {driver.licenseExpiryDate}. Dispatch clearance warning active.
              </div>
            )}

            {/* Top Metrics Strip */}
            <div className="row g-3 mb-4">
              <div className="col-4">
                <div className="p-3 bg-light rounded border text-center">
                  <div className="text-muted small fw-medium mb-1">Safety Rating</div>
                  <div className="h4 fw-bold text-primary mb-0 d-flex align-items-center justify-content-center gap-1">
                    <Star size={18} className="text-warning fill-warning" />
                    {performance ? performance.ratingScore : driver.safetyScore}/100
                  </div>
                </div>
              </div>
              <div className="col-4">
                <div className="p-3 bg-light rounded border text-center">
                  <div className="text-muted small fw-medium mb-1">Trips Completed</div>
                  <div className="h4 fw-bold text-slate-900 mb-0">
                    {performance ? performance.totalTripsCompleted : driver.completedTripsCount}
                  </div>
                </div>
              </div>
              <div className="col-4">
                <div className="p-3 bg-light rounded border text-center">
                  <div className="text-muted small fw-medium mb-1">Duty Status</div>
                  <div className="mt-1">
                    <Badge status={driver.availabilityStatus} />
                  </div>
                </div>
              </div>
            </div>

            {/* Profile & Credentials Card */}
            <div className="card border mb-3">
              <div className="card-header bg-white py-2 px-3 fw-bold small text-slate-800">
                Driver Credentials &amp; Contact Details
              </div>
              <div className="card-body p-3">
                <div className="row g-2 small">
                  <div className="col-md-6">
                    <span className="text-muted">Contact Phone: </span>
                    <span className="fw-medium d-inline-flex align-items-center gap-1">
                      <Phone size={13} className="text-muted" /> {driver.phoneNumber}
                    </span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">Email Address: </span>
                    <span className="fw-medium d-inline-flex align-items-center gap-1">
                      <Mail size={13} className="text-muted" /> {driver.email || 'N/A'}
                    </span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">License Category: </span>
                    <span className="badge bg-secondary-subtle text-secondary border">{driver.licenseCategory}</span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">Experience: </span>
                    <span className="fw-medium">{driver.experienceYears} Years</span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">License Expiry: </span>
                    <span className={`fw-medium ${isExpired ? 'text-danger' : isExpiringSoon ? 'text-warning' : 'text-success'}`}>
                      {driver.licenseExpiryDate}
                    </span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">Medical Cert Expiry: </span>
                    <span className="fw-medium">{driver.medicalCertExpiryDate || 'Not specified'}</span>
                  </div>
                  <div className="col-md-6">
                    <span className="text-muted">Employment State: </span>
                    <span className={`badge ${driver.active ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'}`}>
                      {driver.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                </div>
                {driver.operationalNotes && (
                  <div className="mt-3 pt-2 border-top small text-muted">
                    <strong>Operational Notes:</strong> {driver.operationalNotes}
                  </div>
                )}
              </div>
            </div>

            {/* Detailed Performance Card */}
            <div className="card border">
              <div className="card-header bg-white py-2 px-3 fw-bold small text-slate-800 d-flex justify-content-between align-items-center">
                <span>Trip Dispatch Performance &amp; Safety Compliance</span>
                {loadingPerf && <span className="spinner-border spinner-border-sm text-primary" />}
              </div>
              <div className="card-body p-3">
                {performance ? (
                  <div className="row g-3 text-center small">
                    <div className="col-sm-3 col-6">
                      <div className="text-muted mb-1">On-Time Deliveries</div>
                      <div className="fw-bold text-success fs-6">{performance.onTimeDeliveries}</div>
                    </div>
                    <div className="col-sm-3 col-6">
                      <div className="text-muted mb-1">On-Time Rate</div>
                      <div className="fw-bold text-info fs-6">{performance.onTimeDeliveryRate}%</div>
                    </div>
                    <div className="col-sm-3 col-6">
                      <div className="text-muted mb-1">Safety Violations</div>
                      <div className={`fw-bold fs-6 ${performance.safetyViolationCount > 0 ? 'text-danger' : 'text-success'}`}>
                        {performance.safetyViolationCount}
                      </div>
                    </div>
                    <div className="col-sm-3 col-6">
                      <div className="text-muted mb-1">Total Distance Traveled</div>
                      <div className="fw-bold text-slate-900 fs-6">{performance.totalDistanceKm} km</div>
                    </div>
                  </div>
                ) : (
                  <div className="text-muted small text-center py-2">
                    Performance metrics recorded upon trip completion milestones.
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="modal-footer border-top py-2 px-4 bg-light">
            <button type="button" className="btn btn-sm btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
