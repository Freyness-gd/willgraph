<script lang="ts" setup>
import { computed, ref } from "vue";
import { LControl, LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import L, { type LeafletMouseEvent, type Map as LeafletMap } from "leaflet";
import type { Municipality } from "src/types/Municipality";
import type { Point } from "src/types/Point";
import type { StationDistanceDto } from "src/types/Station";
import type { RealEstateDto } from "src/types/RealEstate";
import { useGeoStore } from "stores/geoStore";
import { regionsToGeoJson } from "src/mapper/MunicipalityMapper";
import { useMapLayers, useMapMarkers } from "src/composables/map";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
const zoomRef = ref<number>(14);

// Vienna bounds with ~25km buffer in all directions
// Vienna center: 48.2087334, 16.3736765
// 1 degree latitude ≈ 111km, 1 degree longitude ≈ 75km at this latitude
// 25km ≈ 0.225 degrees latitude, 0.33 degrees longitude
const viennaMaxBounds: [[number, number], [number, number]] = [
	[47.98, 16.04], // Southwest corner
	[48.44, 16.71], // Northeast corner
];

// Initialize composables
const mapMarkers = useMapMarkers();
const mapLayers = useMapLayers();

const emit = defineEmits<{
	(e: "map-click", lat: number, lon: number): void;
	(e: "estate-select", estate: RealEstateDto): void;
}>();

const geoJson = computed(() => regionsToGeoJson(geoStore.getSelectedRegions));

const onMapReady = (map: LeafletMap) => {
	console.log("L keys:", Object.keys(L));
	console.log("heatLayer in L:", (L as any).heatLayer);
	mapRef.value = map;

	// Initialize composables with map reference
	mapMarkers.init(map);
	mapLayers.init(map);

	// Add scale control
	L.control.scale({ position: "bottomleft", imperial: false }).addTo(map);
};

/**
 * Clear all points/layers from the map
 */
const clearPoints = () => {
	mapLayers.clearPoints();
};

/**
 * Draw municipalities as polygons on the map
 */
const drawMunicipalities = (munis: Municipality[]) => {
	mapLayers.drawMunicipalities(munis);
};

/**
 * Draw heat points on the map
 * @param points Array of [latitude, longitude, intensity] tuples
 */
const drawHeatPoints = (points: [number, number, number][]) => {
	mapLayers.drawHeatPoints(
		points,
		(lat, lon) => geoStore.findEstatesAtCoordinates(lat, lon),
		(estate) => emit("estate-select", estate)
	);
};

/**
 * Adds a POI marker to the map
 */
const addPoiMarker = (poi: Point) => {
	mapMarkers.addPoiMarker(poi);
};

/**
 * Removes a POI marker from the map
 */
const removePoiMarker = (poiId: string) => {
	mapMarkers.removePoiMarker(poiId);
};

/**
 * Adds station markers to the map
 */
const addStationMarkers = (stations: StationDistanceDto[]) => {
	mapMarkers.addStationMarkers(stations);
};

/**
 * Clears all station markers from the map
 */
const clearStationMarkers = () => {
	mapMarkers.clearStationMarkers();
};

/**
 * Adds a transport marker with a radius circle to the map
 */
const addTransportMarker = (lat: number, lon: number, radius: number = 100) => {
	mapMarkers.addTransportMarker(lat, lon, radius);
};

/**
 * Clears the transport marker and circle from the map
 */
const clearTransportMarker = () => {
	mapMarkers.clearTransportMarker();
};

/**
 * Sets the opacity of the heat layer
 * @param opacity Value between 0 and 1
 */
const setHeatLayerOpacity = (opacity: number) => {
	mapLayers.setHeatLayerOpacity(opacity);
};

/**
 * Shows a red marker at the selected estate location
 */
const showSelectedEstateMarker = (lat: number, lon: number) => {
	mapLayers.showSelectedEstateMarker(lat, lon);
};

/**
 * Clears the selected estate marker
 */
const clearSelectedEstateMarker = () => {
	mapLayers.clearSelectedEstateMarker();
};

/**
 * Sets the map zoom to minimum zoom level
 * @returns Promise that resolves when zoom animation is complete
 */
const setZoomToMin = (): Promise<void> => {
	return mapLayers.setZoomToMin();
};

/**
 * Shows estate transport radius circle
 */
const showEstateTransportCircle = (lat: number, lon: number, radius: number) => {
	mapLayers.showEstateTransportCircle(lat, lon, radius);
};

/**
 * Updates estate transport circle radius
 */
const updateEstateTransportCircleRadius = (lat: number, lon: number, radius: number) => {
	mapLayers.updateEstateTransportCircleRadius(lat, lon, radius);
};

/**
 * Clears estate transport circle
 */
const clearEstateTransportCircle = () => {
	mapLayers.clearEstateTransportCircle();
};

/**
 * Shows estate transport station markers
 */
const showEstateTransportStations = (
	stations: Array<{ name: string; type: string; line?: string; location?: { latitude: number; longitude: number } }>
) => {
	mapLayers.showEstateTransportStations(stations);
};

/**
 * Clears estate transport station markers
 */
const clearEstateTransportStations = () => {
	mapLayers.clearEstateTransportStations();
};

/**
 * Clears all estate transport layers
 */
const clearEstateTransport = () => {
	mapLayers.clearEstateTransport();
};

/**
 * Shows estate amenity markers
 */
const showEstateAmenities = (
	amenities: Array<{ name: string; amenityType: string; location?: { latitude: number; longitude: number } }>
) => {
	mapLayers.showEstateAmenities(amenities);
};

/**
 * Clears estate amenity markers
 */
const clearEstateAmenities = () => {
	mapLayers.clearEstateAmenities();
};

/**
 * Shows the amenity circle around the estate
 */
const showEstateAmenitiesCircle = (lat: number, lon: number, radius: number) => {
	mapLayers.showEstateAmenitiesCircle(lat, lon, radius);
};

/**
 * Updates the amenity circle radius (keeps same semantics as transport helper)
 */
const updateEstateAmenitiesCircleRadius = (lat: number, lon: number, radius: number) => {
	// mapLayers only needs radius to update the circle, but accept lat/lon for parity with transport API
	mapLayers.updateEstateAmenitiesCircleRadius(radius);
};

/**
 * Clears amenity circle
 */
const clearEstateAmenitiesCircle = () => {
	mapLayers.clearEstateAmenitiesCircle();
};

/**
 * Clear all amenity layers (circle + markers)
 */
const clearAllEstateAmenities = () => {
	mapLayers.clearAllEstateAmenities();
};

/**
 * Pan to a specific location with optional zoom level
 * @param lat Latitude
 * @param lon Longitude
 * @param zoom Zoom level (default: 12)
 */
const panTo = (lat: number, lon: number, zoom: number = 14) => {
	if (!mapRef.value) {
		console.warn("Map not ready for panning");
		return;
	}
	console.log(`Panning to [${lat}, ${lon}] at zoom ${zoom}`);
	mapRef.value.flyTo([lat, lon], zoom, { animate: true, duration: 0.5 });
};

/**
 * Handles map click events and emits the coordinates
 */
const handleMapClick = (event: LeafletMouseEvent) => {
	emit("map-click", event.latlng.lat, event.latlng.lng);
};

// Expose methods for external usage
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
	showEstateAmenities,
	clearEstateAmenities,
	showEstateAmenitiesCircle,
	updateEstateAmenitiesCircleRadius,
	clearEstateAmenitiesCircle,
	clearAllEstateAmenities,
	panTo,
});
</script>

<template>
	<l-map
		ref="mapRef"
		:center="[48.2087334, 16.3736765]"
		:max-bounds="viennaMaxBounds"
		:max-bounds-viscosity="1.0"
		:maxZoom="18"
		:minZoom="10"
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
