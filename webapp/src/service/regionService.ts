import axios from "axios";
import type { RealEstateDto } from "src/types/RealEstate";
import type { PointToPointDistanceDto } from "src/types/Point";
import type { ListingSearchFilterDto, RealEstateWithScoreDto, RegionDto } from "src/types/dto";

const regionService = {
	/**
	 * Search regions by query string.
	 * Makes GET request to http://localhost:8080/api/regions?q={query}&limit={limit}
	 * @param q Search query (optional)
	 * @param limit Maximum number of results (default 10)
	 * @returns Array of RegionDto objects
	 */
	async searchRegions(q?: string, limit: number = 10): Promise<RegionDto[]> {
		console.log("searchRegions called with query:", q, "limit:", limit);

		try {
			const response = await axios.get<RegionDto[]>("http://localhost:8080/api/regions", {
				params: {
					q,
					limit,
				},
			});

			console.log("Regions API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error searching regions:", error);
			return [];
		}
	},

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

	/**
	 * Calculates distance between two points.
	 * Makes GET request to /api/poi/distance
	 * @param fromLat Starting latitude
	 * @param fromLon Starting longitude
	 * @param toLat Destination latitude
	 * @param toLon Destination longitude
	 * @returns PointToPointDistanceDto with distance and walking duration
	 */
	async calculateDistanceBetweenPoints(
		fromLat: number,
		fromLon: number,
		toLat: number,
		toLon: number
	): Promise<PointToPointDistanceDto | null> {
		console.log("calculateDistanceBetweenPoints called:", { fromLat, fromLon, toLat, toLon });

		try {
			const response = await axios.get<PointToPointDistanceDto>("http://localhost:8080/api/poi/distance", {
				params: {
					fromLat,
					fromLon,
					toLat,
					toLon,
				},
			});

			console.log("Distance API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error calculating distance between points:", error);
			return null;
		}
	},

	/**
	 * Search estates using complex filters. Sends ListingSearchFilterDto in POST body to /api/estate/search
	 */
	async searchEstatesWithFilters(filter: ListingSearchFilterDto): Promise<RealEstateWithScoreDto[]> {
		console.log("searchEstatesWithFilters called with:", filter);
		try {
			const response = await axios.post<RealEstateWithScoreDto[]>("http://localhost:8080/api/estate/search", filter);
			console.log("Search API Response:", response.data);
			return response.data;
		} catch (error) {
			console.error("Error searching estates with filters:", error);
			return [];
		}
	},
};

export default regionService;
