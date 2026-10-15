import { apiClient } from '../../../shared/api/client';
import { ApiResponse, PageResponse } from '../../../shared/api/types';
import {
  RoutePlanResponseDto,
  Shipment,
  ShipmentCreateRequest,
  ShipmentStatus,
  ShipmentTimelineEvent,
  ShipmentUpdateRequest,
  TripLookupOption,
  WaypointItemRequest,
} from '../types';

export const shipmentApi = {
  getShipments: async (params?: {
    search?: string;
    status?: string;
    tripId?: number;
    page?: number;
    size?: number;
  }): Promise<PageResponse<Shipment>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<Shipment>>>(
      '/api/v1/shipments',
      { params }
    );
    return response.data.data;
  },

  getShipment: async (id: number): Promise<Shipment> => {
    const response = await apiClient.get<ApiResponse<Shipment>>(`/api/v1/shipments/${id}`);
    return response.data.data;
  },

  getShipmentByTracking: async (trackingNumber: string): Promise<Shipment> => {
    const response = await apiClient.get<ApiResponse<Shipment>>(
      `/api/v1/shipments/tracking/${encodeURIComponent(trackingNumber)}`
    );
    return response.data.data;
  },

  createShipment: async (payload: ShipmentCreateRequest): Promise<Shipment> => {
    const response = await apiClient.post<ApiResponse<Shipment>>('/api/v1/shipments', payload);
    return response.data.data;
  },

  updateShipment: async (id: number, payload: ShipmentUpdateRequest): Promise<Shipment> => {
    const response = await apiClient.put<ApiResponse<Shipment>>(
      `/api/v1/shipments/${id}`,
      payload
    );
    return response.data.data;
  },

  assignTrip: async (
    id: number,
    payload: { tripId: number; notes?: string }
  ): Promise<Shipment> => {
    const response = await apiClient.patch<ApiResponse<Shipment>>(
      `/api/v1/shipments/${id}/assign-trip`,
      payload
    );
    return response.data.data;
  },

  updateStatus: async (
    id: number,
    payload: {
      status: ShipmentStatus | string;
      locationName?: string;
      description?: string;
    }
  ): Promise<Shipment> => {
    const response = await apiClient.patch<ApiResponse<Shipment>>(
      `/api/v1/shipments/${id}/status`,
      payload
    );
    return response.data.data;
  },

  completeDelivery: async (
    id: number,
    payload?: { locationName?: string; deliveryNotes?: string }
  ): Promise<Shipment> => {
    const response = await apiClient.post<ApiResponse<Shipment>>(
      `/api/v1/shipments/${id}/deliver`,
      payload ?? {}
    );
    return response.data.data;
  },

  saveWaypoints: async (
    id: number,
    payload: {
      waypoints: WaypointItemRequest[];
      averageSpeedKmh?: number;
      stopAllowanceMinutes?: number;
    }
  ): Promise<RoutePlanResponseDto> => {
    const response = await apiClient.put<ApiResponse<RoutePlanResponseDto>>(
      `/api/v1/shipments/${id}/waypoints`,
      payload
    );
    return response.data.data;
  },

  getWaypoints: async (
    id: number,
    params?: { averageSpeedKmh?: number; stopAllowanceMinutes?: number }
  ): Promise<RoutePlanResponseDto> => {
    const response = await apiClient.get<ApiResponse<RoutePlanResponseDto>>(
      `/api/v1/shipments/${id}/waypoints`,
      { params }
    );
    return response.data.data;
  },

  getTimeline: async (id: number): Promise<ShipmentTimelineEvent[]> => {
    const response = await apiClient.get<ApiResponse<ShipmentTimelineEvent[]>>(
      `/api/v1/shipments/${id}/timeline`
    );
    return response.data.data;
  },

  checkDelayedShipments: async (): Promise<Shipment[]> => {
    const response = await apiClient.post<ApiResponse<Shipment[]>>(
      '/api/v1/shipments/check-delayed'
    );
    return response.data.data;
  },

  getAvailableTrips: async (): Promise<TripLookupOption[]> => {
    const response = await apiClient.get<ApiResponse<PageResponse<TripLookupOption>>>(
      '/api/v1/trips',
      { params: { size: 50 } }
    );
    return response.data.data?.content ?? [];
  },
};
