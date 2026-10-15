# FleetX — Intelligent Fleet & Logistics Management System
## REST API & WebSocket Contracts Specification

**Document Reference:** FLEETX-API-2026-V1.0  
**Academic Year:** 2026–27  
**Baseline Date:** 10 October 2026  
**Status:** Approved Formal API Contract (Phase 0)

---

## 1. Global API Standards & Conventions

### 1.1 Base URI & Versioning
All REST endpoints are rooted under the versioned prefix:  
`https://<host>:<port>/api/v1`

### 1.2 Protocol & Serialization Standards
- **Serialization Format:** Strict JSON (`application/json; charset=UTF-8`).
- **Date/Time Standard:** ISO 8601 UTC format (`YYYY-MM-DD'T'HH:mm:ss'Z'`).
- **Identifier Conventions:**
  - Relational Database Keys: Positive 64-bit integers (`Long`), serialized as numbers (e.g., `42`).
  - Business Tracking Identifiers: Uppercase alphanumeric formatted strings:
    - Trips: `TRIP-YYYY-NNNN` (e.g., `TRIP-2026-1042`)
    - Shipments: `SHP-YYYY-NNNN` (e.g., `SHP-2026-5819`)
- **HTTP Methods:**
  - `GET`: Safe, idempotent read operations.
  - `POST`: Creation of resources or execution of commands.
  - `PUT`: Idempotent full replacement or state transitions.
  - `PATCH`: Partial resource updates.
  - `DELETE`: Resource deactivation or removal.

### 1.3 Uniform Response Envelopes

#### Success Envelope (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation completed successfully.",
  "data": { ... },
  "timestamp": "2026-10-10T12:00:00Z"
}
```

#### Paginated Envelope (`PageResponse<T>`)
```json
{
  "success": true,
  "message": "Page fetched successfully.",
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 85,
    "totalPages": 5,
    "last": false
  },
  "timestamp": "2026-10-10T12:00:00Z"
}
```

#### Standard Error Envelope (`ApiErrorResponse`)
```json
{
  "success": false,
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "One or more input fields failed validation constraints.",
  "path": "/api/v1/fleet/vehicles",
  "timestamp": "2026-10-10T12:00:00Z",
  "validationErrors": [
    {
      "field": "vin",
      "rejectedValue": "INVALID-VIN",
      "message": "VIN must be exactly 17 alphanumeric characters."
    }
  ]
}
```

---

## 2. Global Status Enums & Error Codes

### 2.1 Domain Enums
- **User Roles:** `ADMIN`, `FLEET_MANAGER`, `DISPATCHER`, `DRIVER`, `VIEWER`
- **Vehicle Status:** `AVAILABLE`, `IN_TRIP`, `MAINTENANCE`, `RETIRED`
- **Vehicle Type:** `TRUCK`, `VAN`, `CONTAINER`, `MOTORCYCLE`
- **Fuel Type:** `DIESEL`, `PETROL`, `ELECTRIC`
- **Maintenance Type:** `PREVENTIVE`, `REPAIR`, `INSPECTION`
- **Driver Availability:** `AVAILABLE`, `ON_DUTY`, `SUSPENDED`, `ON_LEAVE`
- **Trip Status:** `DRAFT`, `ASSIGNED`, `IN_TRANSIT`, `COMPLETED`, `CANCELLED`
- **Shipment Status:** `ORDERED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `RETURNED`
- **Stop Type:** `ORIGIN`, `INTERMEDIATE`, `DESTINATION`
- **Alert Severity:** `INFO`, `WARNING`, `CRITICAL`
- **Alert Type:** `GEOFENCE_BREACH`, `OVERSPEED`, `SERVICE_DUE`, `ROUTE_DEVIATION`

### 2.2 Standard Error Codes Catalog
| HTTP Code | Error Code | Description |
|---|---|---|
| 400 | `BAD_REQUEST` | Malformed JSON or unparseable parameters |
| 400 | `INVALID_STATE_TRANSITION` | Attempted invalid lifecycle transition |
| 401 | `UNAUTHORIZED` | Session expired or unauthenticated |
| 403 | `FORBIDDEN` | User lacks role authority for endpoint |
| 404 | `RESOURCE_NOT_FOUND` | Target entity does not exist |
| 409 | `RESOURCE_CONFLICT` | Unique key collision (duplicate VIN/license) |
| 422 | `UNPROCESSABLE_ENTITY` | Business rule failed (driver license expired) |
| 500 | `INTERNAL_SERVER_ERROR` | Unhandled server exception |

---

## 3. Module Endpoints & DTO Contracts

### 3.1 Module 1: Identity & Admin

#### `POST /api/v1/auth/login`
- **Security:** Public
- **Request Body:**
```json
{
  "email": "admin@fleetx.local",
  "password": "SecurePassword123!"
}
```
- **Validation Rules:** `@NotBlank @Email email`, `@NotBlank @Size(min=8) password`
- **Response `200 OK`:** (Also issues `Set-Cookie: JSESSIONID=...; HttpOnly; SameSite=Lax`)
```json
{
  "success": true,
  "message": "Authentication successful.",
  "data": {
    "userId": 1,
    "email": "admin@fleetx.local",
    "fullName": "System Administrator",
    "roles": ["ADMIN"]
  },
  "timestamp": "2026-10-10T12:00:00Z"
}
```

#### `POST /api/v1/auth/logout`
- **Security:** Authenticated
- **Response `200 OK`:** Invalidates session and clears cookie.

#### `GET /api/v1/auth/me`
- **Security:** Authenticated
- **Response `200 OK`:** Returns current session user details.

#### `GET /api/v1/admin/users`
- **Security:** `hasRole('ADMIN')`
- **Query Params:** `page=0`, `size=20`, `role=DRIVER`
- **Response `200 OK`:** `PageResponse<UserResponseDto>`

#### `POST /api/v1/admin/users`
- **Security:** `hasRole('ADMIN')`
- **Request Body:**
```json
{
  "email": "driver.john@fleetx.local",
  "password": "InitialPass123!",
  "fullName": "John Doe",
  "roles": ["DRIVER"]
}
```
- **Response `201 Created`:** Created user summary.

#### `GET /api/v1/admin/audit-logs`
- **Security:** `hasRole('ADMIN')`
- **Query Params:** `page=0`, `size=20`, `action=TRIP_DISPATCHED`
- **Response `200 OK`:** Paginated audit log records.

---

### 3.2 Module 2: Fleet & Assets

#### `GET /api/v1/fleet/vehicles`
- **Security:** Authenticated
- **Query Params:** `status=AVAILABLE`, `type=TRUCK`, `page=0`, `size=20`
- **Response `200 OK`:** Paginated list of vehicles.

#### `POST /api/v1/fleet/vehicles`
- **Security:** `hasAnyRole('ADMIN', 'FLEET_MANAGER')`
- **Request Body:**
```json
{
  "vin": "1HGCR2F83HA001234",
  "licensePlate": "MH-12-AB-1234",
  "make": "Tata",
  "model": "Prima 4028",
  "year": 2024,
  "type": "TRUCK",
  "fuelType": "DIESEL",
  "capacityKg": 25000.0,
  "capacityVolumeM3": 45.0,
  "initialOdometerKm": 12500.0
}
```
- **Validation Rules:**
  - `vin`: `@NotBlank @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$")`
  - `licensePlate`: `@NotBlank @Size(min=6, max=15)`
  - `capacityKg`: `@NotNull @Positive`
- **Response `201 Created`:** Vehicle record with assigned ID.

#### `POST /api/v1/fleet/vehicles/{id}/fuel-records`
- **Security:** `hasAnyRole('ADMIN', 'FLEET_MANAGER')`
- **Request Body:**
```json
{
  "liters": 150.0,
  "totalCost": 13500.00,
  "odometerAtFueling": 13100.0,
  "fuelStation": "HP Super Station, Pune Bypass",
  "fuelDate": "2026-10-10T08:30:00Z"
}
```
- **Response `201 Created`:** Created fuel record including calculated `efficiencyKmPerLiter`.

#### `POST /api/v1/fleet/vehicles/{id}/maintenance-records`
- **Security:** `hasAnyRole('ADMIN', 'FLEET_MANAGER')`
- **Request Body:**
```json
{
  "serviceType": "PREVENTIVE",
  "description": "Oil filter replacement & brake pad inspection",
  "cost": 18500.00,
  "serviceDate": "2026-10-10",
  "odometerAtService": 13200.0,
  "nextServiceDueKm": 23200.0,
  "nextServiceDueDate": "2027-04-10"
}
```
- **Response `201 Created`:** Maintenance record.

#### `GET /api/v1/fleet/warnings`
- **Security:** Authenticated
- **Response `200 OK`:** Vehicles within 500 km of service or overdue for maintenance.

---

### 3.3 Module 3: Driver & Trip Operations

#### `GET /api/v1/trips/drivers`
- **Security:** Authenticated
- **Query Params:** `availability=AVAILABLE`
- **Response `200 OK`:** List of driver summaries.

#### `POST /api/v1/trips/drivers`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER')`
- **Request Body:**
```json
{
  "userId": 5,
  "licenseNumber": "DL-1420240098765",
  "licenseCategory": "HEAVY",
  "licenseExpiryDate": "2028-12-31",
  "medicalCertExpiryDate": "2027-06-30"
}
```
- **Response `201 Created`:** Driver profile.

#### `POST /api/v1/trips`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER')`
- **Request Body:**
```json
{
  "vehicleId": 12,
  "driverId": 4,
  "originAddress": "Depot 1, Central Logistics Hub, Mumbai",
  "destinationAddress": "Regional Warehouse 4, Pune",
  "scheduledStartTime": "2026-10-11T06:00:00Z",
  "shipmentIds": [101, 102]
}
```
- **Validation:** Validates vehicle and driver availability before creating trip in `ASSIGNED` state.
- **Response `201 Created`:**
```json
{
  "success": true,
  "message": "Trip created and assets assigned.",
  "data": {
    "tripId": 78,
    "tripCode": "TRIP-2026-0078",
    "status": "ASSIGNED",
    "vehicleId": 12,
    "driverId": 4,
    "assignedShipmentCount": 2
  },
  "timestamp": "2026-10-10T12:00:00Z"
}
```

#### `PUT /api/v1/trips/{id}/status`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')`
- **Request Body:**
```json
{
  "targetStatus": "IN_TRANSIT",
  "remarks": "Driver departed origin depot."
}
```
- **Validation:** Must follow state machine (`DRAFT` → `ASSIGNED` → `IN_TRANSIT` → `COMPLETED`).
- **Response `200 OK`:** Updated trip status.

#### `GET /api/v1/trips/drivers/{id}/performance`
- **Security:** Authenticated
- **Response `200 OK`:** Driver safety score, completed trips, violation counts.

---

### 3.4 Module 4: Shipment & Routes

#### `POST /api/v1/shipments`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER')`
- **Request Body:**
```json
{
  "senderName": "Acme Electronics Ltd",
  "senderAddress": "Bhiwandi Hub, Thane",
  "receiverName": "Tech Retailers Pune",
  "receiverAddress": "Hinjawadi Phase 2, Pune",
  "weightKg": 1200.5,
  "volumeM3": 4.8,
  "specialInstructions": "Handle fragile electronic components."
}
```
- **Response `201 Created`:** Shipment record with generated `trackingNumber: "SHP-2026-0341"`.

#### `POST /api/v1/shipments/{id}/waypoints`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER')`
- **Request Body:**
```json
{
  "waypoints": [
    {
      "waypointName": "Origin Depot",
      "latitude": 19.2968,
      "longitude": 73.0631,
      "sequenceOrder": 1,
      "stopType": "ORIGIN"
    },
    {
      "waypointName": "Toll Checkpoint Expressway",
      "latitude": 18.9894,
      "longitude": 73.2562,
      "sequenceOrder": 2,
      "stopType": "INTERMEDIATE"
    },
    {
      "waypointName": "Destination Warehouse",
      "latitude": 18.5913,
      "longitude": 73.7389,
      "sequenceOrder": 3,
      "stopType": "DESTINATION"
    }
  ]
}
```
- **Response `200 OK`:** Saved waypoints list with calculated total route distance (km).

#### `GET /api/v1/shipments/{trackingNumber}/timeline`
- **Security:** Public
- **Response `200 OK`:** Ordered delivery timeline milestones.

---

### 3.5 Module 5: Tracking & Control Tower

#### `POST /api/v1/tracking/simulator/start`
- **Security:** `hasAnyRole('ADMIN', 'DISPATCHER')`
- **Request Body:**
```json
{
  "tripId": 78,
  "tickIntervalSeconds": 2,
  "speedMultiplier": 1.0
}
```
- **Response `200 OK`:** Simulator worker initiated.

#### `POST /api/v1/tracking/geofences`
- **Security:** `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')`
- **Request Body:**
```json
{
  "name": "Central Depot Mumbai",
  "centerLatitude": 19.2968,
  "centerLongitude": 73.0631,
  "radiusMeters": 500.0,
  "fenceType": "DEPOT"
}
```
- **Response `201 Created`:** Geofence definition.

#### `GET /api/v1/tracking/control-tower/summary`
- **Security:** Authenticated
- **Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "totalVehicles": 45,
    "vehiclesInTransit": 12,
    "vehiclesInMaintenance": 3,
    "activeAlertsCount": 2,
    "fleetFuelEfficiencyAvg": 4.12,
    "tripsCompletedToday": 18
  },
  "timestamp": "2026-10-10T12:00:00Z"
}
```

#### `GET /api/v1/tracking/reports/export-csv`
- **Security:** `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'VIEWER')`
- **Query Params:** `startDate=2026-10-01&endDate=2026-10-10&reportType=TRIPS`
- **Response Headers:** `Content-Type: text/csv`, `Content-Disposition: attachment; filename="fleetx_trips_report.csv"`
- **Response Body:** RFC 4180 CSV plain text payload.

---

## 4. Real-Time WebSocket & STOMP Protocol

### 4.1 WebSocket Connection Endpoint
- **Handshake URI:** `/ws/fleetx` (with SockJS fallback support).
- **Transport Security:** Inherits authenticated HTTP Session (`JSESSIONID`).

### 4.2 STOMP Channels & Payloads

#### 1. Vehicle Telemetry Channel (`/topic/vehicles/{vehicleId}`)
Broadcast every simulator tick for an active vehicle:
```json
{
  "vehicleId": 12,
  "tripId": 78,
  "latitude": 19.1425,
  "longitude": 73.1842,
  "speedKmh": 62.4,
  "heading": 142.5,
  "odometerKm": 12543.2,
  "timestamp": "2026-10-10T12:01:30Z"
}
```

#### 2. Fleet Overview Channel (`/topic/fleet-locations`)
Broadcast consolidated location pings for the Control Tower 2D/3D map.

#### 3. Operational Alerts Channel (`/topic/alerts`)
Broadcast upon geofence breaches or speed violations:
```json
{
  "alertId": 105,
  "alertType": "GEOFENCE_BREACH",
  "severity": "WARNING",
  "entityType": "VEHICLE",
  "entityId": 12,
  "message": "Vehicle MH-12-AB-1234 exited Depot boundary without clearance.",
  "timestamp": "2026-10-10T12:01:32Z"
}
```
