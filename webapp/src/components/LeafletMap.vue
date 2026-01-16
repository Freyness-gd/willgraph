<script lang="ts" setup>
import { computed, ref } from "vue";
import { LControl, LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import L, { type LeafletMouseEvent, type Map as LeafletMap } from "leaflet";
import type { Municipality } from "src/types/Municipality";
import type { Point } from "src/types/Point";
import type { StationDistanceDto } from "src/types/Station";
import type { RealEstateDto } from "src/types/RealEstate";
import { useGeoStore } from "stores/geoStore";
import { municipalitiesToGeoJson } from "src/mapper/MunicipalityMapper";
import { useMapLayers, useMapMarkers } from "src/composables/map";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
const zoomRef = ref<number>(14);

// Initialize composables
const mapMarkers = useMapMarkers();
const mapLayers = useMapLayers();

const emit = defineEmits<{
	(e: "map-click", lat: number, lon: number): void;
	(e: "estate-select", estate: RealEstateDto): void;
}>();

const geoJson = computed(() => municipalitiesToGeoJson(geoStore.getSelectedMunicipalities));

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
 * @param points Array of [latitude, longitude] pairs
 */
const drawHeatPoints = (points: [number, number][]) => {
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
