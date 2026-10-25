import React, { useContext } from 'react';
import { Link } from 'react-router-dom';
import {
  Radio,
  Building2,
  ShieldCheck,
  Navigation,
  Truck,
  ArrowRight,
  CheckCircle2,
  Layers,
  MapPin,
  Cpu,
  MailCheck,
  Users,
  Compass,
  Package,
  Activity,
  FileText,
  Lock,
} from 'lucide-react';
import { AuthContext } from '../../../shared/context/AuthContext';

export const LandingPage: React.FC = () => {
  const auth = useContext(AuthContext);
  const isAuthenticated = auth?.isAuthenticated ?? false;
  const currentUser = auth?.currentUser;

  return (
    <div className="min-vh-100 d-flex flex-column bg-white text-slate-900" style={{ fontFamily: 'Inter, sans-serif' }}>
      {/* Top Navigation */}
      <header className="border-bottom sticky-top bg-white bg-opacity-95 backdrop-blur shadow-sm" style={{ zIndex: 1020 }}>
        <div className="container py-3 d-flex align-items-center justify-content-between">
          <div className="d-flex align-items-center gap-2">
            <div className="bg-primary text-white p-2 rounded d-flex align-items-center justify-content-center shadow-sm">
              <Radio size={20} />
            </div>
            <div>
              <span className="fw-bold fs-5 text-dark">FleetX</span>
              <span className="badge bg-primary-subtle text-primary border ms-2 small">SaaS v2.0</span>
            </div>
          </div>

          <nav className="d-none d-md-flex align-items-center gap-4 text-secondary small fw-medium">
            <a href="#features" className="text-decoration-none text-secondary hover-primary">
              Features
            </a>
            <a href="#modules" className="text-decoration-none text-secondary hover-primary">
              Platform
            </a>
            <a href="#how-it-works" className="text-decoration-none text-secondary hover-primary">
              How It Works
            </a>
            <a href="#security" className="text-decoration-none text-secondary hover-primary">
              Security
            </a>
          </nav>

          <div className="d-flex align-items-center gap-2">
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn btn-primary btn-sm px-3 py-2 fw-semibold d-flex align-items-center gap-2">
                <span>Enter Control Tower</span>
                <ArrowRight size={15} />
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-outline-secondary btn-sm px-3 py-2 fw-medium">
                  Sign In
                </Link>
                <Link to="/signup" className="btn btn-primary btn-sm px-3 py-2 fw-semibold d-flex align-items-center gap-1">
                  <span>Get Started</span>
                  <ArrowRight size={14} />
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="py-5 bg-gradient-to-b from-blue-50 to-white" style={{ background: 'linear-gradient(180deg, #f0f7ff 0%, #ffffff 100%)' }}>
        <div className="container py-4 text-center">
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 bg-white border rounded-pill shadow-sm mb-4">
            <span className="badge bg-primary rounded-pill px-2 py-1 small">Intelligent Multi-Tenant Fleet &amp; Logistics Operating System</span>
            <span className="small text-secondary fw-medium">
              Multi-Tenant Architecture &amp; Passwordless Email OTP Access
            </span>
          </div>

          <h1 className="display-4 fw-extrabold text-slate-900 mb-3 mx-auto" style={{ maxWidth: '850px', letterSpacing: '-0.02em', fontWeight: 800 }}>
            One Control Tower. Your Entire Fleet.
          </h1>

          <p className="lead text-muted mx-auto mb-4" style={{ maxWidth: '750px', fontSize: '1.2rem', lineHeight: '1.6' }}>
            Manage vehicles, drivers, trips, shipments and fleet operations from one secure workspace.
            Empower your warehouse operations with dedicated private workspaces, real-time GPS telemetry simulations, automated geofence boundary enforcement, and end-to-end trip manifest dispatch.
          </p>

          <div className="d-flex flex-wrap justify-content-center gap-3 mb-5">
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn btn-primary btn-lg px-4 py-2 fw-bold d-flex align-items-center gap-2 shadow">
                <span>Go to Control Tower ({currentUser?.companyName || 'My Warehouse'})</span>
                <ArrowRight size={18} />
              </Link>
            ) : (
              <>
                <Link to="/signup" className="btn btn-primary btn-lg px-4 py-2 fw-bold d-flex align-items-center gap-2 shadow" aria-label="Register Private Warehouse">
                  <span>Get Started — Register Private Warehouse</span>
                  <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-outline-dark btn-lg px-4 py-2 fw-semibold" aria-label="Sign In with Email OTP">
                  <span>Sign In with Email OTP</span>
                </Link>
              </>
            )}
          </div>

          {/* Value Proof Badges */}
          <div className="d-flex flex-wrap justify-content-center gap-4 text-secondary small pt-3 border-top mx-auto" style={{ maxWidth: '850px' }}>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} className="text-success" />
              <span>Strict Zero-Leakage Multi-Tenancy</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} className="text-success" />
              <span>Passwordless 6-Digit OTP Auth</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} className="text-success" />
              <span>Real-Time Autonomous Simulator</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} className="text-success" />
              <span>Safe CSV Report Generation</span>
            </div>
          </div>
        </div>
      </section>

      {/* Control Tower Dashboard Preview Card */}
      <section className="container mb-5">
        <div className="card border-0 shadow-lg rounded-4 overflow-hidden" style={{ background: '#0f172a', color: '#f8fafc' }}>
          <div className="px-4 py-3 border-bottom border-secondary border-opacity-25 d-flex align-items-center justify-content-between">
            <div className="d-flex align-items-center gap-2">
              <div className="rounded-circle bg-danger" style={{ width: '10px', height: '10px' }} />
              <div className="rounded-circle bg-warning" style={{ width: '10px', height: '10px' }} />
              <div className="rounded-circle bg-success" style={{ width: '10px', height: '10px' }} />
              <span className="ms-2 small text-muted font-monospace">control-tower.fleetx.cloud</span>
            </div>
            <div className="badge bg-success-subtle text-success small font-monospace">LIVE SIMULATOR ACTIVE</div>
          </div>
          <div className="p-4 p-md-5">
            <div className="row g-4 mb-4">
              <div className="col-6 col-md-3">
                <div className="p-3 rounded-3 bg-slate-800 border border-secondary border-opacity-25" style={{ background: '#1e293b' }}>
                  <div className="text-muted small mb-1">Active Fleet Units</div>
                  <div className="fs-3 fw-bold text-white">24 Vehicles</div>
                  <div className="text-success small d-flex align-items-center gap-1 mt-1">
                    <CheckCircle2 size={12} /> 100% Inspected
                  </div>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="p-3 rounded-3 bg-slate-800 border border-secondary border-opacity-25" style={{ background: '#1e293b' }}>
                  <div className="text-muted small mb-1">Ongoing Dispatches</div>
                  <div className="fs-3 fw-bold text-white">8 Trips</div>
                  <div className="text-info small d-flex align-items-center gap-1 mt-1">
                    <Navigation size={12} /> Live Route GPS
                  </div>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="p-3 rounded-3 bg-slate-800 border border-secondary border-opacity-25" style={{ background: '#1e293b' }}>
                  <div className="text-muted small mb-1">Active Geofences</div>
                  <div className="fs-3 fw-bold text-white">12 Zones</div>
                  <div className="text-warning small d-flex align-items-center gap-1 mt-1">
                    <MapPin size={12} /> Circular &amp; Polygon
                  </div>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="p-3 rounded-3 bg-slate-800 border border-secondary border-opacity-25" style={{ background: '#1e293b' }}>
                  <div className="text-muted small mb-1">Avg Fleet Efficiency</div>
                  <div className="fs-3 fw-bold text-white">4.2 km/L</div>
                  <div className="text-success small d-flex align-items-center gap-1 mt-1">
                    <CheckCircle2 size={12} /> Eco-Optimized
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-3 text-center border border-secondary border-opacity-25" style={{ background: '#1e293b' }}>
              <p className="mb-2 text-slate-300 small">
                Every newly registered warehouse receives an independent, zero-collision data partition.
              </p>
              <Link to="/signup" className="btn btn-outline-light btn-sm px-3 fw-semibold">
                Provision Your Free Warehouse Workspace &rarr;
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 5 PLATFORM MODULES SHOWCASE */}
      <section id="modules" className="py-5 bg-white border-top">
        <div className="container py-4">
          <div className="text-center mb-5">
            <span className="badge bg-primary-subtle text-primary fw-semibold px-3 py-1 rounded-pill mb-2">Integrated Platform</span>
            <h2 className="fw-bold text-slate-900 mb-2">5 Core Modules. One Unified Operating System.</h2>
            <p className="text-muted mx-auto" style={{ maxWidth: '650px' }}>
              Everything your warehouse and logistics teams need to manage operations from receipt to final customer delivery.
            </p>
          </div>

          <div className="row g-4">
            {/* Module 1: Identity & Admin */}
            <div className="col-lg-4 col-md-6">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-light hover-shadow transition">
                <div className="p-3 bg-primary bg-opacity-10 text-primary rounded-3 d-inline-flex mb-3 align-self-start">
                  <Users size={24} />
                </div>
                <h5 className="fw-bold mb-2">1. Identity &amp; Admin</h5>
                <p className="text-muted small mb-3">
                  Multi-tenant workspace isolation with role-based access control (Admin, Dispatcher, Safety Officer, Driver, Analyst) and immutable audit trail logs.
                </p>
                <ul className="list-unstyled small text-secondary mt-auto mb-0 d-flex flex-column gap-1">
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-primary" /> Tenant-scoped user management
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-primary" /> Cryptographic session enforcement
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-primary" /> Immutable security audit log
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 2: Fleet & Asset Management */}
            <div className="col-lg-4 col-md-6">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-light hover-shadow transition">
                <div className="p-3 bg-success bg-opacity-10 text-success rounded-3 d-inline-flex mb-3 align-self-start">
                  <Truck size={24} />
                </div>
                <h5 className="fw-bold mb-2">2. Fleet &amp; Asset Management</h5>
                <p className="text-muted small mb-3">
                  Complete vehicle inventory tracking, inspection checklists, fuel logs, automated maintenance reminders, and dispatch readiness verification.
                </p>
                <ul className="list-unstyled small text-secondary mt-auto mb-0 d-flex flex-column gap-1">
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-success" /> VIN &amp; plate collision checks
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-success" /> Proactive maintenance scheduling
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-success" /> Fuel efficiency telemetry tracking
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 3: Driver & Trip Operations */}
            <div className="col-lg-4 col-md-6">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-light hover-shadow transition">
                <div className="p-3 bg-info bg-opacity-10 text-info rounded-3 d-inline-flex mb-3 align-self-start">
                  <Compass size={24} />
                </div>
                <h5 className="fw-bold mb-2">3. Driver &amp; Trip Operations</h5>
                <p className="text-muted small mb-3">
                  Certified driver management, license validity enforcement, trip creation and dispatching, route status transitions, and safety metrics.
                </p>
                <ul className="list-unstyled small text-secondary mt-auto mb-0 d-flex flex-column gap-1">
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-info" /> Driver qualification validation
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-info" /> Trip manifest dispatch workflow
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-info" /> Real-time status lifecycle tracking
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 4: Shipment & Route Management */}
            <div className="col-lg-6 col-md-6">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-light hover-shadow transition">
                <div className="p-3 bg-warning bg-opacity-10 text-warning rounded-3 d-inline-flex mb-3 align-self-start">
                  <Package size={24} />
                </div>
                <h5 className="fw-bold mb-2">4. Shipment &amp; Route Management</h5>
                <p className="text-muted small mb-3">
                  Shipment manifests, multi-stop route sequencing, delivery progress checkpoints, and automated delay detection algorithms.
                </p>
                <ul className="list-unstyled small text-secondary mt-auto mb-0 d-flex flex-column gap-1">
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-warning" /> Cargo allocation and weight monitoring
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-warning" /> Waypoint checkpoint tracking
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-warning" /> Automated route delay escalation
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 5: Tracking & Control Tower */}
            <div className="col-lg-6 col-md-12">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-light hover-shadow transition">
                <div className="p-3 bg-danger bg-opacity-10 text-danger rounded-3 d-inline-flex mb-3 align-self-start">
                  <Activity size={24} />
                </div>
                <h5 className="fw-bold mb-2">5. Tracking &amp; Control Tower</h5>
                <p className="text-muted small mb-3">
                  Real-time live map telemetry, circular depot &amp; polygonal transit corridor geofences, automated overspeed detection, and instantaneous alert acknowledgement.
                </p>
                <ul className="list-unstyled small text-secondary mt-auto mb-0 d-flex flex-column gap-1">
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-danger" /> Live interactive map with route simulation
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-danger" /> Polygonal &amp; circular geofence boundary alerts
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={13} className="text-danger" /> Sanitized CSV operational report exports
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Core SaaS Capabilities */}
      <section id="features" className="py-5 bg-light border-top border-bottom">
        <div className="container py-4">
          <div className="text-center mb-5">
            <h2 className="fw-bold text-slate-900 mb-2">Designed for High-Volume Logistics Workspaces</h2>
            <p className="text-muted mx-auto" style={{ maxWidth: '600px' }}>
              FleetX replaces manual manifests and unorganized fleet sheets with an automated, multi-tenant digital command center.
            </p>
          </div>

          <div className="row g-4">
            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-primary bg-opacity-10 text-primary rounded-3 d-inline-flex mb-3 align-self-start">
                  <Building2 size={24} />
                </div>
                <h5 className="fw-bold mb-2">Private Warehouse Workspaces</h5>
                <p className="text-muted small mb-0">
                  Multiple warehouse owners register independently. Every vehicle, driver, trip, and shipment is strictly scoped to your tenant ID with zero data cross-visibility.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-success bg-opacity-10 text-success rounded-3 d-inline-flex mb-3 align-self-start">
                  <MailCheck size={24} />
                </div>
                <h5 className="fw-bold mb-2">Passwordless Email OTP Security</h5>
                <p className="text-muted small mb-0">
                  No forgotten passwords or leaked credentials. High-entropy 6-digit numeric OTPs dispatched via real SMTP or in-memory mock with rate limiting and 60-second cooldown guards.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-info bg-opacity-10 text-info rounded-3 d-inline-flex mb-3 align-self-start">
                  <Cpu size={24} />
                </div>
                <h5 className="fw-bold mb-2">Autonomous Telemetry Simulator</h5>
                <p className="text-muted small mb-0">
                  Step vehicle positions forward through waypoint routes with real haversine distance math, heading angles, realistic speed, and boundary transit events.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-warning bg-opacity-10 text-warning rounded-3 d-inline-flex mb-3 align-self-start">
                  <MapPin size={24} />
                </div>
                <h5 className="fw-bold mb-2">Geofence Boundary Enforcement</h5>
                <p className="text-muted small mb-0">
                  Define circular depots and polygonal transit corridors. Receive automated real-time alerts when vehicles exceed speed limits or exit authorized delivery areas.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-danger bg-opacity-10 text-danger rounded-3 d-inline-flex mb-3 align-self-start">
                  <Truck size={24} />
                </div>
                <h5 className="fw-bold mb-2">Fleet Fuel &amp; Maintenance Logs</h5>
                <p className="text-muted small mb-0">
                  Track full-tank refueling calculations, odometer deltas, and automated maintenance warning schedules that prevent vehicle dispatch when service is overdue.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card h-100 p-4 border shadow-sm rounded-3 bg-white">
                <div className="p-3 bg-secondary bg-opacity-10 text-secondary rounded-3 d-inline-flex mb-3 align-self-start">
                  <Layers size={24} />
                </div>
                <h5 className="fw-bold mb-2">Manifest Audits &amp; CSV Exports</h5>
                <p className="text-muted small mb-0">
                  Export sanitized, injection-guarded CSV reports for fleet utilization, driver ratings, delayed deliveries, and operational audit trails.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How it Works / 4-Step Onboarding Guide */}
      <section id="how-it-works" className="py-5 bg-white">
        <div className="container py-4">
          <div className="text-center mb-5">
            <span className="badge bg-primary-subtle text-primary fw-semibold px-3 py-1 rounded-pill mb-2">Streamlined Workflow</span>
            <h2 className="fw-bold text-slate-900">How to Onboard Your Warehouse</h2>
            <p className="text-muted">Four simple steps to full operational visibility.</p>
          </div>

          <div className="row g-4 text-center">
            <div className="col-md-3 col-6">
              <div className="p-4 border rounded-3 h-100 bg-light shadow-sm">
                <div className="d-inline-flex align-items-center justify-content-center bg-primary text-white rounded-circle fw-bold fs-5 mb-3" style={{ width: '48px', height: '48px' }}>
                  1
                </div>
                <h5 className="fw-bold mb-2">Create Workspace</h5>
                <p className="text-muted small">
                  Register your company and warehouse depot name with work email verification.
                </p>
              </div>
            </div>

            <div className="col-md-3 col-6">
              <div className="p-4 border rounded-3 h-100 bg-light shadow-sm">
                <div className="d-inline-flex align-items-center justify-content-center bg-primary text-white rounded-circle fw-bold fs-5 mb-3" style={{ width: '48px', height: '48px' }}>
                  2
                </div>
                <h5 className="fw-bold mb-2">Add Your Fleet</h5>
                <p className="text-muted small">
                  Register vehicles with VIN &amp; plate numbers, and enroll licensed drivers.
                </p>
              </div>
            </div>

            <div className="col-md-3 col-6">
              <div className="p-4 border rounded-3 h-100 bg-light shadow-sm">
                <div className="d-inline-flex align-items-center justify-content-center bg-primary text-white rounded-circle fw-bold fs-5 mb-3" style={{ width: '48px', height: '48px' }}>
                  3
                </div>
                <h5 className="fw-bold mb-2">Dispatch &amp; Track</h5>
                <p className="text-muted small">
                  Create trips, allocate cargo shipments, and track route execution with live telemetry.
                </p>
              </div>
            </div>

            <div className="col-md-3 col-6">
              <div className="p-4 border rounded-3 h-100 bg-light shadow-sm">
                <div className="d-inline-flex align-items-center justify-content-center bg-primary text-white rounded-circle fw-bold fs-5 mb-3" style={{ width: '48px', height: '48px' }}>
                  4
                </div>
                <h5 className="fw-bold mb-2">Optimize &amp; Scale</h5>
                <p className="text-muted small">
                  Monitor geofences, catch delays, analyze fuel economy, and export audit reports.
                </p>
              </div>
            </div>
          </div>

          <div className="text-center mt-5">
            <Link to="/signup" className="btn btn-primary btn-lg px-5 py-2 fw-bold shadow">
              Get Started Free &rarr;
            </Link>
          </div>
        </div>
      </section>

      {/* Security & Reliability */}
      <section id="security" className="py-5 bg-slate-900 text-white" style={{ background: '#0f172a' }}>
        <div className="container py-4">
          <div className="row align-items-center g-5">
            <div className="col-lg-6">
              <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-slate-800 border border-secondary text-info small mb-3">
                <ShieldCheck size={16} /> Enterprise Reliability
              </div>
              <h2 className="fw-bold mb-3">Hardened Multi-Tenant Isolation &amp; RBAC</h2>
              <p className="text-slate-300 mb-4">
                FleetX enforces fail-closed tenant-level query filtering and thread-local tenancy contexts across all database transactions. Foreign key cascading and sole-administrator protection ensure your warehouse operations remain safe from inadvertent lockouts or cross-tenant visibility.
              </p>
              <div className="d-flex flex-column gap-3">
                <div className="d-flex align-items-center gap-3">
                  <div className="p-2 rounded bg-slate-800 text-success">
                    <Lock size={18} />
                  </div>
                  <div>
                    <strong className="d-block text-white">Fail-Closed Tenant Partition</strong>
                    <small className="text-slate-400">Strict thread-local context guards prevent data leaks or unauthenticated tenant operations.</small>
                  </div>
                </div>
                <div className="d-flex align-items-center gap-3">
                  <div className="p-2 rounded bg-slate-800 text-success">
                    <CheckCircle2 size={18} />
                  </div>
                  <div>
                    <strong className="d-block text-white">Cryptographic SHA-256 HMAC OTP Authentication</strong>
                    <small className="text-slate-400">OTPs are hashed with server HMAC secrets; codes expire in 5 minutes with 60-second cooldown guards.</small>
                  </div>
                </div>
                <div className="d-flex align-items-center gap-3">
                  <div className="p-2 rounded bg-slate-800 text-success">
                    <Users size={18} />
                  </div>
                  <div>
                    <strong className="d-block text-white">5-Tier Role-Based Access Control</strong>
                    <small className="text-slate-400">Precise authorization boundaries for ADMIN, DISPATCHER, SAFETY_OFFICER, DRIVER, and ANALYST.</small>
                  </div>
                </div>
                <div className="d-flex align-items-center gap-3">
                  <div className="p-2 rounded bg-slate-800 text-success">
                    <FileText size={18} />
                  </div>
                  <div>
                    <strong className="d-block text-white">Immutable Audit Trail</strong>
                    <small className="text-slate-400">Every authentication attempt, vehicle update, and trip transition is logged with tenant attribution.</small>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-lg-6">
              <div className="p-4 p-md-5 rounded-4 border border-secondary border-opacity-25 shadow-lg" style={{ background: '#1e293b' }}>
                <h4 className="fw-bold text-white mb-3">Ready to modernize your fleet operations?</h4>
                <p className="text-slate-300 small mb-4">
                  Join independent warehouse operators who rely on FleetX for accurate dispatching, GPS telemetry tracking, and compliant manifest workflows.
                </p>
                <div className="d-grid gap-3">
                  <Link to="/signup" className="btn btn-primary py-2 fw-semibold">
                    Get Started Free
                  </Link>
                  <Link to="/login" className="btn btn-outline-light py-2 fw-semibold">
                    Sign In with Registered Email
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto py-4 bg-light border-top">
        <div className="container d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 text-muted small">
          <div className="d-flex align-items-center gap-2">
            <Radio size={16} className="text-primary" />
            <span className="fw-bold text-slate-800">FleetX</span>
            <span>&bull; Intelligent Fleet &amp; Logistics Management System</span>
          </div>
          <div>
            <span>Academic PBL Demonstration &bull; Multi-Tenant SaaS Edition (Phase 11)</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
