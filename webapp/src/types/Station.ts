export interface StationDistance {
	stationId: number | null;
	stationName: string | null;
	latitude: number | null;
	longitude: number | null;
	distance: number | null;
}

export interface StationDistanceDto {
	name: string;
	type: string;
	line: string;
	distanceInMeters: number;
	walkingDurationInMinutes: number;
	location: {
		latitude: number;
		longitude: number;
	};
}
