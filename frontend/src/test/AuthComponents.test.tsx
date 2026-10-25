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
    fetchCsrf: vi.fn().mockResolvedValue({ token: 'test', headerName: 'X-CSRF', parameterName: '_csrf' }),
    login: vi.fn(),
    sendLoginOtp: vi.fn(),
    verifyLoginOtp: vi.fn(),
    sendSignupOtp: vi.fn(),
    verifySignupOtp: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
    getUsers: vi.fn(),
    createUser: vi.fn(),
    updateUser: vi.fn(),
    updateUserStatus: vi.fn(),
    resetPassword: vi.fn(),
    changePassword: vi.fn(),
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
    vi.mocked(authApi.fetchCsrf).mockResolvedValue({ token: 'test', headerName: 'X-CSRF', parameterName: '_csrf' });
    vi.mocked(authApi.getMe).mockRejectedValue({ status: 401, errorCode: 'UNAUTHORIZED' });
  });

  describe('LoginPage', () => {
    it('renders passwordless login form and bootstraps CSRF token on mount', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-csrf-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });

      renderLoginPage();

      expect(screen.getByText(/FleetX Control Tower/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Registered Work Email/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Send Login Code/i })).toBeInTheDocument();
      expect(screen.queryByLabelText(/^Password/i)).not.toBeInTheDocument();

      await waitFor(() => {
        expect(authApi.fetchCsrf).toHaveBeenCalledTimes(1);
      });
    });

    it('submits valid email, sends login code, and displays verification step', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-csrf-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.sendLoginOtp).mockResolvedValueOnce({
        email: 'owner@warehouse.test',
        purpose: 'LOGIN',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'If an account is associated with this email, a verification code has been dispatched.',
      });

      renderLoginPage();

      fireEvent.change(screen.getByLabelText(/Registered Work Email/i), {
        target: { value: 'owner@warehouse.test' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Send Login Code/i }));

      await waitFor(() => {
        expect(authApi.sendLoginOtp).toHaveBeenCalledWith({
          email: 'owner@warehouse.test',
        });
      });

      expect(await screen.findByLabelText(/Enter 6-Digit Code/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Verify & Enter Warehouse/i })).toBeInTheDocument();
    });

    it('renders email input empty by default with no development credentials or password hints', () => {
      renderLoginPage();
      expect(screen.getByLabelText(/Registered Work Email/i)).toHaveValue('');
      expect(screen.queryByLabelText(/^Password/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Development Admin/i)).not.toBeInTheDocument();
    });

    it('displays error message when login OTP verification fails with incorrect code', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.sendLoginOtp).mockResolvedValueOnce({
        email: 'owner@warehouse.test',
        purpose: 'LOGIN',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'Verification code dispatched.',
      });
      vi.mocked(authApi.verifyLoginOtp).mockRejectedValueOnce({
        errorCode: 'INCORRECT_OTP',
        message: 'Incorrect verification code. Attempts remaining: 4',
      });

      renderLoginPage();

      fireEvent.change(screen.getByLabelText(/Registered Work Email/i), {
        target: { value: 'owner@warehouse.test' },
      });
      fireEvent.click(screen.getByRole('button', { name: /Send Login Code/i }));

      const codeInput = await screen.findByLabelText(/Enter 6-Digit Code/i);
      fireEvent.change(codeInput, { target: { value: '999999' } });

      fireEvent.click(screen.getByRole('button', { name: /Verify & Enter Warehouse/i }));

      await waitFor(() => {
        expect(screen.getByText(/Incorrect verification code/i)).toBeInTheDocument();
      });
    });

    it('displays account deactivated alert when deactivated user attempts login verification', async () => {
      vi.mocked(authApi.fetchCsrf).mockResolvedValueOnce({
        token: 'test-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      });
      vi.mocked(authApi.sendLoginOtp).mockResolvedValueOnce({
        email: 'deactivated@warehouse.test',
        purpose: 'LOGIN',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'Verification code dispatched.',
      });
      vi.mocked(authApi.verifyLoginOtp).mockRejectedValueOnce({
        errorCode: 'ACCOUNT_DEACTIVATED',
        message: 'Your account has been deactivated.',
      });

      renderLoginPage();

      fireEvent.change(screen.getByLabelText(/Registered Work Email/i), {
        target: { value: 'deactivated@warehouse.test' },
      });
      fireEvent.click(screen.getByRole('button', { name: /Send Login Code/i }));

      const codeInput = await screen.findByLabelText(/Enter 6-Digit Code/i);
      fireEvent.change(codeInput, { target: { value: '123456' } });

      fireEvent.click(screen.getByRole('button', { name: /Verify & Enter Warehouse/i }));

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

    it('submits self-service password change and displays success feedback', async () => {
      vi.mocked(authApi.getMe).mockResolvedValueOnce(mockUsers[0]);
      vi.mocked(authApi.changePassword).mockResolvedValueOnce();

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Update Password/i })).toBeInTheDocument();
      });

      fireEvent.change(screen.getByLabelText(/Current Password/i), {
        target: { value: 'CurrentSecret123!' },
      });
      fireEvent.change(screen.getByLabelText(/^New Password/i), {
        target: { value: 'NewStrongPass2026!' },
      });
      fireEvent.change(screen.getByLabelText(/Confirm New Password/i), {
        target: { value: 'NewStrongPass2026!' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Update Password/i }));

      await waitFor(() => {
        expect(authApi.changePassword).toHaveBeenCalledWith({
          currentPassword: 'CurrentSecret123!',
          newPassword: 'NewStrongPass2026!',
          confirmPassword: 'NewStrongPass2026!',
        });
      });

      expect(await screen.findByText(/Password changed successfully/i)).toBeInTheDocument();
    });

    it('displays error message when new password and confirmation do not match', async () => {
      vi.mocked(authApi.getMe).mockResolvedValueOnce(mockUsers[0]);

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Update Password/i })).toBeInTheDocument();
      });

      fireEvent.change(screen.getByLabelText(/Current Password/i), {
        target: { value: 'CurrentSecret123!' },
      });
      fireEvent.change(screen.getByLabelText(/^New Password/i), {
        target: { value: 'NewPass12345!' },
      });
      fireEvent.change(screen.getByLabelText(/Confirm New Password/i), {
        target: { value: 'DifferentPass12345!' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Update Password/i }));

      expect(screen.getByText(/New password and confirmation do not match/i)).toBeInTheDocument();
    });
  });
});
