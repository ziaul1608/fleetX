# FleetX — Five-Member Module Ownership Specification (Phase B)

**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Team Structure:** 5 Students (Member 1 = Team Leader + Module 1 Owner; Members 2–5 = Module 2–5 Owners)  
**Golden Ownership Rule:** Zero concurrent file ownership. Every first-party file in the repository has exactly **one** designated owner at any point in the development lifecycle.

---

## Summary of Ownership Partitioning

| Member | Role & Module | Assigned Git Branch | Flyway Range | Backend Package Owned | Frontend Folder Owned |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Member 1 (Leader)** | Team Leader / Shared Baseline (`main`) + Module 1: Identity & Admin | `main` (Baseline & Integration) / `feature/identity-admin` (Module 1) | `V100`–`V199` | `com.fleetx.auth.*` (+ `com.fleetx.shared.*` as Leader) | `frontend/src/features/auth/` (+ `app/`, `shared/` as Leader) |
| **Member 2** | Module 2: Fleet & Asset Management | `feature/fleet-assets` | `V200`–`V299` | `com.fleetx.fleet.*` | `frontend/src/features/fleet/` |
| **Member 3** | Module 3: Driver & Trip Operations | `feature/driver-trip-ops` | `V300`–`V399` | `com.fleetx.drivers.*` | `frontend/src/features/drivers/` |
| **Member 4** | Module 4: Shipment & Route Management | `feature/shipment-route` | `V400`–`V499` | `com.fleetx.shipments.*` | `frontend/src/features/shipments/` |
| **Member 5** | Module 5: Tracking & Control Tower | `feature/tracking-control-tower` | `V500`–`V599` | `com.fleetx.tracking.*` | `frontend/src/features/tracking/` |

---

## 1. MEMBER 1 (TEAM LEADER) — Identity & Admin (`feature/identity-admin`)

### 1.1 Module Objective & Scope
Deliver session-based authentication, CSRF protection bootstrap, BCrypt password hashing, 5-role RBAC enforcement (`ADMIN`, `FLEET_MANAGER`, `DISPATCHER`, `DRIVER`, `VIEWER`), administrative user lifecycle management, sole-admin demotion/deactivation safeguards, and security audit logging.  
*(Note: Member 1 also acts as Team Leader maintaining the `main` branch shared baseline and Phase 7 cross-module integration tests; those shared responsibilities are separated in `MAIN_BRANCH_BASELINE.md` so they are never confused with Module 1 business files).*

### 1.2 Exact Existing Files Owned (36 Module-Exclusive Files)
- **Database Migrations (2 files):**
  - `backend/src/main/resources/db/migration/V100__init_identity_schema.sql`
  - `backend/src/main/resources/db/migration/V101__seed_roles_and_admin.sql`
- **Backend Java Source (`backend/src/main/java/com/fleetx/auth/` — 21 files):**
  - `config/AdminBootstrapRunner.java`
  - `config/DemoIdentitySeeder.java`
  - `controller/AuthController.java`
  - `controller/AdminUserController.java`
  - `dto/LoginRequest.java`
  - `dto/UserCreateRequest.java`
  - `dto/UserUpdateRequest.java`
  - `dto/UserStatusUpdateRequest.java`
  - `dto/AdminPasswordResetRequest.java`
  - `dto/UserResponseDto.java`
  - `dto/AuditLogResponseDto.java`
  - `entity/Role.java`
  - `entity/User.java`
  - `entity/AuditLog.java`
  - `repository/RoleRepository.java`
  - `repository/UserRepository.java`
  - `repository/AuditLogRepository.java`
  - `service/CustomUserDetailsService.java`
  - `service/AuditService.java`
  - `service/AuthService.java`
  - `service/AdminUserService.java`
- **Frontend Feature Source (`frontend/src/features/auth/` — 10 files):**
  - `types.ts`
  - `api/authApi.ts`
  - `pages/LoginPage.tsx`
  - `pages/UsersPage.tsx`
  - `pages/ProfilePage.tsx`
  - `components/CreateUserModal.tsx`
  - `components/EditUserModal.tsx`
  - `components/DeactivateConfirmModal.tsx`
  - `components/PasswordResetModal.tsx`
  - `routes.tsx`
- **Module-Specific Test Files (3 files):**
  - `backend/src/test/java/com/fleetx/auth/AuthLifecycleTest.java`
  - `backend/src/test/java/com/fleetx/auth/AdminUserOperationsTest.java`
  - `frontend/src/test/AuthComponents.test.tsx`

### 1.3 Proposed New Files (`NEW`)
- `.github/workflows/ci.yml` (**NEW** — Team Leader CI pipeline file on `main` to run Maven and Vitest suites on PRs).
- Zero new files required inside `com.fleetx.auth` or `frontend/src/features/auth/`.

### 1.4 API Endpoints Owned
- `GET /api/v1/auth/csrf` (and `/api/auth/csrf`)
- `POST /api/v1/auth/login` (and `/api/auth/login`)
- `POST /api/v1/auth/logout` (and `/api/auth/logout`)
- `GET /api/v1/auth/me` (and `/api/auth/me`)
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{id}`
- `POST /api/v1/admin/users`
- `PUT /api/v1/admin/users/{id}`
- `PUT | PATCH /api/v1/admin/users/{id}/status`
- `PUT | POST /api/v1/admin/users/{id}/password-reset`
- `GET /api/v1/admin/users/roles`
- `GET /api/v1/admin/users/audit-logs`

### 1.5 Shared Interfaces Provided / Consumed
- **Provides:** Spring Security `UserDetailsService` (`CustomUserDetailsService`) consumed by `SecurityConfig` to authenticate HTTP sessions and populate `ROLE_*` authorities for all modules.
- **Consumes:** `ApiResponse`, `PageResponse`, `DomainException`, `ResourceNotFoundException`, `ResourceConflictException` from `com.fleetx.shared`.

### 1.6 Allowed vs. Forbidden Changes & Acceptance Criteria
- **Allowed on `feature/identity-admin`:** Edit files in `backend/src/main/java/com/fleetx/auth/**`, `V100`–`V199` migrations, `frontend/src/features/auth/**`, and Module 1 tests.
- **Forbidden on `feature/identity-admin`:** Editing `com.fleetx.{fleet,drivers,shipments,tracking}` or `frontend/src/features/{fleet,drivers,shipments,tracking}`. Any shared infrastructure change must be committed and reviewed on `main` first.
- **Acceptance Criteria:** All 16 backend tests (`AuthLifecycleTest`, `AdminUserOperationsTest`) and 8 frontend tests (`AuthComponents.test.tsx`) pass; BCrypt hashes never leak in JSON responses; sole active `ADMIN` cannot self-deactivate or self-demote.

---

## 2. MEMBER 2 — Fleet & Asset Management (`feature/fleet-assets`)

### 2.1 Module Objective & Scope
Deliver commercial vehicle registration, search/filtering, validated operational status transitions (`AVAILABLE`, `ACTIVE`, `IDLE`, `IN_TRANSIT`, `IN_TRIP`, `MAINTENANCE`, `OFFLINE`, `RETIRED`), fuel logging with server-side cost and km/L efficiency calculation, maintenance scheduling, and automated `SERVICE_DUE` / `SERVICE_OVERDUE` warning emission.

### 2.2 Exact Existing Files Owned (45 Module-Exclusive Files)
- **Database Migrations (2 files):**
  - `backend/src/main/resources/db/migration/V200__init_fleet_schema.sql`
  - `backend/src/main/resources/db/migration/V201__fleet_module_enhancements.sql`
- **Backend Java Source (`backend/src/main/java/com/fleetx/fleet/` — 30 files):**
  - `controller/VehicleController.java`
  - `controller/FleetMaintenanceController.java`
  - `dto/VehicleCreateRequest.java`
  - `dto/VehicleUpdateRequest.java`
  - `dto/VehicleStatusUpdateRequest.java`
  - `dto/VehicleResponseDto.java`
  - `dto/FuelRecordCreateRequest.java`
  - `dto/FuelRecordResponseDto.java`
  - `dto/MaintenanceRecordCreateRequest.java`
  - `dto/MaintenanceRecordResponseDto.java`
  - `dto/ServiceWarningResponseDto.java`
  - `entity/Vehicle.java`
  - `entity/VehicleStatus.java`
  - `entity/VehicleType.java`
  - `entity/FuelType.java`
  - `entity/FuelRecord.java`
  - `entity/MaintenanceRecord.java`
  - `entity/MaintenanceStatus.java`
  - `entity/ServiceType.java`
  - `entity/ServiceWarning.java`
  - `entity/WarningSeverity.java`
  - `entity/WarningType.java`
  - `repository/VehicleRepository.java`
  - `repository/FuelRecordRepository.java`
  - `repository/MaintenanceRecordRepository.java`
  - `repository/ServiceWarningRepository.java`
  - `service/VehicleService.java`
  - `service/FuelService.java`
  - `service/MaintenanceService.java`
  - `service/DemoFleetSeeder.java`
- **Frontend Feature Source (`frontend/src/features/fleet/` — 10 files):**
  - `types.ts`
  - `api/fleetApi.ts`
  - `pages/VehiclesPage.tsx`
  - `pages/MaintenancePage.tsx`
  - `components/CreateVehicleModal.tsx`
  - `components/EditVehicleModal.tsx`
  - `components/VehicleDetailsModal.tsx`
  - `components/LogFuelModal.tsx`
  - `components/ScheduleMaintenanceModal.tsx`
  - `routes.tsx`
- **Module-Specific Test Files (3 files):**
  - `backend/src/test/java/com/fleetx/fleet/VehicleOperationsTest.java`
  - `backend/src/test/java/com/fleetx/fleet/FuelAndMaintenanceTest.java`
  - `frontend/src/test/FleetComponents.test.tsx`

### 2.3 Proposed New Files (`NEW`)
- Zero new files required; all 44 Module 2 files exist and are functional. (Future cleanup task: replace deprecated `@SpyBean` with `@MockitoSpyBean` in `FuelAndMaintenanceTest.java`).

### 2.4 API Endpoints Owned
- `GET /api/v1/fleet/vehicles`
- `GET /api/v1/fleet/vehicles/{id}`
- `POST /api/v1/fleet/vehicles`
- `PUT /api/v1/fleet/vehicles/{id}`
- `PATCH | PUT /api/v1/fleet/vehicles/{id}/status`
- `DELETE /api/v1/fleet/vehicles/{id}` (soft-deactivation)
- `POST /api/v1/fleet/vehicles/{vehicleId}/fuel-records`
- `GET /api/v1/fleet/vehicles/{vehicleId}/fuel-records`
- `GET /api/v1/fleet/vehicles/{vehicleId}/fuel-efficiency`
- `POST /api/v1/fleet/maintenance/records` (and `/api/v1/fleet/vehicles/{id}/maintenance`)
- `GET /api/v1/fleet/maintenance/records`
- `PATCH /api/v1/fleet/maintenance/records/{id}/complete`
- `GET /api/v1/fleet/maintenance/warnings`
- `PATCH /api/v1/fleet/maintenance/warnings/{id}/resolve`

### 2.5 Shared Interfaces Provided / Consumed
- **Provides (`@Primary`):** `VehicleLookupService` (implemented by `VehicleService`), called by Module 3 (`TripService`) and Module 5 (`TrackingService`).
- **Consumes:** `AlertPublisher` (injected into `MaintenanceService` to publish `SERVICE_DUE` and `SERVICE_OVERDUE` alerts to Module 5; falls back to `SharedFallbackConfiguration` when running standalone).

### 2.6 Allowed vs. Forbidden Changes & Acceptance Criteria
- **Allowed:** Modify only `backend/src/main/java/com/fleetx/fleet/**`, `V200`–`V299` migrations, `frontend/src/features/fleet/**`, and Module 2 test files.
- **Forbidden:** Direct imports from `com.fleetx.{auth,drivers,shipments,tracking}`; editing `pom.xml`, `package.json`, or `frontend/src/app/routes.tsx`.
- **Acceptance Criteria:** All 12 backend tests (`VehicleOperationsTest`, `FuelAndMaintenanceTest`) and 5 frontend tests (`FleetComponents.test.tsx`) pass; duplicate VIN/plate returns `409 Conflict`; fuel efficiency handles initial log and zero distance delta without division-by-zero.

---

## 3. MEMBER 3 — Driver & Trip Operations (`feature/driver-trip-ops`)

### 3.1 Module Objective & Scope
Deliver commercial driver onboarding, duplicate licence prevention, licence & medical expiry validation, role-based PII masking for `VIEWER` accounts, trip dispatch (`PLANNED -> ASSIGNED -> IN_PROGRESS -> COMPLETED / CANCELLED`), vehicle availability verification via `VehicleLookupService`, overlapping trip conflict guards, and automated driver performance scoring.

### 3.2 Exact Existing Files Owned (41 Module-Exclusive Files)
- **Database Migrations (2 files):**
  - `backend/src/main/resources/db/migration/V300__init_driver_trip_schema.sql`
  - `backend/src/main/resources/db/migration/V301__driver_trip_enhancements.sql`
- **Backend Java Source (`backend/src/main/java/com/fleetx/drivers/` — 25 files):**
  - `controller/DriverController.java`
  - `controller/TripController.java`
  - `dto/DriverCreateRequest.java`
  - `dto/DriverUpdateRequest.java`
  - `dto/DriverStatusUpdateRequest.java`
  - `dto/DriverResponseDto.java`
  - `dto/DriverPerformanceResponseDto.java`
  - `dto/TripCreateRequest.java`
  - `dto/TripStatusUpdateRequest.java`
  - `dto/TripResponseDto.java`
  - `dto/TripStatusHistoryResponseDto.java`
  - `entity/Driver.java`
  - `entity/DriverAvailability.java`
  - `entity/DriverPerformanceMetrics.java`
  - `entity/Trip.java`
  - `entity/TripStatus.java`
  - `entity/TripStatusHistory.java`
  - `repository/DriverRepository.java`
  - `repository/DriverPerformanceMetricsRepository.java`
  - `repository/TripRepository.java`
  - `repository/TripStatusHistoryRepository.java`
  - `service/DriverService.java`
  - `service/TripService.java`
  - `service/DriverPerformanceService.java`
  - `service/DemoDriverTripSeeder.java`
- **Frontend Feature Source (`frontend/src/features/drivers/` — 10 files):**
  - `types.ts`
  - `api/driverApi.ts`
  - `pages/DriversPage.tsx`
  - `pages/TripsPage.tsx`
  - `components/CreateDriverModal.tsx`
  - `components/EditDriverModal.tsx`
  - `components/DriverDetailsModal.tsx`
  - `components/CreateTripModal.tsx`
  - `components/TripLifecycleModal.tsx`
  - `routes.tsx`
- **Module-Specific Test Files (4 files):**
  - `backend/src/test/java/com/fleetx/drivers/DriverOperationsTest.java`
  - `backend/src/test/java/com/fleetx/drivers/TripLifecycleOperationsTest.java`
  - `backend/src/test/java/com/fleetx/drivers/DriverPerformanceTest.java`
  - `frontend/src/test/DriverComponents.test.tsx`

### 3.3 Proposed New Files (`NEW`)
- Zero new files required; all 41 Module 3 files exist and are functional.

### 3.4 API Endpoints Owned
- `GET /api/v1/trips/drivers` (and `/api/v1/drivers`)
- `GET /api/v1/trips/drivers/{id}`
- `POST /api/v1/trips/drivers`
- `PUT /api/v1/trips/drivers/{id}`
- `PATCH /api/v1/trips/drivers/{id}/availability`
- `DELETE /api/v1/trips/drivers/{id}`
- `GET /api/v1/trips/drivers/{id}/performance`
- `GET /api/v1/trips/drivers/{id}/trips`
- `GET /api/v1/trips`
- `GET /api/v1/trips/{id}`
- `POST /api/v1/trips`
- `POST /api/v1/trips/{id}/assign`
- `POST /api/v1/trips/{id}/start`
- `POST /api/v1/trips/{id}/complete`
- `POST /api/v1/trips/{id}/cancel`
- `PATCH /api/v1/trips/{id}/status`
- `GET /api/v1/trips/{id}/history`

### 3.5 Shared Interfaces Provided / Consumed
- **Provides (`@Primary`):**
  - `DriverLookupService` (implemented by `DriverService`)
  - `TripLookupService` (implemented by `TripService`, consumed by Module 4 `ShipmentService` and Module 5 `TrackingService`)
- **Consumes:**
  - `VehicleLookupService` (to verify vehicle existence and operational readiness before assigning/starting a trip; never accesses `VehicleRepository` directly)
  - `AlertPublisher` (to publish operational alerts on trip/driver compliance events)

### 3.6 Allowed vs. Forbidden Changes & Acceptance Criteria
- **Allowed:** Modify only `backend/src/main/java/com/fleetx/drivers/**`, `V300`–`V399` migrations (`Note: V300 references users(id) from V100 and vehicles(id) from V200`), `frontend/src/features/drivers/**`, and Module 3 tests.
- **Forbidden:** Direct imports from `com.fleetx.{auth,fleet,shipments,tracking}`.
- **Acceptance Criteria:** All 27 backend tests (`DriverOperationsTest`, `TripLifecycleOperationsTest`, `DriverPerformanceTest`) and 6 frontend tests (`DriverComponents.test.tsx`) pass; expired driver licence blocks trip assignment; `VIEWER` receives masked phone/email/licence fields.

---

## 4. MEMBER 4 — Shipment & Route Management (`feature/shipment-route`)

### 4.1 Module Objective & Scope
Deliver freight shipment creation, search/filtering, ordered route waypoint management, great-circle Haversine straight-line distance and configurable-speed ETA calculation (explicitly labelled `Estimated`), trip-to-shipment association via `TripLookupService`, shipment lifecycle status transitions (`CREATED -> ASSIGNED -> IN_TRANSIT -> DELIVERED / DELAYED / CANCELLED`), chronological timeline events, and delayed-shipment alert emission.

### 4.2 Exact Existing Files Owned (38 Module-Exclusive Files)
- **Database Migrations (2 files):**
  - `backend/src/main/resources/db/migration/V400__init_shipment_route_schema.sql`
  - `backend/src/main/resources/db/migration/V401__shipment_route_enhancements.sql`
- **Backend Java Source (`backend/src/main/java/com/fleetx/shipments/` — 23 files):**
  - `controller/ShipmentController.java`
  - `dto/ShipmentCreateRequest.java`
  - `dto/ShipmentUpdateRequest.java`
  - `dto/ShipmentAssignTripRequest.java`
  - `dto/ShipmentStatusUpdateRequest.java`
  - `dto/ShipmentDeliverRequest.java`
  - `dto/ShipmentResponseDto.java`
  - `dto/WaypointItemRequest.java`
  - `dto/WaypointsUpdateRequest.java`
  - `dto/WaypointResponseDto.java`
  - `dto/RoutePlanResponseDto.java`
  - `dto/ShipmentTimelineEventDto.java`
  - `entity/Shipment.java`
  - `entity/ShipmentStatus.java`
  - `entity/ShipmentWaypoint.java`
  - `entity/StopType.java`
  - `entity/ShipmentTimelineEvent.java`
  - `repository/ShipmentRepository.java`
  - `repository/ShipmentWaypointRepository.java`
  - `repository/ShipmentTimelineEventRepository.java`
  - `service/RouteCalculationService.java`
  - `service/ShipmentService.java`
  - `service/DemoShipmentSeeder.java`
- **Frontend Feature Source (`frontend/src/features/shipments/` — 10 files):**
  - `types.ts`
  - `api/shipmentApi.ts`
  - `pages/ShipmentsPage.tsx`
  - `pages/WaypointsPage.tsx`
  - `components/CreateShipmentModal.tsx`
  - `components/EditShipmentModal.tsx`
  - `components/AssignTripModal.tsx`
  - `components/RouteWaypointsModal.tsx`
  - `components/ShipmentDetailsModal.tsx`
  - `routes.tsx`
- **Module-Specific Test Files (3 files):**
  - `backend/src/test/java/com/fleetx/shipments/ShipmentLifecycleAndTripAssociationTest.java`
  - `backend/src/test/java/com/fleetx/shipments/RouteAndDelayedShipmentTest.java`
  - `frontend/src/test/ShipmentComponents.test.tsx`

### 4.3 Proposed New Files (`NEW`)
- Zero new files required; all 38 Module 4 files exist and are functional.

### 4.4 API Endpoints Owned
- `GET /api/v1/shipments`
- `GET /api/v1/shipments/{id}`
- `GET /api/v1/shipments/tracking/{trackingNumber}`
- `POST /api/v1/shipments`
- `PUT /api/v1/shipments/{id}`
- `PATCH | POST /api/v1/shipments/{id}/assign-trip`
- `PATCH | PUT /api/v1/shipments/{id}/status`
- `POST /api/v1/shipments/{id}/deliver`
- `PUT | POST /api/v1/shipments/{id}/waypoints`
- `GET /api/v1/shipments/{id}/waypoints`
- `GET /api/v1/shipments/{id}/timeline`
- `POST /api/v1/shipments/check-delayed`

### 4.5 Shared Interfaces Provided / Consumed
- **Provides (`@Primary`):** `ShipmentLookupService` (implemented by `ShipmentService`), consumed by Module 5 (`TrackingService`) to associate active shipments with tracked trips.
- **Consumes:**
  - `TripLookupService` (to verify trip existence and prevent linking shipments to `COMPLETED` or `CANCELLED` trips)
  - `AlertPublisher` (to emit `SHIPMENT_DELAYED` alerts when promised delivery times are breached or status transitions to `DELAYED`)

### 4.6 Allowed vs. Forbidden Changes & Acceptance Criteria
- **Allowed:** Modify only `backend/src/main/java/com/fleetx/shipments/**`, `V400`–`V499` migrations (`Note: V400 references trips(id) from V300`), `frontend/src/features/shipments/**`, and Module 4 tests.
- **Forbidden:** Direct imports from `com.fleetx.{auth,fleet,drivers,tracking}`; claiming Haversine distance is exact turn-by-turn road routing.
- **Acceptance Criteria:** All 13 backend tests (`ShipmentLifecycleAndTripAssociationTest`, `RouteAndDelayedShipmentTest`) and 5 frontend tests (`ShipmentComponents.test.tsx`) pass; distance and ETA responses explicitly include `Haversine` and `Estimated` labels.

---

## 5. MEMBER 5 — Tracking & Control Tower (`feature/tracking-control-tower`)

### 5.1 Module Objective & Scope
Deliver the deterministic/repeatable `SimulatedGpsService` interpolating coordinates along route waypoints (`SIMULATED DATA`), real-time STOMP WebSocket broadcasting (`/topic/vehicles/{id}`, `/topic/fleet-locations`, `/topic/alerts`), circular and polygonal (ray-casting) geofencing with enter/exit transition deduplication, alert management and acknowledgement, Leaflet 2D live mapping, stylized Three.js 3D fleet visualization, Control Tower KPI aggregation, and formula-sanitized RFC 4180 CSV export.

### 5.2 Exact Existing Files Owned (36 Module-Exclusive Files)
- **Database Migrations (2 files):**
  - `backend/src/main/resources/db/migration/V500__init_tracking_geofence_schema.sql`
  - `backend/src/main/resources/db/migration/V501__tracking_control_tower_enhancements.sql`
- **Backend Java Source (`backend/src/main/java/com/fleetx/tracking/` — 23 files):**
  - `controller/TrackingController.java`
  - `dto/TelemetryPingDto.java`
  - `dto/SimulatorWaypointDto.java`
  - `dto/SimulatorStartRequest.java`
  - `dto/SimulatorSessionDto.java`
  - `dto/GeofenceCreateRequest.java`
  - `dto/GeofenceResponseDto.java`
  - `dto/AlertResponseDto.java`
  - `dto/ReportSummaryDto.java`
  - `entity/TrackingLog.java`
  - `entity/Geofence.java`
  - `entity/GeofenceVehicleState.java`
  - `entity/Alert.java`
  - `entity/ControlTowerKpi.java`
  - `repository/TrackingRepository.java`
  - `repository/GeofenceRepository.java`
  - `repository/GeofenceVehicleStateRepository.java`
  - `repository/AlertRepository.java`
  - `repository/ControlTowerKpiRepository.java`
  - `repository/ControlTowerReadRepository.java`
  - `service/GeofenceAndTelemetryMathService.java`
  - `service/TrackingService.java`
  - `service/DemoTrackingSeeder.java`
- **Frontend Feature Source (`frontend/src/features/tracking/` — 8 files):**
  - `types.ts`
  - `api/trackingApi.ts`
  - `components/LeafletFleetMap.tsx`
  - `components/ThreeFleetScene.tsx`
  - `components/CreateGeofenceModal.tsx`
  - `pages/ControlTowerPage.tsx`
  - `pages/ReportsPage.tsx`
  - `routes.tsx`
- **Module-Specific Test Files (3 files):**
  - `backend/src/test/java/com/fleetx/tracking/SimulationAndGeofenceTest.java`
  - `backend/src/test/java/com/fleetx/tracking/ControlTowerAlertAndReportTest.java`
  - `frontend/src/test/TrackingComponents.test.tsx`

### 5.3 Proposed New Files (`NEW`)
- Zero new files required; all 36 Module 5 files exist and are functional.

### 5.4 API Endpoints & WebSocket Topics Owned
- `GET /api/v1/tracking/control-tower/summary`
- `GET /api/v1/tracking/simulator/sessions` (and `/simulator/status`)
- `POST /api/v1/tracking/simulator/start`
- `POST /api/v1/tracking/simulator/{vehicleId}/pause`
- `POST /api/v1/tracking/simulator/{vehicleId}/resume`
- `POST /api/v1/tracking/simulator/{vehicleId}/stop`
- `POST /api/v1/tracking/simulator/{vehicleId}/step`
- `POST /api/v1/tracking/telemetry`
- `GET /api/v1/tracking/vehicles/{vehicleId}/history`
- `POST /api/v1/tracking/geofences`
- `GET /api/v1/tracking/geofences`
- `PATCH /api/v1/tracking/geofences/{id}/toggle`
- `GET /api/v1/tracking/alerts`
- `POST | PATCH /api/v1/tracking/alerts/{id}/acknowledge`
- `GET /api/v1/tracking/reports/summary`
- `GET /api/v1/tracking/reports/export-csv`
- **WebSocket Endpoint:** `/ws/fleetx` (configured in shared `WebSocketConfig`)
- **STOMP Topics Broadcast:** `/topic/vehicles/{vehicleId}`, `/topic/fleet-locations`, `/topic/alerts`

### 5.5 Shared Interfaces Provided / Consumed
- **Provides (`@Primary`):** `AlertPublisher` (implemented by `TrackingService`), consumed by Module 2 (`MaintenanceService`), Module 3 (`TripService`), and Module 4 (`ShipmentService`).
- **Consumes:** `VehicleLookupService`, `DriverLookupService`, `TripLookupService`, `ShipmentLookupService` (plus read-only SQL aggregation in `ControlTowerReadRepository`).

### 5.6 Allowed vs. Forbidden Changes & Acceptance Criteria
- **Allowed:** Modify only `backend/src/main/java/com/fleetx/tracking/**`, `V500`–`V599` migrations, `frontend/src/features/tracking/**`, and Module 5 tests.
- **Forbidden:** Direct Java imports from `com.fleetx.{auth,fleet,drivers,shipments}`; claiming telemetry is real hardware GPS.
- **Acceptance Criteria:** All 10 backend tests (`SimulationAndGeofenceTest`, `ControlTowerAlertAndReportTest`) and 4 frontend tests (`TrackingComponents.test.tsx`) pass; stationary vehicles and duplicate alerts are handled cleanly; CSV export escapes commas, quotes, newlines, and leading `=`, `+`, `-`, `@` characters.
