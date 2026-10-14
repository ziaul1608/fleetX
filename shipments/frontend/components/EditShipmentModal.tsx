import React, { useEffect, useState } from 'react';
import { Edit3, X } from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import { Shipment, ShipmentUpdateRequest } from '../types';

interface EditShipmentModalProps {
  shipment: Shipment | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const EditShipmentModal: React.FC<EditShipmentModalProps> = ({
  shipment,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<ShipmentUpdateRequest>({
    senderName: '',
    senderAddress: '',
    receiverName: '',
    receiverAddress: '',
    receiverPhone: '',
    originCity: '',
    destinationCity: '',
    cargoDescription: '',
    weightKg: 100,
    volumeM3: null,
    promisedDeliveryTime: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (shipment && isOpen) {
      setError(null);
      setFormData({
        senderName: shipment.senderName,
        senderAddress: shipment.senderAddress || '',
        receiverName: shipment.receiverName,
        receiverAddress: shipment.receiverAddress || '',
        receiverPhone: shipment.receiverPhone || '',
        originCity: shipment.originCity,
        destinationCity: shipment.destinationCity,
        cargoDescription: shipment.cargoDescription || '',
        weightKg: shipment.weightKg,
        volumeM3: shipment.volumeM3,
        promisedDeliveryTime: shipment.promisedDeliveryTime
          ? shipment.promisedDeliveryTime.slice(0, 16)
          : '',
      });
    }
  }, [shipment, isOpen]);

  if (!isOpen || !shipment) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (formData.weightKg <= 0) {
      setError('Weight (kg) must be strictly greater than zero.');
      return;
    }

    setLoading(true);
    try {
      await shipmentApi.updateShipment(shipment.id, {
        ...formData,
        promisedDeliveryTime: formData.promisedDeliveryTime
          ? new Date(formData.promisedDeliveryTime).toISOString()
          : null,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(
        err?.response?.data?.message || err?.message || 'Failed to update shipment.'
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
              <Edit3 size={18} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">
                Edit Shipment — {shipment.trackingNumber}
              </h5>
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
                  <label className="form-label small text-secondary">Sender Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    value={formData.senderName}
                    onChange={(e) =>
                      setFormData({ ...formData, senderName: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Receiver Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
                    value={formData.receiverName}
                    onChange={(e) =>
                      setFormData({ ...formData, receiverName: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Origin City *</label>
                  <input
                    type="text"
                    required
                    className="form-control bg-dark text-light border-secondary"
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
                    value={formData.destinationCity}
                    onChange={(e) =>
                      setFormData({ ...formData, destinationCity: e.target.value })
                    }
                  />
                </div>

                <div className="col-md-4">
                  <label className="form-label small text-secondary">Weight (kg) *</label>
                  <input
                    type="number"
                    step="0.01"
                    required
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
                {loading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
