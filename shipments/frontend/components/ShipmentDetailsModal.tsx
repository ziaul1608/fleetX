import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  MapPin,
  Package,
  Truck,
  X,
} from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import { Shipment, ShipmentStatus, ShipmentTimelineEvent } from '../types';

interface ShipmentDetailsModalProps {
  shipment: Shipment | null;
  isOpen: boolean;
  onClose: () => void;
  onUpdated: () => void;
}

const ALLOWED_TRANSITIONS: Record<ShipmentStatus, ShipmentStatus[]> = {
  CREATED: ['ASSIGNED', 'PICKED_UP', 'DELAYED', 'CANCELLED'],
  ASSIGNED: ['PICKED_UP', 'IN_TRANSIT', 'DELAYED', 'CANCELLED'],
  PICKED_UP: ['IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELAYED', 'CANCELLED'],
  IN_TRANSIT: ['OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED', 'CANCELLED'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'DELAYED', 'CANCELLED'],
  DELAYED: ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
};

export const ShipmentDetailsModal: React.FC<ShipmentDetailsModalProps> = ({
  shipment,
  isOpen,
  onClose,
  onUpdated,
}) => {
  const [currentShipment, setCurrentShipment] = useState<Shipment | null>(shipment);
  const [timeline, setTimeline] = useState<ShipmentTimelineEvent[]>([]);
  const [nextStatus, setNextStatus] = useState<string>('');
  const [locationName, setLocationName] = useState('');
  const [description, setDescription] = useState('');
  const [deliveryNotes, setDeliveryNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && shipment) {
      setCurrentShipment(shipment);
      setError(null);
      const transitions = ALLOWED_TRANSITIONS[shipment.status] || [];
      setNextStatus(transitions[0] || '');
      setLocationName(shipment.destinationCity || '');
      setDescription('');
      setDeliveryNotes('');

      shipmentApi
        .getTimeline(shipment.id)
        .then(setTimeline)
        .catch(() => setTimeline([]));
    }
  }, [isOpen, shipment]);

  if (!isOpen || !currentShipment) return null;

  const transitions = ALLOWED_TRANSITIONS[currentShipment.status] || [];
  const isTerminal =
    currentShipment.status === 'DELIVERED' || currentShipment.status === 'CANCELLED';

  const refreshData = async (updated: Shipment) => {
    setCurrentShipment(updated);
    const events = await shipmentApi.getTimeline(updated.id);
    setTimeline(events);
    const nextAllowed = ALLOWED_TRANSITIONS[updated.status] || [];
    setNextStatus(nextAllowed[0] || '');
    onUpdated();
  };

  const handleStatusUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nextStatus) return;
    setError(null);
    setLoading(true);
    try {
      const updated = await shipmentApi.updateStatus(currentShipment.id, {
        status: nextStatus,
        locationName: locationName.trim() || undefined,
        description: description.trim() || undefined,
      });
      await refreshData(updated);
      setDescription('');
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to transition shipment status.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteDelivery = async () => {
    setError(null);
    setLoading(true);
    try {
      const updated = await shipmentApi.completeDelivery(currentShipment.id, {
        locationName: locationName.trim() || currentShipment.destinationCity,
        deliveryNotes:
          deliveryNotes.trim() ||
          `POD confirmed at ${currentShipment.destinationCity}`,
      });
      await refreshData(updated);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to complete shipment delivery.'
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
      <div className="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
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
              <Package size={20} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">
                Shipment Manifest & Timeline — {currentShipment.trackingNumber}
              </h5>
              <span className="badge bg-info text-dark ms-2">{currentShipment.status}</span>
              {currentShipment.delayed && (
                <span className="badge bg-danger ms-1 d-inline-flex align-items-center gap-1">
                  <AlertTriangle size={12} /> DELAYED / OVERDUE
                </span>
              )}
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

          <div className="modal-body">
            {error && (
              <div className="alert alert-danger py-2 small" role="alert">
                {error}
              </div>
            )}

            <div className="row g-4">
              {/* Left Column: Manifest & Lifecycle Actions */}
              <div className="col-lg-6">
                <div
                  className="p-3 rounded-3 mb-3 border"
                  style={{
                    background: 'rgba(15, 23, 42, 0.65)',
                    borderColor: 'rgba(148, 163, 184, 0.16)',
                  }}
                >
                  <h6 className="text-info fw-semibold mb-3">Cargo & Consignment Details</h6>
                  <div className="row g-2 small">
                    <div className="col-6">
                      <span className="text-secondary d-block">Sender</span>
                      <strong>{currentShipment.senderName}</strong>
                      {currentShipment.senderAddress && (
                        <div className="text-secondary">{currentShipment.senderAddress}</div>
                      )}
                    </div>
                    <div className="col-6">
                      <span className="text-secondary d-block">Receiver</span>
                      <strong>{currentShipment.receiverName}</strong>
                      {currentShipment.receiverAddress && (
                        <div className="text-secondary">{currentShipment.receiverAddress}</div>
                      )}
                    </div>
                    <div className="col-6 mt-2">
                      <span className="text-secondary d-block">Origin → Destination</span>
                      <strong>
                        {currentShipment.originCity} → {currentShipment.destinationCity}
                      </strong>
                    </div>
                    <div className="col-6 mt-2">
                      <span className="text-secondary d-block">Weight / Volume</span>
                      <strong>
                        {currentShipment.weightKg} kg
                        {currentShipment.volumeM3 ? ` / ${currentShipment.volumeM3} m³` : ''}
                      </strong>
                    </div>
                    <div className="col-6 mt-2">
                      <span className="text-secondary d-block">Associated Trip</span>
                      <strong>
                        {currentShipment.tripNumber
                          ? `${currentShipment.tripNumber} (#${currentShipment.tripId})`
                          : 'Unassigned'}
                      </strong>
                    </div>
                    <div className="col-6 mt-2">
                      <span className="text-secondary d-block">Promised Delivery</span>
                      <strong>
                        {currentShipment.promisedDeliveryTime
                          ? new Date(currentShipment.promisedDeliveryTime).toLocaleString()
                          : 'Not specified'}
                      </strong>
                    </div>
                    {currentShipment.estimatedDistanceKm != null && (
                      <div className="col-12 mt-2 pt-2 border-top border-secondary border-opacity-25">
                        <span className="text-secondary d-block">
                          Estimated Straight-Line Distance (Haversine Formula — Not Exact Road Distance)
                        </span>
                        <strong className="text-info">
                          {currentShipment.estimatedDistanceKm} km
                        </strong>
                        {currentShipment.estimatedDurationMinutes != null && (
                          <span className="ms-2 text-secondary">
                            (Est. Journey: {currentShipment.estimatedDurationMinutes} mins | Est. ETA:{' '}
                            {currentShipment.estimatedArrivalTime
                              ? new Date(currentShipment.estimatedArrivalTime).toLocaleString()
                              : 'N/A'}
                            )
                          </span>
                        )}
                      </div>
                    )}
                    {currentShipment.deliveredAt && (
                      <div className="col-12 mt-2 pt-2 border-top border-success border-opacity-25 text-success">
                        <strong>
                          Delivered At: {new Date(currentShipment.deliveredAt).toLocaleString()}
                        </strong>
                        {currentShipment.deliveryNotes && (
                          <div>Notes: {currentShipment.deliveryNotes}</div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Status Transition Controls */}
                {!isTerminal && (
                  <div
                    className="p-3 rounded-3 mb-3 border"
                    style={{
                      background: 'rgba(15, 23, 42, 0.65)',
                      borderColor: 'rgba(56, 189, 248, 0.22)',
                    }}
                  >
                    <h6 className="text-info fw-semibold mb-2 d-flex align-items-center gap-2">
                      <Truck size={16} /> Advance Shipment Lifecycle
                    </h6>
                    <form onSubmit={handleStatusUpdate}>
                      <div className="row g-2">
                        <div className="col-md-6">
                          <label className="form-label small text-secondary">Next Status</label>
                          <select
                            className="form-select form-select-sm bg-dark text-light border-secondary"
                            value={nextStatus}
                            onChange={(e) => setNextStatus(e.target.value)}
                          >
                            {transitions.map((st) => (
                              <option key={st} value={st}>
                                {st}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="col-md-6">
                          <label className="form-label small text-secondary">Checkpoint Location</label>
                          <input
                            type="text"
                            className="form-control form-control-sm bg-dark text-light border-secondary"
                            placeholder="Hub or City"
                            value={locationName}
                            onChange={(e) => setLocationName(e.target.value)}
                          />
                        </div>
                        <div className="col-12">
                          <label className="form-label small text-secondary">Milestone Description</label>
                          <input
                            type="text"
                            className="form-control form-control-sm bg-dark text-light border-secondary"
                            placeholder="Optional scan note or delay reason"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                          />
                        </div>
                        <div className="col-12 d-flex justify-content-end mt-2">
                          <button
                            type="submit"
                            className="btn btn-info btn-sm text-dark fw-semibold"
                            disabled={loading || !nextStatus}
                          >
                            Update Status
                          </button>
                        </div>
                      </div>
                    </form>
                  </div>
                )}

                {/* Quick Delivery Completion Workflow */}
                {(currentShipment.status === 'IN_TRANSIT' ||
                  currentShipment.status === 'OUT_FOR_DELIVERY' ||
                  currentShipment.status === 'DELAYED') && (
                  <div
                    className="p-3 rounded-3 border"
                    style={{
                      background: 'rgba(16, 185, 129, 0.08)',
                      borderColor: 'rgba(16, 185, 129, 0.3)',
                    }}
                  >
                    <h6 className="text-success fw-semibold mb-2 d-flex align-items-center gap-2">
                      <CheckCircle2 size={16} /> Complete Delivery (Proof of Delivery)
                    </h6>
                    <div className="mb-2">
                      <input
                        type="text"
                        className="form-control form-control-sm bg-dark text-light border-secondary"
                        placeholder="Delivery sign-off notes (e.g., Signed by Warehouse Manager)"
                        value={deliveryNotes}
                        onChange={(e) => setDeliveryNotes(e.target.value)}
                      />
                    </div>
                    <button
                      type="button"
                      className="btn btn-success btn-sm w-100 fw-semibold"
                      disabled={loading}
                      onClick={handleCompleteDelivery}
                    >
                      Confirm Final Delivery
                    </button>
                  </div>
                )}
              </div>

              {/* Right Column: Chronological Delivery Timeline */}
              <div className="col-lg-6">
                <div
                  className="p-3 rounded-3 h-100 border"
                  style={{
                    background: 'rgba(15, 23, 42, 0.65)',
                    borderColor: 'rgba(148, 163, 184, 0.16)',
                  }}
                >
                  <h6 className="text-info fw-semibold mb-3 d-flex align-items-center gap-2">
                    <Clock size={16} /> Chronological Delivery Timeline ({timeline.length} Events)
                  </h6>

                  {timeline.length === 0 ? (
                    <div className="text-secondary small py-4 text-center">
                      No timeline events recorded yet.
                    </div>
                  ) : (
                    <div className="d-flex flex-column gap-3">
                      {timeline.map((evt, idx) => (
                        <div
                          key={evt.id ?? idx}
                          className="p-2 rounded border-start border-3 border-info"
                          style={{ background: 'rgba(30, 41, 59, 0.5)' }}
                        >
                          <div className="d-flex justify-content-between align-items-center">
                            <span className="badge bg-info text-dark">{evt.status}</span>
                            <span className="small text-secondary">
                              {new Date(evt.recordedAt).toLocaleString()}
                            </span>
                          </div>
                          {evt.locationName && (
                            <div className="small text-light mt-1 d-flex align-items-center gap-1">
                              <MapPin size={12} className="text-info" />
                              {evt.locationName}
                            </div>
                          )}
                          {evt.description && (
                            <div className="small text-secondary mt-1">{evt.description}</div>
                          )}
                          {evt.recordedByEmail && (
                            <div className="small text-secondary opacity-75 mt-1">
                              Actor: {evt.recordedByEmail}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="modal-footer border-top border-secondary border-opacity-25">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={onClose}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
