# FleetX — Initial `main` Branch Baseline Inventory (Phase C)

**Coordinator:** Member 1 (Team Leader)  
**Target Branch:** `main`  
**Purpose:** Define the exact set of root manifests, shared backend infrastructure, cross-module contracts, fallback beans, frontend shell, and documentation files that **must be committed to `main` before Members 1–5 branch off** into `feature/identity-admin`, `feature/fleet-assets`, `feature/driver-trip-ops`, `feature/shipment-route`, and `feature/tracking-control-tower`.

---

## 1. Architectural Separation: Leader Baseline vs. Member 1 Business Module

To avoid conflating the Team Leader's repository governance role with Member 1's business module (`Identity & Admin`):
- **Shared Baseline on `main` (78 Existing Files + 1 Proposed `NEW` File):** Contains root config, Maven/npm manifests, `com.fleetx.shared/**`, `frontend/src/app/**`, `frontend/src/shared/**`, and `docs/**`. Maintained exclusively by the Team Leader on `main`.
- **Member 1 Business Module on `feature/identity-admin` (36 Existing Files):** Contains `com.fleetx.auth/**`, `V100`–`V101` migrations, `frontend/src/features/auth/**`, and Module 1 tests. Developed on `feature/identity-admin` and merged via Pull Request like any other module.

---

## 2. Complete Inventory of `main` Branch Baseline Files

### 2.1 Root Configuration, Build Manifests & Documentation (19 Existing Files + 1 `NEW` File)

| Exact File Path | Status | Why It Belongs to Common Baseline | Commit Before Branching? | Post-Baseline Maintainer | Dependent Modules | Overwrites / Moves Existing Work? |
| :--- | :---: | :--- | :---: | :--- | :---: | :---: |
| `.env.example` | EXISTING | Standardizes environment variable names for MySQL, Spring Boot, and Vite across all 5 workstations | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `.gitignore` | EXISTING | Prevents committing `node_modules/`, `target/`, `dist/`, `.env`, and `.DS_Store` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docker-compose.yml` | EXISTING | Provides uniform MySQL 8.4 LTS container setup for all 5 teammates | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `FLEETX_AGENT_RULES.md` | EXISTING | Enforces module boundaries, Flyway ranges, and contract rules | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `README.md` | EXISTING | Documents prerequisites, startup commands, and test execution | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/pom.xml` | EXISTING | Pins Java 21, Spring Boot 3.5.9, Security, JPA, Validation, WebSocket, Flyway, MySQL & H2 | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/mvnw` | EXISTING | Ensures reproducible Maven builds on macOS/Linux | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/mvnw.cmd` | EXISTING | Ensures reproducible Maven builds on Windows | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/.mvn/wrapper/maven-wrapper.properties` | EXISTING | Locks Maven 3.9.9 wrapper distribution | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/PROJECT_CHARTER.md` | EXISTING | Defines capstone scope and academic disclosures | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/SRS.md` | EXISTING | Software requirements specification for all 5 modules | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/ARCHITECTURE.md` | EXISTING | Modular monolith & contract inversion blueprint | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/API_CONTRACTS.md` | EXISTING | Shared REST & WebSocket contract reference | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/DATABASE_DESIGN.md` | EXISTING | Table schemas and Flyway version range assignments | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/TEAM_OWNERSHIP.md` | EXISTING | High-level team ownership rules | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/GITHUB_WORKFLOW.md` | EXISTING | Branching, commit, and PR workflow guide | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/TEST_PLAN.md` | EXISTING | Testing standards across backend and frontend | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/DEVELOPMENT_ROADMAP.md` | EXISTING | Milestone sequence | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `docs/DAILY_WEEKLY_REPORT_TEMPLATES.md` | EXISTING | Progress report templates | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `.github/workflows/ci.yml` | **NEW** | Runs `./mvnw test` and `npm test -- --run && npm run build` automatically on every PR to `main` | Recommended | Team Leader (`main`) | All (M1–M5) | No |

---

### 2.2 Backend Shared Infrastructure (`com.fleetx.shared` & Config — 32 Existing Files)

| Exact File Path | Status | Why It Belongs to Common Baseline | Commit Before Branching? | Post-Baseline Maintainer | Dependent Modules | Overwrites / Moves Existing Work? |
| :--- | :---: | :--- | :---: | :--- | :---: | :---: |
| `backend/src/main/java/com/fleetx/FleetxApplication.java` | EXISTING | Single `@SpringBootApplication` entry point | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/config/SecurityConfig.java` | EXISTING | Configures HTTP sessions, CSRF cookie token repository, CORS, and `@EnableMethodSecurity` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/config/WebMvcConfig.java` | EXISTING | Configures `/api/**` CORS rules for Vite SPA | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/config/WebSocketConfig.java` | EXISTING | Registers `/ws/fleetx` STOMP endpoint and `/topic` broker | Yes | Team Leader (`main`) | M5 (Tracking) | No |
| `backend/src/main/java/com/fleetx/shared/contract/VehicleLookupService.java` | EXISTING | Decouples M3 & M5 from M2's `VehicleRepository` | Yes | Team Leader (`main`) | M2, M3, M5 | No |
| `backend/src/main/java/com/fleetx/shared/contract/DriverLookupService.java` | EXISTING | Decouples M5 from M3's `DriverRepository` | Yes | Team Leader (`main`) | M3, M5 | No |
| `backend/src/main/java/com/fleetx/shared/contract/TripLookupService.java` | EXISTING | Decouples M4 & M5 from M3's `TripRepository` | Yes | Team Leader (`main`) | M3, M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/contract/ShipmentLookupService.java` | EXISTING | Decouples M5 from M4's `ShipmentRepository` | Yes | Team Leader (`main`) | M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/contract/AlertPublisher.java` | EXISTING | Allows M2, M3, M4 to emit operational alerts without importing M5 | Yes | Team Leader (`main`) | M2, M3, M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/controller/HealthController.java` | EXISTING | Provides `/api/health` readiness check | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/dto/ApiResponse.java` | EXISTING | Standard `{success, message, data, timestamp}` wrapper | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/dto/ApiErrorResponse.java` | EXISTING | Standard `{timestamp, status, errorCode, message, path, details}` wrapper | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/dto/PageResponse.java` | EXISTING | Standard pagination metadata wrapper | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/dto/ValidationErrorDto.java` | EXISTING | Field-level Bean Validation error item | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/dto/VehicleSummaryDto.java` | EXISTING | Cross-module immutable vehicle record | Yes | Team Leader (`main`) | M2, M3, M5 | No |
| `backend/src/main/java/com/fleetx/shared/dto/DriverSummaryDto.java` | EXISTING | Cross-module immutable driver record | Yes | Team Leader (`main`) | M3, M5 | No |
| `backend/src/main/java/com/fleetx/shared/dto/TripSummaryDto.java` | EXISTING | Cross-module immutable trip record | Yes | Team Leader (`main`) | M3, M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/dto/ShipmentSummaryDto.java` | EXISTING | Cross-module immutable shipment record | Yes | Team Leader (`main`) | M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/dto/AlertEventDto.java` | EXISTING | Cross-module immutable alert event record | Yes | Team Leader (`main`) | M2, M3, M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/exception/DomainException.java` | EXISTING | Base runtime exception with HTTP status & errorCode | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/exception/ResourceNotFoundException.java` | EXISTING | Standard `404 Not Found` exception | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/exception/ResourceConflictException.java` | EXISTING | Standard `409 Conflict` exception | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/exception/InvalidStateTransitionException.java` | EXISTING | Standard lifecycle state violation exception | Yes | Team Leader (`main`) | M2, M3, M4, M5 | No |
| `backend/src/main/java/com/fleetx/shared/exception/GlobalExceptionHandler.java` | EXISTING | Translates exceptions & validation failures into `ApiErrorResponse` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/java/com/fleetx/shared/fallback/SharedFallbackConfiguration.java` | EXISTING | Provides `@ConditionalOnMissingBean` implementations of all 5 contracts so feature branches compile and run independently | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/resources/application.yml` | EXISTING | Base datasource, Flyway, Hibernate `validate`, and session settings | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/main/resources/application-dev.yml` | EXISTING | Dev profile overrides and `fleetx.demo.seed.enabled` flag | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/test/resources/application-test.yml` | EXISTING | Test profile H2 MySQL-compatibility datasource config | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | EXISTING | Enables `mock-maker-subclass` for JDK 21 test execution | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `backend/src/test/java/com/fleetx/shared/HealthControllerTest.java` | EXISTING | Verifies baseline health endpoint | Yes | Team Leader (`main`) | Shared | No |
| `backend/src/test/java/com/fleetx/shared/FallbackServicesTest.java` | EXISTING | Verifies contract bean resolution | Yes | Team Leader (`main`) | Shared | No |
| `backend/src/test/java/com/fleetx/shared/SecurityAccessTest.java` | EXISTING | Verifies baseline security rules | Yes | Team Leader (`main`) | Shared | No |

---

### 2.3 Frontend Shared Shell, Central Route Registry & Reusable Components (25 Existing Files)

| Exact File Path | Status | Why It Belongs to Common Baseline | Commit Before Branching? | Post-Baseline Maintainer | Dependent Modules | Overwrites / Moves Existing Work? |
| :--- | :---: | :--- | :---: | :--- | :---: | :---: |
| `frontend/index.html` | EXISTING | Root SPA HTML document | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/package.json` | EXISTING | Installs React, Router, Bootstrap, Axios, Leaflet, Three.js, STOMP, Vitest upfront so teammates never edit `package.json` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/package-lock.json` | EXISTING | Deterministic npm dependency lockfile | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/tsconfig.json` | EXISTING | TypeScript strict compiler settings | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/tsconfig.node.json` | EXISTING | Vite build TypeScript config | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/vite.config.ts` | EXISTING | Configures `/api` and `/ws` proxy to `localhost:8080` and Vitest jsdom environment | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/main.tsx` | EXISTING | Imports Bootstrap CSS, `main.css`, and mounts `<App />` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/vite-env.d.ts` | EXISTING | Vite environment types | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/app/App.tsx` | EXISTING | Wraps central `AppRoutes` in `BrowserRouter` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/app/Layout.tsx` | EXISTING | Renders `Sidebar`, `TopNav`, and `<Outlet />` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/app/routes.tsx` | EXISTING | Central route registry importing `authRoutes`, `fleetRoutes`, `driverRoutes`, `shipmentRoutes`, `trackingRoutes` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/api/client.ts` | EXISTING | Shared Axios client with CSRF `X-XSRF-TOKEN` header injection and `/v1/` -> `/api/v1/` normalization | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/api/types.ts` | EXISTING | Shared TypeScript interfaces (`ApiResponse<T>`, `PageResponse<T>`) | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/Badge.tsx` | EXISTING | Shared status badge UI component | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/EmptyState.tsx` | EXISTING | Shared empty table/list state | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/ErrorState.tsx` | EXISTING | Shared error alert state | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/LoadingState.tsx` | EXISTING | Shared loading spinner state | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/PageHeader.tsx` | EXISTING | Shared page header component | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/Sidebar.tsx` | EXISTING | Navigation sidebar linking `/tracking`, `/fleet`, `/drivers`, `/shipments`, `/admin/users` | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/StatCard.tsx` | EXISTING | Shared KPI summary card | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/TopNav.tsx` | EXISTING | Top navigation bar with role indicator and Sign Out handler | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/components/UnauthorizedState.tsx` | EXISTING | Shared 403 unauthorized state | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/shared/styles/main.css` | EXISTING | Global typography, color palette, and layout CSS | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/test/setup.ts` | EXISTING | Vitest DOM assertions setup | Yes | Team Leader (`main`) | All (M1–M5) | No |
| `frontend/src/test/App.test.tsx` | EXISTING | Baseline app shell test suite | Yes | Team Leader (`main`) | Shared | No |

---

### 2.4 Minimal Feature Route Stubs Required if Branching Before Feature Code Exists
Because `frontend/src/app/routes.tsx` imports `authRoutes`, `fleetRoutes`, `driverRoutes`, `shipmentRoutes`, and `trackingRoutes` from `frontend/src/features/{auth,fleet,drivers,shipments,tracking}/routes.tsx`, each of those 5 `routes.tsx` files must exist on `main` (either as lightweight placeholder routes in Phase 1 or as the current completed `routes.tsx` files) so `npm run build` succeeds on every branch. Each module owner then owns only their own `frontend/src/features/<module>/routes.tsx` file and never touches `frontend/src/app/routes.tsx`.

---

## 3. Root Dependency & Foreign-Key Prerequisites Governance

1. **Zero Concurrent Manifest Edits:** All backend Maven dependencies (`pom.xml`) and frontend npm packages (`package.json` — including `leaflet`, `react-leaflet`, `three`, `@stomp/stompjs`, `bootstrap`, `lucide-react`) are already installed in the baseline. Individual members **must not** edit `pom.xml` or `package.json` on feature branches without explicit Team Leader approval.
2. **Flyway Baseline Prerequisite for Module 3 & Module 4:**
   - `V300__init_driver_trip_schema.sql` has foreign keys referencing `users(id)` (`V100`) and `vehicles(id)` (`V200`).
   - `V400__init_shipment_route_schema.sql` has a foreign key referencing `trips(id)` (`V300`).
   - `V500__init_tracking_geofence_schema.sql` has foreign keys referencing `vehicles(id)` (`V200`) and `trips(id)` (`V300`).
   - Therefore, as established in Phase 1, the baseline schema migrations (`V100`, `V200`, `V300`, `V400`, `V500`) belong in the `main` baseline before branches diverge, while each member owns their module enhancement migrations (`V101`, `V201`, `V301`, `V401`, `V501`) in their reserved numerical range!
