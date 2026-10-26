# FleetX — Cover Page, Account Switching & Logout Verification Report

**Repository:** [https://github.com/ziaul1608/fleetX](https://github.com/ziaul1608/fleetX)  
**Production Branch:** `main` | **Feature Branch:** `feature/multitenant-email-otp`  
**Live Production URL:** [https://fleetx-h9wf.onrender.com](https://fleetx-h9wf.onrender.com)  
**Render Service:** `fleetx-h9wf`  
**Verification Date:** 2026-10-26  

---

## 1. Diagnostics & Root Cause Analysis

### 1.1 Root Cause of Missing Cover-Page Behavior
- In `frontend/src/app/routes.tsx`, the root route (`path: '/'`) previously utilized an unconditional redirect in `RootHandler`:
  ```tsx
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }
  ```
  Consequently, whenever an authenticated operator or browser session opened `https://fleetx-h9wf.onrender.com/`, the application unconditionally redirected them away from the public cover page to `/dashboard`.
- Additionally, while `useAuth` checked the initial session with the backend (`/api/v1/auth/me`), `RootHandler` displayed a loading indicator (`LoadingState: Connecting to FleetX...`), delaying or preventing the instantaneous display of the public cover page.
- **Resolution:** Removed the conditional `Navigate to="/dashboard"` from `routes.tsx` so that `path: '/'` directly mounts `<LandingPage />` for all visitors. Authenticated visitors viewing the cover page are now presented with authenticated navigation controls (`Dashboard`, `Change Account`, and `Logout`), while unauthenticated visitors receive the `Sign In` (`/login`) and `Get Started` (`/signup`) CTAs.

### 1.2 Cause of External HTTP 503 Probes
- Render Free Web Services automatically spin down containers to zero after 15 minutes of inactivity. When an external probe or curl connects while the instance is sleeping, Render's edge reverse proxy returns `HTTP 503 Service Unavailable` or delays until the JVM boots.
- Furthermore, `SecurityConfig.java` previously permitted `/api/health` only for `HttpMethod.GET`. Monitoring probes utilizing `HEAD /api/health` received `HTTP 401 Unauthorized` because `HEAD` was not matched.
- **Resolution:** Updated `SecurityConfig.java` to permit all HTTP methods on `/api/health` (`.requestMatchers("/api/health").permitAll()`). Once the Render container is booted, both `/` and `/api/health` return HTTP 200 reliably.

---

## 2. Changes Made to Routing & Cover Page

1. **Root Routing (`frontend/src/app/routes.tsx`):**
   - Configured `LandingPage` directly on route `path: '/'`.
   - Preserved standalone public routes (`/login`, `/signup`) and protected module routes (`/dashboard`, `/fleet/**`, `/trips/**`, `/shipments/**`, `/tracking/**`, `/admin/**`, `/profile`).

2. **Cover Page Structure & Navigation (`frontend/src/features/landing/pages/LandingPage.tsx`):**
   - **Header Navigation:** Features FleetX logo, anchor links (`Features`, `Platform`, `How It Works`, `Security`).
   - **Unauthenticated State:** Shows `Sign In` (`/login`) and `Get Started` (`/signup`).
   - **Authenticated State:** Shows `Dashboard` (`/dashboard`), `Change Account` (action button), and `Logout` (action button).
   - **Hero Section:**
     - Headline: **"One Control Tower. Your Entire Fleet."**
     - Description: *"Manage vehicles, drivers, trips, shipments and logistics operations through one intelligent fleet management workspace..."*
     - CTAs: `Get Started — Register Private Warehouse` (`/signup`), `Sign In with Email OTP` (`/login`), and `Explore FleetX` (scrolls to `#modules`). For logged-in users, displays `Dashboard`, `Change Account`, `Logout`, and `Explore FleetX`.
   - **5 Platform Modules Showcase:** All 5 modules clearly defined with glass cards, icons, and capability checklists:
     1. Identity & Admin
     2. Fleet & Asset Management
     3. Driver & Trip Operations
     4. Shipment & Route Management
     5. Tracking & Control Tower
   - **How It Works Walkthrough:** 4-step onboarding sequence:
     1. *Create Workspace* (Register company and depot)
     2. *Add Your Fleet & Verify OTP* (Verify email through OTP and enroll assets)
     3. *Dispatch & Track* (Access dashboard and dispatch trips)
     4. *Optimize & Scale* (Manage daily operations, geofences, and audits)
   - **Security Section:** Explains fail-closed tenant partition, thread-local context isolation, HMAC OTP security, and role boundaries.
   - **Footer:** Added product navigation links (`Features`, `Platform`, `How It Works`, `Security`) and action buttons (`Sign Up Free`, `Sign In`, or `Dashboard`, `Change Account`, `Logout`).

---

## 3. Change Account & Logout Implementation Details

### 3.1 Change Account
- **Shared TopNav (`TopNav.tsx`):** Added a dedicated "Change Account" item in the authenticated user's dropdown menu with an icon and Champagne Gold accent.
- **Sidebar (`Sidebar.tsx`):** Added a "Change Account" button in the navigation links and footer, ensuring accessibility on mobile and desktop viewports.
- **Cover Page (`LandingPage.tsx`):** Added "Change Account" in both the top navigation bar and hero CTA strip for authenticated visitors.
- **Mechanism:**
  1. Invokes `auth.logout()` which sends a `POST /api/v1/auth/logout` to the backend.
  2. Server invalidates the `HttpSession` and clears the `SecurityContext`.
  3. Frontend resets `currentUser` to `null` and navigates to `/login`.
  4. The user is now on `/login`, ready to enter a new email and verify an OTP for a different warehouse tenant.

### 3.2 Logout
- **Shared TopNav (`TopNav.tsx`):** Updated "Log out" button to invoke `logout()` and navigate directly to `/` (the public cover page).
- **Sidebar (`Sidebar.tsx`):** Added "Log Out" button with red accent.
- **Cover Page (`LandingPage.tsx`):** Added "Logout" button in the top navigation and hero.
- **Mechanism:**
  1. Invokes `POST /api/v1/auth/logout`.
  2. Invalidator destroys server session and audit log records `USER_LOGOUT`.
  3. Frontend clears auth context and redirects to `/`.
  4. User is presented with the public cover page in unauthenticated state; protected routes require fresh authentication.

---

## 4. Test Results & Build Verification

### 4.1 Frontend Test Suite (Vitest)
- **Files:** 8 passed (8)
- **Tests:** 53 passed (53)
- **New Tests Added:**
  - `renders authenticated options (Dashboard, Change Account, Logout) when user has active session` in `MultiTenantLandingAndOtp.test.tsx`
  - `invokes logout when Change Account is clicked in TopNav` in `IntegrationNavigationAndAuth.test.tsx`
  - Dynamic user identity rendering with `getAllByText`

### 4.2 Backend Test Suite (JUnit 5 + Spring Boot)
- **Tests Run:** 134
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 4 (MySQL external testcontainer stubs)
- **Status:** `BUILD SUCCESS`

### 4.3 Production Packaging
- Frontend compiled with `tsc -b && vite build` into:
  - `dist/assets/index-4aU3YxgX.js` (124 kB)
  - `dist/assets/index-CUAakaJm.css` (247 kB)
- Static assets synchronized to `backend/src/main/resources/static/`.
- Executable JAR built: `backend/target/fleetx-backend-0.0.1-SNAPSHOT.jar` (63 MB).

---

## 5. Verification Sign-Off Table

| Requirement | Implementation | Evidence / Test | Status |
| :--- | :--- | :--- | :--- |
| **Cover Page at `/`** | Default entry point without redirects | Verified in `routes.tsx`, `LandingPage.tsx`, Vitest suite | **VERIFIED** |
| **Sign Up & Sign In CTAs** | Links to `/signup` and `/login` | Tested in `MultiTenantLandingAndOtp.test.tsx` | **VERIFIED** |
| **Change Account Action** | In TopNav, Sidebar, and LandingPage | Tested in `IntegrationNavigationAndAuth.test.tsx` | **VERIFIED** |
| **Logout Action** | In TopNav, Sidebar, and LandingPage | Tested in `IntegrationNavigationAndAuth.test.tsx` | **VERIFIED** |
| **All 5 Modules Displayed** | Detailed glass showcase cards | Tested in `MultiTenantLandingAndOtp.test.tsx` | **VERIFIED** |
| **4-Step Onboarding Guide** | Step-by-step visual workflow | Tested in `MultiTenantLandingAndOtp.test.tsx` | **VERIFIED** |
| **Footer Navigation** | Product & auth action links | Tested in `MultiTenantLandingAndOtp.test.tsx` | **VERIFIED** |
| **Continuous Git Push** | Auto-pushed to `origin/feature/multitenant-email-otp` | Remote SHA verified | **VERIFIED** |
