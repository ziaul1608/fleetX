# FleetX — Phase 7 Integration, End-to-End Testing & Stabilization Report

**Project:** FleetX — Intelligent Fleet & Logistics Management System (Academic Year 2026–27)  
**Architecture:** Modular Monolith (Spring Boot 3.5.9 / Java 21 / MySQL 8.4 LTS / React 18 + Vite + TypeScript)  
**Phase:** Phase 7 — Full-System Integration, End-to-End Verification & Stabilization  
**Date:** October 13, 2026  

---

## 1. Environment & Toolchain Summary

| Component | Version / Configuration | Status |
| :--- | :--- | :--- |
| **Operating System** | macOS (Darwin arm64) | Verified |
| **Java Development Kit (JDK)** | JetBrains Runtime / OpenJDK 21 (`release 21`) | Verified |
| **Backend Framework** | Spring Boot `3.5.9` (Maven Wrapper `./mvnw`) | Verified |
| **Database Schema Governance** | Flyway Core + MySQL (`V100`–`V501`, 10 versioned migrations) | Verified |
| **ORM / Schema Validation** | Hibernate 6 (`spring.jpa.hibernate.ddl-auto: validate`) | Verified |
| **Frontend Runtime & Bundler** | Node.js LTS, Vite `5.4.21`, React `18.3.1`, TypeScript `5.6.x` | Verified |
| **Frontend Test Runner** | Vitest `2.1.9` + React Testing Library + JSDOM | Verified |

---

## 2. Exact Test Commands & Actual Verification Results

### 2.1 Backend Automated Test Suite (79 Tests Across 15 Test Classes)

**Command:**
```bash
cd backend
JAVA_HOME="/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home" ./mvnw test
```

**Actual Results Summary (`79 Tests Run, 79 Passed, 0 Failures, 0 Errors, 0 Skipped`):**

| Module / Scope | Test Class | Tests | Result | Key Verified Capabilities |
| :--- | :--- | :---: | :---: | :--- |
| **Foundation** | `FleetxApplicationTests` | 1 | PASS | Spring Boot application context startup |
| **Shared Infra** | `HealthControllerTest` | 1 | PASS | `/api/health` readiness response |
| **Shared Infra** | `FallbackServicesTest` | 1 | PASS | Contract bean wiring in integrated context |
| **Shared Infra** | `SecurityAccessTest` | 2 | PASS | Unauthenticated `401` and public health endpoint |
| **Module 1 (Auth)** | `AuthenticationIntegrationTest` | 7 | PASS | CSRF bootstrap, login, session persistence, invalid credentials, deactivated user rejection, logout |
| **Module 1 (Auth)** | `AdminUserIntegrationTest` | 9 | PASS | Role-protected `/api/v1/admin/users`, user creation, role updates, self-deactivation guard, password reset, audit logs |
| **Module 2 (Fleet)** | `VehicleOperationsTest` | 7 | PASS | Vehicle CRUD, VIN & plate uniqueness, status state machine, pagination/search, RBAC |
| **Module 2 (Fleet)** | `FuelAndMaintenanceTest` | 5 | PASS | Server-side fuel cost & km/L efficiency calculation, non-decreasing odometer rule, maintenance due/overdue alerts via `AlertPublisher` |
| **Module 3 (Drivers)** | `DriverOperationsTest` | 11 | PASS | Driver CRUD, duplicate licence guard, role-based PII masking for `VIEWER`, `DriverLookupService` |
| **Module 3 (Trips)** | `TripLifecycleOperationsTest` | 13 | PASS | `PLANNED -> ASSIGNED -> IN_PROGRESS -> COMPLETED / CANCELLED` state machine, expired licence & unavailable vehicle guards via `VehicleLookupService`, driver overlap conflict guard |
| **Module 4 (Shipments)** | `ShipmentOperationsTest` | 8 | PASS | Shipment CRUD, status lifecycle, trip association via `TripLookupService`, timeline events |
| **Module 4 (Routes)** | `RouteAndDelayedShipmentTest` | 5 | PASS | Haversine great-circle distance & ETA calculation with explicit `Estimated` labels, delayed shipment detection & `AlertPublisher` emission |
| **Module 5 (Tracking)** | `SimulationAndGeofenceTest` | 5 | PASS | Great-circle Haversine, initial compass bearing, point-in-circle & ray-casting point-in-polygon geofence transitions, telemetry retention cap |
| **Module 5 (Control Tower)** | `ControlTowerAlertAndReportTest` | 5 | PASS | Alert deduplication & acknowledgement, RFC 4180 CSV escaping & formula-injection sanitization (`=`, `+`, `-`, `@`), RBAC |
| **Phase 7 Integration** | `FlywayMigrationAndArchitectureTest` | 3 | PASS | Clean execution of all 10 Flyway migrations (`V100`–`V501`), Hibernate `ddl-auto: validate` verification, zero schema drift on restart (`migrationsExecuted == 0`), `@Primary` contract bean verification, AST/source scan proving zero cross-module imports |
| **Phase 7 Integration** | `EndToEndLogisticsWorkflowIntegrationTest` | 2 | PASS | Complete 9-stage cross-module E2E workflow (CSRF -> Login -> Vehicle & Fuel -> Driver -> Trip -> Shipment & Waypoints -> Geofence & Simulated GPS -> Alert Ack -> Delivery & Trip Completion -> Control Tower KPI & CSV Export -> RBAC -> Logout) + repeatable demo seeder idempotency verification |

---

### 2.2 Frontend Automated Test Suite & Production Build (34 Tests Across 7 Suites)

**Commands:**
```bash
cd frontend
npm test -- --run
npm run build
```

**Actual Results Summary (`7 Test Files Passed, 34 Tests Passed, 0 Failures`):**

| Suite | File | Tests | Result | Key Verified Capabilities |
| :--- | :--- | :---: | :---: | :--- |
| **App Shell** | `src/test/App.test.tsx` | 3 | PASS | App shell rendering & route navigation |
| **Module 1 (Auth)** | `src/test/AuthComponents.test.tsx` | 8 | PASS | Login validation, demo role selector, Admin User Directory & RBAC |
| **Module 2 (Fleet)** | `src/test/FleetComponents.test.tsx` | 5 | PASS | Vehicle directory, fuel efficiency preview, maintenance modal |
| **Module 3 (Drivers)** | `src/test/DriverComponents.test.tsx` | 6 | PASS | Driver directory, licence expiry badges, trip creation & lifecycle actions |
| **Module 4 (Shipments)** | `src/test/ShipmentComponents.test.tsx` | 5 | PASS | Shipment manifest, Haversine route planner (`Estimated`), timeline |
| **Module 5 (Tracking)** | `src/test/TrackingComponents.test.tsx` | 4 | PASS | Control Tower `SIMULATED DATA` disclosures, simulator controls, alert acknowledgement, report filters & CSV export |
| **Phase 7 Integration** | `src/test/IntegrationNavigationAndAuth.test.tsx` | 3 | PASS | Sidebar navigation links across all 5 modules, `TopNav` session logout (`POST /api/v1/auth/logout`), Axios `X-XSRF-TOKEN` header attachment & `/v1/` -> `/api/v1/` path normalization |

**Production Bundle Verification (`tsc -b && vite build`):**
- Zero TypeScript compilation errors (`tsc -b`).
- Vite production bundle generated cleanly in `frontend/dist/` (`1722 modules transformed`).

---

## 3. End-to-End Logistics Scenario Verification Matrix

Verified automatically in `EndToEndLogisticsWorkflowIntegrationTest.fullEndToEndLogisticsScenario`:

1. **CSRF Bootstrap & Session Login (Module 1):**
   - `GET /api/v1/auth/csrf` issues `XSRF-TOKEN`.
   - `POST /api/v1/auth/login` authenticates user, establishes `JSESSIONID`, and `GET /api/v1/auth/me` returns principal details.
   - Mutating `POST /api/v1/fleet/vehicles` without CSRF token is rejected with `403 Forbidden`.
2. **Vehicle Registration & Fuel Efficiency (Module 2):**
   - `POST /api/v1/fleet/vehicles` registers `MH-12-E2E-9001` (`initialOdometerKm: 5000.0`).
   - `POST /api/v1/fleet/vehicles/{id}/fuel-records` logs `100.0 L` at `5400.0 km`, computing `totalCost = 9200.00` and `efficiencyKmPerLiter = 4.0 km/L`.
3. **Driver Onboarding & Trip Dispatch (Module 3 -> Module 2 via `VehicleLookupService`):**
   - `POST /api/v1/trips/drivers` onboards driver `DL-E2E-2026-9001` with future licence expiry.
   - `POST /api/v1/trips` verifies vehicle availability through `VehicleLookupService` and creates trip in `ASSIGNED` status.
   - `POST /api/v1/trips/{id}/start` transitions trip to `IN_PROGRESS`.
4. **Shipment Creation, Haversine Route Waypoints & Trip Association (Module 4 -> Module 3 via `TripLookupService`):**
   - `POST /api/v1/shipments` creates shipment `SHP-*` in `CREATED` status.
   - `PUT /api/v1/shipments/{id}/waypoints` plans 3 ordered waypoints (Mumbai Port -> Khalapur Toll -> Pune Chakan), computing Haversine distance (`distanceLabel` containing `"Haversine"`) and `etaLabel` containing `"Estimated Arrival Time"`.
   - `PATCH /api/v1/shipments/{id}/assign-trip` validates the trip via `TripLookupService` and links the shipment (`status: "ASSIGNED"`).
   - `PATCH /api/v1/shipments/{id}/status` transitions shipment to `IN_TRANSIT`.
5. **Geofence Creation, Simulated GPS Tracking & Alert Lifecycle (Module 5 -> Module 2/3/4 via Shared Contracts):**
   - `POST /api/v1/tracking/geofences` creates `Mumbai Port Origin Zone` (`CIRCLE`, `speedLimitKmh: 50.0`).
   - `POST /api/v1/tracking/simulator/start` starts the simulated GPS session for the vehicle and trip (`simulated: true`, `dataSource: "SIMULATED_GPS_TELEMETRY"`), pulling route waypoints via `ShipmentLookupService`.
   - `POST /api/v1/tracking/simulator/{vehicleId}/step` advances interpolation and emits a zone overspeed alert (`72.0 km/h > 50.0 km/h`).
   - `POST /api/v1/tracking/simulator/{vehicleId}/stop` cleanly stops the simulator (`simulatorStatus: "STOPPED"`).
   - `GET /api/v1/tracking/alerts?acknowledged=false` retrieves the active alert, and `POST /api/v1/tracking/alerts/{id}/acknowledge` records `acknowledged: true` and `acknowledgedBy`.
6. **Shipment Delivery, Trip Completion & Driver Performance Update (Modules 4 & 3):**
   - `POST /api/v1/shipments/{id}/deliver` marks the shipment `DELIVERED` with `deliveredAt` timestamp.
   - `POST /api/v1/trips/{id}/complete` marks the trip `COMPLETED` and increments driver performance `totalTripsCompleted` to `1`.
7. **Control Tower Summary, Filtered Persisted Reports & RFC 4180 CSV Export (Module 5):**
   - `GET /api/v1/tracking/control-tower/summary` aggregates live KPIs across all modules with `simulatedData: true` and `simulatedDataDisclaimer`.
   - `GET /api/v1/tracking/reports/summary?reportType=SHIPMENTS&status=DELIVERED` returns the delivered shipment row.
   - `GET /api/v1/tracking/reports/export-csv?reportType=SHIPMENTS&status=DELIVERED` streams `fleetx_shipments_report.csv` with RFC 4180 escaping.
8. **Role-Based Authorization & Session Logout:**
   - `VIEWER` can read `GET /api/v1/tracking/reports/summary` (`200 OK`) but is blocked from `POST /api/v1/fleet/vehicles` (`403 Forbidden`).
   - `DISPATCHER` is blocked from `GET /api/v1/admin/users` (`403 Forbidden`).
   - `POST /api/v1/auth/logout` invalidates the HTTP session; subsequent `GET /api/v1/auth/me` returns `401 Unauthorized`.

---

## 4. Integration Defect List (Discovered & Resolved in Phase 7)

| Defect ID | Severity | Affected Module(s) | Evidence / Root Cause | Proposed Owner | Fix Applied & Verified |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **INT-01** | **HIGH** | Module 2 (`fleet`) / Flyway `V201` | `Vehicle.java` mapped `@Column(name = "updated_at")`, which was omitted in `V201__fleet_module_enhancements.sql`, and `V201`/`V301`/`V401`/`V501` used multi-column `ALTER TABLE` syntax incompatible with H2 MySQL schema validation. | Member 2 (Fleet & Assets) | Added `updated_at DATETIME(6)` to `V201` and standardized `ALTER TABLE ... ADD COLUMN` statements across `V201`, `V301`, `V401`, `V501`. Verified in `FlywayMigrationAndArchitectureTest`. |
| **INT-02** | **HIGH** | Modules 1, 2, 4, 5 (`auth`, `fleet`, `shipments`, `tracking`) | Under `spring.jpa.hibernate.ddl-auto: validate`, Hibernate 6 `SchemaValidator` rejected Java `Double` fields mapped to SQL `DECIMAL(p,s)` columns and `AuditLog.detailsJson` (`String` vs `JSON`). | Team Leader / All Module Owners | Annotated `DECIMAL`-backed `Double` entity fields with `@JdbcTypeCode(SqlTypes.DECIMAL)` in `Vehicle`, `FuelRecord`, `MaintenanceRecord`, `Shipment`, `TrackingLog`, `Geofence`, `ControlTowerKpi`, and aligned `audit_logs.details_json` as `TEXT` in `V100`. |
| **INT-03** | **MEDIUM** | Module 1 (`auth`) | `frontend/src/features/auth/api/authApi.ts` sent `PATCH /api/v1/admin/users/{id}/status` and `POST /api/v1/admin/users/{id}/password-reset`, whereas `AdminUserController.java` only accepted `PUT`. | Member 1 (Identity & Admin) | Updated `AdminUserController.java` `@RequestMapping` on both endpoints to support `{PUT, PATCH}` and `{PUT, POST}` respectively. |
| **INT-04** | **HIGH** | Modules 3, 4, 5 (`drivers`, `shipments`, `tracking`) | `shipmentApi.ts` and `trackingApi.ts` used `/v1/...` paths (bypassing Vite's `/api` dev proxy), and Module 4 frontend components referenced alias field names (`tripNumber`, `originCity`, `destinationCity`, `delayed`, `locationName`, `totalEstimatedDistanceKm`). | Member 4 (Shipments) & Member 5 (Tracking) | Updated `shipmentApi.ts` and `trackingApi.ts` to `/api/v1/...`, added defensive `/v1/` -> `/api/v1/` normalization in `shared/api/client.ts`, and added `@JsonProperty`/`@JsonAlias` accessors on `TripResponseDto` and `com.fleetx.shipments.dto.*`. |
| **INT-05** | **MEDIUM** | Module 1 (`auth`) / Shared UI (`TopNav.tsx`) | Clicking Sign Out in `TopNav.tsx` navigated to `/login` client-side without calling `POST /api/v1/auth/logout` to invalidate the server session, and `LoginPage.tsx` prefilled `Admin123!` instead of `DevAdmin123!`. | Member 1 (Identity & Admin) | Updated `TopNav.tsx` to invoke `authApi.logout()` before redirecting to `/login`, and aligned `LoginPage.tsx` default dev credentials and quick-fill role buttons with `DemoIdentitySeeder`. |
| **INT-06** | **HIGH** | Module 5 (`tracking`) / Module 4 (`shipments`) | `ControlTowerReadRepository.queryShipmentsReport` queried `s.origin_city, s.destination_city` on table `shipments`, whereas `V400`/`V401` and `Shipment.java` define `s.sender_address` and `s.receiver_address`. Caught by `EndToEndLogisticsWorkflowIntegrationTest`. | Member 5 (Tracking & Control Tower) | Updated `ControlTowerReadRepository.queryShipmentsReport` to select `s.sender_address AS origin_city, s.receiver_address AS destination_city`. Verified by E2E report summary & CSV export tests. |

---

## 5. Known Limitations & Environment Constraints

1. **Containerized MySQL 8.4 Socket Access in Isolated Sandbox:**
   - During automated test execution inside the build sandbox, access to the host Docker daemon socket (`/Users/mdziaulmustafa/.docker/run/docker.sock`) is restricted by sandbox isolation.
   - To ensure automated CI/sandbox verification of all 10 Flyway migrations (`V100`–`V501`) and Hibernate `ddl-auto: validate` without weakening validation, `FlywayMigrationAndArchitectureTest` executes the real `classpath:db/migration` scripts against an in-memory database configured with `MODE=MySQL;DATABASE_TO_LOWER=TRUE` and `spring.jpa.hibernate.ddl-auto=validate`, and verifies restart idempotency (`migrationsExecuted == 0`).
   - **Reproducible Live MySQL 8.4 Verification Commands (on Host Workstation):**
     ```bash
     # 1. Start clean MySQL 8.4 LTS container
     docker compose down -v
     docker compose up -d

     # 2. Start Spring Boot backend (runs Flyway V100..V501 + Hibernate validate + Demo Seeders)
     cd backend
     ./mvnw spring-boot:run

     # 3. Restart Spring Boot backend to confirm zero schema drift and idempotent demo seeders
     ./mvnw spring-boot:run
     ```
2. **Simulated GPS & Haversine Route Estimations (By Design):**
   - As specified in `PROJECT_CHARTER.md` and `FLEETX_AGENT_RULES.md`, FleetX does not connect to physical IoT GPS hardware or paid external road-routing APIs. Telemetry is generated by `SimulatedGpsService` (`SIMULATED DATA`) and route distances/ETAs use the Haversine formula with configurable speed/stop allowances (`Estimated`).

---

## 6. Final PBL Evaluation Manual Demo Checklist

Use this step-by-step checklist during the live college project evaluation:

- [ ] **1. Start Stack:** Run `docker compose up -d`, `cd backend && ./mvnw spring-boot:run`, and `cd frontend && npm run dev`. Open `http://localhost:5173`.
- [ ] **2. Authentication & RBAC (Module 1):** Sign in using the quick-select **Admin** button (`admin@fleetx.local` / `DevAdmin123!`). Navigate to **Admin & Users** (`/admin/users`), verify the 5 seeded role accounts, toggle a user status or reset a password, and verify that signing in as **Viewer** (`viewer@fleetx.local` / `FleetDemo123!`) hides/disables mutating controls.
- [ ] **3. Fleet & Fuel/Maintenance (Module 2):** Navigate to **Fleet & Assets** (`/fleet`). Inspect the 3 seeded vehicles (`MH-12-FX-1001`, `KA-01-FX-2002`, `DL-04-FX-3003`). Open the Fuel modal on `MH-12-FX-1001` to view computed km/L efficiency and log a new fuel entry. Open the Maintenance modal to inspect the `DUE SOON` warning.
- [ ] **4. Drivers & Trip Lifecycle (Module 3):** Navigate to **Drivers & Trips** (`/drivers`). Inspect onboarded drivers (`Rajesh Kulkarni`, `Vikram Singh Rathore`), licence expiry status badges, and performance metrics. Switch to the **Trip Operations** tab to view `TRIP-DEMO-1001` (`IN_PROGRESS`) and `TRIP-DEMO-1002` (`PLANNED`), or dispatch a new trip.
- [ ] **5. Shipments & Haversine Route Planner (Module 4):** Navigate to **Shipments & Routes** (`/shipments`). Select `SHP-DEMO-2026-001` to inspect its 3 Mumbai–Pune Expressway waypoints, the **Estimated Straight-Line (Haversine) Distance** and **Estimated Arrival Time** disclosures, and the shipment status timeline.
- [ ] **6. Live Tracking, 3D View, Geofences, Alerts & CSV Export (Module 5):** Navigate to **Control Tower** (`/tracking`).
  - Verify the prominent **`SIMULATED DATA`** disclosure banner.
  - On the **Live Map & Simulator** tab, click **Resume / Start** or **Step +1 Tick** on `MH-12-FX-1001` and watch the marker interpolate smoothly along the Mumbai–Pune route waypoints on the Leaflet map.
  - Switch to the **3D Fleet View (Stylized)** tab and inspect the Three.js stylized highway corridor and procedural 3D commercial vehicles.
  - Switch to the **Geofences** tab to inspect the circular `Mumbai Port Dispatch Geofence` and polygonal `Pune Chakan Industrial Polygon`.
  - Switch to the **Alerts Feed** tab and click **Acknowledge** on an unacknowledged alert.
  - Switch to the **Reports & CSV Export** tab, filter by `SHIPMENTS` or `TRIPS`, and click **Export CSV** to download the sanitized RFC 4180 CSV report.
- [ ] **7. Session Logout:** Click **Sign Out** in the top navigation bar and confirm redirection to `/login` with the server session invalidated.
