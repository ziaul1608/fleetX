import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { DriversPage } from '../features/drivers/pages/DriversPage';
import { TripsPage } from '../features/drivers/pages/TripsPage';
import { CreateDriverModal } from '../features/drivers/components/CreateDriverModal';
import { TripLifecycleModal } from '../features/drivers/components/TripLifecycleModal';
import { driverApi } from '../features/drivers/api/driverApi';
import { Driver, Trip, TripStatusHistory } from '../features/drivers/types';
import { PageResponse } from '../shared/api/types';

vi.mock('../features/drivers/api/driverApi', () => ({
  driverApi: {
    getDrivers: vi.fn(),
    getDriverById: vi.fn(),
    createDriver: vi.fn(),
    updateDriver: vi.fn(),
    updateDriverAvailability: vi.fn(),
    getDriverPerformance: vi.fn(),
    getTrips: vi.fn(),
    getTripById: vi.fn(),
    createTrip: vi.fn(),
    updateTripStatus: vi.fn(),
    startTrip: vi.fn(),
    completeTrip: vi.fn(),
    cancelTrip: vi.fn(),
    getTripHistory: vi.fn(),
  },
}));

const mockDrivers: Driver[] = [
  {
    id: 1,
    fullName: 'Rajesh Sharma',
    phoneNumber: '+91 98765 11111',
    email: 'rajesh@fleetx.test',
    licenseNumber: 'MH-14-2024-001234',
    licenseCategory: 'HEAVY',
    licenseExpiryDate: '2028-12-31',
    medicalCertExpiryDate: '2027-06-30',
    experienceYears: 7,
    availabilityStatus: 'AVAILABLE',
    active: true,
    operationalNotes: 'Interstate highway specialist',
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-10T12:00:00Z',
    safetyScore: 98.5,
    completedTripsCount: 42,
  },
  {
    id: 2,
    fullName: 'Sanjay Verma',
    phoneNumber: '+91 98765 22222',
    email: 'sanjay@fleetx.test',
    licenseNumber: 'DL-142024-SANJAY',
    licenseCategory: 'MEDIUM',
    licenseExpiryDate: '2027-05-15',
    medicalCertExpiryDate: null,
    experienceYears: 4,
    availabilityStatus: 'ON_DUTY',
    active: true,
    operationalNotes: null,
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-10T12:00:00Z',
    safetyScore: 92.0,
    completedTripsCount: 19,
  },
];

const mockDriverPage: PageResponse<Driver> = {
  content: mockDrivers,
  pageNumber: 0,
  pageSize: 15,
  totalElements: 2,
  totalPages: 1,
  last: true,
};

const mockTrips: Trip[] = [
  {
    id: 101,
    tripCode: 'TRIP-2026-0001',
    vehicleId: 1,
    driverId: 1,
    originAddress: 'Mumbai Central Depot',
    destinationAddress: 'Pune Hinjawadi Hub',
    status: 'ASSIGNED',
    scheduledStartTime: '2026-10-15T06:00:00Z',
    scheduledEndTime: '2026-10-15T12:00:00Z',
    actualStartTime: null,
    actualEndTime: null,
    distanceKm: 148.5,
    notes: 'Fragile pharmaceutical cargo',
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-10T12:00:00Z',
    vehicleLicensePlate: 'MH-12-AB-1234',
    vehicleMakeModel: 'Tata Prima 4028',
    driverName: 'Rajesh Sharma',
    driverPhone: '+91 98765 11111',
  },
  {
    id: 102,
    tripCode: 'TRIP-2026-0002',
    vehicleId: 2,
    driverId: 2,
    originAddress: 'Thane Logistics Park',
    destinationAddress: 'Nashik MIDC Area',
    status: 'IN_PROGRESS',
    scheduledStartTime: '2026-10-15T08:00:00Z',
    scheduledEndTime: '2026-10-15T14:00:00Z',
    actualStartTime: '2026-10-15T08:05:00Z',
    actualEndTime: null,
    distanceKm: 165.0,
    notes: 'Express parcel delivery',
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-10T12:00:00Z',
    vehicleLicensePlate: 'MH-14-XY-5678',
    vehicleMakeModel: 'Volvo FM 420',
    driverName: 'Sanjay Verma',
    driverPhone: '+91 98765 22222',
  },
];

const mockTripPage: PageResponse<Trip> = {
  content: mockTrips,
  pageNumber: 0,
  pageSize: 15,
  totalElements: 2,
  totalPages: 1,
  last: true,
};

describe('Driver & Trip Operations Frontend Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(driverApi.getDrivers).mockResolvedValue(mockDriverPage);
    vi.mocked(driverApi.getTrips).mockResolvedValue(mockTripPage);
  });

  describe('DriversPage', () => {
    it('fetches and renders driver roster with names, licenses, and duty badges', async () => {
      vi.mocked(driverApi.getDrivers).mockResolvedValueOnce(mockDriverPage);

      render(
        <BrowserRouter>
          <DriversPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(driverApi.getDrivers).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText('Rajesh Sharma')).toBeInTheDocument();
      expect(screen.getByText('Sanjay Verma')).toBeInTheDocument();
      expect(screen.getByText('MH-14-2024-001234')).toBeInTheDocument();
      expect(screen.getByText('DL-142024-SANJAY')).toBeInTheDocument();
      expect(screen.getAllByText('AVAILABLE').length).toBeGreaterThan(0);
      expect(screen.getAllByText('ON_DUTY').length).toBeGreaterThan(0);
    });

    it('opens Onboard Driver modal when Onboard Driver button is clicked', async () => {
      vi.mocked(driverApi.getDrivers).mockResolvedValueOnce(mockDriverPage);

      render(
        <BrowserRouter>
          <DriversPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Rajesh Sharma')).toBeInTheDocument();
      });

      const onboardBtn = screen.getByRole('button', { name: /Onboard Driver/i });
      fireEvent.click(onboardBtn);

      expect(screen.getByText(/Onboard Commercial Driver/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/e\.g\. Ramesh Kumar/i)).toBeInTheDocument();
    });
  });

  describe('TripsPage', () => {
    it('fetches and renders trip dispatch operations with trip codes and status badges', async () => {
      vi.mocked(driverApi.getTrips).mockResolvedValueOnce(mockTripPage);

      render(
        <BrowserRouter>
          <TripsPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(driverApi.getTrips).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText('TRIP-2026-0001')).toBeInTheDocument();
      expect(screen.getByText('TRIP-2026-0002')).toBeInTheDocument();
      expect(screen.getByText('Mumbai Central Depot')).toBeInTheDocument();
      expect(screen.getByText('Pune Hinjawadi Hub')).toBeInTheDocument();
      expect(screen.getAllByText('ASSIGNED').length).toBeGreaterThan(0);
      expect(screen.getAllByText('IN_PROGRESS').length).toBeGreaterThan(0);
    });

    it('opens Schedule Trip modal when Schedule Trip button is clicked', async () => {
      vi.mocked(driverApi.getTrips).mockResolvedValueOnce(mockTripPage);

      render(
        <BrowserRouter>
          <TripsPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('TRIP-2026-0001')).toBeInTheDocument();
      });

      const scheduleBtn = screen.getByRole('button', { name: /Schedule Trip/i });
      fireEvent.click(scheduleBtn);

      expect(screen.getByText(/Schedule & Dispatch Logistics Trip/i)).toBeInTheDocument();
    });
  });

  describe('CreateDriverModal', () => {
    it('submits onboarding form and triggers createDriver API', async () => {
      vi.mocked(driverApi.createDriver).mockResolvedValueOnce(mockDrivers[0]);
      const onSuccessMock = vi.fn();
      const onCloseMock = vi.fn();

      render(
        <CreateDriverModal
          isOpen={true}
          onClose={onCloseMock}
          onSuccess={onSuccessMock}
        />
      );

      fireEvent.change(screen.getByPlaceholderText(/e\.g\. Ramesh Kumar/i), {
        target: { value: 'Sunil Rao' },
      });
      fireEvent.change(screen.getByPlaceholderText(/e\.g\. \+91 98765 43210/i), {
        target: { value: '+91 98765 88888' },
      });
      fireEvent.change(screen.getByPlaceholderText(/e\.g\. DL-1420240098765/i), {
        target: { value: 'MH-12-SUNIL-01' },
      });
      fireEvent.change(screen.getByLabelText(/License Expiry Date/i), {
        target: { value: '2028-12-31' },
      });

      const submitBtn = screen.getByRole('button', { name: /Register Driver/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(driverApi.createDriver).toHaveBeenCalledTimes(1);
        expect(onSuccessMock).toHaveBeenCalledTimes(1);
        expect(onCloseMock).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('TripLifecycleModal', () => {
    it('renders lifecycle route details and triggers start trip action', async () => {
      const mockHistory: TripStatusHistory[] = [
        {
          id: 1,
          tripId: 101,
          fromStatus: null,
          toStatus: 'ASSIGNED',
          remarks: 'Trip created and assets allocated.',
          updatedBy: null,
          createdAt: '2026-10-15T06:00:00Z',
        },
      ];
      vi.mocked(driverApi.getTripHistory).mockResolvedValueOnce(mockHistory);
      vi.mocked(driverApi.startTrip).mockResolvedValueOnce({
        ...mockTrips[0],
        status: 'IN_PROGRESS',
        actualStartTime: '2026-10-15T06:15:00Z',
      });

      const onSuccessMock = vi.fn();
      const onCloseMock = vi.fn();

      render(
        <TripLifecycleModal
          trip={mockTrips[0]}
          isOpen={true}
          onClose={onCloseMock}
          onSuccess={onSuccessMock}
        />
      );

      expect(screen.getByText(/Trip Lifecycle: TRIP-2026-0001/i)).toBeInTheDocument();
      expect(screen.getByText('Mumbai Central Depot')).toBeInTheDocument();

      const startBtn = screen.getByRole('button', { name: /Start Trip/i });
      fireEvent.click(startBtn);

      await waitFor(() => {
        expect(driverApi.startTrip).toHaveBeenCalledWith(101, expect.any(String));
        expect(onSuccessMock).toHaveBeenCalledTimes(1);
        expect(onCloseMock).toHaveBeenCalledTimes(1);
      });
    });
  });
});
