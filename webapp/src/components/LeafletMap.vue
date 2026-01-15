<script lang="ts" setup>
import { computed, ref } from "vue";
import { LControl, LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import type { LeafletMouseEvent, Map as LeafletMap } from "leaflet";
// eslint-disable-next-line no-duplicate-imports
import L from "leaflet";
import type { Municipality } from "src/types/Municipality";
import type { Point } from "src/types/Point";
import type { StationDistanceDto } from "src/types/Station";
import { useGeoStore } from "stores/geoStore";
import { municipalitiesToGeoJson } from "src/mapper/MunicipalityMapper";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
let heatLayerRef: any = null;
let drawnLayerRef: L.FeatureGroup | null = null;
const zoomRef = ref<number>(14);

// POI Markers storage
const poiMarkersMap = new Map<string, L.Marker>();

// Station Markers storage
const stationMarkersMap = new Map<string, L.Marker>();

// Heat point markers for click interaction
let heatPointMarkersRef: L.LayerGroup | null = null;

// Transport marker with radius circle
let transportMarkerRef: L.Marker | null = null;
let transportCircleRef: L.Circle | null = null;

const emit = defineEmits<{
	(e: "map-click", lat: number, lon: number): void;
}>();

const geoJson = computed(() => municipalitiesToGeoJson(geoStore.getSelectedMunicipalities));

// Convert heatPoints from store to Leaflet format [lat, lng, intensity]
const heatPoints = computed(() => {
	const points = geoStore.heatPoints;
	const mappedPoints = points.map(
		(point) => [point.latitude, point.longitude, point.intensity] as [number, number, number]
	);
	console.log("Mapped points: ", mappedPoints);
	return mappedPoints;
});

const calculateRadiusFromZoom = (zoom: number): number => {
	const metersPerPixel = 40075016.686 / 2 ** (zoom + 8);
	const radiusInPixels = Math.round(300 / metersPerPixel); // 1000 meters = 1 km
	return Math.max(radiusInPixels, 10); // minimum radius of 10 pixels
};

/**
 * Entfernt Heat-Layer und alle gezeichneten Layer (Polygone/Marker)
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
};

/**
 * Zeichnet Municipalities als Leaflet-Polygone + Popup. Die Methode entfernt zuerst alte Punkte.
 */
const drawMunicipalities = (munis: Municipality[]) => {
	if (!mapRef.value) return;
	const map = mapRef.value;

	// Clear previous points
	// clearPoints();

	// Create a feature group to hold drawn shapes (FeatureGroup has getBounds)
	drawnLayerRef = L.featureGroup();

	munis.forEach((m) => {
		try {
			// municipality.boundary.coordinates is [[[ [lng,lat], ... ]]]
			const rings = m.boundary.coordinates;
			// Convert first (outer) ring to Leaflet lat-lng pairs
			const outerRing = rings && rings[0] && rings[0][0] ? rings[0][0] : [];
			const latLngs = outerRing.map((pos: [number, number]) => [pos[1], pos[0]] as [number, number]);

			if (latLngs.length > 0) {
				const polygon = L.polygon(latLngs, {
					color: "#1976d2",
					weight: 2,
					fillOpacity: 0.15,
				});
				polygon.bindPopup(`<strong>${m.name}</strong>`);
				drawnLayerRef!.addLayer(polygon as any);

				// optional: add a marker at centroid
				const centroid = calculateCentroid(latLngs);
				if (centroid) {
					const marker = L.circleMarker(centroid, {
						radius: 6,
						color: "#d32f2f",
						fillColor: "#d32f2f",
						fillOpacity: 0.9,
					});
					marker.bindPopup(`<strong>${m.name}</strong>`);
					drawnLayerRef!.addLayer(marker as any);
				}
			}
		} catch (err) {
			console.error("Error drawing municipality", m.name, err);
		}
	});

	// Add the feature group to the map
	if (drawnLayerRef && map) {
		drawnLayerRef.addTo(map as any);
		// Fit bounds to drawn layer
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
 * Berechnet einen einfachen Schwerpunkt (Centroid) eines Rings von LatLngs
 */
const calculateCentroid = (latLngs: [number, number][]) => {
	if (!latLngs || latLngs.length === 0) return null;
	let x = 0;
	let y = 0;
	latLngs.forEach((p) => {
		x += p[0];
		y += p[1];
	});
	return [x / latLngs.length, y / latLngs.length] as [number, number];
};

const onMapReady = (map: LeafletMap) => {
	console.log("L keys:", Object.keys(L));
	console.log("heatLayer in L:", (L as any).heatLayer);
	mapRef.value = map;

	// Add scale control
	L.control.scale({ position: "bottomleft", imperial: false }).addTo(map);

	// Create a new pane for heat layer
	if (!map.getPane("heatPane")) {
		map.createPane("heatPane");
	}

	const addHeatLayer = () => {
		// Clear existing heat layer
		if (heatLayerRef) {
			map.removeLayer(heatLayerRef);
			heatLayerRef = null;
		}
		const radius = calculateRadiusFromZoom(map.getZoom());
		const heat = (L as any).heatLayer(heatPoints.value, {
			radius,
			gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
			minOpacity: 0.8,
			pane: "heatPane",
		});
		heat.addTo(map);
		heatLayerRef = heat;
	};
	// Update radius when zoom level changes
	// map.on("zoomend", () => {
	// 	zoomRef.value = map.getZoom();
	// 	console.log("Zoom value:", zoomRef.value);
	// 	addHeatLayer();
	// });
};

/**
 * Zeichnet Heat-Punkte auf der Karte. Entfernt zuerst alte Punkte.
 * @param points Array von [latitude, longitude] Paaren
 */
const drawHeatPoints = (points: [number, number][]) => {
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

	// Convert [lat, lon] pairs to heat layer format [lat, lon, intensity]
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

		// Find estates at this location
		const estates = geoStore.findEstatesAtCoordinates(lat, lon);

		let popupContent: string;
		if (estates.length > 0) {
			const estateListings = estates
				.map((estate) => {
					const title = estate.title || "Untitled";
					const price = estate.price != null ? `€${estate.price.toLocaleString()}` : "N/A";
					const area = estate.livingArea != null ? `${estate.livingArea}m²` : "";
					return `<div style="padding: 4px 0; border-bottom: 1px solid #eee;">
					<strong style="font-size: 12px;">${title}</strong><br/>
					<span style="color: #1976d2; font-weight: bold;">${price}</span>
					${area ? `<span style="color: #666; margin-left: 8px;">${area}</span>` : ""}
				</div>`;
				})
				.join("");

			popupContent = `
				<div style="min-width: 200px; max-width: 300px; max-height: 300px; overflow-y: auto;">
					<strong style="font-size: 14px;">Real Estate Listings (${estates.length})</strong>
					<div style="margin-top: 8px;">
						${estateListings}
					</div>
					<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #ddd; font-size: 11px; color: #888;">
						Lat: ${lat.toFixed(6)}, Lon: ${lon.toFixed(6)}
					</div>
				</div>
			`;
		} else {
			popupContent = `
				<div style="min-width: 120px;">
					<strong>Coordinates</strong><br/>
					<span>Lat: ${lat.toFixed(6)}</span><br/>
					<span>Lon: ${lon.toFixed(6)}</span>
				</div>
			`;
		}

		marker.bindPopup(popupContent);
		heatPointMarkersRef!.addLayer(marker);
	});
	heatPointMarkersRef.addTo(map as any);
	console.log("Heat point markers added for click interaction");
};

/**
 * Adds a POI marker to the map
 * @param poi The Point object containing id, lat, lon, and color
 */
const addPoiMarker = (poi: Point) => {
	if (!mapRef.value) {
		console.warn("Map not ready for POI marker");
		return;
	}

	// Create custom icon with the POI color
	const icon = L.divIcon({
		className: "poi-marker-icon",
		html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${poi.color}" width="32" height="32">
			<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
		</svg>`,
		iconSize: [32, 32],
		iconAnchor: [16, 32],
	});

	const marker = L.marker([poi.lat, poi.lon], { icon });
	marker.addTo(mapRef.value as any);
	poiMarkersMap.set(poi.id, marker);
};

/**
 * Removes a POI marker from the map
 * @param poiId The id of the POI to remove
 */
const removePoiMarker = (poiId: string) => {
	const marker = poiMarkersMap.get(poiId);
	if (marker && mapRef.value) {
		mapRef.value.removeLayer(marker);
		poiMarkersMap.delete(poiId);
	}
};

/**
 * Adds station markers to the map with bus icons and popups
 * @param stations Array of StationDistanceDto objects
 */
const addStationMarkers = (stations: StationDistanceDto[]) => {
	if (!mapRef.value) {
		console.warn("Map not ready for station markers");
		return;
	}

	console.log("addStationMarkers called with", stations.length, "stations");
	console.log("First station data:", stations[0]);

	// Create custom bus icon
	const busIcon = L.divIcon({
		className: "station-marker-icon",
		html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#4CAF50" width="28" height="28">
			<path d="M4 16c0 .88.39 1.67 1 2.22V20c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h8v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4v10zm3.5 1c-.83 0-1.5-.67-1.5-1.5S6.67 14 7.5 14s1.5.67 1.5 1.5S8.33 17 7.5 17zm9 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm1.5-6H6V6h12v5z"/>
		</svg>`,
		iconSize: [28, 28],
		iconAnchor: [14, 28],
		popupAnchor: [0, -28],
	});

	stations.forEach((station, index) => {
		console.log(`Station ${index}:`, station);

		// Try to get coordinates from different possible structures
		let lat: number | undefined;
		let lng: number | undefined;

		if (station.location) {
			lat = station.location.latitude;
			lng = station.location.longitude;
		} else if ((station as any).latitude !== undefined && (station as any).longitude !== undefined) {
			// Fallback: coordinates directly on station object
			lat = (station as any).latitude;
			lng = (station as any).longitude;
		}

		if (lat === undefined || lng === undefined) {
			console.warn(`Station ${station.name} has no valid coordinates:`, station);
			return;
		}

		const stationKey = `${station.name}-${lat}-${lng}`;

		// Skip if marker already exists
		if (stationMarkersMap.has(stationKey)) {
			console.log(`Station ${station.name} already exists, skipping`);
			return;
		}

		console.log(`Creating marker for ${station.name} at [${lat}, ${lng}]`);

		const marker = L.marker([lat, lng], { icon: busIcon });

		// Create popup content with station info
		const popupContent = `
			<div style="min-width: 150px;">
				<strong style="font-size: 14px;">${station.name}</strong><br/>
				<span style="color: #666;">Type: ${station.type}</span><br/>
				<span style="color: #666;">Line: ${station.line}</span><br/>
				<span style="color: #888; font-size: 12px;">${station.distanceInMeters?.toFixed(0) || "N/A"}m • ${station.walkingDurationInMinutes?.toFixed(1) || "N/A"} min</span>
			</div>
		`;
		marker.bindPopup(popupContent);

		marker.addTo(mapRef.value as any);
		stationMarkersMap.set(stationKey, marker);
		console.log(`Marker added for ${station.name}`);
	});

	console.log(`Total markers in map: ${stationMarkersMap.size}`);
};

/**
 * Clears all station markers from the map
 */
const clearStationMarkers = () => {
	if (!mapRef.value) return;

	stationMarkersMap.forEach((marker) => {
		mapRef.value?.removeLayer(marker);
	});
	stationMarkersMap.clear();
};

/**
 * Adds a transport marker with a radius circle to the map
 * @param lat Latitude of the marker
 * @param lon Longitude of the marker
 * @param radius Radius in meters (default 100)
 */
const addTransportMarker = (lat: number, lon: number, radius: number = 100) => {
	if (!mapRef.value) {
		console.warn("Map not ready for transport marker");
		return;
	}

	// Remove existing transport marker and circle first
	clearTransportMarker();

	// Create custom transport icon (different from POI markers)
	const transportIcon = L.divIcon({
		className: "transport-marker-icon",
		html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FF5722" width="36" height="36">
			<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 2c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm0 10c-1.67 0-3-1.33-3-3h2c0 .55.45 1 1 1s1-.45 1-1h2c0 1.67-1.33 3-3 3z"/>
		</svg>`,
		iconSize: [36, 36],
		iconAnchor: [18, 36],
		popupAnchor: [0, -36],
	});

	// Create the marker
	transportMarkerRef = L.marker([lat, lon], { icon: transportIcon });
	transportMarkerRef.bindPopup(`<strong>Transport Search Point</strong><br/>Radius: ${radius}m`);
	transportMarkerRef.addTo(mapRef.value as any);

	// Create the radius circle with light orange color
	transportCircleRef = L.circle([lat, lon], {
		radius,
		color: "#FFA726",
		fillColor: "#FFB74D",
		fillOpacity: 0.3,
		weight: 2,
		dashArray: "5, 5",
	});
	transportCircleRef.addTo(mapRef.value as any);
};

/**
 * Clears the transport marker and circle from the map
 */
const clearTransportMarker = () => {
	if (!mapRef.value) return;

	if (transportMarkerRef) {
		mapRef.value.removeLayer(transportMarkerRef);
		transportMarkerRef = null;
	}

	if (transportCircleRef) {
		mapRef.value.removeLayer(transportCircleRef);
		transportCircleRef = null;
	}
};

/**
 * Handles map click events and emits the coordinates
 */
const handleMapClick = (event: LeafletMouseEvent) => {
	emit("map-click", event.latlng.lat, event.latlng.lng);
};

// Expose methods so external code can call map.clearPoints() / map.drawMunicipalities(...) / map.drawHeatPoints(...) / POI methods / Station methods
defineExpose({
	clearPoints,
	drawMunicipalities,
	drawHeatPoints,
	addPoiMarker,
	removePoiMarker,
	addStationMarkers,
	clearStationMarkers,
	addTransportMarker,
	clearTransportMarker,
});
</script>

<template>
	<l-map
		ref="mapRef"
		:center="[48.2087334, 16.3736765]"
		:maxZoom="16"
		:minZoom="14"
		:use-global-leaflet="true"
		:zoom="zoomRef"
		style="height: 100vh; width: 100vw"
		@click="handleMapClick"
		@ready="onMapReady"
	>
		<l-tile-layer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

		<l-control position="bottomleft">
			<div style="background: white; padding: 5px; border-radius: 4px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.2)">
				<div ref="scaleRef"></div>
			</div>
		</l-control>

		<l-geo-json v-if="geoJson.features.length > 0" :geojson="geoJson" />
	</l-map>
</template>

<style scoped>
/* nothing needed */
</style>
