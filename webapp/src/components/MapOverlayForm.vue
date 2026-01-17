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
				<q-slider
					v-model="transport"
					:label-value="transport + 'm'"
					:max="1000"
					:min="100"
					:step="100"
					class="overlay-slider"
					color="primary"
					label
				/>
			</div>
		</div>

		<!-- Amenities Section -->
		<div class="amenities-section">
			<div class="amenities-header">
				<q-icon class="row-icon" name="local_convenience_store" size="24px" />
				<span class="amenities-title">Amenities</span>
				<q-btn v-if="selectedAmenities.length < 3" color="primary" dense flat icon="add" round size="sm">
					<q-tooltip>Add amenity category ({{ selectedAmenities.length }}/3)</q-tooltip>
					<q-menu anchor="bottom right" self="top right">
						<q-list dense style="min-width: 200px; max-height: 300px; overflow-y: auto">
							<q-item
								v-for="cat in availableCategories"
								:key="cat.value"
								v-close-popup
								clickable
								@click="addAmenityCategory(cat.value)"
							>
								<q-item-section avatar>
									<q-icon :name="cat.icon" size="20px" />
								</q-item-section>
								<q-item-section>{{ cat.label }}</q-item-section>
							</q-item>
						</q-list>
					</q-menu>
				</q-btn>
			</div>

			<!-- Selected Amenity Sliders -->
			<div v-if="selectedAmenities.length === 0" class="amenities-empty">Click + to add amenity categories (max 3)</div>
			<div v-for="(amenity, index) in selectedAmenities" :key="amenity.category" class="amenity-slider-row">
				<div class="reorder-buttons">
					<q-btn
						:disable="index === 0"
						color="grey-7"
						dense
						flat
						icon="keyboard_arrow_up"
						round
						size="xs"
						@click="moveAmenityUp(index)"
					>
						<q-tooltip>Move up</q-tooltip>
					</q-btn>
					<q-btn
						:disable="index === selectedAmenities.length - 1"
						color="grey-7"
						dense
						flat
						icon="keyboard_arrow_down"
						round
						size="xs"
						@click="moveAmenityDown(index)"
					>
						<q-tooltip>Move down</q-tooltip>
					</q-btn>
				</div>
				<q-icon :name="getCategoryIcon(amenity.category)" class="amenity-icon" size="20px" />
				<div class="amenity-slider-container">
					<span class="amenity-label">{{ getCategoryLabel(amenity.category) }}</span>
					<q-slider
						v-model="amenity.radius"
						:label-value="amenity.radius + 'm'"
						:max="1000"
						:min="100"
						:step="100"
						class="amenity-slider"
						color="light-blue-6"
						label
					/>
				</div>
				<q-btn color="negative" dense flat icon="close" round size="xs" @click="removeAmenityCategory(index)">
					<q-tooltip>Remove</q-tooltip>
				</q-btn>
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
				<div v-for="(poi, index) in poiList" :key="poi.id" class="poi-item">
					<div class="reorder-buttons">
						<q-btn
							:disable="index === 0"
							color="grey-7"
							dense
							flat
							icon="keyboard_arrow_up"
							round
							size="xs"
							@click="movePoiUp(index)"
						>
							<q-tooltip>Move up</q-tooltip>
						</q-btn>
						<q-btn
							:disable="index === poiList.length - 1"
							color="grey-7"
							dense
							flat
							icon="keyboard_arrow_down"
							round
							size="xs"
							@click="movePoiDown(index)"
						>
							<q-tooltip>Move down</q-tooltip>
						</q-btn>
					</div>
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
import { computed, ref } from "vue";
import regionService from "src/service/regionService";
import type { Point } from "src/types/Point";
import type { ListingSearchFilterDto } from "src/types/dto";
import { useGeoStore } from "stores/geoStore";

const geoStore = useGeoStore();

const priceMin = ref<number | null>(null);
const priceMax = ref<number | null>(null);
const squareMetersMin = ref<number | null>(null);
const squareMetersMax = ref<number | null>(null);
const transport = ref(500);

// Amenity categories with icons
const AMENITY_CATEGORIES = [
	{ value: "pub", label: "Pub", icon: "sports_bar" },
	{ value: "bar", label: "Bar", icon: "local_bar" },
	{ value: "cafe", label: "Café", icon: "local_cafe" },
	{ value: "restaurant", label: "Restaurant", icon: "restaurant" },
	{ value: "fast_food", label: "Fast Food", icon: "fastfood" },
	{ value: "gym", label: "Gym", icon: "fitness_center" },
	{ value: "fitness_center", label: "Fitness Center", icon: "fitness_center" },
	{ value: "swimming_pool", label: "Swimming Pool", icon: "pool" },
	{ value: "library", label: "Library", icon: "local_library" },
	{ value: "university", label: "University", icon: "school" },
	{ value: "cinema", label: "Cinema", icon: "movie" },
	{ value: "theatre", label: "Theatre", icon: "theater_comedy" },
	{ value: "nightclub", label: "Nightclub", icon: "nightlife" },
	{ value: "pharmacy", label: "Pharmacy", icon: "local_pharmacy" },
	{ value: "doctors", label: "Doctors", icon: "medical_services" },
	{ value: "dentist", label: "Dentist", icon: "medical_services" },
	{ value: "supermarket", label: "Supermarket", icon: "local_grocery_store" },
	{ value: "bakery", label: "Bakery", icon: "bakery_dining" },
	{ value: "butcher", label: "Butcher", icon: "lunch_dining" },
	{ value: "laundry", label: "Laundry", icon: "local_laundry_service" },
	{ value: "dry_cleaning", label: "Dry Cleaning", icon: "dry_cleaning" },
	{ value: "bicycle_rental", label: "Bicycle Rental", icon: "pedal_bike" },
	{ value: "car_rental", label: "Car Rental", icon: "car_rental" },
	{ value: "parking", label: "Parking", icon: "local_parking" },
	{ value: "fuel", label: "Fuel Station", icon: "local_gas_station" },
] as const;

type AmenityCategory = (typeof AMENITY_CATEGORIES)[number]["value"];

interface SelectedAmenity {
	category: AmenityCategory;
	radius: number;
}

// Selected amenities (max 3)
const selectedAmenities = ref<SelectedAmenity[]>([]);

// Get available categories (not already selected)
const availableCategories = computed(() => {
	const selectedCats = new Set(selectedAmenities.value.map((a) => a.category));
	return AMENITY_CATEGORIES.filter((cat) => !selectedCats.has(cat.value));
});

// Add a new amenity category
const addAmenityCategory = (category: AmenityCategory) => {
	if (selectedAmenities.value.length >= 3) return;
	selectedAmenities.value.push({ category, radius: 500 });
};

// Remove an amenity category
const removeAmenityCategory = (index: number) => {
	selectedAmenities.value.splice(index, 1);
};

// Move amenity up in the list
const moveAmenityUp = (index: number) => {
	if (index <= 0) return;
	const temp = selectedAmenities.value[index];
	selectedAmenities.value[index] = selectedAmenities.value[index - 1];
	selectedAmenities.value[index - 1] = temp;
};

// Move amenity down in the list
const moveAmenityDown = (index: number) => {
	if (index >= selectedAmenities.value.length - 1) return;
	const temp = selectedAmenities.value[index];
	selectedAmenities.value[index] = selectedAmenities.value[index + 1];
	selectedAmenities.value[index + 1] = temp;
};

// Get icon for a category
const getCategoryIcon = (category: AmenityCategory): string => {
	return AMENITY_CATEGORIES.find((c) => c.value === category)?.icon || "place";
};

// Get label for a category
const getCategoryLabel = (category: AmenityCategory): string => {
	return AMENITY_CATEGORIES.find((c) => c.value === category)?.label || category;
};

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
	// Sync with geoStore and trigger delta distance calculation
	geoStore.setPoiList([...poiList.value]);
	geoStore.syncPoiDistances([...poiList.value]);
	return newPoi;
};

const removePoi = (id: string) => {
	const poi = poiList.value.find((p) => p.id === id);
	if (poi) {
		poiList.value = poiList.value.filter((p) => p.id !== id);
		// Sync with geoStore and update distances
		geoStore.setPoiList([...poiList.value]);
		geoStore.syncPoiDistances([...poiList.value]);
		emit("poi-removed", poi);
	}
};

// Move POI up in the list
const movePoiUp = (index: number) => {
	if (index <= 0) return;
	const temp = poiList.value[index];
	poiList.value[index] = poiList.value[index - 1];
	poiList.value[index - 1] = temp;
};

// Move POI down in the list
const movePoiDown = (index: number) => {
	if (index >= poiList.value.length - 1) return;
	const temp = poiList.value[index];
	poiList.value[index] = poiList.value[index + 1];
	poiList.value[index + 1] = temp;
};

const handleSearch = async () => {
	// Build base listing criteria (region will be filled per-region below)
	const baseListing = {
		minArea: squareMetersMin.value ?? null,
		maxPrice: priceMax.value ?? null,
		minPrice: priceMin.value ?? null,
		region: null,
	};

	// Transport criteria
	const transportCriteria = {
		maxDistanceToStation: transport.value ?? null,
	};

	// Amenity priorities (map selectedAmenities to PriorityItemDto)
	// bonusScoreFactor: top item = list length, bottom item = 1
	const amenityCount = selectedAmenities.value.length;
	const amenityPriorities = selectedAmenities.value.slice(0, 3).map((a, index) => ({
		categoryValue: a.category,
		maxDistanceToAmenity: a.radius ?? null,
		bonusScoreFactor: amenityCount - index, // top = max, bottom = 1
		lat: null,
		lng: null,
	}));

	// POI priorities (map poiList to PriorityItemDto with custom_poi category)
	// bonusScoreFactor: top item = list length, bottom item = 1
	const poiCount = poiList.value.length;
	const poiPriorities = poiList.value.map((poi, index) => ({
		categoryValue: "custom_poi",
		maxDistanceToAmenity: 500, // default radius for POI
		bonusScoreFactor: poiCount - index, // top = max, bottom = 1
		lat: poi.lat,
		lng: poi.lon,
	}));

	// Build base filter (listing.region will be set per-region)
	const baseFilter: ListingSearchFilterDto = {
		listing: baseListing,
		transport: transportCriteria,
		amenityPriorities: amenityPriorities.length > 0 ? amenityPriorities : null,
		poiPriorities: poiPriorities.length > 0 ? poiPriorities : null,
	};

	console.log("Starting search across regions with base filter:", baseFilter);

	// Clear existing estates in map before search
	geoStore.clearAllRegionEstates();

	// Determine regions to search: use only selectedMunicipalities; if none selected, abort
	const regions: string[] =
		geoStore.selectedMunicipalities && geoStore.selectedMunicipalities.length > 0
			? geoStore.selectedMunicipalities
			: [];

	if (regions.length === 0) {
		console.log("No selected regions to search; cleared heat points.");
		return;
	}

	try {
		// Fire requests in parallel, one per region
		const promises = regions.map(async (regionName) => {
			console.log("Region Name:", regionName);
			const filterForRegion: ListingSearchFilterDto = {
				...baseFilter,
				listing: {
					...baseFilter.listing!,
					region: regionName,
				},
			};
			const estates = await regionService.searchEstatesWithFilters(filterForRegion);
			return { regionName, estates };
		});

		const resultsPerRegion = await Promise.all(promises);

		console.log("Results per region", resultsPerRegion);

		// Store results in the geoStore using setRegionEstates for reactivity
		for (const { regionName, estates } of resultsPerRegion) {
			console.log(`Storing ${estates.length} estates for region:`, regionName);
			geoStore.setRegionEstates(regionName, estates);
		}
	} catch (err) {
		console.error("Error during region search:", err);
	}
};

defineExpose({ addPoi, poiList, addPoiMode, selectedAmenities });
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

/* Amenities Section Styles */
.amenities-section {
	display: flex;
	flex-direction: column;
	gap: 8px;
	padding: 12px;
	background-color: rgba(255, 255, 255, 0.5);
	border-radius: 6px;
}

.amenities-header {
	display: flex;
	align-items: center;
	gap: 8px;
}

.amenities-title {
	flex: 1;
	font-weight: 500;
	color: #333;
}

.amenities-empty {
	font-size: 12px;
	color: #999;
	font-style: italic;
	text-align: center;
	padding: 8px;
}

.amenity-slider-row {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px;
	background-color: white;
	border-radius: 4px;
}

.amenity-icon {
	color: #03a9f4;
	flex-shrink: 0;
}

.amenity-slider-container {
	display: flex;
	flex-direction: column;
	flex: 1;
	min-width: 0;
}

.amenity-label {
	font-size: 11px;
	color: #666;
	margin-bottom: 2px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.amenity-slider {
	width: 100%;
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

/* Reorder buttons for drag-like reordering */
.reorder-buttons {
	display: flex;
	flex-direction: column;
	gap: 0;
	flex-shrink: 0;
}

.reorder-buttons .q-btn {
	padding: 0;
	min-height: 16px;
	min-width: 20px;
}
</style>
