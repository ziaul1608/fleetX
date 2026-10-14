import React, { useEffect, useState } from 'react';
import { PackagePlus, X } from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import { ShipmentCreateRequest, TripLookupOption } from '../types';

interface CreateShipmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateShipmentModal: React.FC<CreateShipmentModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<ShipmentCreateRequest>({
    trackingNumber: '',
    tripId: null,
    senderName: '',
    senderAddress: '',
    receiverName: '',
    receiverAddress: '',
    receiverPhone: '',
    originCity: '',
    destinationCity: '',
    cargoDescription: '',
    weightKg: 100,
    volumeM3: 1.5,
    promisedDeliveryTime: '',
  });
  const [availableTrips, setAvailableTrips] = useState<TripLookupOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setError(null);
      shipmentApi
        .getAvailableTrips()
        .then(setAvailableTrips)
        .catch(() => setAvailableTrips([]));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (formData.weightKg <= 0) {
      setError('Weight (kg) must be strictly greater than zero.');
      return;
    }
    if (formData.volumeM3 !== null && formData.volumeM3 !== undefined && formData.volumeM3 <= 0) {
      setError('Volume (m³) must be strictly greater than zero.');
      return;
    }

    setLoading(true);
    try {
      await shipmentApi.createShipment({
        ...formData,
        trackingNumber: formData.trackingNumber?.trim() || undefined,
        tripId: formData.tripId || null,
        promisedDeliveryTime: formData.promisedDeliveryTime
          ? new Date(formData.promisedDeliveryTime).toISOString()
          : null,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to create shipment manifest.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="modal d-block"
      style={{ backgroundColor: 'rgba(5, 8, 22, 0.85)', backdropFilter: 'blur(8px)', zIndex: 1055 }}
      role="dialog"
      aria-modal="true"
    >
      <div className="modal-dialog modal-lg modal-dialog-centered">
        <div
          className="modal-content text-light border"
          style={{
            background: 'rgba(13, 20, 38, 0.96)',
            borderColor: 'rgba(56, 189, 248, 0.28)',
            borderRadius: '16px',
          }}
        >
          <div className="modal-header border-bottom border-secondary border-opacity-25">
            <div className="d-flex align-items-center gap-2">
              <PackagePlus size={20} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">Create New Shipment Manifest</h5>
            </div>
            <button
              type="button"
              className="btn btn-sm btn-link text-secondary"
              onClick={onClose}
              aria-label="Close"
            >
              <X size={18} />
            </button>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {error && (
                <div className="alert alert-danger py-2 small" role="alert">
                  {error}
                </div>
              )}

              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label small text-secondary">
                    Tracking Reference (Optional — Auto-generated if blank)
                  </label>
                  <input
                    type="text"
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="e.g., SHP-2026-9001"
                    value={formData.trackingNumber || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, trackingNumber: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">
                    Associate with Existing Trip (Optional)
                  </label>
                  <select
                    className="form-select bg-dark text-light border-secondary"
                    value={formData.tripId ?? ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        tripId: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                  >
                    <option value="">-- Unassigned (Assign Later) --</option>
                    {availableTrips.map((trip) => (
                      <option key={trip.id} value={trip.id}>
                        {trip.tripNumber} ({trip.originAddress} → {trip.destinationAddress}) [{trip.status}]
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Sender / Shipper Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Apex Industrial Logistics"
                    value={formData.senderName}
                    onChange={(e) =>
                      setFormData({ ...formData, senderName: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Receiver / Consignee Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Horizon Distribution Hub"
                    value={formData.receiverName}
                    onChange={(e) =>
                      setFormData({ ...formData, receiverName: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Origin / Pickup City *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Mumbai, MH"
                    value={formData.originCity}
                    onChange={(e) =>
                      setFormData({ ...formData, originCity: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Destination City *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Pune, MH"
                    value={formData.destinationCity}
                    onChange={(e) =>
                      setFormData({ ...formData, destinationCity: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Pickup Address</label>
                  <input
                    type="text"
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Plot 14, MIDC Industrial Area"
                    value={formData.senderAddress || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, senderAddress: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Delivery Address</label>
                  <input
                    type="text"
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Sector 7, Chakan Logistics Park"
                    value={formData.receiverAddress || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, receiverAddress: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small text-secondary">Weight (kg) *</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    aria-label="Weight (kg)"
                    className="form-control bg-dark text-light border-secondary"
                    value={formData.weightKg}
                    onChange={(e) =>
                      setFormData({ ...formData, weightKg: Number(e.target.value) })
                    }
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small text-secondary">Volume (m³)</label>
                  <input
                    type="number"
                    step="0.01"
                    className="form-control bg-dark text-light border-secondary"
                    value={formData.volumeM3 ?? ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        volumeM3: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small text-secondary">Receiver Phone</label>
                  <input
                    type="text"
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="+91-9876543210"
                    value={formData.receiverPhone || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, receiverPhone: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Cargo Description</label>
                  <input
                    type="text"
                    className="form-control bg-dark text-light border-secondary"
                    placeholder="Palletized Automotive Components"
                    value={formData.cargoDescription || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, cargoDescription: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Promised Delivery Time</label>
                  <input
                    type="datetime-local"
                    className="form-control bg-dark text-light border-secondary"
                    value={formData.promisedDeliveryTime || ''}
                    onChange={(e) =>
                      setFormData({ ...formData, promisedDeliveryTime: e.target.value })
                    }
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer border-top border-secondary border-opacity-25">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-info btn-sm text-dark fw-semibold"
                disabled={loading}
              >
                {loading ? 'Creating...' : 'Create Shipment'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
