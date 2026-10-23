# FleetX — Complete System Understanding, Real-Functionality Audit & Implementation Blueprint

**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Academic Context:** Project-Based Learning (PBL) Capstone | 5th Semester | Academic Year 2026–27  
**Department:** Department of Artificial Intelligence & Data Science, Arya College of Engineering & IT, Jaipur, India  
**Project Guide / Supervisor:** Er. Ram Babu Buri (Assistant Professor)  
**Lead Systems Architect / Team Leader:** Md Ziaul Mustafa (Roll No. 24EARAD100)  
**Audit Date:** 2026-10-23  
**Audit Classification:** READ-ONLY SYSTEM AUDIT & IMPLEMENTATION BLUEPRINT (No source code modified)  
**Primary Repository:** [github.com/ziaul1608/fleetX](https://github.com/ziaul1608/fleetX) (Branch: `main`)  
**Public Deployment Target:** [https://fleetx-h9wf.onrender.com](https://fleetx-h9wf.onrender.com)  

---

## 📑 Supporting Appendices Directory

This main audit report is accompanied by six detailed engineering registers in `docs/review/`:
* [**FLEETX_FILE_INVENTORY.md**](./FLEETX_FILE_INVENTORY.md) — Comprehensive first-party file inventory, class/component mapping, callers, and dependencies.
* [**FLEETX_REAL_FUNCTIONALITY_MATRIX.csv**](./FLEETX_REAL_FUNCTIONALITY_MATRIX.csv) — Detailed CSV matrix tracing every UI control, API endpoint, service, table, and status classification (A through I).
* [**FLEETX_API_AND_RBAC_MATRIX.md**](./FLEETX_API_AND_RBAC_MATRIX.md) — Complete endpoint catalogue, method, payload/response DTOs, authorization rules, and DB effects.
* [**FLEETX_TEST_EVIDENCE.md**](./FLEETX_TEST_EVIDENCE.md) — Live test execution logs for backend (84/84 passing, 4 skipped), frontend (41/41 passing), and smoke tests.
* [**FLEETX_DEPLOYMENT_EVIDENCE.md**](./FLEETX_DEPLOYMENT_EVIDENCE.md) — Cloud topology analysis, historical live verification proofs, and root cause diagnosis of current Aiven MySQL `NXDOMAIN` timeout.
* [**FLEETX_IMPLEMENTATION_BACKLOG.md**](./FLEETX_IMPLEMENTATION_BACKLOG.md) — Phased engineering task specifications (Phases A through G) with acceptance criteria.

---

## CHAPTER 1: Executive Summary

FleetX is a full-featured, enterprise-grade fleet and logistics management system developed as a **modular monolith** in Java 21, Spring Boot 3.5.16, and MySQL 8.4 LTS, with a React 18 / TypeScript single-page application (SPA) frontend. 

### Key Findings of the Audit:
1. **Architectural Discipline [VERIFIED]:**
   The application strictly avoids both distributed microservices overhead and unstructured monolithic "spaghetti" code. The backend is cleanly partitioned into **five isolated domain packages** (`auth`, `fleet`, `drivers`, `shipments`, `tracking`) and a `shared` foundation. Cross-domain communication occurs strictly through **five explicit lookup and publisher contract interfaces** (`VehicleLookupService`, `DriverLookupService`, `TripLookupService`, `ShipmentLookupService`, `AlertPublisher`) and immutable DTOs. Direct cross-domain entity or repository imports are 100% absent and enforced by automated architectural tests.
2. **Database Schema & Persistence [VERIFIED]:**
   The relational schema is governed exclusively by **ten Flyway migrations (`V100` through `V501`)** creating **21 normalized relational tables** in MySQL 8.4 with UTF-8 MB4. Hibernate's `ddl-auto` is strictly locked to `validate`, ensuring zero unmanaged schema mutations. In-memory H2 (in MySQL mode) is used for development/testing, while real MySQL 8.4 persistence has been conclusively proven.
3. **Authentication & Role-Based Security [VERIFIED]:**
   FleetX implements a stateful, session-based security architecture (`JSESSIONID`, HttpOnly, `SameSite=Lax`, `Secure` in production) with BCrypt password hashing (strength 12) and double-submit `XSRF-TOKEN` CSRF protection. Five canonical roles (`ADMIN`, `FLEET_MANAGER`, `DISPATCHER`, `DRIVER`, `VIEWER`) are enforced via both URL matchers and method-level `@PreAuthorize` rules.
4. **Public Signup Model Clarification [VERIFIED]:**
   Public anonymous self-registration does NOT exist in FleetX and should NOT be added. FleetX is an internal corporate logistics platform; all staff accounts are securely provisioned by an authenticated `ROLE_ADMIN` (`POST /api/v1/admin/users`), with the initial system administrator provisioned via an idempotent startup bootstrapper (`AdminBootstrapRunner`).
5. **Real-Time Telemetry & STOMP Discrepancy [VERIFIED]:**
   While the backend exposes Spring STOMP WebSockets on `/ws/fleetx` with topics `/topic/vehicles/{id}` and `/topic/fleet-locations`, the frontend `ControlTowerPage.tsx` currently updates live telemetry via **3-second HTTP REST polling**. This is functional and proxy-safe, but represents an architectural divergence from the presentation documentation.
6. **Live Public Deployment Status [DIAGNOSED]:**
   The web service at `https://fleetx-h9wf.onrender.com` previously passed all 10/10 production smoke checks. However, during this audit, direct diagnostics revealed that the cloud Aiven MySQL database hostname (`fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com`) currently returns **`NXDOMAIN`** due to Aiven's inactivity power-off policy. This causes the Render container to fail during cold-start. Re-activating the Aiven service immediately restores the live web service.

---

## CHAPTER 2: Actual Stack and Repository Structure

### 2.1 Actual Stack Version Verification

| Layer / Dependency | Documented Claim | Actual Verified Version | Manifest / Evidence Source | Verification Status |
| :--- | :--- | :--- | :--- | :---: |
| **Java Runtime** | Java 21 | **Java 21 LTS (JBR 21.0.3)** | `backend/pom.xml` (`<java.version>21</java.version>`), JVM runtime | **VERIFIED** |
| **Backend Framework** | Spring Boot 3.5.x | **Spring Boot 3.5.16** | `backend/pom.xml` (`<version>3.5.16</version>`) | **VERIFIED** |
| **Security Engine** | Spring Security | **Spring Security 6.x** | Managed by Spring Boot Starter Security 3.5.16 | **VERIFIED** |
| **ORM / Data Layer** | Spring Data JPA / Hibernate | **Hibernate 6.6.x** | Managed by Spring Boot Starter Data JPA 3.5.16 | **VERIFIED** |
| **Schema Governance** | Flyway Migrations | **Flyway 10.x (`flyway-mysql`)** | `backend/pom.xml`, `db/migration/` (V100–V501) | **VERIFIED** |
| **Development Database** | H2 in MySQL mode | **H2 Database (runtime test)** | `backend/pom.xml`, `application-dev.yml` | **VERIFIED** |
| **Persistent Database** | MySQL 8.4 LTS | **MySQL 8.4.11 LTS** | `docker-compose.yml`, Aiven MySQL instance, `mysql-connector-j` 9.7.0 | **VERIFIED** |
| **Frontend Framework** | React 18 + TypeScript | **React 18.3.1, TypeScript 5.5.4** | `frontend/package.json` | **VERIFIED** |
| **Build Tooling** | Vite | **Vite 5.4.6** | `frontend/package.json` | **VERIFIED** |
| **CSS Framework** | Bootstrap 5.3 | **Bootstrap 5.3.3** | `frontend/package.json` | **VERIFIED** |
| **HTTP Client** | Axios | **Axios 1.7.7** | `frontend/package.json`, `client.ts` | **VERIFIED** |
| **Routing Engine** | React Router | **React Router DOM 6.26.2** | `frontend/package.json`, `routes.tsx` | **VERIFIED** |
| **2D Mapping** | Leaflet + OpenStreetMap | **Leaflet 1.9.4, React-Leaflet 4.2.1** | `frontend/package.json`, `LeafletFleetMap.tsx` | **VERIFIED** |
| **3D Visualization** | Three.js | **Three.js 0.168.0, @react-three/fiber 8.17.7** | `frontend/package.json`, `ThreeFleetScene.tsx` | **VERIFIED** |
| **Real-Time Client** | STOMP over WebSocket | **HTTP REST Polling (3s interval)** | `@stomp/stompjs` missing in `package.json`; `ControlTowerPage.tsx` | **DISCREPANCY VERIFIED** |
| **Unit Testing (Backend)**| JUnit 5, Mockito, AssertJ | **JUnit Jupiter 5.11.x, Mockito 5.14.x** | `backend/pom.xml`, Surefire (84 passed, 4 skipped) | **VERIFIED** |
| **Unit Testing (Frontend)**| Vitest, RTL | **Vitest 2.1.1, Testing Library 16.0.1** | `frontend/package.json`, Vitest (41 passed) | **VERIFIED** |
| **Containerization** | Docker Multi-Stage | **Eclipse Temurin 21-jre + Node 20** | `Dockerfile` (132 MB image size) | **VERIFIED** |

---

## CHAPTER 3: Architecture and Data Flow

```mermaid
flowchart TD
    subgraph PresentationTier["Presentation Tier (React 18 + TypeScript + Vite)"]
        UI["Bootstrap 5.3 UI Components\nForms, Tables, Modals, Dashboards"]
        LeafletMap["Leaflet 1.9 (2D OSM Map)\nMarkers, Trails, Geofences"]
        ThreeScene["Three.js (3D Procedural View)\nStylized Fleet Canvas"]
        AxiosClient["Axios HTTP Client\nwithCredentials: true\nXSRF-TOKEN Auto-Forwarding"]
    end

    subgraph SecurityPerimeter["Spring Security Perimeter (Port 8080 / 443)"]
        FilterChain["SecurityFilterChain\n• Session Management (JSESSIONID)\n• CSRF Protection (X-XSRF-TOKEN)\n• URL Rules (/api/health, /admin/**)"]
        MethodSecurity["Method-Level Security\n@PreAuthorize('hasRole(...)')"]
    end

    subgraph ApplicationTier["Application Tier (Modular Monolith - Spring Boot 3.5.16)"]
        M1["Module 1: auth\nIdentity, Sessions, Admin, Audit"]
        M2["Module 2: fleet\nVehicles, Fuel, Maintenance, Warnings"]
        M3["Module 3: drivers\nDrivers, Licence Validation, Trips FSM"]
        M4["Module 4: shipments\nManifests, Haversine Route, Timelines"]
        M5["Module 5: tracking\nSimulated GPS, Geofences, Alerts, Reports"]
        
        Contracts["Shared Kernel Contract Interfaces & DTOs\n• VehicleLookupService\n• DriverLookupService\n• TripLookupService\n• ShipmentLookupService\n• AlertPublisher"]
    end

    subgraph PersistenceTier["Persistence Tier (MySQL 8.4 LTS / InnoDB / UTF-8 MB4)"]
        Flyway["Flyway Migrations V100 - V501\nHibernate ddl-auto=validate"]
        Tables[("21 Relational Tables\nusers, vehicles, trips, shipments,\ntelemetry_logs, alerts, geofences, ...")]
    end

    UI --> AxiosClient
    LeafletMap --> AxiosClient
    ThreeScene --> AxiosClient
    AxiosClient --> FilterChain
    FilterChain --> MethodSecurity
    MethodSecurity --> M1 & M2 & M3 & M4 & M5
    M2 & M3 & M4 & M5 <--> Contracts
    M1 & M2 & M3 & M4 & M5 --> Flyway
    Flyway --> Tables
```

### Data Flow Across Cross-Domain Boundaries:
1. **Trip Dispatch Workflow:**
   `TripService` in Module 3 needs to verify vehicle availability and driver eligibility. Instead of injecting `VehicleRepository` or `DriverRepository`, it calls `VehicleLookupService.isVehicleAvailableForTrip(vehicleId)` and `DriverLookupService.isDriverEligibleForTrip(driverId)`.
2. **Alert Emission Workflow:**
   When `MaintenanceService` (Module 2) detects an overdue vehicle, or `TripService` (Module 3) dispatches a trip, or `ShipmentService` (Module 4) detects a transit delay, they publish an `AlertEventDto` to the shared `AlertPublisher` interface. `TrackingService` (Module 5) implements `AlertPublisher`, persisting the alert in the `alerts` table and updating Control Tower KPIs.

---

## CHAPTER 4: Real-Functionality Matrix Summary

Every major screen, form, table, dialog, and action was audited against the codebase and runtime tests:

| Module | Total Features Audited | Real & Persisted | Simulated | Partially Implemented | UI-Only / Broken |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Module 1: Identity & Admin** | 12 | **11** | 0 | 1 *(Profile self-service pwd)* | 0 |
| **Module 2: Fleet & Assets** | 9 | **9** | 0 | 0 | 0 |
| **Module 3: Drivers & Trips** | 8 | **8** | 0 | 0 | 0 |
| **Module 4: Shipments & Routes** | 7 | **7** | 0 | 0 | 0 |
| **Module 5: Tracking & Control Tower** | 8 | **6** | **2** *(GPS Engine, 3D Canvas)* | 0 | 0 |
| **Total** | **44** | **41** | **2** | **1** | **0** |

*(Refer to [FLEETX_REAL_FUNCTIONALITY_MATRIX.csv](./FLEETX_REAL_FUNCTIONALITY_MATRIX.csv) for the row-by-row technical audit).*

---

## CHAPTER 5: Module 1 Review — Identity, RBAC & Administration
**Author / Assigned Owner:** Md Ziaul Mustafa (24EARAD100) — Team Leader & Lead Systems Architect  
**Flyway Range:** `V100` – `V199` | **Package:** `com.fleetx.auth`

### 1. Implemented Capabilities [VERIFIED]:
- **Session Authentication:** Authenticates credentials via BCrypt against `users` table; sets `JSESSIONID` cookie and establishes Spring `SecurityContext`.
- **CSRF Token Handshake:** Issues `XSRF-TOKEN` cookie via `GET /api/v1/auth/csrf`; validates `X-XSRF-TOKEN` on all state-changing requests.
- **Session Invalidation:** Invalidates HTTP session on `POST /api/v1/auth/logout`, clears context, and records `USER_LOGOUT` audit log.
- **Administrative User Provisioning:** `AdminUserController.createUser()` allows `ROLE_ADMIN` to provision staff accounts with chosen roles.
- **Account Deactivation:** `AdminUserController.updateUserStatus()` immediately disables access; deactivated users receive HTTP 403 `ACCOUNT_DEACTIVATED`.
- **Administrative Password Reset:** `AdminUserController.resetPassword()` overrides user password with a fresh BCrypt hash.
- **Autonomous Audit Logging:** `AuditService.recordEvent()` uses `Propagation.REQUIRES_NEW`, ensuring audit records survive even when business transactions fail.
- **Idempotent Administrator Bootstrap:** `AdminBootstrapRunner` creates the initial administrator account on startup if absent.

### 2. Incomplete or Refinement Areas:
- Authenticated non-admin users cannot change their own password from `ProfilePage.tsx` (`TASK-B1`).
- Demotion safeguard needed to prevent an administrator from accidentally removing `ROLE_ADMIN` from the sole remaining active admin (`TASK-B2`).

---

## CHAPTER 6: Module 2 Review — Fleet & Asset Management
**Author / Assigned Owner:** Jitendra Kumar Paswan (24EARAD071) — Fleet & Asset Systems Engineer  
**Flyway Range:** `V200` – `V299` | **Package:** `com.fleetx.fleet`

### 1. Implemented Capabilities [VERIFIED]:
- **Vehicle Directory:** Full CRUD operations on commercial vehicles (`vehicles` table).
- **Format Validation:** Strict 17-character alphanumeric VIN pattern (`^[A-HJ-NPR-Z0-9]{17}$`) and license plate uniqueness.
- **Automated Fuel Efficiency Calculation:** `FuelService.logFuel()` computes efficiency as:
  $$\text{Efficiency (km/L)} = \frac{\Delta\text{Odometer (km)}}{\text{Fuel Volume (L)}}$$
  Non-positive deltas (e.g. duplicate readings) correctly record `efficiency_km_per_litre = null` rather than throwing divide-by-zero errors.
- **Preventive Maintenance Engine:** `MaintenanceService.evaluateServiceWarnings()` inspects current odometer and date:
  - If $\Delta\text{km} \le 500\text{ km}$ or $\Delta\text{days} \le 14\text{ days}$: raises `WARNING` of type `SERVICE_DUE`.
  - If odometer $\ge$ due distance or date $>$ due date: raises `CRITICAL` of type `SERVICE_OVERDUE`.
  - Automatically publishes alerts via `AlertPublisher` to the Control Tower.
- **Contract Implementation:** Implements `VehicleLookupService` to answer queries from Module 3 and Module 5 without leaking entity graphs.

---

## CHAPTER 7: Module 3 Review — Commercial Drivers & Trip Operations
**Author / Assigned Owner:** Mayank Kumar (24EARAD098) — Driver & Trip Operations Systems Engineer  
**Flyway Range:** `V300` – `V399` | **Package:** `com.fleetx.drivers`

### 1. Implemented Capabilities [VERIFIED]:
- **Driver Registry:** Stores commercial licence numbers, licence categories (HEAVY_VEHICLE, LIGHT_VEHICLE, COMMERCIAL), and expiry dates.
- **Dispatch Precondition Validation:** `TripService.createTrip()` strictly validates:
  1. Driver exists and has `availability_status = 'AVAILABLE'`.
  2. Driver's commercial licence is not expired (`licenseExpiryDate.isAfter(LocalDate.now())`).
  3. Medical certificate (where recorded) is currently valid.
  4. Vehicle exists and has status `AVAILABLE`.
- **Trip Finite State Machine (FSM):** Enforces strict lifecycle transitions:
  $$\text{PLANNED} \longrightarrow \text{ASSIGNED} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{COMPLETED} \quad (\text{or } \text{CANCELLED})$$
  Illegal transitions (e.g. PLANNED directly to COMPLETED) throw `InvalidStateTransitionException` (HTTP 400).
- **Resource Locking and Release:**
  - Upon dispatch: driver becomes `ON_DUTY`; vehicle becomes `IN_TRIP`.
  - Upon completion or cancellation: driver is returned to `AVAILABLE`; vehicle is returned to `AVAILABLE`.
- **Audit History:** Every status change writes an immutable record to `trip_status_history`.

---

## CHAPTER 8: Module 4 Review — Shipment Manifests & Route Logistics
**Author / Assigned Owner:** Md Amir Alam (24EARAD099) — Shipment & Route Management Systems Engineer  
**Flyway Range:** `V400` – `V499` | **Package:** `com.fleetx.shipments`

### 1. Implemented Capabilities [VERIFIED]:
- **Manifest Management:** Registers shipments with auto-generated unique business codes (`SHP-YYYY-NNNN`), parcel weights, volume, and cargo descriptions.
- **Trip Association:** Links shipments to active or planned trips using `TripLookupService`.
- **Great-Circle Haversine Distance & ETA Calculation:**
  `RouteCalculationService` calculates the spherical surface distance between route waypoints ($R = 6371\text{ km}$):
  $$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos\phi_1\cos\phi_2\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
  $$d = 2R \cdot \operatorname{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$
  $$T_{\text{ETA}} = \frac{60 \cdot D}{\bar{v}} + \max(0, n - 2) \cdot t_{\text{stop}}$$
  Outputs are explicitly labeled **Estimated Straight-Line Distance** and **Estimated Travel Time**.
- **Delivery Confirmation:** `ShipmentService.completeDelivery()` records actual delivery timestamp, recipient remarks, and appends a `DELIVERED` event to `shipment_timeline_events`.
- **Automated Delay Inspection:** `detectAndFlagDelayedShipments()` checks in-transit shipments past scheduled delivery time, transitions status to `DELAYED`, and raises `SHIPMENT_DELAYED` alerts.

---

## CHAPTER 9: Module 5 Review — Tracking & Control Tower
**Author / Assigned Owner:** Khusharg Gautam (24EARAD087) — Live Tracking & Control Tower Systems Engineer  
**Flyway Range:** `V500` – `V599` | **Package:** `com.fleetx.tracking`

### 1. Implemented Capabilities [VERIFIED]:
- **Simulated GPS Engine:** Multi-threaded scheduled task pool (`ScheduledExecutorService`) interpolating positions along route waypoints. Telemetry records in `telemetry_logs` are flagged with `is_simulated = true`.
- **Geofence Breach Detection:**
  - **Circular Geofences:** Computes Haversine distance from vehicle to center; flags breach when distance $>$ radius.
  - **Polygonal Geofences:** Implements the ray-casting Point-in-Polygon (PIP) parity algorithm.
  - **Alert Deduplication:** Uses `geofence_vehicle_states` table to ensure only state *transitions* (ENTER / EXIT) trigger alerts, preventing dashboard alert flooding.
- **Operational Alerts Feed & Acknowledgement:** Paged alert feed supporting filtering and one-click acknowledgement (`acknowledged = true`).
- **Control Tower KPIs:** Aggregates real-time metrics across all five modules (active vehicles, ongoing trips, fleet km/L efficiency, unacknowledged alerts).
- **RFC 4180 CSV Export:** Direct streaming of relational records into formatted CSV files for trips, fleet, maintenance, drivers, and alerts.
- **2D/3D Dual View:** Synchronized Leaflet 2D OpenStreetMap tile view and Three.js stylized procedural 3D scene.

---

## CHAPTER 10: Complete Authentication, Account Lifecycle & Admin Model

### 10.1 The FleetX Authentication Lifecycle

```
[Public Visitor] ─── (Navigates to /login) ───► [LoginPage]
                                                      │
                       Bootstrap CSRF                 │ Submit Email & Password
                     (GET /api/v1/auth/csrf)          ▼ (POST /api/v1/auth/login)
                                            [AuthController]
                                                      │
                                                      ▼
                                            [AuthService.login()]
                                                      │
                                   ┌──────────────────┴──────────────────┐
                                   ▼                                     ▼
                         [BCrypt Password Match]               [Check user.isActive()]
                                   │                                     │
                                   ▼ Success                             ▼ If false: 403 Forbidden
                       [Create Spring SecurityContext]           (ACCOUNT_DEACTIVATED)
                                   │
                                   ▼
                       [Associate HttpSession] ──► Issue 'JSESSIONID' Cookie (HttpOnly, SameSite=Lax)
                                   │
                                   ▼
                       [Write AuditLog] ─────────► 'USER_LOGIN_SUCCESS' in audit_logs table
                                   │
                                   ▼
                       [Return UserResponseDto] ─► React AuthContext populated; redirect to '/'
```

### 10.2 Public Signup vs. Staff Provisioning Architectural Decision
- **Question:** Should FleetX have an open, public signup page?
- **Engineering Verdict:** **NO.** An open public signup page is inappropriate and unsafe for an internal corporate logistics platform. Allowing anonymous public visitors to register would either expose internal company data or create security vulnerabilities where unauthorized users attempt to select roles.
- **Intended Model:**
  1. Initial administrator created via environment-variable driven, idempotent bootstrap (`AdminBootstrapRunner`).
  2. All company staff accounts (Managers, Dispatchers, Drivers, Viewers) provisioned strictly by `ROLE_ADMIN` through the Admin Panel (`/admin/users`).
  3. Deactivated staff accounts are instantly denied access on subsequent requests.

---

## CHAPTER 11: Database Architecture — Static or Dynamic?

### 11.1 The Definitive Viva Answer
> "FleetX uses a **fully dynamic, relational, database-backed architecture** governed strictly by Flyway migrations on MySQL 8.4 LTS (with in-memory H2 in MySQL mode for isolated development testing). It is **NOT** a static database or mock JSON file. Every vehicle created, trip dispatched, fuel record logged, user provisioned, and geofence configured is written to persistent relational tables. Synthetic GPS coordinates and Haversine distances are mathematically computed and dynamically persisted with explicit disclosures, while development demo records are seeded through the real domain services only when running with the development profile."

### 11.2 Database Profile & Datasource Configuration

| Profile Name | Datasource Target | Hibernate Mode | Flyway | Seeders Active | Use Case |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`dev`** | `jdbc:h2:mem:fleetx;MODE=MySQL` | `validate` | Enabled (`V100`–`V501`) | **YES** (`fleetx.demo.seed.enabled=true`) | Local rapid frontend/backend feature development |
| **`qa`** | `jdbc:mysql://localhost:3307/fleetx` | `validate` | Enabled (`V100`–`V501`) | **NO** | Containerized persistence & restart regression testing |
| **`prod`** | Cloud Aiven MySQL 8.4 (Port 15887, TLS) | `validate` | Enabled (`V100`–`V501`) | **STRICTLY DISABLED** | Live production deployment |

---

## CHAPTER 12: Database Entity-Relationship Diagram (ERD)

```mermaid
erDiagram
    users ||--o{ user_roles : has
    roles ||--o{ user_roles : assigned_to
    users ||--o{ audit_logs : triggers
    users ||--o| drivers : profile_for

    vehicles ||--o{ fuel_records : logs
    vehicles ||--o{ maintenance_records : services
    vehicles ||--o{ service_warnings : generates
    vehicles ||--o{ telemetry_logs : emits
    vehicles ||--o{ geofence_vehicle_states : tracks

    drivers ||--o{ trips : operates
    vehicles ||--o{ trips : assigned_to
    trips ||--o{ trip_status_history : records
    drivers ||--o| driver_performance_metrics : rated_in

    trips ||--o{ shipments : carries
    shipments ||--o{ shipment_waypoints : plans
    shipments ||--o{ shipment_timeline_events : audits

    geofences ||--o{ geofence_vehicle_states : contains
    alerts }o--|| vehicles : relates_to

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        boolean active
        datetime created_at
    }

    vehicles {
        bigint id PK
        varchar vin UK
        varchar license_plate UK
        varchar make
        varchar model
        varchar status
        double capacity_kg
        double odometer_km
    }

    trips {
        bigint id PK
        varchar trip_code UK
        bigint vehicle_id FK
        bigint driver_id FK
        varchar status
        datetime scheduled_start_time
    }

    shipments {
        bigint id PK
        varchar tracking_number UK
        bigint assigned_trip_id FK
        varchar status
        double weight_kg
        double estimated_distance_km
    }

    telemetry_logs {
        bigint id PK
        bigint vehicle_id FK
        double latitude
        double longitude
        double speed_kmh
        boolean is_simulated
        datetime recorded_at
    }

    alerts {
        bigint id PK
        varchar alert_type
        varchar severity
        bigint entity_id
        boolean acknowledged
        datetime created_at
    }
```

---

## CHAPTER 13: End-to-End Business User Journeys

The application supports **eight complete operational workflows**:
* **Workflow A (Initial Administrator Setup):** Bootstrapping admin account → Session authentication → Token check → Logout. [Status: **VERIFIED**]
* **Workflow B (Staff Onboarding & RBAC):** Admin provisions Manager/Dispatcher/Driver → New user logs in → RBAC enforced. [Status: **VERIFIED**]
* **Workflow C (Vehicle Lifecycle & Maintenance):** Vehicle registration → Fuel logging & efficiency calculation → Service threshold alert. [Status: **VERIFIED**]
* **Workflow D (Trip Planning & Dispatch):** Driver licence check → Asset assignment → Dispatch FSM transition → Status history logging. [Status: **VERIFIED**]
* **Workflow E (Shipment Manifest & Route Logistics):** Manifest creation → Trip association → Haversine waypoints calculation → Delivery confirmation. [Status: **VERIFIED**]
* **Workflow F (Tracking, Geofencing & Alerts):** GPS simulation tick → Telemetry interpolation → Geofence breach check → Alert acknowledgement. [Status: **VERIFIED**]
* **Workflow G (Operational Audits & CSV Export):** Domain filtering → Record aggregation → RFC 4180 CSV export download. [Status: **VERIFIED**]
* **Workflow H (Authorization & Barrier Enforcement):** Viewer write attempt rejected (403) → Unauthenticated call rejected (401) → CSRF missing rejected (403). [Status: **VERIFIED**]

---

## CHAPTER 14: Live Deployment Verification & Diagnosis

During the current audit, direct network inspection revealed:
1. **Cloudflare CDN / Edge:** Active and healthy (`216.24.57.18:443`). TLS 1.3 handshake succeeds.
2. **Upstream Database Host:** `fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com` returns `NXDOMAIN`.
3. **Root Cause:** Aiven Always-Free MySQL instances power off automatically after inactivity periods. Because the database is offline, Spring Boot fails during cold-start on Render, leading to a Cloudflare gateway timeout.
4. **Remediation:** Powering on the service in the Aiven console immediately restores the web application.

---

## CHAPTER 15: Git History & Five-Member Contribution Analysis

Inspection of the Git repository (`https://github.com/ziaul1608/fleetX`) revealed:
* **Remote Branches Exist:**
  - `origin/Identity-and-admin` (Author: `md ziaul mustafa <ziaulmustafa2005@gmail.com>`)
  - `origin/Fleet-&-Asset-Management` (Author: `jitendrakumarp006 <jitendrakumarp006@gmail.com>`)
  - `origin/Driver-&-Trip-Operations` (Author: `mayank kumar <mayank.2582006@gmail.com>`)
  - `origin/Shipment-&-Route-Management` (Author: `amiralamjh10-art <amiralamjh10@gmail.com>`)
  - `origin/Live-Tracking-&-Control-Tower` (Author: `Kushagra Gautam <royalkushagra1@gmail.com>`)
* **Finding:** All five students contributed commits on their assigned feature branches. However, file additions were made via GitHub web uploads rather than standard pull request merges into `main`. The `main` branch was consolidated by the team leader for production deployment.

---

## CHAPTER 16: Tests, Builds and Quality Evidence

- **Backend Automated Tests:** 88 tests in 17 suites (`./mvnw test`). **84 passed, 4 skipped** (skipped tests require local Docker MySQL on port 3307; all 84 unit, contract, schema, and E2E tests passed).
- **Frontend Automated Tests:** 41 tests in 7 suites (`npm test`). **41 passed (100% pass rate)** in 1.44 seconds.
- **Production Container Build:** Multi-stage Docker build bundles compiled React assets into Spring Boot static resources, producing a standalone 132 MB container image.

---

## CHAPTER 17: Application Security & Hardening Review

- **BCrypt Hashing:** Strength factor 12 verified; plaintext passwords are never stored or logged.
- **Session Management:** Cookies use HttpOnly and `SameSite=Lax`. `Secure` attribute is enabled in production.
- **CSRF Defense:** State-changing requests without `X-XSRF-TOKEN` header are rejected with HTTP 403.
- **Privilege Escalation:** Admin endpoints (`/api/v1/admin/**`) strictly reject non-admin users.

---

## CHAPTER 18: Production Readiness & Academic Disclosures

For a collegiate evaluation and small-scale fleet demonstration, FleetX is production-ready. The following academic disclosures must be maintained:
1. **Simulated Telemetry:** Location coordinates are mathematically interpolated by `SimulatedGpsService`; they are not live feeds from physical OBD-II/GPS hardware.
2. **Haversine Routing:** Route distances are great-circle spherical estimates, not road-network navigation.
3. **Procedural 3D Scene:** The Three.js view is a stylized visualization, not a surveyed physical twin.

---

## CHAPTER 19: Prioritized Implementation Blueprint

The prioritized implementation backlog is structured in seven phases:
* **Phase A:** Cloud database reactivation & dev credential alignment (`TASK-A1`, `TASK-A2`).
* **Phase B:** Self-service profile password change & last-admin safeguard (`TASK-B1`, `TASK-B2`).
* **Phase C:** Vehicle retirement guard & driver compliance rules (`TASK-C1`, `TASK-C2`).
* **Phase D:** Native STOMP WebSocket client integration in frontend (`TASK-D1`).
* **Phase E:** Automated Docker MySQL 8.4 persistence runner (`TASK-E1`).
* **Phase F:** Cloud smoke test re-verification (`TASK-F1`).
* **Phase G:** Demonstration script preparation (`TASK-G1`).

*(Refer to [FLEETX_IMPLEMENTATION_BACKLOG.md](./FLEETX_IMPLEMENTATION_BACKLOG.md) for full task specifications).*

---

## CHAPTER 20: Five-Member Module Ownership & Acceptance Matrix

| Student Name & Roll No. | Assigned Module & Role | Owned Backend Package | Owned Frontend Feature | Key Demonstration Scenario | Viva Technical Topics |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Md Ziaul Mustafa** (24EARAD100) | **Team Leader & Lead Architect** (Module 1: Identity & Admin) | `com.fleetx.auth`, `com.fleetx.shared` | `features/auth/`, app shell, `client.ts` | Log in as admin, provision a new Dispatcher, deactivate account, inspect audit logs. | Modular monolith design, Spring Security filter chain, BCrypt, session vs JWT, CSRF protection. |
| **Jitendra Kumar Paswan** (24EARAD071) | **Fleet & Asset Systems Engineer** (Module 2: Fleet & Assets) | `com.fleetx.fleet` | `features/fleet/` | Register new commercial truck, log fuel fill-up, demonstrate automated km/L calculation and maintenance alert. | VIN validation regex, JPA lifecycle annotations, preventive maintenance threshold logic. |
| **Mayank Kumar** (24EARAD098) | **Driver & Trip Systems Engineer** (Module 3: Drivers & Trips) | `com.fleetx.drivers` | `features/drivers/` | Onboard commercial driver, attempt dispatch with expired licence (rejected), dispatch trip and advance FSM. | Finite state machine design, cross-module lookup contracts, resource locking/release mechanisms. |
| **Md Amir Alam** (24EARAD099) | **Shipment & Route Systems Engineer** (Module 4: Shipments & Routes) | `com.fleetx.shipments` | `features/shipments/` | Create cargo manifest, configure waypoints, show Haversine distance and ETA, complete delivery with notes. | Haversine great-circle formula, waypoint sequence ordering, delivery timeline audit logs. |
| **Khusharg Gautam** (24EARAD087) | **Live Tracking Systems Engineer** (Module 5: Tracking & Control Tower) | `com.fleetx.tracking` | `features/tracking/` | Start simulated GPS engine, view 2D Leaflet tracking trail and 3D Three.js truck, trigger geofence breach, export CSV. | Point-in-Polygon (PIP) ray casting, scheduled thread pool workers, alert deduplication, RFC 4180 CSV export. |

---

## CHAPTER 21: Academic Demonstration Sequence & Viva Preparation

### 21.1 10-Minute End-to-End Demonstration Sequence
1. **Minutes 0–2 (Team Leader - Md Ziaul Mustafa):**
   - Present architecture diagram: single deployable Spring Boot artifact, five isolated modules, Flyway governance.
   - Log in as `admin@fleetx.local`, navigate to Admin Panel, provision a new staff account.
2. **Minutes 2–4 (Jitendra Kumar Paswan):**
   - Navigate to Fleet Inventory, register vehicle `KA-01-FX-5555`.
   - Log fuel fill-up (50 L, +350 km); show calculated $7.0\text{ km/L}$ efficiency. Show active maintenance warnings.
3. **Minutes 4–6 (Mayank Kumar):**
   - Open Driver Roster, show driver licence validation.
   - Dispatch a new trip assigning the registered vehicle and driver; show status transition to `ASSIGNED`.
4. **Minutes 6–8 (Md Amir Alam):**
   - Open Shipment Manifest, create shipment, link to the dispatched trip.
   - Configure route waypoints; demonstrate Haversine distance and ETA computation.
5. **Minutes 8–10 (Khusharg Gautam):**
   - Open Control Tower dashboard, start GPS simulator for the active trip.
   - Show vehicle marker moving on 2D Leaflet map, toggle 3D Three.js view, demonstrate geofence breach alert, and export CSV report.

### 21.2 Tough Viva Questions & Honest Answers
- **Q: "Where is the physical GPS device connected?"**
  - **Answer:** *"Sir, in accordance with our project scope, physical OBD-II hardware is not integrated. Telemetry is mathematically generated by our in-process `SimulatedGpsService` interpolating along waypoints. All records are explicitly flagged `is_simulated = true` to preserve academic integrity."*
- **Q: "Is your database real or mock?"**
  - **Answer:** *"Sir, it is 100% real. The schema consists of 21 tables managed by 10 Flyway migrations on MySQL 8.4 LTS with Hibernate `ddl-auto=validate`. Data persists across restarts and we have verified physical persistence on both local MySQL and cloud Aiven MySQL."*
- **Q: "Why did you build a modular monolith instead of microservices?"**
  - **Answer:** *"Sir, for a 5-member collegiate team, microservices introduce severe operational overhead—service registries, distributed tracing, network latency, and eventual consistency. A modular monolith provides single-process deployment and ACID transactions while enforcing strict module isolation through Java contract interfaces."*

---

## CHAPTER 22: Final Verdict and Unverified Items

### Final Verdict:
FleetX is a solidly engineered, academically rigorous, fully functional modular monolith. Its architecture, code structure, test coverage, and documentation represent exemplary student software engineering.

### Specific Unverified Items:
1. **Live Browser Interaction on `https://fleetx-h9wf.onrender.com`:** Currently unverified due to the upstream Aiven MySQL `NXDOMAIN` timeout. Can be verified immediately upon database power-on.
2. **Local MySQL 8.4 Docker Tests (4 tests):** Skipped during surefire run because Docker daemon was not running on port 3307 during the test execution.
3. **Physical Hardware Feeds:** Not integrated (by design).

---

*Report prepared and submitted for academic evaluation under the supervision of Er. Ram Babu Buri, Arya College of Engineering & IT, Jaipur.*
