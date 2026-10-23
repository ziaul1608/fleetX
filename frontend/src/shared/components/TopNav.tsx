import React from 'react';
import { Bell, Search, UserCircle, LogOut } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface TopNavProps {
  onToggleSidebar?: () => void;
}

export const TopNav: React.FC<TopNavProps> = () => {
  const navigate = useNavigate();
  const { currentUser, logout, loading } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // Proceed to login screen even if session was already invalidated
    }
    navigate('/login');
  };

  const isAdmin = currentUser?.roles.includes('ADMIN') ?? false;

  return (
    <header className="fx-topbar">
      {/* Quick Search */}
      <div className="d-flex align-items-center" style={{ maxWidth: '380px', width: '100%' }}>
        <div className="input-group input-group-sm">
          <span className="input-group-text bg-white border-end-0 text-muted">
            <Search size={14} />
          </span>
          <input
            type="search"
            className="form-control border-start-0 ps-0"
            placeholder="Search vehicles, trips, shipments (Ctrl + K)..."
            aria-label="Search operational assets"
          />
        </div>
      </div>

      {/* Right Controls */}
      <div className="d-flex align-items-center gap-3">
        {/* Multi-Tenant Workspace Badge */}
        <div className="d-none d-md-flex align-items-center gap-2 px-2 py-1 bg-light rounded border small">
          <span className="badge bg-primary-subtle text-primary fw-semibold" style={{ fontSize: '0.7rem' }}>
            Tenant #{currentUser?.tenantId ?? 1}
          </span>
          <span className="fw-semibold text-slate-800" style={{ fontSize: '0.78rem' }}>
            {currentUser?.companyName || 'FleetX Logistics'}
          </span>
          {currentUser?.warehouseName && (
            <span className="text-muted" style={{ fontSize: '0.72rem' }}>
              &bull; {currentUser.warehouseName}
            </span>
          )}
        </div>

        {/* System Status Pill */}
        <div className="d-none d-sm-flex align-items-center gap-2 px-2 py-1 bg-light rounded border text-muted small">
          <span className="d-inline-block rounded-circle bg-success" style={{ width: '8px', height: '8px' }}></span>
          <span style={{ fontSize: '0.75rem', fontWeight: 500 }}>System Online</span>
        </div>

        {/* Notifications */}
        <button
          className="btn btn-sm btn-light position-relative p-2 text-muted"
          title="Operational alerts"
          aria-label="Notifications"
        >
          <Bell size={18} />
          <span className="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">
            <span className="visually-hidden">New alerts</span>
          </span>
        </button>

        {/* User Profile */}
        <div className="dropdown">
          <button
            className="btn btn-sm btn-light d-flex align-items-center gap-2 border-0 px-2 py-1"
            type="button"
            data-bs-toggle="dropdown"
            aria-expanded="false"
          >
            <UserCircle size={22} className="text-primary" />
            <div className="text-start d-none d-md-block">
              <div className="fw-semibold small lh-1 text-slate-900">
                {loading ? 'Authenticating...' : currentUser?.fullName || 'Guest User'}
              </div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>
                {loading ? '...' : currentUser?.roles?.length ? currentUser.roles.join(', ') : 'NO_ROLE'}
              </small>
            </div>
          </button>
          <ul className="dropdown-menu dropdown-menu-end shadow-sm">
            <li>
              <Link className="dropdown-item small" to="/profile">
                Session &amp; Profile
              </Link>
            </li>
            {isAdmin && (
              <li>
                <Link className="dropdown-item small" to="/admin/users">
                  Account Settings
                </Link>
              </li>
            )}
            <li><hr className="dropdown-divider" /></li>
            <li>
              <button className="dropdown-item small text-danger d-flex align-items-center gap-2" onClick={handleLogout}>
                <LogOut size={14} /> Log out
              </button>
            </li>
          </ul>
        </div>
      </div>
    </header>
  );
};
