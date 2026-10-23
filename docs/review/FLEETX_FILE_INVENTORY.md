# FleetX — Complete First-Party File Inventory & Responsibility Registry

**Document:** System File Inventory & Structural Analysis  
**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Audit Date:** 2026-10-23  
**Status:** VERIFIED (Generated from exact repository files; excludes vendor/build directories)

---

## 1. Directory Structure Overview

```
fleetx/
├── backend/                              # Java 21 / Spring Boot 3.5.16 Application
│   ├── pom.xml                           # Maven project manifest and dependencies
│   ├── src/main/java/com/fleetx/
│   │   ├── FleetxApplication.java        # Main Spring Boot entry point (@EnableScheduling)
│   │   ├── auth/                         # Module 1: Identity, RBAC, Sessions, Audit
│   │   ├── fleet/                        # Module 2: Fleet & Vehicle Asset Management
│   │   ├── drivers/                      # Module 3: Commercial Drivers & Trip Operations
│   │   ├── shipments/                    # Module 4: Shipment Manifests & Route Logistics
│   │   ├── tracking/                     # Module 5: GPS Simulation, Geofences, Alerts, Reports
│   │   └── shared/                       # Shared Kernel: Contracts, Envelopes, Config, Exceptions
│   ├── src/main/resources/
│   │   ├── application.yml               # Base application configuration
│   │   ├── application-dev.yml           # Development profile (H2 in MySQL mode, demo seeders enabled)
│   │   ├── application-qa.yml            # QA profile (Local MySQL 8.4 on port 3307)
│   │   ├── application-prod.yml          # Production profile (Cloud MySQL with TLS, Render port)
│   │   ├── db/migration/                 # Flyway migrations V100 to V501 (21 tables)
│   │   └── static/                       # Pre-compiled React 18 production bundle (Single-Origin)
│   └── src/test/java/com/fleetx/         # 17 automated test suites (88 tests)
├── frontend/                             # React 18 / TypeScript / Vite Application
│   ├── package.json                      # npm manifest (React 18.3.1, Vite 5.4.6, Bootstrap 5.3.3)
│   ├── vite.config.ts                    # Vite dev proxy configuration (targets 127.0.0.1:8080)
│   └── src/
│       ├── main.tsx                      # Frontend DOM mounting and React root
│       ├── app/                          # App shell, Layout, and Router registry
│       ├── features/                     # Feature modules mirroring backend domain packages
│       │   ├── auth/                     # Login, Profile, User Directory & Admin Modals
│       │   ├── fleet/                    # Vehicles, Fuel Logging, Maintenance Modals
│       │   ├── drivers/                  # Drivers, Trip Dispatch & Trip Lifecycle Modals
│       │   ├── shipments/                # Shipments, Waypoints & Route Planning Modals
│       │   └── tracking/                 # Control Tower, Leaflet 2D Map, Three.js 3D, Reports
│       ├── shared/                       # Shared UI components, Axios client, AuthContext
│       └── test/                         # Vitest + React Testing Library test suites (41 tests)
├── docs/                                 # Official academic and technical documentation
│   ├── README.md                         # Documentation index
│   ├── FleetX_Academic_Project_Report.pdf
│   ├── FleetX_Architecture_Conference_Paper.pdf
│   ├── FleetX_Project_Presentation_Slides.pdf
│   ├── FleetX_Research_Paper_IEEE.pdf
│   ├── FleetX_Software_Requirements_Specification_SRS.pdf
│   └── review/                           # Comprehensive audit & implementation blueprint
├── Dockerfile                            # Multi-stage Eclipse Temurin JRE 21 + Node 20 container build
├── docker-compose.yml                    # Local MySQL 8.4 container definition for QA testing
├── render.yaml                           # Render Blueprint Infrastructure-as-Code manifest (Free plan)
├── smoke_test.sh                         # 10-step automated HTTP production smoke test script
├── verify_mysql_persistence.py           # Python REST persistence restart verification script
└── FLEETX_AGENT_RULES.md                 # Strict engineering governance rules
```

---

## 2. Backend First-Party File Inventory

### 2.1 Application Root & Shared Kernel (`com.fleetx.shared`)

| File Path | Primary Classes / Interfaces | What It Actually Implements | Caller / Inbound Dependency | Outbound Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/FleetxApplication.java` | `FleetxApplication` | Main Spring Boot application bootstrap with `@SpringBootApplication` and `@EnableScheduling` | JVM / Maven runtime | SpringApplication | Boots JPA, Flyway, Scheduling | Shared Application Entry |
| `backend/src/main/java/com/fleetx/shared/config/SecurityConfig.java` | `SecurityConfig`, `SpaCsrfTokenRequestHandler` | Spring Security filter chain: server-side session cookies (`JSESSIONID`), `CookieCsrfTokenRepository.withHttpOnlyFalse()`, URL access rules (`/api/health`, `/api/v1/auth/**`), BCrypt password encoder | Spring Security Framework | `ObjectMapper`, `SpaCsrfTokenRequestHandler` | Inspects sessions & CSRF tokens | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/config/WebMvcConfig.java` | `WebMvcConfig` | Single-page application pushState URL fallback router: forwards non-API client routes to `/index.html` | Spring MVC | `ResourceHttpRequestHandler` | Serves frontend SPA assets | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/config/WebSocketConfig.java` | `WebSocketConfig` | STOMP message broker configuration: registers endpoint `/ws/fleetx` with SockJS, enables in-memory broker on `/topic` and application prefix `/app` | Spring Messaging | Spring WebSocket Framework | WebSockets / STOMP | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/contract/VehicleLookupService.java` | `VehicleLookupService` (interface) | Coarse lookup contract for vehicle existence, status, and summary without leaking JPA entities | TripService, TrackingService | `VehicleSummaryDto` | Resolves via `VehicleService` | Shared Contract (Implemented by Fleet) |
| `backend/src/main/java/com/fleetx/shared/contract/DriverLookupService.java` | `DriverLookupService` (interface) | Coarse lookup contract for driver licence validity and trip eligibility | TripService | `DriverSummaryDto` | Resolves via `DriverService` | Shared Contract (Implemented by Drivers) |
| `backend/src/main/java/com/fleetx/shared/contract/TripLookupService.java` | `TripLookupService` (interface) | Coarse lookup contract for trip status, assigned driver/vehicle, and active dispatches | ShipmentService, TrackingService | `TripSummaryDto` | Resolves via `TripService` | Shared Contract (Implemented by Drivers) |
| `backend/src/main/java/com/fleetx/shared/contract/ShipmentLookupService.java` | `ShipmentLookupService` (interface) | Coarse lookup contract for shipment status, tracking number, and cargo manifest | TrackingService | `ShipmentSummaryDto` | Resolves via `ShipmentService` | Shared Contract (Implemented by Shipments) |
| `backend/src/main/java/com/fleetx/shared/contract/AlertPublisher.java` | `AlertPublisher` (interface) | Cross-module event publishing interface for dispatch, overspeed, service-due, and geofence alerts | MaintenanceService, TripService, ShipmentService | `AlertEventDto` | Resolves via `TrackingService` | Shared Contract (Implemented by Tracking) |
| `backend/src/main/java/com/fleetx/shared/fallback/SharedFallbackConfiguration.java` | `SharedFallbackConfiguration` | `@ConditionalOnMissingBean` fallback beans providing safe no-op implementations of all 5 contracts when neighbor modules are absent | Spring Context | All 5 lookup contracts | None (in-memory no-op) | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/controller/HealthController.java` | `HealthController` | Public health verification endpoint: `GET /api/health` returning application status, timestamp, and active Spring profiles | Load Balancers, Render health check, Smoke tests | Spring Environment | None | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/dto/ApiResponse.java` | `ApiResponse<T>` | Standardized JSON response envelope: `success`, `message`, `data`, `timestamp` | All REST Controllers | Generic payload `T` | Formats all successful API responses | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/dto/PageResponse.java` | `PageResponse<T>` | Standardized paginated collection envelope: `content`, `page`, `size`, `totalElements`, `totalPages`, `last` | All paginated list endpoints | Generic content list | Formats paged responses | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/dto/ApiErrorResponse.java` | `ApiErrorResponse` | Standardized error envelope: `status`, `errorCode`, `message`, `path`, `timestamp`, `validationErrors` | `GlobalExceptionHandler` | `ValidationErrorDto` | Formats all 4xx/5xx API responses | Shared Foundation |
| `backend/src/main/java/com/fleetx/shared/exception/GlobalExceptionHandler.java` | `GlobalExceptionHandler` | `@RestControllerAdvice` translating domain exceptions, validation errors, and bad credentials into `ApiErrorResponse` | Spring MVC DispatcherServlet | `DomainException`, `MethodArgumentNotValidException` | Catches exceptions across all controllers | Shared Foundation |

---

### 2.2 Module 1: Identity, RBAC, Sessions & Audit (`com.fleetx.auth`)

| File Path | Primary Classes / Methods | What It Actually Implements | Callers / Consumers | Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/auth/entity/User.java` | `User` | JPA Entity for system users: `id`, `email`, `password_hash`, `full_name`, `active`, timestamps, many-to-many join with `Role` | `UserRepository`, `AuthService`, `AdminUserService` | `Role` | Maps to `users` and `user_roles` tables | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/entity/Role.java` | `Role` | JPA Entity for canonical roles (`ROLE_ADMIN`, `ROLE_FLEET_MANAGER`, `ROLE_DISPATCHER`, `ROLE_DRIVER`, `ROLE_VIEWER`) | `RoleRepository`, `User` | Jakarta Persistence | Maps to `roles` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/entity/AuditLog.java` | `AuditLog` | JPA Entity for security and administrative audit events: `user_id`, `action`, `entity_type`, `entity_id`, `client_ip`, `details_json` | `AuditLogRepository`, `AuditService` | Jakarta Persistence | Maps to `audit_logs` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/repository/UserRepository.java` | `UserRepository` | Spring Data JPA repository for user lookup, email existence checks, and role/status filtering | `AuthService`, `AdminUserService`, `CustomUserDetailsService` | Spring Data JPA | SQL queries on `users` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/repository/RoleRepository.java` | `RoleRepository` | Repository for role lookups by name | `AdminUserService`, `AdminBootstrapRunner`, `DemoIdentitySeeder` | Spring Data JPA | SQL queries on `roles` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/repository/AuditLogRepository.java` | `AuditLogRepository` | Repository for paged audit log queries | `AuditService` | Spring Data JPA | SQL queries on `audit_logs` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/service/AuthService.java` | `login()`, `getCurrentUser()`, `logout()` | Session authentication with BCrypt verification, session creation, active status check, and audit trail recording | `AuthController` | `UserRepository`, `PasswordEncoder`, `AuditService` | Authenticates against `users`, writes to `audit_logs` | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/service/AdminUserService.java` | `getUsers()`, `createUser()`, `updateUser()`, `updateUserStatus()`, `resetUserPassword()` | Administrative staff provisioning, role assignment, deactivation, and password reset | `AdminUserController` | `UserRepository`, `RoleRepository`, `PasswordEncoder`, `AuditService` | Mutates `users`, `user_roles`, writes `audit_logs` | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/service/AuditService.java` | `recordEvent()`, `getAuditLogs()` | Autonomous transaction audit logger (`Propagation.REQUIRES_NEW`) ensuring audit survival on business failures | `AuthService`, `AdminUserService`, `AdminBootstrapRunner` | `AuditLogRepository` | Writes to `audit_logs` table | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/service/CustomUserDetailsService.java` | `loadUserByUsername()` | Spring Security UserDetailsService implementation for database authentication | Spring Security `DaoAuthenticationProvider` | `UserRepository` | Queries `users` and `roles` | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/controller/AuthController.java` | `login()`, `getCurrentUser()`, `logout()`, `getCsrfToken()` | REST Controller: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/csrf` | Frontend `authApi.ts`, Smoke test | `AuthService` | Exposes public/session auth endpoints | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/controller/AdminUserController.java` | `listUsers()`, `getUserById()`, `createUser()`, `updateUser()`, `updateUserStatus()`, `resetPassword()`, `listAuditLogs()` | REST Controller: `GET/POST /api/v1/admin/users`, `PUT /api/v1/admin/users/{id}`, status, password reset, and `GET /api/v1/admin/audit-logs` | Frontend `authApi.ts` | `AdminUserService`, `AuditService` | Restricted to `ROLE_ADMIN` | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/config/AdminBootstrapRunner.java` | `run()` | Idempotent startup runner provisioning the initial administrator account from environment variables | Spring Boot lifecycle | `UserRepository`, `RoleRepository`, `PasswordEncoder`, `AuditService` | Inserts initial admin if missing | Member 1 (Identity & Admin) |
| `backend/src/main/java/com/fleetx/auth/config/DemoIdentitySeeder.java` | `run()` | Development-only seeder creating 5 role-based demo accounts (`admin@fleetx.local`, `manager@fleetx.local`, etc.) | Spring Boot lifecycle (`dev` profile only) | `UserRepository`, `RoleRepository`, `PasswordEncoder` | Populates demo users in H2/dev | Member 1 (Identity & Admin) |

---

### 2.3 Module 2: Fleet & Vehicle Asset Management (`com.fleetx.fleet`)

| File Path | Primary Classes / Methods | What It Actually Implements | Callers / Consumers | Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/fleet/entity/Vehicle.java` | `Vehicle` | JPA Entity: `vin`, `license_plate`, `make`, `model`, `model_year`, `vehicle_type`, `fuel_type`, `capacity_kg`, `capacity_volume_m3`, `odometer_km`, `status` | `VehicleRepository`, `VehicleService` | Jakarta Persistence, `VehicleStatus`, `VehicleType` | Maps to `vehicles` table | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/entity/FuelRecord.java` | `FuelRecord` | JPA Entity: `vehicle_id`, `litres`, `cost`, `odometer_km`, `efficiency_km_per_litre`, `logged_at` | `FuelRecordRepository`, `FuelService` | Jakarta Persistence, `Vehicle` | Maps to `fuel_records` table | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/entity/MaintenanceRecord.java` | `MaintenanceRecord` | JPA Entity: `vehicle_id`, `service_type`, `description`, `cost`, `service_date`, `odometer_at_service`, `next_service_due_km`, `next_service_due_date`, `status` | `MaintenanceRecordRepository`, `MaintenanceService` | Jakarta Persistence | Maps to `maintenance_records` table | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/entity/ServiceWarning.java` | `ServiceWarning` | JPA Entity: `vehicle_id`, `warning_type`, `severity`, `description`, `due_km`, `due_date`, `acknowledged` | `ServiceWarningRepository`, `MaintenanceService` | Jakarta Persistence | Maps to `service_warnings` table | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/service/VehicleService.java` | `findVehicles()`, `createVehicle()`, `updateVehicle()`, `updateVehicleStatus()`, `updateOdometer()` | Vehicle registration, unique VIN/plate enforcement, status transitions, and implementation of `VehicleLookupService` | `VehicleController`, `TripService` | `VehicleRepository` | Queries and mutates `vehicles` table | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/service/FuelService.java` | `logFuel()`, `getFuelHistory()`, `calculateEfficiency()` | Fuel entry logging with automated formula: `ΔOdometer / litres` (km/L). Flags non-positive deltas as null | `VehicleController` | `FuelRecordRepository`, `VehicleRepository` | Mutates `fuel_records`, updates vehicle odometer | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/service/MaintenanceService.java` | `logMaintenance()`, `evaluateServiceWarnings()`, `getActiveWarnings()` | Maintenance logging and threshold evaluation (WARNING if ≤ 500 km or 14 days; CRITICAL if overdue). Emits alerts via `AlertPublisher` | `VehicleController`, `FleetMaintenanceController` | `MaintenanceRecordRepository`, `ServiceWarningRepository`, `AlertPublisher` | Mutates `maintenance_records`, `service_warnings` | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/controller/VehicleController.java` | `getVehicles()`, `getVehicleById()`, `createVehicle()`, `updateVehicle()`, `updateStatus()`, `updateOdometer()`, `logFuel()`, `getFuelRecords()`, `scheduleMaintenance()` | REST Controller: `GET/POST /api/v1/fleet/vehicles`, details, fuel sub-resources, maintenance sub-resources | Frontend `fleetApi.ts`, Smoke test | `VehicleService`, `FuelService`, `MaintenanceService` | Exposes vehicle asset operations | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/controller/FleetMaintenanceController.java` | `getActiveWarnings()`, `getAllMaintenanceRecords()` | REST Controller: `GET /api/v1/fleet/warnings`, `GET /api/v1/fleet/maintenance-records` | Frontend `fleetApi.ts` | `MaintenanceService` | Paged maintenance and warnings | Member 2 (Fleet & Assets) |
| `backend/src/main/java/com/fleetx/fleet/service/DemoFleetSeeder.java` | `run()` | Development-only seeder creating 3 commercial vehicles (MH-12-FX-1001, KA-01-FX-2002, DL-04-FX-3003), fuel logs, and maintenance records | Spring Boot lifecycle (`dev` profile only) | `VehicleService`, `FuelService`, `MaintenanceService` | Populates demo vehicles in H2/dev | Member 2 (Fleet & Assets) |

---

### 2.4 Module 3: Commercial Drivers & Trip Operations (`com.fleetx.drivers`)

| File Path | Primary Classes / Methods | What It Actually Implements | Callers / Consumers | Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/drivers/entity/Driver.java` | `Driver` | JPA Entity: `user_id`, `license_number`, `license_category`, `license_expiry_date`, `medical_cert_expiry_date`, `availability_status`, `rating_score` | `DriverRepository`, `DriverService` | Jakarta Persistence, `DriverAvailability` | Maps to `drivers` table | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/entity/Trip.java` | `Trip` | JPA Entity: `trip_code`, `vehicle_id`, `driver_id`, `origin_address`, `destination_address`, `status`, scheduled/actual timestamps, `distance_km` | `TripRepository`, `TripService` | Jakarta Persistence, `TripStatus` | Maps to `trips` table | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/entity/TripStatusHistory.java` | `TripStatusHistory` | JPA Entity: `trip_id`, `from_status`, `to_status`, `remarks`, `updated_by`, `created_at` | `TripStatusHistoryRepository`, `TripService` | Jakarta Persistence | Maps to `trip_status_history` table | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/entity/DriverPerformanceMetrics.java` | `DriverPerformanceMetrics` | JPA Entity: `driver_id`, `total_trips_completed`, `on_time_deliveries`, `safety_violation_count`, `rating_score` | `DriverPerformanceMetricsRepository`, `DriverPerformanceService` | Jakarta Persistence | Maps to `driver_performance_metrics` table | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/service/DriverService.java` | `createDriver()`, `updateDriver()`, `searchDrivers()`, `isDriverEligibleForTrip()` | Driver registry management, licence expiry verification, and implementation of `DriverLookupService` | `DriverController`, `TripService` | `DriverRepository`, `UserRepository` | Queries and mutates `drivers` table | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/service/TripService.java` | `createTrip()`, `updateTripStatus()`, `searchTrips()`, `isTripActive()` | Trip dispatching with vehicle/driver availability validation, FSM enforcement (PLANNED → ASSIGNED → IN_PROGRESS → COMPLETED/CANCELLED), history recording, and implementation of `TripLookupService` | `TripController`, `ShipmentService` | `TripRepository`, `TripStatusHistoryRepository`, `VehicleLookupService`, `DriverLookupService`, `AlertPublisher` | Mutates `trips`, `trip_status_history` | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/service/DriverPerformanceService.java` | `getPerformanceMetrics()`, `recordTripCompletion()` | Driver performance tracking and scoring calculation | `DriverController`, `TripService` | `DriverPerformanceMetricsRepository` | Queries/updates `driver_performance_metrics` | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/controller/DriverController.java` | `getDrivers()`, `getDriverById()`, `createDriver()`, `updateDriver()`, `updateAvailability()`, `getPerformance()` | REST Controller: `GET/POST /api/v1/drivers`, profile, availability toggle, and performance metrics | Frontend `driverApi.ts` | `DriverService`, `DriverPerformanceService` | Exposes driver operations | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/controller/TripController.java` | `getTrips()`, `getTripById()`, `createTrip()`, `updateTripStatus()`, `getTripHistory()` | REST Controller: `GET/POST /api/v1/trips`, status transitions, and audit history | Frontend `driverApi.ts` | `TripService` | Exposes trip operations | Member 3 (Drivers & Trips) |
| `backend/src/main/java/com/fleetx/drivers/service/DemoDriverTripSeeder.java` | `run()` | Development-only seeder creating 2 commercial drivers, 1 active IN_PROGRESS trip, and 1 PLANNED trip | Spring Boot lifecycle (`dev` profile only) | `DriverService`, `TripService`, `UserRepository` | Populates demo drivers and trips | Member 3 (Drivers & Trips) |

---

### 2.5 Module 4: Shipment Manifests & Route Logistics (`com.fleetx.shipments`)

| File Path | Primary Classes / Methods | What It Actually Implements | Callers / Consumers | Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/shipments/entity/Shipment.java` | `Shipment` | JPA Entity: `tracking_number`, `sender_name`, `receiver_name`, `origin_address`, `destination_address`, `weight_kg`, `volume_m3`, `cargo_description`, `status`, `assigned_trip_id`, `estimated_distance_km`, `estimated_duration_minutes` | `ShipmentRepository`, `ShipmentService` | Jakarta Persistence, `ShipmentStatus` | Maps to `shipments` table | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/entity/ShipmentWaypoint.java` | `ShipmentWaypoint` | JPA Entity: `shipment_id`, `sequence_order`, `location_name`, `latitude`, `longitude`, `stop_type`, `estimated_arrival_time`, `actual_arrival_time`, `completed` | `ShipmentWaypointRepository`, `ShipmentService` | Jakarta Persistence, `StopType` | Maps to `shipment_waypoints` table | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/entity/ShipmentTimelineEvent.java` | `ShipmentTimelineEvent` | JPA Entity: `shipment_id`, `status`, `location_summary`, `remarks`, `occurred_at` | `ShipmentTimelineEventRepository`, `ShipmentService` | Jakarta Persistence | Maps to `shipment_timeline_events` table | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/service/RouteCalculationService.java` | `calculateHaversineDistanceKm()`, `calculateRoutePlan()` | Great-circle Haversine formula calculation (`R = 6371.0 km`) with numeric atan2 formulation, legs calculation, and speed/stop-based ETA estimation | `ShipmentService` | Math library | Pure algorithmic computation (no DB) | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/service/ShipmentService.java` | `createShipment()`, `assignShipmentToTrip()`, `saveWaypointsAndCalculateRoute()`, `updateShipmentStatus()`, `completeDelivery()`, `detectAndFlagDelayedShipments()` | Shipment manifest creation, trip association via `TripLookupService`, waypoint sequence persistence, FSM enforcement (ORDERED → DISPATCHED → IN_TRANSIT → DELIVERED), and timeline logging | `ShipmentController`, `TrackingService` | `ShipmentRepository`, `ShipmentWaypointRepository`, `ShipmentTimelineEventRepository`, `RouteCalculationService`, `TripLookupService`, `AlertPublisher` | Mutates `shipments`, `shipment_waypoints`, `shipment_timeline_events` | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/controller/ShipmentController.java` | `getShipments()`, `getShipment()`, `createShipment()`, `updateShipment()`, `assignTrip()`, `updateStatus()`, `completeDelivery()`, `saveWaypoints()`, `getWaypoints()`, `getTimeline()`, `checkDelayedShipments()` | REST Controller: `GET/POST /api/v1/shipments`, trip assignment, status transitions, delivery confirmation, waypoints, timeline, and delay checking | Frontend `shipmentApi.ts` | `ShipmentService` | Exposes shipment logistics endpoints | Member 4 (Shipments & Routes) |
| `backend/src/main/java/com/fleetx/shipments/service/DemoShipmentSeeder.java` | `run()` | Development-only seeder creating 2 demo shipments (SHP-DEMO-2026-001 with 3 route waypoints and SHP-DEMO-2026-002) | Spring Boot lifecycle (`dev` profile only) | `ShipmentService`, `TripRepository` | Populates demo shipments in H2/dev | Member 4 (Shipments & Routes) |

---

### 2.6 Module 5: GPS Simulation, Geofences, Alerts & Control Tower (`com.fleetx.tracking`)

| File Path | Primary Classes / Methods | What It Actually Implements | Callers / Consumers | Dependencies | DB / API Interaction | Module Ownership |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `backend/src/main/java/com/fleetx/tracking/entity/TrackingLog.java` | `TrackingLog` | JPA Entity: `vehicle_id`, `trip_id`, `latitude`, `longitude`, `speed_kmh`, `heading_degrees`, `is_simulated` (always true), `recorded_at` | `TrackingRepository`, `TrackingService` | Jakarta Persistence | Maps to `telemetry_logs` table | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/entity/Geofence.java` | `Geofence` | JPA Entity: `name`, `type` (CIRCLE/POLYGON), `center_lat`, `center_lon`, `radius_meters`, `polygon_coordinates_json`, `active` | `GeofenceRepository`, `TrackingService` | Jakarta Persistence | Maps to `geofences` table | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/entity/GeofenceVehicleState.java` | `GeofenceVehicleState` | JPA Entity: `geofence_id`, `vehicle_id`, `is_inside`, `last_transition_at` for deduplicating geofence enter/exit alerts | `GeofenceVehicleStateRepository`, `TrackingService` | Jakarta Persistence | Maps to `geofence_vehicle_states` table | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/entity/Alert.java` | `Alert` | JPA Entity: `alert_type`, `severity`, `entity_type`, `entity_id`, `details`, `acknowledged`, `created_at` | `AlertRepository`, `TrackingService` | Jakarta Persistence | Maps to `alerts` table | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/entity/ControlTowerKpi.java` | `ControlTowerKpi` | JPA Entity: `metric_key`, `metric_value`, `category`, `updated_at` | `ControlTowerKpiRepository` | Jakarta Persistence | Maps to `control_tower_kpis` table | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/service/GeofenceAndTelemetryMathService.java` | `isPointInCircle()`, `isPointInPolygon()`, `interpolatePosition()`, `calculateBearing()` | Ray-casting Point-in-Polygon (PIP) algorithm, circular Haversine radius test, and linear waypoint segment interpolation | `TrackingService` | Pure mathematical geometry | Algorithmic breach evaluation | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/service/TrackingService.java` | `startSimulation()`, `pauseSimulation()`, `resumeSimulation()`, `stopSimulation()`, `stepSimulation()`, `getControlTowerSummary()`, `createGeofence()`, `publishAlert()`, `exportCsvReport()` | GPS simulation thread pool scheduling, STOMP broadcasting to `/topic/vehicles/{id}` and `/topic/fleet-locations`, geofence transition evaluation, alert deduplication, and RFC 4180 CSV generation | `TrackingController`, cross-module `AlertPublisher` | `TrackingRepository`, `GeofenceRepository`, `AlertRepository`, `SimpMessagingTemplate`, `VehicleLookupService` | Writes `telemetry_logs`, `alerts`, `geofence_vehicle_states` | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/controller/TrackingController.java` | `getControlTowerSummary()`, `getSimulatorSessions()`, `startSimulation()`, `pauseSimulation()`, `resumeSimulation()`, `stopSimulation()`, `stepSimulation()`, `createGeofence()`, `listGeofences()`, `listAlerts()`, `acknowledgeAlert()`, `exportCsvReport()` | REST Controller: `GET/POST /api/v1/tracking/...` covering simulator controls, geofences, alerts, and CSV export | Frontend `trackingApi.ts` | `TrackingService` | Exposes control tower operations | Member 5 (Tracking & Tower) |
| `backend/src/main/java/com/fleetx/tracking/service/DemoTrackingSeeder.java` | `run()` | Development-only seeder creating 2 geofences (depot circle & port polygon) and initializing a demo GPS session | Spring Boot lifecycle (`dev` profile only) | `TrackingService`, `VehicleRepository` | Populates demo geofences in H2/dev | Member 5 (Tracking & Tower) |

---

## 3. Frontend First-Party File Inventory

### 3.1 App Shell, Context & Routing (`frontend/src/`)

| File Path | Component / Function Name | What It Actually Implements | Callers / Rendered By | Dependencies | Backend API Called |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `frontend/src/main.tsx` | Root bootstrap | Mounts React 18 DOM tree into `#root`, wraps application with `BrowserRouter` and `AuthProvider` | HTML entry (`index.html`) | React DOM, `App` | None |
| `frontend/src/app/App.tsx` | `App` | Central routing wrapper rendering the active router element | `main.tsx` | `routes.tsx`, `AuthContext` | None |
| `frontend/src/app/Layout.tsx` | `Layout` | Main dashboard frame: TopNav bar, Collapsible Sidebar, Breadcrumbs, and `<Outlet />` for child routes | `routes.tsx` | `TopNav`, `Sidebar`, React Router | None |
| `frontend/src/app/routes.tsx` | `router` | `createBrowserRouter` defining public route (`/login`) and authenticated child routes (`/`, `/fleet/*`, `/trips/*`, `/shipments/*`, `/admin/*`) guarded by `ProtectedRoute` | `App.tsx` | Feature route files | None |
| `frontend/src/shared/context/AuthContext.tsx` | `AuthProvider`, `useAuth` | Global authentication state: stores `user`, `loading`, `error`, initializes session on mount via `GET /api/v1/auth/me`, provides `login()` and `logout()` | All protected pages and UI | `authApi.ts`, Axios client | `GET /api/v1/auth/me`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` |
| `frontend/src/shared/api/client.ts` | `apiClient` | Singleton Axios instance configured with `baseURL: ''`, `withCredentials: true`, and CSRF request interceptor reading `XSRF-TOKEN` cookie | All feature API modules | Axios | Attaches `X-XSRF-TOKEN` header to unsafe requests |
| `frontend/src/shared/components/ProtectedRoute.tsx` | `ProtectedRoute` | Route guard component: checks `isAuthenticated`; if false redirects to `/login`. Checks `requiredRoles`; if role missing renders `UnauthorizedState` | `routes.tsx` | `useAuth`, `UnauthorizedState`, `LoadingState` | None (Client-side routing guard) |

### 3.2 Feature Pages & Modals

| File Path | Component Name | What It Actually Implements | Route / Parent Component | API Methods Called | State / Logic Implemented |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `frontend/src/features/auth/pages/LoginPage.tsx` | `LoginPage` | Authentication screen: email & password inputs, demo credentials auto-fill quick buttons, CSRF bootstrap, and error display | `/login` | `authApi.getCsrfToken()`, `authApi.login()` | Full login handshake with redirect to `/` |
| `frontend/src/features/auth/pages/UsersPage.tsx` | `UsersPage` | Admin user directory table with search, role filters, status badges, pagination, and trigger buttons for creation/edit/deactivation/password-reset modals | `/admin/users` | `authApi.getUsers()`, `authApi.getAuditLogs()` | Dynamic server-side pagination and filter state |
| `frontend/src/features/auth/pages/ProfilePage.tsx` | `ProfilePage` | User profile overview: displays current user email, full name, assigned roles, and session status | `/profile` | `useAuth().user` | Reads from `AuthContext` |
| `frontend/src/features/auth/components/CreateUserModal.tsx` | `CreateUserModal` | Admin dialog to provision a new user: email, full name, password, and canonical role selection | `UsersPage` | `authApi.createUser()` | Form validation, role picker, API mutation |
| `frontend/src/features/auth/components/EditUserModal.tsx` | `EditUserModal` | Admin dialog to edit full name and change assigned role of existing user | `UsersPage` | `authApi.updateUser()` | Form validation, role update |
| `frontend/src/features/auth/components/DeactivateConfirmModal.tsx` | `DeactivateConfirmModal` | Confirmation dialog to toggle user active status (`true`/`false`) | `UsersPage` | `authApi.updateUserStatus()` | Account deactivation/reactivation |
| `frontend/src/features/auth/components/PasswordResetModal.tsx` | `PasswordResetModal` | Admin dialog to override user password | `UsersPage` | `authApi.resetPassword()` | Minimum 8-char validation, password reset |
| `frontend/src/features/fleet/pages/VehiclesPage.tsx` | `VehiclesPage` | Fleet inventory table: license plate, VIN, make/model, status, payload capacity, odometer, and action buttons | `/fleet/vehicles` | `fleetApi.getVehicles()`, `fleetApi.getVehicle()` | Dynamic search, category/status filters, pagination |
| `frontend/src/features/fleet/pages/MaintenancePage.tsx` | `MaintenancePage` | Maintenance dashboard: active service warnings (WARNING/CRITICAL) and complete maintenance history logs table | `/fleet/maintenance` | `fleetApi.getActiveWarnings()`, `fleetApi.getMaintenanceRecords()` | Warning badge display and paged record view |
| `frontend/src/features/fleet/components/CreateVehicleModal.tsx` | `CreateVehicleModal` | Modal to register new vehicle: VIN (17-char pattern), plate, make, model, type, fuel, payload, volume, initial odometer | `VehiclesPage` | `fleetApi.createVehicle()` | Client-side pattern validation, vehicle registration |
| `frontend/src/features/fleet/components/LogFuelModal.tsx` | `LogFuelModal` | Modal to record fuel fill-up: litres, cost, odometer reading. Triggers automatic km/L calculation on backend | `VehiclesPage`, `VehicleDetailsModal` | `fleetApi.logFuel()` | Positive quantity validation, fuel logging |
| `frontend/src/features/fleet/components/ScheduleMaintenanceModal.tsx` | `ScheduleMaintenanceModal` | Modal to log maintenance service and set next service due distance (km) and due date | `VehiclesPage`, `VehicleDetailsModal` | `fleetApi.scheduleMaintenance()` | Service scheduling and threshold setup |
| `frontend/src/features/fleet/components/VehicleDetailsModal.tsx` | `VehicleDetailsModal` | Comprehensive vehicle view: specifications, recent fuel logs with computed efficiency, maintenance history, and quick actions | `VehiclesPage` | `fleetApi.getVehicle()`, `fleetApi.getFuelRecords()` | Multi-tab sub-resource view |
| `frontend/src/features/drivers/pages/DriversPage.tsx` | `DriversPage` | Commercial driver roster table: licence number, category, expiry status, medical certificate, availability, and rating | `/trips/drivers` | `driverApi.getDrivers()` | Search, availability filters, pagination |
| `frontend/src/features/drivers/pages/TripsPage.tsx` | `TripsPage` | Trip operations dispatch table: trip code, assigned vehicle/driver, origin/destination, scheduled times, and status | `/trips/dispatch` | `driverApi.getTrips()`, `driverApi.getTrip()` | Trip filtering, status lifecycle transition trigger |
| `frontend/src/features/drivers/components/CreateDriverModal.tsx` | `CreateDriverModal` | Modal to onboard commercial driver: associates with user account, captures licence number, category, and expiry dates | `DriversPage` | `driverApi.createDriver()` | Date validation, driver onboarding |
| `frontend/src/features/drivers/components/CreateTripModal.tsx` | `CreateTripModal` | Dispatch modal: selects available vehicle and eligible driver, enters origin/destination and schedule | `TripsPage` | `driverApi.createTrip()`, `driverApi.getEligibleVehicles()`, `driverApi.getEligibleDrivers()` | Resource availability validation, trip dispatch |
| `frontend/src/features/drivers/components/TripLifecycleModal.tsx` | `TripLifecycleModal` | Modal to advance trip status: start trip (records actual start time), complete trip (releases vehicle/driver), or cancel trip | `TripsPage` | `driverApi.updateTripStatus()` | FSM transition guard, remarks capture |
| `frontend/src/features/shipments/pages/ShipmentsPage.tsx` | `ShipmentsPage` | Cargo manifest table: tracking number, sender/receiver, weight, volume, cargo description, trip assignment, and delivery status | `/shipments/manifests` | `shipmentApi.getShipments()`, `shipmentApi.checkDelayed()` | Manifest search, delay inspection trigger |
| `frontend/src/features/shipments/pages/WaypointsPage.tsx` | `WaypointsPage` | Dedicated route planning view: selects shipment, lists ordered waypoints, shows calculated Haversine distance and estimated ETA | `/shipments/waypoints` | `shipmentApi.getShipments()`, `shipmentApi.getWaypoints()` | Waypoint inspection and route calculation |
| `frontend/src/features/shipments/components/CreateShipmentModal.tsx` | `CreateShipmentModal` | Modal to create cargo consignment: origin, destination, weight, volume, cargo description, and auto-generated tracking code | `ShipmentsPage` | `shipmentApi.createShipment()` | Cargo validation, manifest entry |
| `frontend/src/features/shipments/components/AssignTripModal.tsx` | `AssignTripModal` | Modal to link shipment to an authorized trip | `ShipmentsPage` | `shipmentApi.assignTrip()`, `driverApi.getTrips()` | Trip association |
| `frontend/src/features/shipments/components/RouteWaypointsModal.tsx` | `RouteWaypointsModal` | Modal to add, reorder, edit, and delete route waypoints (latitude, longitude, location name, stop type). Triggers Haversine calculation | `ShipmentsPage`, `WaypointsPage` | `shipmentApi.saveWaypoints()` | Dynamic waypoint list editing |
| `frontend/src/features/shipments/components/ShipmentDetailsModal.tsx` | `ShipmentDetailsModal` | Shipment inspector: manifest specifications, delivery timeline events, route waypoints, and delivery completion action | `ShipmentsPage` | `shipmentApi.getShipment()`, `shipmentApi.getTimeline()`, `shipmentApi.completeDelivery()` | Complete shipment lifecycle audit view |
| `frontend/src/features/tracking/pages/ControlTowerPage.tsx` | `ControlTowerPage` | Central operational dashboard: KPI stat cards, simulated GPS engine controls (Start/Pause/Resume/Stop/Step), 2D/3D toggle, active alerts feed with acknowledge button, and geofence list | `/` (Control Tower) | `trackingApi.getControlTowerSummary()`, `getSimulatorSessions()`, `startSimulation()`, `getAlerts()`, `acknowledgeAlert()`, `getGeofences()` | 3-second polling timer for live telemetry synchronization |
| `frontend/src/features/tracking/pages/ReportsPage.tsx` | `ReportsPage` | Operational audit and reporting portal: filters by domain (Trips, Fleet, Maintenance, Alerts, Drivers, Shipments), date range, and status. Provides live table view and RFC 4180 CSV export download | `/tracking/reports` | `trackingApi.getReportSummary()`, `trackingApi.downloadReportCsv()` | Real-time CSV generation and browser file download |
| `frontend/src/features/tracking/components/LeafletFleetMap.tsx` | `LeafletFleetMap` | 2D map component using Leaflet 1.9 and OpenStreetMap tiles: renders vehicle markers, heading rotation, route polyline breadcrumbs, and geofence boundaries | `ControlTowerPage` | Leaflet API, OpenStreetMap tile server | Visualizes geographic coordinates and boundaries |
| `frontend/src/features/tracking/components/ThreeFleetScene.tsx` | `ThreeFleetScene` | 3D stylized procedural canvas using Three.js / React Three Fiber: renders stylized procedural 3D truck models synchronized with telemetry heading, speed, and status | `ControlTowerPage` | Three.js, `@react-three/fiber`, `@react-three/drei` | Stylized procedural visual scene (not a geographic digital twin) |
| `frontend/src/features/tracking/components/CreateGeofenceModal.tsx` | `CreateGeofenceModal` | Modal to configure geofence: name, type (CIRCLE with center lat/lon and radius, or POLYGON with JSON coordinate array) | `ControlTowerPage` | `trackingApi.createGeofence()` | Geospatial parameter validation |
