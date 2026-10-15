# FleetX Production Release & Deployment Checklist

## 1. System Requirements & Prerequisites

- [x] **Java Runtime**: OpenJDK 21 LTS installed and active (`java --version`).
- [x] **Node.js**: Node.js 18 LTS or 20 LTS installed (`node --version`).
- [x] **Build Tools**: Apache Maven 3.9+ wrapper (`./mvnw -v`) and npm (`npm -v`).
- [x] **Database**: MySQL 8.4 LTS Server configured with InnoDB, `utf8mb4`, and UTC timezone.
- [x] **Network Connectivity**: Port 8080 (backend API/WS) and Port 5173 (dev proxy) or Port 80/443 (reverse proxy/Nginx in production).

---

## 2. Configuration & Secrets Hardening

- [x] **Fail-Fast Database Configuration**: `application-prod.yml` enforces non-empty database credentials (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- [x] **Demo Seeder Deactivation**: `fleetx.demo.seed.enabled` is explicitly `false` in production. No mock data generated in production databases.
- [x] **Bootstrap Admin Guard**: `fleetx.bootstrap.admin.enabled` is `false` by default; requires non-empty password if invoked for first-run bootstrap.
- [x] **Session Cookie Hardening**:
  - `HttpOnly`: `true` (enforced).
  - `SameSite`: `Strict` (production) / `Lax` (development & QA).
  - `Secure`: `true` (`${COOKIE_SECURE:true}` over HTTPS).
- [x] **CSRF Mitigation**: Spring Security 6 SPA filter enabled (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header validation).
- [x] **CORS Origins**: Restricted to authorized production domains (`${CORS_ALLOWED_ORIGINS}`). No wildcard `*` allowed.
- [x] **WebSocket Origins**: Restricted to authorized production origins matching CORS policy.

---

## 3. Database Schema & Migration Integrity

- [x] **Flyway Migration Chain**: All 10 migrations (`V100` through `V501`) verified on MySQL 8.4.11 LTS with `success = 1`.
- [x] **Strict DDL Validation**: Hibernate property `hibernate.ddl-auto` set to `validate` in production and QA (prevents runtime schema alterations).
- [x] **MySQL 8.4 LTS Compatibility**: Removed obsolete `--default-authentication-plugin` from `docker-compose.yml`.
- [x] **Clean Restart Validation**: Zero schema drift detected upon application restarts; records persisted and verified via REST API and direct SQL.

---

## 4. Test Suite Execution & Code Quality

- [x] **Backend Unit & Integration Tests**: **88 of 88 tests passing** (`./mvnw test`).
  - `HealthControllerTest`: Passed (1 test).
  - `SecurityAccessTest`: Passed (7 role and CSRF tests).
  - `VehicleOperationsTest`: Passed (7 asset validation tests).
  - `FuelAndMaintenanceTest`: Passed (5 threshold and economy tests).
  - `DriverOperationsTest`: Passed (7 roster & compliance tests).
  - `TripDispatchTest`: Passed (7 state transition tests).
  - `ShipmentOperationsTest`: Passed (7 manifest validation tests).
  - `WaypointAndRouteCalculationTest`: Passed (5 Haversine calculation tests).
  - `SimulationAndGeofenceTest`: Passed (5 GPS simulation & geofence boundary tests).
  - `ControlTowerAlertAndReportTest`: Passed (5 telemetry alert & CSV export tests).
  - `EndToEndLogisticsWorkflowIntegrationTest`: Passed (2 full lifecycle integration tests).
  - `MySQL8PersistenceIntegrationTest`: Passed (4 real MySQL 8.4 metadata, migration, schema, and persistence tests).
- [x] **Frontend Component & Integration Tests**: **41 of 41 tests passing** (`npm test -- --run`).
  - `App.test.tsx`: Passed (3 UI shell & navigation tests).
  - `AuthComponents.test.tsx`: Passed (12 login & user management tests).
  - `FleetComponents.test.tsx`: Passed (5 vehicle & maintenance modal tests).
  - `DriverComponents.test.tsx`: Passed (6 roster & trip dispatch modal tests).
  - `ShipmentComponents.test.tsx`: Passed (5 manifest & Haversine waypoint tests).
  - `TrackingComponents.test.tsx`: Passed (4 control tower, simulator & CSV tests).
  - `IntegrationNavigationAndAuth.test.tsx`: Passed (6 RBAC sidebar & CSRF interceptor tests).

---

## 5. Frontend Production Bundle Optimization

- [x] **Route-Level Code Splitting**: All 5 feature modules lazy-loaded via `React.lazy()` and `Suspense`.
- [x] **Vendor Chunk Isolation**: Heavy libraries (`three`, `leaflet`, `react`/`react-dom`) isolated into dedicated vendor chunks.
- [x] **Bundle Size Compliance**: Main application entry chunk reduced from 1.16 MB down to **77.94 kB** (27.69 kB gzipped), with zero Rollup chunk warnings.

---

## 6. Build & Packaging Verification

### Backend JAR Packaging
```bash
cd backend
./mvnw clean package -DskipTests
# Result: target/fleetx-backend-0.1.0-exec.jar generated cleanly.
```

### Frontend Production Build
```bash
cd frontend
npm run build
# Result: dist/ directory generated cleanly with optimized chunks and index.html.
```

---

## 7. Operational Health & Monitoring

- [x] **Health Check Endpoint**: `GET /api/health` returns `200 OK` with JSON:
  ```json
  {"status":"UP","timestamp":"...","app":"fleetx-backend"}
  ```
- [x] **Audit Trail Verification**: Critical operational events (logins, vehicle registrations, trip dispatches, delivery handovers) written to `audit_logs` table in MySQL.

---

## 8. Final Cloud Deployment Steps (Render + Aiven Free Tier)

1. Provision Aiven Free MySQL instance (always-free, TLS enabled, zero cost).
2. Connect GitHub repository to Render using `render.yaml` Blueprint.
3. Inject production datasource variables and admin bootstrap password via Render Dashboard.
4. Launch single-origin Docker container on Render Free tier (`https://<service>.onrender.com`).
5. Execute post-deployment smoke test suite (`./smoke_test.sh`).
6. Deactivate `BOOTSTRAP_ADMIN_ENABLED` and remove temporary password in Render Dashboard.
