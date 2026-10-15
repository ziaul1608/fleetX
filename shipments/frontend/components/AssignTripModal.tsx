import React, { useEffect, useState } from 'react';
import { Link2, X } from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import { Shipment, TripLookupOption } from '../types';

interface AssignTripModalProps {
  shipment: Shipment | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const AssignTripModal: React.FC<AssignTripModalProps> = ({
  shipment,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [trips, setTrips] = useState<TripLookupOption[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<string>('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && shipment) {
      setError(null);
      setSelectedTripId(shipment.tripId ? String(shipment.tripId) : '');
      setNotes('');
      shipmentApi
        .getAvailableTrips()
        .then(setTrips)
        .catch(() => setTrips([]));
    }
  }, [isOpen, shipment]);

  if (!isOpen || !shipment) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!selectedTripId) {
      setError('Please select a valid Trip ID to associate with this shipment.');
      return;
    }

    setLoading(true);
    try {
      await shipmentApi.assignTrip(shipment.id, {
        tripId: Number(selectedTripId),
        notes: notes.trim() || undefined,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to associate shipment with trip.'
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
      <div className="modal-dialog modal-dialog-centered">
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
              <Link2 size={18} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">
                Assign Trip — {shipment.trackingNumber}
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

              <div className="mb-3">
                <div className="small text-secondary mb-1">Route Summary</div>
                <div className="fw-semibold">
                  {shipment.originCity} → {shipment.destinationCity} ({shipment.weightKg} kg)
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label small text-secondary">
                  Select Active or Scheduled Trip *
                </label>
                <select
                  className="form-select bg-dark text-light border-secondary"
                  value={selectedTripId}
                  onChange={(e) => setSelectedTripId(e.target.value)}
                  required
                >
                  <option value="">-- Select a Dispatch Trip --</option>
                  {trips.map((trip) => (
                    <option key={trip.id} value={trip.id}>
                      {trip.tripNumber} | {trip.originAddress} → {trip.destinationAddress} [{trip.status}]
                    </option>
                  ))}
                </select>
              </div>

              <div className="mb-2">
                <label className="form-label small text-secondary">
                  Or Enter Trip ID Directly
                </label>
                <input
                  type="number"
                  className="form-control bg-dark text-light border-secondary"
                  placeholder="Numeric Trip ID"
                  value={selectedTripId}
                  onChange={(e) => setSelectedTripId(e.target.value)}
                />
              </div>

              <div className="mt-3">
                <label className="form-label small text-secondary">Dispatch Notes</label>
                <textarea
                  className="form-control bg-dark text-light border-secondary"
                  rows={2}
                  placeholder="Optional loading bay or manifest link note"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
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
                {loading ? 'Assigning...' : 'Confirm Trip Association'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
