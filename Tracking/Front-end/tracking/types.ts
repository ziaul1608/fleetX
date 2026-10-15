export interface SimulatorWaypoint {
  latitude: number;
  longitude: number;
  label?: string | null;
}

export interface ShipmentSummaryItem {
  id: number;
  trackingNumber: string;
  tripId: number | null;
  weightKg: number;
  status: string;
}

export interface SimulatorSession {
  vehicleId: number;
  licensePlate: string;
  vehicleMakeModel: string;
  vehicleStatus: string;
  tripId: number | null;
  tripCode: string | null;
  tripStatus: string | null;
  simulatorStatus: 'RUNNING' | 'PAUSED' | 'STOPPED' | 'IDLE';
  latitude: number;
  longitude: number;
  speedKmh: number;
  heading: number;
  currentOdometerKm: number;
  currentWaypointIndex: number;
  totalWaypoints: number;
  segmentProgress: number;
  tickIntervalSeconds: number;
  speedMultiplier: number;
  simulated: boolean;
  dataSource: string;
  lastUpdatedAt: string;
  waypoints: SimulatorWaypoint[];
  assignedShipments: ShipmentSummaryItem[];
}

export interface ControlTowerSummary {
  totalVehicles: number;
  activeVehiclesCount: number;
  vehiclesInTransit: number;
  idleVehiclesCount: number;
  vehiclesInMaintenance: number;
  offlineVehiclesCount: number;
  vehiclesByStatus: Record<string, number>;
  ongoingTripsCount: number;
  plannedTripsCount: number;
  completedTripsCount: number;
  tripsCompletedToday: number;
  tripsByStatus: Record<string, number>;
  totalShipmentsCount: number;
  delayedShipmentsCount: number;
  shipmentsByStatus: Record<string, number>;
  activeAlertsCount: number;
  criticalAlertsCount: number;
  totalAlertsCount: number;
  fleetFuelEfficiencyAvg: number;
  totalFuelCost: number;
  totalMaintenanceCost: number;
  unresolvedMaintenanceWarningsCount: number;
  activeSimulatorsCount: number;
  simulatedData: boolean;
  simulatedDataDisclaimer: string;
  status: string;
  timestamp: string;
}

export interface GeofenceItem {
  id: number;
  name: string;
  centerLatitude: number;
  centerLongitude: number;
  radiusMeters: number;
  fenceType: 'DEPOT' | 'CUSTOMER_HUB' | 'RESTRICTED' | 'CORRIDOR';
  shapeType: 'CIRCLE' | 'POLYGON';
  polygonCoordinates: number[][];
  speedLimitKmh: number | null;
  active: boolean;
  createdAt: string;
}

export interface GeofenceCreateRequest {
  name: string;
  centerLatitude: number;
  centerLongitude: number;
  radiusMeters: number;
  fenceType: string;
  shapeType: 'CIRCLE' | 'POLYGON';
  polygonCoordinates?: number[][];
  speedLimitKmh?: number | null;
}

export interface AlertItem {
  id: number;
  alertType: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  entityType: string;
  entityId: number;
  geofenceId: number | null;
  message: string;
  acknowledged: boolean;
  acknowledgedBy: string | null;
  acknowledgedAt: string | null;
  createdAt: string;
}

export interface ReportSummary {
  reportType: string;
  startDate: string | null;
  endDate: string | null;
  statusFilter: string | null;
  totalRecords: number;
  columns: string[];
  rows: Record<string, unknown>[];
  generatedAt: string;
}

export interface VehicleOption {
  id: number;
  licensePlate: string;
  make: string;
  model: string;
  status: string;
}
