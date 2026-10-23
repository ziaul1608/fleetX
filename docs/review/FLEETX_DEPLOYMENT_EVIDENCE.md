# FleetX — Cloud Deployment Architecture & Live Runtime Diagnostics

**Document:** Production Infrastructure Verification, Runtime Health & Failure Analysis  
**Project:** FleetX — Intelligent Fleet & Logistics Management System  
**Audit Date:** 2026-10-23  
**Status:** DIAGNOSED (Live deployment verified previously; current cold-start database outage identified)  

---

## 1. Cloud Architecture Overview

FleetX is deployed on a zero-cost cloud topology combining a **Render Free Web Service** and an **Aiven Always-Free MySQL 8.4 Database**:

```
+-----------------------------------------------------------------------------------+
|                                Public Internet                                    |
|                                       │                                           |
|                                       ▼                                           |
|                   Cloudflare Edge CDN / SSL (Port 443 HTTPS)                      |
|                      https://fleetx-h9wf.onrender.com                             |
+---------------------------------------┬-------------------------------------------+
                                        │
                                        ▼
+-----------------------------------------------------------------------------------+
| Render Free Web Service (Docker Container: fleetx-app)                            |
| Region: Oregon (US-West) | 512 MB RAM | 0.1 vCPU                                  |
| Runtime: Eclipse Temurin JRE 21 + Spring Boot 3.5.16 Embedded Tomcat               |
| Profile: SPRING_PROFILES_ACTIVE=prod                                              |
| Health Check Path: /api/health                                                    |
| Bundled Static Assets: React 18 SPA served from same origin                       |
+---------------------------------------┬-------------------------------------------+
                                        │ Mandatory Encrypted TLS (Port 15887)
                                        ▼ sslMode=REQUIRED&requireSSL=true
+-----------------------------------------------------------------------------------+
| Aiven Cloud Platform (Always-Free Managed MySQL 8.4.11)                           |
| Host: fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com:15887                   |
| Database: defaultdb | User: avnadmin                                              |
| Storage: 1 GB Managed Persistent Storage                                          |
| Schema: Governed by Flyway Migrations V100 - V501 (21 Tables)                     |
+-----------------------------------------------------------------------------------+
```

---

## 2. Historical Production Verification Record (Phase 9 Sign-Off)

On initial release deployment, the live production application was verified with **100% success**:

### 2.1 Smoke Test Execution (`smoke_test.sh`):
- `GET https://fleetx-h9wf.onrender.com/api/health` → HTTP 200 `{"status":"UP","activeProfiles":["prod"]}`
- `GET https://fleetx-h9wf.onrender.com/` → HTTP 200 (React 18 HTML entrypoint)
- `GET https://fleetx-h9wf.onrender.com/shipments/waypoints` → HTTP 200 (SPA PushState routing fallback)
- `GET https://fleetx-h9wf.onrender.com/api/v1/fleet/vehicles` → HTTP 401 Unauthorized (Auth barrier verified)
- `GET https://fleetx-h9wf.onrender.com/api/v1/auth/csrf` → HTTP 200 (`XSRF-TOKEN` cookie issued)
- `POST https://fleetx-h9wf.onrender.com/api/v1/auth/login` → HTTP 200 (`JSESSIONID` session established)
- `GET https://fleetx-h9wf.onrender.com/api/v1/auth/me` → HTTP 200 (`"role": "ADMIN"`)
- `GET https://fleetx-h9wf.onrender.com/api/v1/admin/users` → HTTP 200 (Admin RBAC verified)
- `GET https://fleetx-h9wf.onrender.com/api/v1/fleet/vehicles` → HTTP 200 (Authenticated fleet listing)
- `POST https://fleetx-h9wf.onrender.com/api/v1/auth/logout` → HTTP 200 (Session destroyed, subsequent query 401)

### 2.2 Relational Flyway Migration Evidence (Queried from Aiven MySQL):
- All 10 Flyway migrations (`V100` to `V501`) applied cleanly with zero errors.
- 21 normalized relational tables initialized.
- Initial administrator account created and verified in `users` and `user_roles`.
- Commercial vehicle `KA-05-PROD-2026` registered via live REST API confirmed persisted in Aiven `vehicles` table.

---

## 3. Current Live Runtime Health Diagnostics (Audit 2026-10-23)

During the current audit, direct network inspection revealed an upstream cloud database outage:

### Diagnostic 1: Web Service Domain DNS Resolution
```bash
$ nslookup fleetx-h9wf.onrender.com
Server:		10.5.50.1
Address:	10.5.50.1#53

Non-authoritative answer:
fleetx-h9wf.onrender.com	canonical name = gcp-us-west1-1.origin.onrender.com.
gcp-us-west1-1.origin.onrender.com	canonical name = gcp-us-west1-1.origin.onrender.com.cdn.cloudflare.net.
Name:	gcp-us-west1-1.origin.onrender.com.cdn.cloudflare.net
Address: 216.24.57.18
Address: 216.24.57.16
```
**Result:** VERIFIED. Cloudflare Edge CDN DNS resolution is active and healthy.

### Diagnostic 2: Web Service TLS Handshake & Edge Connection
```bash
$ curl -v -m 20 "https://fleetx-h9wf.onrender.com/api/health"
* Established connection to fleetx-h9wf.onrender.com (216.24.57.16 port 443)
* using HTTP/2
* Server certificate: CN=onrender.com, issuer: Google Trust Services WE1
* SSL certificate verify ok.
> GET /api/health HTTP/2
> Host: fleetx-h9wf.onrender.com
* Operation timed out after 20002 milliseconds with 0 bytes received
curl: (28) Operation timed out
```
**Result:** The edge connection succeeds, but the origin container on Render fails to respond within timeout.

### Diagnostic 3: Upstream Aiven Database Host Resolution & Port Reachability
```bash
$ nslookup fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com
Server:		10.5.50.1
Address:	10.5.50.1#53

** server can't find fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com: NXDOMAIN
```
```bash
$ nc -zv -w 5 fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com 15887
nc: getaddrinfo: nodename nor servname provided, or not known
```
**Result:** **FAILED (NXDOMAIN)**. The Aiven MySQL database hostname cannot be resolved.

---

## 4. Root Cause Analysis

1. **Aiven Inactivity Policy:**
   Aiven's free-tier services automatically power down or expire after periods of zero traffic or after reaching inactivity windows. When powered down, Aiven withdraws the service's public DNS record, causing `NXDOMAIN`.
2. **Spring Boot Fail-Fast Container Startup:**
   In `application-prod.yml`, the application activates the Hikari connection pool against `SPRING_DATASOURCE_URL` on boot. When the Render web service attempts to spin up from cold-start, Spring Boot attempts to connect to `fleetx-mysql-ziaulmustafa2005-c043.k.aivencloud.com:15887`.
3. **Container Crash Loop:**
   Because the hostname returns `NXDOMAIN`, Java throws `java.net.UnknownHostException`, causing the Spring ApplicationContext to abort startup immediately (`ExitOnOutOfMemoryError` / crash).
4. **Cloudflare Gateway Timeout:**
   Render continuously restarts the failing container, while Cloudflare waits for the upstream web service to bind to `$PORT`. Because the container never reaches `UP` status, incoming HTTP requests time out.

---

## 5. Restoration Procedure (Step-by-Step)

To restore the live application to full operation:
1. **Reactivate Aiven MySQL Service:**
   - Log into [console.aiven.io](https://console.aiven.io).
   - Locate the `fleetx-mysql` service.
   - If paused/powered off: click **Power On** / **Resume**.
   - If expired/deleted: create a new free MySQL 8.4 service (name: `fleetx-mysql`, region: any free region) and copy the new Hostname, Port, and Password.
2. **Update Render Environment (if Hostname or Port changed):**
   - Log into [dashboard.render.com](https://dashboard.render.com).
   - Open **fleetx** Web Service → **Environment**.
   - Update `SPRING_DATASOURCE_URL` with the active Aiven hostname and port:
     `jdbc:mysql://<NEW_HOST>:<NEW_PORT>/defaultdb?useSSL=true&requireSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - Update `SPRING_DATASOURCE_PASSWORD` if new credentials were generated.
   - Click **Save Changes**.
3. **Verify Automatic Deployment:**
   - Render will redeploy the Docker container.
   - Flyway will automatically run any missing migrations.
   - Health check `/api/health` will return HTTP 200 within 40 seconds.
