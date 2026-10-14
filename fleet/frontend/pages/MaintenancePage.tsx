import React, { useCallback, useEffect, useState } from 'react';
import {
  Wrench,
  AlertTriangle,
  Calendar,
  CheckCircle2,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  Fuel,
} from 'lucide-react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { LoadingState } from '../../../shared/components/LoadingState';
import { ErrorState } from '../../../shared/components/ErrorState';
import { EmptyState } from '../../../shared/components/EmptyState';
import { fleetApi } from '../api/fleetApi';
import { ServiceWarningDto, MaintenanceRecordDto } from '../types';

export const MaintenancePage: React.FC = () => {
  const [warnings, setWarnings] = useState<ServiceWarningDto[]>([]);
  const [records, setRecords] = useState<MaintenanceRecordDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Pagination for maintenance records
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [warningsRes, recordsRes] = await Promise.all([
        fleetApi.getActiveWarnings(),
        fleetApi.getAllMaintenanceRecords({ page, size: pageSize }),
      ]);
      setWarnings(warningsRes);
      setRecords(recordsRes.content);
      setTotalPages(recordsRes.totalPages || 1);
      setTotalElements(recordsRes.totalElements || 0);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Unable to retrieve maintenance schedules.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const criticalWarnings = warnings.filter((w) => w.severity === 'CRITICAL');
  const upcomingWarnings = warnings.filter((w) => w.severity !== 'CRITICAL');

  return (
    <div>
      <PageHeader
        title="Fleet Maintenance &amp; Preventive Service"
        subtitle="Automated service interval warnings, maintenance history logs, odometer thresholds, and component wear tracking."
      >
        <button
          className="btn btn-outline-secondary btn-sm p-1 px-2 d-flex align-items-center gap-1"
          onClick={() => fetchData()}
          title="Refresh Maintenance Data"
        >
          <RefreshCw size={14} /> Refresh
        </button>
      </PageHeader>

      {/* Overview Stat Strip */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-4">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Total Maintenance Logs</div>
            <div className="h4 fw-bold text-slate-900 mb-0">{totalElements}</div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Critical Overdue Services</div>
            <div className={`h4 fw-bold mb-0 ${criticalWarnings.length > 0 ? 'text-danger' : 'text-slate-900'}`}>
              {criticalWarnings.length}
            </div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="p-3 bg-white rounded border shadow-sm">
            <div className="text-muted small fw-medium mb-1">Upcoming Service Due Warnings</div>
            <div className={`h4 fw-bold mb-0 ${upcomingWarnings.length > 0 ? 'text-warning' : 'text-slate-900'}`}>
              {upcomingWarnings.length}
            </div>
          </div>
        </div>
      </div>

      {/* Active Service Warnings Section */}
      <div className="fx-card mb-4 border-warning">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <AlertTriangle size={18} className="text-warning" />
            <span className="fw-semibold small">
              Active Service Interval Warnings ({warnings.length})
            </span>
          </div>
          <span className="text-muted small" style={{ fontSize: '0.75rem' }}>
            AlertPublisher Broadcast Active
          </span>
        </div>

        {warnings.length === 0 ? (
          <div className="p-3 text-center bg-light rounded text-muted small">
            <CheckCircle2 size={16} className="text-success me-1 inline" />
            All commercial fleet assets are operating within healthy service intervals. No pending warnings.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="fx-table align-middle">
              <thead>
                <tr>
                  <th>Vehicle Plate</th>
                  <th>Warning Classification</th>
                  <th>Alert Description</th>
                  <th>Severity</th>
                  <th>Logged Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {warnings.map((w) => (
                  <tr key={w.id}>
                    <td>
                      <strong>{w.licensePlate}</strong>
                    </td>
                    <td>
                      <code>{w.warningType}</code>
                    </td>
                    <td className="fw-medium text-slate-900">{w.message}</td>
                    <td>
                      {w.severity === 'CRITICAL' ? (
                        <span className="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1">
                          <ShieldAlert size={12} className="me-1 inline" /> CRITICAL OVERDUE
                        </span>
                      ) : (
                        <span className="badge bg-warning-subtle text-warning border border-warning-subtle px-2 py-1">
                          <AlertTriangle size={12} className="me-1 inline" /> UPCOMING DUE
                        </span>
                      )}
                    </td>
                    <td className="text-muted small">{new Date(w.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Maintenance History & Schedules Table */}
      <div className="fx-card mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <div className="d-flex align-items-center gap-2">
            <Wrench size={18} className="text-primary" />
            <span className="fw-semibold small">
              Fleet Maintenance History &amp; Schedules ({totalElements})
            </span>
          </div>
          <span className="text-muted small">Flyway V200 &bull; Flyway V201</span>
        </div>

        {loading ? (
          <LoadingState message="Loading maintenance records..." />
        ) : error ? (
          <ErrorState
            title="Failed to load maintenance records"
            message={error}
            onRetry={fetchData}
          />
        ) : records.length === 0 ? (
          <EmptyState
            title="No Maintenance Records Logged"
            description="No scheduled or completed maintenance records have been registered across the fleet."
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="fx-table align-middle">
                <thead>
                  <tr>
                    <th>Service Date</th>
                    <th>Vehicle Plate</th>
                    <th>Classification</th>
                    <th>Description</th>
                    <th>Invoiced Cost</th>
                    <th>Odometer at Service</th>
                    <th>Next Service Due</th>
                    <th>Execution Status</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((r) => (
                    <tr key={r.id}>
                      <td className="fw-medium">{r.serviceDate}</td>
                      <td>
                        <strong>{r.licensePlate}</strong>
                      </td>
                      <td>
                        <span className="badge bg-light text-slate-800 border">
                          {r.serviceType}
                        </span>
                      </td>
                      <td>{r.description}</td>
                      <td className="fw-bold">₹{r.cost.toFixed(2)}</td>
                      <td>{r.odometerAtService.toLocaleString()} km</td>
                      <td>
                        {r.nextServiceDueKm && <div>{r.nextServiceDueKm.toLocaleString()} km</div>}
                        {r.nextServiceDueDate && (
                          <div className="text-muted small" style={{ fontSize: '0.7rem' }}>
                            <Calendar size={11} className="me-1 inline" />
                            {r.nextServiceDueDate}
                          </div>
                        )}
                        {!r.nextServiceDueKm && !r.nextServiceDueDate && '—'}
                      </td>
                      <td>
                        {r.isOverdue ? (
                          <span className="badge bg-danger-subtle text-danger border border-danger-subtle">
                            OVERDUE
                          </span>
                        ) : (
                          <span className="badge bg-success-subtle text-success border border-success-subtle">
                            {r.status}
                          </span>
                        )}
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
                  Showing page {page + 1} of {totalPages} ({totalElements} total entries)
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
                  <span className="small px-2 fw-medium text-slate-800">{page + 1}</span>
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

      {/* Fuel Efficiency Methodology Note */}
      <div className="fx-card bg-light">
        <div className="d-flex align-items-center gap-2 mb-2">
          <Fuel size={18} className="text-primary" />
          <span className="fw-semibold small text-slate-900">
            Automated Fuel Efficiency &amp; Service Formula
          </span>
        </div>
        <p className="text-muted small mb-0">
          Module 2 tracks real-time fuel intake and calculates automated <code>ΔOdometer / Liters</code> efficiency against baseline benchmarks. Zero or non-positive distance deltas are safely recorded as null to avoid mathematical divide-by-zero errors. Automated service threshold triggers notify the Control Tower at 500 km or 14 days before scheduled preventive maintenance.
        </p>
      </div>
    </div>
  );
};
