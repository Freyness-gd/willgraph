export interface PoiDistanceDto {
  name: string;
  category: string;
  distanceInMeters: number;
  amenityType: string;
  walkingDurationInMinutes: number;
  location: {
    latitude: number;
    longitude: number;
  };
}
