import { acceptHMRUpdate, defineStore } from "pinia";
import type { Municipality } from "src/types/Municipality";
import type { MunicipalityFeatureCollection } from "src/types/MunicipalityGeoJson";
import { mapMunicipalities } from "src/mapper/MunicipalityMapper";

export const useGeoStore = defineStore("geoStore", {
	state: () => ({
		municipalities: [] as Municipality[],
		municipalitiesNames: [] as string[],
		selectedMunicipalities: [] as string[],
		loading: false,
		loaded: false,
	}),

	getters: {
		getSelectedMunicipalities(): Municipality[] {
			return this.municipalities.filter((m) => this.selectedMunicipalities.includes(m.name));
		},
	},

	actions: {
		addSelectedMunicipality(name: string) {
			console.log("Selected municipality: ", name);
			this.selectedMunicipalities.push(name);
		},
		removeSelectedMunicipality(name: string) {
			this.selectedMunicipalities = this.selectedMunicipalities.filter((m) => m !== name);
		},
		clearSelectedMunicipalities() {
			this.selectedMunicipalities = [];
		},
		isMunicipalitySaved(name: string): boolean {
			return this.selectedMunicipalities.includes(name);
		},
		async loadGeoData() {
			console.log("Loading Vienna districts...");
			this.loading = true;

			const result = await fetch("../src/data/geo/gemeinden_999_geo.json");

			if (!result.ok) {
				console.error("GeoJSON not found in /public/data");
				throw new Error("GeoJSON not found in /public/data");
			}

			const geoJson: MunicipalityFeatureCollection = await result.json();

			this.municipalities = mapMunicipalities(geoJson);

			console.log("Municipalities loaded:", this.municipalities);

			// Extract names
			this.municipalitiesNames = this.municipalities
				.map((m) => m.name)
				.filter(Boolean)
				.sort((a, b) => a.localeCompare(b));

			this.loading = false;
			this.loaded = true;
		},
	},
});

if (import.meta.hot) {
	import.meta.hot.accept(acceptHMRUpdate(useGeoStore, import.meta.hot));
}
