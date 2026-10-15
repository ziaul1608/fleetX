import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ShipmentsPage } from '../features/shipments/pages/ShipmentsPage';
import { WaypointsPage } from '../features/shipments/pages/WaypointsPage';
import { CreateShipmentModal } from '../features/shipments/components/CreateShipmentModal';
import { ShipmentDetailsModal } from '../features/shipments/components/ShipmentDetailsModal';
import { shipmentApi } from '../features/shipments/api/shipmentApi';
import { Shipment } from '../features/shipments/types';

vi.mock('../features/shipments/api/shipmentApi', () => ({
  shipmentApi: {
    getShipments: vi.fn(),
    getShipment: vi.fn(),
    createShipment: vi.fn(),
    updateShipment: vi.fn(),
    assignTrip: vi.fn(),
    updateStatus: vi.fn(),
    completeDelivery: vi.fn(),
    saveWaypoints: vi.fn(),
    getWaypoints: vi.fn(),
    getTimeline: vi.fn(),
    checkDelayedShipments: vi.fn(),
    getAvailableTrips: vi.fn(),
  },
}));

const mockShipment: Shipment = {
  id: 501,
  trackingNumber: 'SHP-2026-501',
  tripId: 301,
  tripNumber: 'TRP-2026-301',
  senderName: 'Apex Industrial Corp',
  senderAddress: 'Plot 14, MIDC Mumbai',
  receiverName: 'Horizon Distribution Hub',
  receiverAddress: 'Sector 7, Chakan Pune',
  receiverPhone: '+91-9876543210',
  originCity: 'Mumbai',
  destinationCity: 'Pune',
  cargoDescription: 'Precision CNC Spindles',
  weightKg: 850.5,
  volumeM3: 4.2,
  promisedDeliveryTime: '2026-10-15T18:00:00Z',
  estimatedDistanceKm: 118.55,
  estimatedDurationMinutes: 157,
  estimatedArrivalTime: '2026-10-13T14:37:00Z',
  deliveredAt: null,
  deliveryNotes: null,
  status: 'IN_TRANSIT',
  delayed: false,
  createdAt: '2026-10-13T08:00:00Z',
  updatedAt: '2026-10-13T09:00:00Z',
};

describe('Module 4 — Shipment & Route Management Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders ShipmentsPage with manifest rows, trip link, and Haversine distance estimate', async () => {
    vi.mocked(shipmentApi.getShipments).mockResolvedValue({
      content: [mockShipment],
      pageNumber: 0,
      pageSize: 10,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    render(
      <MemoryRouter>
        <ShipmentsPage />
      </MemoryRouter>
    );

    expect(
      await screen.findByText('SHP-2026-501')
    ).toBeInTheDocument();
    expect(screen.getByText('Apex Industrial Corp')).toBeInTheDocument();
    expect(screen.getByText('TRP-2026-301')).toBeInTheDocument();
    expect(screen.getByText('~118.55 km')).toBeInTheDocument();
    expect(screen.getAllByText('IN_TRANSIT').length).toBeGreaterThanOrEqual(1);
  });

  it('triggers overdue shipment scan from ShipmentsPage', async () => {
    vi.mocked(shipmentApi.getShipments).mockResolvedValue({
      content: [mockShipment],
      pageNumber: 0,
      pageSize: 10,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });
    vi.mocked(shipmentApi.checkDelayedShipments).mockResolvedValue([
      { ...mockShipment, status: 'DELAYED', delayed: true },
    ]);

    render(
      <MemoryRouter>
        <ShipmentsPage />
      </MemoryRouter>
    );

    await screen.findByText('SHP-2026-501');

    const scanBtn = screen.getByRole('button', {
      name: /Scan Overdue Shipments/i,
    });
    fireEvent.click(scanBtn);

    expect(
      await screen.findByText(/Detected and flagged 1 overdue shipment/i)
    ).toBeInTheDocument();
    expect(shipmentApi.checkDelayedShipments).toHaveBeenCalledTimes(1);
  });

  it('validates non-positive weight and submits valid shipment in CreateShipmentModal', async () => {
    vi.mocked(shipmentApi.getAvailableTrips).mockResolvedValue([
      {
        id: 301,
        tripNumber: 'TRP-2026-301',
        originAddress: 'Mumbai',
        destinationAddress: 'Pune',
        status: 'PLANNED',
      },
    ]);
    vi.mocked(shipmentApi.createShipment).mockResolvedValue(mockShipment);

    const onSuccess = vi.fn();
    const onClose = vi.fn();

    render(
      <CreateShipmentModal
        isOpen={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    const senderInput = screen.getByPlaceholderText('Apex Industrial Logistics');
    const receiverInput = screen.getByPlaceholderText('Horizon Distribution Hub');
    const originInput = screen.getByPlaceholderText('Mumbai, MH');
    const destInput = screen.getByPlaceholderText('Pune, MH');
    const weightInput = screen.getByLabelText('Weight (kg)');

    fireEvent.change(senderInput, { target: { value: 'Tata Motors' } });
    fireEvent.change(receiverInput, { target: { value: 'Pune Assembly Hub' } });
    fireEvent.change(originInput, { target: { value: 'Mumbai' } });
    fireEvent.change(destInput, { target: { value: 'Pune' } });
    fireEvent.change(weightInput, { target: { value: '-25' } });

    const submitBtn = screen.getByRole('button', { name: /Create Shipment/i });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Weight \(kg\) must be strictly greater than zero/i)
    ).toBeInTheDocument();
    expect(shipmentApi.createShipment).not.toHaveBeenCalled();

    // Fix weight and submit
    fireEvent.change(weightInput, { target: { value: '450' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(shipmentApi.createShipment).toHaveBeenCalledTimes(1);
      expect(onSuccess).toHaveBeenCalled();
    });
  });

  it('displays chronological delivery timeline and completes delivery in ShipmentDetailsModal', async () => {
    vi.mocked(shipmentApi.getTimeline).mockResolvedValue([
      {
        id: 1,
        shipmentId: 501,
        status: 'CREATED',
        locationName: 'Mumbai',
        description: 'Shipment manifest created',
        recordedAt: '2026-10-13T08:00:00Z',
        recordedByEmail: 'dispatcher@fleetx.com',
      },
      {
        id: 2,
        shipmentId: 501,
        status: 'IN_TRANSIT',
        locationName: 'Lonavala Highway',
        description: 'Linehaul transit underway',
        recordedAt: '2026-10-13T10:30:00Z',
        recordedByEmail: 'driver@fleetx.com',
      },
    ]);

    vi.mocked(shipmentApi.completeDelivery).mockResolvedValue({
      ...mockShipment,
      status: 'DELIVERED',
      deliveredAt: '2026-10-13T13:00:00Z',
      deliveryNotes: 'Signed by Dock Supervisor',
    });

    const onUpdated = vi.fn();

    render(
      <ShipmentDetailsModal
        shipment={mockShipment}
        isOpen={true}
        onClose={vi.fn()}
        onUpdated={onUpdated}
      />
    );

    expect(
      await screen.findByText('Linehaul transit underway')
    ).toBeInTheDocument();
    expect(screen.getByText('Lonavala Highway')).toBeInTheDocument();

    const confirmBtn = screen.getByRole('button', {
      name: /Confirm Final Delivery/i,
    });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(shipmentApi.completeDelivery).toHaveBeenCalledWith(
        501,
        expect.objectContaining({
          locationName: 'Pune',
        })
      );
      expect(onUpdated).toHaveBeenCalled();
    });
  });

  it('renders WaypointsPage with Haversine distance & ETA estimation disclaimers and saves route', async () => {
    vi.mocked(shipmentApi.getShipments).mockResolvedValue({
      content: [mockShipment],
      pageNumber: 0,
      pageSize: 50,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    const mockRoutePlan = {
      shipmentId: 501,
      trackingNumber: 'SHP-2026-501',
      waypoints: [
        {
          id: 1,
          shipmentId: 501,
          sequenceOrder: 1,
          locationName: 'Mumbai Hub',
          latitude: 19.076,
          longitude: 72.8777,
          stopType: 'ORIGIN' as const,
          legDistanceKm: 0,
          cumulativeDistanceKm: 0,
          estimatedArrivalTime: '2026-10-13T08:00:00Z',
          actualArrivalTime: null,
        },
        {
          id: 2,
          shipmentId: 501,
          sequenceOrder: 2,
          locationName: 'Pune Hub',
          latitude: 18.5204,
          longitude: 73.8567,
          stopType: 'DESTINATION' as const,
          legDistanceKm: 120.15,
          cumulativeDistanceKm: 120.15,
          estimatedArrivalTime: '2026-10-13T10:25:00Z',
          actualArrivalTime: null,
        },
      ],
      totalEstimatedDistanceKm: 120.15,
      estimatedDurationMinutes: 145,
      estimatedArrivalTime: '2026-10-13T10:25:00Z',
      assumedAverageSpeedKmh: 50,
      stopAllowanceMinutesPerIntermediateStop: 15,
      distanceEstimationMethod:
        'Estimated Straight-Line Distance (Haversine Formula — Not Exact Road Distance)',
      etaEstimationMethod:
        'Estimated Arrival Time (Assumed Constant Speed & Stop Allowance — Not Live Traffic Prediction)',
    };

    vi.mocked(shipmentApi.getWaypoints).mockResolvedValue(mockRoutePlan);
    vi.mocked(shipmentApi.saveWaypoints).mockResolvedValue(mockRoutePlan);

    render(
      <MemoryRouter>
        <WaypointsPage />
      </MemoryRouter>
    );

    const distanceMatches = await screen.findAllByText(/120\.15 km/i);
    expect(distanceMatches.length).toBeGreaterThanOrEqual(1);
    expect(
      screen.getByText(/Estimated Straight-Line Distance \(Haversine Formula/i)
    ).toBeInTheDocument();

    const saveBtn = screen.getByRole('button', {
      name: /Save Waypoints & Calculate Estimates/i,
    });
    fireEvent.click(saveBtn);

    expect(
      await screen.findByText(/Route waypoints saved and Haversine straight-line distance/i)
    ).toBeInTheDocument();
    expect(shipmentApi.saveWaypoints).toHaveBeenCalledTimes(1);
  });
});
