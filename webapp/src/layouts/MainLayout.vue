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

		<q-page-container>
			<router-view />
		</q-page-container>
		<MapOverlayForm />
	</q-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import { useGeoStore } from "stores/geoStore";
import type { QInput } from "quasar";
import MapOverlayForm from "components/MapOverlayForm.vue";

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchMenuOpen = ref(false);
const searchInput = ref<QInput | null>(null);

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

	// Use the new action that fetches region points
	await geoStore.addRegionAndFetchPoints(name);
	clearSearch();
};
</script>
