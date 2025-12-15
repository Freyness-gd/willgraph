<template>
	<q-layout view="lHh Lpr lFf">
		<q-header elevated>
			<q-toolbar>
				<!--				<q-btn aria-label="Menu" dense flat icon="menu" round @click="toggleLeftDrawer" />-->

				<q-toolbar-title> WillGraph </q-toolbar-title>
				<q-input
					v-model="search"
					:loading="searchLoadingState"
					class="p-2"
					clearable
					debounce="500"
					placeholder="Search"
					rounded
					standout
					type="search"
					@focus="searchFocus = true"
				>
					<template v-slot:append>
						<q-icon v-if="searchIconVisibility" name="search" />
					</template>

					<q-menu v-model="searchMenuOpen" anchor="bottom left" fit self="top left">
						<q-list dense style="min-width: 250px">
							<q-item
								v-for="name in searchResults"
								:key="name"
								clickable
								@click="
									geoStore.addSelectedMunicipality(name);
									clearSearch();
								"
							>
								<q-item-section>
									{{ name }}
								</q-item-section>
							</q-item>
						</q-list>
					</q-menu>
				</q-input>
			</q-toolbar>
		</q-header>

		<!--		<q-drawer v-types="leftDrawerOpen" bordered show-if-above>-->
		<!--			<q-list>-->
		<!--				<q-item-label header> Essential Links </q-item-label>-->

		<!--				<EssentialLink v-for="link in linksList" :key="link.title" v-bind="link" />-->
		<!--			</q-list>-->
		<!--		</q-drawer>-->

		<q-page-container>
			<router-view />
		</q-page-container>
	</q-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import { type EssentialLinkProps } from "components/EssentialLink.vue";
import { useGeoStore } from "stores/geoStore";

// Pinia GeoStore
const geoStore = useGeoStore();

// Reactive State
const search = ref("");
const searchFocus = ref(false);
const searchLoadingState = ref(false);
const searchMenuOpen = ref(false);
const leftDrawerOpen = ref(false);

// Variables
const linksList: EssentialLinkProps[] = [
	{
		title: "Docs",
		caption: "quasar.dev",
		icon: "school",
		link: "https://quasar.dev",
	},
];

// Functionality
const toggleLeftDrawer = () => {
	leftDrawerOpen.value = !leftDrawerOpen.value;
};

const clearSearch = () => {
	search.value = "";
	searchFocus.value = false;
};

// Computed State
const searchIconVisibility = computed(() => {
	return !search.value || search.value.length === 0 || !searchFocus.value;
});

const searchResults = computed(() => {
	if (!search.value || search.value.length === 0) {
		return [];
	}

	const query = search.value.toLowerCase();

	console.log("Searching for: ", query);

	const results = geoStore.municipalitiesNames.filter((name) => name.toLowerCase().includes(query));

	return results.slice(0, 5);
});

// Watcher
watch(search, (value) => {
	if (!value) return;
	searchMenuOpen.value = value.length > 0 && searchResults.value.length > 0;
});
</script>
