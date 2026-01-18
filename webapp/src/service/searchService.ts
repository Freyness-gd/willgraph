export interface SelectedAmenity {
	category: string;
	radius: number;
}

export interface SearchCriteria {
	priceMin: number | null;
	priceMax: number | null;
	squareMetersMin: number | null;
	squareMetersMax: number | null;
	transport: number;
	amenities: SelectedAmenity[];
}

const searchService = {
	/**
	 * Performs a search with the given criteria.
	 * @param criteria - The search criteria
	 * @returns true (dummy implementation)
	 */
	search(criteria: SearchCriteria): boolean {
		console.log("Search called with criteria:", criteria);
		console.log("Selected amenities:", criteria.amenities);
		return true;
	},
};

export default searchService;
