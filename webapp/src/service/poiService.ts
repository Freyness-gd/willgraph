import axios from "axios";
import type {PoiDistanceDto} from "src/types/Poi";
import {TransportPathDto} from "src/types/TransportPath";

const poiService = {
  /**
   * Fetches nearby POIs (amenities) from backend API.
   * Makes GET request to http://localhost:8080/api/pois/nearby?lat={lat}&lng={lng}&radius={radius}
   */
  async findPoIsNearby(lat: number, lng: number, radius: number = 1000): Promise<PoiDistanceDto[]> {
    console.log("findPoIsNearby called for lat:", lat, "lng:", lng, "radius:", radius);

    try {
      const response = await axios.get<PoiDistanceDto[]>("http://localhost:8080/api/poi/nearby", {
        params: {
          lat,
          lng,
          radius,
        },
      });

      console.log("POI API Response:", response.data);
      return response.data;
    } catch (error) {
      console.error("Error fetching nearby POIs:", error);
      return [];
    }
  },

  async getTransportPath(fromLat: number, fromLon: number, toLat: number, toLon: number, maxWalkDistance: number = 1000): Promise<TransportPathDto | null> {
    console.log("getTransportPath called for fromLat:", fromLat, "fromLon:", fromLon, "toLat:", toLat, "toLon:", toLon, "maxWalkDistance:", maxWalkDistance);
    try {
      const response = await axios.get<TransportPathDto>("http://localhost:8080/api/poi/transport-path", {
        params: {
          fromLat,
          fromLon,
          toLat,
          toLon,
          maxWalkDistance,
        },
      });

      console.log("POI API Response:", response.data);
      return response.data;
    } catch (error) {
      console.error("Error fetching transport path for POI:", error);
      return null;
    }
  }
};

export default poiService;
