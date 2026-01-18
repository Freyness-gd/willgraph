export type Position = [number, number]; // [lng, lat]

export interface PolygonGeometry {
	type: "Polygon";
	coordinates: Position[][][];
}
