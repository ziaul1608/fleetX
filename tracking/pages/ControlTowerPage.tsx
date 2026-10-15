import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  Box,
  CheckCircle2,
  Compass,
  FastForward,
  FileSpreadsheet,
  Fuel,
  MapPin,
  Navigation,
  Package,
  Pause,
  Play,
  Plus,
  Radio,
  RefreshCw,
  Shield,
  Square,
  Truck,
  Wrench,
} from 'lucide-react';
import { trackingApi } from '../api/trackingApi';
import {
  AlertItem,
  ControlTowerSummary,
  GeofenceItem,
  SimulatorSession,
  VehicleOption,
} from '../types';
import { LeafletFleetMap } from '../components/LeafletFleetMap';
import { ThreeFleetScene } from '../components/ThreeFleetScene';
import { CreateGeofenceModal } from '../components/CreateGeofenceModal';

export const ControlTowerPage: React.FC = () => {
  const [summary, setSummary] = useState<ControlTowerSummary | null>(null);
  const [sessions, setSessions] = useState<SimulatorSession[]>([]);
  const [geofences, setGeofences] = useState<GeofenceItem[]>([]);
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [vehicles, setVehicles] = useState<VehicleOption[]>([]);

  const [selectedVehicleId, setSelectedVehicleId] = useState<number | null>(null);
  const [targetSimVehicleId, setTargetSimVehicleId] = useState<string>('');
  const [speedMultiplier, setSpeedMultiplier] = useState<number>(2);
  const [tickIntervalSeconds, setTickIntervalSeconds] = useState<number>(2);
  const [activeView, setActiveView] = useState<'2d' | '3d' | 'list'>('2d');

  // Alert filter states
  const [alertSeverityFilter, setAlertSeverityFilter] = useState<string>('');
  const [alertAckFilter, setAlertAckFilter] = useState<string>('UNACKNOWLEDGED');
  const [isGeofenceModalOpen, setIsGeofenceModalOpen] = useState<boolean>(false);

  const [loading, setLoading] = useState<boolean>(true);
  const [actionBusy, setActionBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadControlTowerData = useCallback(async () => {
    try {
      const [sumData, sessData, geoData, alertPage] = await Promise.all([
        trackingApi.getControlTowerSummary(),
        trackingApi.getSimulatorSessions(),
        trackingApi.getGeofences(),
        trackingApi.getAlerts({
          severity: alertSeverityFilter || undefined,
          acknowledged:
            alertAckFilter === 'ACKNOWLEDGED'
              ? true
              : alertAckFilter === 'UNACKNOWLEDGED'
              ? false
              : undefined,
          page: 0,
          size: 25,
        }),
      ]);

      setSummary(sumData);
      setSessions(sessData);
      setGeofences(geoData);
      setAlerts(alertPage?.content ?? []);

      if (sessData.length > 0 && selectedVehicleId === null) {
        setSelectedVehicleId(sessData[0].vehicleId);
      }
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to synchronize Control Tower telemetry.'
      );
    } finally {
      setLoading(false);
    }
  }, [alertSeverityFilter, alertAckFilter, selectedVehicleId]);

  useEffect(() => {
    loadControlTowerData();
    trackingApi
      .getEligibleVehicles()
      .then((list) => {
        setVehicles(list);
        if (list.length > 0) {
          setTargetSimVehicleId(String(list[0].id));
        }
      })
      .catch(() => setVehicles([]));
  }, [loadControlTowerData]);

  // Poll live simulator telemetry every 3 seconds when any simulator is running
  useEffect(() => {
    const hasRunning = sessions.some((s) => s.simulatorStatus === 'RUNNING');
    if (!hasRunning) return;
    const timer = setInterval(() => {
      loadControlTowerData();
    }, 3000);
    return () => clearInterval(timer);
  }, [sessions, loadControlTowerData]);

  const selectedSession =
    sessions.find((s) => s.vehicleId === selectedVehicleId) || sessions[0] || null;

  const handleStartSimulator = async () => {
    const vid = targetSimVehicleId
      ? Number(targetSimVehicleId)
      : selectedSession?.vehicleId;
    if (!vid) {
      setError('Select a vehicle to start simulated GPS tracking.');
      return;
    }
    setError(null);
    setActionBusy(true);
    try {
      const started = await trackingApi.startSimulation({
        vehicleId: vid,
        tickIntervalSeconds,
        speedMultiplier,
        baseSpeedKmh: 65,
      });
      setSelectedVehicleId(started.vehicleId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to start GPS simulator.'
      );
    } finally {
      setActionBusy(false);
    }
  };

  const handlePauseSimulator = async (vehicleId: number) => {
    setActionBusy(true);
    setError(null);
    try {
      await trackingApi.pauseSimulation(vehicleId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to pause simulator.');
    } finally {
      setActionBusy(false);
    }
  };

  const handleResumeSimulator = async (vehicleId: number) => {
    setActionBusy(true);
    setError(null);
    try {
      await trackingApi.resumeSimulation(vehicleId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to resume simulator.');
    } finally {
      setActionBusy(false);
    }
  };

  const handleStopSimulator = async (vehicleId: number) => {
    setActionBusy(true);
    setError(null);
    try {
      await trackingApi.stopSimulation(vehicleId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to stop simulator.');
    } finally {
      setActionBusy(false);
    }
  };

  const handleStepSimulator = async (vehicleId: number) => {
    setActionBusy(true);
    setError(null);
    try {
      await trackingApi.stepSimulation(vehicleId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to step simulator.');
    } finally {
      setActionBusy(false);
    }
  };

  const handleAcknowledgeAlert = async (alertId: number) => {
    setError(null);
    try {
      await trackingApi.acknowledgeAlert(alertId);
      await loadControlTowerData();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          'Insufficient permissions or error acknowledging alert.'
      );
    }
  };

  const handleToggleGeofence = async (fence: GeofenceItem) => {
    try {
      await trackingApi.toggleGeofence(fence.id, !fence.active);
      await loadControlTowerData();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to toggle geofence.');
    }
  };

  return (
    <div className="container-fluid py-3 text-light">
      {/* Header & View Mode Switcher */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
        <div>
          <div className="d-flex align-items-center gap-2">
            <Radio size={24} className="text-info" />
            <h2 className="h4 fw-bold mb-0">
              Operational Control Tower &amp; Live Fleet Telemetry
            </h2>
            <span className="badge bg-warning text-dark fw-bold ms-1">
              SIMULATED DATA
            </span>
          </div>
          <p className="text-secondary small mb-0 mt-1">
            {summary?.simulatedDataDisclaimer ||
              'SIMULATED DATA — Telemetry coordinates and vehicle motions are generated by the software simulator along route waypoints.'}
          </p>
        </div>

        <div className="d-flex flex-wrap align-items-center gap-2">
          <div className="btn-group btn-group-sm" role="group" aria-label="Visualization Mode">
            <button
              type="button"
              className={`btn ${
                activeView === '2d' ? 'btn-info text-dark fw-semibold' : 'btn-outline-secondary text-light'
              }`}
              onClick={() => setActiveView('2d')}
            >
              <MapPin size={14} className="me-1" /> 2D Live Map
            </button>
            <button
              type="button"
              className={`btn ${
                activeView === '3d' ? 'btn-info text-dark fw-semibold' : 'btn-outline-secondary text-light'
              }`}
              onClick={() => setActiveView('3d')}
            >
              <Box size={14} className="me-1" /> 3D Fleet Canvas
            </button>
            <button
              type="button"
              className={`btn ${
                activeView === 'list' ? 'btn-info text-dark fw-semibold' : 'btn-outline-secondary text-light'
              }`}
              onClick={() => setActiveView('list')}
            >
              <Activity size={14} className="me-1" /> Telemetry Table
            </button>
          </div>

          <Link
            to="/tracking/reports"
            className="btn btn-outline-info btn-sm d-inline-flex align-items-center gap-1"
          >
            <FileSpreadsheet size={15} /> Reports &amp; CSV Export
          </Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger py-2 small mb-3" role="alert">
          {error}
        </div>
      )}

      {/* Fleet-Wide KPI Cards */}
      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.82)',
              borderColor: 'rgba(56, 189, 248, 0.25)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Active Vehicles</span>
              <Truck size={18} className="text-info" />
            </div>
            <div className="fs-3 fw-bold mt-1">
              {summary?.totalVehicles ?? 0}{' '}
              <span className="fs-6 fw-normal text-secondary">total</span>
            </div>
            <div className="small text-secondary mt-1">
              <span className="text-info">{summary?.activeVehiclesCount ?? 0} active</span> ·{' '}
              <span>{summary?.idleVehiclesCount ?? 0} idle</span> ·{' '}
              <span className="text-warning">{summary?.vehiclesInMaintenance ?? 0} maint</span> ·{' '}
              <span>{summary?.offlineVehiclesCount ?? 0} offline</span>
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.82)',
              borderColor: 'rgba(56, 189, 248, 0.25)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Trips In Transit</span>
              <Navigation size={18} className="text-primary" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-info">
              {summary?.ongoingTripsCount ?? 0}{' '}
              <span className="fs-6 fw-normal text-secondary">active trips</span>
            </div>
            <div className="small text-secondary mt-1">
              <span>{summary?.tripsCompletedToday ?? 0} completed today</span> ·{' '}
              <span>{summary?.totalShipmentsCount ?? 0} shipments</span> (
              <span className="text-danger">{summary?.delayedShipmentsCount ?? 0} delayed</span>)
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.82)',
              borderColor: 'rgba(16, 185, 129, 0.28)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Fleet Fuel Avg</span>
              <Fuel size={18} className="text-success" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-success">
              {summary?.fleetFuelEfficiencyAvg
                ? `${summary.fleetFuelEfficiencyAvg.toFixed(2)} km/L`
                : '0.00 km/L'}
            </div>
            <div className="small text-secondary mt-1">
              Fuel: ₹{(summary?.totalFuelCost ?? 0).toLocaleString()} · Maint: ₹
              {(summary?.totalMaintenanceCost ?? 0).toLocaleString()} (
              <Wrench size={12} className="text-warning mx-1" />
              {summary?.unresolvedMaintenanceWarningsCount ?? 0} warnings)
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.82)',
              borderColor: 'rgba(239, 68, 68, 0.35)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Active Alerts</span>
              <AlertTriangle size={18} className="text-danger" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-danger">
              {summary?.activeAlertsCount ?? 0}
            </div>
            <div className="small text-secondary mt-1">
              <span className="text-danger fw-semibold">
                {summary?.criticalAlertsCount ?? 0} critical
              </span>{' '}
              · {summary?.totalAlertsCount ?? 0} total logged alerts
            </div>
          </div>
        </div>
      </div>

      {/* Simulator Control Bar */}
      <div
        className="p-3 rounded-3 border mb-4"
        style={{
          background: 'rgba(13, 20, 38, 0.88)',
          borderColor: 'rgba(56, 189, 248, 0.28)',
        }}
      >
        <div className="row g-3 align-items-center">
          <div className="col-lg-4">
            <div className="d-flex align-items-center gap-2">
              <Compass size={18} className="text-info" />
              <span className="fw-semibold small">Simulated GPS Telemetry Engine</span>
              <span className="badge bg-info text-dark">
                {summary?.activeSimulatorsCount ?? 0} Running
              </span>
            </div>
            <div className="mt-2 d-flex gap-2">
              <select
                className="form-select form-select-sm bg-dark text-light border-secondary"
                aria-label="Select Vehicle for Simulation"
                value={targetSimVehicleId}
                onChange={(e) => setTargetSimVehicleId(e.target.value)}
              >
                <option value="">-- Choose Eligible Vehicle --</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.licensePlate} ({v.make} {v.model}) [{v.status}]
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="btn btn-info btn-sm text-dark fw-semibold d-inline-flex align-items-center gap-1 flex-shrink-0"
                disabled={actionBusy}
                onClick={handleStartSimulator}
              >
                <Play size={14} /> Start Simulator
              </button>
            </div>
          </div>

          <div className="col-lg-4">
            <div className="small text-secondary mb-1">
              Selected Vehicle Controls ({selectedSession?.licensePlate || 'None Selected'})
            </div>
            <div className="d-flex flex-wrap gap-2">
              {selectedSession ? (
                <>
                  {selectedSession.simulatorStatus === 'RUNNING' ? (
                    <button
                      type="button"
                      className="btn btn-outline-warning btn-sm d-inline-flex align-items-center gap-1"
                      disabled={actionBusy}
                      onClick={() => handlePauseSimulator(selectedSession.vehicleId)}
                    >
                      <Pause size={14} /> Pause
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-outline-success btn-sm d-inline-flex align-items-center gap-1"
                      disabled={actionBusy}
                      onClick={() => handleResumeSimulator(selectedSession.vehicleId)}
                    >
                      <Play size={14} /> Resume
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn btn-outline-info btn-sm d-inline-flex align-items-center gap-1"
                    disabled={actionBusy}
                    onClick={() => handleStepSimulator(selectedSession.vehicleId)}
                    title="Advance one deterministic waypoint tick"
                  >
                    <FastForward size={14} /> Step Tick
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm d-inline-flex align-items-center gap-1"
                    disabled={actionBusy}
                    onClick={() => handleStopSimulator(selectedSession.vehicleId)}
                  >
                    <Square size={14} /> Stop
                  </button>
                </>
              ) : (
                <span className="text-secondary small">
                  Start a simulator to enable Pause / Resume / Step / Stop controls.
                </span>
              )}
            </div>
          </div>

          <div className="col-lg-4">
            <div className="d-flex flex-wrap justify-content-lg-end align-items-center gap-2">
              <div className="d-flex align-items-center gap-1">
                <span className="small text-secondary">Speed:</span>
                {[1, 2, 5, 10].map((mult) => (
                  <button
                    key={mult}
                    type="button"
                    className={`btn btn-sm py-0 px-2 ${
                      speedMultiplier === mult
                        ? 'btn-info text-dark fw-bold'
                        : 'btn-outline-secondary text-light'
                    }`}
                    onClick={() => setSpeedMultiplier(mult)}
                  >
                    {mult}x
                  </button>
                ))}
              </div>
              <div className="d-flex align-items-center gap-1">
                <span className="small text-secondary">Tick (s):</span>
                <input
                  type="number"
                  min={1}
                  max={60}
                  style={{ width: 58 }}
                  className="form-control form-control-sm bg-dark text-light border-secondary"
                  value={tickIntervalSeconds}
                  onChange={(e) => setTickIntervalSeconds(Number(e.target.value))}
                />
              </div>
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={loadControlTowerData}
                title="Refresh Control Tower"
              >
                <RefreshCw size={14} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Split: Left (2D Map / 3D Scene / Telemetry Table + Selected Vehicle Inspector) | Right (Geofences & Alerts) */}
      <div className="row g-4">
        <div className="col-12 col-xl-8">
          <div
            className="rounded-3 border overflow-hidden mb-4"
            style={{
              background: 'rgba(13, 20, 38, 0.85)',
              borderColor: 'rgba(56, 189, 248, 0.24)',
            }}
          >
            {activeView === '2d' && (
              <LeafletFleetMap
                sessions={sessions}
                selectedVehicleId={selectedSession?.vehicleId ?? null}
                onSelectVehicle={setSelectedVehicleId}
                geofences={geofences}
              />
            )}

            {activeView === '3d' && (
              <ThreeFleetScene
                sessions={sessions}
                selectedVehicleId={selectedSession?.vehicleId ?? null}
                onSelectVehicle={setSelectedVehicleId}
              />
            )}

            {activeView === 'list' && (
              <div className="p-3">
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <h6 className="fw-bold mb-0 text-info">
                    Accessible Simulated Telemetry Stream Table
                  </h6>
                  <span className="badge bg-warning text-dark">SIMULATED DATA</span>
                </div>
                <div className="table-responsive">
                  <table className="table table-dark table-sm align-middle mb-0">
                    <thead>
                      <tr className="text-secondary small">
                        <th>Vehicle</th>
                        <th>Sim State</th>
                        <th>Coordinates (Lat, Lng)</th>
                        <th>Speed</th>
                        <th>Heading</th>
                        <th>Odometer</th>
                        <th>Trip</th>
                        <th>Updated</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sessions.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="text-center py-4 text-secondary">
                            No simulated vehicle telemetry recorded yet.
                          </td>
                        </tr>
                      ) : (
                        sessions.map((s) => (
                          <tr
                            key={s.vehicleId}
                            onClick={() => setSelectedVehicleId(s.vehicleId)}
                            style={{ cursor: 'pointer' }}
                          >
                            <td className="fw-bold text-info">{s.licensePlate}</td>
                            <td>
                              <span className="badge bg-secondary">
                                {s.simulatorStatus}
                              </span>
                            </td>
                            <td>
                              {s.latitude.toFixed(4)}, {s.longitude.toFixed(4)}
                            </td>
                            <td>{s.speedKmh.toFixed(1)} km/h</td>
                            <td>{s.heading.toFixed(0)}°</td>
                            <td>{s.currentOdometerKm.toFixed(1)} km</td>
                            <td>{s.tripCode || 'Unassigned'}</td>
                            <td className="small text-secondary">
                              {new Date(s.lastUpdatedAt).toLocaleTimeString()}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* Selected Vehicle Telemetry & Linked Trip/Shipment Inspector */}
          <div
            className="p-3 rounded-3 border"
            style={{
              background: 'rgba(13, 20, 38, 0.82)',
              borderColor: 'rgba(148, 163, 184, 0.18)',
            }}
          >
            <h6 className="fw-bold text-info mb-3 d-flex align-items-center gap-2">
              <Truck size={17} /> Selected Vehicle Telemetry &amp; Consignment Inspector
            </h6>
            {!selectedSession ? (
              <div className="text-secondary small py-2">
                {loading
                  ? 'Loading telemetry inspector...'
                  : 'Select or start a simulated vehicle to inspect its real-time speed, coordinates, route waypoints, assigned trip, and shipments.'}
              </div>
            ) : (
              <div className="row g-3 small">
                <div className="col-md-3">
                  <span className="text-secondary d-block">Vehicle Asset</span>
                  <strong className="fs-6 text-light">
                    {selectedSession.licensePlate}
                  </strong>
                  <div className="text-secondary">{selectedSession.vehicleMakeModel}</div>
                </div>
                <div className="col-md-3">
                  <span className="text-secondary d-block">Simulated Speed &amp; Heading</span>
                  <strong className="fs-6 text-info">
                    {selectedSession.speedKmh.toFixed(1)} km/h · {selectedSession.heading.toFixed(0)}°
                  </strong>
                  <div className="text-secondary">
                    Odometer: {selectedSession.currentOdometerKm.toFixed(1)} km
                  </div>
                </div>
                <div className="col-md-3">
                  <span className="text-secondary d-block">Current Coordinates</span>
                  <strong className="text-light">
                    {selectedSession.latitude.toFixed(5)}, {selectedSession.longitude.toFixed(5)}
                  </strong>
                  <div className="text-secondary">
                    Updated: {new Date(selectedSession.lastUpdatedAt).toLocaleTimeString()}
                  </div>
                </div>
                <div className="col-md-3">
                  <span className="text-secondary d-block">Assigned Dispatch Trip</span>
                  <strong className="text-light">
                    {selectedSession.tripCode
                      ? `${selectedSession.tripCode} (${selectedSession.tripStatus})`
                      : 'No Active Trip Linked'}
                  </strong>
                  <div className="text-secondary">
                    Segment Progress: {(selectedSession.segmentProgress * 100).toFixed(0)}%
                  </div>
                </div>

                {/* Linked Shipments */}
                <div className="col-12 pt-2 border-top border-secondary border-opacity-25">
                  <span className="text-secondary d-inline-flex align-items-center gap-1 me-2">
                    <Package size={14} className="text-info" /> Linked Shipments:
                  </span>
                  {selectedSession.assignedShipments &&
                  selectedSession.assignedShipments.length > 0 ? (
                    selectedSession.assignedShipments.map((sh) => (
                      <span
                        key={sh.id}
                        className="badge bg-dark border border-info text-info me-2"
                      >
                        {sh.trackingNumber} ({sh.weightKg} kg · {sh.status})
                      </span>
                    ))
                  ) : (
                    <span className="text-secondary">
                      No shipments currently linked to this vehicle&apos;s trip.
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Geofence Boundaries & Operational Incident Alerts */}
        <div className="col-12 col-xl-4 d-flex flex-column gap-4">
          {/* Operational Alerts Card */}
          <div
            className="p-3 rounded-3 border"
            style={{
              background: 'rgba(13, 20, 38, 0.85)',
              borderColor: 'rgba(239, 68, 68, 0.28)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center mb-3">
              <div className="d-flex align-items-center gap-2">
                <AlertTriangle size={18} className="text-warning" />
                <h6 className="fw-bold mb-0">Operational Incident Feed</h6>
              </div>
              <span className="badge bg-danger">
                {summary?.activeAlertsCount ?? 0} Open
              </span>
            </div>

            {/* Alert Filters */}
            <div className="row g-2 mb-3">
              <div className="col-6">
                <select
                  className="form-select form-select-sm bg-dark text-light border-secondary"
                  aria-label="Filter Alerts by Status"
                  value={alertAckFilter}
                  onChange={(e) => setAlertAckFilter(e.target.value)}
                >
                  <option value="UNACKNOWLEDGED">Unacknowledged Only</option>
                  <option value="ACKNOWLEDGED">Acknowledged</option>
                  <option value="ALL">All Alerts</option>
                </select>
              </div>
              <div className="col-6">
                <select
                  className="form-select form-select-sm bg-dark text-light border-secondary"
                  aria-label="Filter Alerts by Severity"
                  value={alertSeverityFilter}
                  onChange={(e) => setAlertSeverityFilter(e.target.value)}
                >
                  <option value="">All Severities</option>
                  <option value="CRITICAL">CRITICAL</option>
                  <option value="WARNING">WARNING</option>
                  <option value="INFO">INFO</option>
                </select>
              </div>
            </div>

            <div
              className="d-flex flex-column gap-2 overflow-y-auto"
              style={{ maxHeight: '300px' }}
            >
              {alerts.length === 0 ? (
                <div className="text-secondary small text-center py-4">
                  No alerts match the selected filter criteria.
                </div>
              ) : (
                alerts.map((a) => (
                  <div
                    key={a.id}
                    className="p-2 rounded border"
                    style={{
                      background: 'rgba(15, 23, 42, 0.78)',
                      borderColor:
                        a.severity === 'CRITICAL'
                          ? 'rgba(239, 68, 68, 0.45)'
                          : 'rgba(148, 163, 184, 0.2)',
                    }}
                  >
                    <div className="d-flex justify-content-between align-items-center">
                      <div className="d-flex align-items-center gap-1">
                        <span
                          className={`badge ${
                            a.severity === 'CRITICAL'
                              ? 'bg-danger'
                              : a.severity === 'WARNING'
                              ? 'bg-warning text-dark'
                              : 'bg-info text-dark'
                          }`}
                        >
                          {a.severity}
                        </span>
                        <span className="badge bg-secondary">{a.alertType}</span>
                      </div>
                      <span className="small text-secondary">
                        {new Date(a.createdAt).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                    <div className="small text-light mt-1">{a.message}</div>
                    <div className="d-flex justify-content-between align-items-center mt-2">
                      <span className="small text-secondary">
                        Linked: {a.entityType} #{a.entityId}
                      </span>
                      {a.acknowledged ? (
                        <span className="small text-success d-inline-flex align-items-center gap-1">
                          <CheckCircle2 size={12} /> Ack by {a.acknowledgedBy || 'operator'}
                        </span>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-outline-success btn-sm py-0 px-2"
                          onClick={() => handleAcknowledgeAlert(a.id)}
                        >
                          Acknowledge
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Configurable Geofences Card */}
          <div
            className="p-3 rounded-3 border"
            style={{
              background: 'rgba(13, 20, 38, 0.85)',
              borderColor: 'rgba(56, 189, 248, 0.25)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center mb-3">
              <div className="d-flex align-items-center gap-2">
                <Shield size={18} className="text-info" />
                <h6 className="fw-bold mb-0">Geofence Boundaries ({geofences.length})</h6>
              </div>
              <button
                type="button"
                className="btn btn-outline-info btn-sm py-0 px-2 d-inline-flex align-items-center gap-1"
                onClick={() => setIsGeofenceModalOpen(true)}
              >
                <Plus size={14} /> New Geofence
              </button>
            </div>

            <div
              className="d-flex flex-column gap-2 overflow-y-auto"
              style={{ maxHeight: '220px' }}
            >
              {geofences.length === 0 ? (
                <div className="text-secondary small text-center py-3">
                  No geofences configured yet. Click &quot;New Geofence&quot; to add a circular or polygonal boundary.
                </div>
              ) : (
                geofences.map((g) => (
                  <div
                    key={g.id}
                    className="p-2 rounded border d-flex justify-content-between align-items-center small"
                    style={{
                      background: 'rgba(15, 23, 42, 0.72)',
                      borderColor: 'rgba(148, 163, 184, 0.16)',
                    }}
                  >
                    <div>
                      <div className="fw-semibold text-light">
                        {g.name}{' '}
                        <span className="badge bg-secondary ms-1">{g.shapeType}</span>
                        <span className="badge bg-info text-dark ms-1">{g.fenceType}</span>
                      </div>
                      <div className="text-secondary">
                        Center: [{g.centerLatitude.toFixed(3)}, {g.centerLongitude.toFixed(3)}] ·{' '}
                        {g.radiusMeters}m
                      </div>
                    </div>
                    <button
                      type="button"
                      className={`btn btn-sm py-0 px-2 ${
                        g.active ? 'btn-success' : 'btn-outline-secondary'
                      }`}
                      onClick={() => handleToggleGeofence(g)}
                    >
                      {g.active ? 'Active' : 'Disabled'}
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      <CreateGeofenceModal
        isOpen={isGeofenceModalOpen}
        onClose={() => setIsGeofenceModalOpen(false)}
        onCreated={loadControlTowerData}
      />
    </div>
  );
};
