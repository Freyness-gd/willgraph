import type { ListingCriteria } from "./ListingCriteria";
import type { TransportCriteria } from "./TransportCriteria";
import type { PriorityItemDto } from "./PriorityItemDto";

export interface ListingSearchFilterDto {
	listing: ListingCriteria | null;
	transport: TransportCriteria | null;
	amenityPriorities: PriorityItemDto[] | null;
	poiPriorities: PriorityItemDto[] | null;
}
