# FleetX — Agent & Developer Engineering Rules (FLEETX_AGENT_RULES)

**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Academic Year:** 2026–27  
**Applies To:** All AI Coding Agents, Human Developers, and Contributors  
**Status:** Mandatory Engineering Contract

---

## 1. Fundamental Architecture & Boundary Rules

### Rule 1.1: Strict Modular Monolith — No Microservices
- The application MUST remain a single deployable Modular Monolith running on Java 21 and Spring Boot 3.5.x.
- Do NOT introduce Docker Compose networks for distributed microservices, gRPC, Eureka/Consul service registries, or distributed message brokers like Kafka/RabbitMQ.
- Use internal in-process service invocations and Spring's built-in WebSocket STOMP broker.

### Rule 1.2: Zero Cross-Module Repository Leaks
- A domain module (`identity`, `fleet`, `trip`, `shipment`, `tracking`) MUST NEVER import, `@Autowired`, or inject another domain module's Spring Data JPA repository or internal entity.
- Example: `ShipmentService` MUST NOT inject `VehicleRepository` or `DriverRepository`.
- Any attempt to import across module boundaries without using `com.fleetx.common.contract` will be rejected by code review.

### Rule 1.3: Mandatory Contract Inversion
- All cross-module lookups and events MUST proceed through declared interfaces in `com.fleetx.common.contract`:
  1. `VehicleLookupService` (Implemented by Module 2)
  2. `DriverLookupService` (Implemented by Module 3)
  3. `TripLookupService` (Implemented by Module 3)
  4. `ShipmentLookupService` (Implemented by Module 4)
  5. `AlertPublisher` (Implemented by Module 5)
- Cross-boundary data transfer MUST use immutable Java records located in `com.fleetx.common.dto`.

---

## 2. Database & Migration Governance

### Rule 2.1: Hibernate Auto-DDL Strictly Prohibited
- `spring.jpa.hibernate.ddl-auto` MUST be set to `validate`.
- Never use `update`, `create`, or `create-drop`.
- All schema alterations MUST be executed through Flyway migration scripts.

### Rule 2.2: Strict Flyway Version Range Allocation
Each module developer owns a reserved migration range. Never introduce a migration outside your module's allocated range:
- **Module 1 (Identity & Admin):** `V100` – `V199`
- **Module 2 (Fleet & Assets):** `V200` – `V299`
- **Module 3 (Driver & Trip Operations):** `V300` – `V399`
- **Module 4 (Shipment & Routes):** `V400` – `V499`
- **Module 5 (Tracking & Control Tower):** `V500` – `V599`

### Rule 2.3: Target Relational Database
- The database is MySQL 8.4 LTS using the InnoDB storage engine and `utf8mb4` character set.
- All primary keys MUST be 64-bit `BIGINT AUTO_INCREMENT`.
- All timestamps MUST use `DATETIME(6)`.

---

## 3. Technology Stack & Pinned Standards

- **Java Version:** Java 21 LTS (use modern features: Records, Pattern Matching, Sealed Classes, Text Blocks).
- **Spring Framework:** Spring Boot 3.5.x (Pinned GA patch release; strictly no milestone or snapshot releases).
- **Frontend Stack:** React 18, Vite, TypeScript, Bootstrap 5.3, custom CSS modules.
- **Geospatial & Visualization:**
  - 2D Maps: Leaflet 1.9 + OpenStreetMap tiles.
  - 3D Visualization: Three.js r160+ stylized canvas.
- **Messaging:** Spring WebSocket with STOMP sub-protocol over SockJS fallback.
- **Build Tools:** Apache Maven for backend, npm for frontend.

---

## 4. Security, Session & Authentication Standards

### Rule 4.1: Server-Side HTTP Sessions
- Authentication MUST use stateful Spring Security server-side sessions (`JSESSIONID`).
- Do NOT replace sessions with stateless JWT tokens unless explicitly mandated by faculty revision.
- Cookies MUST enforce `HttpOnly` and `SameSite=Lax`.

### Rule 4.2: Password Encryption
- Passwords MUST be encrypted with BCrypt (`BCryptPasswordEncoder` with strength 12).
- Plaintext passwords must NEVER be logged or serialized in responses.

### Rule 4.3: Mandatory CSRF Defense
- CSRF protection MUST remain enabled for all state-changing HTTP requests (`POST`, `PUT`, `DELETE`, `PATCH`).
- The frontend Axios/fetch interceptor MUST forward the `X-XSRF-TOKEN` header.

### Rule 4.4: Canonical User Roles
Authorization must strictly verify the five defined role strings:
1. `ROLE_ADMIN`
2. `ROLE_FLEET_MANAGER`
3. `ROLE_DISPATCHER`
4. `ROLE_DRIVER`
5. `ROLE_VIEWER`

---

## 5. API & Data Formatting Conventions

### Rule 5.1: Unified URL Prefix & Identifier Standards
- All REST endpoints MUST begin with `/api/v1/`.
- Date/Time fields MUST be serialized in ISO 8601 UTC (`YYYY-MM-DD'T'HH:mm:ss'Z'`).
- Business codes MUST use defined formats: `TRIP-YYYY-NNNN` and `SHP-YYYY-NNNN`.

### Rule 5.2: Enforced Response Envelopes
Every response MUST return one of the standardized envelopes defined in `docs/API_CONTRACTS.md`:
- `ApiResponse<T>` for single object responses.
- `PageResponse<T>` for paginated collections.
- `ApiErrorResponse` for error responses (containing `errorCode`, `message`, `validationErrors`).

---

## 6. Code Quality, Testing & Git Workflow

### Rule 6.1: Conventional Commits
All commit messages MUST adhere to Conventional Commits:  
`<type>(<scope>): <short description>`  
Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

### Rule 6.2: Branching Standard
- No direct commits to `main` or `develop`.
- Work strictly in `feature/mod<1-5>-<short-description>`.
- Pull Requests require at least two approvals (Module Owner + Team Leader).

### Rule 6.3: Verification Before Merge
Every PR must verify:
- Backend: `mvn clean test` succeeds with zero errors.
- Frontend: `npm test` and `npm run lint` pass cleanly.
- No shortcuts, stub bypasses, or disabled security tests.
