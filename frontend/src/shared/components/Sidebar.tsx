import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Truck,
  Users,
  Package,
  Radio,
  FileText,
  Shield,
  ChevronLeft,
  ChevronRight,
  Wrench,
  Navigation,
  MapPin,
  LogOut,
  Globe,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const navigate = useNavigate();
  const { currentUser, logout } = useAuth();
  const roles = currentUser?.roles ?? [];

  const isAdmin = roles.includes('ADMIN');
  const isFleetManager = roles.includes('FLEET_MANAGER');
  const isDispatcher = roles.includes('DISPATCHER');
  const isDriver = roles.includes('DRIVER');
  const isViewer = roles.includes('VIEWER');

  // Role-based visibility flags
  const canViewMaintenance = isAdmin || isFleetManager || isDriver || isViewer;
  const canViewDrivers = isAdmin || isFleetManager || isDispatcher || isViewer;
  const canViewDispatch = isAdmin || isFleetManager || isDispatcher || isDriver || isViewer;
  const canViewShipments = isAdmin || isFleetManager || isDispatcher || isDriver || isViewer;
  const canViewWaypoints = isAdmin || isFleetManager || isDispatcher || isViewer;
  const canViewReports = isAdmin || isFleetManager || isDispatcher || isDriver || isViewer;
  const canViewAdminUsers = isAdmin;

  return (
    <aside className={`fx-sidebar ${collapsed ? 'collapsed' : ''}`} aria-label="Main Navigation">
      {/* Brand Header */}
      <div
        className="d-flex align-items-center justify-content-between px-3 py-3 border-bottom"
        style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}
      >
        <div className="d-flex align-items-center gap-2 overflow-hidden">
          <div
            className="p-2 rounded-2 d-flex align-items-center justify-content-center shadow-sm"
            style={{
              background: 'linear-gradient(135deg, #E8D3A2 0%, #C8753D 100%)',
              color: '#201713',
            }}
          >
            <Radio size={18} strokeWidth={2.5} />
          </div>
          {!collapsed && (
            <div>
              <div className="fw-bold fs-6 lh-1" style={{ color: 'var(--fx-ivory-text)' }}>
                FleetX
              </div>
              <small style={{ fontSize: '0.7rem', color: 'var(--fx-muted-sand)' }}>Logistics Control</small>
            </div>
          )}
        </div>
        <button
          className="btn btn-sm border-0 p-1"
          style={{ background: 'transparent', color: 'var(--fx-muted-sand)' }}
          onClick={onToggle}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      {/* Navigation Groups */}
      <div className="py-2 overflow-y-auto flex-grow-1">
        <NavLink to="/dashboard" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
          <LayoutDashboard size={18} className="icon" />
          <span className="label">Control Tower</span>
        </NavLink>

        <div className="px-3 pt-3 pb-1 text-uppercase fw-bold" style={{ fontSize: '0.65rem', color: 'var(--fx-muted-sand)', letterSpacing: '0.05em' }}>
          {!collapsed && 'Assets & Operations'}
        </div>

        <NavLink to="/fleet/vehicles" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
          <Truck size={18} className="icon" />
          <span className="label">Fleet Inventory</span>
        </NavLink>

        {canViewMaintenance && (
          <NavLink to="/fleet/maintenance" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <Wrench size={18} className="icon" />
            <span className="label">Maintenance &amp; Logs</span>
          </NavLink>
        )}

        {canViewDrivers && (
          <NavLink to="/trips/drivers" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <Users size={18} className="icon" />
            <span className="label">Drivers &amp; Rosters</span>
          </NavLink>
        )}

        {canViewDispatch && (
          <NavLink to="/trips/dispatch" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <Navigation size={18} className="icon" />
            <span className="label">Trip Dispatch</span>
          </NavLink>
        )}

        <div className="px-3 pt-3 pb-1 text-uppercase fw-bold" style={{ fontSize: '0.65rem', color: 'var(--fx-muted-sand)', letterSpacing: '0.05em' }}>
          {!collapsed && 'Logistics & Cargo'}
        </div>

        {canViewShipments && (
          <NavLink to="/shipments" end className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <Package size={18} className="icon" />
            <span className="label">Shipment Manifests</span>
          </NavLink>
        )}

        {canViewWaypoints && (
          <NavLink to="/shipments/waypoints" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <MapPin size={18} className="icon" />
            <span className="label">Route Waypoints</span>
          </NavLink>
        )}

        <div className="px-3 pt-3 pb-1 text-uppercase fw-bold" style={{ fontSize: '0.65rem', color: 'var(--fx-muted-sand)', letterSpacing: '0.05em' }}>
          {!collapsed && 'Intelligence & Admin'}
        </div>

        {canViewReports && (
          <NavLink to="/tracking/reports" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <FileText size={18} className="icon" />
            <span className="label">Audits &amp; CSV Reports</span>
          </NavLink>
        )}

        {canViewAdminUsers && (
          <NavLink to="/admin/users" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
            <Shield size={18} className="icon" />
            <span className="label">Identity &amp; Roles</span>
          </NavLink>
        )}

        <div className="px-3 pt-3 pb-1 text-uppercase fw-bold" style={{ fontSize: '0.65rem', color: 'var(--fx-muted-sand)', letterSpacing: '0.05em' }}>
          {!collapsed && 'Session & Portal'}
        </div>

        <NavLink to="/" className={({ isActive }) => `fx-nav-item ${isActive ? 'active' : ''}`}>
          <Globe size={18} className="icon" />
          <span className="label">Cover Page</span>
        </NavLink>

        <button
          onClick={async () => {
            try { await logout(); } catch {}
            navigate('/login');
          }}
          className="fx-nav-item w-100 text-start border-0 bg-transparent"
          style={{ cursor: 'pointer', color: 'var(--fx-champagne-gold)' }}
          title="Change Account"
          aria-label="Change Account"
          type="button"
        >
          <Users size={18} className="icon" />
          {!collapsed && <span className="label">Change Account</span>}
        </button>

        <button
          onClick={async () => {
            try { await logout(); } catch {}
            navigate('/');
          }}
          className="fx-nav-item w-100 text-start border-0 bg-transparent text-danger-emphasis"
          style={{ cursor: 'pointer' }}
          title="Log Out"
          aria-label="Log Out"
          type="button"
        >
          <LogOut size={18} className="icon" />
          {!collapsed && <span className="label">Log Out</span>}
        </button>
      </div>

      {/* Footer Info */}
      {!collapsed && (
        <div
          className="p-3 border-top small"
          style={{
            borderColor: 'rgba(232, 211, 162, 0.16)',
            fontSize: '0.75rem',
            color: 'var(--fx-muted-sand)',
            background: 'rgba(32, 23, 19, 0.4)',
          }}
        >
          <div><strong style={{ color: 'var(--fx-ivory-text)' }}>Academic Year:</strong> 2026–27</div>
          <div>Phase 3 Integrated Security</div>
        </div>
      )}
    </aside>
  );
};
