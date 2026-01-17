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
const leafletMapRef = ref<any>(null);
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

// Watch for selectedMunicipalities changes to pan to newly added municipality
watch(
	() => [...geoStore.selectedMunicipalities],
	async (newList, oldList) => {
		const oldSet = new Set(oldList || []);
		const newlyAdded = newList.find((name) => !oldSet.has(name));

		if (newlyAdded) {
			await nextTick();
			if (!leafletMapRef.value) return;

			// Find the municipality to get its boundary for centroid calculation
			const municipality = geoStore.municipalities.find((m) => m.name === newlyAdded);
			if (municipality) {
				const rings = municipality.boundary.coordinates;
				const outerRing = rings?.[0]?.[0] ?? [];
				if (outerRing.length > 0) {
					// Calculate centroid (coordinates are [lng, lat])
					let sumLat = 0;
					let sumLng = 0;
					for (const pos of outerRing) {
						sumLng += pos[0];
						sumLat += pos[1];
					}
					const centerLat = sumLat / outerRing.length;
					const centerLng = sumLng / outerRing.length;
					console.log(`Panning to municipality ${newlyAdded} at [${centerLat}, ${centerLng}]`);
					leafletMapRef.value.panTo?.(centerLat, centerLng, 13);
				}
			}
		}
	}
);

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
			// Clear existing points first
			leafletMapRef.value.clearPoints?.();

			// Pan to Vienna center and zoom to 12, then wait for animation to complete
			const viennaCenterLat = 48.2087334;
			const viennaCenterLng = 16.3736765;
			leafletMapRef.value.panTo?.(viennaCenterLat, viennaCenterLng, 14);

			// Wait for pan/zoom animation to complete (500ms duration + buffer)
			await new Promise((resolve) => setTimeout(resolve, 600));

			console.log("Drawing heat points on map, count:", newPoints.length);
			leafletMapRef.value.drawHeatPoints?.(newPoints);
		} else {
			console.log("No heat points to draw");
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

// Watch for estate transport panel state to show/hide circle
watch(
	() => geoStore.showEstateTransport,
	async (show) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		if (show && geoStore.selectedEstate?.address?.location) {
			const lat = geoStore.selectedEstate.address.location.latitude;
			const lon = geoStore.selectedEstate.address.location.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.showEstateTransportCircle?.(lat, lon, geoStore.estateTransportRadius);
			}
		} else {
			leafletMapRef.value.clearEstateTransport?.();
		}
	}
);

// Watch for estate transport radius changes to update circle
watch(
	() => geoStore.estateTransportRadius,
	async (newRadius) => {
		await nextTick();

		if (!leafletMapRef.value || !geoStore.showEstateTransport) return;

		if (geoStore.selectedEstate?.address?.location) {
			const lat = geoStore.selectedEstate.address.location.latitude;
			const lon = geoStore.selectedEstate.address.location.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.updateEstateTransportCircleRadius?.(lat, lon, newRadius);
			}
		}
	}
);

// Watch for estate transport stations to draw markers
watch(
	() => geoStore.estateTransportStations,
	async (stations) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		leafletMapRef.value.clearEstateTransportStations?.();

		if (stations && stations.length > 0 && geoStore.showEstateTransport) {
			leafletMapRef.value.showEstateTransportStations?.(stations);
		}
	},
	{ deep: true }
);

// Watch for estate amenities radius changes
watch(
	() => geoStore.estateAmenitiesRadius,
	async (newRadius) => {
		await nextTick();

		if (!leafletMapRef.value || !geoStore.showEstateAmenities) return;

		if (geoStore.selectedEstate?.address?.location) {
			const lat = geoStore.selectedEstate.address.location.latitude;
			const lon = geoStore.selectedEstate.address.location.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.updateEstateAmenitiesCircleRadius?.(newRadius);
			}
		}
	}
);

// Watch for showEstateAmenities toggle
watch(
	() => geoStore.showEstateAmenities,
	async (show) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		if (show && geoStore.selectedEstate?.address?.location) {
			const lat = geoStore.selectedEstate.address.location.latitude;
			const lon = geoStore.selectedEstate.address.location.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.showEstateAmenitiesCircle?.(lat, lon, geoStore.estateAmenitiesRadius);
			}
		} else {
			leafletMapRef.value.clearAllEstateAmenities?.();
		}
	}
);

// Watch for estate amenities to draw markers
watch(
	() => geoStore.estateAmenities,
	async (amenities) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		leafletMapRef.value.clearEstateAmenities?.();

		if (amenities && amenities.length > 0 && geoStore.showEstateAmenities) {
			leafletMapRef.value.showEstateAmenities?.(amenities);
		}
	},
	{ deep: true }
);

// Clear estate transport and amenities when estate is deselected
watch(
	() => geoStore.selectedEstate,
	async (newEstate, oldEstate) => {
		await nextTick();

		if (!leafletMapRef.value) return;

		if (!newEstate && oldEstate) {
			// Estate overview was closed - restore heat layer, remove marker, and clear transport/amenities
			leafletMapRef.value.setHeatLayerOpacity(1);
			leafletMapRef.value.clearSelectedEstateMarker();
			leafletMapRef.value.clearEstateTransport?.();
			leafletMapRef.value.clearAllEstateAmenities?.();
		} else if (newEstate && oldEstate && newEstate.id !== oldEstate.id) {
			// Different estate selected - update marker position and clear transport/amenities
			leafletMapRef.value.clearEstateTransport?.();
			leafletMapRef.value.clearAllEstateAmenities?.();
			const lat = newEstate.address?.location?.latitude;
			const lon = newEstate.address?.location?.longitude;
			if (lat != null && lon != null) {
				leafletMapRef.value.showSelectedEstateMarker(lat, lon);
			}
		}
	}
);
</script>

<style scoped>
.index-page {
	width: 100%;
	height: 100%;
}
</style>
