import axios from "axios";
import type { RealEstateDto } from "src/types/RealEstate";

const regionService = {
	/**
	 * Fetches region points from backend API and returns an array of [lat, lon] pairs.
	 * Makes GET request to http://localhost:8080/api/estate?regionName={regionName}
	 */
	async fetchRegionPoints(regionName: string): Promise<[number, number][]> {
		console.log("fetchRegionPoints called for region:", regionName);

		try {
			const response = await axios.get<RealEstateDto[]>("http://localhost:8080/api/estate", {
				params: {
					region: regionName,
				},
			});

			console.log("API Response:", response.data);

			const points: [number, number][] = response.data
				.filter((estate) => estate.address?.location?.latitude != null && estate.address?.location?.longitude != null)
				.map((estate) => [estate.address!.location!.longitude!, estate.address!.location!.latitude!]);

			console.log("Extracted points:", points);
			return points;
		} catch (error) {
			console.error("Error fetching region points:", error);
			return [];
		}
	},
};

export default regionService;
