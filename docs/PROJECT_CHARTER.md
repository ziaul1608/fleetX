# FleetX — Intelligent Fleet & Logistics Management System
## Project Charter

**Document Version:** 1.0.0-BASELINE  
**Academic Year:** 2026–27  
**Project Category:** College Java Project-Based Learning (PBL)  
**Baseline Planning Window:** 6 July 2026 – 6 October 2026  
**Architecture Review Date:** 10 October 2026  
**Status:** Approved for Phase 0 Baseline (Architecture & Contracts)

---

### 1. Executive Summary & Vision

**FleetX** is an enterprise-grade, modular monolithic fleet and logistics management system engineered in Java 21, Spring Boot 3.5.x, MySQL 8.4 LTS, and React with TypeScript. Designed as a capstone Project-Based Learning (PBL) software engineering initiative, FleetX solves the operational friction that logistics companies face when managing dispersed vehicle fleets, fluctuating asset health, complex multi-stop trip schedules, dynamic shipments, and real-time transit visibility.

By unifying vehicle lifecycle tracking, automated preventive maintenance, driver duty rosters, trip dispatching, cargo delivery milestones, and a dual-visualization Control Tower (combining 2D Leaflet geographic maps with stylized 3D Three.js fleet status views), FleetX bridges the gap between administrative oversight and real-time field operations.

---

### 2. Problem Statement

Mid-sized logistics providers and transport operators in developing commercial sectors frequently suffer from severe operational fragmentation:
1. **Siloed Data & Fragmented Tools:** Maintenance logs are maintained in spreadsheets, trip schedules on dispatch boards, driver licenses in HR files, and delivery notes on paper manifests.
2. **Maintenance Blindspots:** Fleet managers lack automated warnings triggered by mileage and service intervals, resulting in preventable engine breakdowns and escalated asset depreciation.
3. **Dispatch & Driver Pairing Bottlenecks:** Dispatchers frequently assign overloaded cargo or assign drivers with expired licenses or conflicting duty hours due to the absence of unified availability checks.
4. **Visibility Vacuum:** Customers and dispatchers have no continuous visibility of in-transit shipments, relying on reactive phone calls to resolve delays, route deviations, and cargo status queries.
5. **Architectural Complexity in Student Engineering:** Many academic systems fail either by overengineering into fragile microservices that students cannot run reliably on local machines, or by degenerating into an unmaintainable "spaghetti monolith" where every controller directly queries arbitrary database tables.

FleetX addresses these challenges by delivering an architecturally disciplined **Modular Monolith** with strict inter-module contracts, zero cross-domain repository leaks, Flyway database versioning, and unified real-time telemetry simulation.

---

### 3. Project Objectives (SMART)

1. **Architecture & Boundaries (Specific):** Implement a decoupled modular monolith spanning five isolated domain modules communicating exclusively through immutable DTOs and declared service lookup interfaces.
2. **Traceability & Safety (Measurable):** Achieve 100% database schema versioning via Flyway migration ranges (V100–V599) on MySQL 8.4 LTS with zero reliance on Hibernate automatic schema mutation (`ddl-auto: validate`).
3. **Real-Time Simulation (Achievable):** Broadcast GPS telemetry updates via WebSocket/STOMP at configurable intervals (1–5 seconds) to 2D Leaflet map views and stylized Three.js 3D fleet displays under 500ms broadcast latency.
4. **Operational Efficiency (Relevant):** Reduce dispatch-to-assignment turnaround by validating driver license validity and vehicle roadworthiness automatically prior to trip state confirmation.
5. **Team Feasibility (Time-bound):** Establish an exact five-student development distribution with strict module ownership, daily/weekly audit evidence, and progressive integration milestones.

---

### 4. Scope Definition

#### 4.1 In-Scope (Mandatory Core MVP)
- **Module 1 (Identity & Admin):** Secure session-based authentication, BCrypt password hashing, CSRF protection, RBAC (5 defined roles), user profile management, and tamper-evident audit logging.
- **Module 2 (Fleet & Assets):** Vehicle asset registry, odometer tracking, status tracking (AVAILABLE, IN_TRIP, MAINTENANCE, RETIRED), fuel logs with automated fuel efficiency (km/L) calculation, maintenance scheduling, and preventive service warnings.
- **Module 3 (Driver & Trip Operations):** Driver profile & license expiration tracking, driver availability states (AVAILABLE, ON_DUTY, SUSPENDED, ON_LEAVE), vehicle/driver assignment engine, trip lifecycle state machine (`DRAFT` → `ASSIGNED` → `IN_TRANSIT` → `COMPLETED` / `CANCELLED`), and basic driver performance scoring.
- **Module 4 (Shipment & Routes):** Cargo shipment registry, route waypoints with sequence ordering, trip-to-shipment cargo linking, delivery lifecycle state machine (`ORDERED` → `DISPATCHED` → `IN_TRANSIT` → `DELIVERED` / `RETURNED`), delivery milestone timeline, and route distance/ETA calculation.
- **Module 5 (Tracking & Control Tower):** Configurable GPS telemetry simulator engine, WebSocket/STOMP broker integration, 2D Leaflet interactive map with polyline route history, 3D Three.js stylized depot/vehicle representation, circular geofence boundary breach detection, operational alert publishing, Control Tower KPI summary dashboard, and CSV trip/telemetry report export.

#### 4.2 Out-of-Scope (Optional Enhancements / Post-PBL Phase)
- Integration with physical hardware OBD-II dongles or real cellular IoT GPS hardware (simulated telemetry engine is used instead).
- Native iOS / Android mobile applications (responsive web UI provided via Bootstrap 5).
- Multi-tenant enterprise billing, commercial payment gateway integration, or tax invoice filing.
- Real-time AI road traffic camera feed video analytics.

---

### 5. Stakeholder & User Role Matrix

FleetX establishes five distinct role-based access levels enforced at both the Spring Security filter layer and UI routing level:

| Role Code | Role Name | Primary Responsibilities |
|---|---|---|
| `ADMIN` | System Administrator | User account provisioning, role assignment, system configuration, global audit log review, and database health oversight. |
| `FLEET_MANAGER` | Fleet & Asset Manager | Vehicle asset onboarding, maintenance scheduling, fuel log oversight, service warning review, and fleet asset decommissioning. |
| `DISPATCHER` | Logistics Dispatcher | Trip creation, route waypoint planning, cargo shipment bundling, driver & vehicle assignment, and operational trip initiation. |
| `DRIVER` | Commercial Driver | View assigned active trips, view route waypoints and cargo manifests, report trip status transitions, and review personal performance logs. |
| `VIEWER` | Executive / Auditor | Read-only oversight of Control Tower KPI dashboards, live 2D/3D tracking displays, shipment timeline status, and CSV audit reports. |

---

### 6. Technical Stack & Architectural Constraints

- **Backend Platform:** Java 21 LTS, Spring Boot 3.5.x (Pinned GA patch, strictly no development snapshots).
- **Relational Database:** MySQL 8.4 LTS, managed strictly with Flyway database migration scripts.
- **ORM & Data Access:** Spring Data JPA / Hibernate Core (configured with `hibernate.hbm2ddl.auto = validate`).
- **Web & Security:** Spring MVC, Spring Security with server-side HTTP sessions (`JSESSIONID`, `HttpOnly`, `SameSite=Lax`), CSRF token repository, and BCrypt password encryption.
- **Real-Time Messaging:** Spring WebSocket with STOMP sub-protocol over SockJS fallback.
- **Frontend Stack:** React 18, Vite build tool, TypeScript, Bootstrap 5.3 + custom CSS modules.
- **Spatial & Visual Systems:** Leaflet 1.9 + OpenStreetMap tiles (2D geospatial mapping), Three.js r160+ (3D stylized asset visualization canvas).
- **Build & Quality Tooling:** Apache Maven (Java backend), npm (React frontend), JUnit 5, Mockito, AssertJ, Spring Boot Test, Vitest, and React Testing Library.

---

### 7. Governance, Team Structure & Module Ownership

The engineering team consists of exactly five members with clear separation of concerns:

```
                          ┌──────────────────────────────────────────────┐
                          │     Student 1: Team Leader & Architect       │
                          │   Module 1: Identity & Admin + Foundation    │
                          └──────────────────────┬───────────────────────┘
                                                 │
         ┌────────────────────────┬──────────────┴───────────┬────────────────────────┐
         │                        │                          │                        │
┌────────┴───────────────┐ ┌──────┴────────────────┐ ┌───────┴────────────────┐ ┌─────┴──────────────────┐
│  Student 2: Developer  │ │  Student 3: Developer │ │  Student 4: Developer  │ │  Student 5: Developer  │
│ Module 2: Fleet & Assets│ │Module 3: Driver & Trip│ │Module 4: Shipments/Routes│ │Module 5: Tracking Tower│
└────────────────────────┘ └───────────────────────┘ └────────────────────────┘ └────────────────────────┘
```

- **Student 1 (Leader / Architect):** Shared architecture, common interfaces, security infrastructure, CI workflow, and Module 1 (Identity & Admin).
- **Student 2:** Module 2 (Fleet & Assets — Vehicles, Fuel, Maintenance, Warnings).
- **Student 3:** Module 3 (Driver & Trip Operations — Drivers, Rostering, Assignments, Trip Lifecycle).
- **Student 4:** Module 4 (Shipment & Routes — Shipments, Waypoints, Timeline, ETA Calculation).
- **Student 5:** Module 5 (Tracking & Control Tower — GPS Simulation, WebSocket, Leaflet, Three.js, Alerts, Reports).

---

### 8. Historical Timeline Context & Baseline Review

* **Original Requested Planning Window:** 6 July 2026 – 6 October 2026.
* **Architecture Review & Baseline Sign-Off Date:** 10 October 2026.
* **Status:** Architectural documentation baseline frozen. All subsequent coding phases must strictly align with this specification without ad-hoc deviation. Planned milestone dates in the roadmap define schedule baselines and must not be misinterpreted as completed implementation until substantiated by commit logs and test evidence.
