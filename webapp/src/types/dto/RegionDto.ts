export interface GeographicPoint2d {
	latitude: number;
	longitude: number;
}

export interface RegionDto {
	name: string;
	iso: string;
	geometry: GeoJSON.Geometry;
	center: GeographicPoint2d;
}
