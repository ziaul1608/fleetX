# FleetX — Intelligent Fleet & Logistics Management System
## Comprehensive Test Plan & Verification Strategy

**Document Reference:** FLEETX-TEST-2026-V1.0  
**Academic Year:** 2026–27  
**Baseline Date:** 10 October 2026  
**Status:** Approved Quality Assurance & Test Specification (Phase 0)

---

## 1. Testing Frameworks & Tooling Stack

| Testing Tier | Technology Stack | Scope & Purpose |
|---|---|---|
| **Backend Unit Testing** | JUnit 5 (`jupiter`), Mockito, AssertJ | Fast, isolated unit testing of domain business logic and service state machines without Spring application context overhead. |
| **Backend Slice Testing** | `@WebMvcTest`, Spring Security Test | Controller validation, JSON payload serialization/deserialization, HTTP status codes, validation errors, and `@PreAuthorize` security checks. |
| **Data Layer Testing** | `@DataJpaTest`, H2 (MySQL Mode) / Testcontainers | Verification of custom JPA query methods, entity constraints, and auditing columns. |
| **Cross-Module Contract Testing**| Mockito Stubs of Contract Interfaces | Validating that domain modules properly invoke and handle return values from `VehicleLookupService`, `DriverLookupService`, etc. |
| **Frontend Unit & Component Testing**| Vitest, React Testing Library, jsdom | Component rendering, user form interaction, role-based UI guards, and state changes. |
| **Frontend API Mocking** | Mock Service Worker (MSW) / Mocked Axios | Mocking backend REST responses and error envelopes during UI testing. |
| **STOMP / WebSocket Simulation Testing**| Spring `TestChannelInterceptor`, Mockito | Verification of message broadcasting to STOMP destinations and geofence evaluation triggers. |

---

## 2. Test Architecture & Modular Isolation

To maintain modular monolith boundaries during testing, unit tests must mock external domain services:

```
┌─────────────────────────────────────────────────────────────┐
│                 MODULE 3: TRIP SERVICE TEST                 │
│                                                             │
│   ┌─────────────────────┐                                   │
│   │     TripService     │◄─── Unit Under Test               │
│   └──────────┬──────────┘                                   │
│              │                                              │
│              ├──────────────────────┬──────────────────┐    │
│              ▼                      ▼                  ▼    │
│   ┌─────────────────────┐  ┌──────────────────┐ ┌─────────┐ │
│   │VehicleLookupService │  │DriverLookupServ. │ │TripRepo │ │
│   │   (Mockito Mock)    │  │  (Mockito Mock)  │ │ (Mock)  │ │
│   └─────────────────────┘  └──────────────────┘ └─────────┘ │
└─────────────────────────────────────────────────────────────┘
```
- Module 3 tests do NOT initialize Module 2's repository or database tables.
- `VehicleLookupService.isVehicleAvailableForTrip(id)` is mocked to return `true` or `false` to verify assignment validation branches.

---

## 3. Module-by-Module Test Matrix & Test Cases

### 3.1 Module 1: Identity & Admin Test Cases
| Test ID | Test Scenario | Execution Type | Expected Outcome |
|---|---|:---:|---|
| `TC-ADM-01` | Successful login with valid email & password | Controller (`@WebMvcTest`) | Returns HTTP 200, creates session, sets `JSESSIONID` cookie and CSRF token. |
| `TC-ADM-02` | Login attempt with incorrect password | Controller (`@WebMvcTest`) | Returns HTTP 401 Unauthorized; logs failed attempt in audit log. |
| `TC-ADM-03` | Non-admin user attempts to access `/api/v1/admin/users` | Security Test | Returns HTTP 403 Forbidden. |
| `TC-ADM-04` | Admin creates user with duplicate email | Service Unit Test | Throws `ResourceConflictException`, returns HTTP 409. |
| `TC-ADM-05` | Mutating request submitted without valid CSRF header | Security Filter Test | Request rejected with HTTP 403 Invalid CSRF Token. |

### 3.2 Module 2: Fleet & Assets Test Cases
| Test ID | Test Scenario | Execution Type | Expected Outcome |
|---|---|:---:|---|
| `TC-FLT-01` | Register vehicle with valid 17-char VIN and plate | Service Unit Test | Vehicle saved with status `AVAILABLE` and initial odometer recorded. |
| `TC-FLT-02` | Register vehicle with invalid VIN format (12 chars) | Controller Slice Test | Jakarta validation fails with HTTP 400 and `VALIDATION_FAILED` envelope. |
| `TC-FLT-03` | Log fuel entry with lower odometer than prior entry | Service Unit Test | Throws `ValidationException` rejecting backwards odometer reading. |
| `TC-FLT-04` | Calculate fuel efficiency across two fuelings | Service Unit Test | Computes exact km/L based on $\frac{\Delta\text{km}}{\text{liters}}$. |
| `TC-FLT-05` | Vehicle odometer reaches within 500 km of service threshold | Service Unit Test | System flags vehicle status with `WARNING_SERVICE_DUE`. |

### 3.3 Module 3: Driver & Trip Operations Test Cases
| Test ID | Test Scenario | Execution Type | Expected Outcome |
|---|---|:---:|---|
| `TC-TRP-01` | Dispatcher creates trip with available driver and vehicle | Service Unit Test | Trip created in `ASSIGNED` status; vehicle and driver marked unavailable. |
| `TC-TRP-02` | Assign driver whose license expired yesterday | Service Unit Test | Throws `UnprocessableEntityException` ("Driver license expired"). |
| `TC-TRP-03` | Assign vehicle currently marked `MAINTENANCE` | Service Unit Test | Throws `UnprocessableEntityException` ("Vehicle not available"). |
| `TC-TRP-04` | Advance trip status: `ASSIGNED` → `IN_TRANSIT` | Service Unit Test | Valid transition accepted; audit history record created. |
| `TC-TRP-05` | Attempt invalid state leap: `DRAFT` directly to `COMPLETED` | Service Unit Test | Throws `InvalidStateTransitionException`, HTTP 400. |

### 3.4 Module 4: Shipment & Routes Test Cases
| Test ID | Test Scenario | Execution Type | Expected Outcome |
|---|---|:---:|---|
| `TC-SHP-01` | Create cargo shipment with positive weight & volume | Controller Slice Test | HTTP 201 Created with generated `SHP-2026-XXXX` tracking number. |
| `TC-SHP-02` | Create shipment with negative cargo weight (-50 kg) | Controller Slice Test | Rejected by `@Positive` validation, HTTP 400. |
| `TC-SHP-03` | Add unordered waypoints to shipment | Service Unit Test | Waypoints stored and sorted strictly by `sequence_order` ascending. |
| `TC-SHP-04` | Calculate ETA using Haversine formula across 3 waypoints | Service Unit Test | Returns accurate cumulative distance and timestamp matching average 60 km/h speed. |
| `TC-SHP-05` | Record milestone event on shipment | Service Unit Test | Appends immutable entry to `shipment_timeline_events`. |

### 3.5 Module 5: Tracking & Control Tower Test Cases
| Test ID | Test Scenario | Execution Type | Expected Outcome |
|---|---|:---:|---|
| `TC-TRK-01` | Telemetry simulator interpolates coordinates between waypoints | Service Unit Test | Generates smooth intermediate lat/lng coordinates along route vector. |
| `TC-TRK-02` | Broadcast telemetry ping over WebSocket | STOMP Channel Test | Subscriber receives JSON frame with vehicle ID, coordinates, speed. |
| `TC-TRK-03` | Vehicle ping coordinates fall outside geofence boundary | Service Unit Test | Geofence breach detector triggers `AlertPublisher` with `GEOFENCE_BREACH`. |
| `TC-TRK-04` | Export CSV report with active trip filter | Controller Test | Returns HTTP 200, `text/csv` header, and correctly formatted CSV rows. |
| `TC-TRK-05` | 3D Three.js canvas handles vehicle update event | Frontend Component Test | Updates vehicle model color/position without crashing WebGL context. |

---

## 4. Test Execution Commands & CI Criteria

### 4.1 Local Execution Command Suite
```bash
# Execute fast unit tests only
mvn test -Dtest=*UnitTest

# Execute WebMvc controller slice tests
mvn test -Dtest=*ControllerTest

# Execute full backend test suite
mvn clean test

# Execute frontend Vitest suite
npm run test:run
```

### 4.2 Quality Gate & Exit Criteria
For Phase 1 and subsequent delivery milestones, code must meet the following gates before merging into `develop` or presenting for academic defense:
1. **Zero Failing Tests:** 100% of unit and slice tests must pass.
2. **Line Coverage:** Minimum **75% line coverage** across domain service classes.
3. **No Compiler / Linter Warnings:** Zero ESLint errors, zero TypeScript errors, clean Maven build.
4. **Clean Schema Validation:** Spring Boot application must start cleanly with `hibernate.hbm2ddl.auto = validate` against a migrated MySQL schema.
