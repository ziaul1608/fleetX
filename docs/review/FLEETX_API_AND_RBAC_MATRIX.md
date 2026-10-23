# FleetX — Complete REST API Catalogue & Role-Based Access Control (RBAC) Matrix

**Document:** REST API Specifications & Authorization Enforcement Inventory  
**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Audit Date:** 2026-10-23  
**Status:** VERIFIED from Spring Controllers, Security Configuration, and Test Suites  

---

## 1. Executive Summary & Security Architecture

FleetX enforces a **dual-layer defense-in-depth security architecture**:
1. **Perimeter / URL Matcher Layer (`SecurityConfig.java`):**
   - Public Whitelist: `/api/health`, `/api/v1/auth/login`, `/api/v1/auth/csrf`, `/ws/fleetx/**`, and static SPA assets (`/`, `/assets/**`, `/index.html`).
   - Administrative Restriction: All `/api/v1/admin/**` and `/api/admin/**` paths strictly require `hasRole('ADMIN')`.
   - Default Deny: All other endpoints require an authenticated `SecurityContext` (`anyRequest().authenticated()`).
2. **Method-Level Authorization Layer (`@EnableMethodSecurity`):**
   - Fine-grained `@PreAuthorize("hasRole(...)")` or `@PreAuthorize("hasAnyRole(...)")` annotations on controller methods.
   - Prevents privilege escalation even if a client attempts direct API access bypassing the React frontend.
3. **Session & CSRF Model:**
   - Stateful server-side session identified by HttpOnly `JSESSIONID` cookie (`SameSite=Lax`, `Secure` in production).
   - Double-Submit CSRF protection using `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` request header on all state-changing HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`).

---

## 2. Canonical Roles & Permission Policy

FleetX defines **five canonical application roles** (persisted in the `roles` table):

| Role Identifier | Intended Operational Persona | Permitted Capabilities in FleetX | Prohibited Capabilities |
| :--- | :--- | :--- | :--- |
| **`ROLE_ADMIN`** | System Administrator | Full administrative privileges: user provisioning, role editing, account deactivation, password overrides, audit logs viewing, and full operational access. | None (system superuser). |
| **`ROLE_FLEET_MANAGER`** | Fleet & Asset Supervisor | Registering vehicles, updating vehicle specifications, logging fuel, scheduling maintenance, managing geofences, dispatching trips, and viewing all operational records. | Cannot provision users, edit staff roles, or override passwords. |
| **`ROLE_DISPATCHER`** | Logistics Dispatcher | Onboarding drivers, managing driver availability, creating trips, dispatching trips, managing shipment manifests, assigning shipments to trips, configuring waypoints, running GPS simulator, and acknowledging alerts. | Cannot provision users, modify vehicle baseline assets, or edit system roles. |
| **`ROLE_DRIVER`** | Commercial Vehicle Operator | Operational execution: updating trip status (IN_PROGRESS, COMPLETED), logging fuel fill-ups, confirming shipment delivery, ingesting telemetry, and viewing own profile. | Cannot dispatch trips, onboard drivers, register vehicles, create geofences, or manage users. |
| **`ROLE_VIEWER`** | Read-Only Stakeholder / Auditor | Read-only inspection: viewing vehicles, drivers, trips, shipments, control tower KPI cards, active map, and exporting CSV reports. | Strictly prohibited from executing ANY state-changing operation (`POST`, `PUT`, `PATCH`, `DELETE`). Backend returns HTTP 403 Forbidden. |

---

## 3. Comprehensive REST Endpoint Inventory

### 3.1 Module 1: Authentication & Identity Management

| HTTP Method & Path | Controller & Handler Method | Service Method | Authorized Roles / Precondition | Request Payload / DTO | Response Envelope / DTO | Database Effects | Frontend Consumer | Existing Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/health` | `HealthController.getHealth()` | None (Environment check) | Public / Anonymous | None | `ApiResponse<Map<String, Object>>` | None | Smoke test, Render check | `HealthControllerTest.java`, `smoke_test.sh` [1/10] |
| `GET /api/v1/auth/csrf` | `AuthController.getCsrfToken()` | Spring Security CsrfToken | Public / Anonymous | None | `ApiResponse<Map<String, String>>` | Generates CSRF token | `LoginPage.tsx` | `AuthLifecycleTest.java`, `smoke_test.sh` [5/10] |
| `POST /api/v1/auth/login` | `AuthController.login()` | `AuthService.login()` | Public / Anonymous | `LoginRequest` (`email`, `password`) | `ApiResponse<UserResponseDto>` | Authenticates against `users`, writes `USER_LOGIN_SUCCESS` to `audit_logs` | `LoginPage.tsx` | `AuthLifecycleTest.java`, `smoke_test.sh` [6/10] |
| `GET /api/v1/auth/me` | `AuthController.getCurrentUser()` | `AuthService.getCurrentUser()` | Authenticated (`anyRequest()`) | None | `ApiResponse<UserResponseDto>` | Queries `users` and `roles` | `AuthContext.tsx` on page mount | `AuthLifecycleTest.java`, `smoke_test.sh` [7/10] |
| `POST /api/v1/auth/logout` | `AuthController.logout()` | `AuthService.logout()` | Authenticated | None | `ApiResponse<Void>` | Invalidates `HttpSession`, writes `USER_LOGOUT` to `audit_logs` | `TopNav.tsx` | `AuthLifecycleTest.java`, `smoke_test.sh` [10/10] |
| `GET /api/v1/admin/users` | `AdminUserController.listUsers()` | `AdminUserService.getUsers()` | `hasRole('ADMIN')` | Params: `role`, `active`, `search`, `page`, `size`, `sort` | `ApiResponse<PageResponse<UserResponseDto>>` | Queries `users`, `user_roles` | `UsersPage.tsx` | `AdminUserOperationsTest.java`, `smoke_test.sh` [8/10] |
| `GET /api/v1/admin/users/{id}` | `AdminUserController.getUserById()` | `AdminUserService.getUserById()` | `hasRole('ADMIN')` | Path variable `id` | `ApiResponse<UserResponseDto>` | Queries `users` | `UsersPage.tsx` | `AdminUserOperationsTest.java` |
| `POST /api/v1/admin/users` | `AdminUserController.createUser()` | `AdminUserService.createUser()` | `hasRole('ADMIN')` | `UserCreateRequest` (`email`, `fullName`, `password`, `role`) | `ApiResponse<UserResponseDto>` | Inserts into `users`, `user_roles`; writes to `audit_logs` | `CreateUserModal.tsx` | `AdminUserOperationsTest.java` |
| `PUT /api/v1/admin/users/{id}` | `AdminUserController.updateUser()` | `AdminUserService.updateUser()` | `hasRole('ADMIN')` | `UserUpdateRequest` (`fullName`, `role`) | `ApiResponse<UserResponseDto>` | Updates `users`, updates role join; writes to `audit_logs` | `EditUserModal.tsx` | `AdminUserOperationsTest.java` |
| `PUT /api/v1/admin/users/{id}/status` | `AdminUserController.updateUserStatus()` | `AdminUserService.updateUserStatus()` | `hasRole('ADMIN')` | `UserStatusUpdateRequest` (`active`) | `ApiResponse<UserResponseDto>` | Updates `users.active`; writes to `audit_logs` | `DeactivateConfirmModal.tsx` | `AdminUserOperationsTest.java` |
| `PUT /api/v1/admin/users/{id}/password-reset` | `AdminUserController.resetPassword()` | `AdminUserService.resetUserPassword()` | `hasRole('ADMIN')` | `AdminPasswordResetRequest` (`newPassword`) | `ApiResponse<Void>` | Overrides `users.password_hash`; writes to `audit_logs` | `PasswordResetModal.tsx` | `AdminUserOperationsTest.java` |
| `GET /api/v1/admin/audit-logs` | `AdminUserController.listAuditLogs()` | `AuditService.getAuditLogs()` | `hasRole('ADMIN')` | Params: `action`, `userId`, `page`, `size` | `ApiResponse<PageResponse<AuditLogResponseDto>>` | Queries `audit_logs` table | `UsersPage.tsx` (Audit tab) | `AdminUserOperationsTest.java` |

---

### 3.2 Module 2: Fleet & Vehicle Asset Management

| HTTP Method & Path | Controller & Handler Method | Service Method | Authorized Roles / Precondition | Request Payload / DTO | Response Envelope / DTO | Database Effects | Frontend Consumer | Existing Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/fleet/vehicles` | `VehicleController.getVehicles()` | `VehicleService.findVehicles()` | Authenticated | Params: `status`, `type`, `active`, `search`, `page`, `size`, `sort` | `ApiResponse<PageResponse<VehicleResponseDto>>` | Queries `vehicles` | `VehiclesPage.tsx` | `VehicleOperationsTest.java`, `smoke_test.sh` [9/10] |
| `GET /api/v1/fleet/vehicles/{id}` | `VehicleController.getVehicleById()` | `VehicleService.getVehicleById()` | Authenticated | Path variable `id` | `ApiResponse<VehicleResponseDto>` | Queries `vehicles` | `VehicleDetailsModal.tsx` | `VehicleOperationsTest.java` |
| `POST /api/v1/fleet/vehicles` | `VehicleController.createVehicle()` | `VehicleService.createVehicle()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER')` | `VehicleCreateRequest` (`vin`, `licensePlate`, `make`, `model`, `capacityKg`, etc.) | `ApiResponse<VehicleResponseDto>` | Inserts into `vehicles` table | `CreateVehicleModal.tsx` | `VehicleOperationsTest.java`, `verify_live_persistence.sh` |
| `PUT /api/v1/fleet/vehicles/{id}` | `VehicleController.updateVehicle()` | `VehicleService.updateVehicle()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER')` | `VehicleUpdateRequest` (`make`, `model`, `capacityKg`, `vehicleType`, etc.) | `ApiResponse<VehicleResponseDto>` | Updates `vehicles` row | `EditVehicleModal.tsx` | `VehicleOperationsTest.java` |
| `PATCH /api/v1/fleet/vehicles/{id}/status` | `VehicleController.updateStatus()` | `VehicleService.updateVehicleStatus()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `VehicleStatusUpdateRequest` (`status`) | `ApiResponse<VehicleResponseDto>` | Updates `vehicles.status` | `VehiclesPage.tsx` dropdown | `VehicleOperationsTest.java` |
| `PATCH /api/v1/fleet/vehicles/{id}/odometer` | `VehicleController.updateOdometer()` | `VehicleService.updateOdometer()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DRIVER')` | Param: `newOdometerKm` | `ApiResponse<VehicleResponseDto>` | Updates `vehicles.odometer_km` | Background tracking sync | `VehicleOperationsTest.java` |
| `POST /api/v1/fleet/vehicles/{id}/fuel-records` | `VehicleController.logFuel()` | `FuelService.logFuel()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DRIVER')` | `FuelRecordCreateRequest` (`litres`, `cost`, `odometerKm`) | `ApiResponse<FuelRecordResponseDto>` | Inserts into `fuel_records`, updates `vehicles.odometer_km` | `LogFuelModal.tsx` | `FuelAndMaintenanceTest.java` |
| `GET /api/v1/fleet/vehicles/{id}/fuel-records` | `VehicleController.getFuelRecords()` | `FuelService.getFuelHistory()` | Authenticated | Path variable `id` | `ApiResponse<List<FuelRecordResponseDto>>` | Queries `fuel_records` | `VehicleDetailsModal.tsx` | `FuelAndMaintenanceTest.java` |
| `POST /api/v1/fleet/vehicles/{id}/maintenance-records` | `VehicleController.scheduleMaintenance()` | `MaintenanceService.logMaintenance()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER')` | `MaintenanceRecordCreateRequest` (`serviceType`, `cost`, `nextServiceDueKm`, etc.) | `ApiResponse<MaintenanceRecordResponseDto>` | Inserts into `maintenance_records`, updates `service_warnings` | `ScheduleMaintenanceModal.tsx` | `FuelAndMaintenanceTest.java` |
| `GET /api/v1/fleet/vehicles/{id}/maintenance-records` | `VehicleController.getMaintenanceRecords()` | `MaintenanceService.getVehicleRecords()` | Authenticated | Path variable `id` | `ApiResponse<List<MaintenanceRecordResponseDto>>` | Queries `maintenance_records` | `VehicleDetailsModal.tsx` | `FuelAndMaintenanceTest.java` |
| `GET /api/v1/fleet/warnings` | `FleetMaintenanceController.getActiveWarnings()` | `MaintenanceService.getActiveWarnings()` | Authenticated | None | `ApiResponse<List<ServiceWarningResponseDto>>` | Queries `service_warnings` where `acknowledged=false` | `MaintenancePage.tsx` | `FuelAndMaintenanceTest.java` |
| `GET /api/v1/fleet/maintenance-records` | `FleetMaintenanceController.getAllMaintenanceRecords()` | `MaintenanceService.getAllRecords()` | Authenticated | Params: `page`, `size` | `ApiResponse<PageResponse<MaintenanceRecordResponseDto>>` | Queries `maintenance_records` | `MaintenancePage.tsx` | `FuelAndMaintenanceTest.java` |

---

### 3.3 Module 3: Commercial Drivers & Trip Operations

| HTTP Method & Path | Controller & Handler Method | Service Method | Authorized Roles / Precondition | Request Payload / DTO | Response Envelope / DTO | Database Effects | Frontend Consumer | Existing Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/drivers` | `DriverController.getDrivers()` | `DriverService.searchDrivers()` | Authenticated | Params: `search`, `availability`, `active`, `page`, `size`, `sortBy` | `ApiResponse<PageResponse<DriverResponseDto>>` | Queries `drivers` and `users` | `DriversPage.tsx` | `DriverOperationsTest.java` |
| `GET /api/v1/drivers/{id}` | `DriverController.getDriverById()` | `DriverService.getDriverById()` | Authenticated | Path variable `id` | `ApiResponse<DriverResponseDto>` | Queries `drivers` | `DriverDetailsModal.tsx` | `DriverOperationsTest.java` |
| `POST /api/v1/drivers` | `DriverController.createDriver()` | `DriverService.createDriver()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `DriverCreateRequest` (`userId`, `licenseNumber`, `licenseCategory`, `licenseExpiryDate`) | `ApiResponse<DriverResponseDto>` | Inserts into `drivers` table | `CreateDriverModal.tsx` | `DriverOperationsTest.java` |
| `PUT /api/v1/drivers/{id}` | `DriverController.updateDriver()` | `DriverService.updateDriver()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `DriverUpdateRequest` (`licenseCategory`, `licenseExpiryDate`, `medicalCertExpiryDate`) | `ApiResponse<DriverResponseDto>` | Updates `drivers` row | `EditDriverModal.tsx` | `DriverOperationsTest.java` |
| `PATCH /api/v1/drivers/{id}/availability` | `DriverController.updateAvailability()` | `DriverService.updateAvailability()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `DriverStatusUpdateRequest` (`availabilityStatus`) | `ApiResponse<DriverResponseDto>` | Updates `drivers.availability_status` | `DriversPage.tsx` toggle | `DriverOperationsTest.java` |
| `GET /api/v1/drivers/{id}/performance` | `DriverController.getPerformance()` | `DriverPerformanceService.getPerformanceMetrics()` | Authenticated | Path variable `id` | `ApiResponse<DriverPerformanceResponseDto>` | Queries `driver_performance_metrics` | `DriverDetailsModal.tsx` | `DriverPerformanceTest.java` |
| `GET /api/v1/trips` | `TripController.getTrips()` | `TripService.searchTrips()` | Authenticated | Params: `search`, `status`, `driverId`, `vehicleId`, `page`, `size` | `ApiResponse<PageResponse<TripResponseDto>>` | Queries `trips` | `TripsPage.tsx` | `TripLifecycleOperationsTest.java` |
| `GET /api/v1/trips/{id}` | `TripController.getTripById()` | `TripService.getTripById()` | Authenticated | Path variable `id` | `ApiResponse<TripResponseDto>` | Queries `trips` | `TripLifecycleModal.tsx` | `TripLifecycleOperationsTest.java` |
| `POST /api/v1/trips` | `TripController.createTrip()` | `TripService.createTrip()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `TripCreateRequest` (`vehicleId`, `driverId`, `originAddress`, `destinationAddress`, `scheduledStartTime`) | `ApiResponse<TripResponseDto>` | Inserts into `trips`, `trip_status_history`, marks driver ON_DUTY & vehicle IN_TRIP | `CreateTripModal.tsx` | `TripLifecycleOperationsTest.java`, `EndToEndLogisticsWorkflowIntegrationTest.java` |
| `PATCH /api/v1/trips/{id}/status` | `TripController.updateTripStatus()` | `TripService.updateTripStatus()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')` | `TripStatusUpdateRequest` (`targetStatus`, `remarks`) | `ApiResponse<TripResponseDto>` | Updates `trips.status`, writes `trip_status_history`, releases vehicle/driver on complete/cancel | `TripLifecycleModal.tsx` | `TripLifecycleOperationsTest.java` |
| `GET /api/v1/trips/{id}/history` | `TripController.getTripHistory()` | `TripService.getTripHistory()` | Authenticated | Path variable `id` | `ApiResponse<List<TripStatusHistoryResponseDto>>` | Queries `trip_status_history` | `TripLifecycleModal.tsx` | `TripLifecycleOperationsTest.java` |

---

### 3.4 Module 4: Shipment Manifests & Route Logistics

| HTTP Method & Path | Controller & Handler Method | Service Method | Authorized Roles / Precondition | Request Payload / DTO | Response Envelope / DTO | Database Effects | Frontend Consumer | Existing Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/shipments` | `ShipmentController.getShipments()` | `ShipmentService.searchShipments()` | Authenticated | Params: `search`, `status`, `tripId`, `page`, `size` | `ApiResponse<PageResponse<ShipmentResponseDto>>` | Queries `shipments` | `ShipmentsPage.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `GET /api/v1/shipments/{idOrTracking}` | `ShipmentController.getShipment()` | `ShipmentService.getShipmentByIdOrTracking()` | Authenticated | Path variable `idOrTracking` | `ApiResponse<ShipmentResponseDto>` | Queries `shipments` by ID or tracking number | `ShipmentDetailsModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `POST /api/v1/shipments` | `ShipmentController.createShipment()` | `ShipmentService.createShipment()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `ShipmentCreateRequest` (`senderName`, `receiverName`, `originAddress`, `weightKg`, etc.) | `ApiResponse<ShipmentResponseDto>` | Inserts into `shipments`, writes ORDERED to `shipment_timeline_events` | `CreateShipmentModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `PUT /api/v1/shipments/{id}` | `ShipmentController.updateShipment()` | `ShipmentService.updateShipment()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `ShipmentUpdateRequest` (`cargoDescription`, `weightKg`, `volumeM3`) | `ApiResponse<ShipmentResponseDto>` | Updates `shipments` row | `EditShipmentModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `POST /api/v1/shipments/{id}/assign-trip` | `ShipmentController.assignTrip()` | `ShipmentService.assignShipmentToTrip()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `ShipmentAssignTripRequest` (`tripId`) | `ApiResponse<ShipmentResponseDto>` | Updates `shipments.assigned_trip_id`, transitions status to DISPATCHED | `AssignTripModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `PATCH /api/v1/shipments/{id}/status` | `ShipmentController.updateStatus()` | `ShipmentService.updateShipmentStatus()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')` | `ShipmentStatusUpdateRequest` (`targetStatus`, `locationSummary`, `remarks`) | `ApiResponse<ShipmentResponseDto>` | Updates `shipments.status`, writes to `shipment_timeline_events` | `ShipmentDetailsModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `POST /api/v1/shipments/{id}/deliver` | `ShipmentController.completeDelivery()` | `ShipmentService.completeDelivery()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')` | `ShipmentDeliverRequest` (`deliveryNotes`, `locationSummary`) | `ApiResponse<ShipmentResponseDto>` | Sets status DELIVERED, records actual delivery timestamp and timeline event | `ShipmentDetailsModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `POST /api/v1/shipments/{id}/waypoints` | `ShipmentController.saveWaypoints()` | `ShipmentService.saveWaypointsAndCalculateRoute()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `WaypointsUpdateRequest` (array of `WaypointItemRequest`) | `ApiResponse<RoutePlanResponseDto>` | Deletes old waypoints, inserts new `shipment_waypoints`, updates shipment distance & ETA | `RouteWaypointsModal.tsx` | `RouteAndDelayedShipmentTest.java` |
| `GET /api/v1/shipments/{id}/waypoints` | `ShipmentController.getWaypoints()` | `ShipmentService.getRoutePlan()` | Authenticated | Path variable `id` | `ApiResponse<RoutePlanResponseDto>` | Queries `shipment_waypoints` | `WaypointsPage.tsx` | `RouteAndDelayedShipmentTest.java` |
| `GET /api/v1/shipments/{idOrTracking}/timeline` | `ShipmentController.getTimeline()` | `ShipmentService.getShipmentTimeline()` | Authenticated | Path variable `idOrTracking` | `ApiResponse<List<ShipmentTimelineEventDto>>` | Queries `shipment_timeline_events` | `ShipmentDetailsModal.tsx` | `ShipmentLifecycleAndTripAssociationTest.java` |
| `POST /api/v1/shipments/check-delayed` | `ShipmentController.checkDelayedShipments()` | `ShipmentService.detectAndFlagDelayedShipments()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | None | `ApiResponse<Map<String, Integer>>` | Scans in-transit shipments, updates status to DELAYED, emits SHIPMENT_DELAYED alert | `ShipmentsPage.tsx` | `RouteAndDelayedShipmentTest.java` |

---

### 3.5 Module 5: GPS Simulation, Geofences, Alerts & Control Tower

| HTTP Method & Path | Controller & Handler Method | Service Method | Authorized Roles / Precondition | Request Payload / DTO | Response Envelope / DTO | Database Effects | Frontend Consumer | Existing Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/tracking/control-tower/summary` | `TrackingController.getControlTowerSummary()` | `TrackingService.getControlTowerSummary()` | Authenticated | None | `ApiResponse<Map<String, Object>>` | Aggregates counts from `vehicles`, `trips`, `shipments`, `alerts` | `ControlTowerPage.tsx` | `ControlTowerAlertAndReportTest.java` |
| `GET /api/v1/tracking/simulator/sessions` | `TrackingController.getSimulatorSessions()` | `TrackingService.getSimulatorSessions()` | Authenticated | None | `ApiResponse<List<SimulatorSessionDto>>` | Reads in-memory concurrent map of active simulation worker threads | `ControlTowerPage.tsx` | `ControlTowerAlertAndReportTest.java` |
| `POST /api/v1/tracking/simulator/start` | `TrackingController.startSimulation()` | `TrackingService.startSimulation()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `SimulatorStartRequest` (`vehicleId`, `tripId`, `tickIntervalSeconds`, `speedMultiplier`, etc.) | `ApiResponse<SimulatorSessionDto>` | Spawns scheduled thread, loads waypoints, begins interpolation | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/simulator/{vehicleId}/pause` | `TrackingController.pauseSimulation()` | `TrackingService.pauseSimulation()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path variable `vehicleId` | `ApiResponse<SimulatorSessionDto>` | Modifies simulator session status to PAUSED | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/simulator/{vehicleId}/resume` | `TrackingController.resumeSimulation()` | `TrackingService.resumeSimulation()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path variable `vehicleId` | `ApiResponse<SimulatorSessionDto>` | Modifies simulator session status to RUNNING | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/simulator/{vehicleId}/stop` | `TrackingController.stopSimulation()` | `TrackingService.stopSimulation()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path variable `vehicleId` | `ApiResponse<SimulatorSessionDto>` | Cancels scheduled future, removes from active sessions | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/simulator/{vehicleId}/step` | `TrackingController.stepSimulation()` | `TrackingService.stepSimulation()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path variable `vehicleId` | `ApiResponse<SimulatorSessionDto>` | Manually advances simulation by one tick deterministically | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/telemetry` | `TrackingController.ingestSimulatedTelemetry()` | `TrackingService.ingestSimulatedTelemetry()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER')` | `TelemetryPingDto` (`vehicleId`, `latitude`, `longitude`, `speedKmh`, `headingDegrees`) | `ApiResponse<TelemetryPingDto>` | Inserts into `telemetry_logs`, checks geofences, broadcasts to STOMP | External simulation | `SimulationAndGeofenceTest.java` |
| `GET /api/v1/tracking/vehicles/{vehicleId}/history` | `TrackingController.getVehicleHistory()` | `TrackingService.getVehicleTrackingHistory()` | Authenticated | Path `vehicleId`, param `limit` | `ApiResponse<List<TelemetryPingDto>>` | Queries `telemetry_logs` ordered by timestamp desc | Map polyline trail | `SimulationAndGeofenceTest.java` |
| `POST /api/v1/tracking/geofences` | `TrackingController.createGeofence()` | `TrackingService.createGeofence()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | `GeofenceCreateRequest` (`name`, `type`, `centerLat`, `centerLon`, `radiusMeters`, `polygonCoordinates`) | `ApiResponse<GeofenceResponseDto>` | Inserts into `geofences` table | `CreateGeofenceModal.tsx` | `SimulationAndGeofenceTest.java` |
| `GET /api/v1/tracking/geofences` | `TrackingController.listGeofences()` | `TrackingService.listGeofences()` | Authenticated | Param: `activeOnly` | `ApiResponse<List<GeofenceResponseDto>>` | Queries `geofences` table | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `PATCH /api/v1/tracking/geofences/{id}/toggle` | `TrackingController.toggleGeofence()` | `TrackingService.toggleGeofence()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path `id`, param `active` | `ApiResponse<GeofenceResponseDto>` | Updates `geofences.active` flag | `ControlTowerPage.tsx` | `SimulationAndGeofenceTest.java` |
| `GET /api/v1/tracking/alerts` | `TrackingController.listAlerts()` | `TrackingService.listAlerts()` | Authenticated | Params: `alertType`, `severity`, `entityType`, `acknowledged`, `page`, `size` | `ApiResponse<PageResponse<AlertResponseDto>>` | Queries `alerts` table | `ControlTowerPage.tsx` (AlertFeed) | `ControlTowerAlertAndReportTest.java` |
| `POST /api/v1/tracking/alerts/{id}/acknowledge` | `TrackingController.acknowledgeAlert()` | `TrackingService.acknowledgeAlert()` | `hasAnyRole('ADMIN', 'FLEET_MANAGER', 'DISPATCHER')` | Path variable `id` | `ApiResponse<AlertResponseDto>` | Updates `alerts.acknowledged = true` | `ControlTowerPage.tsx` | `ControlTowerAlertAndReportTest.java` |
| `GET /api/v1/tracking/reports/summary` | `TrackingController.getReportSummary()` | `TrackingService.getReportSummary()` | Authenticated | Params: `reportType`, `startDate`, `endDate`, `status` | `ApiResponse<ReportSummaryDto>` | Aggregates summary statistics for selected domain | `ReportsPage.tsx` | `ControlTowerAlertAndReportTest.java` |
| `GET /api/v1/tracking/reports/export-csv` | `TrackingController.exportCsvReport()` | `TrackingService.exportCsvReport()` | Authenticated | Params: `reportType`, `startDate`, `endDate`, `status` | RFC 4180 CSV text (`text/csv`) | Streams raw CSV rows directly from database entities | `ReportsPage.tsx` (Download button) | `ControlTowerAlertAndReportTest.java` |

---

## 4. Discrepancy & Boundary Verification

### 4.1 Frontend-Backend Contract Alignment
1. **Dual Route Prefix Support:**
   Every controller supports both `/api/v1/...` and legacy `/api/...` mappings via `@RequestMapping({"/api/v1/...", "/api/..."})`.
   The frontend Axios client (`client.ts`) consistently targets `/api/v1/...`.
2. **WebSocket / STOMP Real-Time vs. Polling:**
   - **Backend Implementation:** `WebSocketConfig` exposes `/ws/fleetx` with SockJS fallback and STOMP simple broker on `/topic`. When telemetry is ingested or simulator ticks occur, `TrackingService` publishes messages to `/topic/vehicles/{id}` and `/topic/fleet-locations`.
   - **Frontend Reality:** The frontend `ControlTowerPage.tsx` does NOT instantiate a STOMP client (`@stomp/stompjs` is absent from `package.json`). Instead, it executes an **HTTP REST polling loop every 3 seconds** (`setInterval(loadControlTowerData, 3000)`) while any simulation is active.
   - **Assessment:** While functional and reliable across proxies and firewalls, this is an architectural discrepancy with the presentation and paper claims of STOMP-pushed UI updates.

### 4.2 Security Vulnerability Auditing
- **Viewer Role Mutation Resistance:** Verified. Direct HTTP POST/PUT requests to `/api/v1/fleet/vehicles`, `/api/v1/trips`, or `/api/v1/shipments` by a user with `ROLE_VIEWER` return HTTP 403 Forbidden.
- **CSRF Bypass Resistance:** Verified. Any state-changing mutation without `X-XSRF-TOKEN` header is rejected with HTTP 403 Forbidden by `SpaCsrfTokenRequestHandler`.
- **Privilege Escalation Resistance:** Verified. `/api/v1/admin/**` endpoints are protected by class-level `@PreAuthorize("hasRole('ADMIN')")`. Non-admin users cannot provision accounts or elevate roles.
