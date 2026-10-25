# FleetX Phase 11C — Controlled Production Release & Deployment Readiness Report

**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Repository:** `https://github.com/ziaul1608/fleetX`  
**Feature Branch:** `feature/multitenant-email-otp`  
**Starting Commit:** `5912731`  
**Target Live Environment:** Render Free Web Service (`https://fleetx-h9wf.onrender.com`)  
**Production Database:** Aiven MySQL 8.4 (Oregon)  
**Evaluation Role:** Senior SaaS Security Engineer, Spring Boot Architect, MySQL Migration Engineer & QA Lead  
**Date:** October 25, 2026  
**Release Verdict:** **BLOCKED** *(Code, migrations, and isolation gates PASSED; blocked exclusively pending manual configuration of real SMTP credentials and OTP HMAC secret in the Render Dashboard before production deployment)*

---

## 1. Executive Summary & Mandatory Safety Rules Compliance

FleetX Phase 11C evaluates the production readiness of the multi-tenant SaaS architecture, Flyway schema migrations (V600 and V601), and passwordless Email OTP authentication system.

### Mandatory Safety Rules Compliance
- **Branch Discipline:** Starting commit verified as `5912731`. All work performed strictly on `feature/multitenant-email-otp`.
- **Zero Secrets Disclosed:** No passwords, plaintext OTPs, SMTP passwords, session cookies, database credentials, or HMAC keys are committed, logged, or exposed in this report.
- **Production Isolation:** The live production service (`https://fleetx-h9wf.onrender.com`) and cloud database (Aiven MySQL 8.4) were **not touched or modified**. All schema rehearsals and integration tests executed against an isolated local MySQL 8.4 Docker container (`fleetx-mysql`).
- **₹0 / $0 Cost Policy:** No paid services, tier upgrades, or billable cloud resources were provisioned.
- **Fail-Closed Safety:** No authentication, tenant checks, foreign-key constraints, or tests were disabled or bypassed.
- **Controlled Release Policy:** In accordance with Gate 5 and Gate 8 rules, deployment to production remains held until production SMTP credentials and HMAC keys are configured in the hosting environment.

---

## 2. Gate 1 — Verification of Phase 11B Implementation

The actual code on branch `feature/multitenant-email-otp` was inspected and verified:

| Inspection Item | Code Verification Details | Status |
|-----------------|---------------------------|--------|
| **18 Tenant-Aware Entities** | All 18 entities declare `@Column(name = "tenant_id", nullable = false, updatable = false)` and enforce fail-closed `@PrePersist` and `@PreUpdate` lifecycle checks via `TenantSecurityContext`. | **VERIFIED** |
| **No Silent Tenant 1 Fallbacks** | `grep -rn "orElse(1L)" backend/src/main/java` and `grep -rn "return 1L;"` return 0 matches. Missing context throws HTTP 400 (`TENANT_CONTEXT_REQUIRED`) or HTTP 401 (`UNAUTHORIZED`). | **VERIFIED** |
| **Tenant Reassignment Blocked** | Reassigning an existing entity to another tenant throws HTTP 403 (`TENANT_ACCESS_DENIED`) in `@PreUpdate`. | **VERIFIED** |
| **Transactional Signup** | `AuthService.verifySignupOtp` creates both the `Tenant` record and primary administrator `User` within a single `@Transactional` boundary, assigning the user strictly to the newly generated tenant ID. | **VERIFIED** |
| **Tenant-Local Staff & Roles** | `AdminUserService` scopes user listing, creation, and deactivation strictly to `getCurrentTenantId()`. | **VERIFIED** |
| **Password Auth Retired** | `AuthService.login`, `AuthService.changePassword`, and `AdminUserService.resetUserPassword` reject requests with HTTP 400 and error code `PASSWORD_AUTH_RETIRED`. | **VERIFIED** |
| **OTP Security Guarantees** | `OtpService` enforces purpose binding (`LOGIN` / `SIGNUP`), 5-minute expiry, single-use consumption (`OTP_ALREADY_USED`), 5 max attempts, 60s cooldown, sliding-window IP/email rate limits, and constant-time HMAC comparison. Plaintext OTPs are never stored. | **VERIFIED** |
| **Production SMTP Guard** | `SmtpEmailService` is scoped to `@Profile("!test")`. If mail sender is unconfigured or transport fails, it throws HTTP 503 (`EMAIL_SERVICE_UNAVAILABLE` / `EMAIL_DISPATCH_FAILED`). Never falls back to mock mail in production. | **VERIFIED** |
| **Bootstrap Admin Guard** | `AdminBootstrapRunner` checks `@Value("${fleetx.bootstrap.admin.enabled:false}")`. In `application-prod.yml`, it is hardcoded to `enabled: false`. | **VERIFIED** |

---

## 3. Gate 2 — Proof Demo Seeders Cannot Pollute Production

### Configuration Hardening
In `backend/src/main/resources/application-prod.yml`:
```yaml
fleetx:
  bootstrap:
    admin:
      enabled: false
      password-override: false
  demo:
    seed:
      enabled: false
```
All six seeders/runners evaluate these explicit flags:
- `AdminBootstrapRunner`: `if (!bootstrapEnabled) return;`
- `DemoIdentitySeeder`: `if (!demoSeedEnabled) return;`
- `DemoFleetSeeder`: `if (!demoSeedEnabled) return;`
- `DemoDriverTripSeeder`: `if (!demoSeedEnabled) return;`
- `DemoShipmentSeeder`: `if (!demoSeedEnabled) return;`
- `DemoTrackingSeeder`: `if (!demoSeedEnabled) return;`

### Automated Verification Test
A dedicated test suite was implemented in `backend/src/test/java/com/fleetx/auth/ProductionProfileSeederSafetyTest.java`:
- Executes under `@ActiveProfiles("prod")`.
- Asserts `fleetx.demo.seed.enabled == false` and `fleetx.bootstrap.admin.enabled == false`.
- Verifies database counts:
  - Demo users (`admin@fleetx.local`, `fleet.manager@fleetx.local`, etc.): **0 present**
  - Demo vehicles (`MH-12-FX-1001`, `MH-12-FX-1002`): **0 present (vehicle count: 0)**
  - Demo drivers & trips (`DL-MH-2024-00101`, `TRIP-2026-0001`): **0 present (trip count: 0)**
  - Demo shipments (`SHP-2026-0001`): **0 present (shipment count: 0)**
  - Demo geofences & telemetry: **0 present**
- **Test Result:** 2 tests run, 2 passed, 0 failures, 0 errors.

---

## 4. Gate 3 — Real MySQL 8.4 Integration Tests & Flyway Migration Rehearsal

### Test Execution on Real MySQL 8.4
The four previously skipped tests in `MySQL8PersistenceIntegrationTest` were executed against an isolated, disposable local container (`fleetx-mysql`, image `mysql:8.4.11-1.el9`, port `3307`, database `fleetx_qa`).

```
[INFO] Running com.fleetx.integration.MySQL8PersistenceIntegrationTest
Database: jdbc:mysql://127.0.0.1:3307/fleetx_qa (MySQL 8.4)
Current version of schema `fleetx_qa`: 501
Migrating schema `fleetx_qa` to version "600 - multitenant saas and otp schema"
Migrating schema `fleetx_qa` to version "601 - harden tenant isolation and foreign keys"
Successfully applied 2 migrations to schema `fleetx_qa`, now at version v601 (execution time 00:01.769s)
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.fleetx.integration.MySQL8PersistenceIntegrationTest
```

### Migration & Schema Verification
1. **Pre-Existing Data Preservation:**
   - Pre-migration rows in `fleetx_qa` (1 legacy user `admin@fleetx.local`, 3 legacy vehicles `MH-12-QA-9999`, `KA-05-IT-81654`, `KA-05-IT-23816`, 1 legacy driver) were inspected before running V600.
   - After V600, all pre-existing rows were preserved and backfilled to `tenant_id = 1`.
   - Seeded legacy tenant: `id = 1`, `tenant_code = 'TENANT-LEGACY-DEFAULT'`, `company_name = 'FleetX Global Logistics (Legacy)'`.
2. **Removal of Unintended Column Defaults:**
   - Queried `INFORMATION_SCHEMA.COLUMNS`: `COLUMN_DEFAULT` is `NULL` across all tables.
   - `IS_NULLABLE` is `NO` (`NOT NULL` constraint active).
3. **Foreign Key Protection (`ON DELETE RESTRICT`):**
   - Queried `INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS`: All 18 foreign keys referencing `tenants(id)` enforce `DELETE_RULE = 'RESTRICT'`. Deleting a tenant cannot cascade-delete operational records.
4. **Flyway Schema History:**
   - All 12 migrations (V100 through V601) recorded with `success = 1`.
5. **Application Restart Verification:**
   - Application restarted against `fleetx_qa`. Flyway reported:
     `Schema fleetx_qa is up to date. No migration necessary.`
   - 4 tests passed without migration duplication or schema corruption.

---

## 5. Gate 4 — Multi-Tenant Isolation Verification

Multi-tenant isolation was validated using automated tests spanning all five modules:

1. **Two Independently Registered Tenants:**
   - Tenant Alpha (`owner@alpha-logistics.test`, company "Alpha Global Freight")
   - Tenant Beta (`owner@beta-express.test`, company "Beta Express Line")
2. **Cross-Tenant Visibility:**
   - Tenant Beta querying `/api/v1/fleet/vehicles`, `/api/v1/trips/drivers`, `/api/v1/trips`, `/api/v1/shipments`, `/api/v1/tracking/geofences` returned `totalElements: 0` / empty content.
   - Direct lookup by ID for Tenant Alpha resources from Tenant Beta returned HTTP 404 (`ResourceNotFoundException`).
3. **Cross-Tenant Mutation:**
   - Tenant Beta attempting to create a trip using Tenant Alpha's vehicle and driver was rejected with HTTP 400 (`Bad Request`).
4. **Control Tower & Reporting:**
   - Control Tower KPI summary for Tenant Beta showed 0 vehicles, 0 ongoing trips, 0 shipments.
   - CSV export (`/api/v1/tracking/reports/export-csv`) for Tenant Beta returned only the CSV header row with 0 data rows.
5. **WebSocket / STOMP Channel Hardening:**
   - In `backend/src/main/java/com/fleetx/shared/config/WebSocketConfig.java`, added a `ChannelInterceptor` on `configureClientInboundChannel`:
     - Inspects `SUBSCRIBE` commands to `/topic/tenants/{tenantId}/**`.
     - Rejects subscriptions if the authenticated principal's tenant ID does not match `{tenantId}`.
     - Restricts legacy unpartitioned topics (`/topic/alerts`, `/topic/fleet-locations`) to tenant 1 only.
   - In `backend/src/main/java/com/fleetx/tracking/service/TrackingService.java`:
     - Telemetry pings broadcast to `/topic/tenants/{tenantId}/fleet-locations` and `/topic/tenants/{tenantId}/vehicles/{vehicleId}`.
     - Alerts broadcast to `/topic/tenants/{tenantId}/alerts`.
6. **Background Workers & Unauthenticated Persistence:**
   - Unmanaged background threads writing entities fail closed (`TENANT_CONTEXT_REQUIRED`, HTTP 400).
   - Workers wrapped with `TenantSecurityContext.runWithTenant` / `executeWithTenant` succeed.

---

## 6. Gate 5 — Production OTP & Email Configuration

### Required Render Environment Variables
The application requires the following environment variables in production (`application-prod.yml` and `render.yaml`):

| Variable Name | Project Mapping | Purpose | Production Requirement |
|---------------|-----------------|---------|------------------------|
| `SPRING_MAIL_HOST` | `spring.mail.host` | SMTP Server Host (e.g., SendGrid, Mailgun, AWS SES, Gmail) | Required for OTP delivery |
| `SPRING_MAIL_PORT` | `spring.mail.port` | SMTP Server Port (e.g., 587 for TLS, 465 for SSL) | Required (default: 587) |
| `SPRING_MAIL_USERNAME` | `spring.mail.username` | SMTP Authentication Username / API Key identifier | Required for OTP delivery |
| `SPRING_MAIL_PASSWORD` | `spring.mail.password` | SMTP Authentication Password / API Key | Required for OTP delivery |
| `FLEETX_MAIL_FROM` | `fleetx.mail.from` | Verified Sender Email Address (e.g., `noreply@yourdomain.com`) | Required |
| `FLEETX_OTP_HMAC_SECRET` | `fleetx.auth.otp.hmac-secret` | Cryptographic secret for OTP HMAC-SHA256 digestion | Required (32+ char random string) |

### Blueprint Update
`render.yaml` was updated to explicitly declare `FLEETX_OTP_HMAC_SECRET` with `sync: false`, ensuring secrets are managed strictly through the Render Dashboard UI without committing values to Git.

### Release Gate Status: BLOCKED
Because real SMTP delivery is mandatory for passwordless multi-tenant login and registration in production, and these credentials must be supplied via the Render Dashboard by the infrastructure owner:
- **Status:** **BLOCKED** from deploying to Render until the owner configures the SMTP variables and HMAC secret.
- **Action Required:** The user must enter these variables in Render Dashboard -> `fleetx` service -> **Environment** tab.

---

## 7. Gate 6 — Production Backup & Migration Safety Procedure

### 7.1 Logical Backup Procedure (Aiven MySQL 8.4)
Before deploying the multi-tenant release to production:
1. **Aiven Platform Snapshot:**
   - Aiven provides automatic continuous backups with point-in-time recovery (PITR).
   - In Aiven Console: Verify the latest backup timestamp on service `fleetx-mysql`.
2. **On-Demand Logical mysqldump (Off-Git):**
   ```bash
   mysqldump -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p \
     --single-transaction --quick --routines --triggers --set-gtid-purged=OFF \
     defaultdb > /secure/backup/fleetx_pre_phase11_$(date +%Y%m%d_%H%M%S).sql
   chmod 600 /secure/backup/fleetx_pre_phase11_*.sql
   ```
   *(Ensure backup files reside outside repository directories and are never committed).*

### 7.2 Database Migration Impact Analysis
- **Migration V600:**
  - Adds `tenants` table and `otp_challenges` table.
  - Seeds tenant `id = 1` (`TENANT-LEGACY-DEFAULT`).
  - Adds `tenant_id` column to 18 tables and executes `UPDATE <table> SET tenant_id = 1 WHERE tenant_id IS NULL`.
  - Non-destructive to existing rows; all existing users, vehicles, and shipments remain accessible under tenant 1.
- **Migration V601:**
  - Drops cascade foreign keys and adds `ON DELETE RESTRICT`.
  - Drops default value from `tenant_id` columns.
  - Enforces explicit tenant attribution for all future inserts.

### 7.3 Recovery & Rollback Procedure
1. **Application Rollback:**
   - In Render Dashboard: Service can be redeployed to previous commit `69d01b3` (Phase 10C).
2. **Database Rollback:**
   - Note: Rolling back the application does **not** revert Flyway migrations.
   - If migration fails during deployment:
     1. Inspect Render logs for the failing SQL statement.
     2. Connect to Aiven MySQL using administrative credentials.
     3. Remove failed migration entry from `flyway_schema_history`:
        ```sql
        DELETE FROM flyway_schema_history WHERE version IN ('600', '601') AND success = 0;
        ```
     4. If necessary, restore database state from pre-deployment logical dump.

---

## 8. Gate 7 — Build & Regression Verification

### 8.1 Backend Test Execution Summary

```
[INFO] Results:
[INFO] Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| Test Suite / Area | Executed | Passed | Failed | Errors | Skipped |
|-------------------|----------|--------|--------|--------|---------|
| `TenantFailClosedAttributionTest` | 7 | 7 | 0 | 0 | 0 |
| `ProductionProfileSeederSafetyTest` | 2 | 2 | 0 | 0 | 0 |
| `MySQL8PersistenceIntegrationTest` (Real MySQL 8.4) | 4 | 4 | 0 | 0 | 0 |
| `MultiTenantIsolationIntegrationTest` | 1 | 1 | 0 | 0 | 0 |
| `AdminCrossTenantStaffIsolationTest` | 2 | 2 | 0 | 0 | 0 |
| `AuthLifecycleTest` | 7 | 7 | 0 | 0 | 0 |
| `OtpLifecycleTest` | 13 | 13 | 0 | 0 | 0 |
| `OtpSecurityHardeningTest` | 7 | 7 | 0 | 0 | 0 |
| `SmtpEmailServiceFailClosedTest` | 3 | 3 | 0 | 0 | 0 |
| `VehicleOperationsTest` | 9 | 9 | 0 | 0 | 0 |
| `FuelAndMaintenanceTest` | 5 | 5 | 0 | 0 | 0 |
| `DriverOperationsTest` | 5 | 5 | 0 | 0 | 0 |
| `TripLifecycleOperationsTest` | 9 | 9 | 0 | 0 | 0 |
| `ShipmentOperationsTest` | 7 | 7 | 0 | 0 | 0 |
| `RouteAndDelayedShipmentTest` | 7 | 7 | 0 | 0 | 0 |
| `SimulationAndGeofenceTest` | 5 | 5 | 0 | 0 | 0 |
| `ControlTowerAlertAndReportTest` | 5 | 5 | 0 | 0 | 0 |
| `SecurityAccessTest` | 7 | 7 | 0 | 0 | 0 |
| Other Unit & Integration Tests | 23 | 23 | 0 | 0 | 0 |
| **Total Backend Tests** | **121** | **121** | **0** | **0** | **0** |

### 8.2 Frontend Test Execution Summary

```
Test Files  8 passed (8)
     Tests  49 passed (49)
  Duration  1.52s
```

| Test File | Tests Passed | Status |
|-----------|--------------|--------|
| `App.test.tsx` | 3 | PASSED |
| `AuthComponents.test.tsx` | 15 | PASSED |
| `DriverComponents.test.tsx` | 6 | PASSED |
| `FleetComponents.test.tsx` | 5 | PASSED |
| `IntegrationNavigationAndAuth.test.tsx` | 6 | PASSED |
| `MultiTenantLandingAndOtp.test.tsx` | 5 | PASSED |
| `ShipmentComponents.test.tsx` | 5 | PASSED |
| `TrackingComponents.test.tsx` | 4 | PASSED |
| **Total Frontend Tests** | **49** | **ALL PASSED** |

### 8.3 Frontend Production Build
```
vite v5.4.21 building for production...
✓ 1727 modules transformed.
dist/index.html                            0.88 kB
dist/assets/index-VwbqbW_I.css           236.36 kB
dist/assets/index-CzcDYK00.js            100.70 kB
dist/assets/vendor-leaflet-CcXmx2OD.js   154.79 kB
dist/assets/vendor-react-BKN15k-s.js     206.36 kB
dist/assets/vendor-three-D-W-wMqn.js     467.82 kB
✓ built in 1.40s
```

---

## 9. Gate 8 — Release Decision

### Explicit Verdict: **BLOCKED**

### Verdict Rationale:
1. **Code & Architecture Gates:** **ALL PASSED (100%)**.
   - Fail-closed tenant isolation verified.
   - Demo seeders proven completely disabled in production profile.
   - Real MySQL 8.4 Flyway migration rehearsal passed with zero errors, zero repeats, and pre-existing data backfilled to legacy tenant 1.
   - Multi-tenant isolation verified across all five modules and WebSocket STOMP channels.
   - 121/121 backend tests and 49/49 frontend tests passing.
2. **Blocking Release Gate:** **Gate 5 (Production Credentials Configuration)**.
   - Per mandatory gate instruction: *"If a required SMTP credential or HMAC key is missing, mark the release BLOCKED. Do not merge into main, push release commits or trigger a production deployment until all mandatory gates pass."*
   - Because FleetX password authentication is retired, the application requires valid SMTP credentials (`SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) and `FLEETX_OTP_HMAC_SECRET` in Render to allow users to register and log in.

---

## 10. Required Next Actions to Unblock Production Deployment

To unblock deployment and execute the Phase 11C production release:

1. **Step 1: Configure Render Environment Variables**
   Open the Render Dashboard -> `fleetx` service -> **Environment** tab, and enter:
   - `SPRING_MAIL_HOST`: Your SMTP server hostname (e.g. `smtp.sendgrid.net`, `smtp.mailgun.org`, etc.)
   - `SPRING_MAIL_PORT`: `587`
   - `SPRING_MAIL_USERNAME`: Your SMTP username or API key
   - `SPRING_MAIL_PASSWORD`: Your SMTP password or API secret
   - `FLEETX_MAIL_FROM`: Your verified sending address (e.g. `noreply@yourdomain.com`)
   - `FLEETX_OTP_HMAC_SECRET`: A secure, randomly generated 32+ character HMAC key (e.g. generated via `openssl rand -hex 32`)
2. **Step 2: Trigger Controlled Merge to Main**
   Once credentials are saved in Render:
   - Merge `feature/multitenant-email-otp` into `main`.
   - Push to `origin/main` to trigger the Render Docker auto-build.
3. **Step 3: Post-Deployment Smoke Verification**
   - Check Render build and startup logs to confirm Flyway applied V600 and V601 to Aiven MySQL.
   - Access `https://fleetx-h9wf.onrender.com/`.
   - Register a new warehouse owner via Email OTP signup.
   - Confirm receipt of OTP email, successful verification, and isolated dashboard access.
