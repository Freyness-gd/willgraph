package at.ac.tuwien.mogda.willgraph.service.impl;

import at.ac.tuwien.mogda.willgraph.controller.dto.StationDistanceDto;
import at.ac.tuwien.mogda.willgraph.repository.TransportRepository;
import at.ac.tuwien.mogda.willgraph.service.TransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransportServiceImpl implements TransportService {
    private final TransportRepository transportRepository;


    @Override
    public List<StationDistanceDto> findStationsByLocation(double lat, double lng, double v) {
        return this.transportRepository.findStationsByLocation(lat, lng, v);
    }
}
