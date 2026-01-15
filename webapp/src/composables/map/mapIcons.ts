import L from "leaflet";

/**
 * Factory for creating custom Leaflet icons
 */
export const MapIcons = {
	/**
	 * Creates a POI marker icon with custom color
	 */
	createPoiIcon(color: string): L.DivIcon {
		return L.divIcon({
			className: "poi-marker-icon",
			html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${color}" width="32" height="32">
				<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
			</svg>`,
			iconSize: [32, 32],
			iconAnchor: [16, 32],
		});
	},

	/**
	 * Creates a bus/station marker icon
	 */
	createBusIcon(): L.DivIcon {
		return L.divIcon({
			className: "station-marker-icon",
			html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#4CAF50" width="28" height="28">
				<path d="M4 16c0 .88.39 1.67 1 2.22V20c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h8v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4v10zm3.5 1c-.83 0-1.5-.67-1.5-1.5S6.67 14 7.5 14s1.5.67 1.5 1.5S8.33 17 7.5 17zm9 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm1.5-6H6V6h12v5z"/>
			</svg>`,
			iconSize: [28, 28],
			iconAnchor: [14, 28],
			popupAnchor: [0, -28],
		});
	},

	/**
	 * Creates a transport search marker icon
	 */
	createTransportIcon(): L.DivIcon {
		return L.divIcon({
			className: "transport-marker-icon",
			html: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FF5722" width="36" height="36">
				<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 2c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm0 10c-1.67 0-3-1.33-3-3h2c0 .55.45 1 1 1s1-.45 1-1h2c0 1.67-1.33 3-3 3z"/>
			</svg>`,
			iconSize: [36, 36],
			iconAnchor: [18, 36],
			popupAnchor: [0, -36],
		});
	},
};
