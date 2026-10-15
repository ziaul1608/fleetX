import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { App } from '../app/App';
import { authApi } from '../features/auth/api/authApi';

vi.mock('../features/auth/api/authApi', () => ({
  authApi: {
    getMe: vi.fn(),
    fetchCsrf: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

describe('FleetX Frontend UI Shell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.getMe).mockResolvedValue({
      userId: 1,
      email: 'admin@fleetx.local',
      fullName: 'System Administrator',
      roles: ['ADMIN'],
      active: true,
      createdAt: '2026-10-10T12:00:00Z',
    });
    vi.mocked(authApi.fetchCsrf).mockResolvedValue({
      token: 'test-token',
      headerName: 'X-CSRF-TOKEN',
      parameterName: '_csrf',
    });
  });

  it('renders the main Control Tower brand and dashboard header when authenticated', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText(/Operational Control Tower/i)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/FleetX/i).length).toBeGreaterThan(0);
  });

  it('renders essential navigation links in the sidebar for admin user', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText(/Fleet Inventory/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Drivers & Rosters/i)).toBeInTheDocument();
    expect(screen.getByText(/Shipment Manifests/i)).toBeInTheDocument();
    expect(screen.getByText(/Audits & CSV Reports/i)).toBeInTheDocument();
    expect(screen.getByText(/Identity & Roles/i)).toBeInTheDocument();
  });

  it('renders KPI Stat Cards on the dashboard', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText(/Active Vehicles/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Trips In Transit/i)).toBeInTheDocument();
    expect(screen.getByText(/Fleet Fuel Avg/i)).toBeInTheDocument();
    expect(screen.getByText(/Active Alerts/i)).toBeInTheDocument();
  });
});

