import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LandingPage } from '../features/landing/pages/LandingPage';
import { SignupPage } from '../features/auth/pages/SignupPage';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { authApi } from '../features/auth/api/authApi';
import { AuthProvider } from '../shared/context/AuthContext';
import { UserDto } from '../features/auth/types';

vi.mock('../features/auth/api/authApi', () => ({
  authApi: {
    fetchCsrf: vi.fn().mockResolvedValue({ token: 'csrf-token', headerName: 'X-CSRF', parameterName: '_csrf' }),
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
    sendSignupOtp: vi.fn(),
    verifySignupOtp: vi.fn(),
    sendLoginOtp: vi.fn(),
    verifyLoginOtp: vi.fn(),
  },
}));

const mockOnboardedUser: UserDto = {
  userId: 10,
  email: 'owner@warehouse.test',
  fullName: 'Warehouse Owner',
  roles: ['ADMIN'],
  active: true,
  tenantId: 5,
  companyName: 'Apex Logistics Corp',
  warehouseName: 'Mumbai West Depot',
  createdAt: '2026-10-24T12:00:00Z',
};

describe('Phase 11 — Multi-Tenant SaaS, Public Landing Page & Email OTP Authentication', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.fetchCsrf).mockResolvedValue({ token: 'test', headerName: 'X-CSRF', parameterName: '_csrf' });
    vi.mocked(authApi.getMe).mockRejectedValue({ status: 401, errorCode: 'UNAUTHORIZED' });
  });

  describe('LandingPage', () => {
    it('renders SaaS hero, value proposition, feature highlights and onboarding steps', () => {
      render(
        <MemoryRouter>
          <LandingPage />
        </MemoryRouter>
      );

      expect(screen.getByText(/Intelligent Multi-Tenant Fleet & Logistics Operating System/i)).toBeInTheDocument();
      expect(screen.getByText(/Multi-Tenant Architecture & Passwordless Email OTP Access/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Register Private Warehouse/i })).toHaveAttribute('href', '/signup');
      expect(screen.getByRole('link', { name: /Sign In with Email OTP/i })).toHaveAttribute('href', '/login');
      expect(screen.getByText(/Private Warehouse Workspaces/i)).toBeInTheDocument();
      expect(screen.getByText(/Passwordless Email OTP Security/i)).toBeInTheDocument();
      expect(screen.getByText(/How to Onboard Your Warehouse/i)).toBeInTheDocument();
    });
  });

  describe('SignupPage (2-Step Email OTP Onboarding)', () => {
    it('renders Step 1 onboarding fields and triggers sendSignupOtp API', async () => {
      vi.mocked(authApi.sendSignupOtp).mockResolvedValueOnce({
        email: 'owner@warehouse.test',
        purpose: 'SIGNUP',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'Verification code sent to owner@warehouse.test',
      });

      render(
        <AuthProvider>
          <MemoryRouter>
            <SignupPage />
          </MemoryRouter>
        </AuthProvider>
      );

      expect(screen.getByText(/Launch Warehouse Workspace/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Company \/ Organization Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Warehouse \/ Central Depot Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Owner \/ Manager Full Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Work Email Address/i)).toBeInTheDocument();

      fireEvent.change(screen.getByLabelText(/Company \/ Organization Name/i), {
        target: { value: 'Apex Logistics Corp' },
      });
      fireEvent.change(screen.getByLabelText(/Warehouse \/ Central Depot Name/i), {
        target: { value: 'Mumbai West Depot' },
      });
      fireEvent.change(screen.getByLabelText(/Owner \/ Manager Full Name/i), {
        target: { value: 'Warehouse Owner' },
      });
      fireEvent.change(screen.getByLabelText(/Work Email Address/i), {
        target: { value: 'owner@warehouse.test' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Send Verification Code/i }));

      await waitFor(() => {
        expect(authApi.sendSignupOtp).toHaveBeenCalledWith({
          companyName: 'Apex Logistics Corp',
          warehouseName: 'Mumbai West Depot',
          fullName: 'Warehouse Owner',
          email: 'owner@warehouse.test',
        });
      });

      // Assert transition to Step 2 (Enter 6-digit code)
      await waitFor(() => {
        expect(screen.getByLabelText(/Enter 6-Digit Verification Code/i)).toBeInTheDocument();
      });
    });

    it('verifies valid 6-digit OTP in Step 2 and provisions tenant workspace', async () => {
      vi.mocked(authApi.sendSignupOtp).mockResolvedValueOnce({
        email: 'owner@warehouse.test',
        purpose: 'SIGNUP',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'Verification code sent',
      });
      vi.mocked(authApi.verifySignupOtp).mockResolvedValueOnce(mockOnboardedUser);

      render(
        <AuthProvider>
          <MemoryRouter>
            <SignupPage />
          </MemoryRouter>
        </AuthProvider>
      );

      // Fill and submit Step 1
      fireEvent.change(screen.getByLabelText(/Company \/ Organization Name/i), {
        target: { value: 'Apex Logistics Corp' },
      });
      fireEvent.change(screen.getByLabelText(/Warehouse \/ Central Depot Name/i), {
        target: { value: 'Mumbai West Depot' },
      });
      fireEvent.change(screen.getByLabelText(/Owner \/ Manager Full Name/i), {
        target: { value: 'Warehouse Owner' },
      });
      fireEvent.change(screen.getByLabelText(/Work Email Address/i), {
        target: { value: 'owner@warehouse.test' },
      });
      fireEvent.click(screen.getByRole('button', { name: /Send Verification Code/i }));

      // Wait for Step 2
      await waitFor(() => {
        expect(screen.getByLabelText(/Enter 6-Digit Verification Code/i)).toBeInTheDocument();
      });

      const otpInput = screen.getByLabelText(/Enter 6-Digit Verification Code/i);
      fireEvent.change(otpInput, { target: { value: '849201' } });

      fireEvent.click(screen.getByRole('button', { name: /Verify & Launch Warehouse/i }));

      await waitFor(() => {
        expect(authApi.verifySignupOtp).toHaveBeenCalledWith({
          companyName: 'Apex Logistics Corp',
          warehouseName: 'Mumbai West Depot',
          fullName: 'Warehouse Owner',
          email: 'owner@warehouse.test',
          otp: '849201',
        });
      });
    });
  });

  describe('LoginPage (Passwordless Email OTP)', () => {
    it('renders passwordless login directly without legacy password tabs', () => {
      render(
        <AuthProvider>
          <MemoryRouter>
            <LoginPage />
          </MemoryRouter>
        </AuthProvider>
      );

      expect(screen.queryByRole('button', { name: /Password Login/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Email Code \(OTP\)/i })).not.toBeInTheDocument();
      expect(screen.getByLabelText(/Registered Work Email/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Send Login Code/i })).toBeInTheDocument();
    });

    it('dispatches login OTP and verifies code for passwordless sign-in', async () => {
      vi.mocked(authApi.sendLoginOtp).mockResolvedValueOnce({
        email: 'owner@warehouse.test',
        purpose: 'LOGIN',
        expiresInSeconds: 300,
        cooldownSeconds: 60,
        message: 'Login code dispatched',
      });
      vi.mocked(authApi.verifyLoginOtp).mockResolvedValueOnce(mockOnboardedUser);

      render(
        <AuthProvider>
          <MemoryRouter>
            <LoginPage />
          </MemoryRouter>
        </AuthProvider>
      );

      fireEvent.change(screen.getByLabelText(/Registered Work Email/i), {
        target: { value: 'owner@warehouse.test' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Send Login Code/i }));

      await waitFor(() => {
        expect(authApi.sendLoginOtp).toHaveBeenCalledWith({ email: 'owner@warehouse.test' });
      });

      // Enter 6-digit OTP
      await waitFor(() => {
        expect(screen.getByLabelText(/Enter 6-Digit Code/i)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByLabelText(/Enter 6-Digit Code/i), {
        target: { value: '654321' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Verify & Enter Warehouse/i }));

      await waitFor(() => {
        expect(authApi.verifyLoginOtp).toHaveBeenCalledWith({
          email: 'owner@warehouse.test',
          otp: '654321',
        });
      });
    });
  });
});
