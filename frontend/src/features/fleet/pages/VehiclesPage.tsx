import React, { useCallback, useEffect, useState } from 'react';
import {
  Truck,
  Plus,
  Search,
  Filter,
  RefreshCw,
  Fuel,
  Wrench,
  Edit,
  Eye,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { Badge } from '../../../shared/components/Badge';
import { LoadingState } from '../../../shared/components/LoadingState';
import { ErrorState } from '../../../shared/components/ErrorState';
import { EmptyState } from '../../../shared/components/EmptyState';
import { fleetApi } from '../api/fleetApi';
import {
  VehicleDto,
  VehicleCreateRequest,
  VehicleUpdateRequest,
  FuelRecordCreateRequest,
  MaintenanceRecordCreateRequest,
} from '../types';
import { CreateVehicleModal } from '../components/CreateVehicleModal';
import { EditVehicleModal } from '../components/EditVehicleModal';
import { LogFuelModal } from '../components/LogFuelModal';
import { ScheduleMaintenanceModal } from '../components/ScheduleMaintenanceModal';
import { VehicleDetailsModal } from '../components/VehicleDetailsModal';

const STATUS_OPTIONS = [
  'AVAILABLE',
  'ACTIVE',
  'IDLE',
  'IN_TRANSIT',
  'IN_TRIP',
  'MAINTENANCE',
  'OFFLINE',
  'RETIRED',
];
const TYPE_OPTIONS = ['TRUCK', 'VAN', 'CONTAINER', 'MOTORCYCLE', 'TRAILER'];

export const VehiclesPage: React.FC = () => {
  const [vehicles, setVehicles] = useState<VehicleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notification, setNotification] = useState<{ type: 'success' | 'danger'; message: string } | null>(null);

  // Filters & Pagination
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState<boolean | ''>('');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedVehicleDetails, setSelectedVehicleDetails] = useState<VehicleDto | null>(null);
  const [editingVehicle, setEditingVehicle] = useState<VehicleDto | null>(null);
  const [fuelVehicle, setFuelVehicle] = useState<VehicleDto | null>(null);
  const [maintVehicle, setMaintVehicle] = useState<VehicleDto | null>(null);

  const fetchVehicles = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const pageResult = await fleetApi.getVehicles({
        page,
        size: pageSize,
        status: statusFilter || undefined,
        type: typeFilter || undefined,
        active: activeFilter,
        search: search.trim() || undefined,
      });
      setVehicles(pageResult.content);
      setTotalPages(pageResult.totalPages || 1);
      setTotalElements(pageResult.totalElements || 0);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Unable to retrieve fleet vehicle registry.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusFilter, typeFilter, activeFilter, search]);

  useEffect(() => {
    fetchVehicles();
  }, [fetchVehicles]);

  const showNotification = (type: 'success' | 'danger', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 5000);
  };

  const handleCreateVehicle = async (data: VehicleCreateRequest) => {
    await fleetApi.createVehicle(data);
    showNotification('success', `Vehicle '${data.licensePlate}' registered successfully.`);
    fetchVehicles();
  };

  const handleUpdateVehicle = async (id: number, data: VehicleUpdateRequest) => {
    await fleetApi.updateVehicle(id, data);
    showNotification('success', `Vehicle #${id} updated successfully.`);
    fetchVehicles();
  };

  const handleLogFuel = async (vehicleId: number, data: FuelRecordCreateRequest) => {
    await fleetApi.logFuelRecord(vehicleId, data);
    showNotification('success', `Fuel record logged for vehicle #${vehicleId}.`);
    fetchVehicles();
  };

  const handleLogMaintenance = async (vehicleId: number, data: MaintenanceRecordCreateRequest) => {
    await fleetApi.logMaintenanceRecord(vehicleId, data);
    showNotification('success', `Maintenance record logged for vehicle #${vehicleId}.`);
    fetchVehicles();
  };

  // Fleet KPIs
  const availableCount = vehicles.filter((v) => ['AVAILABLE', 'ACTIVE', 'IDLE'].includes(v.status.toUpperCase())).length;
  const transitCount = vehicles.filter((v) => ['IN_TRANSIT', 'IN_TRIP'].includes(v.status.toUpperCase())).length;
  const maintCount = vehicles.filter((v) => v.status.toUpperCase() === 'MAINTENANCE').length;
  const warningsDueCount = vehicles.filter((v) => v.serviceDue).length;

  return (
    <div>
      <PageHeader
        title="Fleet &amp; Asset Inventory"
        subtitle="Operational registry of commercial vehicles, capacity specs, odometer readings, fuel metrics, and status tracking."
      >
        <button
          className="btn btn-primary btn-sm d-flex align-items-center gap-2"
          onClick={() => setIsCreateOpen(true)}
        >
          <Plus size={16} /> Onboard Vehicle
        </button>
      </PageHeader>

      {/* Alert Notification */}
      {notification && (
        <div
          className={`alert alert-${notification.type} d-flex align-items-center gap-2 small py-2 mb-3 shadow-sm`}
          role="alert"
        >
          {notification.type === 'success' ? (
            <CheckCircle2 size={16} className="flex-shrink-0" />
          ) : (
            <AlertCircle size={16} className="flex-shrink-0" />
          )}
          <div>{notification.message}</div>
        </div>
      )}

      {/* KPI Stats Strip */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Total Assets</div>
            <div className="h4 fw-bold text-slate-900 mb-0">{totalElements}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Available for Trip</div>
            <div className="h4 fw-bold text-success mb-0">{availableCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">In Transit</div>
            <div className="h4 fw-bold text-info mb-0">{transitCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">In Maintenance</div>
            <div className="h4 fw-bold text-warning mb-0">{maintCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Service Due</div>
            <div className={`h4 fw-bold mb-0 ${warningsDueCount > 0 ? 'text-danger' : 'text-slate-900'}`}>
              {warningsDueCount}
            </div>
          </div>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="fx-card mb-4">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-4">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Search size={14} />
              </span>
              <input
                type="search"
                className="form-control"
                placeholder="Search by license plate, VIN, make or model..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>

          <div className="col-6 col-md-3">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Filter size={14} />
              </span>
              <select
                className="form-select"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                aria-label="Filter by operational status"
              >
                <option value="">All Operational Statuses</option>
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="col-6 col-md-2">
            <select
              className="form-select form-select-sm"
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by vehicle category"
            >
              <option value="">All Categories</option>
              {TYPE_OPTIONS.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>

          <div className="col-6 col-md-2">
            <select
              className="form-select form-select-sm"
              value={activeFilter === '' ? '' : activeFilter ? 'true' : 'false'}
              onChange={(e) => {
                const val = e.target.value;
                setActiveFilter(val === '' ? '' : val === 'true');
                setPage(0);
              }}
              aria-label="Filter by active state"
            >
              <option value="">All Active States</option>
              <option value="true">Active Only</option>
              <option value="false">Deactivated Only</option>
            </select>
          </div>

          <div className="col-6 col-md-1 text-end">
            <button
              className="btn btn-outline-secondary btn-sm p-1 px-2"
              onClick={() => fetchVehicles()}
              title="Refresh Registry"
              aria-label="Refresh vehicle list"
            >
              <RefreshCw size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Vehicles Table Card */}
      <div className="fx-card mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <Truck size={18} className="text-primary" />
            <span className="fw-semibold small">
              Commercial Fleet Directory ({totalElements})
            </span>
          </div>
          <span className="text-muted small">Validated via VehicleLookupService</span>
        </div>

        {loading ? (
          <LoadingState message="Loading vehicle inventory..." />
        ) : error ? (
          <ErrorState
            title="Failed to load vehicles"
            message={error}
            onRetry={fetchVehicles}
          />
        ) : vehicles.length === 0 ? (
          <EmptyState
            title="No Vehicles Found"
            description="No vehicles match the selected search terms and filter criteria."
            actionText="Reset Filters"
            onAction={() => {
              setSearch('');
              setStatusFilter('');
              setTypeFilter('');
              setActiveFilter('');
              setPage(0);
            }}
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="fx-table align-middle">
                <thead>
                  <tr>
                    <th>License Plate</th>
                    <th>Make &amp; Model</th>
                    <th>Category</th>
                    <th>Payload (kg)</th>
                    <th>Odometer</th>
                    <th>Fuel Avg</th>
                    <th>Status</th>
                    <th className="text-end" style={{ width: '200px' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {vehicles.map((v) => (
                    <tr key={v.id}>
                      <td>
                        <strong>{v.licensePlate}</strong>
                        <div className="text-muted small" style={{ fontSize: '0.7rem' }}>
                          <code>{v.vin}</code>
                        </div>
                      </td>
                      <td>
                        <div className="fw-medium text-slate-900">{v.make} {v.model}</div>
                        <small className="text-muted" style={{ fontSize: '0.7rem' }}>Year: {v.modelYear}</small>
                      </td>
                      <td>
                        <span className="badge bg-light text-slate-800 border">
                          {v.vehicleType}
                        </span>
                        <div className="text-muted small" style={{ fontSize: '0.7rem' }}>{v.fuelType}</div>
                      </td>
                      <td>{v.capacityKg.toLocaleString()} kg</td>
                      <td>
                        <span className="fw-semibold">{v.odometerKm.toLocaleString()} km</span>
                        {v.serviceDue && (
                          <div className="text-warning small d-flex align-items-center gap-1" style={{ fontSize: '0.7rem' }}>
                            <AlertTriangle size={11} /> Service Due
                          </div>
                        )}
                      </td>
                      <td>
                        {v.averageFuelEfficiency ? (
                          <span className="badge bg-success-subtle text-success border border-success-subtle">
                            {v.averageFuelEfficiency.toFixed(2)} km/L
                          </span>
                        ) : (
                          <span className="text-muted small">—</span>
                        )}
                      </td>
                      <td>
                        <Badge status={v.status} />
                        {!v.active && (
                          <span className="badge bg-secondary ms-1" style={{ fontSize: '0.65rem' }}>
                            OFFLINE
                          </span>
                        )}
                      </td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm" role="group">
                          <button
                            type="button"
                            className="btn btn-outline-secondary py-1 px-2"
                            onClick={() => setSelectedVehicleDetails(v)}
                            title="View Full Specifications & Logs"
                          >
                            <Eye size={13} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-primary py-1 px-2"
                            onClick={() => setFuelVehicle(v)}
                            title="Log Fuel Intake"
                          >
                            <Fuel size={13} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary py-1 px-2"
                            onClick={() => setMaintVehicle(v)}
                            title="Schedule or Record Maintenance"
                          >
                            <Wrench size={13} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary py-1 px-2"
                            onClick={() => setEditingVehicle(v)}
                            title="Edit Vehicle Attributes"
                          >
                            <Edit size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="d-flex align-items-center justify-content-between pt-3 border-top mt-2">
                <div className="small text-muted">
                  Showing page {page + 1} of {totalPages} ({totalElements} total assets)
                </div>
                <div className="d-flex align-items-center gap-1">
                  <button
                    className="btn btn-outline-secondary btn-sm p-1 px-2"
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(p - 1, 0))}
                    aria-label="Previous page"
                  >
                    <ChevronLeft size={16} />
                  </button>
                  <span className="small px-2 fw-medium text-slate-800">
                    {page + 1}
                  </span>
                  <button
                    className="btn btn-outline-secondary btn-sm p-1 px-2"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                    aria-label="Next page"
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Modals */}
      <CreateVehicleModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={handleCreateVehicle}
      />

      <EditVehicleModal
        isOpen={Boolean(editingVehicle)}
        vehicle={editingVehicle}
        onClose={() => setEditingVehicle(null)}
        onSubmit={handleUpdateVehicle}
      />

      <LogFuelModal
        isOpen={Boolean(fuelVehicle)}
        vehicle={fuelVehicle}
        onClose={() => setFuelVehicle(null)}
        onSubmit={handleLogFuel}
      />

      <ScheduleMaintenanceModal
        isOpen={Boolean(maintVehicle)}
        vehicle={maintVehicle}
        onClose={() => setMaintVehicle(null)}
        onSubmit={handleLogMaintenance}
      />

      <VehicleDetailsModal
        isOpen={Boolean(selectedVehicleDetails)}
        vehicle={selectedVehicleDetails}
        onClose={() => setSelectedVehicleDetails(null)}
        onLogFuel={(v) => {
          setSelectedVehicleDetails(null);
          setFuelVehicle(v);
        }}
        onLogMaintenance={(v) => {
          setSelectedVehicleDetails(null);
          setMaintVehicle(v);
        }}
      />
    </div>
  );
};
