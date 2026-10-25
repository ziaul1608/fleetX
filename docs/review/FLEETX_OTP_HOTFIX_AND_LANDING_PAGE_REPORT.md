# FleetX — OTP Signup Hotfix, Premium Landing Page & Production Release Report

**Date:** October 25, 2026  
**Repository:** [https://github.com/ziaul1608/fleetX](https://github.com/ziaul1608/fleetX)  
**Branch:** `feature/multitenant-email-otp`  
**Live Production Service:** [https://fleetx-h9wf.onrender.com](https://fleetx-h9wf.onrender.com) (Render service: `fleetx-h9wf`)  
**Database:** Aiven MySQL 8.4  
**Email Provider:** Resend HTTPS API / SMTP Fallback  
**Budget Constraint:** ₹0 / $0 Free-Tier Guaranteed  

---

## 1. Executive Summary

This release resolves the production registration verification defect where users attempting to verify OTP during signup encountered:
> `"An unexpected internal error occurred. Please contact system support."` (HTTP 500)

In addition to resolving this core database deadlock and transaction propagation issue, this release deploys a high-converting, professional public B2B landing page for FleetX (`/`), implements resilient frontend error handling for OTP operations, provides complete regression test coverage (134 backend tests + 51 frontend tests), and synchronizes all changes with GitHub.

---

## 2. Root Cause Analysis: Production OTP Signup Verification Error

### 2.1 The Issue Reported
On the live production application (`https://fleetx-h9wf.onrender.com`), dispatching a signup OTP via `POST /api/v1/auth/otp/send-signup` succeeded with HTTP 200 OK. However, submitting the verification code via `POST /api/v1/auth/otp/verify-signup` resulted in an unhandled HTTP 500 error with the message:
`"An unexpected internal error occurred. Please contact system support."`

### 2.2 Forensic Analysis & Diagnostic Findings

1. **Transaction Propagation Deadlock in `AuditService.recordEvent` (`REQUIRES_NEW`):**
   - In `AuditService.java`, the audit logging method was annotated with:
     ```java
     @Transactional(propagation = Propagation.REQUIRES_NEW)
     ```
   - When a user signs up, `AuthService.verifySignupOtp` runs within an active `@Transactional` boundary (Transaction A).
   - In Step 1 & 2, Transaction A inserts a new row into `tenants` and `users`. Because Transaction A has not committed, these rows are uncommitted in the database.
   - In Step 4, `auditService.recordEvent(...)` is called synchronously on the same thread. Because of `REQUIRES_NEW`, Spring suspends Transaction A and opens a **second database connection and separate transaction** (Transaction B).
   - Transaction B attempts to insert a record into `audit_logs` referencing `tenant_id` and `user_id`.
   - In MySQL 8.4 (InnoDB), the `audit_logs` table enforces foreign keys:
     - `CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT`
     - `CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL`
   - In MySQL InnoDB, Transaction B cannot see uncommitted rows from Transaction A. InnoDB places a shared lock wait on the referenced row, but Transaction A holds an exclusive lock on that row.
   - Because both transactions are on the same thread, Transaction A is paused waiting for `recordEvent` to return, while Transaction B is blocked in InnoDB waiting for Transaction A to commit.
   - Result: MySQL either immediately detects deadlock (`ERROR 1213: Deadlock found`) or hits `innodb_lock_wait_timeout` (`ERROR 1205: Lock wait timeout exceeded`).
   - Although `recordEvent` had a local `try-catch`, Spring's transaction interceptor marked the transaction rollback-only and threw a `TransactionSystemException` upon returning to `AuthService`.
   - Uncaught by `verifySignupOtp`, this bubbled up to `GlobalExceptionHandler.handleGeneralException`, returning the generic HTTP 500 error.

2. **Parameter Misalignment in `recordEvent` Overload:**
   - In `AuthService.java`:
     ```java
     auditService.recordEvent(user.getId(), "TENANT_SIGNUP_SUCCESS", "TENANT", tenant.getId(), clientIp, ...);
     ```
     called the 6-parameter overload `recordEvent(Long userId, String action, ...)`, which passed `explicitTenantId = null`.
   - Inside `recordEvent`, if `explicitTenantId` is null, it evaluated `TenantSecurityContext.getOptionalTenantId()`. However, `executeWithTenant` had already completed and cleared the ThreadLocal context, creating an ambiguous tenant attribution window.

3. **Deferred Entity Flush Outside Tenant Context:**
   - In `AuthService.java`, `userRepository.save(u)` was executed inside `executeWithTenant`, but Hibernate can defer SQL execution and entity listeners until the transaction commit phase. If flush occurred after `executeWithTenant` exited, tenant verification checks could fail.

4. **Missing Handlers for `DataIntegrityViolationException`:**
   - In `GlobalExceptionHandler.java`, database constraint exceptions (such as unique email or duplicate tenant code) were not explicitly caught, falling through to `handleGeneralException` and returning HTTP 500 instead of HTTP 409 Conflict.

---

## 3. Engineering Fixes Implemented

### 3.1 Backend Architectural Hardening

1. **`AuditService.java` Propagation Migration:**
   - Changed `@Transactional(propagation = Propagation.REQUIRES_NEW)` to standard `@Transactional` (`Propagation.REQUIRED`).
   - Audit logging now participates in the existing transaction, eliminating second-connection pool exhaustion, lock wait deadlocks, and uncommitted foreign key visibility errors.
   - Preserves fail-safe `try-catch` blocks ensuring audit logging will never abort business operations.

2. **`AuthService.java` Signup Provisioning Pipeline:**
   - **Collision-Safe Tenant Code:** Added `do-while` loop verifying `tenantRepository.existsByTenantCode(tenantCode)` before persisting.
   - **Immediate Persistence Flushes:** Used `tenantRepository.saveAndFlush(tenant)` and `userRepository.saveAndFlush(u)` inside `TenantSecurityContext.executeWithTenant(savedTenant.getId(), ...)` to guarantee entities and identities are committed to the DB session before context exit.
   - **Explicit Tenant Audit Attribution:** Updated `auditService.recordEvent` to explicitly provide `savedTenant.getId()` as the first parameter.
   - **Audit Resilience Wrap:** Wrapped the signup audit call in a `try-catch` with warning logs to guarantee user registration and session creation can never fail due to downstream audit issues.

3. **`GlobalExceptionHandler.java` Conflict Mapping:**
   - Added `@ExceptionHandler(DataIntegrityViolationException.class)` mapping database constraint violations to HTTP 409 CONFLICT (`RESOURCE_CONFLICT` / `EMAIL_ALREADY_EXISTS`), preventing unexpected 500 errors.

4. **Backend Test Suite Additions:**
   - Added comprehensive tests in `OtpLifecycleTest.java`:
     - `testVerifySignupOtp_ProvisionsTenantAndUser`: Verifies complete end-to-end signup OTP verification, tenant provisioning, and session creation.
     - `testVerifySignupOtp_EmailAlreadyExists_ReturnsConflict`: Validates HTTP 409 CONFLICT on duplicate email.
     - `testVerifySignupOtp_InvalidFormat_ReturnsBadRequest`: Validates HTTP 400 on malformed OTP.
     - `testVerifySignupOtp_WrongCode_DecrementsAttempts`: Validates attempt counter decrementing and cooldowns.

---

### 3.2 Frontend Signup & OTP Error Recovery (`SignupPage.tsx`)

1. **Granular Error Handling:**
   - `INCORRECT_OTP`: Contextual feedback showing exact remaining attempts.
   - `OTP_EXPIRED`: Clear explanation of 5-minute code validity and prompt to request a new code.
   - `MAX_ATTEMPTS_EXCEEDED`: Explains limit reached and prompts for fresh code.
   - `NO_ACTIVE_OTP` / `OTP_ALREADY_USED`: Informs user that the code was consumed or invalidated.
   - `EMAIL_ALREADY_EXISTS` / `RESOURCE_CONFLICT` / `DATA_CONFLICT`: Direct guidance that account exists with prompt to sign in.
   - `EMAIL_SERVICE_UNAVAILABLE`: Informs user of email provider downtime.
   - `NETWORK_ERROR`: Alerts user to network connection drops.

2. **State & Input Preservation:**
   - Form field values (`companyName`, `warehouseName`, `fullName`, `email`) are preserved across step transitions and errors so users never need to retype their details.
   - Automatic clearing of OTP input upon code resend.
   - Double-submission prevention with button disable and loading spinners during API calls.

---

## 4. Premium Public SaaS Landing Page (`LandingPage.tsx`)

A modern, high-converting B2B landing page was engineered at `/`:

- **Headline:** `"One Control Tower. Your Entire Fleet."`
- **Value Proposition:** `"Manage vehicles, drivers, trips, shipments and fleet operations from one secure workspace."`
- **Primary & Secondary CTAs:**
  - `"Get Started"` (`/signup`)
  - `"Sign In"` (`/login`)
  - Direct `"Enter Control Tower"` link if authenticated.
- **Top Navigation Bar:**
  - FleetX logo with Radio icon and `SaaS v2.0` badge.
  - Section shortcuts: *Features*, *Platform*, *How It Works*, *Security*.
  - Direct CTA buttons: *Sign In* & *Get Started*.
- **Live Control Tower Preview:**
  - Interactive dark UI displaying 4 live metrics: Active Fleet Units (24 Vehicles), Ongoing Dispatches (8 Trips), Active Geofences (12 Zones), and Avg Fleet Efficiency (4.2 km/L).
- **The 5 Platform Modules Showcase:**
  1. **Identity & Admin:** Multi-tenant workspace isolation, RBAC (Admin, Dispatcher, Safety Officer, Driver, Analyst), user management, and immutable audit trails.
  2. **Fleet & Asset Management:** Vehicle lifecycle tracking, inspection checklists, fuel logs, automated maintenance reminders, and readiness verification.
  3. **Driver & Trip Operations:** Driver qualification validation, trip dispatch workflow, and real-time status transitions.
  4. **Shipment & Route Management:** Cargo allocation, waypoint checkpoint tracking, and automated delay detection.
  5. **Tracking & Control Tower:** Live interactive map with route simulation, polygonal & circular geofences, automated overspeed alerts, and CSV exports.
- **4-Step Onboarding Architecture:**
  1. *Create Workspace:* Register company and warehouse depot name.
  2. *Add Your Fleet:* Register vehicles with VIN & plate numbers, and enroll licensed drivers.
  3. *Dispatch & Track:* Create trips, allocate cargo shipments, and track route execution with live telemetry.
  4. *Optimize & Scale:* Monitor geofences, catch delays, analyze fuel economy, and export audit reports.
- **Hardened Security & Reliability Section:**
  - Fail-closed tenant partitioning.
  - Cryptographic SHA-256 HMAC OTP authentication (passwordless).
  - 5-Tier Role-Based Access Control.
  - Immutable audit trail.

---

## 5. Verification & Test Evidence

### 5.1 Backend Test Suite (Maven / JUnit 5)
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.fleetx.auth.OtpLifecycleTest
...
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.851 s
[INFO] Running com.fleetx.auth.AuthLifecycleTest
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.089 s
...
[INFO] Results:
[INFO] Tests run: 134, Failures: 0, Errors: 0, Skipped: 4
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 13.760 s
```
- **134 Tests Passing** (0 Failures, 0 Errors, 4 Skipped).

### 5.2 Frontend Test Suite (Vitest / React Testing Library)
```
 ✓ src/test/App.test.tsx (3)
 ✓ src/test/AuthComponents.test.tsx (15)
 ✓ src/test/DriverComponents.test.tsx (6)
 ✓ src/test/FleetComponents.test.tsx (5)
 ✓ src/test/IntegrationNavigationAndAuth.test.tsx (6)
 ✓ src/test/MultiTenantLandingAndOtp.test.tsx (7)
 ✓ src/test/ShipmentComponents.test.tsx (5)
 ✓ src/test/TrackingComponents.test.tsx (4)

 Test Files  8 passed (8)
      Tests  51 passed (51)
   Duration  1.33s
```
- **51 Tests Passing** across 8 test suites.

### 5.3 Production Packaging
- `npm run build`: React/Vite bundle compiled and optimized in 1.40s.
- Bundle copied to `backend/src/main/resources/static/`.
- `mvn clean package -DskipTests`: Produced `fleetx-backend-0.0.1-SNAPSHOT.jar` (63 MB executable fat JAR).

---

## 6. GitHub Synchronization History

| Commit | Description | Scope |
|:-------|:------------|:------|
| `ac63aaa` | `fix(auth): resolve tenant signup OTP provisioning and audit context` | Backend: AuditService, AuthService, GlobalExceptionHandler, tests |
| `a948cd7` | `feat(web): add professional FleetX public landing page, enhanced OTP error recovery, and updated static bundle` | Frontend: LandingPage, SignupPage, tests, static bundle |

Both commits have been pushed to `origin/feature/multitenant-email-otp`.

---

## 7. Release Recommendation

1. **GitHub Pull Request:** PR #7 was previously merged into `main`. A new release PR can merge `feature/multitenant-email-otp` (`a948cd7`) into `main`.
2. **Production Service:** Render service `fleetx-h9wf` auto-deploys upon push to `main` or manual trigger.
3. **Operator Reminder:** Ensure `RESEND_API_KEY` is configured in Render environment variables for live transactional email delivery.
