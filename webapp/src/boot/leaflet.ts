import { boot } from "quasar/wrappers";
import "leaflet/dist/leaflet.css";
import "leaflet-defaulticon-compatibility";
import "leaflet-defaulticon-compatibility/dist/leaflet-defaulticon-compatibility.css";
import L from "leaflet";
import "leaflet.heat/dist/leaflet-heat.js";

export default boot(() => {
	globalThis.L = L;
});
