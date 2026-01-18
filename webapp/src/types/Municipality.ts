import type { PolygonGeometry } from "src/types/PolygonGeometry";

export interface Municipality {
	name: string;
	iso: string;
	boundary: PolygonGeometry;
}
