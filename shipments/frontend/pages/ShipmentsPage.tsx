import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  Compass,
  Edit3,
  Eye,
  Link2,
  Navigation,
  Package,
  PackagePlus,
  RefreshCw,
  Search,
  Truck,
} from 'lucide-react';
import { shipmentApi } from '../api/shipmentApi';
import { Shipment, ShipmentStatus } from '../types';
import { CreateShipmentModal } from '../components/CreateShipmentModal';
import { EditShipmentModal } from '../components/EditShipmentModal';
import { AssignTripModal } from '../components/AssignTripModal';
import { ShipmentDetailsModal } from '../components/ShipmentDetailsModal';
import { RouteWaypointsModal } from '../components/RouteWaypointsModal';

const STATUS_BADGE_CLASS: Record<ShipmentStatus, string> = {
  CREATED: 'bg-secondary text-light',
  ASSIGNED: 'bg-primary text-light',
  PICKED_UP: 'bg-info text-dark',
  IN_TRANSIT: 'bg-info text-dark',
  OUT_FOR_DELIVERY: 'bg-warning text-dark',
  DELIVERED: 'bg-success text-light',
  DELAYED: 'bg-danger text-light',
  CANCELLED: 'bg-dark text-secondary border border-secondary',
};

export const ShipmentsPage: React.FC = () => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [delayScanMessage, setDelayScanMessage] = useState<string | null>(null);

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Shipment | null>(null);
  const [assignTripTarget, setAssignTripTarget] = useState<Shipment | null>(null);
  const [detailsTarget, setDetailsTarget] = useState<Shipment | null>(null);
  const [waypointsTarget, setWaypointsTarget] = useState<Shipment | null>(null);

  const loadShipments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await shipmentApi.getShipments({
        search: search.trim() || undefined,
        status: statusFilter || undefined,
        page,
        size: 10,
      });
      setShipments(data?.content ?? []);
      setTotalElements(data?.totalElements ?? 0);
      setTotalPages(Math.max(1, data?.totalPages ?? 1));
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to load shipment manifests.'
      );
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, page]);

  useEffect(() => {
    loadShipments();
  }, [loadShipments]);

  const handleCheckDelayed = async () => {
    setDelayScanMessage(null);
    try {
      const newlyDelayed = await shipmentApi.checkDelayedShipments();
      setDelayScanMessage(
        newlyDelayed.length > 0
          ? `Detected and flagged ${newlyDelayed.length} overdue shipment(s) as DELAYED.`
          : 'All active shipments are currently within promised delivery windows.'
      );
      loadShipments();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to run delayed shipment check.'
      );
    }
  };

  const inTransitCount = shipments.filter(
    (s) =>
      s.status === 'PICKED_UP' ||
      s.status === 'IN_TRANSIT' ||
      s.status === 'OUT_FOR_DELIVERY'
  ).length;
  const deliveredCount = shipments.filter((s) => s.status === 'DELIVERED').length;
  const delayedCount = shipments.filter((s) => s.delayed || s.status === 'DELAYED').length;

  return (
    <div className="container-fluid py-3 text-light">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2">
            <Package size={24} className="text-info" />
            <h2 className="h4 fw-bold mb-0">Shipment & Consignment Manifests</h2>
          </div>
          <p className="text-secondary small mb-0 mt-1">
            Manage consignment lifecycle, trip associations, Haversine route distance estimates, and delivery timelines
          </p>
        </div>

        <div className="d-flex flex-wrap gap-2">
          <Link
            to="/shipments/waypoints"
            className="btn btn-outline-info btn-sm d-inline-flex align-items-center gap-1"
          >
            <Compass size={16} /> Route & Waypoint Planner
          </Link>
          <button
            type="button"
            className="btn btn-outline-warning btn-sm d-inline-flex align-items-center gap-1"
            onClick={handleCheckDelayed}
          >
            <AlertTriangle size={16} /> Scan Overdue Shipments
          </button>
          <button
            type="button"
            className="btn btn-info btn-sm text-dark fw-semibold d-inline-flex align-items-center gap-1"
            onClick={() => setIsCreateOpen(true)}
          >
            <PackagePlus size={16} /> New Shipment
          </button>
        </div>
      </div>

      {/* KPI Summary Cards */}
      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.78)',
              borderColor: 'rgba(56, 189, 248, 0.22)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Total Shipments</span>
              <Package size={18} className="text-info" />
            </div>
            <div className="fs-3 fw-bold mt-1">{totalElements}</div>
            <div className="small text-secondary">Registered manifests</div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.78)',
              borderColor: 'rgba(56, 189, 248, 0.22)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Active In-Transit</span>
              <Truck size={18} className="text-primary" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-info">{inTransitCount}</div>
            <div className="small text-secondary">Picked up / In transit / Out for delivery</div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.78)',
              borderColor: 'rgba(16, 185, 129, 0.28)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Completed Deliveries</span>
              <CheckCircle2 size={18} className="text-success" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-success">{deliveredCount}</div>
            <div className="small text-secondary">Confirmed PODs on current page</div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div
            className="p-3 rounded-3 border h-100"
            style={{
              background: 'rgba(13, 20, 38, 0.78)',
              borderColor: 'rgba(239, 68, 68, 0.3)',
            }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-secondary">Delayed / Overdue</span>
              <AlertTriangle size={18} className="text-danger" />
            </div>
            <div className="fs-3 fw-bold mt-1 text-danger">{delayedCount}</div>
            <div className="small text-secondary">Past promised delivery time</div>
          </div>
        </div>
      </div>

      {delayScanMessage && (
        <div className="alert alert-info py-2 small mb-3" role="status">
          {delayScanMessage}
        </div>
      )}

      {error && (
        <div className="alert alert-danger py-2 small mb-3" role="alert">
          {error}
        </div>
      )}

      {/* Search & Filter Bar */}
      <div
        className="p-3 rounded-3 border mb-3"
        style={{
          background: 'rgba(13, 20, 38, 0.78)',
          borderColor: 'rgba(148, 163, 184, 0.16)',
        }}
      >
        <div className="row g-2 align-items-center">
          <div className="col-md-6">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-dark border-secondary text-secondary">
                <Search size={15} />
              </span>
              <input
                type="text"
                className="form-control bg-dark text-light border-secondary"
                placeholder="Search by tracking reference, sender, receiver, origin or destination city..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>

          <div className="col-md-4">
            <select
              className="form-select form-select-sm bg-dark text-light border-secondary"
              aria-label="Filter by Status"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All Lifecycle Statuses</option>
              <option value="CREATED">CREATED</option>
              <option value="ASSIGNED">ASSIGNED</option>
              <option value="PICKED_UP">PICKED_UP</option>
              <option value="IN_TRANSIT">IN_TRANSIT</option>
              <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY</option>
              <option value="DELIVERED">DELIVERED</option>
              <option value="DELAYED">DELAYED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          <div className="col-md-2 d-flex justify-content-end">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm w-100 d-inline-flex align-items-center justify-content-center gap-1"
              onClick={loadShipments}
            >
              <RefreshCw size={14} /> Refresh
            </button>
          </div>
        </div>
      </div>

      {/* Manifest Table */}
      <div
        className="rounded-3 border overflow-hidden"
        style={{
          background: 'rgba(13, 20, 38, 0.82)',
          borderColor: 'rgba(148, 163, 184, 0.16)',
        }}
      >
        <div className="table-responsive">
          <table className="table table-dark table-hover align-middle mb-0">
            <thead>
              <tr className="text-secondary small">
                <th>Tracking Ref</th>
                <th>Sender → Receiver</th>
                <th>Corridor (Origin → Dest)</th>
                <th>Cargo & Weight</th>
                <th>Trip Link</th>
                <th>Est. Route (Haversine)</th>
                <th>Promised / ETA</th>
                <th>Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} className="text-center py-5 text-secondary">
                    Loading shipment manifests...
                  </td>
                </tr>
              ) : shipments.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-5 text-secondary">
                    No shipments match the current search or filter criteria.
                  </td>
                </tr>
              ) : (
                shipments.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <div className="fw-bold text-info">{s.trackingNumber}</div>
                      <div className="small text-secondary">
                        Created {new Date(s.createdAt).toLocaleDateString()}
                      </div>
                    </td>
                    <td>
                      <div className="fw-semibold">{s.senderName}</div>
                      <div className="small text-secondary">→ {s.receiverName}</div>
                    </td>
                    <td>
                      <div>
                        {s.originCity} → {s.destinationCity}
                      </div>
                    </td>
                    <td>
                      <div>{s.weightKg} kg</div>
                      {s.cargoDescription && (
                        <div className="small text-secondary text-truncate" style={{ maxWidth: 160 }}>
                          {s.cargoDescription}
                        </div>
                      )}
                    </td>
                    <td>
                      {s.tripNumber ? (
                        <span className="badge bg-primary bg-opacity-25 text-info border border-info border-opacity-25">
                          {s.tripNumber}
                        </span>
                      ) : (
                        <span className="text-secondary small">Unassigned</span>
                      )}
                    </td>
                    <td>
                      {s.estimatedDistanceKm != null ? (
                        <div>
                          <span className="text-info fw-semibold">
                            ~{s.estimatedDistanceKm} km
                          </span>
                          <div className="small text-secondary">
                            Est. {s.estimatedDurationMinutes ?? 0} mins
                          </div>
                        </div>
                      ) : (
                        <span className="text-secondary small">Not planned</span>
                      )}
                    </td>
                    <td>
                      <div className="small">
                        {s.promisedDeliveryTime ? (
                          <div>
                            <span className="text-secondary">Promised: </span>
                            {new Date(s.promisedDeliveryTime).toLocaleDateString()}
                          </div>
                        ) : (
                          <span className="text-secondary">No SLA set</span>
                        )}
                        {s.estimatedArrivalTime && (
                          <div className="text-warning">
                            Est. ETA: {new Date(s.estimatedArrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="d-flex flex-column gap-1 align-items-start">
                        <span
                          className={`badge ${
                            STATUS_BADGE_CLASS[s.status] || 'bg-secondary'
                          }`}
                        >
                          {s.status}
                        </span>
                        {s.delayed && s.status !== 'DELAYED' && (
                          <span className="badge bg-danger">OVERDUE</span>
                        )}
                      </div>
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <button
                          type="button"
                          className="btn btn-outline-info"
                          title="View Details, Timeline & Status Transitions"
                          onClick={() => setDetailsTarget(s)}
                        >
                          <Eye size={14} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-light"
                          title="Plan Route Waypoints & Haversine Distance"
                          onClick={() => setWaypointsTarget(s)}
                        >
                          <Navigation size={14} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-primary"
                          title="Associate with Dispatch Trip"
                          onClick={() => setAssignTripTarget(s)}
                        >
                          <Link2 size={14} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          title="Edit Shipment Manifest"
                          onClick={() => setEditTarget(s)}
                        >
                          <Edit3 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-top border-secondary border-opacity-25 small text-secondary">
          <div>
            Showing page {page + 1} of {totalPages} ({totalElements} total manifests)
          </div>
          <div className="btn-group btn-group-sm">
            <button
              type="button"
              className="btn btn-outline-secondary"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Modals */}
      <CreateShipmentModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={loadShipments}
      />

      <EditShipmentModal
        shipment={editTarget}
        isOpen={Boolean(editTarget)}
        onClose={() => setEditTarget(null)}
        onSuccess={loadShipments}
      />

      <AssignTripModal
        shipment={assignTripTarget}
        isOpen={Boolean(assignTripTarget)}
        onClose={() => setAssignTripTarget(null)}
        onSuccess={loadShipments}
      />

      <ShipmentDetailsModal
        shipment={detailsTarget}
        isOpen={Boolean(detailsTarget)}
        onClose={() => setDetailsTarget(null)}
        onUpdated={loadShipments}
      />

      <RouteWaypointsModal
        shipment={waypointsTarget}
        isOpen={Boolean(waypointsTarget)}
        onClose={() => setWaypointsTarget(null)}
        onSaved={loadShipments}
      />
    </div>
  );
};
