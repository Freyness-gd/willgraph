import { acceptHMRUpdate, defineStore } from "pinia";
import type { Municipality } from "src/types/Municipality";
import type { MunicipalityFeatureCollection } from "src/types/MunicipalityGeoJson";
import type { StationDistanceDto } from "src/types/Station";
import type { PoiDistanceDto } from "src/types/Poi";
import type { RealEstateDto } from "src/types/RealEstate";
import type { Point, PoiWithDistance } from "src/types/Point";
import { mapMunicipalities } from "src/mapper/MunicipalityMapper";
import regionService from "src/service/regionService";
import transportService from "src/service/transportService";
import poiService from "src/service/poiService";

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
		regionEstatesMap: new Map<string, RealEstateDto[]>(),
		selectedEstate: null as RealEstateDto | null,
		// POI state
		poiList: [] as Point[],
		poiDistances: [] as PoiWithDistance[],
		poiDistancesLoading: false,
		showPoiDistances: false,
		// Estate transport state
		showEstateTransport: false,
		estateTransportRadius: 100,
		estateTransportStations: [] as StationDistanceDto[],
		estateTransportLoading: false,
		// Estate amenities state
		showEstateAmenities: false,
		estateAmenitiesRadius: 500,
		estateAmenities: [] as PoiDistanceDto[],
		estateAmenitiesLoading: false,
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
			// Remove estates from map
			this.regionEstatesMap.delete(name);
			// Recalculate heat points from remaining regions
			this.recalculateHeatPoints();
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

			// Fetch real estates for region
			const estates = await regionService.fetchRealEstatesInRegion(regionName);
			console.log("Fetched estates for region:", regionName, estates.length);

			// Save estates in map
			this.regionEstatesMap.set(regionName, estates);

			// Recalculate heat points from all regions
			this.recalculateHeatPoints();
		},
		// Recalculate heat points from all regions in the map
		recalculateHeatPoints() {
			const allPoints: [number, number][] = [];
			this.regionEstatesMap.forEach((estates) => {
				estates.forEach((estate) => {
					if (estate.address?.location?.latitude != null && estate.address?.location?.longitude != null) {
						allPoints.push([estate.address.location.latitude, estate.address.location.longitude]);
					}
				});
			});
			this.regionHeatPoints = allPoints;
			console.log("Recalculated heat points:", allPoints.length);
		},
		// Get all estates from all regions as a flat array
		getAllEstates(): RealEstateDto[] {
			const allEstates: RealEstateDto[] = [];
			this.regionEstatesMap.forEach((estates) => {
				allEstates.push(...estates);
			});
			return allEstates;
		},
		// Find estate at specific coordinates
		findEstateAtCoordinates(lat: number, lon: number, tolerance: number = 0.0001): RealEstateDto | null {
			for (const estates of this.regionEstatesMap.values()) {
				for (const estate of estates) {
					const estateLat = estate.address?.location?.latitude;
					const estateLon = estate.address?.location?.longitude;
					if (estateLat != null && estateLon != null) {
						if (Math.abs(estateLat - lat) < tolerance && Math.abs(estateLon - lon) < tolerance) {
							return estate;
						}
					}
				}
			}
			return null;
		},
		// Find all estates at specific coordinates (there may be multiple at same location)
		findEstatesAtCoordinates(lat: number, lon: number, tolerance: number = 0.0001): RealEstateDto[] {
			const found: RealEstateDto[] = [];
			for (const estates of this.regionEstatesMap.values()) {
				for (const estate of estates) {
					const estateLat = estate.address?.location?.latitude;
					const estateLon = estate.address?.location?.longitude;
					if (estateLat != null && estateLon != null) {
						if (Math.abs(estateLat - lat) < tolerance && Math.abs(estateLon - lon) < tolerance) {
							found.push(estate);
						}
					}
				}
			}
			return found;
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
		// Select an estate for detailed view
		selectEstate(estate: RealEstateDto) {
			this.selectedEstate = estate;
			// Reset POI distances when selecting a new estate
			this.poiDistances = [];
			this.showPoiDistances = false;
			// Reset estate transport
			this.showEstateTransport = false;
			this.estateTransportRadius = 100;
			this.estateTransportStations = [];
			// Reset estate amenities
			this.showEstateAmenities = false;
			this.estateAmenitiesRadius = 500;
			this.estateAmenities = [];
		},
		// Clear selected estate
		clearSelectedEstate() {
			this.selectedEstate = null;
			this.poiDistances = [];
			this.showPoiDistances = false;
			// Reset estate transport
			this.showEstateTransport = false;
			this.estateTransportRadius = 100;
			this.estateTransportStations = [];
			// Reset estate amenities
			this.showEstateAmenities = false;
			this.estateAmenitiesRadius = 500;
			this.estateAmenities = [];
		},
		// Set POI list (called from MapOverlayForm)
		setPoiList(pois: Point[]) {
			this.poiList = pois;
		},
		// Add a POI
		addPoi(poi: Point) {
			this.poiList.push(poi);
		},
		// Remove a POI
		removePoi(poiId: string) {
			this.poiList = this.poiList.filter((p) => p.id !== poiId);
		},
		// Toggle POI distances panel
		togglePoiDistances() {
			this.showPoiDistances = !this.showPoiDistances;
			if (this.showPoiDistances) {
				void this.calculatePoiDistances();
			}
		},
		// Toggle estate transport panel
		toggleEstateTransport() {
			this.showEstateTransport = !this.showEstateTransport;
			if (this.showEstateTransport) {
				void this.fetchEstateTransportStations();
			} else {
				this.estateTransportStations = [];
			}
		},
		// Set estate transport radius (called with debounce from UI)
		setEstateTransportRadius(radius: number) {
			this.estateTransportRadius = Math.min(Math.max(radius, 100), 1000);
		},
		// Fetch transport stations around the selected estate
		async fetchEstateTransportStations() {
			if (!this.selectedEstate?.address?.location) {
				console.warn("No selected estate with location");
				return;
			}

			const estateLat = this.selectedEstate.address.location.latitude;
			const estateLon = this.selectedEstate.address.location.longitude;

			if (estateLat == null || estateLon == null) {
				console.warn("Selected estate has no coordinates");
				return;
			}

			this.estateTransportLoading = true;

			try {
				console.log(
					`Fetching stations around estate at [${estateLat}, ${estateLon}] with radius ${this.estateTransportRadius}m`
				);
				const stations = await transportService.findNearbyStationsDetailed(
					estateLat,
					estateLon,
					this.estateTransportRadius
				);
				this.estateTransportStations = stations;
				console.log("Fetched estate transport stations:", stations.length);
			} catch (error) {
				console.error("Error fetching estate transport stations:", error);
				this.estateTransportStations = [];
			} finally {
				this.estateTransportLoading = false;
			}
		},
		// Toggle estate amenities panel
		toggleEstateAmenities() {
			this.showEstateAmenities = !this.showEstateAmenities;
			if (this.showEstateAmenities) {
				void this.fetchEstateAmenities();
			} else {
				this.estateAmenities = [];
			}
		},
		// Set estate amenities radius (called with debounce from UI)
		setEstateAmenitiesRadius(radius: number) {
			this.estateAmenitiesRadius = Math.min(Math.max(radius, 100), 1000);
		},
		// Fetch amenities around the selected estate
		async fetchEstateAmenities() {
			if (!this.selectedEstate?.address?.location) {
				console.warn("No selected estate with location");
				return;
			}

			const estateLat = this.selectedEstate.address.location.latitude;
			const estateLon = this.selectedEstate.address.location.longitude;

			if (estateLat == null || estateLon == null) {
				console.warn("Selected estate has no coordinates");
				return;
			}

			this.estateAmenitiesLoading = true;

			try {
				console.log(
					`Fetching amenities around estate at [${estateLat}, ${estateLon}] with radius ${this.estateAmenitiesRadius}m`
				);
				const amenities = await poiService.findPoIsNearby(estateLat, estateLon, this.estateAmenitiesRadius);
				this.estateAmenities = amenities;
				console.log("Fetched estate amenities:", amenities.length);
			} catch (error) {
				console.error("Error fetching estate amenities:", error);
				this.estateAmenities = [];
			} finally {
				this.estateAmenitiesLoading = false;
			}
		},
		// Calculate distances from selected estate to all POIs
		async calculatePoiDistances() {
			if (!this.selectedEstate?.address?.location) {
				console.warn("No selected estate with location");
				return;
			}

			const estateLat = this.selectedEstate.address.location.latitude;
			const estateLon = this.selectedEstate.address.location.longitude;

			if (estateLat == null || estateLon == null) {
				console.warn("Selected estate has no coordinates");
				return;
			}

			if (this.poiList.length === 0) {
				console.log("No POIs to calculate distances for");
				this.poiDistances = [];
				return;
			}

			this.poiDistancesLoading = true;
			const results: PoiWithDistance[] = [];

			try {
				for (const poi of this.poiList) {
					const distance = await regionService.calculateDistanceBetweenPoints(estateLat, estateLon, poi.lat, poi.lon);

					results.push({
						...poi,
						distance: distance ?? undefined,
					});
				}

				this.poiDistances = results;
				console.log("POI distances calculated:", results);
			} catch (error) {
				console.error("Error calculating POI distances:", error);
			} finally {
				this.poiDistancesLoading = false;
			}
		},
		// Calculate distance for a single new POI (delta calculation)
		async calculateSinglePoiDistance(poi: Point) {
			if (!this.showPoiDistances || !this.selectedEstate?.address?.location) {
				return;
			}

			const estateLat = this.selectedEstate.address.location.latitude;
			const estateLon = this.selectedEstate.address.location.longitude;

			if (estateLat == null || estateLon == null) {
				return;
			}

			try {
				console.log("Calculating distance for new POI:", poi.id);
				const distance = await regionService.calculateDistanceBetweenPoints(estateLat, estateLon, poi.lat, poi.lon);

				const poiWithDistance: PoiWithDistance = {
					...poi,
					distance: distance ?? undefined,
				};

				// Add to existing distances
				this.poiDistances = [...this.poiDistances, poiWithDistance];
				console.log("Added POI distance:", poiWithDistance);
			} catch (error) {
				console.error("Error calculating single POI distance:", error);
				// Still add the POI without distance
				this.poiDistances = [...this.poiDistances, { ...poi, distance: undefined }];
			}
		},
		// Remove POI from distances list
		removePoiDistance(poiId: string) {
			this.poiDistances = this.poiDistances.filter((p) => p.id !== poiId);
		},
		// Sync POI distances with POI list (handles additions and removals)
		syncPoiDistances(newPoiList: Point[]) {
			if (!this.showPoiDistances) {
				return;
			}

			const currentIds = new Set(this.poiDistances.map((p) => p.id));
			const newIds = new Set(newPoiList.map((p) => p.id));

			// Find removed POIs
			const removedIds = [...currentIds].filter((id) => !newIds.has(id));
			if (removedIds.length > 0) {
				this.poiDistances = this.poiDistances.filter((p) => !removedIds.includes(p.id));
				console.log("Removed POI distances:", removedIds);
			}

			// Find new POIs that need distance calculation
			const newPois = newPoiList.filter((p) => !currentIds.has(p.id));
			for (const poi of newPois) {
				void this.calculateSinglePoiDistance(poi);
			}
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
