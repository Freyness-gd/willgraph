import axios from "axios";
import type { RealEstateDto } from "src/types/RealEstate";

const regionService = {
	/**
	 * Fetches region points from backend API and returns an array of [lat, lon] pairs.
	 * Makes GET request to http://localhost:8080/api/estate?region={regionName}
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
				.map((estate) => [estate.address!.location!.latitude!, estate.address!.location!.longitude!]);

			console.log("Extracted points:", points);
			return points;
		} catch (error) {
			console.error("Error fetching region points:", error);
			return [];
		}
	},

	/**
	 * Fetches all real estate data for a specific region.
	 * Makes GET request to http://localhost:8080/api/estate?region={regionName}&iso={iso}
	 * @param region The region name (optional)
	 * @param iso The ISO code (optional)
	 * @returns Array of RealEstateDto objects
	 */
	async fetchRealEstatesInRegion(region?: string, iso?: string): Promise<RealEstateDto[]> {
		console.log("fetchRealEstatesInRegion called for region:", region, "iso:", iso);

		try {
			const response = await axios.get<RealEstateDto[]>("http://localhost:8080/api/estate", {
				params: {
					region,
					iso,
				},
			});

			console.log("API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error fetching real estates in region:", error);
			return [];
		}
	},
};

export default regionService;
