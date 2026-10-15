import React, { useState, useEffect } from 'react';
import { Truck, AlertCircle } from 'lucide-react';
import { VehicleDto, VehicleUpdateRequest, VehicleType, FuelType } from '../types';

interface EditVehicleModalProps {
  isOpen: boolean;
  vehicle: VehicleDto | null;
  onClose: () => void;
  onSubmit: (id: number, data: VehicleUpdateRequest) => Promise<void>;
}

const VEHICLE_TYPES: VehicleType[] = ['TRUCK', 'VAN', 'CONTAINER', 'MOTORCYCLE', 'TRAILER'];
const FUEL_TYPES: FuelType[] = ['DIESEL', 'PETROL', 'ELECTRIC', 'CNG', 'HYBRID'];
const VEHICLE_STATUSES = ['AVAILABLE', 'ACTIVE', 'IDLE', 'IN_TRANSIT', 'IN_TRIP', 'MAINTENANCE', 'OFFLINE', 'RETIRED'];

export const EditVehicleModal: React.FC<EditVehicleModalProps> = ({
  isOpen,
  vehicle,
  onClose,
  onSubmit,
}) => {
  const [make, setMake] = useState('');
  const [model, setModel] = useState('');
  const [year, setYear] = useState(2024);
  const [type, setType] = useState('TRUCK');
  const [fuelType, setFuelType] = useState('DIESEL');
  const [capacityKg, setCapacityKg] = useState<number>(20000);
  const [capacityVolumeM3, setCapacityVolumeM3] = useState<number>(35);
  const [status, setStatus] = useState('AVAILABLE');
  const [active, setActive] = useState(true);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (vehicle) {
      setMake(vehicle.make);
      setModel(vehicle.model);
      setYear(vehicle.modelYear);
      setType(vehicle.vehicleType);
      setFuelType(vehicle.fuelType);
      setCapacityKg(vehicle.capacityKg);
      setCapacityVolumeM3(vehicle.capacityVolumeM3);
      setStatus(vehicle.status);
      setActive(vehicle.active);
      setError(null);
    }
  }, [vehicle]);

  if (!isOpen || !vehicle) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(vehicle.id, {
        make: make.trim(),
        model: model.trim(),
        year,
        type,
        fuelType,
        capacityKg,
        capacityVolumeM3,
        status,
        active,
      });
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string; validationErrors?: Array<{ message: string }> };
      if (apiErr.validationErrors && apiErr.validationErrors.length > 0) {
        setError(apiErr.validationErrors.map((v) => v.message).join(', '));
      } else {
        setError(apiErr.message || 'Failed to update vehicle.');
      }
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
              <Truck size={18} className="text-primary" />
              Update Vehicle: {vehicle.licensePlate}
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
                  <label className="form-label small fw-medium text-secondary">
                    Registration Plate
                  </label>
                  <input
                    type="text"
                    className="form-control form-control-sm bg-light text-muted"
                    value={vehicle.licensePlate}
                    disabled
                  />
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary">
                    VIN Identifier
                  </label>
                  <input
                    type="text"
                    className="form-control form-control-sm bg-light text-muted"
                    value={vehicle.vin}
                    disabled
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-make">
                    Make <span className="text-danger">*</span>
                  </label>
                  <input
                    id="edit-v-make"
                    type="text"
                    className="form-control form-control-sm"
                    value={make}
                    onChange={(e) => setMake(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-model">
                    Model <span className="text-danger">*</span>
                  </label>
                  <input
                    id="edit-v-model"
                    type="text"
                    className="form-control form-control-sm"
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-year">
                    Year <span className="text-danger">*</span>
                  </label>
                  <input
                    id="edit-v-year"
                    type="number"
                    min={1980}
                    max={2030}
                    className="form-control form-control-sm"
                    value={year}
                    onChange={(e) => setYear(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-type">
                    Category <span className="text-danger">*</span>
                  </label>
                  <select
                    id="edit-v-type"
                    className="form-select form-select-sm"
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                  >
                    {VEHICLE_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-fuel">
                    Fuel Type <span className="text-danger">*</span>
                  </label>
                  <select
                    id="edit-v-fuel"
                    className="form-select form-select-sm"
                    value={fuelType}
                    onChange={(e) => setFuelType(e.target.value)}
                  >
                    {FUEL_TYPES.map((f) => (
                      <option key={f} value={f}>
                        {f}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-cap-kg">
                    Payload (kg) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="edit-v-cap-kg"
                    type="number"
                    step="0.1"
                    min="1"
                    className="form-control form-control-sm"
                    value={capacityKg}
                    onChange={(e) => setCapacityKg(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-cap-m3">
                    Volume (m³) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="edit-v-cap-m3"
                    type="number"
                    step="0.1"
                    min="0.1"
                    className="form-control form-control-sm"
                    value={capacityVolumeM3}
                    onChange={(e) => setCapacityVolumeM3(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="edit-v-status">
                    Status <span className="text-danger">*</span>
                  </label>
                  <select
                    id="edit-v-status"
                    className="form-select form-select-sm"
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                  >
                    {VEHICLE_STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 pt-2 border-top">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="edit-v-active"
                      checked={active}
                      onChange={(e) => setActive(e.target.checked)}
                    />
                    <label className="form-check-label small fw-medium" htmlFor="edit-v-active">
                      Active Asset ({active ? 'Enabled for Dispatch' : 'Deactivated / Sidelined'})
                    </label>
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
                {submitting ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
