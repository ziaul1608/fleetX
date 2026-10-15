# FleetX — Project Repository Audit (Phase A)

**Audit Timestamp:** 2026-10-13T11:25:00+05:30  
**Workspace Root:** `/Users/mdziaulmustafa/.gemini/antigravity/scratch/fleetx`  
**Project Name:** FleetX — Intelligent Fleet & Logistics Management System  
**Team Size:** 5 Students (Member 1 = Team Leader)

---

## 1. Archive / ZIP & Git Repository Inspection

### 1.1 Archive / ZIP Check
- Inspected `/Users/mdziaulmustafa/.gemini/antigravity/scratch` and `/Users/mdziaulmustafa/.gemini/antigravity/scratch/fleetx`.
- **Finding:** No `.zip` archive is present. The project is already extracted and present directly as a full monorepo directory at `/Users/mdziaulmustafa/.gemini/antigravity/scratch/fleetx`.

### 1.2 Git Repository State & Commit History Check
- Executed read-only Git inspection commands (`git status --short --branch`, `git branch -a`, `git remote -v`, `git log --oneline -n 20`) inside `/Users/mdziaulmustafa/.gemini/antigravity/scratch/fleetx`.
- **Finding:** `fatal: not a git repository (or any of the parent directories): .git`.
- **Implication for Audit & Historical Reconciliation:**
  1. There is currently **no `.git` directory, no configured GitHub remote, no feature branches (`feature/*`), and no historical Git commit log** on disk.
  2. File modification timestamps (`mtime`) on disk are from **12–13 October 2026**.
  3. While the source files, Flyway migrations, and tests exist and pass verification today (13 October 2026), **no historical daily activity between 6 July 2026 and 6 October 2026 can be claimed as verified by Git commits**. All historical daily entries for that period must be classified as `UNVERIFIED` (with note that the resulting file exists on disk as of October 2026).

### 1.3 Explicitly Excluded Generated / Vendor / OS-Cache Paths
The following paths exist in the workspace but are excluded from first-party source code ownership:
- `frontend/node_modules/**` (npm third-party packages)
- `frontend/dist/**` (Vite compiled production bundle)
- `backend/target/**` (Maven compiled classes, surefire reports, and build artifacts)
- `.DS_Store`, `backend/.DS_Store`, `backend/src/.DS_Store`, `backend/src/main/.DS_Store`, `backend/src/main/java/.DS_Store`, `backend/src/main/java/com/.DS_Store` (macOS Finder metadata caches — should remain in `.gitignore` and be removed before `git init`)

### 1.4 Coverage Statement
Every first-party source file, migration script, configuration file, build manifest, test suite, and documentation file in `/Users/mdziaulmustafa/.gemini/antigravity/scratch/fleetx` (276 first-party files in total, excluding the newly created `docs/team-planning/` audit files) was directly inspected and verified on disk. Zero files were inaccessible.

---

## 2. Actual Project Directory Tree (First-Party Files)

```text
fleetx/
├── .env.example
├── .gitignore
├── FLEETX_AGENT_RULES.md
├── README.md
├── docker-compose.yml
├── docs/
│   ├── API_CONTRACTS.md
│   ├── ARCHITECTURE.md
│   ├── DAILY_WEEKLY_REPORT_TEMPLATES.md
│   ├── DATABASE_DESIGN.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── GITHUB_WORKFLOW.md
│   ├── INTEGRATION_TEST_REPORT.md
│   ├── PROJECT_CHARTER.md
│   ├── SRS.md
│   ├── TEAM_OWNERSHIP.md
│   └── TEST_PLAN.md
├── backend/
│   ├── .mvn/wrapper/maven-wrapper.properties
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/fleetx/
│       │   │   ├── FleetxApplication.java
│       │   │   ├── shared/
│       │   │   │   ├── config/ (SecurityConfig, WebMvcConfig, WebSocketConfig)
│       │   │   │   ├── contract/ (VehicleLookupService, DriverLookupService, TripLookupService, ShipmentLookupService, AlertPublisher)
│       │   │   │   ├── controller/ (HealthController)
│       │   │   │   ├── dto/ (ApiResponse, ApiErrorResponse, PageResponse, ValidationErrorDto, VehicleSummaryDto, DriverSummaryDto, TripSummaryDto, ShipmentSummaryDto, AlertEventDto)
│       │   │   │   ├── exception/ (DomainException, ResourceNotFoundException, ResourceConflictException, InvalidStateTransitionException, GlobalExceptionHandler)
│       │   │   │   └── fallback/ (SharedFallbackConfiguration)
│       │   │   ├── auth/          # Module 1 (21 Java files)
│       │   │   ├── fleet/         # Module 2 (29 Java files)
│       │   │   ├── drivers/       # Module 3 (25 Java files)
│       │   │   ├── shipments/     # Module 4 (23 Java files)
│       │   │   └── tracking/      # Module 5 (23 Java files)
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/  # 10 Flyway SQL scripts (V100..V501)
│       └── test/
│           ├── java/com/fleetx/   # 16 JUnit 5 / Spring Boot Test classes
│           └── resources/
│               ├── application-test.yml
│               └── mockito-extensions/org.mockito.plugins.MockMaker
└── frontend/
    ├── index.html
    ├── package.json
    ├── package-lock.json
    ├── tsconfig.json
    ├── tsconfig.node.json
    ├── vite.config.ts
    └── src/
        ├── main.tsx
        ├── vite-env.d.ts
        ├── app/ (App.tsx, Layout.tsx, routes.tsx)
        ├── shared/
        │   ├── api/ (client.ts, types.ts)
        │   ├── components/ (Badge, EmptyState, ErrorState, LoadingState, PageHeader, Sidebar, StatCard, TopNav, UnauthorizedState)
        │   └── styles/ (main.css)
        ├── features/
        │   ├── auth/              # Module 1 (10 TS/TSX files)
        │   ├── fleet/             # Module 2 (10 TS/TSX files)
        │   ├── drivers/           # Module 3 (10 TS/TSX files)
        │   ├── shipments/         # Module 4 (10 TS/TSX files)
        │   └── tracking/          # Module 5 (8 TS/TSX files)
        └── test/                  # 7 Vitest test suites + setup.ts
```

---

## 3. Language, Framework & Dependency Inventory

### 3.1 Backend (`backend/pom.xml`)
- **Language:** Java 21 (`<java.version>21</java.version>`)
- **Framework:** Spring Boot `3.5.9` (`spring-boot-starter-parent:3.5.9`)
- **Dependencies:**
  - `spring-boot-starter-web` (REST controllers, Jackson serialization)
  - `spring-boot-starter-data-jpa` (Hibernate 6 ORM, Spring Data repositories)
  - `spring-boot-starter-security` (HTTP session auth, BCrypt, CSRF protection, `@PreAuthorize` RBAC)
  - `spring-boot-starter-validation` (Jakarta Bean Validation)
  - `spring-boot-starter-websocket` (STOMP over WebSocket message broker)
  - `flyway-core` & `flyway-mysql` (Database schema migrations `V100`–`V501`)
  - `mysql-connector-j` (Runtime MySQL 8.4 JDBC driver)
  - `spring-boot-starter-test`, `spring-security-test`, `com.h2database:h2` (Test-scope verification)

### 3.2 Frontend (`frontend/package.json`)
- **Language & Build:** TypeScript `^5.6.2`, Vite `^5.4.10`, React `^18.3.1`, React DOM `^18.3.1`
- **Routing & HTTP:** `react-router-dom` `^6.28.0`, `axios` `^1.7.7`
- **UI & Styling:** `bootstrap` `^5.3.3`, `lucide-react` `^0.454.0`, custom CSS (`src/shared/styles/main.css`)
- **Geospatial & 3D Visualization:** `leaflet` `^1.9.4`, `react-leaflet` `^4.2.1`, `three` `^0.170.0`, `@types/three` `^0.170.0`
- **Real-Time Messaging:** `@stomp/stompjs` `^7.0.0`
- **Testing:** `vitest` `^2.1.4`, `@testing-library/react` `^16.0.1`, `@testing-library/jest-dom` `^6.6.3`, `jsdom` `^25.0.1`

### 3.3 Infrastructure (`docker-compose.yml`)
- **Database:** `mysql:8.4` (`container_name: fleetx-mysql`, port `3306`, healthcheck via `mysqladmin ping`, persistent volume `fleetx_mysql_data`).

---

## 4. Complete File-by-File Classification Inventory

Classification Legend:
- **Functional Status:** `Existing and functional` | `Existing but incomplete` | `Existing but requiring correction` | `New file genuinely required (NEW)`
- **Ownership Scope:** `Shared infrastructure` | `Module-exclusive (M1..M5)` | `Integration-only` | `Obsolete/duplicate candidate`

### 4.1 Root, Infrastructure & Documentation Files (20 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes / Dependencies |
| :--- | :--- | :--- | :--- |
| `.env.example` | Existing and functional | Shared infrastructure | Template for MySQL, Spring Boot, Bootstrap Admin, and Vite env vars |
| `.gitignore` | Existing and functional | Shared infrastructure | Excludes `target/`, `node_modules/`, `dist/`, `.env`, `.DS_Store` |
| `docker-compose.yml` | Existing and functional | Shared infrastructure | MySQL 8.4 LTS container definition |
| `FLEETX_AGENT_RULES.md` | Existing and functional | Shared infrastructure | Architectural & module boundary governance rules |
| `README.md` | Existing and functional | Shared infrastructure | Monorepo setup, run, and test instructions |
| `backend/pom.xml` | Existing and functional | Shared infrastructure | Root Maven manifest; unsafe for concurrent edits |
| `backend/mvnw` | Existing and functional | Shared infrastructure | POSIX Maven wrapper script |
| `backend/mvnw.cmd` | Existing and functional | Shared infrastructure | Windows Maven wrapper script |
| `backend/.mvn/wrapper/maven-wrapper.properties` | Existing and functional | Shared infrastructure | Pins Maven 3.9.9 distribution |
| `docs/PROJECT_CHARTER.md` | Existing and functional | Shared infrastructure | Scope, academic constraints, non-goals |
| `docs/SRS.md` | Existing and functional | Shared infrastructure | Functional & non-functional requirements |
| `docs/ARCHITECTURE.md` | Existing and functional | Shared infrastructure | Modular monolith & contract inversion architecture |
| `docs/API_CONTRACTS.md` | Existing and functional | Shared infrastructure | REST API & WebSocket topic contracts |
| `docs/DATABASE_DESIGN.md` | Existing and functional | Shared infrastructure | ER design and Flyway V100–V599 governance |
| `docs/TEAM_OWNERSHIP.md` | Existing and functional | Shared infrastructure | High-level 5-member role matrix |
| `docs/GITHUB_WORKFLOW.md` | Existing and functional | Shared infrastructure | Branching, PR, and review rules |
| `docs/TEST_PLAN.md` | Existing and functional | Shared infrastructure | Unit, integration, and manual test strategy |
| `docs/DEVELOPMENT_ROADMAP.md` | Existing and functional | Shared infrastructure | Phased delivery roadmap |
| `docs/DAILY_WEEKLY_REPORT_TEMPLATES.md` | Existing and functional | Shared infrastructure | Student progress reporting templates |
| `docs/INTEGRATION_TEST_REPORT.md` | Existing and functional | Integration-only | Phase 7 integration test execution report & defect log |

### 4.2 Backend Shared Infrastructure (`com.fleetx.shared` & Resources — 29 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes / Multi-Module Impact |
| :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/FleetxApplication.java` | Existing and functional | Shared infrastructure | `@SpringBootApplication`, `@EnableScheduling` |
| `backend/src/main/java/com/fleetx/shared/config/SecurityConfig.java` | Existing and functional | Shared infrastructure | HTTP sessions, CSRF cookie repository, BCrypt, CORS |
| `backend/src/main/java/com/fleetx/shared/config/WebMvcConfig.java` | Existing and functional | Shared infrastructure | SPA CORS & MVC configuration |
| `backend/src/main/java/com/fleetx/shared/config/WebSocketConfig.java` | Existing and functional | Shared infrastructure | STOMP `/ws/fleetx` endpoint & `/topic` broker |
| `backend/src/main/java/com/fleetx/shared/contract/VehicleLookupService.java` | Existing and functional | Shared infrastructure | Provided by M2 (`VehicleService`), consumed by M3 & M5 |
| `backend/src/main/java/com/fleetx/shared/contract/DriverLookupService.java` | Existing and functional | Shared infrastructure | Provided by M3 (`DriverService`), consumed by M5 |
| `backend/src/main/java/com/fleetx/shared/contract/TripLookupService.java` | Existing and functional | Shared infrastructure | Provided by M3 (`TripService`), consumed by M4 & M5 |
| `backend/src/main/java/com/fleetx/shared/contract/ShipmentLookupService.java` | Existing and functional | Shared infrastructure | Provided by M4 (`ShipmentService`), consumed by M5 |
| `backend/src/main/java/com/fleetx/shared/contract/AlertPublisher.java` | Existing and functional | Shared infrastructure | Provided by M5 (`TrackingService`), consumed by M2, M3, M4 |
| `backend/src/main/java/com/fleetx/shared/controller/HealthController.java` | Existing and functional | Shared infrastructure | `GET /api/health` & `/api/v1/health` |
| `backend/src/main/java/com/fleetx/shared/dto/ApiResponse.java` | Existing and functional | Shared infrastructure | Standard JSON success envelope across all controllers |
| `backend/src/main/java/com/fleetx/shared/dto/ApiErrorResponse.java` | Existing and functional | Shared infrastructure | Standard JSON error envelope |
| `backend/src/main/java/com/fleetx/shared/dto/PageResponse.java` | Existing and functional | Shared infrastructure | Standard paginated response wrapper |
| `backend/src/main/java/com/fleetx/shared/dto/ValidationErrorDto.java` | Existing and functional | Shared infrastructure | Field-level validation error details |
| `backend/src/main/java/com/fleetx/shared/dto/VehicleSummaryDto.java` | Existing and functional | Shared infrastructure | Cross-module vehicle projection |
| `backend/src/main/java/com/fleetx/shared/dto/DriverSummaryDto.java` | Existing and functional | Shared infrastructure | Cross-module driver projection |
| `backend/src/main/java/com/fleetx/shared/dto/TripSummaryDto.java` | Existing and functional | Shared infrastructure | Cross-module trip projection |
| `backend/src/main/java/com/fleetx/shared/dto/ShipmentSummaryDto.java` | Existing and functional | Shared infrastructure | Cross-module shipment projection |
| `backend/src/main/java/com/fleetx/shared/dto/AlertEventDto.java` | Existing and functional | Shared infrastructure | Cross-module alert payload |
| `backend/src/main/java/com/fleetx/shared/exception/DomainException.java` | Existing and functional | Shared infrastructure | Base domain runtime exception |
| `backend/src/main/java/com/fleetx/shared/exception/ResourceNotFoundException.java` | Existing and functional | Shared infrastructure | `404` domain exception |
| `backend/src/main/java/com/fleetx/shared/exception/ResourceConflictException.java` | Existing and functional | Shared infrastructure | `409` domain exception |
| `backend/src/main/java/com/fleetx/shared/exception/InvalidStateTransitionException.java` | Existing and functional | Shared infrastructure | `422`/`400` lifecycle transition exception |
| `backend/src/main/java/com/fleetx/shared/exception/GlobalExceptionHandler.java` | Existing and functional | Shared infrastructure | `@RestControllerAdvice` for uniform error responses |
| `backend/src/main/java/com/fleetx/shared/fallback/SharedFallbackConfiguration.java` | Existing and functional | Shared infrastructure | `@ConditionalOnMissingBean` stubs for isolated branch builds |
| `backend/src/main/resources/application.yml` | Existing and functional | Shared infrastructure | Base Spring Boot, Flyway, JPA (`validate`), and security config |
| `backend/src/main/resources/application-dev.yml` | Existing and functional | Shared infrastructure | Dev profile properties & `fleetx.demo.seed.enabled` |
| `backend/src/test/resources/application-test.yml` | Existing and functional | Shared infrastructure | H2 MySQL-mode test profile config |
| `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | Existing and functional | Shared infrastructure | Configures `mock-maker-subclass` for Java 21 compatibility |

### 4.3 Module 1 — Identity & Admin Files (36 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `backend/src/main/resources/db/migration/V100__init_identity_schema.sql` | Existing and functional | Module-exclusive (M1) | Creates `roles`, `users`, `user_roles`, `audit_logs` |
| `backend/src/main/resources/db/migration/V101__seed_roles_and_admin.sql` | Existing and functional | Module-exclusive (M1) | Seeds 5 canonical roles (`ADMIN`, `FLEET_MANAGER`, `DISPATCHER`, `DRIVER`, `VIEWER`) |
| `backend/src/main/java/com/fleetx/auth/entity/Role.java` | Existing and functional | Module-exclusive (M1) | JPA entity for `roles` |
| `backend/src/main/java/com/fleetx/auth/entity/User.java` | Existing and functional | Module-exclusive (M1) | JPA entity for `users` & `user_roles` |
| `backend/src/main/java/com/fleetx/auth/entity/AuditLog.java` | Existing and functional | Module-exclusive (M1) | JPA entity for `audit_logs` |
| `backend/src/main/java/com/fleetx/auth/repository/RoleRepository.java` | Existing and functional | Module-exclusive (M1) | Spring Data repository for `Role` |
| `backend/src/main/java/com/fleetx/auth/repository/UserRepository.java` | Existing and functional | Module-exclusive (M1) | Spring Data repository for `User` with search/filter |
| `backend/src/main/java/com/fleetx/auth/repository/AuditLogRepository.java` | Existing and functional | Module-exclusive (M1) | Spring Data repository for `AuditLog` |
| `backend/src/main/java/com/fleetx/auth/dto/LoginRequest.java` | Existing and functional | Module-exclusive (M1) | Validated login credentials DTO |
| `backend/src/main/java/com/fleetx/auth/dto/UserCreateRequest.java` | Existing and functional | Module-exclusive (M1) | Admin user creation request DTO |
| `backend/src/main/java/com/fleetx/auth/dto/UserUpdateRequest.java` | Existing and functional | Module-exclusive (M1) | Admin user update request DTO |
| `backend/src/main/java/com/fleetx/auth/dto/UserStatusUpdateRequest.java` | Existing and functional | Module-exclusive (M1) | Account activation/deactivation DTO |
| `backend/src/main/java/com/fleetx/auth/dto/AdminPasswordResetRequest.java` | Existing and functional | Module-exclusive (M1) | Admin password reset DTO |
| `backend/src/main/java/com/fleetx/auth/dto/UserResponseDto.java` | Existing and functional | Module-exclusive (M1) | Safe user response projection (never exposes password hash) |
| `backend/src/main/java/com/fleetx/auth/dto/AuditLogResponseDto.java` | Existing and functional | Module-exclusive (M1) | Audit log response DTO |
| `backend/src/main/java/com/fleetx/auth/service/CustomUserDetailsService.java` | Existing and functional | Module-exclusive (M1) | Spring Security `UserDetailsService` implementation |
| `backend/src/main/java/com/fleetx/auth/service/AuditService.java` | Existing and functional | Module-exclusive (M1) | Persists security and administrative audit trail events |
| `backend/src/main/java/com/fleetx/auth/service/AuthService.java` | Existing and functional | Module-exclusive (M1) | Session login, logout, and `/me` resolution |
| `backend/src/main/java/com/fleetx/auth/service/AdminUserService.java` | Existing and functional | Module-exclusive (M1) | User CRUD, role assignment, sole-admin protection, password reset |
| `backend/src/main/java/com/fleetx/auth/controller/AuthController.java` | Existing and functional | Module-exclusive (M1) | `/api/v1/auth/{csrf,login,logout,me}` |
| `backend/src/main/java/com/fleetx/auth/controller/AdminUserController.java` | Existing and functional | Module-exclusive (M1) | `/api/v1/admin/users/**` (`@PreAuthorize("hasRole('ADMIN')")`) |
| `backend/src/main/java/com/fleetx/auth/config/AdminBootstrapRunner.java` | Existing and functional | Module-exclusive (M1) | Bootstraps initial admin user from env vars |
| `backend/src/main/java/com/fleetx/auth/config/DemoIdentitySeeder.java` | Existing and functional | Integration-only (M1) | `@Order(10)` repeatable dev role/user seeder |
| `frontend/src/features/auth/types.ts` | Existing and functional | Module-exclusive (M1) | Auth & user management TypeScript interfaces |
| `frontend/src/features/auth/api/authApi.ts` | Existing and functional | Module-exclusive (M1) | Frontend API client for `/api/v1/auth` and `/api/v1/admin/users` |
| `frontend/src/features/auth/pages/LoginPage.tsx` | Existing and functional | Module-exclusive (M1) | Login screen with dev role quick-fill buttons |
| `frontend/src/features/auth/pages/UsersPage.tsx` | Existing and functional | Module-exclusive (M1) | Admin user management table, filters, and audit view |
| `frontend/src/features/auth/pages/ProfilePage.tsx` | Existing and functional | Module-exclusive (M1) | Current user session & role profile page |
| `frontend/src/features/auth/components/CreateUserModal.tsx` | Existing and functional | Module-exclusive (M1) | Modal for creating new users |
| `frontend/src/features/auth/components/EditUserModal.tsx` | Existing and functional | Module-exclusive (M1) | Modal for updating user roles and profile |
| `frontend/src/features/auth/components/DeactivateConfirmModal.tsx` | Existing and functional | Module-exclusive (M1) | Confirmation dialog for account status toggle |
| `frontend/src/features/auth/components/PasswordResetModal.tsx` | Existing and functional | Module-exclusive (M1) | Admin password reset modal |
| `frontend/src/features/auth/routes.tsx` | Existing and functional | Module-exclusive (M1) | Exports `authRoutes` (`/login`, `/admin/users`, `/profile`) |
| `backend/src/test/java/com/fleetx/auth/AuthLifecycleTest.java` | Existing and functional | Module-exclusive (M1) | 7 integration tests for CSRF, login, session, logout |
| `backend/src/test/java/com/fleetx/auth/AdminUserOperationsTest.java` | Existing and functional | Module-exclusive (M1) | 9 integration tests for admin user CRUD & RBAC |
| `frontend/src/test/AuthComponents.test.tsx` | Existing and functional | Module-exclusive (M1) | 8 frontend component tests for Login & UsersPage |

### 4.4 Module 2 — Fleet & Asset Management Files (44 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `backend/src/main/resources/db/migration/V200__init_fleet_schema.sql` | Existing and functional | Module-exclusive (M2) | Creates `vehicles`, `fuel_records`, `maintenance_records`, `service_warnings` |
| `backend/src/main/resources/db/migration/V201__fleet_module_enhancements.sql` | Existing and functional | Module-exclusive (M2) | Adds `active`, `updated_at`, `price_per_liter`, `efficiency_km_per_liter`, `status` |
| `backend/src/main/java/com/fleetx/fleet/entity/Vehicle.java` | Existing and functional | Module-exclusive (M2) | JPA entity for `vehicles` |
| `backend/src/main/java/com/fleetx/fleet/entity/VehicleStatus.java` | Existing and functional | Module-exclusive (M2) | Canonical vehicle statuses enum |
| `backend/src/main/java/com/fleetx/fleet/entity/VehicleType.java` | Existing and functional | Module-exclusive (M2) | Canonical vehicle types enum |
| `backend/src/main/java/com/fleetx/fleet/entity/FuelType.java` | Existing and functional | Module-exclusive (M2) | Canonical fuel types enum |
| `backend/src/main/java/com/fleetx/fleet/entity/FuelRecord.java` | Existing and functional | Module-exclusive (M2) | JPA entity for `fuel_records` |
| `backend/src/main/java/com/fleetx/fleet/entity/MaintenanceRecord.java` | Existing and functional | Module-exclusive (M2) | JPA entity for `maintenance_records` |
| `backend/src/main/java/com/fleetx/fleet/entity/MaintenanceStatus.java` | Existing and functional | Module-exclusive (M2) | Maintenance record lifecycle enum |
| `backend/src/main/java/com/fleetx/fleet/entity/ServiceType.java` | Existing and functional | Module-exclusive (M2) | Maintenance service category enum |
| `backend/src/main/java/com/fleetx/fleet/entity/ServiceWarning.java` | Existing and functional | Module-exclusive (M2) | JPA entity for `service_warnings` |
| `backend/src/main/java/com/fleetx/fleet/entity/WarningSeverity.java` | Existing and functional | Module-exclusive (M2) | Warning severity enum |
| `backend/src/main/java/com/fleetx/fleet/entity/WarningType.java` | Existing and functional | Module-exclusive (M2) | Warning type enum (`SERVICE_DUE`, `SERVICE_OVERDUE`) |
| `backend/src/main/java/com/fleetx/fleet/repository/VehicleRepository.java` | Existing and functional | Module-exclusive (M2) | Vehicle JPA repository |
| `backend/src/main/java/com/fleetx/fleet/repository/FuelRecordRepository.java` | Existing and functional | Module-exclusive (M2) | FuelRecord JPA repository |
| `backend/src/main/java/com/fleetx/fleet/repository/MaintenanceRecordRepository.java` | Existing and functional | Module-exclusive (M2) | MaintenanceRecord JPA repository |
| `backend/src/main/java/com/fleetx/fleet/repository/ServiceWarningRepository.java` | Existing and functional | Module-exclusive (M2) | ServiceWarning JPA repository |
| `backend/src/main/java/com/fleetx/fleet/dto/VehicleCreateRequest.java` | Existing and functional | Module-exclusive (M2) | Validated vehicle registration request |
| `backend/src/main/java/com/fleetx/fleet/dto/VehicleUpdateRequest.java` | Existing and functional | Module-exclusive (M2) | Validated vehicle update request |
| `backend/src/main/java/com/fleetx/fleet/dto/VehicleStatusUpdateRequest.java` | Existing and functional | Module-exclusive (M2) | Vehicle operational status transition request |
| `backend/src/main/java/com/fleetx/fleet/dto/VehicleResponseDto.java` | Existing and functional | Module-exclusive (M2) | Vehicle detail & efficiency projection |
| `backend/src/main/java/com/fleetx/fleet/dto/FuelRecordCreateRequest.java` | Existing and functional | Module-exclusive (M2) | Validated fuel log request |
| `backend/src/main/java/com/fleetx/fleet/dto/FuelRecordResponseDto.java` | Existing and functional | Module-exclusive (M2) | Fuel log response with computed totalCost & km/L |
| `backend/src/main/java/com/fleetx/fleet/dto/MaintenanceRecordCreateRequest.java` | Existing and functional | Module-exclusive (M2) | Maintenance scheduling/logging request |
| `backend/src/main/java/com/fleetx/fleet/dto/MaintenanceRecordResponseDto.java` | Existing and functional | Module-exclusive (M2) | Maintenance record response DTO |
| `backend/src/main/java/com/fleetx/fleet/dto/ServiceWarningResponseDto.java` | Existing and functional | Module-exclusive (M2) | Service warning response DTO |
| `backend/src/main/java/com/fleetx/fleet/service/VehicleService.java` | Existing and functional | Module-exclusive (M2) | `@Primary` implementation of `VehicleLookupService` + vehicle CRUD |
| `backend/src/main/java/com/fleetx/fleet/service/FuelService.java` | Existing and functional | Module-exclusive (M2) | Fuel cost & odometer delta km/L efficiency calculation |
| `backend/src/main/java/com/fleetx/fleet/service/MaintenanceService.java` | Existing and functional | Module-exclusive (M2) | Maintenance scheduling & `AlertPublisher` integration |
| `backend/src/main/java/com/fleetx/fleet/service/DemoFleetSeeder.java` | Existing and functional | Integration-only (M2) | `@Order(20)` repeatable dev fleet/fuel/maintenance seeder |
| `backend/src/main/java/com/fleetx/fleet/controller/VehicleController.java` | Existing and functional | Module-exclusive (M2) | `/api/v1/fleet/vehicles/**` & fuel endpoints |
| `backend/src/main/java/com/fleetx/fleet/controller/FleetMaintenanceController.java` | Existing and functional | Module-exclusive (M2) | `/api/v1/fleet/maintenance/**` & warnings endpoints |
| `frontend/src/features/fleet/types.ts` | Existing and functional | Module-exclusive (M2) | Fleet, fuel, and maintenance TypeScript types |
| `frontend/src/features/fleet/api/fleetApi.ts` | Existing and functional | Module-exclusive (M2) | Frontend API client for `/api/v1/fleet/**` |
| `frontend/src/features/fleet/pages/VehiclesPage.tsx` | Existing and functional | Module-exclusive (M2) | Vehicle inventory, status filters, and modal triggers |
| `frontend/src/features/fleet/pages/MaintenancePage.tsx` | Existing and functional | Module-exclusive (M2) | Service warnings & maintenance schedule view |
| `frontend/src/features/fleet/components/CreateVehicleModal.tsx` | Existing and functional | Module-exclusive (M2) | Vehicle registration modal |
| `frontend/src/features/fleet/components/EditVehicleModal.tsx` | Existing and functional | Module-exclusive (M2) | Vehicle update & status change modal |
| `frontend/src/features/fleet/components/VehicleDetailsModal.tsx` | Existing and functional | Module-exclusive (M2) | Vehicle fuel history, efficiency & maintenance tabs |
| `frontend/src/features/fleet/components/LogFuelModal.tsx` | Existing and functional | Module-exclusive (M2) | Fuel entry modal with live cost calculation preview |
| `frontend/src/features/fleet/components/ScheduleMaintenanceModal.tsx` | Existing and functional | Module-exclusive (M2) | Maintenance scheduling modal |
| `frontend/src/features/fleet/routes.tsx` | Existing and functional | Module-exclusive (M2) | Exports `fleetRoutes` (`/fleet`, `/fleet/maintenance`) |
| `backend/src/test/java/com/fleetx/fleet/VehicleOperationsTest.java` | Existing and functional | Module-exclusive (M2) | 7 vehicle CRUD, uniqueness & RBAC tests |
| `backend/src/test/java/com/fleetx/fleet/FuelAndMaintenanceTest.java` | Existing and functional | Module-exclusive (M2) | 5 fuel efficiency & maintenance warning tests (Note: uses deprecated `@SpyBean`) |
| `frontend/src/test/FleetComponents.test.tsx` | Existing and functional | Module-exclusive (M2) | 5 frontend unit tests for Fleet UI |

### 4.5 Module 3 — Driver & Trip Operations Files (41 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `backend/src/main/resources/db/migration/V300__init_driver_trip_schema.sql` | Existing and functional | Module-exclusive (M3) | Creates `drivers`, `trips`, `trip_status_history`, `driver_performance_metrics` |
| `backend/src/main/resources/db/migration/V301__driver_trip_enhancements.sql` | Existing and functional | Module-exclusive (M3) | Adds driver profile columns, trip `distance_km`, performance metrics columns |
| `backend/src/main/java/com/fleetx/drivers/entity/Driver.java` | Existing and functional | Module-exclusive (M3) | JPA entity for `drivers` |
| `backend/src/main/java/com/fleetx/drivers/entity/DriverAvailability.java` | Existing and functional | Module-exclusive (M3) | Driver availability enum |
| `backend/src/main/java/com/fleetx/drivers/entity/DriverPerformanceMetrics.java` | Existing and functional | Module-exclusive (M3) | JPA entity for `driver_performance_metrics` |
| `backend/src/main/java/com/fleetx/drivers/entity/Trip.java` | Existing and functional | Module-exclusive (M3) | JPA entity for `trips` |
| `backend/src/main/java/com/fleetx/drivers/entity/TripStatus.java` | Existing and functional | Module-exclusive (M3) | Canonical trip statuses (`PLANNED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `backend/src/main/java/com/fleetx/drivers/entity/TripStatusHistory.java` | Existing and functional | Module-exclusive (M3) | JPA entity for `trip_status_history` |
| `backend/src/main/java/com/fleetx/drivers/repository/DriverRepository.java` | Existing and functional | Module-exclusive (M3) | Driver JPA repository |
| `backend/src/main/java/com/fleetx/drivers/repository/DriverPerformanceMetricsRepository.java` | Existing and functional | Module-exclusive (M3) | DriverPerformanceMetrics JPA repository |
| `backend/src/main/java/com/fleetx/drivers/repository/TripRepository.java` | Existing and functional | Module-exclusive (M3) | Trip JPA repository with active overlap queries |
| `backend/src/main/java/com/fleetx/drivers/repository/TripStatusHistoryRepository.java` | Existing and functional | Module-exclusive (M3) | TripStatusHistory JPA repository |
| `backend/src/main/java/com/fleetx/drivers/dto/DriverCreateRequest.java` | Existing and functional | Module-exclusive (M3) | Driver onboarding DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/DriverUpdateRequest.java` | Existing and functional | Module-exclusive (M3) | Driver update DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/DriverStatusUpdateRequest.java` | Existing and functional | Module-exclusive (M3) | Driver availability update DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/DriverResponseDto.java` | Existing and functional | Module-exclusive (M3) | Driver response DTO with PII masking support for `VIEWER` |
| `backend/src/main/java/com/fleetx/drivers/dto/DriverPerformanceResponseDto.java` | Existing and functional | Module-exclusive (M3) | Driver performance metrics DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/TripCreateRequest.java` | Existing and functional | Module-exclusive (M3) | Trip creation/dispatch request DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/TripStatusUpdateRequest.java` | Existing and functional | Module-exclusive (M3) | Trip status transition request DTO |
| `backend/src/main/java/com/fleetx/drivers/dto/TripResponseDto.java` | Existing and functional | Module-exclusive (M3) | Trip response DTO (includes `@JsonProperty("tripNumber")` alias for M4 compatibility) |
| `backend/src/main/java/com/fleetx/drivers/dto/TripStatusHistoryResponseDto.java` | Existing and functional | Module-exclusive (M3) | Trip lifecycle audit entry DTO |
| `backend/src/main/java/com/fleetx/drivers/service/DriverService.java` | Existing and functional | Module-exclusive (M3) | `@Primary` implementation of `DriverLookupService` + driver CRUD & licence checks |
| `backend/src/main/java/com/fleetx/drivers/service/TripService.java` | Existing and functional | Module-exclusive (M3) | `@Primary` implementation of `TripLookupService` + trip state machine & `VehicleLookupService` checks |
| `backend/src/main/java/com/fleetx/drivers/service/DriverPerformanceService.java` | Existing and functional | Module-exclusive (M3) | Computes completion rate, on-time %, and rating score |
| `backend/src/main/java/com/fleetx/drivers/service/DemoDriverTripSeeder.java` | Existing and functional | Integration-only (M3) | `@Order(30)` repeatable dev driver & trip seeder |
| `backend/src/main/java/com/fleetx/drivers/controller/DriverController.java` | Existing and functional | Module-exclusive (M3) | `/api/v1/trips/drivers/**` & `/api/v1/drivers/**` |
| `backend/src/main/java/com/fleetx/drivers/controller/TripController.java` | Existing and functional | Module-exclusive (M3) | `/api/v1/trips/**` |
| `frontend/src/features/drivers/types.ts` | Existing and functional | Module-exclusive (M3) | Driver, trip, and performance TypeScript types |
| `frontend/src/features/drivers/api/driverApi.ts` | Existing and functional | Module-exclusive (M3) | Frontend API client for drivers and trips |
| `frontend/src/features/drivers/pages/DriversPage.tsx` | Existing and functional | Module-exclusive (M3) | Driver roster, licence status badges & trip tabs |
| `frontend/src/features/drivers/pages/TripsPage.tsx` | Existing and functional | Module-exclusive (M3) | Dedicated trip dispatch & lifecycle management page |
| `frontend/src/features/drivers/components/CreateDriverModal.tsx` | Existing and functional | Module-exclusive (M3) | Driver onboarding modal |
| `frontend/src/features/drivers/components/EditDriverModal.tsx` | Existing and functional | Module-exclusive (M3) | Driver update modal |
| `frontend/src/features/drivers/components/DriverDetailsModal.tsx` | Existing and functional | Module-exclusive (M3) | Driver scorecard & trip history modal |
| `frontend/src/features/drivers/components/CreateTripModal.tsx` | Existing and functional | Module-exclusive (M3) | Trip creation & vehicle/driver assignment modal |
| `frontend/src/features/drivers/components/TripLifecycleModal.tsx` | Existing and functional | Module-exclusive (M3) | Trip start/complete/cancel & status history modal |
| `frontend/src/features/drivers/routes.tsx` | Existing and functional | Module-exclusive (M3) | Exports `driverRoutes` (`/drivers`, `/drivers/trips`) |
| `backend/src/test/java/com/fleetx/drivers/DriverOperationsTest.java` | Existing and functional | Module-exclusive (M3) | 11 driver CRUD, licence validation & PII masking tests |
| `backend/src/test/java/com/fleetx/drivers/TripLifecycleOperationsTest.java` | Existing and functional | Module-exclusive (M3) | 13 trip state machine & conflict validation tests |
| `backend/src/test/java/com/fleetx/drivers/DriverPerformanceTest.java` | Existing and functional | Module-exclusive (M3) | 3 driver performance computation unit tests |
| `frontend/src/test/DriverComponents.test.tsx` | Existing and functional | Module-exclusive (M3) | 6 frontend component tests for Drivers & Trips |

### 4.6 Module 4 — Shipment & Route Management Files (38 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `backend/src/main/resources/db/migration/V400__init_shipment_route_schema.sql` | Existing and functional | Module-exclusive (M4) | Creates `shipments`, `shipment_waypoints`, `shipment_timeline_events` |
| `backend/src/main/resources/db/migration/V401__shipment_route_enhancements.sql` | Existing and functional | Module-exclusive (M4) | Adds addresses, cargo description, promised delivery, estimated distance/duration/ETA |
| `backend/src/main/java/com/fleetx/shipments/entity/Shipment.java` | Existing and functional | Module-exclusive (M4) | JPA entity for `shipments` |
| `backend/src/main/java/com/fleetx/shipments/entity/ShipmentStatus.java` | Existing and functional | Module-exclusive (M4) | Canonical shipment statuses (`CREATED`, `ASSIGNED`, `IN_TRANSIT`, `DELIVERED`, `DELAYED`, `CANCELLED`) |
| `backend/src/main/java/com/fleetx/shipments/entity/ShipmentWaypoint.java` | Existing and functional | Module-exclusive (M4) | JPA entity for `shipment_waypoints` |
| `backend/src/main/java/com/fleetx/shipments/entity/StopType.java` | Existing and functional | Module-exclusive (M4) | Waypoint stop types (`ORIGIN`, `INTERMEDIATE`, `HUB`, `DESTINATION`) |
| `backend/src/main/java/com/fleetx/shipments/entity/ShipmentTimelineEvent.java` | Existing and functional | Module-exclusive (M4) | JPA entity for `shipment_timeline_events` |
| `backend/src/main/java/com/fleetx/shipments/repository/ShipmentRepository.java` | Existing and functional | Module-exclusive (M4) | Shipment JPA repository |
| `backend/src/main/java/com/fleetx/shipments/repository/ShipmentWaypointRepository.java` | Existing and functional | Module-exclusive (M4) | ShipmentWaypoint JPA repository |
| `backend/src/main/java/com/fleetx/shipments/repository/ShipmentTimelineEventRepository.java` | Existing and functional | Module-exclusive (M4) | ShipmentTimelineEvent JPA repository |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentCreateRequest.java` | Existing and functional | Module-exclusive (M4) | Shipment creation DTO (supports `originCity`/`destinationCity` JSON aliases) |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentUpdateRequest.java` | Existing and functional | Module-exclusive (M4) | Shipment update DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentAssignTripRequest.java` | Existing and functional | Module-exclusive (M4) | Trip association request DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentStatusUpdateRequest.java` | Existing and functional | Module-exclusive (M4) | Status transition request DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentDeliverRequest.java` | Existing and functional | Module-exclusive (M4) | Delivery confirmation DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentResponseDto.java` | Existing and functional | Module-exclusive (M4) | Shipment response DTO with compatibility getters |
| `backend/src/main/java/com/fleetx/shipments/dto/WaypointItemRequest.java` | Existing and functional | Module-exclusive (M4) | Individual route waypoint DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/WaypointsUpdateRequest.java` | Existing and functional | Module-exclusive (M4) | Ordered waypoint list request DTO |
| `backend/src/main/java/com/fleetx/shipments/dto/WaypointResponseDto.java` | Existing and functional | Module-exclusive (M4) | Waypoint response with leg distance & cumulative ETA |
| `backend/src/main/java/com/fleetx/shipments/dto/RoutePlanResponseDto.java` | Existing and functional | Module-exclusive (M4) | Haversine route summary with explicit `Estimated` labels |
| `backend/src/main/java/com/fleetx/shipments/dto/ShipmentTimelineEventDto.java` | Existing and functional | Module-exclusive (M4) | Shipment timeline event DTO |
| `backend/src/main/java/com/fleetx/shipments/service/RouteCalculationService.java` | Existing and functional | Module-exclusive (M4) | Great-circle Haversine distance & configurable speed/stop ETA calculator |
| `backend/src/main/java/com/fleetx/shipments/service/ShipmentService.java` | Existing and functional | Module-exclusive (M4) | `@Primary` implementation of `ShipmentLookupService` + shipment CRUD, trip association via `TripLookupService`, delay detection via `AlertPublisher` |
| `backend/src/main/java/com/fleetx/shipments/service/DemoShipmentSeeder.java` | Existing and functional | Integration-only (M4) | `@Order(40)` repeatable dev shipment & waypoint seeder |
| `backend/src/main/java/com/fleetx/shipments/controller/ShipmentController.java` | Existing and functional | Module-exclusive (M4) | `/api/v1/shipments/**` |
| `frontend/src/features/shipments/types.ts` | Existing and functional | Module-exclusive (M4) | Shipment, waypoint, and timeline TypeScript types |
| `frontend/src/features/shipments/api/shipmentApi.ts` | Existing and functional | Module-exclusive (M4) | Frontend API client for `/api/v1/shipments/**` |
| `frontend/src/features/shipments/pages/ShipmentsPage.tsx` | Existing and functional | Module-exclusive (M4) | Shipment manifest, status filter, and route/timeline modal triggers |
| `frontend/src/features/shipments/pages/WaypointsPage.tsx` | Existing and functional | Module-exclusive (M4) | Dedicated route & waypoint planning view |
| `frontend/src/features/shipments/components/CreateShipmentModal.tsx` | Existing and functional | Module-exclusive (M4) | Shipment creation modal |
| `frontend/src/features/shipments/components/EditShipmentModal.tsx` | Existing and functional | Module-exclusive (M4) | Shipment edit modal |
| `frontend/src/features/shipments/components/AssignTripModal.tsx` | Existing and functional | Module-exclusive (M4) | Trip-to-shipment linking modal |
| `frontend/src/features/shipments/components/RouteWaypointsModal.tsx` | Existing and functional | Module-exclusive (M4) | Interactive waypoint planner with Haversine estimate disclosure |
| `frontend/src/features/shipments/components/ShipmentDetailsModal.tsx` | Existing and functional | Module-exclusive (M4) | Shipment details, status updates, delivery confirmation & timeline |
| `frontend/src/features/shipments/routes.tsx` | Existing and functional | Module-exclusive (M4) | Exports `shipmentRoutes` (`/shipments`, `/shipments/routes`) |
| `backend/src/test/java/com/fleetx/shipments/ShipmentLifecycleAndTripAssociationTest.java` | Existing and functional | Module-exclusive (M4) | 8 shipment CRUD, lifecycle & trip association tests |
| `backend/src/test/java/com/fleetx/shipments/RouteAndDelayedShipmentTest.java` | Existing and functional | Module-exclusive (M4) | 5 Haversine distance, ETA & delayed shipment alert tests |
| `frontend/src/test/ShipmentComponents.test.tsx` | Existing and functional | Module-exclusive (M4) | 5 frontend component tests for Shipments & Waypoints |

### 4.7 Module 5 — Tracking & Control Tower Files (36 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `backend/src/main/resources/db/migration/V500__init_tracking_geofence_schema.sql` | Existing and functional | Module-exclusive (M5) | Creates `telemetry_logs`, `geofences`, `alerts`, `control_tower_kpis` |
| `backend/src/main/resources/db/migration/V501__tracking_control_tower_enhancements.sql` | Existing and functional | Module-exclusive (M5) | Adds `simulated`, `shape_type`, `polygon_coordinates_json`, `dedup_key`, `geofence_vehicle_states` |
| `backend/src/main/java/com/fleetx/tracking/entity/TrackingLog.java` | Existing and functional | Module-exclusive (M5) | JPA entity for `telemetry_logs` |
| `backend/src/main/java/com/fleetx/tracking/entity/Geofence.java` | Existing and functional | Module-exclusive (M5) | JPA entity for `geofences` |
| `backend/src/main/java/com/fleetx/tracking/entity/GeofenceVehicleState.java` | Existing and functional | Module-exclusive (M5) | JPA entity for `geofence_vehicle_states` (enter/exit transition tracking) |
| `backend/src/main/java/com/fleetx/tracking/entity/Alert.java` | Existing and functional | Module-exclusive (M5) | JPA entity for `alerts` |
| `backend/src/main/java/com/fleetx/tracking/entity/ControlTowerKpi.java` | Existing and functional | Module-exclusive (M5) | JPA entity for `control_tower_kpis` |
| `backend/src/main/java/com/fleetx/tracking/repository/TrackingRepository.java` | Existing and functional | Module-exclusive (M5) | TrackingLog JPA repository with retention cap support |
| `backend/src/main/java/com/fleetx/tracking/repository/GeofenceRepository.java` | Existing and functional | Module-exclusive (M5) | Geofence JPA repository |
| `backend/src/main/java/com/fleetx/tracking/repository/GeofenceVehicleStateRepository.java` | Existing and functional | Module-exclusive (M5) | GeofenceVehicleState JPA repository |
| `backend/src/main/java/com/fleetx/tracking/repository/AlertRepository.java` | Existing and functional | Module-exclusive (M5) | Alert JPA repository with deduplication & filtered queries |
| `backend/src/main/java/com/fleetx/tracking/repository/ControlTowerKpiRepository.java` | Existing and functional | Module-exclusive (M5) | ControlTowerKpi JPA repository |
| `backend/src/main/java/com/fleetx/tracking/repository/ControlTowerReadRepository.java` | Existing and functional | Module-exclusive (M5) | Read-only JDBC aggregation & report queries across persisted tables |
| `backend/src/main/java/com/fleetx/tracking/dto/TelemetryPingDto.java` | Existing and functional | Module-exclusive (M5) | Simulated GPS ping DTO (`dataSource: "SIMULATED_GPS_TELEMETRY"`) |
| `backend/src/main/java/com/fleetx/tracking/dto/SimulatorWaypointDto.java` | Existing and functional | Module-exclusive (M5) | Waypoint coordinate DTO for simulator interpolation |
| `backend/src/main/java/com/fleetx/tracking/dto/SimulatorStartRequest.java` | Existing and functional | Module-exclusive (M5) | Simulator start configuration DTO |
| `backend/src/main/java/com/fleetx/tracking/dto/SimulatorSessionDto.java` | Existing and functional | Module-exclusive (M5) | Active simulator session state DTO |
| `backend/src/main/java/com/fleetx/tracking/dto/GeofenceCreateRequest.java` | Existing and functional | Module-exclusive (M5) | Circular/polygonal geofence creation DTO |
| `backend/src/main/java/com/fleetx/tracking/dto/GeofenceResponseDto.java` | Existing and functional | Module-exclusive (M5) | Geofence response DTO |
| `backend/src/main/java/com/fleetx/tracking/dto/AlertResponseDto.java` | Existing and functional | Module-exclusive (M5) | Operational alert response DTO |
| `backend/src/main/java/com/fleetx/tracking/dto/ReportSummaryDto.java` | Existing and functional | Module-exclusive (M5) | Tabular report summary DTO |
| `backend/src/main/java/com/fleetx/tracking/service/GeofenceAndTelemetryMathService.java` | Existing and functional | Module-exclusive (M5) | Haversine, compass bearing, linear interpolation, point-in-circle & ray-casting polygon math |
| `backend/src/main/java/com/fleetx/tracking/service/TrackingService.java` | Existing and functional | Module-exclusive (M5) | `@Primary` `AlertPublisher`, `SimulatedGpsService` worker pool, STOMP broadcaster, geofence evaluator, RFC 4180 CSV exporter |
| `backend/src/main/java/com/fleetx/tracking/service/DemoTrackingSeeder.java` | Existing and functional | Integration-only (M5) | `@Order(50)` repeatable dev geofence & simulator seeder |
| `backend/src/main/java/com/fleetx/tracking/controller/TrackingController.java` | Existing and functional | Module-exclusive (M5) | `/api/v1/tracking/**` |
| `frontend/src/features/tracking/types.ts` | Existing and functional | Module-exclusive (M5) | Telemetry, simulator, geofence, alert, and report TypeScript types |
| `frontend/src/features/tracking/api/trackingApi.ts` | Existing and functional | Module-exclusive (M5) | Frontend API client for `/api/v1/tracking/**` |
| `frontend/src/features/tracking/components/LeafletFleetMap.tsx` | Existing and functional | Module-exclusive (M5) | Leaflet/OpenStreetMap live vehicle markers, route polylines & geofence overlays |
| `frontend/src/features/tracking/components/ThreeFleetScene.tsx` | Existing and functional | Module-exclusive (M5) | Stylized Three.js 3D fleet corridor visualization with WebGL cleanup |
| `frontend/src/features/tracking/components/CreateGeofenceModal.tsx` | Existing and functional | Module-exclusive (M5) | Circular & polygonal geofence creation modal |
| `frontend/src/features/tracking/pages/ControlTowerPage.tsx` | Existing and functional | Module-exclusive (M5) | Unified Control Tower dashboard (`SIMULATED DATA` disclosures, Map, 3D View, Geofences, Alerts, Reports) |
| `frontend/src/features/tracking/pages/ReportsPage.tsx` | Existing and functional | Module-exclusive (M5) | Standalone operational reports & CSV export view |
| `frontend/src/features/tracking/routes.tsx` | Existing and functional | Module-exclusive (M5) | Exports `trackingRoutes` (`/tracking`, `/tracking/reports`) |
| `backend/src/test/java/com/fleetx/tracking/SimulationAndGeofenceTest.java` | Existing and functional | Module-exclusive (M5) | 5 unit/integration tests for GPS interpolation, geofences & retention |
| `backend/src/test/java/com/fleetx/tracking/ControlTowerAlertAndReportTest.java` | Existing and functional | Module-exclusive (M5) | 5 integration tests for alerts, deduplication, CSV formula sanitization & RBAC |
| `frontend/src/test/TrackingComponents.test.tsx` | Existing and functional | Module-exclusive (M5) | 4 frontend tests for Control Tower UI, simulator controls & CSV export |

### 4.8 Frontend Shared Infrastructure & Cross-Module Integration Tests (29 Existing Files)

| Exact File Path | Functional Status | Ownership Scope | Notes |
| :--- | :--- | :--- | :--- |
| `frontend/index.html` | Existing and functional | Shared infrastructure | HTML root entry point |
| `frontend/package.json` | Existing and functional | Shared infrastructure | Frontend dependencies & scripts; unsafe for concurrent edits |
| `frontend/package-lock.json` | Existing and functional | Shared infrastructure | Locked npm dependency tree |
| `frontend/tsconfig.json` | Existing and functional | Shared infrastructure | TypeScript compiler configuration |
| `frontend/tsconfig.node.json` | Existing and functional | Shared infrastructure | Vite Node TypeScript configuration |
| `frontend/vite.config.ts` | Existing and functional | Shared infrastructure | Vite dev server (`5173`), `/api` & `/ws` proxy to `8080`, Vitest config |
| `frontend/src/main.tsx` | Existing and functional | Shared infrastructure | React DOM root mount |
| `frontend/src/vite-env.d.ts` | Existing and functional | Shared infrastructure | Vite client type declarations |
| `frontend/src/app/App.tsx` | Existing and functional | Shared infrastructure | `BrowserRouter` and central route renderer |
| `frontend/src/app/Layout.tsx` | Existing and functional | Shared infrastructure | Global shell wrapping `Sidebar`, `TopNav`, and `<Outlet />` |
| `frontend/src/app/routes.tsx` | Existing and functional | Shared infrastructure | Central route registry importing `authRoutes`, `fleetRoutes`, `driverRoutes`, `shipmentRoutes`, `trackingRoutes` |
| `frontend/src/shared/api/client.ts` | Existing and functional | Shared infrastructure | Axios instance with `withCredentials: true`, `X-XSRF-TOKEN` interceptor, and `/v1/` -> `/api/v1/` normalization |
| `frontend/src/shared/api/types.ts` | Existing and functional | Shared infrastructure | Frontend TypeScript definitions for `ApiResponse`, `ApiErrorResponse`, `PageResponse` |
| `frontend/src/shared/components/Badge.tsx` | Existing and functional | Shared infrastructure | Reusable status badge component |
| `frontend/src/shared/components/EmptyState.tsx` | Existing and functional | Shared infrastructure | Reusable empty state component |
| `frontend/src/shared/components/ErrorState.tsx` | Existing and functional | Shared infrastructure | Reusable error banner component |
| `frontend/src/shared/components/LoadingState.tsx` | Existing and functional | Shared infrastructure | Reusable loading spinner component |
| `frontend/src/shared/components/PageHeader.tsx` | Existing and functional | Shared infrastructure | Reusable page title/subtitle header |
| `frontend/src/shared/components/Sidebar.tsx` | Existing and functional | Shared infrastructure | Global navigation sidebar linking all 5 modules |
| `frontend/src/shared/components/StatCard.tsx` | Existing and functional | Shared infrastructure | Reusable KPI metric card |
| `frontend/src/shared/components/TopNav.tsx` | Existing and functional | Shared infrastructure | Global top bar with active role badge & Sign Out button |
| `frontend/src/shared/components/UnauthorizedState.tsx` | Existing and functional | Shared infrastructure | Reusable 403 / insufficient role state |
| `frontend/src/shared/styles/main.css` | Existing and functional | Shared infrastructure | Global CSS variables and enterprise control-tower styling |
| `frontend/src/test/setup.ts` | Existing and functional | Shared infrastructure | Vitest `@testing-library/jest-dom` setup |
| `frontend/src/test/App.test.tsx` | Existing and functional | Shared infrastructure | 3 app-shell rendering tests |
| `frontend/src/test/IntegrationNavigationAndAuth.test.tsx` | Existing and functional | Integration-only | 3 cross-module navigation, CSRF & logout tests |
| `backend/src/test/java/com/fleetx/shared/HealthControllerTest.java` | Existing and functional | Shared infrastructure | 1 health endpoint test |
| `backend/src/test/java/com/fleetx/shared/FallbackServicesTest.java` | Existing and functional | Shared infrastructure | 1 contract bean resolution test |
| `backend/src/test/java/com/fleetx/shared/SecurityAccessTest.java` | Existing and functional | Shared infrastructure | 2 security baseline tests |
| `backend/src/test/java/com/fleetx/integration/FlywayMigrationAndArchitectureTest.java` | Existing and functional | Integration-only | 3 Flyway V100–V501, `ddl-auto: validate`, and zero cross-module import tests |
| `backend/src/test/java/com/fleetx/integration/EndToEndLogisticsWorkflowIntegrationTest.java` | Existing and functional | Integration-only | 2 full 9-stage E2E logistics workflow & demo seeder idempotency tests |

### 4.9 Proposed New Files (`NEW`)
Only one optional CI automation file is genuinely recommended for GitHub team collaboration:
- `.github/workflows/ci.yml` (**NEW** — Shared infrastructure, owned by Member 1 / Team Leader): Automates `./mvnw test` and `npm test -- --run && npm run build` on pull requests targeting `main`.

---

## 5. Database Entities, Relationships & Flyway Migrations

All 10 Flyway migrations execute in strict numerical order (`V100` $\rightarrow$ `V501`) and pass Hibernate `ddl-auto: validate`:

| Migration Version | Module Owner | Tables Created / Altered | Foreign Key Relationships |
| :--- | :--- | :--- | :--- |
| `V100__init_identity_schema.sql` | M1 (Identity) | `roles`, `users`, `user_roles`, `audit_logs` | `user_roles.user_id -> users.id`, `user_roles.role_id -> roles.id`, `audit_logs.user_id -> users.id` |
| `V101__seed_roles_and_admin.sql` | M1 (Identity) | Seeds `roles` (`ADMIN`, `FLEET_MANAGER`, `DISPATCHER`, `DRIVER`, `VIEWER`) | — |
| `V200__init_fleet_schema.sql` | M2 (Fleet) | `vehicles`, `fuel_records`, `maintenance_records`, `service_warnings` | `fuel_records.vehicle_id -> vehicles.id`, `maintenance_records.vehicle_id -> vehicles.id`, `service_warnings.vehicle_id -> vehicles.id` |
| `V201__fleet_module_enhancements.sql` | M2 (Fleet) | Alters `vehicles`, `fuel_records`, `maintenance_records` | — |
| `V300__init_driver_trip_schema.sql` | M3 (Drivers) | `drivers`, `trips`, `trip_status_history`, `driver_performance_metrics` | `drivers.user_id -> users.id`, `trips.vehicle_id -> vehicles.id`, `trips.driver_id -> drivers.id`, `trip_status_history.trip_id -> trips.id`, `driver_performance_metrics.driver_id -> drivers.id` |
| `V301__driver_trip_enhancements.sql` | M3 (Drivers) | Alters `drivers` (`user_id` nullable, profile columns), `trips`, `driver_performance_metrics` | — |
| `V400__init_shipment_route_schema.sql` | M4 (Shipments) | `shipments`, `shipment_waypoints`, `shipment_timeline_events` | `shipments.trip_id -> trips.id`, `shipment_waypoints.shipment_id -> shipments.id`, `shipment_timeline_events.shipment_id -> shipments.id` |
| `V401__shipment_route_enhancements.sql` | M4 (Shipments) | Alters `shipments` (addresses, Haversine distance/duration/ETA, delivery fields) | — |
| `V500__init_tracking_geofence_schema.sql` | M5 (Tracking) | `telemetry_logs`, `geofences`, `alerts`, `control_tower_kpis` | `telemetry_logs.vehicle_id -> vehicles.id`, `telemetry_logs.trip_id -> trips.id`, `alerts.geofence_id -> geofences.id` |
| `V501__tracking_control_tower_enhancements.sql` | M5 (Tracking) | Alters `telemetry_logs`, `geofences`, `alerts`; creates `geofence_vehicle_states` | `geofence_vehicle_states.geofence_id -> geofences.id` |

---

## 6. Executed Build & Test Verification Results

Executed directly on the current workspace on **13 October 2026**:
1. **Backend Maven Test Suite (`./mvnw test` with JDK 21):**
   - **Result:** `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`
2. **Frontend Vitest Suite (`npm test -- --run`):**
   - **Result:** `Test Files: 7 passed (7) | Tests: 34 passed (34)`
3. **Frontend Production Build (`npm run build` / `tsc -b && vite build`):**
   - **Result:** `✓ 1722 modules transformed. ✓ built in 3.24s` (0 TypeScript errors).

---

## 7. Technical Risks, Architectural Observations & Unsafe Shared Files

### 7.1 Technical Risks & Observations
1. **Uninitialized Git Repository:** The workspace currently has no `.git` folder. Before students can push branches, the Team Leader must initialize Git, remove `.DS_Store` files, commit the `main` baseline, and create the five feature branches.
2. **Cross-Table Read Queries in `ControlTowerReadRepository.java` (Module 5):** While Module 5's `TrackingService` uses shared contracts (`VehicleLookupService`, `TripLookupService`, `ShipmentLookupService`, `DriverLookupService`) for business operations, `ControlTowerReadRepository` uses `JdbcTemplate` SQL queries across `vehicles`, `trips`, `drivers`, `shipments`, `fuel_records`, `maintenance_records`, and `alerts` for dashboard aggregation and CSV reporting. Consequently, any schema column rename in Modules 2–4 directly impacts `ControlTowerReadRepository`.
3. **Deprecated `@SpyBean` Warning in 4 Test Classes:** `DriverOperationsTest`, `TripLifecycleOperationsTest`, `FuelAndMaintenanceTest`, and `RouteAndDelayedShipmentTest` emit javac deprecation warnings for `org.springframework.boot.test.mock.mockito.SpyBean` in Spring Boot 3.5.x (which favours `@MockitoSpyBean`). Tests compile and pass, but migrating to `@MockitoSpyBean` is a clean future backlog item.

### 7.2 Files Unsafe to Assign Independently (Shared Baseline Files)
The following files are depended upon by all 5 modules and **must never be edited concurrently by multiple students**:
- `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-dev.yml`
- `backend/src/main/java/com/fleetx/FleetxApplication.java`
- `backend/src/main/java/com/fleetx/shared/**` (all 24 files in `config`, `contract`, `controller`, `dto`, `exception`, `fallback`)
- `frontend/package.json`, `frontend/package-lock.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`
- `frontend/src/main.tsx`, `frontend/src/app/App.tsx`, `frontend/src/app/Layout.tsx`, `frontend/src/app/routes.tsx`
- `frontend/src/shared/**` (`api/client.ts`, `api/types.ts`, `components/*`, `styles/main.css`)
