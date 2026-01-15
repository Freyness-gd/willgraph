import { ref } from "vue";
import L, { type Map as LeafletMap } from "leaflet";
import type { Municipality } from "src/types/Municipality";
import type { RealEstateDto } from "src/types/RealEstate";
import { PopupBuilder } from "./mapPopups";

/**
 * Composable for managing map layers (Heat, Municipality polygons, etc.)
 */
export function useMapLayers() {
	const mapRef = ref<LeafletMap | null>(null);

	// Layer references
	let heatLayerRef: any = null;
	let drawnLayerRef: L.FeatureGroup | null = null;
	let heatPointMarkersRef: L.LayerGroup | null = null;

	/**
	 * Initialize the composable with a map reference
	 */
	const init = (map: LeafletMap) => {
		mapRef.value = map;

		// Create heat pane if not exists
		if (!map.getPane("heatPane")) {
			map.createPane("heatPane");
		}
	};

	/**
	 * Calculate heat layer radius based on zoom level
	 */
	const calculateRadiusFromZoom = (zoom: number): number => {
		const metersPerPixel = 40075016.686 / 2 ** (zoom + 8);
		const radiusInPixels = Math.round(300 / metersPerPixel);
		return Math.max(radiusInPixels, 10);
	};

	/**
	 * Calculate centroid of a polygon
	 */
	const calculateCentroid = (latLngs: [number, number][]): [number, number] | null => {
		if (!latLngs || latLngs.length === 0) return null;
		let x = 0;
		let y = 0;
		latLngs.forEach((p) => {
			x += p[0];
			y += p[1];
		});
		return [x / latLngs.length, y / latLngs.length];
	};

	/**
	 * Clear all points/layers from the map
	 */
	const clearPoints = () => {
		if (!mapRef.value) return;
		const map = mapRef.value;

		if (heatLayerRef) {
			try {
				map.removeLayer(heatLayerRef);
			} catch (error) {
				console.warn("Failed to remove heatLayer:", error);
			}
			heatLayerRef = null;
		}

		if (drawnLayerRef) {
			try {
				map.removeLayer(drawnLayerRef as any);
			} catch (error) {
				console.warn("Failed to remove drawn layer:", error);
			}
			drawnLayerRef = null;
		}

		if (heatPointMarkersRef) {
			try {
				map.removeLayer(heatPointMarkersRef);
			} catch (error) {
				console.warn("Failed to remove heat point markers:", error);
			}
			heatPointMarkersRef = null;
		}
	};

	/**
	 * Draw municipalities as polygons on the map
	 */
	const drawMunicipalities = (munis: Municipality[]) => {
		if (!mapRef.value) return;
		const map = mapRef.value;

		drawnLayerRef = L.featureGroup();

		munis.forEach((m) => {
			try {
				const rings = m.boundary.coordinates;
				const outerRing = rings && rings[0] && rings[0][0] ? rings[0][0] : [];
				const latLngs = outerRing.map((pos: [number, number]) => [pos[1], pos[0]] as [number, number]);

				if (latLngs.length > 0) {
					const polygon = L.polygon(latLngs, {
						color: "#1976d2",
						weight: 2,
						fillOpacity: 0.15,
					});
					polygon.bindPopup(PopupBuilder.createMunicipalityPopup(m.name));
					drawnLayerRef!.addLayer(polygon as any);

					const centroid = calculateCentroid(latLngs);
					if (centroid) {
						const marker = L.circleMarker(centroid, {
							radius: 6,
							color: "#d32f2f",
							fillColor: "#d32f2f",
							fillOpacity: 0.9,
						});
						marker.bindPopup(PopupBuilder.createMunicipalityPopup(m.name));
						drawnLayerRef!.addLayer(marker as any);
					}
				}
			} catch (err) {
				console.error("Error drawing municipality", m.name, err);
			}
		});

		if (drawnLayerRef && map) {
			drawnLayerRef.addTo(map as any);
			try {
				const bounds = drawnLayerRef.getBounds();
				if (bounds && bounds.isValid()) {
					map.fitBounds(bounds.pad(0.2));
				}
			} catch (err) {
				console.warn("Could not fit map bounds to drawn layer:", err);
			}
		}
	};

	/**
	 * Draw heat points on the map
	 * @param points Array of [latitude, longitude] pairs
	 * @param findEstatesAtCoordinates Function to find estates at coordinates
	 * @param onEstateSelect Callback when an estate is selected from popup
	 */
	const drawHeatPoints = (
		points: [number, number][],
		findEstatesAtCoordinates?: (lat: number, lon: number) => RealEstateDto[],
		onEstateSelect?: (estate: RealEstateDto) => void
	) => {
		if (!mapRef.value) {
			console.warn("Map not ready for heat points");
			return;
		}
		const map = mapRef.value;

		// Clear previous heat layer
		if (heatLayerRef) {
			try {
				map.removeLayer(heatLayerRef);
			} catch (e) {
				console.warn("Failed to remove old heat layer:", e);
			}
			heatLayerRef = null;
		}

		// Clear previous heat point markers
		if (heatPointMarkersRef) {
			try {
				map.removeLayer(heatPointMarkersRef);
			} catch (e) {
				console.warn("Failed to remove old heat point markers:", e);
			}
			heatPointMarkersRef = null;
		}

		// Ensure heat pane exists
		if (!map.getPane("heatPane")) {
			map.createPane("heatPane");
		}

		// Convert [lat, lon] pairs to heat layer format
		const heatData = points.map((p) => [p[0], p[1], 0.8] as [number, number, number]);
		console.log("Creating heat layer with", heatData.length, "points");

		try {
			const radius = calculateRadiusFromZoom(map.getZoom());
			const heat = (L as any).heatLayer(heatData, {
				radius,
				gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
				minOpacity: 0.8,
				pane: "heatPane",
			});
			heat.addTo(map);
			heatLayerRef = heat;
			console.log("Heat layer added successfully");
		} catch (err) {
			console.error("Failed to draw heat points:", err);
		}

		// Add clickable markers for each heat point
		heatPointMarkersRef = L.layerGroup();
		points.forEach((point) => {
			const [lat, lon] = point;
			const marker = L.circleMarker([lat, lon], {
				radius: 8,
				fillColor: "transparent",
				color: "transparent",
				fillOpacity: 0,
				weight: 0,
			});

			// Find estates at this location if function provided
			const estates = findEstatesAtCoordinates ? findEstatesAtCoordinates(lat, lon) : [];
			const popupContent = PopupBuilder.createEstatePopup(estates, lat, lon);
			const popup = L.popup().setContent(popupContent);
			marker.bindPopup(popup);

			// Add click handler for estate selection when popup opens
			marker.on("popupopen", () => {
				if (onEstateSelect && estates.length > 0) {
					const popupElement = popup.getElement();
					if (popupElement) {
						const items = popupElement.querySelectorAll(".estate-popup-item");
						items.forEach((item) => {
							item.addEventListener("click", (e) => {
								const target = e.currentTarget as HTMLElement;
								const index = Number.parseInt(target.dataset.estateIndex || "0", 10);
								const selectedEstate = estates[index];
								if (selectedEstate) {
									onEstateSelect(selectedEstate);
									marker.closePopup();
								}
							});
						});
					}
				}
			});

			heatPointMarkersRef!.addLayer(marker);
		});
		heatPointMarkersRef.addTo(map as any);
		console.log("Heat point markers added for click interaction");
	};

	/**
	 * Clear only the heat layer
	 */
	const clearHeatLayer = () => {
		if (!mapRef.value) return;

		if (heatLayerRef) {
			try {
				mapRef.value.removeLayer(heatLayerRef);
			} catch (e) {
				console.warn("Failed to remove heat layer:", e);
			}
			heatLayerRef = null;
		}

		if (heatPointMarkersRef) {
			try {
				mapRef.value.removeLayer(heatPointMarkersRef);
			} catch (e) {
				console.warn("Failed to remove heat point markers:", e);
			}
			heatPointMarkersRef = null;
		}
	};

	return {
		init,
		clearPoints,
		drawMunicipalities,
		drawHeatPoints,
		clearHeatLayer,
		calculateRadiusFromZoom,
	};
}
