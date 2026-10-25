import React, { useEffect, useState } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { Badge } from '../../../shared/components/Badge';
import {
  Users,
  Plus,
  ShieldCheck,
  Search,
  AlertTriangle,
  Star,
  Edit2,
  Eye,
  RefreshCw,
} from 'lucide-react';
import { driverApi } from '../api/driverApi';
import { Driver } from '../types';
import { CreateDriverModal } from '../components/CreateDriverModal';
import { EditDriverModal } from '../components/EditDriverModal';
import { DriverDetailsModal } from '../components/DriverDetailsModal';

export const DriversPage: React.FC = () => {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [availability, setAvailability] = useState('');
  const [activeFilter, setActiveFilter] = useState<boolean | ''>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingDriver, setEditingDriver] = useState<Driver | null>(null);
  const [viewingDriver, setViewingDriver] = useState<Driver | null>(null);

  const fetchDrivers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await driverApi.getDrivers({
        search: search.trim() || undefined,
        availability: availability || undefined,
        active: activeFilter === '' ? undefined : Boolean(activeFilter),
        page,
        size: 15,
      });
      setDrivers(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load driver roster.');
      setDrivers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDrivers();
  }, [search, availability, activeFilter, page]);

  // KPI Calculations
  const availableCount = drivers.filter((d) => d.availabilityStatus === 'AVAILABLE').length;
  const onDutyCount = drivers.filter((d) => d.availabilityStatus === 'ON_DUTY').length;
  const expiringCount = drivers.filter((d) => {
    const diff = new Date(d.licenseExpiryDate).getTime() - new Date().getTime();
    return diff < 30 * 24 * 60 * 60 * 1000;
  }).length;

  return (
    <div>
      <PageHeader
        title="Driver Roster &amp; Compliance"
        subtitle="Commercial driver licensing credentials, duty hour availability, and safety performance scoring."
      >
        <button
          className="btn fx-btn-gold btn-sm d-flex align-items-center gap-2 shadow"
          onClick={() => setIsCreateOpen(true)}
        >
          <Plus size={16} /> Onboard Driver
        </button>
      </PageHeader>

      {/* KPI Stats Strip */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Total Roster</div>
            <div className="h4 fw-bold mb-0" style={{ color: 'var(--fx-ivory-text)' }}>{totalElements}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>Available for Dispatch</div>
            <div className="h4 fw-bold mb-0" style={{ color: '#8CE3A7' }}>{availableCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>On Duty (Transit)</div>
            <div className="h4 fw-bold mb-0" style={{ color: 'var(--fx-champagne-gold)' }}>{onDutyCount}</div>
          </div>
        </div>
        <div className="col-6 col-md">
          <div className="p-3 rounded-3 border fx-glass-card" style={{ background: 'rgba(48, 33, 28, 0.78)', borderColor: 'rgba(232, 211, 162, 0.22)' }}>
            <div className="small fw-medium mb-1" style={{ color: 'var(--fx-muted-sand)' }}>License Expiries (&lt;30d)</div>
            <div className={`h4 fw-bold mb-0`} style={{ color: expiringCount > 0 ? '#FFAE78' : 'var(--fx-ivory-text)' }}>
              {expiringCount}
            </div>
          </div>
        </div>
      </div>

      {/* Search and Filters Bar */}
      <div className="fx-card mb-4">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-4">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white text-muted">
                <Search size={14} />
              </span>
              <input
                type="text"
                className="form-control"
                placeholder="Search by driver name, license, phone..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="col-6 col-md-3">
            <select
              className="form-select form-select-sm"
              value={availability}
              onChange={(e) => setAvailability(e.target.value)}
            >
              <option value="">All Duty Statuses</option>
              <option value="AVAILABLE">AVAILABLE</option>
              <option value="ON_DUTY">ON_DUTY</option>
              <option value="ON_LEAVE">ON_LEAVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
            </select>
          </div>

          <div className="col-6 col-md-3">
            <select
              className="form-select form-select-sm"
              value={activeFilter === '' ? '' : activeFilter ? 'true' : 'false'}
              onChange={(e) => setActiveFilter(e.target.value === '' ? '' : e.target.value === 'true')}
            >
              <option value="">All Employment States</option>
              <option value="true">Active Only</option>
              <option value="false">Inactive Only</option>
            </select>
          </div>

          <div className="col-12 col-md-2 text-md-end">
            <button
              className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1"
              onClick={fetchDrivers}
              disabled={loading}
            >
              <RefreshCw size={14} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>
      </div>

      {/* Driver Roster Table */}
      <div className="fx-card mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <Users size={18} className="text-primary" />
            <span className="fw-semibold small">Active Driver Roster ({totalElements})</span>
          </div>
          <span className="d-flex align-items-center gap-1 text-success small">
            <ShieldCheck size={14} /> License Compliance Enforced
          </span>
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
                <th>Driver Name</th>
                <th>License Number</th>
                <th>Category</th>
                <th>License Expiry</th>
                <th>Safety Score</th>
                <th>Duty Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-muted">
                    <span className="spinner-border spinner-border-sm me-2 text-primary" /> Loading driver roster...
                  </td>
                </tr>
              ) : drivers.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-muted">
                    No drivers found matching current search criteria.
                  </td>
                </tr>
              ) : (
                drivers.map((d) => {
                  const isExp = new Date(d.licenseExpiryDate) < new Date();
                  const isSoon =
                    !isExp &&
                    new Date(d.licenseExpiryDate).getTime() - new Date().getTime() < 30 * 24 * 60 * 60 * 1000;

                  return (
                    <tr key={d.id}>
                      <td>
                        <div className="fw-medium text-slate-900">{d.fullName}</div>
                        <small className="text-muted">{d.phoneNumber}</small>
                      </td>
                      <td>
                        <code className="font-monospace">{d.licenseNumber}</code>
                      </td>
                      <td>
                        <span className="badge bg-secondary-subtle text-secondary border">
                          {d.licenseCategory}
                        </span>
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-1">
                          <span className={isExp ? 'text-danger fw-bold' : isSoon ? 'text-warning fw-semibold' : 'text-slate-800'}>
                            {d.licenseExpiryDate}
                          </span>
                          {isSoon && (
                            <span title="License expiring within 30 days">
                              <AlertTriangle size={13} className="text-warning" />
                            </span>
                          )}
                          {isExp && (
                            <span title="License expired!">
                              <AlertTriangle size={13} className="text-danger" />
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className="badge bg-light text-dark border d-inline-flex align-items-center gap-1">
                          <Star size={12} className="text-warning fill-warning" />
                          {d.safetyScore}/100
                        </span>
                      </td>
                      <td>
                        <Badge status={d.availabilityStatus} />
                      </td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm">
                          <button
                            className="btn btn-outline-secondary py-0 px-2"
                            title="View Profile and Metrics"
                            onClick={() => setViewingDriver(d)}
                          >
                            <Eye size={13} />
                          </button>
                          <button
                            className="btn btn-outline-primary py-0 px-2"
                            title="Edit Driver Details"
                            onClick={() => setEditingDriver(d)}
                          >
                            <Edit2 size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
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
      <CreateDriverModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={fetchDrivers}
      />

      {editingDriver && (
        <EditDriverModal
          driver={editingDriver}
          isOpen={true}
          onClose={() => setEditingDriver(null)}
          onSuccess={fetchDrivers}
        />
      )}

      {viewingDriver && (
        <DriverDetailsModal
          driver={viewingDriver}
          isOpen={true}
          onClose={() => setViewingDriver(null)}
        />
      )}
    </div>
  );
};
