export type VehicleStatus =
  | 'ACTIVE'
  | 'AVAILABLE'
  | 'IDLE'
  | 'IN_TRANSIT'
  | 'IN_TRIP'
  | 'MAINTENANCE'
  | 'OFFLINE'
  | 'RETIRED';

export type VehicleType = 'TRUCK' | 'VAN' | 'CONTAINER' | 'MOTORCYCLE' | 'TRAILER';

export type FuelType = 'DIESEL' | 'PETROL' | 'ELECTRIC' | 'CNG' | 'HYBRID';

export interface VehicleDto {
  id: number;
  vin: string;
  licensePlate: string;
  make: string;
  model: string;
  modelYear: number;
  vehicleType: VehicleType | string;
  fuelType: FuelType | string;
  capacityKg: number;
  capacityVolumeM3: number;
  odometerKm: number;
  status: VehicleStatus | string;
  active: boolean;
  averageFuelEfficiency?: number | null;
  serviceDue?: boolean;
  createdAt?: string;
}

export interface VehicleCreateRequest {
  vin: string;
  licensePlate: string;
  make: string;
  model: string;
  year: number;
  type: string;
  fuelType: string;
  capacityKg: number;
  capacityVolumeM3: number;
  initialOdometerKm: number;
  status?: string;
}

export interface VehicleUpdateRequest {
  make: string;
  model: string;
  year: number;
  type: string;
  fuelType: string;
  capacityKg: number;
  capacityVolumeM3: number;
  status: string;
  active: boolean;
}

export interface VehicleStatusUpdateRequest {
  status: string;
  reason?: string;
}

export interface FuelRecordDto {
  id: number;
  vehicleId: number;
  licensePlate: string;
  liters: number;
  pricePerLiter?: number;
  totalCost: number;
  odometerAtFueling: number;
  fuelStation?: string;
  fuelDate: string;
  efficiencyKmPerLiter?: number | null;
}

export interface FuelRecordCreateRequest {
  liters: number;
  pricePerLiter?: number;
  totalCost?: number;
  odometerAtFueling: number;
  fuelStation?: string;
  fuelDate?: string;
}

export interface MaintenanceRecordDto {
  id: number;
  vehicleId: number;
  licensePlate: string;
  serviceType: string;
  description: string;
  cost: number;
  serviceDate: string;
  odometerAtService: number;
  nextServiceDueKm?: number | null;
  nextServiceDueDate?: string | null;
  status: string;
  isOverdue?: boolean;
  createdAt?: string;
}

export interface MaintenanceRecordCreateRequest {
  serviceType: string;
  description: string;
  cost: number;
  serviceDate: string;
  odometerAtService: number;
  nextServiceDueKm?: number | null;
  nextServiceDueDate?: string | null;
  status?: string;
}

export interface ServiceWarningDto {
  id: number;
  vehicleId: number;
  licensePlate: string;
  warningType: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  message: string;
  resolved: boolean;
  createdAt: string;
}
