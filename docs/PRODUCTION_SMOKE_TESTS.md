# FleetX Production Post-Deployment Smoke Tests

**Document Version:** 1.0.0  
**Target:** Live Production Environment (Single Public Origin)  
**Execution Time:** < 3 minutes  
**Prerequisites:** `curl`, `jq`, and network access to the live deployment URL.

---

## 1. Overview & Objectives

This smoke test protocol validates the operational integrity of the newly deployed FleetX application immediately after a cloud deployment. It verifies:
1. Container runtime and health check status.
2. Single-origin static asset delivery and SPA HTML5 pushState routing.
3. Unauthenticated API boundary protection.
4. Session authentication, CSRF cookie exchange, and role-based access control.
5. Persistent MySQL 8.4 transactional read/write operations.
6. Clean session invalidation upon logout.

---

## 2. Structured 10-Step Smoke Test Suite

### Step 1: Container Health Check
Verifies that Spring Boot has booted successfully, connected to MySQL 8.4, and executed Flyway migrations.
- **Request:**
  ```bash
  curl -s -i "https://${FLEETX_HOST}/api/health"
  ```
- **Expected Outcome:**
  - HTTP Status: `200 OK`
  - Body: JSON containing `"status": "UP"` and `"application": "fleetx-backend"`.

---

### Step 2: Single-Origin Root & HTML5 PushState Navigation
Verifies that the single public origin serves `index.html` on root and resolves client-side deep links without 404 errors.
- **Requests:**
  ```bash
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/"
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/login"
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/fleet/vehicles"
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/shipments/waypoints"
  ```
- **Expected Outcome:**
  - All four requests return HTTP `200 OK`.
  - Content includes `<title>FleetX — Intelligent Fleet & Logistics Management</title>`.

---

### Step 3: Static Asset Delivery & Cache Integrity
Verifies that compiled JavaScript chunks are served with valid MIME types, and non-existent assets return 404 rather than false 200 fallbacks.
- **Requests:**
  ```bash
  # Valid asset request (inspect asset path from index.html)
  curl -s -I "https://${FLEETX_HOST}/assets/index-*.js" | grep -E "(HTTP/|content-type:)"

  # Non-existent asset request
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/assets/non-existent-file.js"
  ```
- **Expected Outcome:**
  - Valid asset returns HTTP `200 OK` with `Content-Type: application/javascript`.
  - Non-existent asset returns HTTP `404 Not Found`.

---

### Step 4: Security Barrier & Unauthenticated Rejection
Verifies that protected backend APIs cannot be accessed without an active session cookie.
- **Request:**
  ```bash
  curl -s -o /dev/null -w "%{http_code}\n" "https://${FLEETX_HOST}/api/v1/fleet/vehicles"
  ```
- **Expected Outcome:**
  - HTTP Status: `401 Unauthorized`
  - Response body: JSON error matching `ApiErrorResponse` structure.

---

### Step 5: CSRF Token Handshake
Verifies that the application provides a valid `XSRF-TOKEN` cookie required for state-changing requests.
- **Request:**
  ```bash
  curl -s -c cookies.txt "https://${FLEETX_HOST}/api/v1/auth/csrf"
  ```
- **Expected Outcome:**
  - HTTP Status: `200 OK`
  - `cookies.txt` contains a valid `XSRF-TOKEN` cookie.

---

### Step 6: Admin Authentication & Session Generation
Performs a real login using administrator credentials and stores the `JSESSIONID` session cookie.
- **Request:**
  ```bash
  CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

  curl -s -b cookies.txt -c cookies.txt -X POST "https://${FLEETX_HOST}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}" \
    -d "{\"email\": \"admin@fleetx.local\", \"password\": \"${ADMIN_PASSWORD}\"}"
  ```
- **Expected Outcome:**
  - HTTP Status: `200 OK`
  - `cookies.txt` now contains a `JSESSIONID` cookie with `HttpOnly` and `SameSite=Lax`.
  - Response JSON confirms successful authentication.

---

### Step 7: Identity & Session Validation (`/auth/me`)
Verifies that the established session correctly identifies the authenticated user and roles.
- **Request:**
  ```bash
  curl -s -b cookies.txt "https://${FLEETX_HOST}/api/v1/auth/me"
  ```
- **Expected Outcome:**
  - HTTP Status: `200 OK`
  - JSON response:
    - `"email": "admin@fleetx.local"`
    - `"role": "ROLE_ADMIN"` (or list of authorities containing `ROLE_ADMIN`).

---

### Step 8: Role-Based Authorization Enforcement
Verifies that role-protected admin endpoints are accessible to `ROLE_ADMIN`.
- **Request:**
  ```bash
  curl -s -b cookies.txt "https://${FLEETX_HOST}/api/v1/admin/users"
  ```
- **Expected Outcome:**
  - HTTP Status: `200 OK`
  - Returns paginated list or collection of system user accounts.

---

### Step 9: Persistent Database Write & Query Verification
Creates a test vehicle or driver asset to confirm full MySQL 8.4 transactional persistence.
- **Request:**
  ```bash
  CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

  curl -s -b cookies.txt -c cookies.txt -X POST "https://${FLEETX_HOST}/api/v1/fleet/vehicles" \
    -H "Content-Type: application/json" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}" \
    -d '{
      "vin": "1SMOKETEST998877",
      "licensePlate": "MH-01-SM-9999",
      "model": "Volvo FH16 Smoke",
      "status": "AVAILABLE",
      "currentOdometer": 1250.0,
      "fuelCapacity": 300.0,
      "currentFuel": 280.0
    }'
  ```
- **Expected Outcome:**
  - HTTP Status: `201 Created`
  - Vehicle ID returned and persisted in MySQL 8.4.

---

### Step 10: Session Invalidation & Logout
Terminates the session and verifies subsequent API calls return 401 Unauthorized.
- **Request:**
  ```bash
  CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

  curl -s -b cookies.txt -c cookies.txt -X POST "https://${FLEETX_HOST}/api/v1/auth/logout" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}"

  # Attempt access after logout
  curl -s -o /dev/null -w "%{http_code}\n" -b cookies.txt "https://${FLEETX_HOST}/api/v1/fleet/vehicles"
  ```
- **Expected Outcome:**
  - Logout returns HTTP `200 OK`.
  - Follow-up call returns HTTP `401 Unauthorized`.
  - Session cookie is fully invalidated on the server.

---

## 3. Automated Smoke Test Script (`smoke_test.sh`)

Save the following script and run against any target environment:

```bash
#!/usr/bin/env bash
set -euo pipefail

TARGET_URL="${1:-http://127.0.0.1:8080}"
ADMIN_USER="${2:-admin@fleetx.local}"
ADMIN_PASS="${3:-Admin@123}"
COOKIE_JAR="$(mktemp)"

trap 'rm -f "${COOKIE_JAR}"' EXIT

echo "=========================================================="
echo " FleetX Production Smoke Test Suite"
echo " Target URL : ${TARGET_URL}"
echo " Admin User : ${ADMIN_USER}"
echo "=========================================================="

# 1. Health
echo -n "[1/10] Verifying Health Endpoint (/api/health)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_URL}/api/health")
[ "${STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${STATUS})"; exit 1; }

# 2. SPA Root
echo -n "[2/10] Verifying Single-Origin Root (GET /)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_URL}/")
[ "${STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${STATUS})"; exit 1; }

# 3. SPA PushState Fallback
echo -n "[3/10] Verifying SPA Fallback (/shipments/waypoints)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_URL}/shipments/waypoints")
[ "${STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${STATUS})"; exit 1; }

# 4. Auth Barrier
echo -n "[4/10] Verifying Auth Barrier (/api/v1/fleet/vehicles)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_URL}/api/v1/fleet/vehicles")
[ "${STATUS}" = "401" ] && echo "PASS (401)" || { echo "FAIL (${STATUS})"; exit 1; }

# 5. CSRF Handshake
echo -n "[5/10] Fetching CSRF Token (/api/v1/auth/csrf)... "
curl -s -c "${COOKIE_JAR}" "${TARGET_URL}/api/v1/auth/csrf" > /dev/null
CSRF=$(grep -i "XSRF-TOKEN" "${COOKIE_JAR}" | awk '{print $7}' || true)
[ -n "${CSRF}" ] && echo "PASS (Token: ${CSRF:0:8}...)" || { echo "FAIL (No CSRF cookie)"; exit 1; }

# 6. Admin Login
echo -n "[6/10] Performing Admin Login (/api/v1/auth/login)... "
LOGIN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" \
  -X POST "${TARGET_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: ${CSRF}" \
  -d "{\"email\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
[ "${LOGIN_STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${LOGIN_STATUS})"; exit 1; }

# Update CSRF from jar if rotated
NEW_CSRF=$(grep -i "XSRF-TOKEN" "${COOKIE_JAR}" | awk '{print $7}' || true)
[ -n "${NEW_CSRF}" ] && CSRF="${NEW_CSRF}"

# 7. Auth Identity Me
echo -n "[7/10] Verifying Current Identity (/api/v1/auth/me)... "
ME_ROLE=$(curl -s -b "${COOKIE_JAR}" "${TARGET_URL}/api/v1/auth/me" | grep -o '"ADMIN"' || true)
[ "${ME_ROLE}" = '"ADMIN"' ] && echo "PASS (ADMIN role confirmed)" || { echo "FAIL"; exit 1; }

# 8. RBAC Users API
echo -n "[8/10] Verifying RBAC Access (/api/v1/admin/users)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -b "${COOKIE_JAR}" "${TARGET_URL}/api/v1/admin/users")
[ "${STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${STATUS})"; exit 1; }

# 9. Query Fleet Data
echo -n "[9/10] Verifying Authenticated Query (/api/v1/fleet/vehicles)... "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -b "${COOKIE_JAR}" "${TARGET_URL}/api/v1/fleet/vehicles")
[ "${STATUS}" = "200" ] && echo "PASS (200)" || { echo "FAIL (${STATUS})"; exit 1; }

# 10. Logout & Invalidation
echo -n "[10/10] Logging Out & Verifying Invalidation (/api/v1/auth/logout)... "
curl -s -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" -X POST "${TARGET_URL}/api/v1/auth/logout" \
  -H "X-XSRF-TOKEN: ${CSRF}" > /dev/null
AFTER_LOGOUT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -b "${COOKIE_JAR}" "${TARGET_URL}/api/v1/fleet/vehicles")
[ "${AFTER_LOGOUT_STATUS}" = "401" ] && echo "PASS (Session terminated, 401)" || { echo "FAIL (${AFTER_LOGOUT_STATUS})"; exit 1; }

echo "=========================================================="
echo " ALL 10 SMOKE TESTS PASSED SUCCESSFULLY!"
echo "=========================================================="
```
