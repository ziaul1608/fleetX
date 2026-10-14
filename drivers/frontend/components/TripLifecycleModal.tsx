import React, { useEffect, useState } from 'react';
import { Play, CheckCircle2, XCircle, ArrowRight, Clock, History, AlertCircle } from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Trip, TripStatusHistory } from '../types';
import { Badge } from '../../../shared/components/Badge';

interface TripLifecycleModalProps {
  trip: Trip;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const TripLifecycleModal: React.FC<TripLifecycleModalProps> = ({ trip, isOpen, onClose, onSuccess }) => {
  const [history, setHistory] = useState<TripStatusHistory[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && trip) {
      setLoadingHistory(true);
      setError(null);
      setRemarks('');
      driverApi
        .getTripHistory(trip.id)
        .then((data) => setHistory(data))
        .catch(() => setHistory([]))
        .finally(() => setLoadingHistory(false));
    }
  }, [isOpen, trip]);

  if (!isOpen) return null;

  const handleStart = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await driverApi.startTrip(trip.id, remarks || 'Departed origin depot on schedule.');
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start trip.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleComplete = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await driverApi.completeTrip(trip.id, remarks || 'Consignment safely delivered at destination.');
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to complete trip.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm(`Are you sure you want to cancel trip ${trip.tripCode}? Assigned vehicle and driver will be released immediately.`)) {
      return;
    }

    setActionLoading(true);
    setError(null);
    try {
      await driverApi.cancelTrip(trip.id, remarks || 'Cancelled by dispatcher.');
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to cancel trip.');
    } finally {
      setActionLoading(false);
    }
  };

  const isTerminal = trip.status === 'COMPLETED' || trip.status === 'CANCELLED';

  return (
    <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content shadow-lg border-0">
          <div className="modal-header border-bottom py-3">
            <div className="d-flex align-items-center gap-2">
              <h5 className="modal-title fw-bold text-slate-900 mb-0">Trip Lifecycle: {trip.tripCode}</h5>
              <Badge status={trip.status} />
            </div>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} disabled={actionLoading} />
          </div>

          <div className="modal-body p-4">
            {error && (
              <div className="alert alert-danger d-flex align-items-center gap-2 py-2 px-3 small mb-3">
                <AlertCircle size={16} /> {error}
              </div>
            )}

            {/* Route & Asset Overview Card */}
            <div className="p-3 bg-light rounded border mb-3">
              <div className="d-flex align-items-center justify-content-between mb-2">
                <div className="d-flex align-items-center gap-2 fw-semibold text-slate-900">
                  <span>{trip.originAddress}</span>
                  <ArrowRight size={16} className="text-primary" />
                  <span>{trip.destinationAddress}</span>
                </div>
                {trip.distanceKm && <span className="badge bg-secondary-subtle text-secondary">{trip.distanceKm} km</span>}
              </div>
              <div className="row g-2 small text-muted pt-2 border-top">
                <div className="col-md-6">
                  <strong>Assigned Vehicle: </strong>
                  <code>{trip.vehicleLicensePlate}</code> ({trip.vehicleMakeModel})
                </div>
                <div className="col-md-6">
                  <strong>Assigned Driver: </strong>
                  {trip.driverName} ({trip.driverPhone})
                </div>
                <div className="col-md-6">
                  <strong>Scheduled Departure: </strong>
                  {new Date(trip.scheduledStartTime).toLocaleString()}
                </div>
                <div className="col-md-6">
                  <strong>Scheduled Arrival: </strong>
                  {trip.scheduledEndTime ? new Date(trip.scheduledEndTime).toLocaleString() : 'Open'}
                </div>
                {trip.actualStartTime && (
                  <div className="col-md-6 text-success">
                    <strong>Actual Departure: </strong>
                    {new Date(trip.actualStartTime).toLocaleString()}
                  </div>
                )}
                {trip.actualEndTime && (
                  <div className="col-md-6 text-success">
                    <strong>Actual Arrival: </strong>
                    {new Date(trip.actualEndTime).toLocaleString()}
                  </div>
                )}
              </div>
            </div>

            {/* Lifecycle Action Buttons */}
            {!isTerminal ? (
              <div className="card border mb-3 p-3">
                <label className="form-label small fw-semibold text-slate-800 mb-1">
                  Transition Remarks / Checkpoint Notes
                </label>
                <input
                  type="text"
                  className="form-control form-control-sm mb-3"
                  placeholder="e.g. Departed toll checkpoint, cargo intact"
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  disabled={actionLoading}
                />

                <div className="d-flex gap-2 justify-content-end">
                  {trip.status === 'ASSIGNED' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-success d-flex align-items-center gap-2"
                      onClick={handleStart}
                      disabled={actionLoading}
                    >
                      <Play size={16} /> Start Trip (Depart Origin)
                    </button>
                  )}

                  {trip.status === 'IN_PROGRESS' && (
                    <button
                      type="button"
                      className="btn btn-sm btn-success d-flex align-items-center gap-2"
                      onClick={handleComplete}
                      disabled={actionLoading}
                    >
                      <CheckCircle2 size={16} /> Complete Trip (Mark Arrived)
                    </button>
                  )}

                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger d-flex align-items-center gap-2"
                    onClick={handleCancel}
                    disabled={actionLoading}
                  >
                    <XCircle size={16} /> Cancel Trip
                  </button>
                </div>
              </div>
            ) : (
              <div className="alert alert-secondary py-2 px-3 small mb-3">
                <strong>Terminal Lifecycle State:</strong> This trip has concluded as <code>{trip.status}</code>. Its allocated resources have been automatically freed back to the active fleet pool.
              </div>
            )}

            {/* Status History Timeline */}
            <div className="card border">
              <div className="card-header bg-white py-2 px-3 fw-bold small text-slate-800 d-flex align-items-center gap-2">
                <History size={16} className="text-primary" /> Lifecycle Audit History
              </div>
              <div className="card-body p-3">
                {loadingHistory ? (
                  <div className="text-center py-2 text-muted small">
                    <span className="spinner-border spinner-border-sm me-2" /> Loading audit history...
                  </div>
                ) : history.length === 0 ? (
                  <div className="text-muted small text-center py-2">No transition events logged yet.</div>
                ) : (
                  <ul className="list-group list-group-flush small">
                    {history.map((h) => (
                      <li key={h.id} className="list-group-item px-0 py-2 d-flex justify-content-between align-items-start">
                        <div>
                          <div className="fw-medium text-slate-800">
                            {h.fromStatus ? `${h.fromStatus} → ${h.toStatus}` : `Created as ${h.toStatus}`}
                          </div>
                          {h.remarks && <div className="text-muted small">{h.remarks}</div>}
                        </div>
                        <span className="text-muted small d-flex align-items-center gap-1">
                          <Clock size={12} /> {new Date(h.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>

          <div className="modal-footer border-top py-2 px-4 bg-light">
            <button type="button" className="btn btn-sm btn-secondary" onClick={onClose} disabled={actionLoading}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
