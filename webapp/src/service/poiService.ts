import axios from "axios";
import type { PoiDistanceDto } from "src/types/Poi";

const poiService = {
	/**
	 * Fetches nearby POIs (amenities) from backend API.
	 * Makes GET request to http://localhost:8080/api/pois/nearby?lat={lat}&lng={lng}&radius={radius}
	 */
	async findPoIsNearby(lat: number, lng: number, radius: number = 1000): Promise<PoiDistanceDto[]> {
		console.log("findPoIsNearby called for lat:", lat, "lng:", lng, "radius:", radius);

		try {
			const response = await axios.get<PoiDistanceDto[]>("http://localhost:8080/pois/nearby", {
				params: {
					lat,
					lng,
					radius,
				},
			});

			console.log("POI API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error fetching nearby POIs:", error);
			return [];
		}
	},
};

export default poiService;
