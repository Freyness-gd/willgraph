import { ref } from "vue";
import L, { type Map as LeafletMap } from "leaflet";
import type { Point } from "src/types/Point";
import type { StationDistanceDto } from "src/types/Station";
import { MapIcons } from "./mapIcons";
import { PopupBuilder } from "./mapPopups";

/**
 * Composable for managing map markers (POI, Stations, Transport)
 */
export function useMapMarkers() {
	const mapRef = ref<LeafletMap | null>(null);

	// Storage for different marker types
	const poiMarkersMap = new Map<string, L.Marker>();
	const stationMarkersMap = new Map<string, L.Marker>();
	let transportMarkerRef: L.Marker | null = null;
	let transportCircleRef: L.Circle | null = null;

	/**
	 * Initialize the composable with a map reference
	 */
	const init = (map: LeafletMap) => {
		mapRef.value = map;
	};

	/**
	 * Adds a POI marker to the map
	 */
	const addPoiMarker = (poi: Point) => {
		if (!mapRef.value) {
			console.warn("Map not ready for POI marker");
			return;
		}

		const icon = MapIcons.createPoiIcon(poi.color);
		const marker = L.marker([poi.lat, poi.lon], { icon });
		marker.addTo(mapRef.value as any);
		poiMarkersMap.set(poi.id, marker);
	};

	/**
	 * Removes a POI marker from the map
	 */
	const removePoiMarker = (poiId: string) => {
		const marker = poiMarkersMap.get(poiId);
		if (marker && mapRef.value) {
			mapRef.value.removeLayer(marker);
			poiMarkersMap.delete(poiId);
		}
	};

	/**
	 * Clears all POI markers
	 */
	const clearPoiMarkers = () => {
		if (!mapRef.value) return;
		poiMarkersMap.forEach((marker) => {
			mapRef.value?.removeLayer(marker);
		});
		poiMarkersMap.clear();
	};

	/**
	 * Adds station markers to the map with bus icons and popups
	 */
	const addStationMarkers = (stations: StationDistanceDto[]) => {
		if (!mapRef.value) {
			console.warn("Map not ready for station markers");
			return;
		}

		console.log("addStationMarkers called with", stations.length, "stations");

		const busIcon = MapIcons.createBusIcon();

		stations.forEach((station, index) => {
			console.log(`Station ${index}:`, station);

			// Try to get coordinates from different possible structures
			let lat: number | undefined;
			let lng: number | undefined;

			if (station.location) {
				lat = station.location.latitude;
				lng = station.location.longitude;
			} else if ((station as any).latitude !== undefined && (station as any).longitude !== undefined) {
				lat = (station as any).latitude;
				lng = (station as any).longitude;
			}

			if (lat === undefined || lng === undefined) {
				console.warn(`Station ${station.name} has no valid coordinates:`, station);
				return;
			}

			const stationKey = `${station.name}-${lat}-${lng}`;

			// Skip if marker already exists
			if (stationMarkersMap.has(stationKey)) {
				console.log(`Station ${station.name} already exists, skipping`);
				return;
			}

			console.log(`Creating marker for ${station.name} at [${lat}, ${lng}]`);

			const marker = L.marker([lat, lng], { icon: busIcon });
			marker.bindPopup(PopupBuilder.createStationPopup(station));

			marker.addTo(mapRef.value as any);
			stationMarkersMap.set(stationKey, marker);
			console.log(`Marker added for ${station.name}`);
		});

		console.log(`Total station markers: ${stationMarkersMap.size}`);
	};

	/**
	 * Clears all station markers from the map
	 */
	const clearStationMarkers = () => {
		if (!mapRef.value) return;

		stationMarkersMap.forEach((marker) => {
			mapRef.value?.removeLayer(marker);
		});
		stationMarkersMap.clear();
	};

	/**
	 * Adds a transport marker with a radius circle to the map
	 */
	const addTransportMarker = (lat: number, lon: number, radius: number = 100) => {
		if (!mapRef.value) {
			console.warn("Map not ready for transport marker");
			return;
		}

		// Remove existing transport marker and circle first
		clearTransportMarker();

		const transportIcon = MapIcons.createTransportIcon();

		// Create the marker
		transportMarkerRef = L.marker([lat, lon], { icon: transportIcon });
		transportMarkerRef.bindPopup(PopupBuilder.createTransportPopup(radius));
		transportMarkerRef.addTo(mapRef.value as any);

		// Create the radius circle with light orange color
		transportCircleRef = L.circle([lat, lon], {
			radius,
			color: "#FFA726",
			fillColor: "#FFB74D",
			fillOpacity: 0.3,
			weight: 2,
			dashArray: "5, 5",
		});
		transportCircleRef.addTo(mapRef.value as any);
	};

	/**
	 * Clears the transport marker and circle from the map
	 */
	const clearTransportMarker = () => {
		if (!mapRef.value) return;

		if (transportMarkerRef) {
			mapRef.value.removeLayer(transportMarkerRef);
			transportMarkerRef = null;
		}

		if (transportCircleRef) {
			mapRef.value.removeLayer(transportCircleRef);
			transportCircleRef = null;
		}
	};

	/**
	 * Check if transport marker exists
	 */
	const hasTransportMarker = (): boolean => {
		return transportMarkerRef !== null;
	};

	return {
		init,
		// POI markers
		addPoiMarker,
		removePoiMarker,
		clearPoiMarkers,
		// Station markers
		addStationMarkers,
		clearStationMarkers,
		// Transport marker
		addTransportMarker,
		clearTransportMarker,
		hasTransportMarker,
	};
}
