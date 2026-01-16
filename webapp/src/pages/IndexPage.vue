<template>
	<div class="index-page">
		<leaflet-map ref="leafletMapRef" @map-click="handleMapClick" @estate-select="handleEstateSelect"></leaflet-map>
		<map-overlay-form ref="overlayFormRef" @poi-mode-changed="handlePoiModeChanged" @poi-removed="handlePoiRemoved" />
	</div>
</template>

<script lang="ts" setup>
import { nextTick, ref, watch } from "vue";
import LeafletMap from "components/LeafletMap.vue";
import MapOverlayForm from "components/MapOverlayForm.vue";
import { useGeoStore } from "stores/geoStore";
import type { Point } from "src/types/Point";
import type { RealEstateDto } from "src/types/RealEstate";

const geoStore = useGeoStore();
const leafletMapRef = ref<InstanceType<typeof LeafletMap> | null>(null);
const overlayFormRef = ref<InstanceType<typeof MapOverlayForm> | null>(null);
const isPoiModeActive = ref(false);

// Handle estate selection from popup
const handleEstateSelect = (estate: RealEstateDto) => {
	console.log("Estate selected:", estate);
	geoStore.selectEstate(estate);

	// Show marker and dim heat layer
	if (leafletMapRef.value) {
		const lat = estate.address?.location?.latitude;
		const lon = estate.address?.location?.longitude;
		if (lat != null && lon != null) {
			leafletMapRef.value.showSelectedEstateMarker(lat, lon);
		}
		leafletMapRef.value.setHeatLayerOpacity(0.3);
	}
};

// Watch for selectedEstate changes to handle closing the overview
watch(
	() => geoStore.selectedEstate,
	async (newEstate, oldEstate) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		if (!newEstate && oldEstate) {
			// Estate overview was closed - restore heat layer and remove marker
			leafletMapRef.value.setHeatLayerOpacity(1);
			leafletMapRef.value.clearSelectedEstateMarker();
		} else if (newEstate && oldEstate && newEstate.id !== oldEstate.id) {
			// Different estate selected - update marker position
			const lat = newEstate.address?.location?.latitude;
			const lon = newEstate.address?.location?.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.showSelectedEstateMarker(lat, lon);
			}
		}
	}
);

// Handle map click - add POI if mode is active, or place transport marker
const handleMapClick = async (lat: number, lon: number) => {
	// Check if transport marker mode is active
	if (geoStore.transportMarkerModeActive) {
		await geoStore.setTransportMarker(lat, lon);
		return;
	}

	// Check if POI mode is active
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

		// Always clear existing points first, then draw new ones if any
		leafletMapRef.value.clearPoints?.();

		if (newPoints && newPoints.length > 0) {
			// Zoom out to minZoom and wait for animation to complete before drawing heat points
			// (heat point sizes are calculated based on current zoom level)
			await leafletMapRef.value.setZoomToMin?.();
			console.log("Drawing heat points on map, count:", newPoints.length);
			leafletMapRef.value.drawHeatPoints?.(newPoints);
		} else {
			console.log("No heat points to draw, map cleared");
		}
	},
	{ deep: true }
);

// Watch for stationMarkers changes and draw them on the map
watch(
	() => geoStore.stationMarkers,
	async (newStations) => {
		console.log("stationMarkers changed:", newStations);

		await nextTick();

		if (!leafletMapRef.value) {
			console.warn("leafletMapRef is not ready yet");
			return;
		}

		// Clear existing station markers first
		leafletMapRef.value.clearStationMarkers?.();

		if (newStations && newStations.length > 0) {
			console.log("Drawing station markers on map, count:", newStations.length);
			leafletMapRef.value.addStationMarkers?.(newStations);
		}
	},
	{ deep: true }
);

// Watch for transportMarker changes to draw/clear the marker with radius
watch(
	() => geoStore.transportMarker,
	async (newMarker) => {
		console.log("transportMarker changed:", newMarker);

		await nextTick();

		if (!leafletMapRef.value) {
			console.warn("leafletMapRef is not ready yet");
			return;
		}

		if (newMarker) {
			// Add transport marker with the stored radius
			leafletMapRef.value.addTransportMarker?.(newMarker.lat, newMarker.lon, newMarker.radius);
		} else {
			// Clear transport marker
			leafletMapRef.value.clearTransportMarker?.();
			leafletMapRef.value.clearStationMarkers?.();
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
