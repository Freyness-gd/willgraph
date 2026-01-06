<template>
	<q-layout view="lHh Lpr lFf">
		<q-header elevated>
			<q-toolbar>
				<q-toolbar-title> WillGraph </q-toolbar-title>
				<div class="search-wrapper" @click="focusSearch">
					<q-input
						ref="searchInput"
						v-model="search"
						:loading="searchLoadingState"
						class="p-2"
						clearable
						debounce="500"
						placeholder="Search (min 3 letters)"
						rounded
						standout
						type="search"
						@keydown="handleKeyDown"
					>
						<template v-slot:append>
							<q-icon v-if="searchIconVisibility" name="search" />
						</template>

						<q-menu v-model="searchMenuOpen" anchor="bottom left" fit self="top left">
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
	</q-layout>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch } from "vue";
import { useGeoStore } from "stores/geoStore";
import type { QInput } from "quasar";

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchLoadingState = ref(false);
const searchMenuOpen = ref(false);
const searchInput = ref<QInput | null>(null);
const leftDrawerOpen = ref(false);

// Functionality
const toggleLeftDrawer = () => {
	leftDrawerOpen.value = !leftDrawerOpen.value;
};

const focusSearch = async () => {
	await nextTick();
	const inputElement = searchInput.value?.$el?.querySelector("input") as HTMLInputElement;
	inputElement?.focus();
};

const handleKeyDown = (event: KeyboardEvent) => {
	// Hier können Sie später Keyboard-Events wie Escape, Enter, etc. behandeln
	if (event.key === "Escape") {
		clearSearch();
	}
};

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

	return results.slice(0, 5);
});

// Watcher
watch(search, (value) => {
	searchMenuOpen.value = value.length >= 3 && searchResults.value.length > 0;
});

// Methods

const addMunicipality = (name: string) => {
	if (geoStore.isMunicipalitySaved(name)) {
		geoStore.removeSelectedMunicipality(name);
		clearSearch();
		return;
	}

	geoStore.addSelectedMunicipality(name);
	clearSearch();
};
</script>
