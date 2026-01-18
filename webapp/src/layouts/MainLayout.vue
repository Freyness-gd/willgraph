<template>
	<q-layout view="lHh Lpr lFf">
		<q-header elevated>
			<q-toolbar>
				<q-toolbar-title> WillGraph</q-toolbar-title>
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
								<q-item v-if="searchLoading" disable>
									<q-item-section>
										<q-spinner class="q-mr-sm" size="20px" />
										Searching...
									</q-item-section>
								</q-item>
								<q-item v-else-if="searchResults.length === 0" disable>
									<q-item-section> No results found</q-item-section>
								</q-item>
								<q-item v-for="region in searchResults" :key="region.iso" clickable @click="addMunicipality(region)">
									<q-item-section>
										{{ region.name }}
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
						<q-btn
							:color="geoStore.showEstateTransport ? 'secondary' : 'primary'"
							:loading="geoStore.estateTransportLoading"
							dense
							icon="directions_bus"
							outline
							@click="onTransportTool"
						>
							<q-tooltip>Transport in the area</q-tooltip>
						</q-btn>
						<q-btn
							:color="geoStore.showEstateAmenities ? 'secondary' : 'primary'"
							:loading="geoStore.estateAmenitiesLoading"
							dense
							icon="storefront"
							outline
							@click="onAmenitiesTool"
						>
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

				<!-- Estate Transport Panel -->
				<div v-if="geoStore.showEstateTransport" class="estate-transport-panel">
					<div class="transport-panel-header">
						<q-icon color="primary" name="directions_bus" size="18px" />
						<span>Transport Stations</span>
					</div>

					<!-- Radius Slider -->
					<div class="transport-radius-control">
						<span class="radius-label">Radius: {{ geoStore.estateTransportRadius }}m</span>
						<q-slider
							v-model="estateTransportRadiusLocal"
							:label-value="estateTransportRadiusLocal + 'm'"
							:max="1000"
							:min="100"
							:step="100"
							color="primary"
							label
							@update:model-value="onEstateTransportRadiusChange"
						/>
					</div>

					<!-- Loading State -->
					<div v-if="geoStore.estateTransportLoading" class="transport-loading">
						<q-spinner color="primary" size="20px" />
						<span>Loading stations...</span>
					</div>

					<!-- Empty State -->
					<div v-else-if="geoStore.estateTransportStations.length === 0" class="transport-empty">
						<span>No stations found in this radius.</span>
					</div>

					<!-- Stations List -->
					<div v-else class="transport-stations-list">
						<div
							v-for="(station, index) in geoStore.estateTransportStations"
							:key="index"
							class="transport-station-item"
						>
							<q-icon color="green" name="directions_bus" size="16px" />
							<div class="station-info">
								<div class="station-name">{{ station.name }}</div>
								<div class="station-details">
									<span class="station-type">{{ station.type }}</span>
									<span v-if="station.line" class="station-line">Line {{ station.line }}</span>
								</div>
								<div class="station-distance">
									<span>{{ formatDistanceMeters(station.distanceInMeters) }}</span>
									<span class="walking-time">🚶 {{ formatWalkingTime(station.walkingDurationInMinutes) }}</span>
								</div>
							</div>
						</div>
					</div>
				</div>

				<!-- Estate Amenities Panel -->
				<div v-if="geoStore.showEstateAmenities" class="estate-amenities-panel">
					<div class="amenities-panel-header">
						<q-icon color="orange" name="storefront" size="18px" />
						<span>Nearby Amenities</span>
					</div>

					<!-- Radius Slider -->
					<div class="amenities-radius-control">
						<span class="radius-label">Radius: {{ geoStore.estateAmenitiesRadius }}m</span>
						<q-slider
							v-model="estateAmenitiesRadiusLocal"
							:label-value="estateAmenitiesRadiusLocal + 'm'"
							:max="1000"
							:min="100"
							:step="100"
							color="orange"
							label
							@update:model-value="onEstateAmenitiesRadiusChange"
						/>
					</div>

					<!-- Loading State -->
					<div v-if="geoStore.estateAmenitiesLoading" class="amenities-loading">
						<q-spinner color="orange" size="20px" />
						<span>Loading amenities...</span>
					</div>

					<!-- Empty State -->
					<div v-else-if="geoStore.estateAmenities.length === 0" class="amenities-empty">
						<span>No amenities found in this radius.</span>
					</div>

					<!-- Amenities List -->
					<div v-else class="amenities-list">
						<div v-for="(amenity, index) in geoStore.estateAmenities" :key="index" class="amenity-item">
							<q-icon color="orange" name="storefront" size="16px" />
							<div class="amenity-info">
								<div class="amenity-name">{{ amenity.name }}</div>
								<div class="amenity-details">
									<span class="amenity-category">{{ amenity.category }}</span>
								</div>
								<div class="amenity-distance">
									<span>{{ formatDistanceMeters(amenity.distanceInMeters) }}</span>
									<span class="walking-time">🚶 {{ formatWalkingTime(amenity.walkingDurationInMinutes) }}</span>
								</div>
							</div>
						</div>
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
						<span>Calculating direct distances...</span>
					</div>
					<div v-else-if="geoStore.poiDistances.length === 0" class="poi-distances-empty">
						<span>No POIs added. Add POIs using the form on the right.</span>
					</div>
					<div v-else class="poi-distances-list">
						<div v-for="poi in geoStore.poiDistances" :key="poi.id" class="poi-distance-item">
							<div class="poi-entry-summary">
								<div :style="{ backgroundColor: poi.color }" class="poi-color-dot"></div>
								<div class="poi-distance-info">
									<div class="poi-top-row">
										<span class="poi-distance-coords">{{ poi.lat.toFixed(4) }}, {{ poi.lon.toFixed(4) }}</span>
										<q-btn
											:color="poi.transportPath ? 'grey' : 'primary'"
											:icon="poi.transportPath ? 'expand_less' : 'alt_route'"
											:loading="poi.isLoadingRoute"
											dense
											flat
											round
											size="sm"
											@click="geoStore.toggleRoute(poi)"
										>
											<q-tooltip>
												{{ poi.transportPath ? "Hide Route" : "Show Transport Route" }}
											</q-tooltip>
										</q-btn>
									</div>
									<div v-if="poi.distance" class="poi-distance-values">
										<span class="distance-meters">{{ formatDistanceMeters(poi.distance.distanceInMeters) }}</span>
										<span class="distance-walking">
											🚶 {{ formatWalkingTime(poi.distance.walkingDurationInMinutes) }}
										</span>
									</div>
									<div v-else class="poi-distance-error">Unable to calculate</div>
								</div>
							</div>

							<div v-if="poi.transportPath" class="poi-route-details">
								<div class="route-summary">
									<span class="text-weight-bold">{{ poi.transportPath.numberOfStops }} Stops</span>
									<span class="text-caption text-grey">
										(Walk start: {{ Math.round(poi.transportPath.walkToStationMeters) }}m)
									</span>
								</div>
								<q-timeline class="compact-timeline" color="secondary" layout="dense">
									<q-timeline-entry
										v-for="(station, idx) in poi.transportPath.stations"
										:key="idx"
										:color="getSegmentColor(station)"
										:icon="getSegmentIcon(station)"
										:title="station.name"
										dense
									>
										<div v-if="idx > 0 && station.travelTimeInMinutes" class="text-caption text-grey">
											<q-icon name="schedule" size="xs" />
											{{ Math.ceil(station.travelTimeInMinutes) }} min
											{{ station.segmentType === "WALK" ? "transfer walk" : "ride" }}
											<span v-if="station.segmentType === 'WALK'" class="text-orange text-weight-bold">
												(Change Lines)
											</span>
										</div>
									</q-timeline-entry>
								</q-timeline>
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
import { useDebounceFn } from "@vueuse/core";
import type { StationDistanceDto } from "../types/TransportPath";
import regionService from "src/service/regionService";
import type { RegionDto } from "src/types/dto";

const $q = useQuasar();

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchMenuOpen = ref(false);
const searchInput = ref<QInput | null>(null);
const draggedIndex = ref<number | null>(null);
const searchResults = ref<RegionDto[]>([]);
const searchLoading = ref(false);

// Estate transport radius - local state for immediate UI feedback
const estateTransportRadiusLocal = ref(300);

// Debounced function to update radius and fetch stations
const debouncedFetchStations = useDebounceFn(() => {
	geoStore.setEstateTransportRadius(estateTransportRadiusLocal.value);
	void geoStore.fetchEstateTransportStations();
}, 1000);

// Handler for radius slider change
const onEstateTransportRadiusChange = (value: number | null) => {
	if (value === null) return;
	estateTransportRadiusLocal.value = value;
	void debouncedFetchStations();
};

// Sync local radius with store when transport panel opens
watch(
	() => geoStore.showEstateTransport,
	(show) => {
		if (show) {
			estateTransportRadiusLocal.value = geoStore.estateTransportRadius;
		}
	}
);

// Estate amenities radius - local state for immediate UI feedback
const estateAmenitiesRadiusLocal = ref(300);

// Debounced function to update radius and fetch amenities
const debouncedFetchAmenities = useDebounceFn(() => {
	geoStore.setEstateAmenitiesRadius(estateAmenitiesRadiusLocal.value);
	void geoStore.fetchEstateAmenities();
}, 1000);

// Handler for amenities radius slider change
const onEstateAmenitiesRadiusChange = (value: number | null) => {
	if (value === null) return;
	estateAmenitiesRadiusLocal.value = value;
	void debouncedFetchAmenities();
};

// Sync local radius with store when amenities panel opens
watch(
	() => geoStore.showEstateAmenities,
	(show) => {
		if (show) {
			estateAmenitiesRadiusLocal.value = geoStore.estateAmenitiesRadius;
		}
	}
);

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

// Debounced function to search regions from API
const debouncedSearchRegions = useDebounceFn(async (query: string) => {
	if (query.length < 3) {
		searchResults.value = [];
		searchMenuOpen.value = false;
		return;
	}

	searchLoading.value = true;
	try {
		const results = await regionService.searchRegions(query, 10);
		searchResults.value = results;
		searchMenuOpen.value = results.length > 0;
	} catch (error) {
		console.error("Error searching regions:", error);
		searchResults.value = [];
	} finally {
		searchLoading.value = false;
	}
}, 300);

// Watcher
watch(search, (value) => {
	if (value.length < 3) {
		searchResults.value = [];
		searchMenuOpen.value = false;
		return;
	}
	void debouncedSearchRegions(value);
});

// Methods
const addMunicipality = (region: RegionDto) => {
	if (geoStore.isMunicipalitySaved(region.name)) {
		geoStore.removeSelectedMunicipality(region.name);
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

	// Use the new action that adds the region with its geometry
	geoStore.addRegion(region);
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

const getSegmentIcon = (station: StationDistanceDto) => {
	if (station.segmentType === "WALK") return "directions_walk";
	if (station.segmentType === "START") return "place";
	return station.type === "Subway" ? "subway" : "directions_bus";
};

const getSegmentColor = (station: StationDistanceDto) => {
	if (station.segmentType === "WALK") return "orange";
	if (station.segmentType === "START") return "grey";
	return "secondary";
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
	geoStore.toggleEstateTransport();
};

const onAmenitiesTool = () => {
	console.log("Amenities in the area tool clicked");
	geoStore.toggleEstateAmenities();
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
	bottom: 20px;
	z-index: 1001;
	padding: 16px;
	background-color: rgba(255, 255, 255, 0.95);
	border-radius: 8px;
	backdrop-filter: blur(4px);
	min-width: 300px;
	max-width: 340px;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	overflow-y: auto;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.estate-header {
	display: flex;
	align-items: flex-start;
	gap: 8px;
	margin-bottom: 12px;
	padding-bottom: 8px;
	border-bottom: 2px solid #1976d2;
	flex-shrink: 0;
}

.estate-icon {
	flex-shrink: 0;
}

.estate-title {
	flex: 1;
	font-weight: 600;
	font-size: 14px;
	color: #333;
	word-wrap: break-word;
	white-space: normal;
}

.estate-content {
	display: flex;
	flex-direction: column;
	gap: 6px;
	flex: 1;
	overflow-y: auto;
}

.estate-row {
	display: flex;
	align-items: flex-start;
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
	word-wrap: break-word;
	white-space: normal;
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
	flex-direction: column;
	overflow: hidden;
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
	display: flex;
	align-items: flex-start;
	gap: 10px;
	padding: 10px;
	width: 100%;
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

.poi-top-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.poi-route-details {
	background-color: #fafafa;
	border-top: 1px solid #eee;
	padding: 10px 12px;
	width: 100%;
	box-sizing: border-box;
}

.q-timeline__entry--dense .q-timeline__content {
	padding-bottom: 8px;
}

.q-timeline__title {
	font-size: 13px;
	line-height: 1.2;
	margin-bottom: 2px;
	white-space: normal;
	word-break: break-word;
}

.q-timeline__subtitle {
	font-size: 11px;
	opacity: 0.7;
	text-transform: none;
	white-space: normal;
	line-height: 1.1;
}

.poi-distances-panel {
	margin-top: 12px;
	padding: 12px;
	background-color: #f5f5f5;
	border-radius: 6px;
	border: 1px solid #e0e0e0;
	width: 100%;
	box-sizing: border-box;
}

/* Estate Transport Panel */
.estate-transport-panel {
	margin-top: 12px;
	padding: 12px;
	background-color: #e8f5e9;
	border-radius: 6px;
	border: 1px solid #c8e6c9;
}

.transport-panel-header {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 12px;
	font-weight: 600;
	color: #333;
	margin-bottom: 10px;
	padding-bottom: 6px;
	border-bottom: 1px solid #c8e6c9;
}

.transport-radius-control {
	margin-bottom: 12px;
}

.radius-label {
	font-size: 11px;
	color: #666;
	display: block;
	margin-bottom: 4px;
}

.transport-loading {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
	color: #666;
	padding: 8px 0;
}

.transport-empty {
	font-size: 11px;
	color: #888;
	font-style: italic;
	padding: 8px 0;
}

.transport-stations-list {
	display: flex;
	flex-direction: column;
	gap: 8px;
	max-height: 200px;
	overflow-y: auto;
}

.transport-station-item {
	display: flex;
	align-items: flex-start;
	gap: 8px;
	padding: 8px;
	background-color: white;
	border-radius: 4px;
	border: 1px solid #e0e0e0;
}

.station-info {
	flex: 1;
	min-width: 0;
}

.station-name {
	font-size: 12px;
	font-weight: 600;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.station-details {
	display: flex;
	gap: 8px;
	font-size: 10px;
	color: #666;
	margin-top: 2px;
}

.station-type {
	text-transform: capitalize;
}

.station-line {
	color: #1976d2;
	font-weight: 500;
}

.station-distance {
	display: flex;
	gap: 8px;
	font-size: 11px;
	margin-top: 4px;
	color: #4caf50;
	font-weight: 500;
}

.walking-time {
	color: #666;
	font-weight: normal;
}

/* Estate Amenities Panel */
.estate-amenities-panel {
	margin-top: 12px;
	padding: 12px;
	background-color: #fff3e0;
	border-radius: 6px;
	border: 1px solid #ffe0b2;
}

.amenities-panel-header {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 12px;
	font-weight: 600;
	color: #333;
	margin-bottom: 10px;
	padding-bottom: 6px;
	border-bottom: 1px solid #ffe0b2;
}

.amenities-radius-control {
	margin-bottom: 12px;
}

.amenities-loading {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 12px;
	color: #666;
	padding: 8px 0;
}

.amenities-empty {
	font-size: 11px;
	color: #888;
	font-style: italic;
	padding: 8px 0;
}

.amenities-list {
	display: flex;
	flex-direction: column;
	gap: 8px;
	max-height: 200px;
	overflow-y: auto;
}

.amenity-item {
	display: flex;
	align-items: flex-start;
	gap: 8px;
	padding: 8px;
	background-color: white;
	border-radius: 4px;
	border: 1px solid #e0e0e0;
}

.amenity-info {
	flex: 1;
	min-width: 0;
}

.amenity-name {
	font-size: 12px;
	font-weight: 600;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.amenity-details {
	display: flex;
	gap: 8px;
	font-size: 10px;
	color: #666;
	margin-top: 2px;
}

.amenity-category {
	text-transform: capitalize;
}

.amenity-distance {
	display: flex;
	gap: 8px;
	font-size: 11px;
	margin-top: 4px;
	color: #ff9800;
	font-weight: 500;
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

.route-summary {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
	padding-bottom: 6px;
	border-bottom: 1px dashed #e0e0e0;
	font-size: 12px;
}

.compact-timeline {
	padding-left: 4px;
}
</style>
