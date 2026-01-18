export interface StationDistanceDto {
  name: string;
  type: string | null;
  line: string | null;
  location: { x: number; y: number };
  distanceInMeters: number | null;
  travelTimeInMinutes: number;
  segmentType?: string;
}

export interface TransportPathDto {
  numberOfStops: number;
  walkToStationMeters: number;
  walkFromStationMeters: number;
  stations: StationDistanceDto[];
}
