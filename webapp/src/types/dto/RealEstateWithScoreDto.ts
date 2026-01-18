import type { RealEstateDto } from "src/types/RealEstate";

export interface RealEstateWithScoreDto {
	listing: RealEstateDto;
	score: number;
}
