<template>
	<div class="index-page">
		<leaflet-map ref="leafletMapRef" @map-click="handleMapClick"></leaflet-map>
		<map-overlay-form ref="overlayFormRef" @poi-mode-changed="handlePoiModeChanged" @poi-removed="handlePoiRemoved" />
	</div>
</template>

<script lang="ts" setup>
import { nextTick, ref, watch } from "vue";
import LeafletMap from "components/LeafletMap.vue";
import MapOverlayForm from "components/MapOverlayForm.vue";
import { useGeoStore } from "stores/geoStore";
import type { Point } from "src/types/Point";

const geoStore = useGeoStore();
const leafletMapRef = ref<InstanceType<typeof LeafletMap> | null>(null);
const overlayFormRef = ref<InstanceType<typeof MapOverlayForm> | null>(null);
const isPoiModeActive = ref(false);

// Handle map click - add POI if mode is active
const handleMapClick = (lat: number, lon: number) => {
	if (!isPoiModeActive.value || !overlayFormRef.value) {
		return;
	}

	const newPoi = overlayFormRef.value.addPoi(lat, lon);
	if (newPoi && leafletMapRef.value) {
		leafletMapRef.value.addPoiMarker(newPoi);
	}
};

// Handle POI mode toggle
const handlePoiModeChanged = (active: boolean) => {
	isPoiModeActive.value = active;
};

// Handle POI removal - remove marker from map
const handlePoiRemoved = (poi: Point) => {
	if (leafletMapRef.value) {
		leafletMapRef.value.removePoiMarker(poi.id);
	}
};

// Watch for regionHeatPoints changes and draw them on the map
watch(
	() => geoStore.regionHeatPoints,
	async (newPoints) => {
		console.log("regionHeatPoints changed:", newPoints);

		await nextTick();

		if (!leafletMapRef.value) {
			console.warn("leafletMapRef is not ready yet");
			return;
		}

		if (newPoints && newPoints.length > 0) {
			console.log("Drawing heat points on map, count:", newPoints.length);
			leafletMapRef.value.drawHeatPoints?.(newPoints);
		}
	},
	{ deep: true }
);
</script>

<style scoped>
.index-page {
	width: 100%;
	height: 100%;
}
</style>
