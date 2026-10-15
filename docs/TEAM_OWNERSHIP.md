# FleetX — Intelligent Fleet & Logistics Management System
## Team Ownership & Governance Matrix

**Document Reference:** FLEETX-TEAM-2026-V1.0  
**Academic Year:** 2026–27  
**Team Roster Size:** Exactly 5 Students  
**Baseline Date:** 10 October 2026  
**Status:** Approved Team Governance Charter (Phase 0)

---

## 1. Team Composition & Role Distribution

| Student Role | Project Title | Primary Module Ownership | Package & Directory Scope |
|---|---|---|---|
| **Student 1** *(Team Leader & Lead Architect)* | System Architect & Identity Lead | **Module 1: Identity & Admin** + Shared Foundation | `com.fleetx.common.*`<br>`com.fleetx.identity.*`<br>`frontend/src/features/identity/*`<br>`frontend/src/shared/*` |
| **Student 2** | Fleet Asset Lead | **Module 2: Fleet & Assets** | `com.fleetx.fleet.*`<br>`frontend/src/features/fleet/*` |
| **Student 3** | Operations & Dispatch Lead | **Module 3: Driver & Trip Operations** | `com.fleetx.trip.*`<br>`frontend/src/features/trips/*` |
| **Student 4** | Logistics & Routing Lead | **Module 4: Shipment & Routes** | `com.fleetx.shipment.*`<br>`frontend/src/features/shipments/*` |
| **Student 5** | Telemetry & Control Tower Lead | **Module 5: Tracking & Control Tower** | `com.fleetx.tracking.*`<br>`frontend/src/features/tracking/*` |

---

## 2. Leadership & Autonomous Ownership Principles

To prevent both chaos and over-centralization, FleetX operates under four fundamental governance rules:
1. **Module Autonomy:** Each module owner (Students 2–5) has full creative and technical authority over the internal implementation of their domain (controllers, internal services, repositories, private DTOs, and React feature components).
2. **Contract Preservation:** No student may unilaterally modify shared contracts in `com.fleetx.common.contract` or shared DTOs in `com.fleetx.common.dto`. Any change to a shared contract requires consensus between the caller, the implementer, and Student 1 (Architect).
3. **Leader as Facilitator & Integrator:** Student 1 provides the shared Maven/npm skeleton, security layer, and CI pipeline, and coordinates pull requests and releases. The leader acts as an unblocker and integration reviewer, **not** a micromanager who rewires other members' domain code.
4. **Flyway Namespace Quarantine:** Each student owns their migration directory prefix (`V100`–`V199` for Student 1, `V200`–`V299` for Student 2, etc.). No student may touch another student's SQL migration files.

---

## 3. RACI Responsibility Assignment Matrix

**RACI Definitions:**
- **R (Responsible):** The primary author doing the work.
- **A (Accountable):** The individual with final approval and sign-off authority.
- **C (Consulted):** Stakeholders whose input must be sought before finalizing changes.
- **I (Informed):** Individuals notified of progress and completed milestones.

| Deliverable / Functional Area | Student 1 (Lead/Mod 1) | Student 2 (Mod 2) | Student 3 (Mod 3) | Student 4 (Mod 4) | Student 5 (Mod 5) |
|---|:---:|:---:|:---:|:---:|:---:|
| **Shared Foundation (`com.fleetx.common.*`)** | **A / R** | C | C | C | C |
| **Spring Security, Sessions & CSRF Engine** | **A / R** | I | I | I | I |
| **Module 1 (Identity, Users, Audit Logs)** | **A / R** | I | I | I | I |
| **Module 2 (Vehicles, Fuel, Maintenance)** | C | **A / R** | C | I | C |
| **VehicleLookupService Contract Implementation**| C | **A / R** | C | I | C |
| **Module 3 (Drivers, Trip Lifecycle, Scoring)** | C | C | **A / R** | C | C |
| **DriverLookupService & TripLookupService** | C | I | **A / R** | C | C |
| **Module 4 (Shipments, Waypoints, Timeline)** | C | I | C | **A / R** | I |
| **ShipmentLookupService Contract Implementation**| C | I | C | **A / R** | I |
| **Module 5 (GPS Simulation, WebSocket STOMP)** | C | I | I | I | **A / R** |
| **2D Leaflet Map & 3D Three.js Visualizer** | C | I | I | I | **A / R** |
| **AlertPublisher Contract Implementation** | C | C | C | C | **A / R** |
| **Flyway Schema Migrations Coordination** | **A** | R (V200) | R (V300) | R (V400) | R (V500) |
| **Frontend Global Shell, Navbar, Layout** | **A / R** | C | C | C | C |
| **End-to-End Cross-Module Integration Tests** | **A** | R | R | R | R |
| **PBL Final Presentation & Report Assembly** | **A / R** | R | R | R | R |

---

## 4. Code Review & Approval Policies

1. **Pull Request Peer Review Requirement:**
   - Every Pull Request (PR) into the `develop` integration branch MUST have at least **two approvals**:
     - The **Module Owner** (accountable author).
     - The **Team Leader (Student 1)** OR a directly impacted **Peer Module Owner** (for contract integration).
2. **Disallowed Actions:**
   - Direct pushing to `main` or `develop` is strictly blocked via branch protection.
   - Self-approval and self-merging of PRs are disabled.
3. **Merge Conflict Resolution:**
   - Merge conflicts occurring within module packages (`com.fleetx.fleet`, `src/features/fleet`, etc.) must be resolved by that module's owner.
   - Merge conflicts occurring in `com.fleetx.common` or `pom.xml` / `package.json` must be resolved jointly with Student 1.

---

## 5. Escalation & Dispute Resolution Protocol

1. **Interface Contract Disputes:** If Student 3 requires a new method on `VehicleLookupService` (owned by Student 2), both students must submit an Interface RFC issue on GitHub. If they cannot agree on the DTO shape within 24 hours, Student 1 arbitrates the decision based on architectural simplicity.
2. **Blocked Integration:** If a student is blocked due to delayed upstream work (e.g., Module 4 blocked waiting for Module 3 trip APIs), the blocked student MUST use Mockito stubs or MSW (Mock Service Worker) adhering to `docs/API_CONTRACTS.md` so that development continues without idle delays.
3. **Faculty Escalation:** If persistent attendance or contribution issues occur, the issue is logged in the Weekly Report and escalated to the academic faculty mentor.
