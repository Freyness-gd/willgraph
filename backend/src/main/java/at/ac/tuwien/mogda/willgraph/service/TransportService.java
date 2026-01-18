package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;

import java.util.List;

public interface TransportService {
    List<StationDistanceDto> findStationsByLocation(double lat, double lng, double v);
}
