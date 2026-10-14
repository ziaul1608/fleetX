import { apiClient } from '../../../shared/api/client';
import { ApiResponse, PageResponse } from '../../../shared/api/types';
import {
  Driver,
  DriverCreateRequest,
  DriverUpdateRequest,
  DriverPerformance,
  Trip,
  TripCreateRequest,
  TripStatusUpdateRequest,
  TripStatusHistory,
} from '../types';

export const driverApi = {
  // Driver Endpoints
  getDrivers: async (params?: {
    search?: string;
    availability?: string;
    active?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: string;
  }): Promise<PageResponse<Driver>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Driver>>>('/api/v1/trips/drivers', { params });
    return res.data.data;
  },

  getDriverById: async (id: number): Promise<Driver> => {
    const res = await apiClient.get<ApiResponse<Driver>>(`/api/v1/trips/drivers/${id}`);
    return res.data.data;
  },

  createDriver: async (data: DriverCreateRequest): Promise<Driver> => {
    const res = await apiClient.post<ApiResponse<Driver>>('/api/v1/trips/drivers', data);
    return res.data.data;
  },

  updateDriver: async (id: number, data: DriverUpdateRequest): Promise<Driver> => {
    const res = await apiClient.put<ApiResponse<Driver>>(`/api/v1/trips/drivers/${id}`, data);
    return res.data.data;
  },

  updateDriverAvailability: async (id: number, availabilityStatus: string): Promise<Driver> => {
    const res = await apiClient.patch<ApiResponse<Driver>>(`/api/v1/trips/drivers/${id}/availability`, {
      availabilityStatus,
    });
    return res.data.data;
  },

  getDriverPerformance: async (id: number): Promise<DriverPerformance> => {
    const res = await apiClient.get<ApiResponse<DriverPerformance>>(`/api/v1/trips/drivers/${id}/performance`);
    return res.data.data;
  },

  // Trip Endpoints
  getTrips: async (params?: {
    search?: string;
    status?: string;
    driverId?: number;
    vehicleId?: number;
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: string;
  }): Promise<PageResponse<Trip>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Trip>>>('/api/v1/trips', { params });
    return res.data.data;
  },

  getTripById: async (id: number): Promise<Trip> => {
    const res = await apiClient.get<ApiResponse<Trip>>(`/api/v1/trips/${id}`);
    return res.data.data;
  },

  createTrip: async (data: TripCreateRequest): Promise<Trip> => {
    const res = await apiClient.post<ApiResponse<Trip>>('/api/v1/trips', data);
    return res.data.data;
  },

  updateTripStatus: async (id: number, data: TripStatusUpdateRequest): Promise<Trip> => {
    const res = await apiClient.put<ApiResponse<Trip>>(`/api/v1/trips/${id}/status`, data);
    return res.data.data;
  },

  startTrip: async (id: number, remarks?: string): Promise<Trip> => {
    const res = await apiClient.post<ApiResponse<Trip>>(`/api/v1/trips/${id}/start`, null, {
      params: { remarks },
    });
    return res.data.data;
  },

  completeTrip: async (id: number, remarks?: string): Promise<Trip> => {
    const res = await apiClient.post<ApiResponse<Trip>>(`/api/v1/trips/${id}/complete`, null, {
      params: { remarks },
    });
    return res.data.data;
  },

  cancelTrip: async (id: number, remarks?: string): Promise<Trip> => {
    const res = await apiClient.post<ApiResponse<Trip>>(`/api/v1/trips/${id}/cancel`, null, {
      params: { remarks },
    });
    return res.data.data;
  },

  getTripHistory: async (id: number): Promise<TripStatusHistory[]> => {
    const res = await apiClient.get<ApiResponse<TripStatusHistory[]>>(`/api/v1/trips/${id}/history`);
    return res.data.data;
  },
};
