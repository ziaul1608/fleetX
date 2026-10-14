import React, { useEffect, useState } from 'react';
import { Navigation, AlertCircle } from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Driver, TripCreateRequest } from '../types';
import { apiClient } from '../../../shared/api/client';

interface CreateTripModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface VehicleOption {
  id: number;
  licensePlate: string;
  make: string;
  model: string;
  vehicleType: string;
}

export const CreateTripModal: React.FC<CreateTripModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [availableDrivers, setAvailableDrivers] = useState<Driver[]>([]);
  const [availableVehicles, setAvailableVehicles] = useState<VehicleOption[]>([]);
  const [loadingOptions, setLoadingOptions] = useState(false);

  const [formData, setFormData] = useState<{
    vehicleId: string;
    driverId: string;
    originAddress: string;
    destinationAddress: string;
    scheduledStartTime: string;
    scheduledEndTime: string;
    distanceKm: string;
    notes: string;
  }>({
    vehicleId: '',
    driverId: '',
    originAddress: '',
    destinationAddress: '',
    scheduledStartTime: '',
    scheduledEndTime: '',
    distanceKm: '',
    notes: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setLoadingOptions(true);
      setError(null);

      // Fetch eligible drivers
      const fetchDrivers = driverApi
        .getDrivers({ availability: 'AVAILABLE', active: true, size: 50 })
        .then((res) => setAvailableDrivers(res.content || []))
        .catch(() => setAvailableDrivers([]));

      // Fetch eligible vehicles
      const fetchVehicles = apiClient
        .get('/api/v1/fleet/vehicles', { params: { status: 'AVAILABLE', active: true, size: 50 } })
        .then((res: any) => {
          const list = res.data?.data?.content || [];
          setAvailableVehicles(
            list.map((v: any) => ({
              id: v.id,
              licensePlate: v.licensePlate,
              make: v.make,
              model: v.model,
              vehicleType: v.vehicleType || v.type,
            }))
          );
        })
        .catch(() => setAvailableVehicles([]));

      Promise.all([fetchDrivers, fetchVehicles]).finally(() => setLoadingOptions(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.vehicleId || !formData.driverId) {
      setError('Please select both an available commercial vehicle and an eligible driver.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload: TripCreateRequest = {
        vehicleId: Number(formData.vehicleId),
        driverId: Number(formData.driverId),
        originAddress: formData.originAddress.trim(),
        destinationAddress: formData.destinationAddress.trim(),
        scheduledStartTime: new Date(formData.scheduledStartTime).toISOString(),
        scheduledEndTime: formData.scheduledEndTime ? new Date(formData.scheduledEndTime).toISOString() : undefined,
        distanceKm: formData.distanceKm ? Number(formData.distanceKm) : undefined,
        notes: formData.notes.trim() || undefined,
      };

      await driverApi.createTrip(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to dispatch trip. Please verify resource availability.');
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
              <Navigation size={20} className="text-primary" /> Schedule &amp; Dispatch Logistics Trip
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

              {loadingOptions && (
                <div className="text-muted small mb-3 d-flex align-items-center gap-2">
                  <span className="spinner-border spinner-border-sm text-primary" /> Querying eligible resources across modules...
                </div>
              )}

              <div className="row g-3">
                {/* Vehicle Selection */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Select Available Vehicle *</label>
                  <select
                    required
                    className="form-select form-select-sm"
                    value={formData.vehicleId}
                    onChange={(e) => setFormData({ ...formData, vehicleId: e.target.value })}
                  >
                    <option value="">-- Choose Commercial Asset --</option>
                    {availableVehicles.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.licensePlate} — {v.make} {v.model} ({v.vehicleType})
                      </option>
                    ))}
                  </select>
                  {availableVehicles.length === 0 && !loadingOptions && (
                    <small className="text-warning">No vehicles currently in AVAILABLE state.</small>
                  )}
                </div>

                {/* Driver Selection */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Select Certified Driver *</label>
                  <select
                    required
                    className="form-select form-select-sm"
                    value={formData.driverId}
                    onChange={(e) => setFormData({ ...formData, driverId: e.target.value })}
                  >
                    <option value="">-- Choose Certified Driver --</option>
                    {availableDrivers.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.fullName} ({d.licenseNumber} - {d.licenseCategory}, Rating: {d.safetyScore}/100)
                      </option>
                    ))}
                  </select>
                  {availableDrivers.length === 0 && !loadingOptions && (
                    <small className="text-warning">No drivers currently in AVAILABLE status.</small>
                  )}
                </div>

                {/* Origin */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Origin Dispatch Address *</label>
                  <input
                    type="text"
                    required
                    className="form-control form-control-sm"
                    placeholder="e.g. Central Depot 1, Bhiwandi, Thane"
                    value={formData.originAddress}
                    onChange={(e) => setFormData({ ...formData, originAddress: e.target.value })}
                  />
                </div>

                {/* Destination */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Destination Delivery Address *</label>
                  <input
                    type="text"
                    required
                    className="form-control form-control-sm"
                    placeholder="e.g. Regional Fulfillment Hub, Hinjawadi Phase 2, Pune"
                    value={formData.destinationAddress}
                    onChange={(e) => setFormData({ ...formData, destinationAddress: e.target.value })}
                  />
                </div>

                {/* Scheduled Start */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Scheduled Departure *</label>
                  <input
                    type="datetime-local"
                    required
                    className="form-control form-control-sm"
                    value={formData.scheduledStartTime}
                    onChange={(e) => setFormData({ ...formData, scheduledStartTime: e.target.value })}
                  />
                </div>

                {/* Scheduled End */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Scheduled Estimated Arrival (ETA)</label>
                  <input
                    type="datetime-local"
                    className="form-control form-control-sm"
                    value={formData.scheduledEndTime}
                    onChange={(e) => setFormData({ ...formData, scheduledEndTime: e.target.value })}
                  />
                </div>

                {/* Planned Distance */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Estimated Route Distance (km)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    className="form-control form-control-sm"
                    placeholder="e.g. 148.5"
                    value={formData.distanceKm}
                    onChange={(e) => setFormData({ ...formData, distanceKm: e.target.value })}
                  />
                </div>

                {/* Notes */}
                <div className="col-md-6">
                  <label className="form-label small fw-semibold text-slate-700">Trip Dispatch Notes</label>
                  <input
                    type="text"
                    className="form-control form-control-sm"
                    placeholder="e.g. High value electronics, expressway toll route"
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer border-top py-2 px-4 bg-light">
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClose} disabled={loading}>
                Cancel
              </button>
              <button type="submit" className="btn btn-sm btn-primary d-flex align-items-center gap-2" disabled={loading}>
                {loading ? 'Dispatching...' : 'Confirm & Dispatch Trip'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
