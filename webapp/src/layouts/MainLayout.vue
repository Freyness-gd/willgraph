<template>
	<q-layout view="lHh Lpr lFf">
		<q-header elevated>
			<q-toolbar>
				<q-toolbar-title> WillGraph </q-toolbar-title>
				<div class="search-wrapper">
					<q-input
						ref="searchInput"
						v-model="search"
						class="p-2"
						debounce="500"
						placeholder="Search (min 3 letters)"
						rounded
						standout
						type="search"
					>
						<template v-slot:append>
							<q-icon v-if="searchIconVisibility" name="search" />
						</template>

						<q-menu v-model="searchMenuOpen" anchor="bottom left" fit no-focus no-refocus self="top left">
							<q-list dense style="min-width: 250px">
								<q-item v-if="searchResults.length === 0" disable>
									<q-item-section> No results found </q-item-section>
								</q-item>
								<q-item v-for="name in searchResults" :key="name" clickable @click="addMunicipality(name)">
									<q-item-section>
										{{ name }}
									</q-item-section>
								</q-item>
							</q-list>
						</q-menu>
					</q-input>
				</div>
			</q-toolbar>
		</q-header>

		<!-- Estate Overview Card -->
		<div v-if="geoStore.selectedEstate" class="estate-overview-card">
			<div class="estate-header">
				<q-icon class="estate-icon" color="primary" name="home" size="24px" />
				<span class="estate-title">{{ geoStore.selectedEstate.title || "Untitled Property" }}</span>
				<q-btn color="negative" dense flat icon="close" round size="sm" @click="closeEstateOverview">
					<q-tooltip>Close</q-tooltip>
				</q-btn>
			</div>
			<div class="estate-content">
				<!-- Price Info -->
				<div v-if="geoStore.selectedEstate.price != null" class="estate-row">
					<q-icon name="euro" size="16px" />
					<span class="estate-label">Price:</span>
					<span class="estate-value price">{{ formatPrice(geoStore.selectedEstate.price) }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.pricePerM2 != null" class="estate-row">
					<q-icon name="square_foot" size="16px" />
					<span class="estate-label">Price/m²:</span>
					<span class="estate-value">{{ formatPrice(geoStore.selectedEstate.pricePerM2) }}</span>
				</div>

				<!-- Area Info -->
				<div v-if="geoStore.selectedEstate.livingArea != null" class="estate-row">
					<q-icon name="straighten" size="16px" />
					<span class="estate-label">Living Area:</span>
					<span class="estate-value">{{ formatArea(geoStore.selectedEstate.livingArea) }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.totalArea != null" class="estate-row">
					<q-icon name="crop_free" size="16px" />
					<span class="estate-label">Total Area:</span>
					<span class="estate-value">{{ formatArea(geoStore.selectedEstate.totalArea) }}</span>
				</div>

				<!-- Room Info -->
				<div v-if="geoStore.selectedEstate.roomCount != null" class="estate-row">
					<q-icon name="meeting_room" size="16px" />
					<span class="estate-label">Rooms:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.roomCount }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.bedroomCount != null" class="estate-row">
					<q-icon name="bed" size="16px" />
					<span class="estate-label">Bedrooms:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.bedroomCount }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.bathroomCount != null" class="estate-row">
					<q-icon name="bathtub" size="16px" />
					<span class="estate-label">Bathrooms:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.bathroomCount }}</span>
				</div>

				<!-- Address Info -->
				<div v-if="hasAddressInfo" class="estate-divider"></div>
				<div v-if="geoStore.selectedEstate.address?.fullAddressString" class="estate-row">
					<q-icon name="location_on" size="16px" />
					<span class="estate-label">Address:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.address.fullAddressString }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.address?.street" class="estate-row">
					<q-icon name="signpost" size="16px" />
					<span class="estate-label">Street:</span>
					<span class="estate-value"
						>{{ geoStore.selectedEstate.address.street }} {{ geoStore.selectedEstate.address?.houseNumber || "" }}</span
					>
				</div>
				<div v-if="geoStore.selectedEstate.address?.city" class="estate-row">
					<q-icon name="location_city" size="16px" />
					<span class="estate-label">City:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.address.city }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.address?.postalCode" class="estate-row">
					<q-icon name="markunread_mailbox" size="16px" />
					<span class="estate-label">Postal Code:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.address.postalCode }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.address?.countryCode" class="estate-row">
					<q-icon name="flag" size="16px" />
					<span class="estate-label">Country:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.address.countryCode }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.address?.distanceToNearestStation != null" class="estate-row">
					<q-icon name="directions_transit" size="16px" />
					<span class="estate-label">Nearest Station:</span>
					<span class="estate-value">{{
						formatDistance(geoStore.selectedEstate.address.distanceToNearestStation)
					}}</span>
				</div>

				<!-- Source Info -->
				<div
					v-if="geoStore.selectedEstate.source || geoStore.selectedEstate.timestampFound"
					class="estate-divider"
				></div>
				<div v-if="geoStore.selectedEstate.source" class="estate-row">
					<q-icon name="source" size="16px" />
					<span class="estate-label">Source:</span>
					<span class="estate-value">{{ geoStore.selectedEstate.source }}</span>
				</div>
				<div v-if="geoStore.selectedEstate.timestampFound" class="estate-row">
					<q-icon name="schedule" size="16px" />
					<span class="estate-label">Found:</span>
					<span class="estate-value">{{ formatDate(geoStore.selectedEstate.timestampFound) }}</span>
				</div>

				<!-- External Link -->
				<div v-if="geoStore.selectedEstate.externalUrl" class="estate-row estate-link">
					<q-icon name="link" size="16px" />
					<a :href="geoStore.selectedEstate.externalUrl" target="_blank">View Original Listing ↗</a>
				</div>

				<!-- Tools Section -->
				<div class="estate-tools">
					<div class="tools-header">Tools</div>
					<div class="tools-buttons">
						<q-btn color="primary" dense icon="directions_bus" outline @click="onTransportTool">
							<q-tooltip>Transport in the area</q-tooltip>
						</q-btn>
						<q-btn color="primary" dense icon="storefront" outline @click="onAmenitiesTool">
							<q-tooltip>Amenities in the area</q-tooltip>
						</q-btn>
						<q-btn
							:color="geoStore.showPoiDistances ? 'secondary' : 'primary'"
							:loading="geoStore.poiDistancesLoading"
							dense
							icon="place"
							outline
							@click="onPoiDistanceTool"
						>
							<q-tooltip>POI distance</q-tooltip>
						</q-btn>
					</div>
				</div>

				<!-- POI Distances Panel -->
				<div v-if="geoStore.showPoiDistances" class="poi-distances-panel">
					<div class="poi-distances-header">
						<q-icon color="primary" name="place" size="18px" />
						<span>POI Distances</span>
					</div>
					<div v-if="geoStore.poiDistancesLoading" class="poi-distances-loading">
						<q-spinner color="primary" size="20px" />
						<span>Calculating...</span>
					</div>
					<div v-else-if="geoStore.poiDistances.length === 0" class="poi-distances-empty">
						<span>No POIs added. Add POIs using the form on the right.</span>
					</div>
					<div v-else class="poi-distances-list">
						<div v-for="poi in geoStore.poiDistances" :key="poi.id" class="poi-distance-item">
							<div :style="{ backgroundColor: poi.color }" class="poi-color-dot"></div>
							<div class="poi-distance-info">
								<div class="poi-distance-coords">{{ poi.lat.toFixed(4) }}, {{ poi.lon.toFixed(4) }}</div>
								<div v-if="poi.distance" class="poi-distance-values">
									<span class="distance-meters">{{ formatDistanceMeters(poi.distance.distanceInMeters) }}</span>
									<span class="distance-walking"
										>🚶 {{ formatWalkingTime(poi.distance.walkingDurationInMinutes) }}</span
									>
								</div>
								<div v-else class="poi-distance-error">Unable to calculate</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- Municipalities List Panel - Hidden when estate is selected -->
		<div v-if="!geoStore.selectedEstate" class="municipalities-panel">
			<div class="panel-header">
				<q-icon class="panel-icon" name="location_city" size="24px" />
				<span class="panel-title">Municipalities</span>
				<span class="panel-count">({{ selectedMunicipalities.length }}/5)</span>
			</div>
			<div class="municipalities-list">
				<div v-if="selectedMunicipalities.length === 0" class="empty-message">No municipalities selected</div>
				<div
					v-for="(name, index) in selectedMunicipalities"
					:key="name"
					class="municipality-item"
					draggable="true"
					@dragend="onDragEnd"
					@dragstart="onDragStart($event, index)"
					@drop="onDrop($event, index)"
					@dragover.prevent
				>
					<div class="item-order">{{ index + 1 }}</div>
					<div class="item-name">{{ name }}</div>
					<div class="item-actions">
						<q-btn
							:disable="index === 0"
							color="primary"
							dense
							flat
							icon="arrow_upward"
							round
							size="xs"
							@click="moveMunicipalityUp(name)"
						>
							<q-tooltip>Move up</q-tooltip>
						</q-btn>
						<q-btn
							:disable="index === selectedMunicipalities.length - 1"
							color="primary"
							dense
							flat
							icon="arrow_downward"
							round
							size="xs"
							@click="moveMunicipalityDown(name)"
						>
							<q-tooltip>Move down</q-tooltip>
						</q-btn>
						<q-btn color="negative" dense flat icon="delete" round size="xs" @click="removeMunicipality(name)">
							<q-tooltip>Remove</q-tooltip>
						</q-btn>
					</div>
				</div>
			</div>

			<!-- Transport Button -->
			<div class="transport-section">
				<q-btn
					:color="transportButtonColor"
					:label="transportButtonLabel"
					:loading="geoStore.loadingStations"
					dense
					icon="directions_bus"
					@click="toggleTransportMarker"
				>
					<q-tooltip>{{ transportButtonTooltip }}</q-tooltip>
				</q-btn>

				<!-- Radius input - shown only when in placement mode (before marker is placed) -->
				<div v-if="geoStore.transportMarkerModeActive && !geoStore.transportMarker" class="radius-input">
					<q-input
						v-model.number="transportRadius"
						:rules="[(val) => (val >= 1 && val <= 1000) || 'Max 1000m']"
						dense
						label="Radius (m)"
						outlined
						style="width: 180px"
						type="number"
					>
						<template v-slot:prepend>
							<q-icon name="radar" size="xs" />
						</template>
					</q-input>
				</div>
			</div>
		</div>

		<q-page-container>
			<router-view />
		</q-page-container>
	</q-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import { useGeoStore } from "stores/geoStore";
import { QInput, useQuasar } from "quasar";

const $q = useQuasar();

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchMenuOpen = ref(false);
const searchInput = ref<QInput | null>(null);
const draggedIndex = ref<number | null>(null);

// Computed for transport radius (bound to store)
const transportRadius = computed({
	get: () => geoStore.pendingTransportRadius,
	set: (val: number) => geoStore.setPendingTransportRadius(val),
});

// Computed - selectedMunicipalities from store
const selectedMunicipalities = computed(() => geoStore.selectedMunicipalities);

const clearSearch = () => {
	search.value = "";
	searchMenuOpen.value = false;
};

// Computed State
const searchIconVisibility = computed(() => {
	return !search.value || search.value.length === 0;
});

const hasAddressInfo = computed(() => {
	const address = geoStore.selectedEstate?.address;
	if (!address) return false;
	return (
		address.fullAddressString ||
		address.street ||
		address.city ||
		address.postalCode ||
		address.countryCode ||
		address.distanceToNearestStation != null
	);
});

const searchResults = computed(() => {
	if (!search.value || search.value.length < 3) {
		return [];
	}

	const query = search.value.toLowerCase();

	console.log("Searching for: ", query);

	const results = geoStore.municipalitiesNames.filter((name) => name.toLowerCase().includes(query));

	return results.slice(0, 10);
});

// Watcher
watch(search, (value) => {
	searchMenuOpen.value = value.length >= 3 && searchResults.value.length > 0;
});

// Methods
const addMunicipality = async (name: string) => {
	if (geoStore.isMunicipalitySaved(name)) {
		geoStore.removeSelectedMunicipality(name);
		clearSearch();
		return;
	}

	if (!geoStore.canAddMoreMunicipalities) {
		$q.notify({
			type: "warning",
			message: "Maximum of 5 municipalities reached",
			position: "top",
		});
		clearSearch();
		return;
	}

	// Use the new action that fetches region points
	await geoStore.addRegionAndFetchPoints(name);
	clearSearch();
};

const removeMunicipality = (name: string) => {
	geoStore.removeSelectedMunicipality(name);
};

const moveMunicipalityUp = (name: string) => {
	geoStore.moveMunicipalityUp(name);
};

const moveMunicipalityDown = (name: string) => {
	geoStore.moveMunicipalityDown(name);
};

// Drag and Drop handlers
const onDragStart = (event: DragEvent, index: number) => {
	draggedIndex.value = index;
	if (event.dataTransfer) {
		// eslint-disable-next-line no-param-reassign
		event.dataTransfer.effectAllowed = "move";
		event.dataTransfer.setData("text/plain", index.toString());
	}
};

const onDrop = (event: DragEvent, dropIndex: number) => {
	event.preventDefault();
	if (draggedIndex.value === null || draggedIndex.value === dropIndex) {
		return;
	}

	const newOrder = [...selectedMunicipalities.value];
	const draggedItem = newOrder[draggedIndex.value];
	if (draggedItem !== undefined) {
		newOrder.splice(draggedIndex.value, 1);
		newOrder.splice(dropIndex, 0, draggedItem);
		geoStore.reorderMunicipalities(newOrder);
	}
};

const onDragEnd = () => {
	draggedIndex.value = null;
};

// Transport marker computed properties
const transportButtonColor = computed(() => {
	if (geoStore.transportMarker) return "negative"; // Red when marker is placed (click to remove)
	if (geoStore.transportMarkerModeActive) return "warning"; // Orange when in placement mode
	return "primary"; // Blue default
});

const transportButtonLabel = computed(() => {
	if (geoStore.transportMarker) return "Remove Marker";
	if (geoStore.transportMarkerModeActive) return "Click Map...";
	return "Add Transport";
});

const transportButtonTooltip = computed(() => {
	if (geoStore.transportMarker) return "Click to remove transport marker and stations";
	if (geoStore.transportMarkerModeActive) return "Click on the map to place transport marker";
	return "Click to enable transport marker placement";
});

const toggleTransportMarker = () => {
	geoStore.toggleTransportMarkerMode();
};

// Estate overview methods
const closeEstateOverview = () => {
	geoStore.clearSelectedEstate();
};

// Format helpers
const formatPrice = (value: number | null | undefined): string => {
	if (value === null || value === undefined) return "None";
	return `€${value.toLocaleString()}`;
};

const formatArea = (value: number | null | undefined): string => {
	if (value === null || value === undefined) return "None";
	return `${value}m²`;
};

const formatDistance = (value: number | null | undefined): string => {
	if (value === null || value === undefined) return "None";
	return `${Math.round(value)}m`;
};

const formatDate = (value: string | null | undefined): string => {
	if (!value) return "None";
	try {
		return new Date(value).toLocaleDateString();
	} catch {
		return value;
	}
};

const formatDistanceMeters = (value: number | null | undefined): string => {
	if (value === null || value === undefined) return "N/A";
	if (value >= 1000) {
		return `${(value / 1000).toFixed(1)}km`;
	}
	return `${Math.round(value)}m`;
};

const formatWalkingTime = (value: number | null | undefined): string => {
	if (value === null || value === undefined) return "N/A";
	if (value < 1) {
		return "< 1 min";
	}
	return `${Math.round(value)} min`;
};

// Tool button handlers
const onTransportTool = () => {
	console.log("Transport in the area tool clicked");
	// TODO: Implement transport search around estate location
};

const onAmenitiesTool = () => {
	console.log("Amenities in the area tool clicked");
	// TODO: Implement amenities search around estate location
};

const onPoiDistanceTool = () => {
	console.log("POI distance tool clicked");
	geoStore.togglePoiDistances();
};
</script>

<style scoped>
/* Estate Overview Card */
.estate-overview-card {
	position: fixed;
	top: 80px;
	left: 20px;
	z-index: 1001;
	padding: 16px;
	background-color: rgba(255, 255, 255, 0.95);
	border-radius: 8px;
	backdrop-filter: blur(4px);
	min-width: 300px;
	max-width: 340px;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	max-height: calc(100vh - 200px);
	overflow-y: auto;
}

.estate-header {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 12px;
	padding-bottom: 8px;
	border-bottom: 2px solid #1976d2;
}

.estate-icon {
	flex-shrink: 0;
}

.estate-title {
	flex: 1;
	font-weight: 600;
	font-size: 14px;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.estate-content {
	display: flex;
	flex-direction: column;
	gap: 6px;
}

.estate-row {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
}

.estate-row .q-icon {
	color: #666;
	flex-shrink: 0;
}

.estate-label {
	color: #666;
	min-width: 100px;
	flex-shrink: 0;
}

.estate-value {
	color: #333;
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.estate-value.price {
	color: #1976d2;
	font-weight: 700;
	font-size: 14px;
}

.estate-divider {
	height: 1px;
	background-color: #e0e0e0;
	margin: 6px 0;
}

.estate-link {
	margin-top: 8px;
	padding-top: 8px;
	border-top: 1px solid #e0e0e0;
}

.estate-link a {
	color: #1976d2;
	text-decoration: none;
	font-weight: 500;
}

.estate-link a:hover {
	text-decoration: underline;
}

/* Estate Tools Section */
.estate-tools {
	margin-top: 12px;
	padding-top: 12px;
	border-top: 2px solid #1976d2;
}

.tools-header {
	font-size: 12px;
	font-weight: 600;
	color: #666;
	margin-bottom: 8px;
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

.tools-buttons {
	display: flex;
	gap: 8px;
	justify-content: space-between;
}

.tools-buttons .q-btn {
	flex: 1;
}

/* POI Distances Panel */
.poi-distances-panel {
	margin-top: 12px;
	padding: 12px;
	background-color: #f5f5f5;
	border-radius: 6px;
	border: 1px solid #e0e0e0;
}

.poi-distances-header {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 12px;
	font-weight: 600;
	color: #333;
	margin-bottom: 10px;
	padding-bottom: 6px;
	border-bottom: 1px solid #e0e0e0;
}

.poi-distances-loading {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
	color: #666;
	padding: 8px 0;
}

.poi-distances-empty {
	font-size: 11px;
	color: #888;
	font-style: italic;
	padding: 8px 0;
}

.poi-distances-list {
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.poi-distance-item {
	display: flex;
	align-items: flex-start;
	gap: 10px;
	padding: 8px;
	background-color: white;
	border-radius: 4px;
	border: 1px solid #eee;
}

.poi-color-dot {
	width: 12px;
	height: 12px;
	border-radius: 50%;
	flex-shrink: 0;
	margin-top: 2px;
}

.poi-distance-info {
	flex: 1;
	min-width: 0;
}

.poi-distance-coords {
	font-size: 11px;
	color: #666;
	margin-bottom: 4px;
}

.poi-distance-values {
	display: flex;
	gap: 12px;
	font-size: 12px;
}

.distance-meters {
	font-weight: 600;
	color: #1976d2;
}

.distance-walking {
	color: #666;
}

.poi-distance-error {
	font-size: 11px;
	color: #d32f2f;
	font-style: italic;
}

/* Municipalities Panel */
.municipalities-panel {
	position: fixed;
	top: 80px;
	left: 20px;
	z-index: 1000;
	display: flex;
	flex-direction: column;
	gap: 8px;
	padding: 16px;
	background-color: rgba(255, 255, 255, 0.3);
	border-radius: 8px;
	backdrop-filter: blur(4px);
	min-width: 250px;
	max-width: 300px;
}

.panel-header {
	display: flex;
	align-items: center;
	gap: 8px;
}

.panel-icon {
	color: #1976d2;
}

.panel-title {
	flex: 1;
	font-weight: 500;
	color: #333;
}

.panel-count {
	font-size: 12px;
	color: #666;
}

.municipalities-list {
	display: flex;
	flex-direction: column;
	gap: 6px;
}

.empty-message {
	font-size: 12px;
	color: #999;
	font-style: italic;
	text-align: center;
	padding: 8px;
}

.municipality-item {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px;
	background-color: white;
	border-radius: 4px;
	cursor: grab;
	transition:
		background-color 0.2s,
		transform 0.2s;
}

.municipality-item:hover {
	background-color: #f5f5f5;
}

.municipality-item:active {
	cursor: grabbing;
	transform: scale(1.02);
}

.item-order {
	width: 24px;
	height: 24px;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #1976d2;
	color: white;
	border-radius: 50%;
	font-size: 12px;
	font-weight: bold;
	flex-shrink: 0;
}

.item-name {
	flex: 1;
	font-size: 14px;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.item-actions {
	display: flex;
	gap: 2px;
	flex-shrink: 0;
}

.transport-section {
	margin-top: 12px;
	padding-top: 12px;
	border-top: 1px solid rgba(0, 0, 0, 0.1);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 8px;
}

.radius-input {
	margin-top: 4px;
}
</style>
