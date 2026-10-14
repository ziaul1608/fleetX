import React, { useState, useEffect } from 'react';
import { Wrench, AlertCircle } from 'lucide-react';
import { VehicleDto, MaintenanceRecordCreateRequest } from '../types';

interface ScheduleMaintenanceModalProps {
  isOpen: boolean;
  vehicle: VehicleDto | null;
  onClose: () => void;
  onSubmit: (vehicleId: number, data: MaintenanceRecordCreateRequest) => Promise<void>;
}

const SERVICE_TYPES = ['PREVENTIVE', 'REPAIR', 'INSPECTION'];

export const ScheduleMaintenanceModal: React.FC<ScheduleMaintenanceModalProps> = ({
  isOpen,
  vehicle,
  onClose,
  onSubmit,
}) => {
  const [serviceType, setServiceType] = useState('PREVENTIVE');
  const [description, setDescription] = useState('Scheduled periodic maintenance & mechanical inspection');
  const [cost, setCost] = useState<number>(12000);
  const [serviceDate, setServiceDate] = useState(new Date().toISOString().slice(0, 10));
  const [odometerAtService, setOdometerAtService] = useState<number>(0);
  const [nextServiceDueKm, setNextServiceDueKm] = useState<number | ''>('');
  const [nextServiceDueDate, setNextServiceDueDate] = useState<string>('');
  const [status, setStatus] = useState('COMPLETED');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (vehicle) {
      setOdometerAtService(vehicle.odometerKm);
      setNextServiceDueKm(Math.round(vehicle.odometerKm + 10000));
      const nextDate = new Date();
      nextDate.setMonth(nextDate.getMonth() + 6);
      setNextServiceDueDate(nextDate.toISOString().slice(0, 10));
      setError(null);
    }
  }, [vehicle]);

  if (!isOpen || !vehicle) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!description.trim()) {
      setError('Service description is required.');
      return;
    }
    if (cost < 0) {
      setError('Cost cannot be negative.');
      return;
    }
    if (odometerAtService < 0) {
      setError('Service odometer cannot be negative.');
      return;
    }
    if (nextServiceDueKm !== '' && nextServiceDueKm <= odometerAtService) {
      setError('Next service due km must be greater than current service odometer reading.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(vehicle.id, {
        serviceType,
        description: description.trim(),
        cost,
        serviceDate,
        odometerAtService,
        nextServiceDueKm: nextServiceDueKm !== '' ? Number(nextServiceDueKm) : undefined,
        nextServiceDueDate: nextServiceDueDate ? nextServiceDueDate : undefined,
        status,
      });
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to record maintenance.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal fade show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.45)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow border-0">
          <div className="modal-header border-bottom py-3">
            <h5 className="modal-title fs-6 fw-bold text-slate-900 d-flex align-items-center gap-2">
              <Wrench size={18} className="text-primary" />
              Schedule / Record Maintenance: {vehicle.licensePlate}
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

              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-type">
                    Maintenance Classification <span className="text-danger">*</span>
                  </label>
                  <select
                    id="m-type"
                    className="form-select form-select-sm"
                    value={serviceType}
                    onChange={(e) => setServiceType(e.target.value)}
                  >
                    {SERVICE_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-status">
                    Execution State <span className="text-danger">*</span>
                  </label>
                  <select
                    id="m-status"
                    className="form-select form-select-sm"
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                  >
                    <option value="COMPLETED">COMPLETED (Servicing finished)</option>
                    <option value="SCHEDULED">SCHEDULED (Upcoming booking)</option>
                  </select>
                </div>

                <div className="col-12">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-desc">
                    Service Description &amp; Notes <span className="text-danger">*</span>
                  </label>
                  <textarea
                    id="m-desc"
                    className="form-control form-control-sm"
                    rows={2}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-cost">
                    Invoiced Cost (₹ / $) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="m-cost"
                    type="number"
                    step="0.01"
                    min="0"
                    className="form-control form-control-sm"
                    value={cost}
                    onChange={(e) => setCost(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-date">
                    Service Date <span className="text-danger">*</span>
                  </label>
                  <input
                    id="m-date"
                    type="date"
                    className="form-control form-control-sm"
                    value={serviceDate}
                    onChange={(e) => setServiceDate(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-odo">
                    Odometer at Service (km) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="m-odo"
                    type="number"
                    step="0.1"
                    min="0"
                    className="form-control form-control-sm"
                    value={odometerAtService}
                    onChange={(e) => setOdometerAtService(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-12 col-md-6 pt-2 border-top">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-next-km">
                    Next Service Due (Odometer km)
                  </label>
                  <input
                    id="m-next-km"
                    type="number"
                    step="0.1"
                    className="form-control form-control-sm"
                    placeholder="e.g. 20000"
                    value={nextServiceDueKm}
                    onChange={(e) => setNextServiceDueKm(e.target.value === '' ? '' : Number(e.target.value))}
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Automated warnings trigger when vehicle is within 500 km of this mark.
                  </div>
                </div>

                <div className="col-12 col-md-6 pt-2 border-top">
                  <label className="form-label small fw-medium text-secondary" htmlFor="m-next-date">
                    Next Service Due (Calendar Date)
                  </label>
                  <input
                    id="m-next-date"
                    type="date"
                    className="form-control form-control-sm"
                    value={nextServiceDueDate}
                    onChange={(e) => setNextServiceDueDate(e.target.value)}
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Triggers automated warning within 14 days of this date.
                  </div>
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
                {submitting ? 'Recording...' : 'Save Maintenance Record'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
