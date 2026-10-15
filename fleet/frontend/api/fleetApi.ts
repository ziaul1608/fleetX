import { apiClient } from '../../../shared/api/client';
import { ApiResponse, PageResponse } from '../../../shared/api/types';
import {
  VehicleDto,
  VehicleCreateRequest,
  VehicleUpdateRequest,
  VehicleStatusUpdateRequest,
  FuelRecordDto,
  FuelRecordCreateRequest,
  MaintenanceRecordDto,
  MaintenanceRecordCreateRequest,
  ServiceWarningDto,
} from '../types';

export const fleetApi = {
  /**
   * Fetches paginated vehicle inventory with filters
   */
  async getVehicles(params: {
    page?: number;
    size?: number;
    status?: string;
    type?: string;
    active?: boolean | '';
    search?: string;
  }): Promise<PageResponse<VehicleDto>> {
    const queryParams: Record<string, string | number | boolean> = {};
    if (params.page !== undefined) queryParams.page = params.page;
    if (params.size !== undefined) queryParams.size = params.size;
    if (params.status) queryParams.status = params.status;
    if (params.type) queryParams.type = params.type;
    if (params.active !== '' && params.active !== undefined) queryParams.active = params.active;
    if (params.search) queryParams.search = params.search;

    const response = await apiClient.get<ApiResponse<PageResponse<VehicleDto>>>('/api/v1/fleet/vehicles', {
      params: queryParams,
    });
    return response.data.data;
  },

  /**
   * Fetches single vehicle specifications and stats
   */
  async getVehicleById(id: number): Promise<VehicleDto> {
    const response = await apiClient.get<ApiResponse<VehicleDto>>(`/api/v1/fleet/vehicles/${id}`);
    return response.data.data;
  },

  /**
   * Onboards a new commercial vehicle
   */
  async createVehicle(payload: VehicleCreateRequest): Promise<VehicleDto> {
    const response = await apiClient.post<ApiResponse<VehicleDto>>('/api/v1/fleet/vehicles', payload);
    return response.data.data;
  },

  /**
   * Updates vehicle details and active state
   */
  async updateVehicle(id: number, payload: VehicleUpdateRequest): Promise<VehicleDto> {
    const response = await apiClient.put<ApiResponse<VehicleDto>>(`/api/v1/fleet/vehicles/${id}`, payload);
    return response.data.data;
  },

  /**
   * Updates operational status
   */
  async updateStatus(id: number, payload: VehicleStatusUpdateRequest): Promise<VehicleDto> {
    const response = await apiClient.patch<ApiResponse<VehicleDto>>(`/api/v1/fleet/vehicles/${id}/status`, payload);
    return response.data.data;
  },

  /**
   * Updates odometer reading
   */
  async updateOdometer(id: number, newOdometerKm: number): Promise<VehicleDto> {
    const response = await apiClient.patch<ApiResponse<VehicleDto>>(
      `/api/v1/fleet/vehicles/${id}/odometer`,
      null,
      { params: { newOdometerKm } }
    );
    return response.data.data;
  },

  /**
   * Logs a fuel intake record for a vehicle
   */
  async logFuelRecord(vehicleId: number, payload: FuelRecordCreateRequest): Promise<FuelRecordDto> {
    const response = await apiClient.post<ApiResponse<FuelRecordDto>>(
      `/api/v1/fleet/vehicles/${vehicleId}/fuel-records`,
      payload
    );
    return response.data.data;
  },

  /**
   * Fetches paginated fuel records for a specific vehicle
   */
  async getFuelRecords(vehicleId: number, params?: { page?: number; size?: number }): Promise<PageResponse<FuelRecordDto>> {
    const response = await apiClient.get<ApiResponse<PageResponse<FuelRecordDto>>>(
      `/api/v1/fleet/vehicles/${vehicleId}/fuel-records`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Logs a completed or scheduled maintenance record
   */
  async logMaintenanceRecord(
    vehicleId: number,
    payload: MaintenanceRecordCreateRequest
  ): Promise<MaintenanceRecordDto> {
    const response = await apiClient.post<ApiResponse<MaintenanceRecordDto>>(
      `/api/v1/fleet/vehicles/${vehicleId}/maintenance-records`,
      payload
    );
    return response.data.data;
  },

  /**
   * Fetches maintenance history for a vehicle
   */
  async getMaintenanceRecords(
    vehicleId: number,
    params?: { page?: number; size?: number }
  ): Promise<PageResponse<MaintenanceRecordDto>> {
    const response = await apiClient.get<ApiResponse<PageResponse<MaintenanceRecordDto>>>(
      `/api/v1/fleet/vehicles/${vehicleId}/maintenance-records`,
      { params }
    );
    return response.data.data;
  },

  /**
   * Fetches fleet-wide maintenance records
   */
  async getAllMaintenanceRecords(params?: { page?: number; size?: number }): Promise<PageResponse<MaintenanceRecordDto>> {
    const response = await apiClient.get<ApiResponse<PageResponse<MaintenanceRecordDto>>>(
      '/api/v1/fleet/maintenance-records',
      { params }
    );
    return response.data.data;
  },

  /**
   * Fetches active service interval warnings
   */
  async getActiveWarnings(): Promise<ServiceWarningDto[]> {
    const response = await apiClient.get<ApiResponse<ServiceWarningDto[]>>('/api/v1/fleet/warnings');
    return response.data.data;
  },
};
