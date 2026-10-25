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
  Gauge,
  Sparkles,
} from 'lucide-react';
import { AuthContext } from '../../../shared/context/AuthContext';

export const LandingPage: React.FC = () => {
  const auth = useContext(AuthContext);
  const isAuthenticated = auth?.isAuthenticated ?? false;
  const currentUser = auth?.currentUser;

  return (
    <div className="min-vh-100 d-flex flex-column" style={{ background: 'var(--fx-espresso-bg)', color: 'var(--fx-ivory-text)' }}>
      {/* Ambient background glows */}
      <div className="fx-glow-orb fx-glow-top-left" aria-hidden="true" />
      <div className="fx-glow-orb fx-glow-bottom-right" aria-hidden="true" />

      {/* Top Navigation */}
      <header className="fx-glass-topbar sticky-top py-3" style={{ zIndex: 1030 }}>
        <div className="container d-flex align-items-center justify-content-between">
          {/* Brand */}
          <Link to="/" className="d-flex align-items-center gap-2 text-decoration-none">
            <div
              className="p-2 rounded-3 d-flex align-items-center justify-content-center shadow-sm"
              style={{
                background: 'linear-gradient(135deg, #E8D3A2 0%, #C8753D 100%)',
                color: '#201713',
              }}
            >
              <Radio size={20} strokeWidth={2.5} />
            </div>
            <div className="d-flex align-items-baseline">
              <span className="fw-extrabold fs-4 tracking-tight" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
                Fleet<span style={{ color: 'var(--fx-champagne-gold)' }}>X</span>
              </span>
              <span className="badge fx-badge-gold ms-2 px-2 py-0.5" style={{ fontSize: '0.7rem' }}>
                SaaS v2.0
              </span>
            </div>
          </Link>

          {/* Navigation Links */}
          <nav className="d-none d-lg-flex align-items-center gap-4 small fw-semibold" aria-label="Main Navigation">
            <a href="#overview" className="text-decoration-none fx-link-nav">
              Overview
            </a>
            <a href="#modules" className="text-decoration-none fx-link-nav">
              Platform
            </a>
            <a href="#features" className="text-decoration-none fx-link-nav">
              Capabilities
            </a>
            <a href="#how-it-works" className="text-decoration-none fx-link-nav">
              How It Works
            </a>
            <a href="#security" className="text-decoration-none fx-link-nav">
              Security
            </a>
          </nav>

          {/* Action CTAs */}
          <div className="d-flex align-items-center gap-2">
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn fx-btn-gold btn-sm px-3 py-2 fw-bold d-flex align-items-center gap-2">
                <span>Enter Control Tower</span>
                <ArrowRight size={15} />
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn fx-btn-glass btn-sm px-3 py-2 fw-medium">
                  Sign In
                </Link>
                <Link to="/signup" className="btn fx-btn-gold btn-sm px-3 py-2 fw-bold d-flex align-items-center gap-1">
                  <span>Get Started</span>
                  <ArrowRight size={14} />
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section id="overview" className="py-5 position-relative overflow-hidden">
        <div className="container py-4 text-center position-relative" style={{ zIndex: 1 }}>
          {/* Pill Badge */}
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1.5 fx-glass-card rounded-pill mb-4 border" style={{ borderColor: 'rgba(232, 211, 162, 0.28)' }}>
            <span className="badge fx-badge-amber rounded-pill px-2 py-1 small fw-semibold">
              Intelligent Multi-Tenant Fleet &amp; Logistics Operating System
            </span>
            <span className="small fw-medium d-none d-sm-inline" style={{ color: 'var(--fx-muted-sand)' }}>
              Multi-Tenant Architecture &amp; Passwordless Email OTP Access
            </span>
          </div>

          {/* Headline */}
          <h1
            className="display-4 fw-extrabold mb-3 mx-auto"
            style={{
              maxWidth: '920px',
              letterSpacing: '-0.03em',
              fontWeight: 800,
              color: 'var(--fx-ivory-text)',
            }}
          >
            One Control Tower. Your Entire Fleet.
          </h1>

          {/* Subheading */}
          <p
            className="lead mx-auto mb-4"
            style={{
              maxWidth: '780px',
              fontSize: '1.2rem',
              lineHeight: '1.65',
              color: 'var(--fx-muted-sand)',
            }}
          >
            Manage vehicles, drivers, trips, shipments and fleet operations from one secure workspace.
            Empower your warehouse operations with dedicated private workspaces, real-time GPS telemetry simulations, automated geofence boundary enforcement, and end-to-end trip manifest dispatch.
          </p>

          {/* CTA Buttons */}
          <div className="d-flex flex-wrap justify-content-center gap-3 mb-5">
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn fx-btn-gold btn-lg px-4 py-2.5 fw-bold d-flex align-items-center gap-2 shadow">
                <span>Go to Control Tower ({currentUser?.companyName || 'My Warehouse'})</span>
                <ArrowRight size={18} />
              </Link>
            ) : (
              <>
                <Link
                  to="/signup"
                  className="btn fx-btn-gold btn-lg px-4 py-2.5 fw-bold d-flex align-items-center gap-2 shadow"
                  aria-label="Register Private Warehouse"
                >
                  <Sparkles size={18} />
                  <span>Get Started — Register Private Warehouse</span>
                  <ArrowRight size={18} />
                </Link>
                <Link
                  to="/login"
                  className="btn fx-btn-glass btn-lg px-4 py-2.5 fw-semibold"
                  aria-label="Sign In with Email OTP"
                >
                  <span>Sign In with Email OTP</span>
                </Link>
              </>
            )}
          </div>

          {/* Value Proof Badges */}
          <div
            className="d-flex flex-wrap justify-content-center gap-4 small pt-3 border-top mx-auto"
            style={{
              maxWidth: '900px',
              borderColor: 'rgba(232, 211, 162, 0.16)',
              color: 'var(--fx-muted-sand)',
            }}
          >
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} style={{ color: 'var(--fx-champagne-gold)' }} />
              <span>Strict Zero-Leakage Multi-Tenancy</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} style={{ color: 'var(--fx-champagne-gold)' }} />
              <span>Passwordless 6-Digit OTP Auth</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} style={{ color: 'var(--fx-champagne-gold)' }} />
              <span>Real-Time Autonomous Simulator</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <CheckCircle2 size={16} style={{ color: 'var(--fx-champagne-gold)' }} />
              <span>Safe CSV Report Generation</span>
            </div>
          </div>
        </div>
      </section>

      {/* Floating Control Tower Dashboard Preview */}
      <section className="container mb-5 position-relative" style={{ zIndex: 2 }}>
        <div
          className="fx-glass-panel p-1 rounded-4 overflow-hidden border shadow-lg"
          style={{
            borderColor: 'rgba(232, 211, 162, 0.28)',
            background: 'linear-gradient(180deg, rgba(48, 33, 28, 0.88) 0%, rgba(32, 23, 19, 0.95) 100%)',
          }}
        >
          {/* Window Chrome Header */}
          <div
            className="px-4 py-3 border-bottom d-flex align-items-center justify-content-between"
            style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}
          >
            <div className="d-flex align-items-center gap-2">
              <div className="rounded-circle" style={{ width: '10px', height: '10px', background: '#E06C75' }} />
              <div className="rounded-circle" style={{ width: '10px', height: '10px', background: '#E8D3A2' }} />
              <div className="rounded-circle" style={{ width: '10px', height: '10px', background: '#5CB87A' }} />
              <span className="ms-3 small font-monospace" style={{ color: 'var(--fx-muted-sand)' }}>
                control-tower.fleetx.cloud &bull; Live Telemetry Engine
              </span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <span className="fx-pulse-dot" />
              <span className="badge fx-badge-success small font-monospace">LIVE SIMULATOR ACTIVE</span>
            </div>
          </div>

          {/* Dashboard Canvas Preview */}
          <div className="p-4 p-md-5">
            {/* Metric Cards Row */}
            <div className="row g-4 mb-4">
              <div className="col-6 col-md-3">
                <div className="fx-glass-card p-3 rounded-3 border h-100" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
                  <div className="small mb-1" style={{ color: 'var(--fx-muted-sand)' }}>
                    Active Fleet Units
                  </div>
                  <div className="fs-3 fw-bold" style={{ color: 'var(--fx-ivory-text)' }}>
                    24 Vehicles
                  </div>
                  <div className="small d-flex align-items-center gap-1 mt-1" style={{ color: '#8CE3A7' }}>
                    <CheckCircle2 size={13} /> 100% Inspected
                  </div>
                </div>
              </div>

              <div className="col-6 col-md-3">
                <div className="fx-glass-card p-3 rounded-3 border h-100" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
                  <div className="small mb-1" style={{ color: 'var(--fx-muted-sand)' }}>
                    Ongoing Dispatches
                  </div>
                  <div className="fs-3 fw-bold" style={{ color: 'var(--fx-ivory-text)' }}>
                    8 Trips
                  </div>
                  <div className="small d-flex align-items-center gap-1 mt-1" style={{ color: 'var(--fx-champagne-gold)' }}>
                    <Navigation size={13} /> Live Route GPS
                  </div>
                </div>
              </div>

              <div className="col-6 col-md-3">
                <div className="fx-glass-card p-3 rounded-3 border h-100" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
                  <div className="small mb-1" style={{ color: 'var(--fx-muted-sand)' }}>
                    Active Geofences
                  </div>
                  <div className="fs-3 fw-bold" style={{ color: 'var(--fx-ivory-text)' }}>
                    12 Zones
                  </div>
                  <div className="small d-flex align-items-center gap-1 mt-1" style={{ color: '#FFAE78' }}>
                    <MapPin size={13} /> Circular &amp; Polygon
                  </div>
                </div>
              </div>

              <div className="col-6 col-md-3">
                <div className="fx-glass-card p-3 rounded-3 border h-100" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
                  <div className="small mb-1" style={{ color: 'var(--fx-muted-sand)' }}>
                    Avg Fleet Efficiency
                  </div>
                  <div className="fs-3 fw-bold" style={{ color: 'var(--fx-ivory-text)' }}>
                    4.2 km/L
                  </div>
                  <div className="small d-flex align-items-center gap-1 mt-1" style={{ color: '#8CE3A7' }}>
                    <Gauge size={13} /> Eco-Optimized
                  </div>
                </div>
              </div>
            </div>

            {/* Inner Interactive Visual Banner */}
            <div
              className="p-4 rounded-3 text-center border"
              style={{
                background: 'rgba(70, 49, 40, 0.45)',
                borderColor: 'rgba(232, 211, 162, 0.2)',
              }}
            >
              <div className="d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 text-start">
                <div>
                  <div className="fw-bold mb-1" style={{ color: 'var(--fx-ivory-text)' }}>
                    Private Partition Guarantees
                  </div>
                  <div className="small" style={{ color: 'var(--fx-muted-sand)' }}>
                    Every newly registered warehouse receives an independent, zero-collision data partition with isolated foreign keys and thread-local tenancy.
                  </div>
                </div>
                <Link to="/signup" className="btn fx-btn-gold btn-sm px-4 py-2 fw-semibold text-nowrap">
                  Provision Your Free Warehouse Workspace &rarr;
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 5 PLATFORM MODULES SHOWCASE */}
      <section id="modules" className="py-5 border-top position-relative" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
        <div className="container py-4">
          <div className="text-center mb-5">
            <span className="badge fx-badge-gold px-3 py-1 rounded-pill mb-2 fw-semibold">
              Integrated Platform
            </span>
            <h2 className="fw-extrabold mb-2" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
              5 Core Modules. One Unified Operating System.
            </h2>
            <p className="mx-auto" style={{ maxWidth: '680px', color: 'var(--fx-muted-sand)' }}>
              Everything your warehouse and logistics teams need to manage operations from receipt to final customer delivery.
            </p>
          </div>

          <div className="row g-4">
            {/* Module 1: Identity & Admin */}
            <div className="col-lg-4 col-md-6">
              <div className="fx-glass-card-interactive h-100 p-4 rounded-3 border d-flex flex-column" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{
                    background: 'rgba(232, 211, 162, 0.15)',
                    color: 'var(--fx-champagne-gold)',
                    border: '1px solid rgba(232, 211, 162, 0.3)',
                  }}
                >
                  <Users size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  1. Identity &amp; Admin
                </h5>
                <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
                  Multi-tenant workspace isolation with role-based access control (Admin, Dispatcher, Safety Officer, Driver, Analyst) and immutable audit trail logs.
                </p>
                <ul className="list-unstyled small mt-auto mb-0 d-flex flex-column gap-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-champagne-gold)' }} />
                    <span>Tenant-scoped user management</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-champagne-gold)' }} />
                    <span>Cryptographic session enforcement</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-champagne-gold)' }} />
                    <span>Immutable security audit log</span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 2: Fleet & Asset Management */}
            <div className="col-lg-4 col-md-6">
              <div className="fx-glass-card-interactive h-100 p-4 rounded-3 border d-flex flex-column" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{
                    background: 'rgba(92, 184, 122, 0.16)',
                    color: '#8CE3A7',
                    border: '1px solid rgba(92, 184, 122, 0.32)',
                  }}
                >
                  <Truck size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  2. Fleet &amp; Asset Management
                </h5>
                <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
                  Complete vehicle inventory tracking, inspection checklists, fuel logs, automated maintenance reminders, and dispatch readiness verification.
                </p>
                <ul className="list-unstyled small mt-auto mb-0 d-flex flex-column gap-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#8CE3A7' }} />
                    <span>VIN &amp; plate collision checks</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#8CE3A7' }} />
                    <span>Proactive maintenance scheduling</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#8CE3A7' }} />
                    <span>Fuel efficiency telemetry tracking</span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 3: Driver & Trip Operations */}
            <div className="col-lg-4 col-md-6">
              <div className="fx-glass-card-interactive h-100 p-4 rounded-3 border d-flex flex-column" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{
                    background: 'rgba(200, 117, 61, 0.18)',
                    color: 'var(--fx-burnt-amber)',
                    border: '1px solid rgba(200, 117, 61, 0.35)',
                  }}
                >
                  <Compass size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  3. Driver &amp; Trip Operations
                </h5>
                <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
                  Certified driver management, license validity enforcement, trip creation and dispatching, route status transitions, and safety metrics.
                </p>
                <ul className="list-unstyled small mt-auto mb-0 d-flex flex-column gap-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-burnt-amber)' }} />
                    <span>Driver qualification validation</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-burnt-amber)' }} />
                    <span>Trip manifest dispatch workflow</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-burnt-amber)' }} />
                    <span>Real-time status lifecycle tracking</span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 4: Shipment & Route Management */}
            <div className="col-lg-6 col-md-6">
              <div className="fx-glass-card-interactive h-100 p-4 rounded-3 border d-flex flex-column" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{
                    background: 'rgba(207, 183, 120, 0.16)',
                    color: 'var(--fx-soft-gold)',
                    border: '1px solid rgba(207, 183, 120, 0.3)',
                  }}
                >
                  <Package size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  4. Shipment &amp; Route Management
                </h5>
                <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
                  Shipment manifests, multi-stop route sequencing, delivery progress checkpoints, and automated delay detection algorithms.
                </p>
                <ul className="list-unstyled small mt-auto mb-0 d-flex flex-column gap-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-soft-gold)' }} />
                    <span>Cargo allocation and weight monitoring</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-soft-gold)' }} />
                    <span>Waypoint checkpoint tracking</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: 'var(--fx-soft-gold)' }} />
                    <span>Automated route delay escalation</span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Module 5: Tracking & Control Tower */}
            <div className="col-lg-6 col-md-12">
              <div className="fx-glass-card-interactive h-100 p-4 rounded-3 border d-flex flex-column" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{
                    background: 'rgba(224, 108, 117, 0.16)',
                    color: '#FFA4AC',
                    border: '1px solid rgba(224, 108, 117, 0.32)',
                  }}
                >
                  <Activity size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  5. Tracking &amp; Control Tower
                </h5>
                <p className="small mb-3" style={{ color: 'var(--fx-muted-sand)' }}>
                  Real-time live map telemetry, circular depot &amp; polygonal transit corridor geofences, automated overspeed detection, and instantaneous alert acknowledgement.
                </p>
                <ul className="list-unstyled small mt-auto mb-0 d-flex flex-column gap-2" style={{ color: 'var(--fx-muted-sand)' }}>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#FFA4AC' }} />
                    <span>Live interactive map with route simulation</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#FFA4AC' }} />
                    <span>Polygonal &amp; circular geofence boundary alerts</span>
                  </li>
                  <li className="d-flex align-items-center gap-2">
                    <CheckCircle2 size={14} style={{ color: '#FFA4AC' }} />
                    <span>Sanitized CSV operational report exports</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Core SaaS Capabilities Grid */}
      <section id="features" className="py-5 border-top" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
        <div className="container py-4">
          <div className="text-center mb-5">
            <h2 className="fw-extrabold mb-2" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
              Designed for High-Volume Logistics Workspaces
            </h2>
            <p className="mx-auto" style={{ maxWidth: '640px', color: 'var(--fx-muted-sand)' }}>
              FleetX replaces manual manifests and unorganized fleet sheets with an automated, multi-tenant digital command center.
            </p>
          </div>

          <div className="row g-4">
            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(232, 211, 162, 0.12)', color: 'var(--fx-champagne-gold)' }}
                >
                  <Building2 size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Private Warehouse Workspaces
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Multiple warehouse owners register independently. Every vehicle, driver, trip, and shipment is strictly scoped to your tenant ID with zero data cross-visibility.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(92, 184, 122, 0.14)', color: '#8CE3A7' }}
                >
                  <MailCheck size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Passwordless Email OTP Security
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  No forgotten passwords or leaked credentials. High-entropy 6-digit numeric OTPs dispatched via real SMTP or in-memory mock with rate limiting and 60-second cooldown guards.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(200, 117, 61, 0.16)', color: 'var(--fx-burnt-amber)' }}
                >
                  <Cpu size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Autonomous Telemetry Simulator
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Step vehicle positions forward through waypoint routes with real haversine distance math, heading angles, realistic speed, and boundary transit events.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(207, 183, 120, 0.14)', color: 'var(--fx-soft-gold)' }}
                >
                  <MapPin size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Geofence Boundary Enforcement
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Define circular depots and polygonal transit corridors. Receive automated real-time alerts when vehicles exceed speed limits or exit authorized delivery areas.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(224, 108, 117, 0.14)', color: '#FFA4AC' }}
                >
                  <Truck size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Fleet Fuel &amp; Maintenance Logs
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Track full-tank refueling calculations, odometer deltas, and automated maintenance warning schedules that prevent vehicle dispatch when service is overdue.
                </p>
              </div>
            </div>

            <div className="col-md-4">
              <div className="fx-glass-card h-100 p-4 rounded-3 border" style={{ borderColor: 'rgba(232, 211, 162, 0.18)' }}>
                <div
                  className="p-3 rounded-3 d-inline-flex mb-3 align-self-start"
                  style={{ background: 'rgba(232, 211, 162, 0.12)', color: 'var(--fx-champagne-gold)' }}
                >
                  <Layers size={24} />
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Manifest Audits &amp; CSV Exports
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Export sanitized, injection-guarded CSV reports for fleet utilization, driver ratings, delayed deliveries, and operational audit trails.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4-Step Onboarding Guide */}
      <section id="how-it-works" className="py-5 border-top" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
        <div className="container py-4">
          <div className="text-center mb-5">
            <span className="badge fx-badge-amber px-3 py-1 rounded-pill mb-2 fw-semibold">
              Streamlined Workflow
            </span>
            <h2 className="fw-extrabold" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
              How to Onboard Your Warehouse
            </h2>
            <p style={{ color: 'var(--fx-muted-sand)' }}>Four simple steps to full operational visibility.</p>
          </div>

          <div className="row g-4 text-center">
            {/* Step 1 */}
            <div className="col-md-3 col-6">
              <div className="fx-glass-card p-4 rounded-3 h-100 border" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="d-inline-flex align-items-center justify-content-center rounded-circle fw-extrabold fs-5 mb-3 shadow"
                  style={{
                    width: '52px',
                    height: '52px',
                    background: 'linear-gradient(135deg, #E8D3A2 0%, #CFB778 100%)',
                    color: '#201713',
                  }}
                >
                  1
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Create Workspace
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Register your company and warehouse depot name with work email verification.
                </p>
              </div>
            </div>

            {/* Step 2 */}
            <div className="col-md-3 col-6">
              <div className="fx-glass-card p-4 rounded-3 h-100 border" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="d-inline-flex align-items-center justify-content-center rounded-circle fw-extrabold fs-5 mb-3 shadow"
                  style={{
                    width: '52px',
                    height: '52px',
                    background: 'linear-gradient(135deg, #CFB778 0%, #C8753D 100%)',
                    color: '#201713',
                  }}
                >
                  2
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Add Your Fleet
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Register vehicles with VIN &amp; plate numbers, and enroll licensed drivers.
                </p>
              </div>
            </div>

            {/* Step 3 */}
            <div className="col-md-3 col-6">
              <div className="fx-glass-card p-4 rounded-3 h-100 border" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="d-inline-flex align-items-center justify-content-center rounded-circle fw-extrabold fs-5 mb-3 shadow"
                  style={{
                    width: '52px',
                    height: '52px',
                    background: 'linear-gradient(135deg, #C8753D 0%, #E8D3A2 100%)',
                    color: '#201713',
                  }}
                >
                  3
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Dispatch &amp; Track
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Create trips, allocate cargo shipments, and track route execution with live telemetry.
                </p>
              </div>
            </div>

            {/* Step 4 */}
            <div className="col-md-3 col-6">
              <div className="fx-glass-card p-4 rounded-3 h-100 border" style={{ borderColor: 'rgba(232, 211, 162, 0.2)' }}>
                <div
                  className="d-inline-flex align-items-center justify-content-center rounded-circle fw-extrabold fs-5 mb-3 shadow"
                  style={{
                    width: '52px',
                    height: '52px',
                    background: 'linear-gradient(135deg, #5CB87A 0%, #8CE3A7 100%)',
                    color: '#201713',
                  }}
                >
                  4
                </div>
                <h5 className="fw-bold mb-2" style={{ color: 'var(--fx-ivory-text)' }}>
                  Optimize &amp; Scale
                </h5>
                <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>
                  Monitor geofences, catch delays, analyze fuel economy, and export audit reports.
                </p>
              </div>
            </div>
          </div>

          <div className="text-center mt-5">
            <Link to="/signup" className="btn fx-btn-gold btn-lg px-5 py-2.5 fw-bold shadow">
              Get Started Free &rarr;
            </Link>
          </div>
        </div>
      </section>

      {/* Security & Reliability Section */}
      <section id="security" className="py-5 border-top" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
        <div className="container py-4">
          <div className="row align-items-center g-5">
            <div className="col-lg-6">
              <div
                className="d-inline-flex align-items-center gap-2 px-3 py-1.5 rounded-pill mb-3 small"
                style={{
                  background: 'rgba(232, 211, 162, 0.12)',
                  color: 'var(--fx-champagne-gold)',
                  border: '1px solid rgba(232, 211, 162, 0.28)',
                }}
              >
                <ShieldCheck size={16} /> Enterprise Reliability
              </div>
              <h2 className="fw-extrabold mb-3" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
                Hardened Multi-Tenant Isolation &amp; RBAC
              </h2>
              <p className="mb-4" style={{ color: 'var(--fx-muted-sand)', lineHeight: '1.65' }}>
                FleetX enforces fail-closed tenant-level query filtering and thread-local tenancy contexts across all database transactions. Foreign key cascading and sole-administrator protection ensure your warehouse operations remain safe from inadvertent lockouts or cross-tenant visibility.
              </p>
              <div className="d-flex flex-column gap-3">
                <div className="d-flex align-items-start gap-3">
                  <div
                    className="p-2 rounded-2"
                    style={{ background: 'rgba(92, 184, 122, 0.18)', color: '#8CE3A7' }}
                  >
                    <Lock size={18} />
                  </div>
                  <div>
                    <strong className="d-block" style={{ color: 'var(--fx-ivory-text)' }}>
                      Fail-Closed Tenant Partition
                    </strong>
                    <small style={{ color: 'var(--fx-muted-sand)' }}>
                      Strict thread-local context guards prevent data leaks or unauthenticated tenant operations.
                    </small>
                  </div>
                </div>

                <div className="d-flex align-items-start gap-3">
                  <div
                    className="p-2 rounded-2"
                    style={{ background: 'rgba(92, 184, 122, 0.18)', color: '#8CE3A7' }}
                  >
                    <CheckCircle2 size={18} />
                  </div>
                  <div>
                    <strong className="d-block" style={{ color: 'var(--fx-ivory-text)' }}>
                      Cryptographic SHA-256 HMAC OTP Authentication
                    </strong>
                    <small style={{ color: 'var(--fx-muted-sand)' }}>
                      OTPs are hashed with server HMAC secrets; codes expire in 5 minutes with 60-second cooldown guards.
                    </small>
                  </div>
                </div>

                <div className="d-flex align-items-start gap-3">
                  <div
                    className="p-2 rounded-2"
                    style={{ background: 'rgba(92, 184, 122, 0.18)', color: '#8CE3A7' }}
                  >
                    <Users size={18} />
                  </div>
                  <div>
                    <strong className="d-block" style={{ color: 'var(--fx-ivory-text)' }}>
                      5-Tier Role-Based Access Control
                    </strong>
                    <small style={{ color: 'var(--fx-muted-sand)' }}>
                      Precise authorization boundaries for ADMIN, DISPATCHER, SAFETY_OFFICER, DRIVER, and ANALYST.
                    </small>
                  </div>
                </div>

                <div className="d-flex align-items-start gap-3">
                  <div
                    className="p-2 rounded-2"
                    style={{ background: 'rgba(92, 184, 122, 0.18)', color: '#8CE3A7' }}
                  >
                    <FileText size={18} />
                  </div>
                  <div>
                    <strong className="d-block" style={{ color: 'var(--fx-ivory-text)' }}>
                      Immutable Audit Trail
                    </strong>
                    <small style={{ color: 'var(--fx-muted-sand)' }}>
                      Every authentication attempt, vehicle update, and trip transition is logged with tenant attribution.
                    </small>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-lg-6">
              <div
                className="p-4 p-md-5 rounded-4 border shadow-lg fx-glass-card"
                style={{ borderColor: 'rgba(232, 211, 162, 0.28)' }}
              >
                <h4 className="fw-bold mb-3" style={{ color: 'var(--fx-ivory-text)' }}>
                  Ready to modernize your fleet operations?
                </h4>
                <p className="small mb-4" style={{ color: 'var(--fx-muted-sand)' }}>
                  Join independent warehouse operators who rely on FleetX for accurate dispatching, GPS telemetry tracking, and compliant manifest workflows.
                </p>
                <div className="d-grid gap-3">
                  <Link to="/signup" className="btn fx-btn-gold py-2.5 fw-bold shadow">
                    Create Your Workspace Free &rarr;
                  </Link>
                  <Link to="/login" className="btn fx-btn-glass py-2.5 fw-semibold">
                    Sign In with Registered Email
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer
        className="mt-auto py-4 border-top"
        style={{
          background: 'rgba(32, 23, 19, 0.85)',
          borderColor: 'rgba(232, 211, 162, 0.16)',
        }}
      >
        <div className="container d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 small" style={{ color: 'var(--fx-muted-sand)' }}>
          <div className="d-flex align-items-center gap-2">
            <Radio size={16} style={{ color: 'var(--fx-champagne-gold)' }} />
            <span className="fw-bold" style={{ color: 'var(--fx-ivory-text)' }}>
              FleetX
            </span>
            <span>&bull; Intelligent Fleet &amp; Logistics Management System</span>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span>&copy; {new Date().getFullYear()} FleetX SaaS</span>
            <span>&bull;</span>
            <span>Multi-Tenant SaaS Edition (Phase 11)</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
