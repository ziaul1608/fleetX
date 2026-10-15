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
