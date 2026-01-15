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

		<!-- Search Button -->
		<div class="form-row button-row">
			<q-btn class="search-btn" color="primary" icon="search" label="Search" @click="handleSearch" />
		</div>
	</div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import searchService from "src/service/searchService";

const priceMin = ref<number | null>(null);
const priceMax = ref<number | null>(null);
const squareMetersMin = ref<number | null>(null);
const squareMetersMax = ref<number | null>(null);
const transport = ref(50);
const amenities = ref(50);

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
</style>
