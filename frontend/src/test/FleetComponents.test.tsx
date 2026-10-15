import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { VehiclesPage } from '../features/fleet/pages/VehiclesPage';
import { MaintenancePage } from '../features/fleet/pages/MaintenancePage';
import { LogFuelModal } from '../features/fleet/components/LogFuelModal';
import { CreateVehicleModal } from '../features/fleet/components/CreateVehicleModal';
import { fleetApi } from '../features/fleet/api/fleetApi';
import { VehicleDto, ServiceWarningDto, MaintenanceRecordDto } from '../features/fleet/types';
import { PageResponse } from '../shared/api/types';

vi.mock('../features/fleet/api/fleetApi', () => ({
  fleetApi: {
    getVehicles: vi.fn(),
    getVehicleById: vi.fn(),
    createVehicle: vi.fn(),
    updateVehicle: vi.fn(),
    updateStatus: vi.fn(),
    updateOdometer: vi.fn(),
    logFuelRecord: vi.fn(),
    getFuelRecords: vi.fn(),
    logMaintenanceRecord: vi.fn(),
    getMaintenanceRecords: vi.fn(),
    getAllMaintenanceRecords: vi.fn(),
    getActiveWarnings: vi.fn(),
  },
}));

const mockVehicles: VehicleDto[] = [
  {
    id: 1,
    vin: '1HGCR2F83HA001234',
    licensePlate: 'MH-12-AB-1234',
    make: 'Tata',
    model: 'Prima 4028',
    modelYear: 2024,
    vehicleType: 'TRUCK',
    fuelType: 'DIESEL',
    capacityKg: 25000,
    capacityVolumeM3: 45,
    odometerKm: 12500,
    status: 'AVAILABLE',
    active: true,
    averageFuelEfficiency: 8.5,
    serviceDue: false,
    createdAt: '2026-10-10T12:00:00Z',
  },
  {
    id: 2,
    vin: '2HGCR2F83HA005678',
    licensePlate: 'MH-14-XY-5678',
    make: 'Volvo',
    model: 'FM 420',
    modelYear: 2023,
    vehicleType: 'TRUCK',
    fuelType: 'DIESEL',
    capacityKg: 30000,
    capacityVolumeM3: 50,
    odometerKm: 48900,
    status: 'IN_TRANSIT',
    active: true,
    averageFuelEfficiency: 7.2,
    serviceDue: true,
    createdAt: '2026-10-10T12:00:00Z',
  },
];

const mockVehiclePage: PageResponse<VehicleDto> = {
  content: mockVehicles,
  pageNumber: 0,
  pageSize: 10,
  totalElements: 2,
  totalPages: 1,
  last: true,
};

const mockWarnings: ServiceWarningDto[] = [
  {
    id: 1,
    vehicleId: 2,
    licensePlate: 'MH-14-XY-5678',
    warningType: 'SERVICE_OVERDUE_KM',
    severity: 'CRITICAL',
    message: 'Vehicle MH-14-XY-5678 is OVERDUE for service by 400 km.',
    resolved: false,
    createdAt: '2026-10-12T10:00:00Z',
  },
  {
    id: 2,
    vehicleId: 1,
    licensePlate: 'MH-12-AB-1234',
    warningType: 'SERVICE_DUE_KM',
    severity: 'WARNING',
    message: 'Vehicle MH-12-AB-1234 maintenance is DUE SOON in 250 km.',
    resolved: false,
    createdAt: '2026-10-12T11:00:00Z',
  },
];

const mockMaintenanceRecords: PageResponse<MaintenanceRecordDto> = {
  content: [
    {
      id: 1,
      vehicleId: 1,
      licensePlate: 'MH-12-AB-1234',
      serviceType: 'PREVENTIVE',
      description: 'Engine oil change & air filter',
      cost: 14500,
      serviceDate: '2026-10-01',
      odometerAtService: 12000,
      nextServiceDueKm: 22000,
      nextServiceDueDate: '2027-04-01',
      status: 'COMPLETED',
      isOverdue: false,
      createdAt: '2026-10-01T10:00:00Z',
    },
  ],
  pageNumber: 0,
  pageSize: 10,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

describe('Fleet & Asset Management Frontend Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('VehiclesPage', () => {
    it('fetches and renders vehicle inventory, specs, and status badges', async () => {
      vi.mocked(fleetApi.getVehicles).mockResolvedValueOnce(mockVehiclePage);

      render(
        <BrowserRouter>
          <VehiclesPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(fleetApi.getVehicles).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText('MH-12-AB-1234')).toBeInTheDocument();
      expect(screen.getByText('MH-14-XY-5678')).toBeInTheDocument();
      expect(screen.getByText(/Tata Prima 4028/i)).toBeInTheDocument();
      expect(screen.getByText(/Volvo FM 420/i)).toBeInTheDocument();
      expect(screen.getAllByText('AVAILABLE').length).toBeGreaterThan(0);
      expect(screen.getAllByText('IN_TRANSIT').length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Service Due/i).length).toBeGreaterThan(0);
    });

    it('opens Onboard Vehicle modal when Onboard Vehicle button is clicked', async () => {
      vi.mocked(fleetApi.getVehicles).mockResolvedValueOnce(mockVehiclePage);

      render(
        <BrowserRouter>
          <VehiclesPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('MH-12-AB-1234')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /Onboard Vehicle/i }));
      expect(screen.getByText(/Onboard Commercial Vehicle/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/VIN/i)).toBeInTheDocument();
    });
  });

  describe('MaintenancePage', () => {
    it('fetches active service warnings and maintenance schedules', async () => {
      vi.mocked(fleetApi.getActiveWarnings).mockResolvedValueOnce(mockWarnings);
      vi.mocked(fleetApi.getAllMaintenanceRecords).mockResolvedValueOnce(mockMaintenanceRecords);

      render(
        <BrowserRouter>
          <MaintenancePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(fleetApi.getActiveWarnings).toHaveBeenCalledTimes(1);
        expect(fleetApi.getAllMaintenanceRecords).toHaveBeenCalledTimes(1);
      });

      expect(screen.getByText(/OVERDUE for service by 400 km/i)).toBeInTheDocument();
      expect(screen.getByText(/DUE SOON in 250 km/i)).toBeInTheDocument();
      expect(screen.getByText('CRITICAL OVERDUE')).toBeInTheDocument();
      expect(screen.getByText('UPCOMING DUE')).toBeInTheDocument();
      expect(screen.getByText('Engine oil change & air filter')).toBeInTheDocument();
    });
  });

  describe('LogFuelModal', () => {
    it('auto-calculates total cost from liters and pricePerLiter', () => {
      const mockSubmit = vi.fn().mockResolvedValue(undefined);

      render(
        <LogFuelModal
          isOpen={true}
          vehicle={mockVehicles[0]}
          onClose={vi.fn()}
          onSubmit={mockSubmit}
        />
      );

      const litersInput = screen.getByLabelText(/Fuel Volume \(Litres\)/i);
      const priceInput = screen.getByLabelText(/Price per Litre/i);
      const costInput = screen.getByLabelText(/Total Transaction Cost/i);

      fireEvent.change(litersInput, { target: { value: '50' } });
      fireEvent.change(priceInput, { target: { value: '100' } });

      expect(costInput).toHaveValue(5000);
    });
  });

  describe('CreateVehicleModal', () => {
    it('rejects submission if VIN is not exactly 17 characters', async () => {
      const mockSubmit = vi.fn().mockResolvedValue(undefined);

      render(
        <CreateVehicleModal
          isOpen={true}
          onClose={vi.fn()}
          onSubmit={mockSubmit}
        />
      );

      const vinInput = screen.getByLabelText(/VIN/i);
      fireEvent.change(vinInput, { target: { value: 'SHORTVIN' } });
      fireEvent.change(screen.getByLabelText(/Registration Plate/i), { target: { value: 'MH-12-AB-1234' } });
      fireEvent.change(screen.getByLabelText(/Model Name/i), { target: { value: 'Prima 4028' } });

      fireEvent.click(screen.getByRole('button', { name: /Register Vehicle/i }));

      expect(await screen.findByText(/VIN must be exactly 17 alphanumeric characters/i)).toBeInTheDocument();
      expect(mockSubmit).not.toHaveBeenCalled();
    });
  });
});
