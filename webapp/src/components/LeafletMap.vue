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

const geoJson = computed(() => municipalitiesToGeoJson(geoStore.getSelectedMunicipalities));

const heatPoints: [number, number, number][] = [
	[48.2085, 16.373, 1], // 1st district – Innere Stadt
	[48.22, 16.392, 0.8], // 2nd district – Leopoldstadt
	[48.177, 16.3, 0.6], // 12th district – Meidling
	[48.138, 16.284, 0.4], // 23rd district – Liesing
];

const onMapReady = (map: LeafletMap) => {
	console.log("L keys:", Object.keys(L));
	console.log("heatLayer in L:", (L as any).heatLayer);
	mapRef.value = map;

	const heat = (L as any).heatLayer(heatPoints, {
		radius: 100,
		gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
		minOpacity: 0.8,
	});

	heat.addTo(map);
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
