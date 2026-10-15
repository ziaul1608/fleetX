import { apiClient } from '../../../shared/api/client';
import { ApiResponse, PageResponse } from '../../../shared/api/types';
import {
  AlertItem,
  ControlTowerSummary,
  GeofenceCreateRequest,
  GeofenceItem,
  ReportSummary,
  SimulatorSession,
  SimulatorWaypoint,
  VehicleOption,
} from '../types';

export const trackingApi = {
  getControlTowerSummary: async (): Promise<ControlTowerSummary> => {
    const response = await apiClient.get<ApiResponse<ControlTowerSummary>>(
      '/api/v1/tracking/control-tower/summary'
    );
    return response.data.data;
  },

  getSimulatorSessions: async (): Promise<SimulatorSession[]> => {
    const response = await apiClient.get<ApiResponse<SimulatorSession[]>>(
      '/api/v1/tracking/simulator/sessions'
    );
    return response.data.data ?? [];
  },

  startSimulation: async (payload: {
    vehicleId?: number;
    tripId?: number;
    tickIntervalSeconds?: number;
    speedMultiplier?: number;
    baseSpeedKmh?: number;
    waypoints?: SimulatorWaypoint[];
  }): Promise<SimulatorSession> => {
    const response = await apiClient.post<ApiResponse<SimulatorSession>>(
      '/api/v1/tracking/simulator/start',
      payload
    );
    return response.data.data;
  },

  pauseSimulation: async (vehicleId: number): Promise<SimulatorSession> => {
    const response = await apiClient.post<ApiResponse<SimulatorSession>>(
      `/api/v1/tracking/simulator/${vehicleId}/pause`
    );
    return response.data.data;
  },

  resumeSimulation: async (vehicleId: number): Promise<SimulatorSession> => {
    const response = await apiClient.post<ApiResponse<SimulatorSession>>(
      `/api/v1/tracking/simulator/${vehicleId}/resume`
    );
    return response.data.data;
  },

  stopSimulation: async (vehicleId: number): Promise<SimulatorSession> => {
    const response = await apiClient.post<ApiResponse<SimulatorSession>>(
      `/api/v1/tracking/simulator/${vehicleId}/stop`
    );
    return response.data.data;
  },

  stepSimulation: async (vehicleId: number): Promise<SimulatorSession> => {
    const response = await apiClient.post<ApiResponse<SimulatorSession>>(
      `/api/v1/tracking/simulator/${vehicleId}/step`
    );
    return response.data.data;
  },

  getGeofences: async (activeOnly?: boolean): Promise<GeofenceItem[]> => {
    const response = await apiClient.get<ApiResponse<GeofenceItem[]>>(
      '/api/v1/tracking/geofences',
      { params: activeOnly !== undefined ? { activeOnly } : undefined }
    );
    return response.data.data ?? [];
  },

  createGeofence: async (payload: GeofenceCreateRequest): Promise<GeofenceItem> => {
    const response = await apiClient.post<ApiResponse<GeofenceItem>>(
      '/api/v1/tracking/geofences',
      payload
    );
    return response.data.data;
  },

  toggleGeofence: async (id: number, active: boolean): Promise<GeofenceItem> => {
    const response = await apiClient.patch<ApiResponse<GeofenceItem>>(
      `/api/v1/tracking/geofences/${id}/toggle`,
      null,
      { params: { active } }
    );
    return response.data.data;
  },

  getAlerts: async (params?: {
    alertType?: string;
    severity?: string;
    entityType?: string;
    acknowledged?: boolean;
    page?: number;
    size?: number;
  }): Promise<PageResponse<AlertItem>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<AlertItem>>>(
      '/api/v1/tracking/alerts',
      { params }
    );
    return response.data.data;
  },

  acknowledgeAlert: async (alertId: number): Promise<AlertItem> => {
    const response = await apiClient.post<ApiResponse<AlertItem>>(
      `/api/v1/tracking/alerts/${alertId}/acknowledge`
    );
    return response.data.data;
  },

  getReportSummary: async (params: {
    reportType: string;
    startDate?: string;
    endDate?: string;
    status?: string;
  }): Promise<ReportSummary> => {
    const response = await apiClient.get<ApiResponse<ReportSummary>>(
      '/api/v1/tracking/reports/summary',
      { params }
    );
    return response.data.data;
  },

  downloadReportCsv: async (params: {
    reportType: string;
    startDate?: string;
    endDate?: string;
    status?: string;
  }): Promise<string> => {
    const response = await apiClient.get<string>('/api/v1/tracking/reports/export-csv', {
      params,
      responseType: 'text' as any,
    });
    return response.data;
  },

  getEligibleVehicles: async (): Promise<VehicleOption[]> => {
    const response = await apiClient.get<ApiResponse<PageResponse<VehicleOption>>>(
      '/api/v1/fleet/vehicles',
      { params: { size: 50 } }
    );
    return response.data.data?.content ?? [];
  },
};
