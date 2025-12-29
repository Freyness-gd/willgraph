// src/types/MunicipalityGeoJson.ts
import type { PolygonGeometry } from "src/types/PolygonGeometry";

export interface MunicipalityProperties {
	name: string;
	iso: string;
}

export interface MunicipalityFeature {
	type: "Feature";
	properties: MunicipalityProperties;
	geometry: PolygonGeometry;
}

export interface MunicipalityFeatureCollection {
	type: "FeatureCollection";
	features: MunicipalityFeature[];
}
