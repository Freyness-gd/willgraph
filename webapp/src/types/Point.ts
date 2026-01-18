import {TransportPathDto} from "src/types/TransportPath";

export interface Point {
  id: string;
  lat: number;
  lon: number;
  color: string;
}

export interface PointToPointDistanceDto {
  fromLatitude: number;
  fromLongitude: number;
  toLatitude: number;
  toLongitude: number;
  distanceInMeters: number;
  walkingDurationInMinutes: number;
}

export interface PoiWithDistance extends Point {
  distance?: PointToPointDistanceDto | undefined;
  transportPath?: TransportPathDto | null;
  isLoadingRoute?: boolean;
}
