import type { RealEstateDto } from "src/types/RealEstate";
import type { StationDistanceDto } from "src/types/Station";

/**
 * Popup content builder for map markers
 */
export const PopupBuilder = {
	/**
	 * Creates popup content for real estate listings at a specific coordinate
	 * Each listing is clickable and has a data-estate-index attribute
	 */
	createEstatePopup(estates: RealEstateDto[], lat: number, lon: number): string {
		if (estates.length === 0) {
			return this.createCoordinatesPopup(lat, lon);
		}

		const estateListings = estates
			.map((estate, index) => {
				const title = estate.title || "Untitled";
				const price = estate.price !== null && estate.price !== undefined ? `€${estate.price.toLocaleString()}` : "N/A";
				const area = estate.livingArea !== null && estate.livingArea !== undefined ? `${estate.livingArea}m²` : "";
				return `
					<div class="estate-popup-item" data-estate-index="${index}" style="padding: 8px 6px; border-bottom: 1px solid #eee; cursor: pointer; transition: background-color 0.2s; border-radius: 4px;">
						<div style="display: flex; justify-content: space-between; align-items: center;">
							<strong style="font-size: 12px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${title}</strong>
							<span style="color: #1976d2; font-size: 11px; margin-left: 8px;">▶</span>
						</div>
						<div style="margin-top: 4px;">
							<span style="color: #1976d2; font-weight: bold;">${price}</span>
							${area ? `<span style="color: #666; margin-left: 8px;">${area}</span>` : ""}
						</div>
					</div>
				`;
			})
			.join("");

		return `
			<div class="estate-popup-container" style="min-width: 220px; max-width: 320px; max-height: 320px; overflow-y: auto;">
				<strong style="font-size: 14px; display: block; margin-bottom: 8px;">Real Estate Listings (${estates.length})</strong>
				<div style="font-size: 11px; color: #666; margin-bottom: 8px;">Click to view details</div>
				<div class="estate-popup-list">
					${estateListings}
				</div>
				<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #ddd; font-size: 11px; color: #888;">
					Lat: ${lat.toFixed(6)}, Lon: ${lon.toFixed(6)}
				</div>
			</div>
			<style>
				.estate-popup-item:hover {
					background-color: #e3f2fd !important;
				}
			</style>
		`;
	},

	/**
	 * Creates simple coordinates popup
	 */
	createCoordinatesPopup(lat: number, lon: number): string {
		return `
			<div style="min-width: 120px;">
				<strong>Coordinates</strong><br/>
				<span>Lat: ${lat.toFixed(6)}</span><br/>
				<span>Lon: ${lon.toFixed(6)}</span>
			</div>
		`;
	},

	/**
	 * Creates popup content for a station marker
	 */
	createStationPopup(station: StationDistanceDto): string {
		return `
			<div style="min-width: 150px;">
				<strong style="font-size: 14px;">${station.name}</strong><br/>
				<span style="color: #666;">Type: ${station.type}</span><br/>
				<span style="color: #666;">Line: ${station.line}</span><br/>
				<span style="color: #888; font-size: 12px;">
					${station.distanceInMeters?.toFixed(0) || "N/A"}m •
					${station.walkingDurationInMinutes?.toFixed(1) || "N/A"} min
				</span>
			</div>
		`;
	},

	/**
	 * Creates popup content for transport search marker
	 */
	createTransportPopup(radius: number): string {
		return `<strong>Transport Search Point</strong><br/>Radius: ${radius}m`;
	},

	/**
	 * Creates popup content for municipality polygon
	 */
	createMunicipalityPopup(name: string): string {
		return `<strong>${name}</strong>`;
	},
};
