import axios from "axios";

interface EstateLocation {
	x: number;
	y: number;
}

interface EstateAddress {
	location: EstateLocation;
}

interface Estate {
	address: EstateAddress;
}

const regionService = {
	/**
	 * Fetches region points from backend API and returns an array of [lat, lon] pairs.
	 * Makes GET request to http://localhost:8080/api/estate?regionName={regionName}
	 */
	async fetchRegionPoints(regionName: string): Promise<[number, number][]> {
		console.log("fetchRegionPoints called for region:", regionName);

		try {
			const response = await axios.get<Estate[]>("http://localhost:8080/api/estate", {
				params: {
					regionName,
				},
			});

			console.log("API Response:", response.data);

			const points: [number, number][] = response.data.map((estate) => [
				estate.address.location.y, // latitude
				estate.address.location.x, // longitude
			]);

			console.log("Extracted points:", points);
			return points;
		} catch (error) {
			console.error("Error fetching region points:", error);
			return [];
		}
	},
};

export default regionService;
