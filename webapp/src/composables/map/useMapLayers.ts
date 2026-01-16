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
	let selectedEstateMarkerRef: L.Marker | null = null;
	let estateTransportCircleRef: L.Circle | null = null;
	let estateTransportStationsRef: L.LayerGroup | null = null;

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

	/**
	 * Set the opacity of the heat layer
	 * @param opacity Value between 0 and 1
	 */
	const setHeatLayerOpacity = (opacity: number) => {
		if (heatLayerRef) {
			try {
				// Heat layer uses canvas, we need to access the canvas element
				const canvas = heatLayerRef._canvas;
				if (canvas) {
					canvas.style.opacity = opacity.toString();
				}
			} catch (e) {
				console.warn("Failed to set heat layer opacity:", e);
			}
		}
	};

	/**
	 * Show a marker at the selected estate location
	 * @param lat Latitude
	 * @param lon Longitude
	 */
	const showSelectedEstateMarker = (lat: number, lon: number) => {
		if (!mapRef.value) return;

		// Remove existing marker first
		clearSelectedEstateMarker();

		// Create home icon marker with red background circle
		const selectedIcon = L.divIcon({
			className: "selected-estate-marker",
			html: `<div style="position: relative; width: 44px; height: 44px;">
				<div style="position: absolute; width: 44px; height: 44px; background: #d32f2f; border-radius: 50%; box-shadow: 0 3px 8px rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center;">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white" width="26" height="26">
						<path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
					</svg>
				</div>
				<div style="position: absolute; bottom: -8px; left: 50%; transform: translateX(-50%); width: 0; height: 0; border-left: 8px solid transparent; border-right: 8px solid transparent; border-top: 10px solid #d32f2f;"></div>
			</div>`,
			iconSize: [44, 54],
			iconAnchor: [22, 54],
		});

		selectedEstateMarkerRef = L.marker([lat, lon], {
			icon: selectedIcon,
			zIndexOffset: 1000, // Make sure it's on top
		});
		selectedEstateMarkerRef.addTo(mapRef.value as any);
	};

	/**
	 * Clear the selected estate marker
	 */
	const clearSelectedEstateMarker = () => {
		if (selectedEstateMarkerRef && mapRef.value) {
			mapRef.value.removeLayer(selectedEstateMarkerRef);
			selectedEstateMarkerRef = null;
		}
	};

	/**
	 * Set the map zoom to minimum zoom level
	 * @returns Promise that resolves when zoom animation is complete
	 */
	const setZoomToMin = (): Promise<void> => {
		return new Promise((resolve) => {
			if (!mapRef.value) {
				resolve();
				return;
			}

			const map = mapRef.value;
			const minZoom = map.getMinZoom();
			const currentZoom = map.getZoom();

			// If already at minZoom, resolve immediately
			if (currentZoom === minZoom) {
				resolve();
				return;
			}

			// Listen for zoomend event once
			const onZoomEnd = () => {
				map.off("zoomend", onZoomEnd);
				resolve();
			};
			map.on("zoomend", onZoomEnd);

			// Start zoom animation
			map.setZoom(minZoom);
		});
	};

	/**
	 * Show estate transport radius circle on the map
	 * @param lat Latitude of the estate
	 * @param lon Longitude of the estate
	 * @param radius Radius in meters
	 */
	const showEstateTransportCircle = (lat: number, lon: number, radius: number) => {
		if (!mapRef.value) return;

		// Clear existing circle first
		clearEstateTransportCircle();

		// Create the radius circle with light green color
		estateTransportCircleRef = L.circle([lat, lon], {
			radius,
			color: "#4CAF50",
			fillColor: "#81C784",
			fillOpacity: 0.2,
			weight: 2,
			dashArray: "5, 5",
		});
		estateTransportCircleRef.addTo(mapRef.value as any);
	};

	/**
	 * Update estate transport circle radius (just redraw with new radius)
	 * @param radius New radius in meters
	 */
	const updateEstateTransportCircleRadius = (lat: number, lon: number, radius: number) => {
		showEstateTransportCircle(lat, lon, radius);
	};

	/**
	 * Clear estate transport circle from the map
	 */
	const clearEstateTransportCircle = () => {
		if (estateTransportCircleRef && mapRef.value) {
			mapRef.value.removeLayer(estateTransportCircleRef);
			estateTransportCircleRef = null;
		}
	};

	/**
	 * Show estate transport station markers on the map
	 * @param stations Array of station objects with location
	 */
	const showEstateTransportStations = (
		stations: Array<{ name: string; type: string; line?: string; location?: { latitude: number; longitude: number } }>
	) => {
		if (!mapRef.value) return;

		// Clear existing station markers
		clearEstateTransportStations();

		estateTransportStationsRef = L.layerGroup();

		const busIcon = L.divIcon({
			className: "estate-station-marker",
			html: `<div style="background: #4CAF50; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">
				<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white" width="16" height="16">
					<path d="M4 16c0 .88.39 1.67 1 2.22V20c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h8v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4v10zm3.5 1c-.83 0-1.5-.67-1.5-1.5S6.67 14 7.5 14s1.5.67 1.5 1.5S8.33 17 7.5 17zm9 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm1.5-6H6V6h12v5z"/>
				</svg>
			</div>`,
			iconSize: [24, 24],
			iconAnchor: [12, 12],
		});

		stations.forEach((station) => {
			if (!station.location?.latitude || !station.location?.longitude) return;

			const marker = L.marker([station.location.latitude, station.location.longitude], { icon: busIcon });
			marker.bindPopup(`
				<div style="min-width: 120px;">
					<strong>${station.name}</strong><br/>
					<span style="color: #666;">Type: ${station.type}</span><br/>
					${station.line ? `<span style="color: #1976d2;">Line: ${station.line}</span>` : ""}
				</div>
			`);
			estateTransportStationsRef?.addLayer(marker);
		});

		estateTransportStationsRef?.addTo(mapRef.value as any);
	};

	/**
	 * Clear estate transport station markers from the map
	 */
	const clearEstateTransportStations = () => {
		if (estateTransportStationsRef && mapRef.value) {
			mapRef.value.removeLayer(estateTransportStationsRef);
			estateTransportStationsRef = null;
		}
	};

	/**
	 * Clear all estate transport layers (circle and stations)
	 */
	const clearEstateTransport = () => {
		clearEstateTransportCircle();
		clearEstateTransportStations();
	};

	return {
		init,
		clearPoints,
		drawMunicipalities,
		drawHeatPoints,
		clearHeatLayer,
		calculateRadiusFromZoom,
		setHeatLayerOpacity,
		showSelectedEstateMarker,
		clearSelectedEstateMarker,
		setZoomToMin,
		showEstateTransportCircle,
		updateEstateTransportCircleRadius,
		clearEstateTransportCircle,
		showEstateTransportStations,
		clearEstateTransportStations,
		clearEstateTransport,
	};
}
