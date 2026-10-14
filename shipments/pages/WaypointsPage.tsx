import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  Compass,
  Info,
  MapPin,
  Plus,
  Save,
  Trash2,
} from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import {
  RoutePlanResponseDto,
  Shipment,
  StopType,
  WaypointItemRequest,
} from '../types';

export const WaypointsPage: React.FC = () => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [selectedShipmentId, setSelectedShipmentId] = useState<number | null>(null);
  const [waypoints, setWaypoints] = useState<WaypointItemRequest[]>([]);
  const [routePlan, setRoutePlan] = useState<RoutePlanResponseDto | null>(null);
  const [averageSpeedKmh, setAverageSpeedKmh] = useState<number>(50);
  const [stopAllowanceMinutes, setStopAllowanceMinutes] = useState<number>(15);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    shipmentApi
      .getShipments({ page: 0, size: 50 })
      .then((pageData) => {
        const list = pageData?.content ?? [];
        setShipments(list);
        if (list.length > 0) {
          setSelectedShipmentId(list[0].id);
        }
      })
      .catch((err: any) => {
        setError(
          err?.response?.data?.message ||
            err?.message ||
            'Failed to load shipments for route planning.'
        );
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedShipmentId) return;
    setError(null);
    setSuccessMessage(null);

    shipmentApi
      .getWaypoints(selectedShipmentId, {
        averageSpeedKmh,
        stopAllowanceMinutes,
      })
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
          const current = shipments.find((s) => s.id === selectedShipmentId);
          setWaypoints([
            {
              sequenceOrder: 1,
              locationName: current?.originCity || 'Mumbai Logistics Hub',
              latitude: 19.076,
              longitude: 72.8777,
              stopType: 'ORIGIN',
            },
            {
              sequenceOrder: 2,
              locationName: current?.destinationCity || 'Pune Distribution Center',
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
  }, [selectedShipmentId, shipments, averageSpeedKmh, stopAllowanceMinutes]);

  const handleAddStop = () => {
    const nextOrder = waypoints.length + 1;
    setWaypoints([
      ...waypoints,
      {
        sequenceOrder: nextOrder,
        locationName: `Transit Checkpoint #${nextOrder}`,
        latitude: 18.7546,
        longitude: 73.4062,
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

  const handleFieldChange = (
    index: number,
    field: keyof WaypointItemRequest,
    value: string | number
  ) => {
    const copy = [...waypoints];
    copy[index] = {
      ...copy[index],
      [field]: value,
    };
    setWaypoints(copy);
  };

  const handleSaveWaypoints = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedShipmentId) return;
    setError(null);
    setSuccessMessage(null);

    if (waypoints.length < 2) {
      setError('A valid route plan requires at least 2 waypoints (Origin and Destination).');
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

    setSaving(true);
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
      const updatedPlan = await shipmentApi.saveWaypoints(selectedShipmentId, {
        waypoints: normalized,
        averageSpeedKmh,
        stopAllowanceMinutes,
      });
      setRoutePlan(updatedPlan);
      setSuccessMessage(
        'Route waypoints saved and Haversine straight-line distance / ETA estimates updated.'
      );
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to save route waypoints.'
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="container-fluid py-3 text-light">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2">
            <Compass size={24} className="text-info" />
            <h2 className="h4 fw-bold mb-0">
              Route Planning & Waypoint Estimator
            </h2>
          </div>
          <p className="text-secondary small mb-0 mt-1">
            Configure ordered waypoints and compute Haversine straight-line distance and constant-speed ETA estimates
          </p>
        </div>

        <Link
          to="/shipments"
          className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1"
        >
          <ArrowLeft size={15} /> Back to Shipment Manifests
        </Link>
      </div>

      {/* Prominent Estimation Disclaimer Banner */}
      <div
        className="alert alert-info d-flex align-items-start gap-2 small mb-4"
        role="note"
      >
        <Info size={18} className="mt-1 flex-shrink-0" />
        <div>
          <strong>Methodology & Estimation Disclaimer:</strong> Distance between ordered
          waypoints is calculated using the{' '}
          <strong>Haversine Great-Circle Formula (R = 6371.0 km)</strong> and
          represents an <strong>Estimated Straight-Line Distance</strong>, not exact
          turn-by-turn road mileage. Journey duration and stop arrival times are{' '}
          <strong>Estimated ETAs</strong> derived from the configured constant average
          speed and per-stop dwell allowance, not live traffic telemetry.
        </div>
      </div>

      {error && (
        <div className="alert alert-danger py-2 small mb-3" role="alert">
          {error}
        </div>
      )}
      {successMessage && (
        <div className="alert alert-success py-2 small mb-3" role="status">
          {successMessage}
        </div>
      )}

      {/* Shipment & Estimation Parameter Controls */}
      <div
        className="p-3 rounded-3 border mb-4"
        style={{
          background: 'rgba(13, 20, 38, 0.82)',
          borderColor: 'rgba(56, 189, 248, 0.24)',
        }}
      >
        <div className="row g-3 align-items-end">
          <div className="col-md-5">
            <label className="form-label small text-secondary">
              Select Shipment Manifest
            </label>
            <select
              className="form-select bg-dark text-light border-secondary"
              aria-label="Select Shipment Manifest"
              value={selectedShipmentId ?? ''}
              onChange={(e) =>
                setSelectedShipmentId(e.target.value ? Number(e.target.value) : null)
              }
              disabled={loading || shipments.length === 0}
            >
              {shipments.length === 0 ? (
                <option value="">No shipments available</option>
              ) : (
                shipments.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.trackingNumber} — {s.originCity} → {s.destinationCity} [{s.status}]
                  </option>
                ))
              )}
            </select>
          </div>

          <div className="col-md-3">
            <label className="form-label small text-secondary">
              Assumed Constant Speed (km/h)
            </label>
            <input
              type="number"
              min="5"
              max="130"
              className="form-control bg-dark text-light border-secondary"
              value={averageSpeedKmh}
              onChange={(e) => setAverageSpeedKmh(Number(e.target.value))}
            />
          </div>

          <div className="col-md-2">
            <label className="form-label small text-secondary">
              Stop Allowance (mins)
            </label>
            <input
              type="number"
              min="0"
              max="240"
              className="form-control bg-dark text-light border-secondary"
              value={stopAllowanceMinutes}
              onChange={(e) => setStopAllowanceMinutes(Number(e.target.value))}
            />
          </div>

          <div className="col-md-2">
            <button
              type="button"
              className="btn btn-outline-info w-100 d-inline-flex align-items-center justify-content-center gap-1"
              onClick={handleAddStop}
              disabled={!selectedShipmentId}
            >
              <Plus size={16} /> Add Stop
            </button>
          </div>
        </div>
      </div>

      {/* Route Summary Cards */}
      {routePlan && (
        <div className="row g-3 mb-4">
          <div className="col-md-4">
            <div
              className="p-3 rounded-3 border h-100"
              style={{
                background: 'rgba(13, 20, 38, 0.78)',
                borderColor: 'rgba(56, 189, 248, 0.25)',
              }}
            >
              <div className="small text-secondary">
                Total Estimated Straight-Line Distance
              </div>
              <div className="fs-3 fw-bold text-info mt-1">
                {routePlan.totalEstimatedDistanceKm} km
              </div>
              <div className="small text-secondary opacity-75">
                {routePlan.distanceEstimationMethod}
              </div>
            </div>
          </div>

          <div className="col-md-4">
            <div
              className="p-3 rounded-3 border h-100"
              style={{
                background: 'rgba(13, 20, 38, 0.78)',
                borderColor: 'rgba(56, 189, 248, 0.25)',
              }}
            >
              <div className="small text-secondary">Estimated Total Journey Duration</div>
              <div className="fs-3 fw-bold text-light mt-1">
                {routePlan.estimatedDurationMinutes} mins
              </div>
              <div className="small text-secondary opacity-75">
                Assumed {routePlan.assumedAverageSpeedKmh} km/h +{' '}
                {routePlan.stopAllowanceMinutesPerIntermediateStop} min/intermediate stop
              </div>
            </div>
          </div>

          <div className="col-md-4">
            <div
              className="p-3 rounded-3 border h-100"
              style={{
                background: 'rgba(13, 20, 38, 0.78)',
                borderColor: 'rgba(245, 158, 11, 0.3)',
              }}
            >
              <div className="small text-secondary">Estimated Destination ETA</div>
              <div className="fs-5 fw-bold text-warning mt-1">
                {routePlan.estimatedArrivalTime
                  ? new Date(routePlan.estimatedArrivalTime).toLocaleString()
                  : 'Save waypoints to compute'}
              </div>
              <div className="small text-secondary opacity-75">
                {routePlan.etaEstimationMethod}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Interactive Waypoints Editor Form */}
      <form onSubmit={handleSaveWaypoints}>
        <div
          className="p-3 rounded-3 border mb-4"
          style={{
            background: 'rgba(13, 20, 38, 0.82)',
            borderColor: 'rgba(148, 163, 184, 0.16)',
          }}
        >
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="h6 fw-bold mb-0 d-flex align-items-center gap-2">
              <MapPin size={18} className="text-info" /> Ordered Route Waypoints
            </h5>
            <button
              type="submit"
              className="btn btn-info btn-sm text-dark fw-semibold d-inline-flex align-items-center gap-1"
              disabled={saving || !selectedShipmentId}
            >
              <Save size={15} />{' '}
              {saving ? 'Computing...' : 'Save Waypoints & Calculate Estimates'}
            </button>
          </div>

          <div className="table-responsive">
            <table className="table table-dark table-sm align-middle mb-0">
              <thead>
                <tr className="text-secondary small">
                  <th>Seq #</th>
                  <th>Stop Role</th>
                  <th>Stop Label / Location</th>
                  <th>Latitude</th>
                  <th>Longitude</th>
                  <th>Est. Leg Distance</th>
                  <th>Cumulative Est.</th>
                  <th>Remove</th>
                </tr>
              </thead>
              <tbody>
                {waypoints.map((wp, idx) => {
                  const computedWp = routePlan?.waypoints?.[idx];
                  return (
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
                            handleFieldChange(idx, 'locationName', e.target.value)
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
                            handleFieldChange(idx, 'latitude', Number(e.target.value))
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
                            handleFieldChange(idx, 'longitude', Number(e.target.value))
                          }
                        />
                      </td>
                      <td className="text-info small">
                        {computedWp ? `~${computedWp.legDistanceKm} km` : '—'}
                      </td>
                      <td className="text-light small">
                        {computedWp ? `~${computedWp.cumulativeDistanceKm} km` : '—'}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger"
                          disabled={waypoints.length <= 2}
                          onClick={() => handleRemoveStop(idx)}
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </form>
    </div>
  );
};
