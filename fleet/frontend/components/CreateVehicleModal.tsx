import React, { useState } from 'react';
import { Truck, AlertCircle } from 'lucide-react';
import { VehicleCreateRequest, VehicleType, FuelType } from '../types';

interface CreateVehicleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: VehicleCreateRequest) => Promise<void>;
}

const VEHICLE_TYPES: VehicleType[] = ['TRUCK', 'VAN', 'CONTAINER', 'MOTORCYCLE', 'TRAILER'];
const FUEL_TYPES: FuelType[] = ['DIESEL', 'PETROL', 'ELECTRIC', 'CNG', 'HYBRID'];

export const CreateVehicleModal: React.FC<CreateVehicleModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
}) => {
  const [vin, setVin] = useState('');
  const [licensePlate, setLicensePlate] = useState('');
  const [make, setMake] = useState('Tata');
  const [model, setModel] = useState('');
  const [year, setYear] = useState(new Date().getFullYear());
  const [type, setType] = useState<VehicleType>('TRUCK');
  const [fuelType, setFuelType] = useState<FuelType>('DIESEL');
  const [capacityKg, setCapacityKg] = useState<number>(20000);
  const [capacityVolumeM3, setCapacityVolumeM3] = useState<number>(35);
  const [initialOdometerKm, setInitialOdometerKm] = useState<number>(0);
  const status = 'AVAILABLE';

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanVin = vin.trim().toUpperCase();
    const cleanPlate = licensePlate.trim().toUpperCase();

    if (!cleanVin || cleanVin.length !== 17) {
      setError('VIN must be exactly 17 alphanumeric characters.');
      return;
    }
    if (!cleanPlate) {
      setError('Registration license plate is required.');
      return;
    }
    if (capacityKg <= 0 || capacityVolumeM3 <= 0) {
      setError('Payload capacities must be positive numbers.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit({
        vin: cleanVin,
        licensePlate: cleanPlate,
        make: make.trim(),
        model: model.trim(),
        year,
        type,
        fuelType,
        capacityKg,
        capacityVolumeM3,
        initialOdometerKm,
        status,
      });
      // Reset form
      setVin('');
      setLicensePlate('');
      setModel('');
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string; validationErrors?: Array<{ message: string }> };
      if (apiErr.validationErrors && apiErr.validationErrors.length > 0) {
        setError(apiErr.validationErrors.map((v) => v.message).join(', '));
      } else {
        setError(apiErr.message || 'Failed to register commercial vehicle.');
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
              Onboard Commercial Vehicle
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
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-vin">
                    VIN (Vehicle Identification Number) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-vin"
                    type="text"
                    className="form-control form-control-sm text-uppercase"
                    maxLength={17}
                    placeholder="e.g. 1HGCR2F83HA001234"
                    value={vin}
                    onChange={(e) => setVin(e.target.value)}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    17 uppercase alphanumeric characters
                  </div>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-plate">
                    Registration Plate <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-plate"
                    type="text"
                    className="form-control form-control-sm text-uppercase"
                    placeholder="e.g. MH-12-AB-1234"
                    value={licensePlate}
                    onChange={(e) => setLicensePlate(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-make">
                    Make / Manufacturer <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-make"
                    type="text"
                    className="form-control form-control-sm"
                    placeholder="e.g. Tata, Volvo, BharatBenz"
                    value={make}
                    onChange={(e) => setMake(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-model">
                    Model Name <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-model"
                    type="text"
                    className="form-control form-control-sm"
                    placeholder="e.g. Prima 4028"
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-year">
                    Manufacturing Year <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-year"
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
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-type">
                    Vehicle Category <span className="text-danger">*</span>
                  </label>
                  <select
                    id="v-type"
                    className="form-select form-select-sm"
                    value={type}
                    onChange={(e) => setType(e.target.value as VehicleType)}
                  >
                    {VEHICLE_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 col-md-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-fuel">
                    Fuel / Powertrain Type <span className="text-danger">*</span>
                  </label>
                  <select
                    id="v-fuel"
                    className="form-select form-select-sm"
                    value={fuelType}
                    onChange={(e) => setFuelType(e.target.value as FuelType)}
                  >
                    {FUEL_TYPES.map((f) => (
                      <option key={f} value={f}>
                        {f}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-12 col-md-4">
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-cap-kg">
                    Payload Capacity (kg) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-cap-kg"
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
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-cap-m3">
                    Volume Capacity (m³) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-cap-m3"
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
                  <label className="form-label small fw-medium text-secondary" htmlFor="v-odo">
                    Initial Odometer (km) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="v-odo"
                    type="number"
                    step="0.1"
                    min="0"
                    className="form-control form-control-sm"
                    value={initialOdometerKm}
                    onChange={(e) => setInitialOdometerKm(Number(e.target.value))}
                    required
                  />
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
                {submitting ? 'Registering...' : 'Register Vehicle'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
