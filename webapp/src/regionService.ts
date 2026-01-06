const VIENNA_CENTER: [number, number] = [48.2087334, 16.3736765]; // [lat, lon]

const randomOffset = (maxDegrees = 0.005) => (Math.random() - 0.5) * 2 * maxDegrees;

const regionService = {
	/**
	 * Fetches region points and returns an array of [lat, lon] pairs.
	 * For now returns 2 random points near Vienna's center and logs the regionName.
	 */
	async fetchRegionPoints(regionName: string): Promise<[number, number][]> {
		console.log("fetchRegionPoints called for region:", regionName);

		// small await to satisfy linter for async functions
		await Promise.resolve();

		const p1: [number, number] = [VIENNA_CENTER[0] + randomOffset(0.003), VIENNA_CENTER[1] + randomOffset(0.003)];
		const p2: [number, number] = [VIENNA_CENTER[0] + randomOffset(0.003), VIENNA_CENTER[1] + randomOffset(0.003)];

		console.log("Generated points:", [p1, p2]);
		return [p1, p2];
	},
};

export default regionService;
