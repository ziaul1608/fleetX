import React, { useState, useEffect } from 'react';
import { Truck, Fuel, Wrench, AlertTriangle, CheckCircle2, X } from 'lucide-react';
import { VehicleDto, FuelRecordDto, MaintenanceRecordDto } from '../types';
import { fleetApi } from '../api/fleetApi';

interface VehicleDetailsModalProps {
  isOpen: boolean;
  vehicle: VehicleDto | null;
  onClose: () => void;
  onLogFuel?: (vehicle: VehicleDto) => void;
  onLogMaintenance?: (vehicle: VehicleDto) => void;
}

export const VehicleDetailsModal: React.FC<VehicleDetailsModalProps> = ({
  isOpen,
  vehicle,
  onClose,
  onLogFuel,
  onLogMaintenance,
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'fuel' | 'maintenance'>('overview');
  const [fuelRecords, setFuelRecords] = useState<FuelRecordDto[]>([]);
  const [maintenanceRecords, setMaintenanceRecords] = useState<MaintenanceRecordDto[]>([]);
  const [loadingRecords, setLoadingRecords] = useState(false);

  useEffect(() => {
    if (isOpen && vehicle) {
      setActiveTab('overview');
      loadRecords(vehicle.id);
    }
  }, [isOpen, vehicle]);

  const loadRecords = async (vehicleId: number) => {
    setLoadingRecords(true);
    try {
      const [fuelRes, maintRes] = await Promise.all([
        fleetApi.getFuelRecords(vehicleId, { page: 0, size: 20 }),
        fleetApi.getMaintenanceRecords(vehicleId, { page: 0, size: 20 }),
      ]);
      setFuelRecords(fuelRes.content);
      setMaintenanceRecords(maintRes.content);
    } catch {
      // Fallback cleanly on error
    } finally {
      setLoadingRecords(false);
    }
  };

  if (!isOpen || !vehicle) return null;

  return (
    <div className="modal fade show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(15, 23, 42, 0.45)' }}>
      <div className="modal-dialog modal-dialog-centered modal-xl">
        <div className="modal-content shadow border-0">
          <div className="modal-header border-bottom py-3">
            <div className="d-flex align-items-center gap-2">
              <div className="bg-primary text-white p-2 rounded">
                <Truck size={18} />
              </div>
              <div>
                <h5 className="modal-title fs-6 fw-bold text-slate-900 mb-0">
                  {vehicle.make} {vehicle.model} ({vehicle.licensePlate})
                </h5>
                <span className="text-muted small">VIN: <code>{vehicle.vin}</code></span>
              </div>
            </div>
            <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
          </div>

          <div className="modal-header border-bottom py-2 bg-light d-flex justify-content-between align-items-center">
            <ul className="nav nav-pills small">
              <li className="nav-item">
                <button
                  className={`nav-link py-1 px-3 ${activeTab === 'overview' ? 'active' : ''}`}
                  onClick={() => setActiveTab('overview')}
                >
                  Asset Specifications
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link py-1 px-3 ${activeTab === 'fuel' ? 'active' : ''}`}
                  onClick={() => setActiveTab('fuel')}
                >
                  Fuel History ({fuelRecords.length})
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link py-1 px-3 ${activeTab === 'maintenance' ? 'active' : ''}`}
                  onClick={() => setActiveTab('maintenance')}
                >
                  Maintenance Logs ({maintenanceRecords.length})
                </button>
              </li>
            </ul>

            <div className="d-flex gap-2">
              {onLogFuel && (
                <button
                  className="btn btn-outline-primary btn-sm py-1 px-2 d-flex align-items-center gap-1"
                  onClick={() => onLogFuel(vehicle)}
                >
                  <Fuel size={14} /> Log Fuel
                </button>
              )}
              {onLogMaintenance && (
                <button
                  className="btn btn-outline-secondary btn-sm py-1 px-2 d-flex align-items-center gap-1"
                  onClick={() => onLogMaintenance(vehicle)}
                >
                  <Wrench size={14} /> Schedule Service
                </button>
              )}
            </div>
          </div>

          <div className="modal-body p-4" style={{ minHeight: '340px' }}>
            {activeTab === 'overview' && (
              <div className="row g-4">
                <div className="col-12 col-md-6">
                  <div className="p-3 bg-light rounded border h-100">
                    <h6 className="fw-bold small text-slate-900 mb-3">Vehicle Attributes &amp; Telemetry</h6>
                    <dl className="row small mb-0">
                      <dt className="col-5 text-muted">License Plate:</dt>
                      <dd className="col-7 fw-semibold text-slate-900">{vehicle.licensePlate}</dd>

                      <dt className="col-5 text-muted">VIN Identifier:</dt>
                      <dd className="col-7"><code>{vehicle.vin}</code></dd>

                      <dt className="col-5 text-muted">Make &amp; Model:</dt>
                      <dd className="col-7">{vehicle.make} {vehicle.model} ({vehicle.modelYear})</dd>

                      <dt className="col-5 text-muted">Category:</dt>
                      <dd className="col-7">{vehicle.vehicleType}</dd>

                      <dt className="col-5 text-muted">Fuel Type:</dt>
                      <dd className="col-7">{vehicle.fuelType}</dd>

                      <dt className="col-5 text-muted">Current Odometer:</dt>
                      <dd className="col-7 fw-bold text-primary">{vehicle.odometerKm.toLocaleString()} km</dd>
                    </dl>
                  </div>
                </div>

                <div className="col-12 col-md-6">
                  <div className="p-3 bg-light rounded border h-100">
                    <h6 className="fw-bold small text-slate-900 mb-3">Capacity &amp; Service Health</h6>
                    <dl className="row small mb-0">
                      <dt className="col-5 text-muted">Payload Capacity:</dt>
                      <dd className="col-7">{vehicle.capacityKg.toLocaleString()} kg</dd>

                      <dt className="col-5 text-muted">Cargo Volume:</dt>
                      <dd className="col-7">{vehicle.capacityVolumeM3} m³</dd>

                      <dt className="col-5 text-muted">Operational State:</dt>
                      <dd className="col-7">
                        <span className="badge bg-light text-primary border border-primary-subtle px-2 py-1">
                          {vehicle.status}
                        </span>
                      </dd>

                      <dt className="col-5 text-muted">Active in System:</dt>
                      <dd className="col-7">
                        {vehicle.active ? (
                          <span className="text-success d-flex align-items-center gap-1">
                            <CheckCircle2 size={14} /> Active
                          </span>
                        ) : (
                          <span className="text-danger d-flex align-items-center gap-1">
                            <X size={14} /> Deactivated
                          </span>
                        )}
                      </dd>

                      <dt className="col-5 text-muted">Avg Fuel Efficiency:</dt>
                      <dd className="col-7">
                        {vehicle.averageFuelEfficiency ? (
                          <strong className="text-success">{vehicle.averageFuelEfficiency} km/L</strong>
                        ) : (
                          <span className="text-muted">Not established yet</span>
                        )}
                      </dd>

                      <dt className="col-5 text-muted">Maintenance Due:</dt>
                      <dd className="col-7">
                        {vehicle.serviceDue ? (
                          <span className="badge bg-warning-subtle text-warning border border-warning-subtle d-inline-flex align-items-center gap-1">
                            <AlertTriangle size={12} /> Service Required
                          </span>
                        ) : (
                          <span className="text-success">Up to Date</span>
                        )}
                      </dd>
                    </dl>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'fuel' && (
              <div>
                {loadingRecords ? (
                  <div className="text-center py-4 text-muted small">Loading fuel history...</div>
                ) : fuelRecords.length === 0 ? (
                  <div className="text-center py-5 text-muted small">No fuel records logged for this vehicle yet.</div>
                ) : (
                  <div className="table-responsive">
                    <table className="fx-table small">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Station</th>
                          <th>Volume (L)</th>
                          <th>Price / L</th>
                          <th>Total Cost</th>
                          <th>Odometer</th>
                          <th>Efficiency</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fuelRecords.map((f) => (
                          <tr key={f.id}>
                            <td>{new Date(f.fuelDate).toLocaleString()}</td>
                            <td>{f.fuelStation || '—'}</td>
                            <td className="fw-medium">{f.liters.toFixed(2)} L</td>
                            <td>{f.pricePerLiter ? `₹${f.pricePerLiter.toFixed(2)}` : '—'}</td>
                            <td className="fw-bold">₹{f.totalCost.toFixed(2)}</td>
                            <td>{f.odometerAtFueling.toLocaleString()} km</td>
                            <td>
                              {f.efficiencyKmPerLiter ? (
                                <span className="badge bg-success-subtle text-success border border-success-subtle">
                                  {f.efficiencyKmPerLiter.toFixed(2)} km/L
                                </span>
                              ) : (
                                <span className="text-muted small">Baseline / Top-up</span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'maintenance' && (
              <div>
                {loadingRecords ? (
                  <div className="text-center py-4 text-muted small">Loading maintenance records...</div>
                ) : maintenanceRecords.length === 0 ? (
                  <div className="text-center py-5 text-muted small">No maintenance records logged yet.</div>
                ) : (
                  <div className="table-responsive">
                    <table className="fx-table small">
                      <thead>
                        <tr>
                          <th>Service Date</th>
                          <th>Type</th>
                          <th>Description</th>
                          <th>Cost</th>
                          <th>Odometer</th>
                          <th>Next Due (km / Date)</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {maintenanceRecords.map((m) => (
                          <tr key={m.id}>
                            <td>{m.serviceDate}</td>
                            <td>
                              <span className="badge bg-light text-slate-800 border">
                                {m.serviceType}
                              </span>
                            </td>
                            <td>{m.description}</td>
                            <td className="fw-semibold">₹{m.cost.toFixed(2)}</td>
                            <td>{m.odometerAtService.toLocaleString()} km</td>
                            <td>
                              {m.nextServiceDueKm ? `${m.nextServiceDueKm.toLocaleString()} km` : ''}
                              {m.nextServiceDueDate ? ` (${m.nextServiceDueDate})` : ''}
                              {!m.nextServiceDueKm && !m.nextServiceDueDate && '—'}
                            </td>
                            <td>
                              {m.isOverdue ? (
                                <span className="badge bg-danger-subtle text-danger border border-danger-subtle">
                                  OVERDUE
                                </span>
                              ) : (
                                <span className="badge bg-success-subtle text-success border border-success-subtle">
                                  {m.status}
                                </span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="modal-footer border-top py-2 bg-light">
            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
