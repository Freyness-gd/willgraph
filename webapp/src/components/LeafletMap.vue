<script lang="ts" setup>
import { computed, ref } from "vue";
import { LControl, LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import type { Map as LeafletMap } from "leaflet";
// eslint-disable-next-line no-duplicate-imports
import L from "leaflet";
import { useGeoStore } from "stores/geoStore";
import { municipalitiesToGeoJson } from "src/mapper/MunicipalityMapper";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
let heatLayerRef: any = null;
const zoomRef = ref<number>(12);

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
	// 1 km at the equator is approximately 40075 km / 2^(zoom + 8) pixels
	// We want the radius to represent 1 km in pixels
	// At zoom level z, 1 km ≈ Math.pow(2, zoom + 8) / 40075 pixels
	// But leaflet's heatLayer radius is in pixels on the map
	// Using the formula: pixels = Math.pow(2, zoom + 8) / 40075
	// For 1 km, we get: radius ≈ 40075 / Math.pow(2, zoom + 8) in world units
	// Converting to pixels for heatLayer
	const metersPerPixel = 40075016.686 / 2 ** (zoom + 8);
	const radiusInPixels = Math.round(200 / metersPerPixel); // 1000 meters = 1 km
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

	addHeatLayer();

	// Update radius when zoom level changes
	map.on("zoomend", () => {
		zoomRef.value = map.getZoom();
		console.log("Zoom value:", zoomRef.value);
		addHeatLayer();
	});
};
</script>

<template>
	<l-map
		ref="mapRef"
		:center="[48.2087334, 16.3736765]"
		:maxZoom="16"
		:minZoom="8"
		:use-global-leaflet="true"
		:zoom="zoomRef"
		style="height: 100vh; width: 100vw"
		@ready="onMapReady"
	>
		<l-tile-layer
			attribution="© OpenStreetMap contributors"
			url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
		/>

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
