export type ShipmentStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'DELAYED';

export type StopType = 'ORIGIN' | 'INTERMEDIATE' | 'DESTINATION';

export interface Shipment {
  id: number;
  trackingNumber: string;
  tripId: number | null;
  tripNumber: string | null;
  senderName: string;
  senderAddress: string | null;
  receiverName: string;
  receiverAddress: string | null;
  receiverPhone: string | null;
  originCity: string;
  destinationCity: string;
  cargoDescription: string | null;
  weightKg: number;
  volumeM3: number | null;
  promisedDeliveryTime: string | null;
  estimatedDistanceKm: number | null;
  estimatedDurationMinutes: number | null;
  estimatedArrivalTime: string | null;
  deliveredAt: string | null;
  deliveryNotes: string | null;
  status: ShipmentStatus;
  delayed: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export interface ShipmentCreateRequest {
  trackingNumber?: string;
  tripId?: number | null;
  senderName: string;
  senderAddress?: string;
  receiverName: string;
  receiverAddress?: string;
  receiverPhone?: string;
  originCity: string;
  destinationCity: string;
  cargoDescription?: string;
  weightKg: number;
  volumeM3?: number | null;
  promisedDeliveryTime?: string | null;
}

export interface ShipmentUpdateRequest {
  senderName: string;
  senderAddress?: string;
  receiverName: string;
  receiverAddress?: string;
  receiverPhone?: string;
  originCity: string;
  destinationCity: string;
  cargoDescription?: string;
  weightKg: number;
  volumeM3?: number | null;
  promisedDeliveryTime?: string | null;
}

export interface WaypointItemRequest {
  sequenceOrder: number;
  locationName: string;
  latitude: number;
  longitude: number;
  stopType?: StopType;
}

export interface WaypointResponseDto {
  id: number;
  shipmentId: number;
  sequenceOrder: number;
  locationName: string;
  latitude: number;
  longitude: number;
  stopType: StopType;
  legDistanceKm: number;
  cumulativeDistanceKm: number;
  estimatedArrivalTime: string | null;
  actualArrivalTime: string | null;
}

export interface RoutePlanResponseDto {
  shipmentId: number;
  trackingNumber: string;
  waypoints: WaypointResponseDto[];
  totalEstimatedDistanceKm: number;
  estimatedDurationMinutes: number;
  estimatedArrivalTime: string | null;
  assumedAverageSpeedKmh: number;
  stopAllowanceMinutesPerIntermediateStop: number;
  distanceEstimationMethod: string;
  etaEstimationMethod: string;
}

export interface ShipmentTimelineEvent {
  id: number;
  shipmentId: number;
  status: string;
  locationName: string | null;
  description: string | null;
  recordedAt: string;
  recordedByEmail: string | null;
}

export interface TripLookupOption {
  id: number;
  tripNumber: string;
  originAddress: string;
  destinationAddress: string;
  status: string;
}
