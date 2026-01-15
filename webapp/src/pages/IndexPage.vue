<template>
	<div class="index-page">
		<leaflet-map ref="leafletMapRef"></leaflet-map>
	</div>
</template>

<script lang="ts" setup>
import { nextTick, ref, watch } from "vue";
import LeafletMap from "components/LeafletMap.vue";
import { useGeoStore } from "stores/geoStore";

const geoStore = useGeoStore();
const leafletMapRef = ref<any>(null);

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
