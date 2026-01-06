<script lang="ts" setup>
import { computed, ref } from "vue";
import { LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import type { Map as LeafletMap } from "leaflet";
// eslint-disable-next-line no-duplicate-imports
import L from "leaflet";
import { useGeoStore } from "stores/geoStore";
import { municipalitiesToGeoJson } from "src/mapper/MunicipalityMapper";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
let heatLayerRef: any = null;

const geoJson = computed(() => municipalitiesToGeoJson(geoStore.getSelectedMunicipalities));

const heatPoints: [number, number, number][] = [
	[48.2085, 16.373, 1], // 1st district – Innere Stadt
	[48.22, 16.392, 0.8], // 2nd district – Leopoldstadt
	[48.177, 16.3, 0.6], // 12th district – Meidling
	[48.138, 16.284, 0.4], // 23rd district – Liesing
];

const calculateRadiusFromZoom = (zoom: number): number => {
	// 1 km at the equator is approximately 40075 km / 2^(zoom + 8) pixels
	// We want the radius to represent 1 km in pixels
	// At zoom level z, 1 km ≈ Math.pow(2, zoom + 8) / 40075 pixels
	// But leaflet's heatLayer radius is in pixels on the map
	// Using the formula: pixels = Math.pow(2, zoom + 8) / 40075
	// For 1 km, we get: radius ≈ 40075 / Math.pow(2, zoom + 8) in world units
	// Converting to pixels for heatLayer
	const metersPerPixel = 40075016.686 / 2 ** (zoom + 8);
	const radiusInPixels = Math.round(1000 / metersPerPixel); // 1000 meters = 1 km
	return Math.max(radiusInPixels, 10); // minimum radius of 10 pixels
};

const clearPoints = () => {
	if (mapRef.value && heatLayerRef) {
		mapRef.value.removeLayer(heatLayerRef);
		heatLayerRef = null;
	}
};

const onMapReady = (map: LeafletMap) => {
	console.log("L keys:", Object.keys(L));
	console.log("heatLayer in L:", (L as any).heatLayer);
	mapRef.value = map;

	const addHeatLayer = () => {
		// Clear existing heat layer
		if (heatLayerRef) {
			map.removeLayer(heatLayerRef);
			heatLayerRef = null;
		}
		const radius = calculateRadiusFromZoom(map.getZoom());
		const heat = (L as any).heatLayer(heatPoints, {
			radius,
			gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
			minOpacity: 0.8,
		});
		heat.addTo(map);
		heatLayerRef = heat;
	};

	addHeatLayer();

	// Update radius when zoom level changes
	map.on("zoomend", () => {
		addHeatLayer();
	});
};
</script>

<template>
	<l-map
		ref="mapRef"
		:center="[48.2087334, 16.3736765]"
		:use-global-leaflet="true"
		:zoom="12"
		style="height: 100vh; width: 100vw"
		@ready="onMapReady"
	>
		<l-tile-layer
			attribution="© OpenStreetMap contributors"
			url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
		/>

		<l-geo-json v-if="geoJson.features.length > 0" :geojson="geoJson" />
	</l-map>
</template>

<style scoped>
/* nothing needed */
</style>
