import { acceptHMRUpdate, defineStore } from "pinia";
import type { Municipality } from "src/types/Municipality";
import type { MunicipalityFeatureCollection } from "src/types/MunicipalityGeoJson";
import type { StationDistanceDto } from "src/types/Station";
import { mapMunicipalities } from "src/mapper/MunicipalityMapper";
import regionService from "src/service/regionService";
import transportService from "src/service/transportService";

const MAX_MUNICIPALITIES = 5;

export const useGeoStore = defineStore("geoStore", {
	state: () => ({
		municipalities: [] as Municipality[],
		municipalitiesNames: [] as string[],
		selectedMunicipalities: [] as string[],
		heatPoints: [] as Array<{
			name: string;
			iso: string;
			latitude: number;
			longitude: number;
			intensity: number;
		}>,
		regionHeatPoints: [] as [number, number][],
		stationMarkers: [] as StationDistanceDto[],
		transportMarker: null as { lat: number; lon: number; radius: number } | null,
		transportMarkerModeActive: false,
		pendingTransportRadius: 100, // Radius to use when placing marker
		loadingStations: false,
		loading: false,
		loaded: false,
	}),

	getters: {
		getSelectedMunicipalities(): Municipality[] {
			return this.municipalities.filter((m) => this.selectedMunicipalities.includes(m.name));
		},
		canAddMoreMunicipalities(): boolean {
			return this.selectedMunicipalities.length < MAX_MUNICIPALITIES;
		},
		selectedMunicipalitiesCount(): number {
			return this.selectedMunicipalities.length;
		},
	},

	actions: {
		addSelectedMunicipality(name: string): boolean {
			if (this.selectedMunicipalities.length >= MAX_MUNICIPALITIES) {
				console.warn(`Cannot add more than ${MAX_MUNICIPALITIES} municipalities`);
				return false;
			}
			if (this.selectedMunicipalities.includes(name)) {
				console.warn(`Municipality ${name} is already selected`);
				return false;
			}
			console.log("Selected municipality: ", name);
			this.selectedMunicipalities.push(name);
			return true;
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
		moveMunicipalityUp(name: string) {
			const index = this.selectedMunicipalities.indexOf(name);
			if (index > 0) {
				const temp = this.selectedMunicipalities[index - 1];
				if (temp !== undefined) {
					this.selectedMunicipalities[index - 1] = name;
					this.selectedMunicipalities[index] = temp;
				}
			}
		},
		moveMunicipalityDown(name: string) {
			const index = this.selectedMunicipalities.indexOf(name);
			if (index < this.selectedMunicipalities.length - 1) {
				const temp = this.selectedMunicipalities[index + 1];
				if (temp !== undefined) {
					this.selectedMunicipalities[index + 1] = name;
					this.selectedMunicipalities[index] = temp;
				}
			}
		},
		reorderMunicipalities(newOrder: string[]) {
			this.selectedMunicipalities = newOrder;
		},
		async addRegionAndFetchPoints(regionName: string) {
			console.log("addRegionAndFetchPoints called for:", regionName);
			const added = this.addSelectedMunicipality(regionName);
			if (!added) {
				return;
			}

			// Fetch region heat points
			const points = await regionService.fetchRegionPoints(regionName);
			console.log("Fetched region points:", points);
			this.regionHeatPoints = points;
		},
		// Transport marker mode - toggle to enable placing a marker on the map
		toggleTransportMarkerMode() {
			if (this.transportMarker) {
				// If marker exists, remove it and clear stations
				this.clearTransportMarker();
			} else {
				// Enable mode to place marker
				this.transportMarkerModeActive = !this.transportMarkerModeActive;
			}
		},
		// Set transport marker at a specific location and fetch stations
		async setTransportMarker(lat: number, lon: number) {
			const radius = this.pendingTransportRadius;
			this.transportMarkerModeActive = false;
			this.transportMarker = { lat, lon, radius };
			this.loadingStations = true;
			this.stationMarkers = [];

			try {
				console.log(`Fetching stations at:`, lat, lon, `with radius:`, radius);
				const stations = await transportService.findNearbyStationsDetailed(lat, lon, radius);
				this.stationMarkers = stations;
				console.log("Fetched stations:", stations.length);
			} catch (error) {
				console.error("Error fetching nearby stations:", error);
			} finally {
				this.loadingStations = false;
			}
		},
		// Set the pending radius for transport marker
		setPendingTransportRadius(radius: number) {
			this.pendingTransportRadius = Math.min(Math.max(radius, 1), 1000);
		},
		// Clear transport marker and stations
		clearTransportMarker() {
			this.transportMarker = null;
			this.transportMarkerModeActive = false;
			this.stationMarkers = [];
		},
		async loadGeoData() {
			console.log("Loading Vienna districts...");
			this.loading = true;

			const result = await fetch("../src/data/geo/bezirke_999_geo.json");

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

			// Load heat points
			// try {
			// 	const heatPointsResult = await fetch("../src/data/geo/test_points.json");
			// 	if (heatPointsResult.ok) {
			// 		this.heatPoints = await heatPointsResult.json();
			// 		console.log("Heat points loaded:", this.heatPoints);
			// 	} else {
			// 		console.warn("Heat points JSON not found");
			// 	}
			// } catch (error) {
			// 	console.error("Error loading heat points:", error);
			// }

			this.loading = false;
			this.loaded = true;
		},
	},
});

if (import.meta.hot) {
	import.meta.hot.accept(acceptHMRUpdate(useGeoStore, import.meta.hot));
}
