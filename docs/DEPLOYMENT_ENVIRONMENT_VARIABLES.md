# FleetX Deployment Environment Variables Reference

This document provides a comprehensive specification of all environment variables used by the FleetX application across development, QA, and cloud production environments (Render + Aiven Free MySQL).

---

## 1. Core Application & Server Runtime

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `PORT` | **Yes** (Cloud) | `8080` | Assigned by Render (`10000` or dynamic) | Web container listening port. Spring Boot resolves this via `${PORT:${SERVER_PORT:8080}}`. Automatically supplied by Render. |
| `SERVER_PORT` | No | `8080` | `8080` | Fallback port if `PORT` is not defined. |
| `SPRING_PROFILES_ACTIVE` | **Yes** | `dev` | `prod` | Specifies the active Spring configuration profile (`dev`, `qa`, or `prod`). Activates `application-prod.yml`. |
| `JAVA_OPTS` | No | *None* | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` | JVM container tuning parameters. Directs the JVM to respect cgroup memory limits and fail-fast upon Out-Of-Memory errors. |

---

## 2. Database & Connection Pool (Aiven Free MySQL 8.4)

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `SPRING_DATASOURCE_URL` | **Yes** | Local fallback | `jdbc:mysql://${DB_HOST}:${DB_PORT}/defaultdb?useSSL=true&requireSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC connection string. Aiven requires encrypted TLS. Never use `useSSL=false` in production. |
| `SPRING_DATASOURCE_USERNAME` | **Yes** | *None (Fails fast)* | Injected secret (e.g. `avnadmin`) | Aiven database username. Must be entered securely via Render Dashboard. |
| `SPRING_DATASOURCE_PASSWORD` | **Yes** | *None (Fails fast)* | Injected secret | Aiven database password. Must never be committed to Git or logs. |
| `DB_HOST` | No | `localhost` | `mysql-xxxxx.aivencloud.com` | Aiven public service hostname (if constructing URL via components). |
| `DB_PORT` | No | `3306` | Aiven assigned port (e.g., `12345` or `3306`) | Aiven service port. |
| `DB_NAME` | No | `fleetx` | `defaultdb` | Target database schema name. |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | No | `20` | `20` | Maximum JDBC connections in Hikari pool (well within Aiven's 76 connection cap). |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | No | `5` | `5` | Minimum idle connections maintained in pool. |
| `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | No | `20000` | `20000` | Maximum milliseconds to wait for a database connection before timing out (fail-fast). |
| `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT` | No | `300000` | `300000` | Maximum idle time (ms) before connection retirement. |

---

## 3. Flyway Database Migrations

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `SPRING_FLYWAY_ENABLED` | No | `true` | `true` | Enables automatic schema migration on startup. |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | No | `true` | `true` | Establishes baseline schema version if existing unversioned database is encountered. |
| `SPRING_FLYWAY_LOCATIONS` | No | `classpath:db/migration` | `classpath:db/migration` | Migration scripts path containing `V100` through `V501` SQL scripts. |
| `SPRING_FLYWAY_VALIDATE_ON_MIGRATE` | No | `true` | `true` | Validates migration checksums before applying new scripts to prevent schema drift. |

---

## 4. JPA & Hibernate Configuration

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | No | `validate` | `validate` | Production safety: strictly validates that entity models match Flyway schema. Never set to `create` or `update` in production. |
| `SPRING_JPA_SHOW_SQL` | No | `false` | `false` | Suppresses raw SQL logs in production to prevent leaking sensitive customer or logistics data. |
| `SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL` | No | `false` | `false` | Suppresses verbose SQL formatting in container logs. |

---

## 5. Security, Sessions & Cookies

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `COOKIE_SECURE` | **Yes** (Prod) | `true` | `true` | Sets the `Secure` flag on `JSESSIONID` cookies so browsers only transmit them over HTTPS. |
| `COOKIE_SAME_SITE` | No | `lax` | `lax` | Enforces SameSite policy to prevent cross-site request forgery while preserving browser navigation. |
| `SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY` | No | `true` | `true` | Prevents JavaScript access to the `JSESSIONID` cookie, mitigating XSS attacks. |
| `SERVER_SERVLET_SESSION_TIMEOUT` | No | `30m` | `30m` | Session inactivity timeout before automatic expiration. |

---

## 6. Cross-Origin Resource Sharing (CORS) & WebSockets

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `CORS_ALLOWED_ORIGINS` | No | `https://*.onrender.com,https://fleetx.local` | `https://fleetx-xxxx.onrender.com` | Allowed origin patterns for CORS and SockJS/STOMP WebSockets. With single-origin serving, browser API calls are relative. |

---

## 7. Initial Bootstrap & Administrator Provisioning

| Variable Name | Required | Default Value | Recommended Production Value | Description |
| :--- | :---: | :---: | :---: | :--- |
| `BOOTSTRAP_ADMIN_ENABLED` | No | `false` | `true` on first boot only; `false` afterward | Activates initial administrator creation in `AdminBootstrapRunner`. Must be disabled after first boot. |
| `BOOTSTRAP_ADMIN_EMAIL` | No | `admin@fleetx.com` | `admin@fleetx.local` or team leader email | Email address for initial bootstrap system administrator. |
| `BOOTSTRAP_ADMIN_PASSWORD` | **Yes** (if bootstrap enabled) | *Empty* | `<STRONG_CUSTOM_PASSWORD>` | Initial administrator password. Hashed on first boot using BCrypt (cost factor 12). If account already exists in DB, runner safely skips. |
| `BOOTSTRAP_ADMIN_NAME` | No | `FleetX Administrator` | `FleetX Administrator` | Display name for bootstrap administrator. |

---

## 8. Example Production Environment Variables (Render Dashboard Format)

```bash
# Core Runtime
SPRING_PROFILES_ACTIVE=prod
COOKIE_SECURE=true
COOKIE_SAME_SITE=lax
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError

# Aiven Managed MySQL (TLS Encrypted)
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-xxxx.aivencloud.com:12345/defaultdb?useSSL=true&requireSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=avnadmin
SPRING_DATASOURCE_PASSWORD=A_SECURE_AIVEN_GENERATED_PASSWORD

# Allowed Origins (Render Domain)
CORS_ALLOWED_ORIGINS=https://fleetx.onrender.com

# One-time Admin Bootstrap (Disable after initial login)
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@fleetx.local
BOOTSTRAP_ADMIN_PASSWORD=A_STRONG_RANDOM_ADMIN_PASSWORD_2026!
```
