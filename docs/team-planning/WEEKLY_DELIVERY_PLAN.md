# FleetX — 14-Week Team Delivery & Milestone Plan (Phase F)

**Reporting Window:** `2026-07-06` through `2026-10-06` (14 Academic Weeks / 93 Calendar Days) + Post-Audit Stabilization  
**Team Structure:** 5 Students (`Member 1` = Team Leader & Identity/Admin; `Member 2` = Fleet & Assets; `Member 3` = Drivers & Trips; `Member 4` = Shipments & Routes; `Member 5` = Tracking & Control Tower)

> [!NOTE]
> **Historical Evidence Note:** As documented in `PROJECT_REPOSITORY_AUDIT.md`, all 276 first-party source, migration, test, and documentation files exist on disk and pass automated verification as of 13 October 2026 (`79/79` backend tests, `34/34` frontend tests), while historical date-specific commits for the July–October 2026 window remain `UNVERIFIED` due to the absence of a `.git` commit log from that period.

---

## Weekly Delivery Schedule

### Week 1: `2026-07-06` – `2026-07-12` (Phase 0 — Architecture, Contracts & Database Blueprint)
- **Member 1 (Leader — `main`):** Author `docs/PROJECT_CHARTER.md`, `docs/SRS.md`, `docs/ARCHITECTURE.md`, `docs/DATABASE_DESIGN.md`, `docs/API_CONTRACTS.md`, `docs/TEAM_OWNERSHIP.md`, `docs/GITHUB_WORKFLOW.md`, `docs/TEST_PLAN.md`.
- **Members 2–5:** Review SRS requirements, verify assigned Flyway migration ranges (`V200`–`V599`), and sign off on shared Java contracts (`VehicleLookupService`, `DriverLookupService`, `TripLookupService`, `ShipmentLookupService`, `AlertPublisher`).
- **Expected Functionality:** Approved architecture blueprint, API contracts, and zero-overlap package ownership rules.
- **Tests / Verification:** Architecture & contract sign-off review.
- **Shared Dependencies:** None.
- **Review & Integration Checkpoint:** Team Leader freezes package names (`com.fleetx.{auth,fleet,drivers,shipments,tracking}`) and Flyway ranges before coding begins.

---

### Week 2: `2026-07-13` – `2026-07-19` (Phase 1A — Monorepo Build Foundation & Shared Backend Infrastructure)
- **Member 1 (Leader — `main`):** Create `.gitignore`, `.env.example`, `docker-compose.yml`, `FLEETX_AGENT_RULES.md`, `README.md`, `backend/pom.xml`, Maven wrapper (`mvnw`, `mvnw.cmd`), `FleetxApplication.java`, `application.yml`, `application-dev.yml`, `application-test.yml`, and `com.fleetx.shared.dto.*` / `com.fleetx.shared.exception.*`.
- **Members 2–5:** Set up local JDK 21, Node 20+, and Docker MySQL 8.4 environment; verify `.env` template.
- **Expected Functionality:** Runnable Spring Boot 3.5.9 skeleton with uniform `ApiResponse<T>` / `ApiErrorResponse` envelopes and `GlobalExceptionHandler`.
- **Tests to Run:** `cd backend && ./mvnw test-compile`
- **Shared Dependencies:** `backend/pom.xml`, `application.yml`.
- **Review & Integration Checkpoint:** Verify Maven wrapper builds cleanly on all 5 student machines.

---

### Week 3: `2026-07-20` – `2026-07-26` (Phase 1B — Shared Contracts, Fallbacks, UI Shell & Baseline Migrations)
- **Member 1 (Leader — `main`):** Implement `com.fleetx.shared.contract.*` (5 interfaces), `SharedFallbackConfiguration.java` (`@ConditionalOnMissingBean`), `SecurityConfig.java`, `WebMvcConfig.java`, `WebSocketConfig.java`, `HealthController.java`, and frontend shell (`frontend/package.json`, `vite.config.ts`, `src/shared/api/client.ts`, `src/app/Layout.tsx`, `src/app/routes.tsx`, `src/shared/components/*`).
- **Member 2 (`feature/fleet-assets`):** Create `V200__init_fleet_schema.sql` and `V201__fleet_module_enhancements.sql`.
- **Member 3 (`feature/driver-trip-ops`):** Create `V300__init_driver_trip_schema.sql` and `V301__driver_trip_enhancements.sql`.
- **Member 4 (`feature/shipment-route`):** Create `V400__init_shipment_route_schema.sql` and `V401__shipment_route_enhancements.sql`.
- **Member 5 (`feature/tracking-control-tower`):** Create `V500__init_tracking_geofence_schema.sql` and `V501__tracking_control_tower_enhancements.sql`.
- **Expected Functionality:** Complete `main` branch baseline + all 10 Flyway SQL migrations (`V100`–`V501`).
- **Tests to Run:** `./mvnw test -Dtest=HealthControllerTest,FallbackServicesTest,SecurityAccessTest` and `npm test -- --run src/test/App.test.tsx`.
- **Shared Dependencies:** `SharedFallbackConfiguration`, `SecurityConfig`, `frontend/src/app/routes.tsx`.
- **Review & Integration Checkpoint:** All 5 members branch off `main` into their dedicated `feature/*` branches.

---

### Week 4: `2026-07-27` – `2026-08-02` (Domain Entities & Enum State Machines Across All 5 Modules)
- **Member 1 (`feature/identity-admin`):** `V100__init_identity_schema.sql`, `V101__seed_roles_and_admin.sql`, `Role.java`, `User.java`, `AuditLog.java`.
- **Member 2 (`feature/fleet-assets`):** `VehicleStatus.java`, `VehicleType.java`, `FuelType.java`, `Vehicle.java`, `FuelRecord.java`.
- **Member 3 (`feature/driver-trip-ops`):** `DriverAvailability.java`, `TripStatus.java`, `Driver.java`, `DriverPerformanceMetrics.java`.
- **Member 4 (`feature/shipment-route`):** `ShipmentStatus.java`, `StopType.java`, `Shipment.java`, `ShipmentWaypoint.java`, `ShipmentTimelineEvent.java`.
- **Member 5 (`feature/tracking-control-tower`):** `TrackingLog.java`, `Geofence.java`, `GeofenceVehicleState.java`, `Alert.java`, `ControlTowerKpi.java`.
- **Expected Functionality:** All JPA domain entities mapped to Flyway tables and passing Hibernate `ddl-auto: validate` (`@JdbcTypeCode(SqlTypes.DECIMAL)` on `DECIMAL`-backed `Double` columns).
- **Tests to Run:** `./mvnw test-compile` on each feature branch.
- **Shared Dependencies:** Flyway migrations `V100`–`V501`.
- **Review & Integration Checkpoint:** Leader verifies zero cross-module entity imports and clean `ddl-auto: validate` startup.

---

### Week 5: `2026-08-03` – `2026-08-09` (Spring Data Repositories & Custom Query Layer)
- **Member 1 (`feature/identity-admin`):** `RoleRepository.java`, `UserRepository.java`, `AuditLogRepository.java`.
- **Member 2 (`feature/fleet-assets`):** `MaintenanceStatus.java`, `ServiceType.java`, `WarningSeverity.java`, `WarningType.java`, `MaintenanceRecord.java`, `ServiceWarning.java`, `VehicleRepository.java`, `FuelRecordRepository.java`, `MaintenanceRecordRepository.java`, `ServiceWarningRepository.java`.
- **Member 3 (`feature/driver-trip-ops`):** `Trip.java`, `TripStatusHistory.java`, `DriverRepository.java`, `DriverPerformanceMetricsRepository.java`, `TripRepository.java`, `TripStatusHistoryRepository.java`.
- **Member 4 (`feature/shipment-route`):** `ShipmentRepository.java`, `ShipmentWaypointRepository.java`, `ShipmentTimelineEventRepository.java`.
- **Member 5 (`feature/tracking-control-tower`):** `TrackingRepository.java`, `GeofenceRepository.java`, `GeofenceVehicleStateRepository.java`, `AlertRepository.java`, `ControlTowerKpiRepository.java`, `ControlTowerReadRepository.java`.
- **Expected Functionality:** Complete repository layer with pagination, filtering, overlap detection, and read-only KPI aggregation.
- **Tests to Run:** `./mvnw test-compile`
- **Shared Dependencies:** Module JPA entities.
- **Review & Integration Checkpoint:** Verify zero cross-module repository injections (`grep -rn "Repository"` across modules).

---

### Week 6: `2026-08-10` – `2026-08-16` (Validated Request & Response DTOs)
- **Member 1 (`feature/identity-admin`):** `LoginRequest.java`, `UserCreateRequest.java`, `UserUpdateRequest.java`, `UserStatusUpdateRequest.java`, `AdminPasswordResetRequest.java`, `UserResponseDto.java`, `AuditLogResponseDto.java`.
- **Member 2 (`feature/fleet-assets`):** `VehicleCreateRequest.java`, `VehicleUpdateRequest.java`, `VehicleStatusUpdateRequest.java`, `VehicleResponseDto.java`, `FuelRecordCreateRequest.java`, `FuelRecordResponseDto.java`, `MaintenanceRecordCreateRequest.java`, `MaintenanceRecordResponseDto.java`, `ServiceWarningResponseDto.java`.
- **Member 3 (`feature/driver-trip-ops`):** `DriverCreateRequest.java`, `DriverUpdateRequest.java`, `DriverStatusUpdateRequest.java`, `DriverResponseDto.java`, `DriverPerformanceResponseDto.java`, `TripCreateRequest.java`, `TripStatusUpdateRequest.java`, `TripResponseDto.java`, `TripStatusHistoryResponseDto.java`.
- **Member 4 (`feature/shipment-route`):** `ShipmentCreateRequest.java`, `ShipmentUpdateRequest.java`, `ShipmentAssignTripRequest.java`, `ShipmentStatusUpdateRequest.java`, `ShipmentDeliverRequest.java`, `ShipmentResponseDto.java`, `WaypointItemRequest.java`, `WaypointsUpdateRequest.java`, `WaypointResponseDto.java`, `RoutePlanResponseDto.java`, `ShipmentTimelineEventDto.java`.
- **Member 5 (`feature/tracking-control-tower`):** `TelemetryPingDto.java`, `SimulatorWaypointDto.java`, `SimulatorStartRequest.java`, `SimulatorSessionDto.java`, `GeofenceCreateRequest.java`, `GeofenceResponseDto.java`, `AlertResponseDto.java`, `ReportSummaryDto.java`.
- **Expected Functionality:** All REST DTOs with Jakarta Bean Validation (`@NotBlank`, `@Positive`, `@Min`, `@Max`) and Jackson serialization rules.
- **Tests to Run:** `./mvnw test-compile`
- **Shared Dependencies:** `com.fleetx.shared.dto.*`.
- **Review & Integration Checkpoint:** Verify DTOs match `docs/API_CONTRACTS.md` and never expose sensitive internal fields (`passwordHash`, PII to `VIEWER`).

---

### Week 7: `2026-08-17` – `2026-08-23` (Core Domain Services & `@Primary` Shared Contract Providers — Part 1)
- **Member 1 (`feature/identity-admin`):** `CustomUserDetailsService.java`, `AuditService.java`, `AuthService.java`.
- **Member 2 (`feature/fleet-assets`):** `VehicleService.java` (`@Primary VehicleLookupService`), `FuelService.java` (server-side total cost & km/L efficiency calculation).
- **Member 3 (`feature/driver-trip-ops`):** `DriverPerformanceService.java`, `DriverService.java` (`@Primary DriverLookupService`, duplicate licence & expiry guards, PII masking).
- **Member 4 (`feature/shipment-route`):** `RouteCalculationService.java` (great-circle Haversine distance & configurable speed/stop ETA calculator with `Estimated` disclosures).
- **Member 5 (`feature/tracking-control-tower`):** `GeofenceAndTelemetryMathService.java` (Haversine, compass bearing, linear interpolation, point-in-circle, ray-casting point-in-polygon).
- **Expected Functionality:** Core mathematical and business rules implemented in Java 21.
- **Tests to Run:** `./mvnw test -Dtest=DriverPerformanceTest`
- **Shared Dependencies:** `com.fleetx.shared.contract.*`.
- **Review & Integration Checkpoint:** Verify `@Primary` annotations on contract providers override `@ConditionalOnMissingBean` fallbacks cleanly.

---

### Week 8: `2026-08-24` – `2026-08-30` (Core Domain Services & Cross-Module Contract Integration — Part 2)
- **Member 1 (`feature/identity-admin`):** `AdminUserService.java` (sole-admin protection, status toggle, BCrypt reset), `AdminBootstrapRunner.java`.
- **Member 2 (`feature/fleet-assets`):** `MaintenanceService.java` (service threshold evaluation & `AlertPublisher` integration).
- **Member 3 (`feature/driver-trip-ops`):** `TripService.java` (`@Primary TripLookupService`, `VehicleLookupService` readiness check, active overlap prevention, `PLANNED -> ASSIGNED -> IN_PROGRESS -> COMPLETED / CANCELLED` state machine).
- **Member 4 (`feature/shipment-route`):** `ShipmentService.java` (`@Primary ShipmentLookupService`, trip association via `TripLookupService`, timeline events, delayed shipment detection via `AlertPublisher`).
- **Member 5 (`feature/tracking-control-tower`):** `TrackingService.java` (`@Primary AlertPublisher`, `SimulatedGpsService` scheduled worker pool, STOMP broadcaster, geofence transition detector, alert deduplication, formula-safe RFC 4180 CSV exporter).
- **Expected Functionality:** Complete backend service layer across all 5 modules.
- **Tests to Run:** `./mvnw test-compile`
- **Shared Dependencies:** `VehicleLookupService`, `DriverLookupService`, `TripLookupService`, `ShipmentLookupService`, `AlertPublisher`.
- **Review & Integration Checkpoint:** Verify all cross-module calls go strictly through `com.fleetx.shared.contract.*`.

---

### Week 9: `2026-08-31` – `2026-09-06` (REST Controllers, RBAC Annotations & Backend Test Suites)
- **Member 1 (`feature/identity-admin`):** `AuthController.java`, `AdminUserController.java`, `AuthLifecycleTest.java` (7 tests), `AdminUserOperationsTest.java` (9 tests).
- **Member 2 (`feature/fleet-assets`):** `VehicleController.java`, `FleetMaintenanceController.java`, `VehicleOperationsTest.java` (7 tests), `FuelAndMaintenanceTest.java` (5 tests).
- **Member 3 (`feature/driver-trip-ops`):** `DriverController.java`, `TripController.java`, `DriverOperationsTest.java` (11 tests), `TripLifecycleOperationsTest.java` (13 tests), `DriverPerformanceTest.java` (3 tests).
- **Member 4 (`feature/shipment-route`):** `ShipmentController.java`, `ShipmentLifecycleAndTripAssociationTest.java` (8 tests), `RouteAndDelayedShipmentTest.java` (5 tests).
- **Member 5 (`feature/tracking-control-tower`):** `TrackingController.java`, `SimulationAndGeofenceTest.java` (5 tests), `ControlTowerAlertAndReportTest.java` (5 tests).
- **Expected Functionality:** All documented `/api/v1/**` REST endpoints secured with `@PreAuthorize` and verified by 74 module/shared JUnit 5 tests.
- **Tests to Run:** `cd backend && ./mvnw test`
- **Shared Dependencies:** `SecurityConfig`, `GlobalExceptionHandler`.
- **Review & Integration Checkpoint:** Backend PR review checkpoint — every module's backend test suite must pass with 0 failures before frontend integration.

---

### Week 10: `2026-09-07` – `2026-09-13` (Frontend TypeScript Contracts & Axios API Client Services)
- **Member 1 (`feature/identity-admin`):** `frontend/src/features/auth/types.ts`, `frontend/src/features/auth/api/authApi.ts`.
- **Member 2 (`feature/fleet-assets`):** `frontend/src/features/fleet/types.ts`, `frontend/src/features/fleet/api/fleetApi.ts`.
- **Member 3 (`feature/driver-trip-ops`):** `frontend/src/features/drivers/types.ts`, `frontend/src/features/drivers/api/driverApi.ts`.
- **Member 4 (`feature/shipment-route`):** `frontend/src/features/shipments/types.ts`, `frontend/src/features/shipments/api/shipmentApi.ts`.
- **Member 5 (`feature/tracking-control-tower`):** `frontend/src/features/tracking/types.ts`, `frontend/src/features/tracking/api/trackingApi.ts`.
- **Expected Functionality:** Strongly-typed frontend API service layers routing through `/api/v1/**` and automatically attaching `X-XSRF-TOKEN` headers.
- **Tests to Run:** `cd frontend && npx tsc -b`
- **Shared Dependencies:** `frontend/src/shared/api/client.ts`, `frontend/src/shared/api/types.ts`.
- **Review & Integration Checkpoint:** Verify all frontend API paths use `/api/v1/...` prefix compatible with Vite's `/api` dev proxy.

---

### Week 11: `2026-09-14` – `2026-09-20` (Frontend Interactive Modals, Leaflet Map & Three.js 3D Components)
- **Member 1 (`feature/identity-admin`):** `CreateUserModal.tsx`, `EditUserModal.tsx`, `DeactivateConfirmModal.tsx`, `PasswordResetModal.tsx`.
- **Member 2 (`feature/fleet-assets`):** `CreateVehicleModal.tsx`, `EditVehicleModal.tsx`, `LogFuelModal.tsx`, `ScheduleMaintenanceModal.tsx`, `VehicleDetailsModal.tsx`.
- **Member 3 (`feature/driver-trip-ops`):** `CreateDriverModal.tsx`, `EditDriverModal.tsx`, `DriverDetailsModal.tsx`, `CreateTripModal.tsx`, `TripLifecycleModal.tsx`.
- **Member 4 (`feature/shipment-route`):** `CreateShipmentModal.tsx`, `EditShipmentModal.tsx`, `AssignTripModal.tsx`, `RouteWaypointsModal.tsx`, `ShipmentDetailsModal.tsx`.
- **Member 5 (`feature/tracking-control-tower`):** `LeafletFleetMap.tsx` (2D OpenStreetMap + simulated markers + geofences), `ThreeFleetScene.tsx` (stylized 3D highway scene with WebGL cleanup), `CreateGeofenceModal.tsx`.
- **Expected Functionality:** Complete interactive modal and visualization component suite across all 5 modules.
- **Tests to Run:** `cd frontend && npx tsc -b`
- **Shared Dependencies:** `frontend/src/shared/components/*`.
- **Review & Integration Checkpoint:** Verify Three.js WebGL disposal on unmount and explicit `Estimated` / `SIMULATED DATA` badges.

---

### Week 12: `2026-09-21` – `2026-09-27` (Frontend Feature Pages, Route Registration & Vitest Component Suites)
- **Member 1 (`feature/identity-admin`):** `LoginPage.tsx`, `UsersPage.tsx`, `ProfilePage.tsx`, `features/auth/routes.tsx`, `src/test/AuthComponents.test.tsx` (8 tests).
- **Member 2 (`feature/fleet-assets`):** `VehiclesPage.tsx`, `MaintenancePage.tsx`, `features/fleet/routes.tsx`, `src/test/FleetComponents.test.tsx` (5 tests).
- **Member 3 (`feature/driver-trip-ops`):** `DriversPage.tsx`, `TripsPage.tsx`, `features/drivers/routes.tsx`, `src/test/DriverComponents.test.tsx` (6 tests).
- **Member 4 (`feature/shipment-route`):** `ShipmentsPage.tsx`, `WaypointsPage.tsx`, `features/shipments/routes.tsx`, `src/test/ShipmentComponents.test.tsx` (5 tests).
- **Member 5 (`feature/tracking-control-tower`):** `ControlTowerPage.tsx`, `ReportsPage.tsx`, `features/tracking/routes.tsx`, `src/test/TrackingComponents.test.tsx` (4 tests).
- **Expected Functionality:** All 5 feature UIs complete, route-registered, and verified by 31 module Vitest tests.
- **Tests to Run:** `cd frontend && npm test -- --run && npm run build`
- **Shared Dependencies:** `frontend/src/app/routes.tsx`.
- **Review & Integration Checkpoint:** All 5 members open Pull Requests from their `feature/*` branches targeting `main`.

---

### Week 13: `2026-09-28` – `2026-10-04` (Sequential PR Merges & Repeatable Development Demo Seeders)
- **Member 1 (`feature/identity-admin`):** Implement `DemoIdentitySeeder.java` (`@Order(10)`).
- **Member 2 (`feature/fleet-assets`):** Implement `DemoFleetSeeder.java` (`@Order(20)`).
- **Member 3 (`feature/driver-trip-ops`):** Implement `DemoDriverTripSeeder.java` (`@Order(30)`).
- **Member 4 (`feature/shipment-route`):** Implement `DemoShipmentSeeder.java` (`@Order(40)`).
- **Member 5 (`feature/tracking-control-tower`):** Implement `DemoTrackingSeeder.java` (`@Order(50)`).
- **Expected Functionality:** Ordered, idempotent development dataset seeding controlled by `fleetx.demo.seed.enabled` (disabled in prod/test).
- **Tests to Run:** `cd backend && ./mvnw test`
- **Shared Dependencies:** `application-dev.yml`, `V100`–`V501` migrations.
- **Review & Integration Checkpoint:** Team Leader merges PRs in dependency order: `M1 -> M2 -> M3 -> M4 -> M5`.

---

### Week 14: `2026-10-05` – `2026-10-06` & Post-Audit Stabilization (Phase 7 Full-System E2E Verification)
- **Member 1 (Team Leader — Integration Branch / `main`):**
  - Implement `FlywayMigrationAndArchitectureTest.java` (3 tests) and `EndToEndLogisticsWorkflowIntegrationTest.java` (2 tests).
  - Implement `frontend/src/test/IntegrationNavigationAndAuth.test.tsx` (3 tests).
  - Resolve cross-module integration defects (`INT-01` through `INT-06`) and publish `docs/INTEGRATION_TEST_REPORT.md` and updated `README.md`.
- **Expected Functionality:** Fully stabilized 5-module system passing all **79 backend tests**, **34 frontend tests**, and production Vite build (`tsc -b && vite build`).
- **Tests to Run:**
  - `cd backend && ./mvnw test` (`79 passed, 0 failed`)
  - `cd frontend && npm test -- --run && npm run build` (`34 passed, 0 failed`)
- **Review & Integration Checkpoint:** Final academic PBL capstone release sign-off.
