import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Download,
  FileSpreadsheet,
  Filter,
  RefreshCw,
} from 'lucide-react';
import { trackingApi } from '../api/trackingApi';
import { ReportSummary } from '../types';

export const ReportsPage: React.FC = () => {
  const [reportType, setReportType] = useState<string>('TRIPS');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [reportSummary, setReportSummary] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [downloading, setDownloading] = useState<boolean>(false);
  const [downloadSuccess, setDownloadSuccess] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadReport = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trackingApi.getReportSummary({
        reportType,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        status: statusFilter.trim() || undefined,
      });
      setReportSummary(data);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to load persisted report data.'
      );
    } finally {
      setLoading(false);
    }
  }, [reportType, startDate, endDate, statusFilter]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  const handleDownloadCsv = async () => {
    setDownloading(true);
    setDownloadSuccess(false);
    setError(null);
    try {
      const csvText = await trackingApi.downloadReportCsv({
        reportType,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        status: statusFilter.trim() || undefined,
      });

      const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute(
        'download',
        `fleetx_${reportType.toLowerCase()}_report.csv`
      );
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setDownloadSuccess(true);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to export CSV report.'
      );
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="container-fluid py-3" style={{ color: 'var(--fx-ivory-text)' }}>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2">
            <FileSpreadsheet size={24} style={{ color: 'var(--fx-champagne-gold)' }} />
            <h2 className="h4 fw-bold mb-0" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
              Operational Summary Reports &amp; Safe CSV Export
            </h2>
          </div>
          <p className="small mb-0 mt-1" style={{ color: 'var(--fx-muted-sand)' }}>
            Query actual persisted operational data with date/status filters and export RFC 4180 compliant CSV files
          </p>
        </div>

        <Link
          to="/tracking/map"
          className="btn fx-btn-glass btn-sm d-inline-flex align-items-center gap-1"
        >
          <ArrowLeft size={15} /> Back to Control Tower
        </Link>
      </div>

      {error && (
        <div className="alert alert-danger py-2 small mb-3 shadow-sm" role="alert">
          {error}
        </div>
      )}

      {downloadSuccess && (
        <div
          className="alert alert-success py-2 small mb-3 d-flex align-items-center gap-2 shadow-sm"
          role="status"
        >
          <CheckCircle2 size={16} /> RFC 4180 CSV report downloaded successfully.
        </div>
      )}

      {/* Filter & Export Controls */}
      <div
        className="p-3 rounded-3 border mb-4 fx-glass-panel"
        style={{
          background: 'rgba(48, 33, 28, 0.85)',
          borderColor: 'rgba(232, 211, 162, 0.22)',
        }}
      >
        <div className="row g-3 align-items-end">
          <div className="col-md-3">
            <label className="form-label small fw-medium" htmlFor="report-type" style={{ color: 'var(--fx-ivory-text)' }}>
              Report Domain Category
            </label>
            <select
              id="report-type"
              className="form-select form-select-sm"
              value={reportType}
              onChange={(e) => setReportType(e.target.value)}
            >
              <option value="TRIPS">Trip Operations Log (TRIPS)</option>
              <option value="SHIPMENTS">Shipment Consignments (SHIPMENTS)</option>
              <option value="FUEL">Fuel Consumption &amp; Efficiency (FUEL)</option>
              <option value="MAINTENANCE">Maintenance &amp; Service (MAINTENANCE)</option>
              <option value="ALERTS">Geofence &amp; Speed Alerts (ALERTS)</option>
              <option value="VEHICLES">Fleet Vehicle Roster (VEHICLES)</option>
            </select>
          </div>

          <div className="col-md-2">
            <label className="form-label small fw-medium" htmlFor="start-date" style={{ color: 'var(--fx-ivory-text)' }}>
              <Calendar size={13} className="me-1" style={{ color: 'var(--fx-champagne-gold)' }} /> Start Date
            </label>
            <input
              id="start-date"
              type="date"
              className="form-control form-control-sm"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </div>

          <div className="col-md-2">
            <label className="form-label small fw-medium" htmlFor="end-date" style={{ color: 'var(--fx-ivory-text)' }}>
              <Calendar size={13} className="me-1" style={{ color: 'var(--fx-champagne-gold)' }} /> End Date
            </label>
            <input
              id="end-date"
              type="date"
              className="form-control form-control-sm"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>

          <div className="col-md-2">
            <label className="form-label small fw-medium" htmlFor="status-filter" style={{ color: 'var(--fx-ivory-text)' }}>
              <Filter size={13} className="me-1" style={{ color: 'var(--fx-champagne-gold)' }} /> Status / Severity Filter
            </label>
            <input
              id="status-filter"
              type="text"
              placeholder="e.g., COMPLETED, WARNING"
              className="form-control form-control-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            />
          </div>

          <div className="col-md-3 d-flex gap-2">
            <button
              type="button"
              className="btn fx-btn-glass btn-sm d-inline-flex align-items-center gap-1"
              onClick={loadReport}
              disabled={loading}
            >
              <RefreshCw size={14} /> Run Query
            </button>
            <button
              type="button"
              className="btn fx-btn-gold btn-sm fw-bold flex-grow-1 d-inline-flex align-items-center justify-content-center gap-1 shadow"
              onClick={handleDownloadCsv}
              disabled={downloading}
            >
              <Download size={14} />{' '}
              {downloading ? 'Exporting...' : 'Download CSV Report'}
            </button>
          </div>
        </div>
      </div>

      {/* Persisted Report Preview Table */}
      <div
        className="rounded-3 border overflow-hidden fx-glass-panel"
        style={{
          background: 'rgba(48, 33, 28, 0.85)',
          borderColor: 'rgba(232, 211, 162, 0.22)',
        }}
      >
        <div className="px-3 py-2.5 border-bottom d-flex justify-content-between align-items-center small" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
          <span className="fw-semibold" style={{ color: 'var(--fx-champagne-gold)' }}>
            Persisted Database Records — {reportSummary?.reportType || reportType} (
            {reportSummary?.totalRecords ?? 0} matching rows)
          </span>
          <span style={{ color: 'var(--fx-muted-sand)' }}>
            Generated:{' '}
            {reportSummary?.generatedAt
              ? new Date(reportSummary.generatedAt).toLocaleString()
              : '—'}
          </span>
        </div>

        <div className="table-responsive">
          <table className="fx-table align-middle mb-0">
            <thead>
              <tr>
                {(reportSummary?.columns ?? []).map((col) => (
                  <th key={col}>{col}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td
                    colSpan={Math.max(1, reportSummary?.columns?.length ?? 1)}
                    className="text-center py-5 text-secondary"
                  >
                    Querying persisted records...
                  </td>
                </tr>
              ) : !reportSummary || reportSummary.rows.length === 0 ? (
                <tr>
                  <td
                    colSpan={Math.max(1, reportSummary?.columns?.length ?? 1)}
                    className="text-center py-5"
                    style={{ color: 'var(--fx-muted-sand)' }}
                  >
                    No persisted records found in the database for the selected domain, date range, and status filter.
                  </td>
                </tr>
              ) : (
                reportSummary.rows.map((row, idx) => (
                  <tr key={idx}>
                    {reportSummary.columns.map((col) => (
                      <td key={col} className="small">
                        {row[col] !== null && row[col] !== undefined
                          ? String(row[col])
                          : '—'}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
