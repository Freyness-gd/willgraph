// src/types/GeoJson.ts
import type { PolygonGeometry } from "src/types/PolygonGeometry";

export interface MunicipalityGeoJsonFeature {
	type: "Feature";
	properties: {
		name: string;
		iso: string;
	};
	geometry: PolygonGeometry;
}

export interface MunicipalityGeoJsonFeatureCollection {
	type: "FeatureCollection";
	features: MunicipalityGeoJsonFeature[];
}
