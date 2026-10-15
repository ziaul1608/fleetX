# FleetX — Intelligent Fleet & Logistics Management System
## GitHub Engineering Workflow & Quality Standards

**Document Reference:** FLEETX-GIT-2026-V1.0  
**Academic Year:** 2026–27  
**Baseline Date:** 10 October 2026  
**Status:** Approved Engineering Workflow Specification (Phase 0)

---

## 1. Branching Strategy & Lifecycle

FleetX utilizes a structured Git Flow adaptation tailored for an agile five-student collegiate team:

```
[main]          ───●──────────────────────────────────────────────●─── (Release v1.0.0 Tag)
                   ▲                                              ▲
                   │ Merge for Demo Release                       │
[develop]       ───●──────────●────────────────────────●──────────●─── (Integration)
                   ▲          ▲                        ▲
                   │          │                        │
[feature/mod1]  ───●──────────┘                        │
[feature/mod2]  ──────────────●────────────────────────┘
[feature/mod3]  ───────────────────────────────────────●
```

### 1.1 Branch Hierarchy
- **`main` (Production / Academic Demo):** Represents stable, thoroughly tested milestone releases ready for faculty demonstration. Direct pushes strictly prohibited.
- **`develop` (Active Integration):** The primary staging branch where completed features are integrated. All feature branches branch from and merge back into `develop`.
- **`feature/<module>-<description>`:** Individual feature branches created by students for specific tasks.
- **`bugfix/<module>-<description>`:** Targeted bug remediation branches branching from `develop`.
- **`hotfix/<description>`:** Emergency patches branching from `main`.

### 1.2 Feature Branch Naming Standard
All branches must adhere strictly to the pattern:  
`<type>/mod<1-5>-<short-kebab-description>`

**Standard Examples:**
- `feature/mod1-session-auth` (Student 1)
- `feature/mod2-vehicle-crud-api` (Student 2)
- `feature/mod2-fuel-efficiency-calc` (Student 2)
- `feature/mod3-trip-state-machine` (Student 3)
- `feature/mod4-shipment-waypoint-crud` (Student 4)
- `feature/mod5-telemetry-websocket` (Student 5)
- `feature/mod5-threejs-fleet-canvas` (Student 5)
- `bugfix/mod3-driver-license-validation`

---

## 2. Commit Message Standards (Conventional Commits)

All commits must follow the **Conventional Commits 1.0.0** specification:

```
<type>(<scope>): <short imperative subject>

[optional body explaining rationale and technical context]

[optional issue reference: e.g., Refs: #42]
```

### 2.1 Allowed Commit Types
- `feat`: A new user-facing or API feature.
- `fix`: A bug repair.
- `docs`: Documentation alterations only.
- `style`: Formatting, semicolons, whitespace (no functional code changes).
- `refactor`: Code restructurings that neither fix a bug nor add a feature.
- `test`: Adding missing unit/integration tests or correcting existing tests.
- `chore`: Build scripts, Maven dependency updates, Flyway migration file sorting.

### 2.2 Allowed Scopes
- `identity` or `auth` (Module 1)
- `fleet` (Module 2)
- `trip` or `driver` (Module 3)
- `shipment` or `route` (Module 4)
- `tracking` or `control-tower` (Module 5)
- `common` (Cross-cutting infrastructure, contracts, DTOs)
- `db` (Flyway SQL scripts)
- `frontend` (React shared components, routing, layout)

### 2.3 Commit Examples
- `feat(fleet): implement automated preventive service warning calculation`
- `fix(trip): reject trip assignment when driver license is expired`
- `feat(tracking): broadcast vehicle location pings over STOMP topic`
- `test(shipment): add unit tests for waypoint distance and ETA computation`
- `docs(api): update OpenAPI request schema for trip creation`

---

## 3. Pull Request (PR) Requirements & Rules

### 3.1 Pull Request Mandates
1. **Target Branch:** All feature PRs must target `develop`. No feature PR may target `main` directly.
2. **Reviewers:** Every PR requires at least **two approvals** before merge:
   - The accountable **Module Owner**.
   - The **Team Leader (Student 1)** OR a peer module owner if an interface contract is touched.
3. **Automated Checks:** Local builds and unit test suites must pass cleanly prior to requesting review.
4. **Clean Merge Strategy:** Squash and Merge (or Rebase Merge) is enforced on `develop` to preserve a linear, intelligible git history for faculty inspection.

### 3.2 PR Description Template
Every pull request opened in GitHub must use the following Markdown template:

```markdown
## Summary of Changes
- Briefly describe the functional problem addressed and the solution implemented.

## Module Affected
- [ ] Module 1: Identity & Admin
- [ ] Module 2: Fleet & Assets
- [ ] Module 3: Driver & Trip Operations
- [ ] Module 4: Shipment & Routes
- [ ] Module 5: Tracking & Control Tower
- [ ] Cross-Cutting / Common Foundation

## Flyway Migrations
- [ ] No database changes in this PR
- [ ] Migration script added within module's reserved range: (e.g., `V201__add_fuel_records.sql`)

## Local Testing Evidence
- [ ] Backend tests passing: `mvn clean test` (attached console summary or screenshot)
- [ ] Frontend tests passing: `npm test` and `npm run lint`
- [ ] Tested manually against local MySQL 8.4 database

## Checklist
- [ ] Adheres to package-by-feature architecture
- [ ] No direct repository imports from external domain modules
- [ ] API endpoints and DTOs match `docs/API_CONTRACTS.md`
- [ ] CSRF and role-based authorization verified
```

---

## 4. Local Developer Verification Protocol

Before pushing code or opening a PR, every student must run the following verification steps on their local machine:

### 4.1 Backend Verification
```bash
# 1. Clean build and run all unit & integration tests
mvn clean test

# 2. Check for compile-time validation errors
mvn compile

# 3. Verify Spring Boot runs cleanly with local MySQL
mvn spring-boot:run
```

### 4.2 Frontend Verification
```bash
# 1. Check TypeScript compilation & linting
npm run lint

# 2. Run component tests
npm test

# 3. Verify production bundle build
npm run build
```

Any compilation failure or broken test immediately halts the PR process. Merging broken code into `develop` results in immediate branch freeze until the responsible student issues a fix.
