import React from 'react';
import { Bell, Search, UserCircle, LogOut, Users, Globe, Shield } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface TopNavProps {
  onToggleSidebar?: () => void;
}

export const TopNav: React.FC<TopNavProps> = () => {
  const navigate = useNavigate();
  const { currentUser, logout, loading } = useAuth();

  const handleChangeAccount = async () => {
    try {
      await logout();
    } catch {
      // Proceed to login screen even if session was already invalidated
    }
    navigate('/login');
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // Proceed to cover page even if session was already invalidated
    }
    navigate('/');
  };

  const isAdmin = currentUser?.roles.includes('ADMIN') ?? false;

  return (
    <header className="fx-topbar">
      {/* Quick Search */}
      <div className="d-flex align-items-center" style={{ maxWidth: '380px', width: '100%' }}>
        <div className="input-group input-group-sm">
          <span
            className="input-group-text border-end-0"
            style={{
              background: 'rgba(48, 33, 28, 0.65)',
              borderColor: 'rgba(232, 211, 162, 0.22)',
              color: 'var(--fx-muted-sand)',
            }}
          >
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
        <div
          className="d-none d-md-flex align-items-center gap-2 px-2.5 py-1 rounded-2 border small"
          style={{
            background: 'rgba(48, 33, 28, 0.65)',
            borderColor: 'rgba(232, 211, 162, 0.2)',
          }}
        >
          <span className="badge fx-badge-gold fw-semibold" style={{ fontSize: '0.7rem' }}>
            Tenant #{currentUser?.tenantId ?? 1}
          </span>
          <span className="fw-semibold" style={{ fontSize: '0.78rem', color: 'var(--fx-ivory-text)' }}>
            {currentUser?.companyName || 'FleetX Logistics'}
          </span>
          {currentUser?.warehouseName && (
            <span style={{ fontSize: '0.72rem', color: 'var(--fx-muted-sand)' }}>
              &bull; {currentUser.warehouseName}
            </span>
          )}
        </div>

        {/* System Status Pill */}
        <div
          className="d-none d-sm-flex align-items-center gap-2 px-2.5 py-1 rounded-2 border small"
          style={{
            background: 'rgba(48, 33, 28, 0.65)',
            borderColor: 'rgba(232, 211, 162, 0.2)',
            color: 'var(--fx-muted-sand)',
          }}
        >
          <span className="fx-pulse-dot" style={{ width: '8px', height: '8px' }}></span>
          <span style={{ fontSize: '0.75rem', fontWeight: 500, color: '#8CE3A7' }}>System Online</span>
        </div>

        {/* Notifications */}
        <button
          className="btn btn-sm position-relative p-2"
          style={{
            background: 'rgba(48, 33, 28, 0.65)',
            border: '1px solid rgba(232, 211, 162, 0.2)',
            color: 'var(--fx-champagne-gold)',
          }}
          title="Operational alerts"
          aria-label="Notifications"
        >
          <Bell size={17} />
          <span
            className="position-absolute top-0 start-100 translate-middle p-1 rounded-circle"
            style={{ background: '#E06C75', border: '1px solid var(--fx-espresso-bg)' }}
          >
            <span className="visually-hidden">New alerts</span>
          </span>
        </button>

        {/* User Profile */}
        <div className="dropdown">
          <button
            className="btn btn-sm d-flex align-items-center gap-2 border px-2.5 py-1 rounded-2"
            style={{
              background: 'rgba(48, 33, 28, 0.65)',
              borderColor: 'rgba(232, 211, 162, 0.22)',
              color: 'var(--fx-ivory-text)',
            }}
            type="button"
            data-bs-toggle="dropdown"
            aria-expanded="false"
          >
            <UserCircle size={22} style={{ color: 'var(--fx-champagne-gold)' }} />
            <div className="text-start d-none d-md-block">
              <div className="fw-semibold small lh-1" style={{ color: 'var(--fx-ivory-text)' }}>
                {loading ? 'Authenticating...' : currentUser?.fullName || 'Guest User'}
              </div>
              <small style={{ fontSize: '0.7rem', color: 'var(--fx-muted-sand)' }}>
                {loading ? '...' : currentUser?.roles?.length ? currentUser.roles.join(', ') : 'NO_ROLE'}
              </small>
            </div>
          </button>
          <ul className="dropdown-menu dropdown-menu-end shadow-lg" style={{ minWidth: '240px' }}>
            <li className="px-3 py-2 border-bottom" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }}>
              <div className="fw-bold small" style={{ color: 'var(--fx-ivory-text)' }}>
                {currentUser?.fullName || 'Guest User'}
              </div>
              <div className="text-truncate small" style={{ color: 'var(--fx-muted-sand)', fontSize: '0.75rem' }}>
                {currentUser?.email}
              </div>
              <div className="badge fx-badge-gold mt-1" style={{ fontSize: '0.65rem' }}>
                Tenant #{currentUser?.tenantId ?? 1} &bull; {currentUser?.companyName || 'Warehouse'}
              </div>
            </li>
            <li>
              <Link className="dropdown-item small d-flex align-items-center gap-2 py-2" to="/">
                <Globe size={15} style={{ color: 'var(--fx-champagne-gold)' }} />
                <span>Public Cover Page</span>
              </Link>
            </li>
            <li>
              <Link className="dropdown-item small d-flex align-items-center gap-2 py-2" to="/profile">
                <UserCircle size={15} style={{ color: 'var(--fx-champagne-gold)' }} />
                <span>Session &amp; Profile</span>
              </Link>
            </li>
            {isAdmin && (
              <li>
                <Link className="dropdown-item small d-flex align-items-center gap-2 py-2" to="/admin/users">
                  <Shield size={15} style={{ color: 'var(--fx-champagne-gold)' }} />
                  <span>Account Settings &amp; Roles</span>
                </Link>
              </li>
            )}
            <li><hr className="dropdown-divider my-1" style={{ borderColor: 'rgba(232, 211, 162, 0.16)' }} /></li>
            <li>
              <button
                className="dropdown-item small d-flex align-items-center gap-2 py-2"
                style={{ color: 'var(--fx-champagne-gold)' }}
                onClick={handleChangeAccount}
                type="button"
                aria-label="Change Account"
              >
                <Users size={15} />
                <span>Change Account</span>
              </button>
            </li>
            <li>
              <button
                className="dropdown-item small d-flex align-items-center gap-2 py-2"
                style={{ color: '#FFA4AC' }}
                onClick={handleLogout}
                type="button"
                aria-label="Log out"
              >
                <LogOut size={15} />
                <span>Log out</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </header>
  );
};
