// src/mappers/municipalityMapper.ts
import type { Municipality } from "src/types/Municipality";
import type { MunicipalityFeatureCollection } from "src/types/MunicipalityGeoJson";
import type { MunicipalityGeoJsonFeatureCollection } from "src/types/GeoJson";

export function mapMunicipalities(geoJson: MunicipalityFeatureCollection): Municipality[] {
	return geoJson.features.map((feature) => ({
		name: feature.properties.name,
		iso: feature.properties.iso,
		boundary: feature.geometry,
	}));
}

export function municipalitiesToGeoJson(municipalities: Municipality[]): MunicipalityGeoJsonFeatureCollection {
	return {
		type: "FeatureCollection", // ✅ literal
		features: municipalities.map((m) => ({
			type: "Feature", // ✅ literal
			properties: {
				name: m.name,
				iso: m.iso,
			},
			geometry: m.boundary,
		})),
	};
}
