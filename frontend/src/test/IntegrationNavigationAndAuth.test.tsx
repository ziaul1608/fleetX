import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TopNav } from '../shared/components/TopNav';
import { Sidebar } from '../shared/components/Sidebar';
import { apiClient } from '../shared/api/client';
import { AuthContext, AuthContextType } from '../shared/context/AuthContext';
import { UserDto } from '../features/auth/types';

const mockAdminUser: UserDto = {
  userId: 1,
  email: 'admin@fleetx.local',
  fullName: 'System Administrator',
  roles: ['ADMIN'],
  active: true,
  createdAt: '2026-10-10T12:00:00Z',
};

const mockDriverUser: UserDto = {
  userId: 2,
  email: 'driver@fleetx.local',
  fullName: 'Rajesh Driver',
  roles: ['DRIVER'],
  active: true,
  createdAt: '2026-10-10T12:00:00Z',
};

const renderWithAuth = (
  ui: React.ReactElement,
  authOverrides?: Partial<AuthContextType>
) => {
  const defaultAuth: AuthContextType = {
    currentUser: mockAdminUser,
    isAuthenticated: true,
    loading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refreshUser: vi.fn(),
    ...authOverrides,
  };

  return render(
    <AuthContext.Provider value={defaultAuth}>
      <MemoryRouter initialEntries={['/']}>{ui}</MemoryRouter>
    </AuthContext.Provider>
  );
};

describe('Phase 7 — Frontend Route Registration, Auth Header & Logout Integration', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders all 5 module navigation links and Identity & Roles in Sidebar for ADMIN user', () => {
    renderWithAuth(<Sidebar collapsed={false} onToggle={() => {}} />, {
      currentUser: mockAdminUser,
    });

    expect(screen.getByRole('link', { name: /Control Tower/i })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /Fleet Inventory/i })).toHaveAttribute('href', '/fleet/vehicles');
    expect(screen.getByRole('link', { name: /Maintenance & Logs/i })).toHaveAttribute('href', '/fleet/maintenance');
    expect(screen.getByRole('link', { name: /Drivers & Rosters/i })).toHaveAttribute('href', '/trips/drivers');
    expect(screen.getByRole('link', { name: /Trip Dispatch/i })).toHaveAttribute('href', '/trips/dispatch');
    expect(screen.getByRole('link', { name: /Shipment Manifests/i })).toHaveAttribute('href', '/shipments');
    expect(screen.getByRole('link', { name: /Route Waypoints/i })).toHaveAttribute('href', '/shipments/waypoints');
    expect(screen.getByRole('link', { name: /Audits & CSV Reports/i })).toHaveAttribute('href', '/tracking/reports');
    expect(screen.getByRole('link', { name: /Identity & Roles/i })).toHaveAttribute('href', '/admin/users');
  });

  it('hides Identity & Roles navigation link and driver rosters in Sidebar for DRIVER user', () => {
    renderWithAuth(<Sidebar collapsed={false} onToggle={() => {}} />, {
      currentUser: mockDriverUser,
    });

    // Permitted operational modules for driver
    expect(screen.getByRole('link', { name: /Control Tower/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Fleet Inventory/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Trip Dispatch/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Shipment Manifests/i })).toBeInTheDocument();

    // Administrative user management and driver rosters are hidden
    expect(screen.queryByRole('link', { name: /Identity & Roles/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Drivers & Rosters/i })).not.toBeInTheDocument();
  });

  it('allows DISPATCHER user to see Drivers & Rosters but hides Identity & Roles', () => {
    const mockDispatcherUser: UserDto = {
      userId: 3,
      email: 'dispatcher@fleetx.local',
      fullName: 'Anita Dispatcher',
      roles: ['DISPATCHER'],
      active: true,
      createdAt: '2026-10-10T12:00:00Z',
    };

    renderWithAuth(<Sidebar collapsed={false} onToggle={() => {}} />, {
      currentUser: mockDispatcherUser,
    });

    expect(screen.getByRole('link', { name: /Drivers & Rosters/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Trip Dispatch/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Identity & Roles/i })).not.toBeInTheDocument();
  });

  it('renders dynamic user identity and roles in TopNav', () => {
    renderWithAuth(<TopNav />, {
      currentUser: mockAdminUser,
    });

    expect(screen.getByText('System Administrator')).toBeInTheDocument();
    expect(screen.getByText('ADMIN')).toBeInTheDocument();
    expect(screen.queryByText('Admin User / ROLE_ADMIN')).not.toBeInTheDocument();
  });

  it('invokes logout when Log out is clicked in TopNav', async () => {
    const logoutMock = vi.fn().mockResolvedValue(undefined);

    renderWithAuth(<TopNav />, {
      currentUser: mockAdminUser,
      logout: logoutMock,
    });

    const logoutBtn = screen.getByRole('button', { name: /Log out/i });
    fireEvent.click(logoutBtn);

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledTimes(1);
    });
  });

  it('attaches X-XSRF-TOKEN header from XSRF-TOKEN cookie and normalizes /v1/ prefix on mutating requests', async () => {
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: 'XSRF-TOKEN=integration-csrf-token-999',
    });

    const handlers = (apiClient.interceptors.request as any).handlers;
    expect(handlers.length).toBeGreaterThan(0);

    const fulfilled = handlers[0].fulfilled;
    const config = await fulfilled({
      url: '/v1/shipments',
      method: 'post',
      headers: {},
    });

    expect(config.url).toBe('/api/v1/shipments');
    expect(config.headers['X-XSRF-TOKEN']).toBe('integration-csrf-token-999');
  });
});

