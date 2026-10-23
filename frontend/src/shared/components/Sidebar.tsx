import React from 'react';
import { NavLink } from 'react-router-dom';
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
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const { currentUser } = useAuth();
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
      <div className="d-flex align-items-center justify-content-between px-3 py-3 border-bottom">
        <div className="d-flex align-items-center gap-2 overflow-hidden">
          <div className="bg-primary text-white p-2 rounded d-flex align-items-center justify-content-center">
            <Radio size={18} />
          </div>
          {!collapsed && (
            <div>
              <div className="fw-bold fs-6 lh-1 text-slate-900">FleetX</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Logistics Control</small>
            </div>
          )}
        </div>
        <button
          className="btn btn-sm btn-light border-0 p-1 text-muted"
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

        <div className="px-3 pt-3 pb-1 text-uppercase text-muted fw-bold" style={{ fontSize: '0.65rem' }}>
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

        <div className="px-3 pt-3 pb-1 text-uppercase text-muted fw-bold" style={{ fontSize: '0.65rem' }}>
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

        <div className="px-3 pt-3 pb-1 text-uppercase text-muted fw-bold" style={{ fontSize: '0.65rem' }}>
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
      </div>

      {/* Footer Info */}
      {!collapsed && (
        <div className="p-3 border-top bg-light text-muted small" style={{ fontSize: '0.75rem' }}>
          <div><strong>Academic Year:</strong> 2026–27</div>
          <div>Phase 3 Integrated Security</div>
        </div>
      )}
    </aside>
  );
};
