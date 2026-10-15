# FleetX PBL Evaluation & Live Demonstration Guide

This guide provides an end-to-end presentation script and demonstration workflow for the FleetX PBL team evaluation.

## 1. Demonstration Roles & Pre-Seeded Credentials

| Role | Username / Email | Password | Access Capabilities |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin@fleetx.local` | `DevAdmin123!` | Unrestricted full access across all modules and user provisioning. |
| **Fleet Manager** | `manager@fleetx.local` | `DevManager123!` | Fleet inventory, maintenance scheduling, and fuel telemetry. |
| **Dispatcher** | `dispatcher@fleetx.local` | `DevDispatch123!` | Driver assignment, trip dispatch, shipment manifests, and GPS tracking. |
| **Driver** | `driver.rajesh@fleetx.local` | `DevDriver123!` | View assigned trips and trigger trip status transitions. |
| **Auditor / Viewer** | `viewer@fleetx.local` | `DevViewer123!` | Read-only dashboards and report export; all mutation actions denied (403). |

---

## 2. Pre-Demo Setup & Verification

1. Ensure the backend is running at `http://127.0.0.1:8080`.
2. Ensure the Vite frontend is running at `http://127.0.0.1:5173`.
3. Open Google Chrome or Firefox and navigate to `http://127.0.0.1:5173/login`.

---

## 3. Step-by-Step Demonstration Script

### Scene 1: Secure Authentication & Operational Control Tower
1. **Action**: On the login page (`/login`), sign in with:
   - Email: `admin@fleetx.local`
   - Password: `DevAdmin123!`
2. **Talking Points**:
   - Point out that authentication is backed by server-side Spring Security HTTP sessions (`JSESSIONID`) and double-submit cookie CSRF tokens (`XSRF-TOKEN`).
   - The top navigation bar dynamically displays `System Administrator` and the role badge `ADMIN`.
3. **Control Tower Overview**:
   - The landing page shows real-time KPI stat cards: Active Vehicles, Trips In Transit, Fleet Fuel Average, and Active Alerts.
   - The live Leaflet GIS map displays active vehicle telemetry markers and geofences.

---

### Scene 2: Fleet Asset Management & Fuel Telemetry (Module 2)
1. **Action**: In the sidebar, navigate to **Fleet Inventory** (`/fleet/vehicles`).
2. **Talking Points**:
   - Display the asset table: vehicle registration plates (e.g., `MH-12-FX-1001`), VIN, make, model, capacity, and current operational status (`AVAILABLE`, `IN_TRANSIT`, `MAINTENANCE`).
   - Click **+ Register Vehicle**: demonstrate client and server-side validation (unique VIN, non-negative capacity).
3. **Action**: Navigate to **Maintenance & Logs** (`/fleet/maintenance`).
   - Review odometer-based maintenance scheduling.
   - Point out maintenance threshold warnings (e.g. `SERVICE_DUE` alert when within 500 km of service interval).

---

### Scene 3: Driver Rosters & Trip Dispatch (Module 3)
1. **Action**: Navigate to **Drivers & Rosters** (`/trips/drivers`).
   - View roster list: license numbers, categories (Heavy Commercial Vehicle - HCV), expiry dates, and driver statuses (`AVAILABLE`, `ON_TRIP`).
2. **Action**: Navigate to **Trip Dispatch** (`/trips/dispatch`).
   - Click **+ Create Trip Dispatch**:
     - Trip Code: `TRIP-2026-DEMO`
     - Origin: `Mumbai Central Depot`
     - Destination: `Pune Industrial Gateway`
     - Vehicle: Select an `AVAILABLE` vehicle
     - Driver: Select an `AVAILABLE` driver
   - Dispatch the trip and note status change to `DISPATCHED`.

---

### Scene 4: Shipment Manifests & Haversine Distance (Module 4)
1. **Action**: Navigate to **Shipment Manifests** (`/shipments`).
   - View manifests linked to active trips.
   - Click **+ Create Shipment**: enter tracking number `FX-SHP-DEMO-01`, sender, recipient, weight, and assign to `TRIP-2026-DEMO`.
2. **Action**: Navigate to **Route Waypoints** (`/shipments/waypoints`).
   - Point out the Haversine formula calculation:
     $$\Delta\sigma = 2\arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos\phi_1\cos\phi_2\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)$$
     $$d = R \cdot \Delta\sigma \quad (R \approx 6371\text{ km})$$
   - Display total calculated route distance (e.g., 120.22 km) and estimated transit duration based on 45 km/h commercial average.

---

### Scene 5: GPS Simulation & Real-Time Alerts (Module 5)
1. **Action**: Return to the **Operational Control Tower** (`/`).
2. **Talking Points**:
   - Under the map controls, locate the simulated GPS runner.
   - Explain the simulation engine: it generates deterministic coordinate updates along approved route waypoints without jumping randomly across the map.
   - Start the simulation for the dispatched trip: watch the vehicle move along the transit corridor in real time over STOMP WebSocket (`/ws/fleetx`).
   - Point out automated event generation:
     - Geofence exit alert (`GEOFENCE_EXIT`) as the vehicle leaves depot boundaries.
     - Telemetry updates with validated speed and heading.

---

### Scene 6: Delivery Handover & Trip Completion
1. **Action**: In **Shipment Manifests** (`/shipments`), open details for `FX-SHP-DEMO-01`.
2. **Action**: Click **Mark Delivered**:
   - Status updates from `IN_TRANSIT` to `DELIVERED`.
   - Chronological delivery timeline displays timestamps and audit events.
3. **Action**: In **Trip Dispatch** (`/trips/dispatch`), mark `TRIP-2026-DEMO` as **Complete**:
   - Trip concludes with status `COMPLETED`.
   - Assigned vehicle returns to `AVAILABLE` and driver status returns to `AVAILABLE`.

---

### Scene 7: CSV Audits & Telemetry Reporting
1. **Action**: Navigate to **Audits & CSV Reports** (`/tracking/reports`).
2. **Talking Points**:
   - Displays aggregated fleet KPIs: completed trips, total distance covered, average fuel economy, and alert frequency.
   - Click **Export CSV Report**: download the generated CSV containing timestamped audit logs, vehicle telemetry, and trip summaries.

---

### Scene 8: Security Audit & Least-Privilege Verification (Viewer Role)
1. **Action**: In the top-right user menu, click **Log out**.
2. **Action**: Log in with viewer credentials:
   - Email: `viewer@fleetx.local`
   - Password: `DevViewer123!`
3. **Talking Points**:
   - TopNav reflects `Auditor User` / `VIEWER`.
   - Sidebar automatically hides administrative menus (**Identity & Roles** and **Driver Rosters** are not visible).
   - Attempt a mutation (or trigger a POST via developer tools):
     - Notice that the backend returns `403 Forbidden` (`INSUFFICIENT_PERMISSIONS`).
     - Read-only dashboards and CSV reports continue to function cleanly with `200 OK`.
