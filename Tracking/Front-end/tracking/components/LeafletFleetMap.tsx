import React from 'react';
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Polyline,
  Circle,
  Polygon,
  Popup,
} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Navigation, Shield } from 'lucide-react';
import { GeofenceItem, SimulatorSession } from '../types';

interface LeafletFleetMapProps {
  sessions: SimulatorSession[];
  selectedVehicleId: number | null;
  onSelectVehicle: (vehicleId: number) => void;
  geofences: GeofenceItem[];
}

export const LeafletFleetMap: React.FC<LeafletFleetMapProps> = ({
  sessions,
  selectedVehicleId,
  onSelectVehicle,
  geofences,
}) => {
  const selectedSession =
    sessions.find((s) => s.vehicleId === selectedVehicleId) || sessions[0] || null;

  const mapCenter: [number, number] = selectedSession
    ? [selectedSession.latitude, selectedSession.longitude]
    : [18.85, 73.35];

  const routePositions: [number, number][] =
    selectedSession?.waypoints?.map((wp) => [wp.latitude, wp.longitude]) ?? [];

  // Detect headless JSDOM or missing SVG geometry engine to provide a clean accessible fallback
  const isHeadlessFallback =
    typeof navigator !== 'undefined' && /jsdom/i.test(navigator.userAgent || '');

  return (
    <div className="position-relative d-flex flex-column h-100">
      {/* Explicit Simulated Data & OpenStreetMap Attribution Banner */}
      <div
        className="d-flex flex-wrap justify-content-between align-items-center px-3 py-2 border-bottom border-secondary border-opacity-25 small"
        style={{ background: 'rgba(15, 23, 42, 0.92)' }}
      >
        <div className="d-flex align-items-center gap-2">
          <span className="badge bg-warning text-dark fw-bold">
            SIMULATED DATA
          </span>
          <span className="text-secondary">
            Simulated GPS Waypoint Interpolation — Not Physical Hardware GPS
          </span>
        </div>
        <div className="text-secondary">
          Tiles: &copy;{' '}
          <a
            href="https://www.openstreetmap.org/copyright"
            target="_blank"
            rel="noreferrer"
            className="text-info text-decoration-none"
          >
            OpenStreetMap
          </a>{' '}
          contributors
        </div>
      </div>

      {/* Interactive Leaflet Map Container or Accessible Map Fallback */}
      <div style={{ height: '380px', width: '100%' }} data-testid="leaflet-fleet-map">
        {!isHeadlessFallback ? (
          <MapContainer
            center={mapCenter}
            zoom={9}
            style={{ height: '100%', width: '100%', background: '#0b1325' }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Active Geofences (Circular and Polygonal) */}
            {geofences
              .filter((g) => g.active)
              .map((fence) => {
                const color =
                  fence.fenceType === 'RESTRICTED'
                    ? '#ef4444'
                    : fence.fenceType === 'CUSTOMER_HUB'
                    ? '#f59e0b'
                    : '#38bdf8';

                if (
                  fence.shapeType === 'POLYGON' &&
                  fence.polygonCoordinates &&
                  fence.polygonCoordinates.length >= 3
                ) {
                  const positions: [number, number][] = fence.polygonCoordinates.map(
                    (pt) => [pt[0], pt[1]]
                  );
                  return (
                    <Polygon
                      key={`poly-${fence.id}`}
                      positions={positions}
                      pathOptions={{ color, fillOpacity: 0.18, weight: 2 }}
                    >
                      <Popup>
                        <strong>{fence.name}</strong> ({fence.fenceType} - POLYGON)
                      </Popup>
                    </Polygon>
                  );
                }

                return (
                  <Circle
                    key={`circle-${fence.id}`}
                    center={[fence.centerLatitude, fence.centerLongitude]}
                    radius={fence.radiusMeters}
                    pathOptions={{ color, fillOpacity: 0.16, weight: 2 }}
                  >
                    <Popup>
                      <strong>{fence.name}</strong> ({fence.fenceType} -{' '}
                      {fence.radiusMeters}m)
                    </Popup>
                  </Circle>
                );
              })}

            {/* Selected Vehicle Route Waypoints Polyline */}
            {routePositions.length >= 2 && (
              <Polyline
                positions={routePositions}
                pathOptions={{ color: '#38bdf8', weight: 4, dashArray: '8 6' }}
              />
            )}

            {/* Waypoint Stop Markers for Selected Vehicle */}
            {selectedSession?.waypoints?.map((wp, idx) => (
              <CircleMarker
                key={`wp-${idx}`}
                center={[wp.latitude, wp.longitude]}
                radius={5}
                pathOptions={{
                  color: '#cbd5e1',
                  fillColor: '#0f172a',
                  fillOpacity: 0.9,
                  weight: 2,
                }}
              >
                <Popup>
                  <div>
                    <strong>Stop #{idx + 1}:</strong> {wp.label || 'Waypoint'}
                  </div>
                  <div className="small">
                    [{wp.latitude.toFixed(4)}, {wp.longitude.toFixed(4)}]
                  </div>
                </Popup>
              </CircleMarker>
            ))}

            {/* Moving Vehicle Markers */}
            {sessions.map((s) => {
              const isSelected = s.vehicleId === selectedSession?.vehicleId;
              const markerColor =
                s.simulatorStatus === 'RUNNING'
                  ? '#10b981'
                  : s.simulatorStatus === 'PAUSED'
                  ? '#f59e0b'
                  : '#64748b';

              return (
                <CircleMarker
                  key={`veh-${s.vehicleId}`}
                  center={[s.latitude, s.longitude]}
                  radius={isSelected ? 10 : 7}
                  pathOptions={{
                    color: isSelected ? '#38bdf8' : '#ffffff',
                    fillColor: markerColor,
                    fillOpacity: 0.95,
                    weight: isSelected ? 3 : 2,
                  }}
                  eventHandlers={{
                    click: () => onSelectVehicle(s.vehicleId),
                  }}
                >
                  <Popup>
                    <div>
                      <strong>{s.licensePlate}</strong> ({s.simulatorStatus})
                    </div>
                    <div className="small">
                      Speed: {s.speedKmh.toFixed(1)} km/h | Heading: {s.heading.toFixed(0)}°
                    </div>
                    <div className="small">
                      Coords: {s.latitude.toFixed(4)}, {s.longitude.toFixed(4)}
                    </div>
                    <div className="small text-warning">SIMULATED GPS STREAM</div>
                  </Popup>
                </CircleMarker>
              );
            })}
          </MapContainer>
        ) : (
          <div
            className="h-100 d-flex flex-column justify-content-center align-items-center p-4 text-center"
            style={{ background: '#0b1325' }}
          >
            <MapPin size={36} className="text-info mb-2" />
            <h6 className="fw-bold text-light mb-1">
              OpenStreetMap / React Leaflet Geospatial Viewport (Accessible Fallback)
            </h6>
            <p className="text-secondary small mb-2">
              Center: [{mapCenter[0].toFixed(4)}, {mapCenter[1].toFixed(4)}] ·{' '}
              {sessions.length} Vehicle Marker(s) · {geofences.filter((g) => g.active).length}{' '}
              Active Geofence(s)
            </p>
            {selectedSession && selectedSession.waypoints?.length > 0 && (
              <div className="d-flex flex-wrap justify-content-center gap-2 small text-info">
                <Navigation size={14} />
                {selectedSession.waypoints.map((wp, idx) => (
                  <span key={idx} className="badge bg-dark border border-secondary">
                    #{idx + 1} {wp.label || 'Stop'} ({wp.latitude.toFixed(2)},{' '}
                    {wp.longitude.toFixed(2)})
                  </span>
                ))}
              </div>
            )}
            {geofences.length > 0 && (
              <div className="d-flex flex-wrap justify-content-center gap-2 small mt-2">
                <Shield size={14} className="text-warning" />
                {geofences.map((g) => (
                  <span key={g.id} className="badge bg-secondary">
                    {g.name} ({g.shapeType})
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Accessible Responsive Vehicle Telemetry Strip */}
      <div
        className="px-3 py-2 border-top border-secondary border-opacity-25 small"
        style={{ background: 'rgba(15, 23, 42, 0.92)' }}
      >
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
          <span className="text-secondary fw-semibold">
            Active Simulated Vehicles ({sessions.length}):
          </span>
          <div className="d-flex flex-wrap gap-2">
            {sessions.length === 0 ? (
              <span className="text-secondary">
                No active simulated vehicles yet — start a simulator above.
              </span>
            ) : (
              sessions.map((s) => (
                <button
                  key={s.vehicleId}
                  type="button"
                  onClick={() => onSelectVehicle(s.vehicleId)}
                  className={`btn btn-sm py-0 px-2 ${
                    s.vehicleId === selectedSession?.vehicleId
                      ? 'btn-info text-dark fw-semibold'
                      : 'btn-outline-secondary text-light'
                  }`}
                >
                  {s.licensePlate} ({s.speedKmh.toFixed(0)} km/h · {s.simulatorStatus})
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
