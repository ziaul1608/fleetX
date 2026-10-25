# FleetX Phase 11D — HTTPS Email API Integration & Final Release Gate Report

**Date:** 2026-10-25  
**Branch:** `feature/multitenant-email-otp`  
**Repository:** [https://github.com/ziaul1608/fleetX](https://github.com/ziaul1608/fleetX)  
**Live Application:** [https://fleetx-h9wf.onrender.com](https://fleetx-h9wf.onrender.com)  
**Database:** Aiven MySQL 8.4 (Cloud) / Local Rehearsal Container `fleetx-mysql` (MySQL 8.4.11)  
**Release Readiness Gate Status:** **BLOCKED FOR LIVE PRODUCTION DEPLOYMENT** (Awaiting Operator Configuration of `RESEND_API_KEY` and `FLEETX_MAIL_FROM` in Render Dashboard)

---

## 1. Executive Summary

Phase 11D addresses the final infrastructural blocker preventing production deployment of FleetX's multi-tenant SaaS architecture: **transactional email delivery on Render Free Web Services**.

During Phase 11C, internal security hardening, database schema backfills (V600/V601), and MySQL 8.4 migration rehearsals passed with zero errors. However, live production remained blocked because Render Free tier environments block outbound TCP ports **25, 465, and 587** to prevent email spam abuse. Consequently, traditional SMTP delivery (JavaMailSender) causes network socket timeouts when running on Render.

To resolve this limitation under a **strict ₹0 / $0 budget constraint**, Phase 11D replaced the production outbound SMTP dependency with an HTTPS REST API client targeting **Resend** (`POST https://api.resend.com/emails`). Resend operates entirely over outbound HTTPS (TCP port 443), which is fully supported and unrestricted on Render.

All backend tests (130/130 passing), frontend tests (49/49 passing), and production builds compile cleanly. The codebase is fully prepared for zero-downtime production deployment as soon as the operator inputs the Resend API credentials into the Render dashboard.

---

## 2. Infrastructure Constraint & Architectural Solution

### 2.1 The Render Free Service SMTP Port Block
Render Free Web Services enforce egress firewall filtering that drops or blocks outbound packets to:
- Port 25 (Standard unencrypted / STARTTLS SMTP)
- Port 465 (SMTPS / SSL)
- Port 587 (STARTTLS Submission)

Attempting to connect to external SMTP servers (such as smtp.gmail.com, smtp.sendgrid.net, or mailgun.org) results in `java.net.SocketTimeoutException: Connect timed out` or `ResourceAccessException`.

### 2.2 HTTPS Transactional Email Solution: Resend REST API
To achieve guaranteed delivery without port blocking or hosting infrastructure upgrades:
1. **Transport:** Outbound HTTPS (`https://api.resend.com/emails`) on port 443.
2. **Cost Constraint:** Free tier provides 3,000 emails/month and 100 emails/day with no credit card required.
3. **Implementation Class:** `com.fleetx.auth.service.ResendEmailService` implementing `EmailService`.
4. **Spring Profile Isolation:**
   - `ResendEmailService`: Annotated `@Service`, `@Primary`, and `@Profile("!test")`. Active in `dev`, `qa`, and `prod`.
   - `SmtpEmailService`: Annotated `@Service` and `@Profile("smtp")`. Preserved for on-premise or dedicated server deployments with unrestricted ports.
   - `MockEmailService`: Annotated `@Service` and `@Profile("test")`. Strictly limited to unit and integration tests; completely inaccessible in `prod`.

---

## 3. Resend HTTPS Email Adapter Implementation Details

### 3.1 Source File: `backend/src/main/java/com/fleetx/auth/service/ResendEmailService.java`
The adapter encapsulates production HTTP communication with Resend using Spring's modern `RestClient`:
- **Timeout Bounds:** 5-second connection timeout and 10-second read timeout enforced via `SimpleClientHttpRequestFactory`. Prevents web server worker thread starvation during transient network degradation.
- **Fail-Closed Guarantees:**
  - If `RESEND_API_KEY` is blank or unset, calls to send OTP or welcome emails immediately throw `DomainException("EMAIL_SERVICE_UNAVAILABLE", ...)` with HTTP 503. The system never pretends an email was dispatched when it was not.
  - Rate limiting (HTTP 429) returns `DomainException("EMAIL_RATE_LIMITED", ...)` with HTTP 429.
  - Unverified senders or domain mismatches (HTTP 422) return `DomainException("EMAIL_DISPATCH_FAILED", ...)` with HTTP 503.
  - Network timeouts return `DomainException("EMAIL_SERVICE_TIMEOUT", ...)` with HTTP 503.
- **Security & Privacy:**
  - `maskEmail(recipient)` ensures that operational logs only print masked email addresses (e.g. `j***@example.com`).
  - Plaintext OTPs, HMAC secrets, and API keys are never included in log output, exception messages, or HTTP response bodies.
  - The API key header `Authorization: Bearer <token>` is managed securely in HTTP headers without URL leakage.

---

## 4. Configuration & Environment Variables

### 4.1 Application Properties

#### `backend/src/main/resources/application.yml` & `application-prod.yml`
```yaml
fleetx:
  auth:
    otp:
      hmac-secret: ${FLEETX_OTP_HMAC_SECRET:fleetx-dev-hmac-secret-change-in-prod-32-chars-long}
  mail:
    from: ${FLEETX_MAIL_FROM:onboarding@resend.dev}
    resend:
      api-key: ${RESEND_API_KEY:}
      api-url: ${RESEND_API_URL:https://api.resend.com/emails}
```
*Note: Obsolete `spring.mail.*` SMTP configuration blocks were removed to eliminate unused socket connection pools.*

### 4.2 Infrastructure as Code: `render.yaml`
```yaml
      # Production HTTPS Transactional Email Delivery (Resend REST API)
      # Bypasses Render outbound SMTP port blocking (ports 25, 465, 587)
      - key: RESEND_API_KEY
        sync: false
      - key: FLEETX_MAIL_FROM
        sync: false
      - key: FLEETX_OTP_HMAC_SECRET
        sync: false
```

---

## 5. Verification & Test Evidence

### 5.1 Resend Email Service Unit & Mock Server Tests
**File:** `backend/src/test/java/com/fleetx/auth/ResendEmailServiceTest.java`  
Executed with `MockRestServiceServer` against all failure and success branches:

| Test Case | Scenario Verified | Outcome |
|:---|:---|:---:|
| `testSendOtpEmail_Success` | Dispatches JSON payload to `https://api.resend.com/emails` with Bearer header; parses 200 OK | **PASSED** |
| `testSendWelcomeEmail_Success` | Dispatches tenant welcome email with Bearer header; parses 200 OK | **PASSED** |
| `testSendEmail_MissingApiKey_ThrowsServiceUnavailable` | Fail-closed check: blank API key throws HTTP 503 `EMAIL_SERVICE_UNAVAILABLE` | **PASSED** |
| `testSendEmail_RateLimit429_ThrowsRateLimited` | Resend HTTP 429 returns `EMAIL_RATE_LIMITED` | **PASSED** |
| `testSendEmail_UnverifiedSender422_ThrowsDispatchFailed` | Resend HTTP 422 returns `EMAIL_DISPATCH_FAILED` | **PASSED** |
| `testSendEmail_ConnectTimeout_ThrowsServiceTimeout` | Connection timeout triggers HTTP 503 `EMAIL_SERVICE_TIMEOUT` | **PASSED** |
| `testSendEmail_ReadTimeout_ThrowsServiceTimeout` | Socket read timeout triggers HTTP 503 `EMAIL_SERVICE_TIMEOUT` | **PASSED** |
| `testSendEmail_ServerError500_ThrowsDispatchFailed` | Upstream HTTP 500 triggers HTTP 503 `EMAIL_DISPATCH_FAILED` | **PASSED** |
| `testLogging_EmailMasked` | Verifies recipient emails are masked in operational output | **PASSED** |

**Summary:** 9 tests run, 9 passed, 0 failures, 0 errors.

### 5.2 Full Backend Test Suite
Executed command:
```bash
JAVA_HOME="..." mvn test
```
**Results:**
- **Total Tests Run:** 130
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Status:** **BUILD SUCCESS** in 17.209 s
- **Key Modules Verified:**
  - `MySQL8PersistenceIntegrationTest`: 4/4 passed against live MySQL 8.4 container
  - `ProductionProfileSeederSafetyTest`: 4/4 passed, verifies zero test/demo seeders run in prod
  - `MultiTenantIsolationTest`: All tenant context enforcement and cross-tenant isolation tests passed
  - `ResendEmailServiceTest`: 9/9 passed
  - `VehicleOperationsTest`, `FuelAndMaintenanceTest`, `DriverOperationsTest`, `SimulationAndGeofenceTest`: All passed

### 5.3 Frontend Test Suite & Production Build
Executed commands:
```bash
npm test -- --run
npm run build
```
**Results:**
- **Test Files:** 8 passed (8)
- **Tests:** 49 passed (49)
- **TypeScript & Vite Build:** Transformed 1,727 modules, output 47 assets, 0 errors, 0 warnings.

---

## 6. Pre-Deployment Release Gate Evaluation

| Gate # | Release Requirement | Status | Evidence |
|:---:|:---|:---:|:---|
| **Gate 1** | Outbound SMTP port block resolved on Render | **MET** | Implemented `ResendEmailService` using HTTPS REST API on port 443 |
| **Gate 2** | Fail-closed tenant isolation across all entities | **MET** | All JPA entities reject null tenant context; fallback to tenant 1 removed |
| **Gate 3** | Flyway migrations V600 & V601 MySQL 8.4 verified | **MET** | Clean rehearsal on MySQL 8.4.11 (`fleetx-mysql`), 12 migrations validated |
| **Gate 4** | Zero test / demo seeders executed in production | **MET** | Verified via `ProductionProfileSeederSafetyTest` under `prod` profile |
| **Gate 5** | Passwordless authentication & email OTP security | **MET** | BCrypt OTP hashing, constant-time HMAC, no plaintext credentials |
| **Gate 6** | Full automated test suite passing (Backend + Frontend) | **MET** | 130 backend tests + 49 frontend tests = 179 total tests passing |
| **Gate 7** | Production environment variables configured in Render | **PENDING** | Operator must set `RESEND_API_KEY` and `FLEETX_MAIL_FROM` in Render Dashboard |

### Final Verdict: **BLOCKED FOR LIVE PRODUCTION DEPLOYMENT**
In accordance with mandatory safety rules, release to production cannot be completed until the production environment contains valid Resend API credentials. Attempting to deploy prior to setting these variables would result in HTTP 503 errors on the OTP signup/login flow.

---

## 7. Operator Runbook for Production Release

Once the repository owner is ready to release Phase 11 multi-tenancy to production:

### Step 1: Obtain Free Resend API Key
1. Sign in to [https://resend.com](https://resend.com) (Free tier: 3,000 emails/mo, no credit card required).
2. Navigate to **API Keys** -> **Create API Key**.
3. Name: `fleetx-production`, Permission: `Sending access`.
4. Copy the generated key (`re_...`).

### Step 2: Configure Environment Variables in Render Dashboard
1. Open the Render Dashboard: [https://dashboard.render.com](https://dashboard.render.com).
2. Select the `fleetx` Web Service (`fleetx-h9wf`).
3. Navigate to **Environment**.
4. Add / update the following variables:
   - `RESEND_API_KEY`: `<Your Resend API Key>`
   - `FLEETX_MAIL_FROM`: `onboarding@resend.dev` (or your verified domain sender, e.g. `auth@yourdomain.com`)
   - `FLEETX_OTP_HMAC_SECRET`: `<Generate a random 32+ character alphanumeric string>`
5. Click **Save Changes**.

### Step 3: Merge and Deploy
1. Review the pull request or merge `feature/multitenant-email-otp` into `main`.
2. Render will automatically detect the new commit on `main` and execute the build pipeline (`mvn clean package -DskipTests`).
3. Flyway will execute migrations V600 and V601 against Aiven MySQL 8.4.
4. The service will boot up with `ResendEmailService` as the active email bean.

### Step 4: Post-Deployment Smoke Verification
1. Visit [https://fleetx-h9wf.onrender.com](https://fleetx-h9wf.onrender.com).
2. Click **Get Started** on the public landing page.
3. Fill out the registration form: Company Name, Owner Name, and the email associated with your Resend account (if using `onboarding@resend.dev`).
4. Check your inbox for the 6-digit verification code.
5. Enter the OTP code to log in.
6. Verify that the private tenant workspace loads with tenant-isolated data and zero cross-tenant contamination.
