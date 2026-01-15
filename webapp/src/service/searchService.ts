export interface SearchCriteria {
	priceMin: number | null;
	priceMax: number | null;
	squareMetersMin: number | null;
	squareMetersMax: number | null;
	transport: number;
	amenities: number;
}

const searchService = {
	/**
	 * Performs a search with the given criteria.
	 * @param criteria - The search criteria
	 * @returns true (dummy implementation)
	 */
	search(criteria: SearchCriteria): boolean {
		console.log("Search called with criteria:", criteria);
		return true;
	},
};

export default searchService;
