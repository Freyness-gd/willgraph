import axios from "axios";
import type { StationDistance, StationDistanceDto } from "src/types/Station";

const transportService = {
	/**
	 * Fetches nearby transport stations from backend API.
	 * Makes GET request to http://localhost:8080/api/transport/nearby?lat={lat}&lng={lng}&radius={radius}
	 */
	async findNearbyStations(lat: number, lng: number, radius: number = 1000): Promise<StationDistance[]> {
		console.log("findNearbyStations called for lat:", lat, "lng:", lng, "radius:", radius);

		try {
			const response = await axios.get<StationDistance[]>("http://localhost:8080/api/transport/nearby", {
				params: {
					lat,
					lng,
					radius,
				},
			});

			console.log("API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error fetching nearby stations:", error);
			return [];
		}
	},

	/**
	 * Fetches nearby transport stations with detailed info (name, type, line).
	 * Returns StationDistanceDto objects for display on map.
	 */
	async findNearbyStationsDetailed(lat: number, lng: number, radius: number = 100): Promise<StationDistanceDto[]> {
		console.log("findNearbyStationsDetailed called for lat:", lat, "lng:", lng, "radius:", radius);

		try {
			const response = await axios.get<StationDistanceDto[]>("http://localhost:8080/api/transport/nearby", {
				params: {
					lat,
					lng,
					radius,
				},
			});

			console.log("API Response (detailed):", response.data);
			return response.data;
		} catch (error) {
			console.error("Error fetching nearby stations (detailed):", error);
			return [];
		}
	},
};

export default transportService;
