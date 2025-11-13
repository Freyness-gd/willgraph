import { boot } from "quasar/wrappers";
import "leaflet/dist/leaflet.css";
import "leaflet-defaulticon-compatibility";
import "leaflet-defaulticon-compatibility/dist/leaflet-defaulticon-compatibility.css";
import * as L from "leaflet";

export default boot(() => {
	globalThis.L = L;
});
