import React, { useEffect, useState } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { Badge } from '../../../shared/components/Badge';
import {
  Navigation,
  Plus,
  ArrowRight,
  Search,
  RefreshCw,
  Clock,
  Layers,
} from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Trip } from '../types';
import { CreateTripModal } from '../components/CreateTripModal';
import { TripLifecycleModal } from '../components/TripLifecycleModal';

export const TripsPage: React.FC = () => {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);

  const fetchTrips = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await driverApi.getTrips({
        search: search.trim() || undefined,
        status: status || undefined,
        page,
        size: 15,
      });
      setTrips(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load trip dispatch records.');
      setTrips([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTrips();
  }, [search, status, page]);

  // KPI calculations
  const inProgressCount = trips.filter((t) => t.status === 'IN_PROGRESS').length;
  const assignedCount = trips.filter((t) => t.status === 'ASSIGNED').length;
  const completedCount = trips.filter((t) => t.status === 'COMPLETED').length;

  return (
    <div>
      <PageHeader
        title="Trip Dispatch &amp; Operations"
        subtitle="Manage logistics dispatch workflows, driver/vehicle pairing, and active trip transit milestones."
      >
        <button
          className="btn fx-btn-gold btn-sm d-flex align-items-center gap-2 shadow"
          onClick={() => setIsCreateOpen(true)}
        >
          <Plus size={16} /> Schedule Trip
        </button>
      </PageHeader>

      {/* KPI Stats Strip */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Total Dispatched</div>
            <div className="h4 fw-bold mb-0" style={{ color: 'var(--fx-ivory-text)' }}>{totalElements}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>In Progress (Transit)</div>
            <div className="h4 fw-bold mb-0" style={{ color: 'var(--fx-champagne-gold)' }}>{inProgressCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Scheduled / Assigned</div>
            <div className="h4 fw-bold mb-0" style={{ color: '#FFAE78' }}>{assignedCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Completed Deliveries</div>
            <div className="h4 fw-bold mb-0" style={{ color: '#8CE3A7' }}>{completedCount}</div>
          </div>
        </div>
      </div>

      {/* Search and Filters Bar */}
      <div className="fx-card mb-4">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-5">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Search size={14} />
              </span>
              <input
                type="text"
                className="form-control"
                placeholder="Search trip code, origin, or destination..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="col-6 col-md-4">
            <select
              className="form-select form-select-sm"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="">All Lifecycle Statuses</option>
              <option value="ASSIGNED">ASSIGNED (Assets Allocated)</option>
              <option value="IN_PROGRESS">IN_PROGRESS (Departed / On Route)</option>
              <option value="COMPLETED">COMPLETED (Delivered)</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          <div className="col-6 col-md-3 text-md-end">
            <button
              className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1"
              onClick={fetchTrips}
              disabled={loading}
            >
              <RefreshCw size={14} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>
      </div>

      {/* Trips Dispatch Table */}
      <div className="fx-card mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <Navigation size={18} className="text-primary" />
            <span className="fw-semibold small">Active Dispatch Operations ({totalElements})</span>
          </div>
          <span className="text-muted small">Validated via VehicleLookup &amp; DriverLookup</span>
        </div>

        {error && (
          <div className="alert alert-danger py-2 px-3 small mb-3">
            {error}
          </div>
        )}

        <div className="table-responsive">
          <table className="fx-table">
            <thead>
              <tr>
                <th>Trip Code</th>
                <th>Route (Origin → Destination)</th>
                <th>Assigned Vehicle</th>
                <th>Assigned Driver</th>
                <th>Scheduled Departure</th>
                <th>Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-muted">
                    <span className="spinner-border spinner-border-sm me-2 text-primary" /> Loading dispatch operations...
                  </td>
                </tr>
              ) : trips.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-muted">
                    No trips found matching current filters.
                  </td>
                </tr>
              ) : (
                trips.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong className="font-monospace text-slate-900">{t.tripCode}</strong>
                      {t.distanceKm && <div className="text-muted small">{t.distanceKm} km</div>}
                    </td>
                    <td>
                      <div className="d-flex align-items-center gap-2 small">
                        <span className="fw-medium text-slate-800">{t.originAddress}</span>
                        <ArrowRight size={13} className="text-muted flex-shrink-0" />
                        <span className="fw-medium text-slate-800">{t.destinationAddress}</span>
                      </div>
                    </td>
                    <td>
                      <code className="font-monospace fw-semibold">{t.vehicleLicensePlate}</code>
                      <div className="text-muted small">{t.vehicleMakeModel}</div>
                    </td>
                    <td>
                      <div className="fw-medium text-slate-800">{t.driverName}</div>
                      <div className="text-muted small">{t.driverPhone}</div>
                    </td>
                    <td className="small text-muted">
                      <div>{new Date(t.scheduledStartTime).toLocaleDateString()}</div>
                      <div className="d-flex align-items-center gap-1">
                        <Clock size={11} /> {new Date(t.scheduledStartTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td>
                      <Badge status={t.status} />
                    </td>
                    <td className="text-end">
                      <button
                        className="btn btn-outline-primary btn-sm py-0 px-2 d-inline-flex align-items-center gap-1"
                        style={{ fontSize: '0.75rem' }}
                        onClick={() => setSelectedTrip(t)}
                      >
                        <Layers size={12} /> Lifecycle
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="d-flex justify-content-between align-items-center mt-3 pt-3 border-top small text-muted">
            <span>Page {page + 1} of {totalPages}</span>
            <div className="btn-group btn-group-sm">
              <button
                className="btn btn-outline-secondary"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <button
                className="btn btn-outline-secondary"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateTripModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={fetchTrips}
      />

      {selectedTrip && (
        <TripLifecycleModal
          trip={selectedTrip}
          isOpen={true}
          onClose={() => setSelectedTrip(null)}
          onSuccess={fetchTrips}
        />
      )}
    </div>
  );
};
