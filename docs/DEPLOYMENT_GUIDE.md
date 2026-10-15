# FleetX Free Production Deployment Guide
**Document Version:** 2.0.0  
**Target Architecture:** 100% Free Public Cloud (Zero Cost / No Credit Card Required)  
**Web Tier:** Render Free Docker Web Service (Single Public Origin)  
**Database Tier:** Aiven Always-Free Managed MySQL (Encrypted TLS)  

---

## 1. Architecture & Design Principles

### 1.1 Single Public Origin Topology
FleetX is architected and packaged as a unified, single-public-origin application:
- **Embedded Frontend:** The React 18 TypeScript Single Page Application is compiled and embedded into Spring Boot's static resource directory (`src/main/resources/static/`).
- **Unified Origin:** Spring Boot's embedded Tomcat serves the React SPA (`index.html`, `/assets/*`), the REST API (`/api/v1/*`), and WebSocket endpoints (`/ws/fleetx/*`) under a single HTTPS domain (`https://fleetx.onrender.com`).
- **No CORS Friction:** All frontend API calls use relative paths (`/api/v1/...`), eliminating cross-origin preflight requests and third-party cookie dropping.
- **First-Party Security:** `JSESSIONID` session cookies and `XSRF-TOKEN` CSRF cookies operate as first-party cookies with `HttpOnly` and `SameSite=Lax`.

### 1.2 HTML5 History PushState Routing Fallback
Client-side routing (`/login`, `/fleet/vehicles`, `/shipments/waypoints`, `/admin/users`, etc.) is managed via custom `PathResourceResolver` in `WebMvcConfig.java`:
- Non-API, non-WebSocket, non-asset requests automatically return `classpath:/static/index.html`.
- Genuine missing asset files (`/assets/*.js`) and invalid API calls return HTTP 404/401 rather than false 200 SPA fallbacks.

---

## 2. Free-Tier Characteristics & Guardrails

| Attribute | Render Free Web Service | Aiven Always-Free MySQL |
| :--- | :--- | :--- |
| **Cost** | **$0.00 / month** (No payment method required) | **$0.00 / month** (No credit card required) |
| **Compute / Memory** | 512 MB RAM, 0.1 vCPU | 1 GB RAM, 1 vCPU |
| **Storage** | Ephemeral container storage | 1 GB Managed Persistent Disk |
| **Inactivity Policy** | Spins down after 15 mins idle; cold start takes ~50s | Powers off after prolonged inactivity (can be re-activated in console) |
| **Networking** | Public HTTPS (`https://<service>.onrender.com`) | Public hostname with mandatory TLS encryption |
| **Connection Limits** | N/A | Max 76 connections (Hikari pool capped at 20) |

> [!IMPORTANT]
> **Zero Cost Guarantee:** Neither service requires a credit card or auto-upgrading trial. Do NOT add billing details or select paid plans (Starter, Business, etc.).

---

## 3. Step-by-Step Deployment Procedure

### Step 1: Prepare GitHub Repository
1. Initialize or select a private GitHub repository under your personal GitHub account.
2. Ensure the repository contains the root `render.yaml`, `Dockerfile`, `.dockerignore`, `backend/`, and `frontend/`.
3. Push to the `main` branch.

### Step 2: Provision Free Aiven MySQL Database
1. Go to [https://aiven.io](https://aiven.io) and create a free account (no credit card required).
2. Click **Create Service**.
3. Select **MySQL**.
4. Cloud Provider & Region: Choose **AWS** or **Google Cloud** in a region close to Render (e.g. `us-east` or `us-west`).
5. Service Plan: Select **Free Plan** ($0/month).
6. Service Name: `fleetx-mysql`.
7. Once provisioned, note the connection parameters from the Aiven Console Overview:
   - **Host** (e.g., `mysql-xxxxx.aivencloud.com`)
   - **Port** (e.g., `12345` or `3306`)
   - **User** (`avnadmin` or custom user)
   - **Password**
   - **Database Name** (`defaultdb`)
   - **SSL/TLS Mode**: Aiven requires encrypted TLS.

### Step 3: Deploy via Render Blueprint (`render.yaml`)
1. Log in to [https://render.com](https://render.com) (sign up with GitHub).
2. Click **New +** > **Blueprint**.
3. Connect your GitHub repository containing FleetX.
4. Render will detect `render.yaml` and configure:
   - **Service Name:** `fleetx`
   - **Runtime:** `docker`
   - **Plan:** `free`
   - **Health Check Path:** `/api/health`
5. Render will prompt you to enter values for variables marked `sync: false`:

| Variable Key | Value Source / Recommendation |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` (automatically set by blueprint) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/defaultdb?useSSL=true&requireSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | `<AIVEN_USER>` (e.g., `avnadmin`) |
| `SPRING_DATASOURCE_PASSWORD` | `<AIVEN_PASSWORD>` |
| `COOKIE_SECURE` | `true` |
| `COOKIE_SAME_SITE` | `lax` |
| `CORS_ALLOWED_ORIGINS` | `https://<service-name>.onrender.com` |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` (set `true` on first boot only) |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@fleetx.local` (or your preferred admin email) |
| `BOOTSTRAP_ADMIN_PASSWORD` | `<YOUR_CHOSEN_STRONG_ADMIN_PASSWORD>` |

6. Click **Apply**.
7. Render triggers the multi-stage Docker build, pulls the container image, connects to Aiven MySQL, executes Flyway migrations `V100`–`V501`, and provisions the initial administrator account.

---

## 4. Post-Deployment Verification & Smoke Tests

Once Render displays **Service Live**:
1. Open the platform-assigned HTTPS URL: `https://fleetx-xxxx.onrender.com`.
2. Verify public health check:
   ```bash
   curl -s "https://fleetx-xxxx.onrender.com/api/health"
   ```
   Expected response: `{"status":"UP","application":"fleetx-backend","activeProfiles":["prod"]}`.
3. Run the automated smoke test script against your live deployment:
   ```bash
   ./smoke_test.sh "https://fleetx-xxxx.onrender.com" "admin@fleetx.local" "<YOUR_ADMIN_PASSWORD>"
   ```
4. Verify all 10 smoke test steps pass (Health, SPA Root, SPA PushState Fallback, Auth Barrier, CSRF Handshake, Admin Login, Identity /me, RBAC Admin Access, Fleet Query, Logout).

---

## 5. Security & Post-Bootstrap Hardening

1. **Disable Admin Bootstrap:**
   - Once the initial administrator account is provisioned, navigate to Render **Environment** settings.
   - Set `BOOTSTRAP_ADMIN_ENABLED=false` and remove `BOOTSTRAP_ADMIN_PASSWORD`.
   - The admin account remains securely hashed with BCrypt (cost factor 12) in Aiven MySQL and will not be affected by subsequent redeploys.
2. **Cold Start Note:**
   - On the Free tier, if no traffic is received for 15 minutes, Render spins down the container.
   - The first request after sleep will take ~45-60 seconds to spin up the container and initialize the JVM. Subsequent requests respond instantly.
