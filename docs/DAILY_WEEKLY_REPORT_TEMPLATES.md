# FleetX — Intelligent Fleet & Logistics Management System
## Daily & Weekly Academic Reporting Templates

**Document Reference:** FLEETX-REPORTS-2026-V1.0  
**Academic Year:** 2026–27  
**PBL Assessment Standard:** Continuous Faculty Verification & Evidence Audit  
**Baseline Date:** 10 October 2026  
**Status:** Approved Formal Reporting Protocol (Phase 0)

---

## 1. Reporting Guidelines & Evidentiary Standards

To ensure transparent grading and satisfy academic PBL evaluation rubrics, progress claims must be substantiated with **verifiable engineering evidence**:
1. **Commit & PR Traceability:** Every completed task must link to a specific Git commit hash or GitHub Pull Request ID.
2. **Test Execution Evidence:** Claims of working functionality must include automated test results (test count, execution time, pass status).
3. **Artifact References:** Screenshots, schema logs, or API response snippets must be cited where applicable.
4. **Zero Hand-Waving:** Statements such as *"worked on backend"* or *"made progress on UI"* are invalid. Entries must state specific classes, endpoints, or components modified.

---

## 2. Daily Standup / Individual Work Log Template

*Instructions: Each student must complete this log at the conclusion of their daily working session and commit it to `docs/reports/daily/YYYY-MM-DD-student<1-5>.md`.*

```markdown
# Daily Engineering Log — FleetX PBL

**Date:** YYYY-MM-DD  
**Student Name:** [Student Name]  
**Student Roll / ID:** [University Roll Number]  
**Assigned Module:** Module [1/2/3/4/5] — [Module Name]  
**Active Branch:** `feature/mod[X]-[task-name]`

---

### 1. Work Completed Today
- **Task Summary:** [Concise description of the specific engineering task finished]
- **Classes / Files Touched:**
  - `src/main/java/com/fleetx/.../SpecificService.java`
  - `src/test/java/com/fleetx/.../SpecificServiceTest.java`
  - `src/main/resources/db/migration/V[XXX]__description.sql`
- **Git Commit Hash(es):** `[e.g., abc1234]` (Message: `feat(scope): concise message`)
- **Pull Request ID:** PR #[XX] (Target: `develop`)

---

### 2. Verification & Test Evidence
- **Automated Tests Executed:**
  - Total tests run: [X]
  - Tests passing: [X]
  - Failures / Errors: 0
- **Test Command Output Snippet:**
  ```text
  [INFO] Running com.fleetx.fleet.service.VehicleServiceTest
  [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.412 s -- in com.fleetx.fleet.service.VehicleServiceTest
  [INFO] 
  [INFO] Results:
  [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
- **Local Database Verification:**
  - [ ] Flyway migration verified against local MySQL 8.4 LTS
  - [ ] Query / curl result verified against REST endpoint

---

### 3. Plan for Next Working Session
- [Specific component, service, or test to be tackled next]

---

### 4. Blockers & Dependencies
- **Blocker Description:** [None / Describe any technical barrier or contract dependency]
- **Action Required By:** [Self / Student X / Team Leader / Faculty Mentor]
```

---

## 3. Weekly Team Progress Report Template

*Instructions: Prepared weekly by Student 1 (Team Leader) in coordination with Students 2–5. Submitted to the Faculty Guide every Monday morning for review and evaluation grading.*

```markdown
# Weekly Engineering & Progress Audit Report — FleetX

**Report Reference:** FLEETX-WEEKLY-REPORT-[WK-NUM]  
**Date Range:** YYYY-MM-DD to YYYY-MM-DD  
**Academic Year:** 2026–27  
**Submission Date:** YYYY-MM-DD  
**Faculty Mentor / Evaluator:** [Faculty Name]

---

## 1. Executive Summary & Sprint Goal
- **Sprint Goal:** [State the primary objective of the week, e.g., Implement Phase 1 Vehicle and Driver Registries]
- **Sprint Status:** [ON TRACK / MINOR DELAY / BLOCKED]
- **Key Milestones Achieved:**
  - Milestone A: [e.g., Completed V200 Flyway migration and Vehicle CRUD API]
  - Milestone B: [e.g., Integrated Spring Security session auth with React login screen]

---

## 2. Individual Member Contribution & Audit Matrix

| Student Name & ID | Assigned Module | Key Deliverables Completed This Week | Git Commit Hashes | PRs Merged | Test Pass Count |
|---|---|---|---|:---:|:---:|
| **Student 1** (Leader) | Mod 1: Identity & Admin + Foundation | [e.g., Session security, User CRUD, Audit interceptor] | `abc101`, `abc102` | PR #12 | 14 / 14 |
| **Student 2** | Mod 2: Fleet & Assets | [e.g., Vehicle entity, Fuel records, km/L calculation] | `def201`, `def202` | PR #14 | 11 / 11 |
| **Student 3** | Mod 3: Driver & Trip Ops | [e.g., Driver profile, license expiry check, Trip state] | `ghi301`, `ghi302` | PR #15 | 16 / 16 |
| **Student 4** | Mod 4: Shipment & Routes | [e.g., Shipment manifest, Waypoint entity, Haversine ETA] | `jkl401`, `jkl402` | PR #16 | 12 / 12 |
| **Student 5** | Mod 5: Tracking & Control Tower | [e.g., Telemetry simulator worker, WebSocket STOMP] | `mno501`, `mno502` | PR #18 | 9 / 9 |

---

## 3. Quantitative Quality & Verification Summary

| Metric | Target | Current Actual | Status |
|---|:---:|:---:|:---:|
| **Total Automated Tests (Backend + Frontend)** | $\ge 50$ | [e.g., 62] | PASS |
| **Test Pass Rate** | 100% | 100% | PASS |
| **Line Coverage (Service Layer)** | $\ge 75\%$ | [e.g., 81.4%] | PASS |
| **Open Critical Defects** | 0 | 0 | PASS |
| **Build Status (`mvn clean test`)** | Clean | BUILD SUCCESS | PASS |
| **Frontend Lint Status (`npm run lint`)** | 0 Errors | 0 Errors | PASS |

---

## 4. Architectural Boundary Compliance Audit

- [ ] **Package-by-Feature Integrity:** Verified that no cross-module direct repository calls exist.
- [ ] **Contract Inversion:** All inter-module queries route through `VehicleLookupService`, `DriverLookupService`, etc.
- [ ] **Flyway Range Quotas:** Migrations strictly confined to designated prefixes (`V100`–`V500`).
- [ ] **Security Enforcement:** `@PreAuthorize` guards confirmed on all new REST endpoints.

---

## 5. Impediments, Risks & Remediation Plan

| Risk / Blocker Identified | Severity | Impacted Member | Root Cause | Remediation Plan |
|---|:---:|:---:|---|---|
| [e.g., Leaflet map lagging with >100 vehicle pins] | Medium | Student 5 | Excessive DOM re-renders | Implement marker clustering and debounce STOMP ticks. |

---

## 6. Objectives for the Upcoming Week
1. [Target Objective 1]
2. [Target Objective 2]
3. [Target Objective 3]

---

## 7. Formal Review & Faculty Sign-Off

### Student Team Declarations:
*We hereby declare that the contributions detailed above reflect our individual, original engineering work.*

- **Student 1 (Leader):** ___________________________ (Date: _____________)
- **Student 2:** ___________________________ (Date: _____________)
- **Student 3:** ___________________________ (Date: _____________)
- **Student 4:** ___________________________ (Date: _____________)
- **Student 5:** ___________________________ (Date: _____________)

### Faculty Guide Review & Feedback:
- **Evaluation Grade / Rating for Sprint:** [ ] Exceeds Expectations | [ ] Satisfactory | [ ] Needs Improvement
- **Faculty Comments & Guidance:**  
  _____________________________________________________________________________________  
  _____________________________________________________________________________________  
- **Faculty Signature:** ___________________________ (Date: _____________)
```
