import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { UsersPage } from '../features/auth/pages/UsersPage';
import { ProfilePage } from '../features/auth/pages/ProfilePage';
import { authApi } from '../features/auth/api/authApi';
import { AuthProvider, AuthContext } from '../shared/context/AuthContext';
import { ProtectedRoute } from '../shared/components/ProtectedRoute';
import { PageResponse } from '../shared/api/types';
import { UserDto } from '../features/auth/types';

vi.mock('../features/auth/api/authApi', () => ({
  authApi: {
    fetchCsrf: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
    getUsers: vi.fn(),
    createUser: vi.fn(),
    updateUser: vi.fn(),
    updateUserStatus: vi.fn(),
    resetPassword: vi.fn(),
  },
}));

const mockUsers: UserDto[] = [
  {
    userId: 1,
    email: 'admin@fleetx.local',
    fullName: 'System Administrator',
    roles: ['ADMIN'],
    active: true,
    createdAt: '2026-10-10T12:00:00Z',
  },
  {
    userId: 2,
    email: 'driver.john@fleetx.local',
    fullName: 'John Doe',
    roles: ['DRIVER'],
    active: false,
    createdAt: '2026-10-10T12:00:00Z',
  },
];

const mockPageResponse: PageResponse<UserDto> = {
  content: mockUsers,
  pageNumber: 0,
  pageSize: 10,
  totalElements: 2,
  totalPages: 1,
  last: true,
};

const renderLoginPage = () =>
  render(
    <AuthProvider>
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    </AuthProvider>
  );

describe('Identity & Admin Frontend Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.getMe).mockRejectedValue({ status: 401, errorCode: 'UNAUTHORIZED' });
  });

  describe('LoginPage', () => {
    it('renders login form and bootstraps CSRF token on mount', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-csrf-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });

      renderLoginPage();

      expect(screen.getByText(/FleetX Control Tower/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Sign In to Control Tower/i })).toBeInTheDocument();

      await waitFor(() => {
        expect(authApi.fetchCsrf).toHaveBeenCalledTimes(1);
      });
    });

    it('submits valid credentials and triggers login API', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-csrf-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.login).mockResolvedValueOnce(mockUsers[0]);

      renderLoginPage();

      fireEvent.change(screen.getByLabelText(/Email Address/i), {
        target: { value: 'admin@fleetx.local' },
      });
      fireEvent.change(screen.getByLabelText(/Password/i), {
        target: { value: 'SecretPass123!' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Sign In to Control Tower/i }));

      await waitFor(() => {
        expect(authApi.login).toHaveBeenCalledWith({
          email: 'admin@fleetx.local',
          password: 'SecretPass123!',
        });
      });
    });

    it('displays error message when login fails with 401 credentials error', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.login).mockRejectedValueOnce({
        errorCode: 'UNAUTHORIZED',
        message: 'Invalid email or password credentials.',
      });

      renderLoginPage();

      fireEvent.click(screen.getByRole('button', { name: /Sign In to Control Tower/i }));

      await waitFor(() => {
        expect(screen.getByText(/Invalid email or password/i)).toBeInTheDocument();
      });
    });

    it('displays account deactivated alert when deactivated user attempts login', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.login).mockRejectedValueOnce({
        errorCode: 'ACCOUNT_DEACTIVATED',
        message: 'Account has been deactivated.',
      });

      renderLoginPage();

      fireEvent.click(screen.getByRole('button', { name: /Sign In to Control Tower/i }));

      await waitFor(() => {
        expect(screen.getByText(/Your account has been deactivated/i)).toBeInTheDocument();
      });
    });
  });

  describe('ProtectedRoute', () => {
    it('shows loading indicator while verifying session credentials', () => {
      render(
        <AuthContext.Provider
          value={{
            currentUser: null,
            isAuthenticated: false,
            loading: true,
            error: null,
            login: vi.fn(),
            logout: vi.fn(),
            refreshUser: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={['/fleet/vehicles']}>
            <ProtectedRoute>
              <div>Protected Content</div>
            </ProtectedRoute>
          </MemoryRouter>
        </AuthContext.Provider>
      );

      expect(screen.getByText(/Verifying session credentials.../i)).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('redirects unauthenticated users to /login', () => {
      render(
        <AuthContext.Provider
          value={{
            currentUser: null,
            isAuthenticated: false,
            loading: false,
            error: null,
            login: vi.fn(),
            logout: vi.fn(),
            refreshUser: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={['/trips']}>
            <Routes>
              <Route
                path="/trips"
                element={
                  <ProtectedRoute>
                    <div>Protected Content</div>
                  </ProtectedRoute>
                }
              />
              <Route path="/login" element={<div>Login Page Target</div>} />
            </Routes>
          </MemoryRouter>
        </AuthContext.Provider>
      );

      expect(screen.getByText('Login Page Target')).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('denies access when user does not have the required role', () => {
      render(
        <AuthContext.Provider
          value={{
            currentUser: mockUsers[1], // DRIVER
            isAuthenticated: true,
            loading: false,
            error: null,
            login: vi.fn(),
            logout: vi.fn(),
            refreshUser: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={['/admin/users']}>
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <div>Admin Only Content</div>
            </ProtectedRoute>
          </MemoryRouter>
        </AuthContext.Provider>
      );

      expect(screen.getAllByText(/Access Restricted/i).length).toBeGreaterThan(0);
      expect(screen.queryByText('Admin Only Content')).not.toBeInTheDocument();
    });

    it('renders child component when user is authenticated with required role', () => {
      render(
        <AuthContext.Provider
          value={{
            currentUser: mockUsers[0], // ADMIN
            isAuthenticated: true,
            loading: false,
            error: null,
            login: vi.fn(),
            logout: vi.fn(),
            refreshUser: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={['/admin/users']}>
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <div>Admin Only Content</div>
            </ProtectedRoute>
          </MemoryRouter>
        </AuthContext.Provider>
      );

      expect(screen.getByText('Admin Only Content')).toBeInTheDocument();
    });
  });


  describe('UsersPage', () => {
    it('fetches and renders paginated user directory with role badges', async () => {
      vi.mocked(authApi.getUsers).mockResolvedValueOnce(mockPageResponse);

      render(
        <BrowserRouter>
          <UsersPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(authApi.getUsers).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText('System Administrator')).toBeInTheDocument();
      expect(screen.getByText('admin@fleetx.local')).toBeInTheDocument();
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('driver.john@fleetx.local')).toBeInTheDocument();
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByText('DEACTIVATED')).toBeInTheDocument();
    });

    it('opens provision user modal on Provision User button click', async () => {
      vi.mocked(authApi.getUsers).mockResolvedValueOnce(mockPageResponse);

      render(
        <BrowserRouter>
          <UsersPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('System Administrator')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /Provision User/i }));

      expect(screen.getByText(/Provision New User Account/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Initial Password/i)).toBeInTheDocument();
    });
  });

  describe('ProfilePage', () => {
    it('fetches current user session and renders profile details and security guarantees', async () => {
      vi.mocked(authApi.getMe).mockResolvedValueOnce(mockUsers[0]);

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(authApi.getMe).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText('System Administrator')).toBeInTheDocument();
      expect(screen.getByText('admin@fleetx.local')).toBeInTheDocument();
      expect(screen.getByText(/Server-Side Session:/i)).toBeInTheDocument();
      expect(screen.getByText(/CSRF Token Validation:/i)).toBeInTheDocument();
    });

    it('renders unauthorized state when user session has expired (401)', async () => {
      vi.mocked(authApi.getMe).mockRejectedValueOnce({
        status: 401,
        message: 'Authentication required.',
      });

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Access Restricted/i)).toBeInTheDocument();
      });
      expect(screen.getByText(/session has expired/i)).toBeInTheDocument();
    });
  });
});
