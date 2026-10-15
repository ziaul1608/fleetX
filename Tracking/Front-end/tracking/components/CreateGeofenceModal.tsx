import React, { useState } from 'react';
import { ShieldAlert, X } from 'lucide-react';
import { trackingApi } from '../api/trackingApi';
import { GeofenceCreateRequest } from '../types';

interface CreateGeofenceModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export const CreateGeofenceModal: React.FC<CreateGeofenceModalProps> = ({
  isOpen,
  onClose,
  onCreated,
}) => {
  const [name, setName] = useState('');
  const [fenceType, setFenceType] = useState('DEPOT');
  const [shapeType, setShapeType] = useState<'CIRCLE' | 'POLYGON'>('CIRCLE');
  const [centerLatitude, setCenterLatitude] = useState(19.076);
  const [centerLongitude, setCenterLongitude] = useState(72.8777);
  const [radiusMeters, setRadiusMeters] = useState(1500);
  const [speedLimitKmh, setSpeedLimitKmh] = useState<string>('60');
  const [polygonText, setPolygonText] = useState(
    '19.0900, 72.8600\n19.0900, 72.9000\n19.0600, 72.9000\n19.0600, 72.8600'
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (centerLatitude < -90 || centerLatitude > 90) {
      setError('Center latitude must be between -90 and 90.');
      return;
    }
    if (centerLongitude < -180 || centerLongitude > 180) {
      setError('Center longitude must be between -180 and 180.');
      return;
    }
    if (radiusMeters <= 0) {
      setError('Radius in meters must be strictly greater than zero.');
      return;
    }

    let polygonCoordinates: number[][] | undefined = undefined;
    if (shapeType === 'POLYGON') {
      const lines = polygonText
        .split('\n')
        .map((l) => l.trim())
        .filter(Boolean);
      if (lines.length < 3) {
        setError('A polygonal geofence requires at least 3 vertex lines (lat, lng).');
        return;
      }
      polygonCoordinates = [];
      for (const line of lines) {
        const parts = line.split(',').map((p) => Number(p.trim()));
        if (parts.length < 2 || Number.isNaN(parts[0]) || Number.isNaN(parts[1])) {
          setError(`Invalid vertex format "${line}". Expected "latitude, longitude".`);
          return;
        }
        polygonCoordinates.push([parts[0], parts[1]]);
      }
    }

    const payload: GeofenceCreateRequest = {
      name: name.trim(),
      centerLatitude,
      centerLongitude,
      radiusMeters,
      fenceType,
      shapeType,
      polygonCoordinates,
      speedLimitKmh: speedLimitKmh ? Number(speedLimitKmh) : null,
    };

    setLoading(true);
    try {
      await trackingApi.createGeofence(payload);
      setName('');
      onCreated();
      onClose();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to create geofence boundary.'
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
      <div className="modal-dialog modal-dialog-centered">
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
              <ShieldAlert size={18} className="text-info" />
              <h5 className="modal-title fw-bold mb-0">
                Create Configurable Geofence Boundary
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

          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {error && (
                <div className="alert alert-danger py-2 small" role="alert">
                  {error}
                </div>
              )}

              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label small text-secondary">Geofence Name *</label>
                  <input
                    type="text"
                    required
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    placeholder="e.g., Central Depot Mumbai"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Zone Type *</label>
                  <select
                    className="form-select form-select-sm bg-dark text-light border-secondary"
                    value={fenceType}
                    onChange={(e) => setFenceType(e.target.value)}
                  >
                    <option value="DEPOT">DEPOT</option>
                    <option value="CUSTOMER_HUB">CUSTOMER_HUB</option>
                    <option value="RESTRICTED">RESTRICTED</option>
                    <option value="CORRIDOR">CORRIDOR</option>
                  </select>
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Boundary Geometry *</label>
                  <select
                    className="form-select form-select-sm bg-dark text-light border-secondary"
                    value={shapeType}
                    onChange={(e) =>
                      setShapeType(e.target.value as 'CIRCLE' | 'POLYGON')
                    }
                  >
                    <option value="CIRCLE">CIRCLE (Center + Radius)</option>
                    <option value="POLYGON">POLYGON (Vertex Coordinates)</option>
                  </select>
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Center Latitude *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={centerLatitude}
                    onChange={(e) => setCenterLatitude(Number(e.target.value))}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Center Longitude *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={centerLongitude}
                    onChange={(e) => setCenterLongitude(Number(e.target.value))}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">Radius (Meters) *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={radiusMeters}
                    onChange={(e) => setRadiusMeters(Number(e.target.value))}
                  />
                </div>

                <div className="col-md-6">
                  <label className="form-label small text-secondary">
                    Zone Speed Limit (km/h)
                  </label>
                  <input
                    type="number"
                    step="any"
                    className="form-control form-control-sm bg-dark text-light border-secondary"
                    value={speedLimitKmh}
                    onChange={(e) => setSpeedLimitKmh(e.target.value)}
                  />
                </div>

                {shapeType === 'POLYGON' && (
                  <div className="col-12">
                    <label className="form-label small text-secondary">
                      Polygon Vertices (One &quot;lat, lng&quot; pair per line, min 3)
                    </label>
                    <textarea
                      rows={4}
                      className="form-control form-control-sm bg-dark text-light border-secondary font-monospace"
                      value={polygonText}
                      onChange={(e) => setPolygonText(e.target.value)}
                    />
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer border-top border-secondary border-opacity-25">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-info btn-sm text-dark fw-semibold"
                disabled={loading}
              >
                {loading ? 'Creating...' : 'Save Geofence'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
