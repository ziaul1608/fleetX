import React, { useState, useEffect } from 'react';
import { Fuel, AlertCircle, Calculator } from 'lucide-react';
import { VehicleDto, FuelRecordCreateRequest } from '../types';

interface LogFuelModalProps {
  isOpen: boolean;
  vehicle: VehicleDto | null;
  onClose: () => void;
  onSubmit: (vehicleId: number, data: FuelRecordCreateRequest) => Promise<void>;
}

export const LogFuelModal: React.FC<LogFuelModalProps> = ({
  isOpen,
  vehicle,
  onClose,
  onSubmit,
}) => {
  const [liters, setLiters] = useState<number>(100);
  const [pricePerLiter, setPricePerLiter] = useState<number>(95);
  const [totalCost, setTotalCost] = useState<number>(9500);
  const [odometerAtFueling, setOdometerAtFueling] = useState<number>(0);
  const [fuelStation, setFuelStation] = useState('IndianOil National Highway Hub');
  const [fuelDate, setFuelDate] = useState(new Date().toISOString().slice(0, 16));

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (vehicle) {
      setOdometerAtFueling(vehicle.odometerKm);
      setError(null);
    }
  }, [vehicle]);

  if (!isOpen || !vehicle) return null;

  const handleLitersChange = (val: number) => {
    setLiters(val);
    if (pricePerLiter > 0) {
      setTotalCost(Math.round(val * pricePerLiter * 100) / 100);
    }
  };

  const handlePriceChange = (val: number) => {
    setPricePerLiter(val);
    if (liters > 0) {
      setTotalCost(Math.round(val * liters * 100) / 100);
    }
  };

  const handleCostChange = (val: number) => {
    setTotalCost(val);
    if (liters > 0) {
      setPricePerLiter(Math.round((val / liters) * 100) / 100);
    }
  };

  const deltaDistance = odometerAtFueling - vehicle.odometerKm;
  const estimatedEfficiency = (deltaDistance > 0 && liters > 0)
    ? (deltaDistance / liters).toFixed(2)
    : null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (liters <= 0) {
      setError('Liters consumed must be greater than zero.');
      return;
    }
    if (odometerAtFueling < 0) {
      setError('Odometer reading cannot be negative.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit(vehicle.id, {
        liters,
        pricePerLiter: pricePerLiter > 0 ? pricePerLiter : undefined,
        totalCost: totalCost > 0 ? totalCost : undefined,
        odometerAtFueling,
        fuelStation: fuelStation.trim() || undefined,
        fuelDate: new Date(fuelDate).toISOString(),
      });
      onClose();
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to log fuel record.');
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
              <Fuel size={18} className="text-primary" />
              Log Fuel Intake: {vehicle.licensePlate}
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
                <div className="col-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-liters">
                    Fuel Volume (Litres) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="f-liters"
                    type="number"
                    step="0.01"
                    min="0.1"
                    className="form-control form-control-sm"
                    value={liters}
                    onChange={(e) => handleLitersChange(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="col-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-price">
                    Price per Litre (₹ / $)
                  </label>
                  <input
                    id="f-price"
                    type="number"
                    step="0.01"
                    min="0"
                    className="form-control form-control-sm"
                    value={pricePerLiter}
                    onChange={(e) => handlePriceChange(Number(e.target.value))}
                  />
                </div>

                <div className="col-12">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-cost">
                    Total Transaction Cost
                  </label>
                  <div className="input-group input-group-sm">
                    <span className="input-group-text bg-light text-muted">
                      <Calculator size={14} />
                    </span>
                    <input
                      id="f-cost"
                      type="number"
                      step="0.01"
                      className="form-control"
                      value={totalCost}
                      onChange={(e) => handleCostChange(Number(e.target.value))}
                    />
                  </div>
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Calculated in Java using validated inputs: Litres × Price per Litre.
                  </div>
                </div>

                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-odo">
                    Odometer at Fueling (km) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="f-odo"
                    type="number"
                    step="0.1"
                    min="0"
                    className="form-control form-control-sm"
                    value={odometerAtFueling}
                    onChange={(e) => setOdometerAtFueling(Number(e.target.value))}
                    required
                  />
                  <div className="form-text small" style={{ fontSize: '0.75rem' }}>
                    Current vehicle reading: {vehicle.odometerKm.toLocaleString()} km
                  </div>
                </div>

                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-date">
                    Fueling Date &amp; Time
                  </label>
                  <input
                    id="f-date"
                    type="datetime-local"
                    className="form-control form-control-sm"
                    value={fuelDate}
                    onChange={(e) => setFuelDate(e.target.value)}
                    required
                  />
                </div>

                <div className="col-12">
                  <label className="form-label small fw-medium text-secondary" htmlFor="f-station">
                    Fuel Station / Location
                  </label>
                  <input
                    id="f-station"
                    type="text"
                    className="form-control form-control-sm"
                    placeholder="e.g. IndianOil Express Hub, Pune Highway"
                    value={fuelStation}
                    onChange={(e) => setFuelStation(e.target.value)}
                  />
                </div>

                {/* Calculation preview callout */}
                <div className="col-12">
                  <div className="p-2 bg-light rounded border text-muted small">
                    <div className="d-flex justify-content-between mb-1">
                      <span>Trip Distance Covered:</span>
                      <strong className="text-slate-900">
                        {deltaDistance > 0 ? `${deltaDistance.toFixed(1)} km` : '0.0 km (baseline)'}
                      </strong>
                    </div>
                    <div className="d-flex justify-content-between">
                      <span>Estimated Fuel Efficiency:</span>
                      <strong className={estimatedEfficiency ? 'text-success' : 'text-slate-900'}>
                        {estimatedEfficiency ? `${estimatedEfficiency} km/L` : 'First Log / Baseline'}
                      </strong>
                    </div>
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
                {submitting ? 'Recording...' : 'Record Fuel Entry'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
