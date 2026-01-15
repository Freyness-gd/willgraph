import axios from "axios";
import type { StationDistance } from "src/types/Station";

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
};

export default transportService;
