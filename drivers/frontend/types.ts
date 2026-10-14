export interface Driver {
  id: number;
  userId?: number | null;
  fullName: string;
  phoneNumber: string;
  email?: string | null;
  licenseNumber: string;
  licenseCategory: string;
  licenseExpiryDate: string;
  medicalCertExpiryDate?: string | null;
  experienceYears: number;
  availabilityStatus: 'AVAILABLE' | 'ON_DUTY' | 'SUSPENDED' | 'ON_LEAVE' | 'INACTIVE';
  active: boolean;
  operationalNotes?: string | null;
  createdAt: string;
  updatedAt: string;
  safetyScore: number;
  completedTripsCount: number;
}

export interface DriverCreateRequest {
  userId?: number | null;
  fullName: string;
  phoneNumber: string;
  email?: string;
  licenseNumber: string;
  licenseCategory: string;
  licenseExpiryDate: string;
  medicalCertExpiryDate?: string;
  experienceYears?: number;
  availabilityStatus?: string;
  operationalNotes?: string;
}

export interface DriverUpdateRequest {
  fullName: string;
  phoneNumber: string;
  email?: string;
  licenseCategory?: string;
  licenseExpiryDate: string;
  medicalCertExpiryDate?: string;
  experienceYears?: number;
  availabilityStatus?: string;
  active?: boolean;
  operationalNotes?: string;
}

export interface DriverPerformance {
  driverId: number;
  driverName: string;
  totalTripsCompleted: number;
  onTimeDeliveries: number;
  onTimeDeliveryRate: number;
  safetyViolationCount: number;
  totalTripsCancelled: number;
  totalDistanceKm: number;
  ratingScore: number;
  lastCalculatedAt?: string | null;
}

export interface Trip {
  id: number;
  tripCode: string;
  vehicleId: number;
  driverId: number;
  originAddress: string;
  destinationAddress: string;
  status: 'PLANNED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  scheduledStartTime: string;
  scheduledEndTime?: string | null;
  actualStartTime?: string | null;
  actualEndTime?: string | null;
  distanceKm?: number | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
  vehicleLicensePlate: string;
  vehicleMakeModel: string;
  driverName: string;
  driverPhone: string;
}

export interface TripCreateRequest {
  vehicleId: number;
  driverId: number;
  originAddress: string;
  destinationAddress: string;
  scheduledStartTime: string;
  scheduledEndTime?: string;
  distanceKm?: number;
  notes?: string;
}

export interface TripStatusUpdateRequest {
  targetStatus: string;
  remarks?: string;
}

export interface TripStatusHistory {
  id: number;
  tripId: number;
  fromStatus?: string | null;
  toStatus: string;
  remarks?: string | null;
  updatedBy?: number | null;
  createdAt: string;
}
