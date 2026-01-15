<script lang="ts" setup>
import { computed, ref } from "vue";
import { LControl, LGeoJson, LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import type { LeafletMouseEvent, Map as LeafletMap } from "leaflet";
// eslint-disable-next-line no-duplicate-imports
import L from "leaflet";
import type { Municipality } from "src/types/Municipality";
import type { Point } from "src/types/Point";
import { useGeoStore } from "stores/geoStore";
import { municipalitiesToGeoJson } from "src/mapper/MunicipalityMapper";

const geoStore = useGeoStore();
const mapRef = ref<LeafletMap | null>(null);
let heatLayerRef: any = null;
let drawnLayerRef: L.FeatureGroup | null = null;
const zoomRef = ref<number>(12);

// POI Markers storage
const poiMarkersMap = new Map<string, L.Marker>();

const emit = defineEmits<{
	(e: "map-click", lat: number, lon: number): void;
}>();

const geoJson = computed(() => municipalitiesToGeoJson(geoStore.getSelectedMunicipalities));

// Convert heatPoints from store to Leaflet format [lat, lng, intensity]
const heatPoints = computed(() => {
	const points = geoStore.heatPoints;
	const mappedPoints = points.map(
		(point) => [point.latitude, point.longitude, point.intensity] as [number, number, number]
	);
	console.log("Mapped points: ", mappedPoints);
	return mappedPoints;
});

const calculateRadiusFromZoom = (zoom: number): number => {
	const metersPerPixel = 40075016.686 / 2 ** (zoom + 8);
	const radiusInPixels = Math.round(300 / metersPerPixel); // 1000 meters = 1 km
	return Math.max(radiusInPixels, 10); // minimum radius of 10 pixels
};

/**
 * Entfernt Heat-Layer und alle gezeichneten Layer (Polygone/Marker)
 */
const clearPoints = () => {
	if (!mapRef.value) return;
	const map = mapRef.value;

	if (heatLayerRef) {
		try {
			map.removeLayer(heatLayerRef);
		} catch (error) {
			console.warn("Failed to remove heatLayer:", error);
		}
		heatLayerRef = null;
	}

	if (drawnLayerRef) {
		try {
			map.removeLayer(drawnLayerRef as any);
		} catch (error) {
			console.warn("Failed to remove drawn layer:", error);
		}
		drawnLayerRef = null;
	}
};

/**
 * Zeichnet Municipalities als Leaflet-Polygone + Popup. Die Methode entfernt zuerst alte Punkte.
 */
const drawMunicipalities = (munis: Municipality[]) => {
	if (!mapRef.value) return;
	const map = mapRef.value;

	// Clear previous points
	// clearPoints();

	// Create a feature group to hold drawn shapes (FeatureGroup has getBounds)
	drawnLayerRef = L.featureGroup();

	munis.forEach((m) => {
		try {
			// municipality.boundary.coordinates is [[[ [lng,lat], ... ]]]
			const rings = m.boundary.coordinates;
			// Convert first (outer) ring to Leaflet lat-lng pairs
			const outerRing = rings && rings[0] && rings[0][0] ? rings[0][0] : [];
			const latLngs = outerRing.map((pos: [number, number]) => [pos[1], pos[0]] as [number, number]);

			if (latLngs.length > 0) {
				const polygon = L.polygon(latLngs, {
					color: "#1976d2",
					weight: 2,
					fillOpacity: 0.15,
				});
				polygon.bindPopup(`<strong>${m.name}</strong>`);
				drawnLayerRef!.addLayer(polygon as any);

				// optional: add a marker at centroid
				const centroid = calculateCentroid(latLngs);
				if (centroid) {
					const marker = L.circleMarker(centroid, {
						radius: 6,
						color: "#d32f2f",
						fillColor: "#d32f2f",
						fillOpacity: 0.9,
					});
					marker.bindPopup(`<strong>${m.name}</strong>`);
					drawnLayerRef!.addLayer(marker as any);
				}
			}
		} catch (err) {
			console.error("Error drawing municipality", m.name, err);
		}
	});

	// Add the feature group to the map
	if (drawnLayerRef && map) {
		drawnLayerRef.addTo(map as any);
		// Fit bounds to drawn layer
		try {
			const bounds = drawnLayerRef.getBounds();
			if (bounds && bounds.isValid()) {
				map.fitBounds(bounds.pad(0.2));
			}
		} catch (err) {
			console.warn("Could not fit map bounds to drawn layer:", err);
		}
	}
};

/**
 * Berechnet einen einfachen Schwerpunkt (Centroid) eines Rings von LatLngs
 */
const calculateCentroid = (latLngs: [number, number][]) => {
	if (!latLngs || latLngs.length === 0) return null;
	let x = 0;
	let y = 0;
	latLngs.forEach((p) => {
		x += p[0];
		y += p[1];
	});
	return [x / latLngs.length, y / latLngs.length] as [number, number];
};

const onMapReady = (map: LeafletMap) => {
	console.log("L keys:", Object.keys(L));
	console.log("heatLayer in L:", (L as any).heatLayer);
	mapRef.value = map;

	// Add scale control
	L.control.scale({ position: "bottomleft", imperial: false }).addTo(map);

	// Create a new pane for heat layer
	if (!map.getPane("heatPane")) {
		map.createPane("heatPane");
	}

	const addHeatLayer = () => {
		// Clear existing heat layer
		if (heatLayerRef) {
			map.removeLayer(heatLayerRef);
			heatLayerRef = null;
		}
		const radius = calculateRadiusFromZoom(map.getZoom());
		const heat = (L as any).heatLayer(heatPoints.value, {
			radius,
			gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
			minOpacity: 0.8,
			pane: "heatPane",
		});
		heat.addTo(map);
		heatLayerRef = heat;
	};
	// Update radius when zoom level changes
	// map.on("zoomend", () => {
	// 	zoomRef.value = map.getZoom();
	// 	console.log("Zoom value:", zoomRef.value);
	// 	addHeatLayer();
	// });
};

/**
 * Zeichnet Heat-Punkte auf der Karte. Entfernt zuerst alte Punkte.
 * @param points Array von [latitude, longitude] Paaren
 */
const drawHeatPoints = (points: [number, number][]) => {
	if (!mapRef.value) {
		console.warn("Map not ready for heat points");
		return;
	}
	const map = mapRef.value;

	// Clear previous heat layer
	if (heatLayerRef) {
		try {
			map.removeLayer(heatLayerRef);
		} catch (e) {
			console.warn("Failed to remove old heat layer:", e);
		}
		heatLayerRef = null;
	}

	// Ensure heat pane exists
	if (!map.getPane("heatPane")) {
		map.createPane("heatPane");
	}

	// Convert [lat, lon] pairs to heat layer format [lat, lon, intensity]
	const heatData = points.map((p) => [p[0], p[1], 0.8] as [number, number, number]);
	console.log("Creating heat layer with", heatData.length, "points");

	try {
		const radius = calculateRadiusFromZoom(map.getZoom());
		const heat = (L as any).heatLayer(heatData, {
			radius,
			gradient: { 0.4: "blue", 0.55: "yellow", 1: "red" },
			minOpacity: 0.8,
			pane: "heatPane",
		});
		heat.addTo(map);
		heatLayerRef = heat;
		console.log("Heat layer added successfully");
	} catch (err) {
		console.error("Failed to draw heat points:", err);
	}
};

/**
 * Adds a POI marker to the map
 * @param poi The Point object containing id, lat, lon, and color
 */
const addPoiMarker = (poi: Point) => {
	if (!mapRef.value) {
		console.warn("Map not ready for POI marker");
		return;
	}

	// Create custom icon with the POI color
	const icon = L.divIcon({
		className: "poi-marker-icon",
		html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${poi.color}" width="32" height="32">
			<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
		</svg>`,
		iconSize: [32, 32],
		iconAnchor: [16, 32],
	});

	const marker = L.marker([poi.lat, poi.lon], { icon });
	marker.addTo(mapRef.value as any);
	poiMarkersMap.set(poi.id, marker);
};

/**
 * Removes a POI marker from the map
 * @param poiId The id of the POI to remove
 */
const removePoiMarker = (poiId: string) => {
	const marker = poiMarkersMap.get(poiId);
	if (marker && mapRef.value) {
		mapRef.value.removeLayer(marker);
		poiMarkersMap.delete(poiId);
	}
};

/**
 * Handles map click events and emits the coordinates
 */
const handleMapClick = (event: LeafletMouseEvent) => {
	emit("map-click", event.latlng.lat, event.latlng.lng);
};

// Expose methods so external code can call map.clearPoints() / map.drawMunicipalities(...) / map.drawHeatPoints(...) / POI methods
defineExpose({ clearPoints, drawMunicipalities, drawHeatPoints, addPoiMarker, removePoiMarker });
</script>

<template>
	<l-map
		ref="mapRef"
		:center="[48.2087334, 16.3736765]"
		:maxZoom="16"
		:minZoom="8"
		:use-global-leaflet="true"
		:zoom="zoomRef"
		style="height: 100vh; width: 100vw"
		@click="handleMapClick"
		@ready="onMapReady"
	>
		<l-tile-layer attribution="© OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

		<l-control position="bottomleft">
			<div style="background: white; padding: 5px; border-radius: 4px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.2)">
				<div ref="scaleRef"></div>
			</div>
		</l-control>

		<l-geo-json v-if="geoJson.features.length > 0" :geojson="geoJson" />
	</l-map>
</template>

<style scoped>
/* nothing needed */
</style>
