import axios from "axios";
import type { StationDistance, StationDistanceDto } from "src/types/Station";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

const transportService = {
	/**
	 * Fetches nearby transport stations from backend API.
	 * Makes GET request to /api/transport/nearby?lat={lat}&lng={lng}&radius={radius}
	 */
	async findNearbyStations(lat: number, lng: number, radius: number = 1000): Promise<StationDistance[]> {
		console.log("findNearbyStations called for lat:", lat, "lng:", lng, "radius:", radius);

		try {
			const response = await axios.get<StationDistance[]>(`${API_BASE_URL}/api/transport/nearby`, {
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
			const response = await axios.get<StationDistanceDto[]>(`${API_BASE_URL}/api/transport/nearby`, {
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
