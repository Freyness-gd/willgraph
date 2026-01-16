<template>
	<div class="map-overlay-form">
		<!-- Price Range -->
		<div class="form-row">
			<q-icon class="row-icon" name="euro" size="24px" />
			<div class="range-inputs">
				<q-input v-model.number="priceMin" class="overlay-input" dense filled label="Min Price" type="number" />
				<span class="range-separator">-</span>
				<q-input v-model.number="priceMax" class="overlay-input" dense filled label="Max Price" type="number" />
			</div>
		</div>

		<!-- Square Meters Range -->
		<div class="form-row">
			<q-icon class="row-icon" name="straighten" size="24px" />
			<div class="range-inputs">
				<q-input v-model.number="squareMetersMin" class="overlay-input" dense filled label="Min m²" type="number" />
				<span class="range-separator">-</span>
				<q-input v-model.number="squareMetersMax" class="overlay-input" dense filled label="Max m²" type="number" />
			</div>
		</div>

		<!-- Transport Slider -->
		<div class="form-row">
			<q-icon class="row-icon" name="directions_bus" size="24px" />
			<div class="slider-container">
				<span class="slider-label">Transport</span>
				<q-slider v-model="transport" :max="100" :min="0" class="overlay-slider" color="primary" label />
			</div>
		</div>

		<!-- Amenities Slider -->
		<div class="form-row">
			<q-icon class="row-icon" name="local_convenience_store" size="24px" />
			<div class="slider-container">
				<span class="slider-label">Amenities</span>
				<q-slider v-model="amenities" :max="100" :min="0" class="overlay-slider" color="primary" label />
			</div>
		</div>

		<!-- POI Section -->
		<div class="poi-section">
			<div class="poi-header">
				<q-icon class="row-icon" name="place" size="24px" />
				<span class="poi-title">Points of Interest</span>
				<q-btn
					:color="addPoiMode ? 'negative' : 'primary'"
					:icon="addPoiMode ? 'close' : 'add_location'"
					dense
					flat
					round
					size="sm"
					@click="toggleAddPoiMode"
				>
					<q-tooltip>{{ addPoiMode ? "Stop adding POIs" : "Click to add POIs" }}</q-tooltip>
				</q-btn>
			</div>
			<div v-if="addPoiMode" class="poi-hint">Click on the map to add a POI (max 5)</div>
			<div class="poi-list">
				<div v-for="poi in poiList" :key="poi.id" class="poi-item">
					<q-icon :style="{ color: poi.color }" name="place" size="20px" />
					<span class="poi-coords">{{ poi.lat.toFixed(4) }}, {{ poi.lon.toFixed(4) }}</span>
					<q-btn color="negative" dense flat icon="delete" round size="xs" @click="removePoi(poi.id)" />
				</div>
				<div v-if="poiList.length === 0" class="poi-empty">No POIs added yet</div>
			</div>
		</div>

		<!-- Search Button -->
		<div class="form-row button-row">
			<q-btn class="search-btn" color="primary" icon="search" label="Search" @click="handleSearch" />
		</div>
	</div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import searchService from "src/service/searchService";
import type { Point } from "src/types/Point";
import { useGeoStore } from "stores/geoStore";

const geoStore = useGeoStore();

const priceMin = ref<number | null>(null);
const priceMax = ref<number | null>(null);
const squareMetersMin = ref<number | null>(null);
const squareMetersMax = ref<number | null>(null);
const transport = ref(50);
const amenities = ref(50);

// POI Management
const addPoiMode = ref(false);
const poiList = ref<Point[]>([]);
const MAX_POIS = 5;

const POI_COLORS = ["#e91e63", "#9c27b0", "#3f51b5", "#009688", "#ff9800"];

const emit = defineEmits<{
	(e: "poi-mode-changed", active: boolean): void;
	(e: "poi-removed", poi: Point): void;
}>();

const toggleAddPoiMode = () => {
	addPoiMode.value = !addPoiMode.value;
	emit("poi-mode-changed", addPoiMode.value);
};

const addPoi = (lat: number, lon: number): Point | null => {
	if (poiList.value.length >= MAX_POIS) {
		return null;
	}

	const newPoi: Point = {
		id: crypto.randomUUID(),
		lat,
		lon,
		color: POI_COLORS[poiList.value.length % POI_COLORS.length] ?? "#e91e63",
	};

	poiList.value.push(newPoi);
	// Sync with geoStore
	geoStore.setPoiList([...poiList.value]);
	return newPoi;
};

const removePoi = (id: string) => {
	const poi = poiList.value.find((p) => p.id === id);
	if (poi) {
		poiList.value = poiList.value.filter((p) => p.id !== id);
		// Sync with geoStore
		geoStore.setPoiList([...poiList.value]);
		emit("poi-removed", poi);
	}
};

const handleSearch = () => {
	const result = searchService.search({
		priceMin: priceMin.value,
		priceMax: priceMax.value,
		squareMetersMin: squareMetersMin.value,
		squareMetersMax: squareMetersMax.value,
		transport: transport.value,
		amenities: amenities.value,
	});
	console.log("Search result:", result);
};

defineExpose({ addPoi, poiList, addPoiMode });
</script>

<style scoped>
.map-overlay-form {
	position: fixed;
	top: 80px;
	right: 20px;
	z-index: 1000;
	display: flex;
	flex-direction: column;
	gap: 12px;
	padding: 16px;
	background-color: rgba(255, 255, 255, 0.3);
	border-radius: 8px;
	backdrop-filter: blur(4px);
}

.form-row {
	display: flex;
	align-items: center;
	gap: 12px;
}

.row-icon {
	color: #1976d2;
	flex-shrink: 0;
}

.range-inputs {
	display: flex;
	align-items: center;
	gap: 8px;
}

.range-separator {
	font-weight: bold;
	color: #666;
}

.overlay-input {
	background-color: white;
	border-radius: 4px;
	min-width: 100px;
	max-width: 120px;
}

.overlay-input :deep(.q-field__control) {
	background-color: white !important;
}

.slider-container {
	display: flex;
	flex-direction: column;
	flex: 1;
	min-width: 200px;
}

.slider-label {
	font-size: 12px;
	color: #666;
	margin-bottom: 4px;
}

.overlay-slider {
	width: 100%;
}

.button-row {
	justify-content: center;
	margin-top: 8px;
}

.search-btn {
	min-width: 120px;
}

/* POI Section Styles */
.poi-section {
	display: flex;
	flex-direction: column;
	gap: 8px;
	padding: 12px;
	background-color: rgba(255, 255, 255, 0.5);
	border-radius: 6px;
}

.poi-header {
	display: flex;
	align-items: center;
	gap: 8px;
}

.poi-title {
	flex: 1;
	font-weight: 500;
	color: #333;
}

.poi-hint {
	font-size: 12px;
	color: #e91e63;
	font-style: italic;
	padding-left: 32px;
}

.poi-list {
	display: flex;
	flex-direction: column;
	gap: 6px;
	max-height: 150px;
	overflow-y: auto;
}

.poi-item {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 6px 8px;
	background-color: white;
	border-radius: 4px;
}

.poi-coords {
	flex: 1;
	font-size: 12px;
	font-family: monospace;
	color: #666;
}

.poi-empty {
	font-size: 12px;
	color: #999;
	font-style: italic;
	text-align: center;
	padding: 8px;
}
</style>
