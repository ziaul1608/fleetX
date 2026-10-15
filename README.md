# FleetX — Intelligent Fleet & Logistics Management System
website link :- https://fleetx-h9wf.onrender.com/
**Academic Year:** 2026–27 | **Project Category:** Java PBL Capstone (5-Member Team)  
**Architecture:** Modular Monolith | **Backend:** Spring Boot 3.5.9 (Java 21) | **Frontend:** React 18 + TypeScript (Vite + Bootstrap 5)  
**Database:** MySQL 8.4 LTS (Flyway V100–V501) | **Phase:** Phase 7 — Full Integrated System, E2E Verified & Stabilized

---

## 1. Monorepo Repository Structure

```
fleetx/
├── backend/                   # Spring Boot 3.5.9 Modular Monolith application
│   ├── pom.xml                # Maven build configuration (Java 21)
│   ├── mvnw / mvnw.cmd        # Maven wrapper scripts
│   └── src/
│       ├── main/java/com/fleetx/
│       │   ├── auth/          # Module 1: Identity, RBAC, Sessions, CSRF & User Management
│       │   ├── fleet/         # Module 2: Fleet Vehicles, Fuel Efficiency & Maintenance
│       │   ├── drivers/       # Module 3: Driver Onboarding, Trip Lifecycle & Performance
│       │   ├── shipments/     # Module 4: Shipments, Haversine Route Waypoints & Tracking Events
│       │   ├── tracking/      # Module 5: Simulated GPS Engine, STOMP WebSockets, Geofences, Alerts & Control Tower
│       │   └── shared/        # Shared DTOs, contracts, fallback beans, security, error envelope
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── db/migration/  # Flyway migration scripts (V100–V501)
│       └── test/java/com/fleetx/
│           ├── auth/          # Module 1 integration & security tests
│           ├── fleet/         # Module 2 unit & integration tests
│           ├── drivers/       # Module 3 unit & integration tests
│           ├── shipments/     # Module 4 unit & integration tests
│           ├── tracking/      # Module 5 unit & integration tests
│           └── integration/   # Phase 7 full-stack E2E workflow & Flyway schema tests
│
├── frontend/                  # React 18 + Vite + TypeScript + Leaflet + Three.js application
│   ├── package.json           # Frontend dependencies and scripts
│   ├── vite.config.ts         # Vite configuration with /api and /ws proxy
│   └── src/
│       ├── app/               # Central route registry and global layout
│       ├── features/          # Independently owned feature folders
│       │   ├── auth/          # Module 1: Login, CSRF bootstrap & Admin User Directory
│       │   ├── fleet/         # Module 2: Vehicle Directory, Fuel Logs & Maintenance
│       │   ├── drivers/       # Module 3: Driver Directory, Trip Dispatch & Performance
│       │   ├── shipments/     # Module 4: Shipment Manifest, Haversine Route Planner & Timeline
│       │   └── tracking/      # Module 5: Live Leaflet Map, 3D Fleet View, Geofences, Alerts & CSV Reports
│       ├── shared/            # Common UI components, Axios CSRF client, layouts, styles
│       └── test/              # Vitest + React Testing Library suites (7 suites, 34 tests)
│
├── docs/                      # Architecture, requirements & verification documentation
│   ├── PROJECT_CHARTER.md
│   ├── SRS.md
│   ├── ARCHITECTURE.md
│   ├── API_CONTRACTS.md
│   ├── DATABASE_DESIGN.md
│   ├── TEAM_OWNERSHIP.md
│   ├── GITHUB_WORKFLOW.md
│   ├── TEST_PLAN.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── DAILY_WEEKLY_REPORT_TEMPLATES.md
│   └── INTEGRATION_TEST_REPORT.md
│
├── docker-compose.yml         # Containerized MySQL 8.4 LTS service
├── .env.example               # Environment variables template
├── .gitignore                 # Version control exclusions
├── FLEETX_AGENT_RULES.md      # Governing rules for AI and developers
└── README.md                  # This file
```

---

## 2. Prerequisites

Ensure the following tools are installed on your workstation:
- **Java Development Kit (JDK):** Version 21 LTS.
- **Node.js & npm:** Node.js v20+ LTS and npm v10+.
- **Docker & Docker Compose:** Required to run the containerized MySQL 8.4 LTS database locally.
- **Git:** Standard Git client.

---

## 3. Quickstart & Local Setup Guide

### Step 1: Environment Configuration
Copy the sample environment file to create your local `.env`:
```bash
cp .env.example .env
```

### Step 2: Start the MySQL 8.4 Database Container
Start the MySQL 8.4 LTS service using Docker Compose:
```bash
docker compose up -d
```
Verify the container health:
```bash
docker compose ps
```
The MySQL container initializes persistent storage in the `fleetx_mysql_data` Docker volume on port `3306`.

To stop the database:
```bash
docker compose down
```

---

## 4. Backend (Spring Boot) Commands

Navigate to the `backend/` directory:
```bash
cd backend
```

### Run the Backend Test Suite
Execute all 79 automated unit, module, Flyway schema, and end-to-end integration tests:
```bash
./mvnw test
```

### Run the Spring Boot Application (Development Profile)
```bash
./mvnw spring-boot:run
```
By default, `SPRING_PROFILES_ACTIVE=dev` is active and the backend listens on `http://localhost:8080`:
- **Health Check:** `http://localhost:8080/api/health`
- **CSRF Bootstrap:** `http://localhost:8080/api/v1/auth/csrf`
- **REST API Base:** `http://localhost:8080/api/v1`
- **STOMP WebSocket Endpoint:** `ws://localhost:8080/ws/fleetx`

### Repeatable Development Demo Dataset (`fleetx.demo.seed.enabled`)
When running with the `dev` profile (`DEMO_SEED_ENABLED=true`, default in `application-dev.yml`, disabled in production and test profiles), FleetX automatically runs ordered, idempotent demo seeders across all 5 modules using their real domain services and shared contracts:
- **Demo Accounts (Dev Only):**
  - `admin@fleetx.local` / `DevAdmin123!` (`ADMIN`)
  - `manager@fleetx.local` / `FleetDemo123!` (`FLEET_MANAGER`)
  - `dispatcher@fleetx.local` / `FleetDemo123!` (`DISPATCHER`)
  - `driver@fleetx.local` / `FleetDemo123!` (`DRIVER`)
  - `viewer@fleetx.local` / `FleetDemo123!` (`VIEWER`)
- **Demo Logistics Data:** 3 commercial vehicles (`MH-12-FX-1001`, `KA-01-FX-2002`, `DL-04-FX-3003`), fuel logs with computed km/L efficiency, preventive/upcoming maintenance records, 2 commercial drivers, an active `IN_PROGRESS` trip and a `PLANNED` trip, 2 shipments (`SHP-DEMO-2026-001` with 3 Haversine route waypoints and `SHP-DEMO-2026-002`), 2 circular/polygonal geofence zones, and an initialized simulated GPS tracking session.

---

## 5. Frontend (React + Vite) Commands

Navigate to the `frontend/` directory:
```bash
cd frontend
```

### Install Dependencies
```bash
npm install
```

### Run the Development Server
```bash
npm run dev
```
The frontend SPA is accessible at `http://localhost:5173` and proxies `/api` and `/ws` requests to `http://localhost:8080`.

### Run Frontend Automated Tests
```bash
npm test -- --run
```

### Build for Production
```bash
npm run build
```

---

## 6. Architectural Principles & Honest Academic Disclosures

- **Modular Monolith & Strict Boundary Isolation:** All 5 modules live in a single deployable artifact (`com.fleetx.{auth,fleet,drivers,shipments,tracking}`). Cross-module communication strictly uses interfaces in `com.fleetx.shared.contract` (`VehicleLookupService`, `DriverLookupService`, `TripLookupService`, `ShipmentLookupService`, `AlertPublisher`). Direct cross-module repository or entity imports are forbidden and enforced by automated architectural tests.
- **Flyway Schema Governance:** Database schema is managed strictly via Flyway scripts (`V100`–`V501`) in `backend/src/main/resources/db/migration/`. Hibernate `ddl-auto` is set to `validate`.
- **Simulated GPS Telemetry Disclosure:** All live location coordinates and STOMP WebSocket telemetry streams are generated by the software `SimulatedGpsService` interpolating along route waypoints (`SIMULATED_GPS_TELEMETRY`) and are explicitly labeled **`SIMULATED DATA`** in both API responses and UI badges.
- **Estimated Route Distance & ETA Disclosure:** Route distances are calculated via the great-circle Haversine formula and labeled as **Estimated Straight-Line (Haversine) Distance** and **Estimated Arrival Time**.
- **Stylized 3D Fleet View Disclosure:** The Three.js scene in the Control Tower renders a stylized procedural 3D visualization synchronized with vehicle status and simulation progress.
