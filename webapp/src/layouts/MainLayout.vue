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

		<!-- Municipalities List Panel -->
		<div class="municipalities-panel">
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
import type { QInput } from "quasar";
import { useQuasar } from "quasar";

const $q = useQuasar();

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchMenuOpen = ref(false);
const searchInput = ref<QInput | null>(null);
const draggedIndex = ref<number | null>(null);

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
</script>

<style scoped>
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
</style>
