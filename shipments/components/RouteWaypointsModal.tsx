import React, { useEffect, useState } from 'react';
import { Compass, Info, Plus, Trash2, X } from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import {
  RoutePlanResponseDto,
  Shipment,
  StopType,
  WaypointItemRequest,
} from '../types';

interface RouteWaypointsModalProps {
  shipment: Shipment | null;
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
}

const DEFAULT_WAYPOINTS: WaypointItemRequest[] = [
  {
    sequenceOrder: 1,
    locationName: 'Mumbai Port Hub',
    latitude: 19.076,
    longitude: 72.8777,
    stopType: 'ORIGIN',
  },
  {
    sequenceOrder: 2,
    locationName: 'Lonavala Transit Checkpoint',
    latitude: 18.7546,
    longitude: 73.4062,
    stopType: 'INTERMEDIATE',
  },
  {
    sequenceOrder: 3,
    locationName: 'Pune Chakan Distribution Center',
    latitude: 18.5204,
    longitude: 73.8567,
    stopType: 'DESTINATION',
  },
];

export const RouteWaypointsModal: React.FC<RouteWaypointsModalProps> = ({
  shipment,
  isOpen,
  onClose,
  onSaved,
}) => {
  const [waypoints, setWaypoints] = useState<WaypointItemRequest[]>(DEFAULT_WAYPOINTS);
  const [averageSpeedKmh, setAverageSpeedKmh] = useState<number>(50);
  const [stopAllowanceMinutes, setStopAllowanceMinutes] = useState<number>(15);
  const [routePlan, setRoutePlan] = useState<RoutePlanResponseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && shipment) {
      setError(null);
      shipmentApi
        .getWaypoints(shipment.id)
        .then((plan) => {
          setRoutePlan(plan);
          if (plan.waypoints && plan.waypoints.length > 0) {
            setWaypoints(
              plan.waypoints.map((wp, idx) => ({
                sequenceOrder: idx + 1,
                locationName: wp.locationName,
                latitude: wp.latitude,
                longitude: wp.longitude,
                stopType: wp.stopType,
              }))
            );
          } else {
            setWaypoints([
              {
                sequenceOrder: 1,
                locationName: shipment.originCity || 'Origin Stop',
                latitude: 19.076,
                longitude: 72.8777,
                stopType: 'ORIGIN',
              },
              {
                sequenceOrder: 2,
                locationName: shipment.destinationCity || 'Destination Stop',
                latitude: 18.5204,
                longitude: 73.8567,
                stopType: 'DESTINATION',
              },
            ]);
          }
        })
        .catch(() => {
          setRoutePlan(null);
        });
    }
  }, [isOpen, shipment]);

  if (!isOpen || !shipment) return null;

  const handleAddStop = () => {
    const nextOrder = waypoints.length + 1;
    setWaypoints([
      ...waypoints,
      {
        sequenceOrder: nextOrder,
        locationName: `Stop #${nextOrder}`,
        latitude: 18.65,
        longitude: 73.5,
        stopType: 'INTERMEDIATE',
      },
    ]);
  };

  const handleRemoveStop = (index: number) => {
    const updated = waypoints
      .filter((_, idx) => idx !== index)
      .map((wp, idx) => ({ ...wp, sequenceOrder: idx + 1 }));
    setWaypoints(updated);
  };

  const handleWaypointChange = (
    index: number,
    field: keyof WaypointItemRequest,
    value: string | number
  ) => {
    const updated = [...waypoints];
    updated[index] = {
      ...updated[index],
      [field]: value,
    };
    setWaypoints(updated);
  };

  const handleSaveRoute = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (waypoints.length < 2) {
      setError('At least 2 waypoints (Origin and Destination) are required to calculate a route.');
      return;
    }

    for (const wp of waypoints) {
      if (wp.latitude < -90 || wp.latitude > 90) {
        setError(`Invalid latitude ${wp.latitude} for "${wp.locationName}". Must be between -90 and 90.`);
        return;
      }
      if (wp.longitude < -180 || wp.longitude > 180) {
        setError(`Invalid longitude ${wp.longitude} for "${wp.locationName}". Must be between -180 and 180.`);
        return;
      }
    }

    setLoading(true);
    try {
      const normalized = waypoints.map((wp, idx) => ({
        ...wp,
        sequenceOrder: idx + 1,
        stopType:
          idx === 0
            ? ('ORIGIN' as StopType)
            : idx === waypoints.length - 1
            ? ('DESTINATION' as StopType)
            : ('INTERMEDIATE' as StopType),
      }));
      const savedPlan = await shipmentApi.saveWaypoints(shipment.id, {
        waypoints: normalized,
        averageSpeedKmh,
        stopAllowanceMinutes,
      });
      setRoutePlan(savedPlan);
      onSaved();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to save route waypoints.'
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
              <Compass size={20} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">
                Route Waypoints & Distance Estimator — {shipment.trackingNumber}
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

          <form onSubmit={handleSaveRoute}>
            <div className="modal-body">
              <div
                className="alert alert-info py-2 small d-flex align-items-center gap-2"
                role="note"
              >
                <Info size={16} />
                <div>
                  <strong>Estimation Notice:</strong> Waypoint distances are calculated using the{' '}
                  <strong>Haversine straight-line formula</strong> (great-circle estimate, not exact
                  turn-by-turn road distance). Journey duration and ETA are{' '}
                  <strong>estimated values</strong> based on configurable constant speed and stop
                  allowances, not live traffic predictions.
                </div>
              </div>

              {error && (
                <div className="alert alert-danger py-2 small" role="alert">
                  {error}
                </div>
              )}

              <div className="row g-3 mb-3">
                <div className="col-md-4">
                  <label className="form-label small text-secondary">
                    Assumed Average Speed (km/h)
                  </label>
                  <input
                    type="number"
                    min="5"
                    max="130"
                    step="1"
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={averageSpeedKmh}
                    onChange={(e) => setAverageSpeedKmh(Number(e.target.value))}
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label small text-secondary">
                    Intermediate Stop Allowance (minutes/stop)
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="240"
                    step="5"
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={stopAllowanceMinutes}
                    onChange={(e) => setStopAllowanceMinutes(Number(e.target.value))}
                  />
                </div>
                <div className="col-md-4 d-flex align-items-end">
                  <button
                    type="button"
                    className="btn btn-outline-info btn-sm w-100 d-flex align-items-center justify-content-center gap-1"
                    onClick={handleAddStop}
                  >
                    <Plus size={15} /> Add Waypoint Stop
                  </button>
                </div>
              </div>

              {/* Waypoint Rows */}
              <div className="table-responsive mb-3">
                <table className="table table-dark table-sm align-middle mb-0">
                  <thead>
                    <tr className="text-secondary small">
                      <th>#</th>
                      <th>Role</th>
                      <th>Stop Label / Location Name</th>
                      <th>Latitude (-90 to 90)</th>
                      <th>Longitude (-180 to 180)</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {waypoints.map((wp, idx) => (
                      <tr key={idx}>
                        <td className="fw-bold text-info">{idx + 1}</td>
                        <td>
                          <span className="badge bg-secondary">
                            {idx === 0
                              ? 'ORIGIN'
                              : idx === waypoints.length - 1
                              ? 'DESTINATION'
                              : 'INTERMEDIATE'}
                          </span>
                        </td>
                        <td>
                          <input
                            type="text"
                            required
                            className="form-control form-control-sm bg-dark text-light border-secondary"
                            value={wp.locationName}
                            onChange={(e) =>
                              handleWaypointChange(idx, 'locationName', e.target.value)
                            }
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            step="any"
                            required
                            className="form-control form-control-sm bg-dark text-light border-secondary"
                            value={wp.latitude}
                            onChange={(e) =>
                              handleWaypointChange(idx, 'latitude', Number(e.target.value))
                            }
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            step="any"
                            required
                            className="form-control form-control-sm bg-dark text-light border-secondary"
                            value={wp.longitude}
                            onChange={(e) =>
                              handleWaypointChange(idx, 'longitude', Number(e.target.value))
                            }
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-danger"
                            disabled={waypoints.length <= 2}
                            onClick={() => handleRemoveStop(idx)}
                            title="Remove Stop"
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Computed Route Estimation Summary */}
              {routePlan && (
                <div
                  className="p-3 rounded-3 border"
                  style={{
                    background: 'rgba(15, 23, 42, 0.75)',
                    borderColor: 'rgba(56, 189, 248, 0.25)',
                  }}
                >
                  <div className="row g-3">
                    <div className="col-md-4">
                      <div className="small text-secondary">
                        Estimated Straight-Line Distance (Haversine)
                      </div>
                      <div className="fs-5 fw-bold text-info">
                        {routePlan.totalEstimatedDistanceKm} km
                      </div>
                      <div className="small text-secondary opacity-75">
                        {routePlan.distanceEstimationMethod}
                      </div>
                    </div>
                    <div className="col-md-4">
                      <div className="small text-secondary">Estimated Journey Duration</div>
                      <div className="fs-5 fw-bold text-light">
                        {routePlan.estimatedDurationMinutes} mins
                      </div>
                      <div className="small text-secondary opacity-75">
                        @ {routePlan.assumedAverageSpeedKmh} km/h +{' '}
                        {routePlan.stopAllowanceMinutesPerIntermediateStop}m/stop
                      </div>
                    </div>
                    <div className="col-md-4">
                      <div className="small text-secondary">Estimated Arrival Time (ETA)</div>
                      <div className="fs-6 fw-bold text-warning">
                        {routePlan.estimatedArrivalTime
                          ? new Date(routePlan.estimatedArrivalTime).toLocaleString()
                          : 'N/A'}
                      </div>
                      <div className="small text-secondary opacity-75">
                        {routePlan.etaEstimationMethod}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer border-top border-secondary border-opacity-25">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={onClose}
              >
                Close
              </button>
              <button
                type="submit"
                className="btn btn-info btn-sm text-dark fw-semibold"
                disabled={loading}
              >
                {loading ? 'Calculating & Saving...' : 'Save Waypoints & Compute Estimates'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
