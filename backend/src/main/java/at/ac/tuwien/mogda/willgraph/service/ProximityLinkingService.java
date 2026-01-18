package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.repository.AddressRepository;
import at.ac.tuwien.mogda.willgraph.repository.TransportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProximityLinkingService {
    private final AddressRepository addressRepository;
    private final TransportRepository transportRepository;
    private final Neo4jClient neo4jClient;

    @Async
    public void waitForDataAndLink() {
        log.info("Background task started: Waiting for Transport Import to complete...");

        try {
            waitForTransportImportCompletion();

            log.info("Transport data detected. Generating proximity links...");
            addressRepository.generateAllProximityLinks();
            transportRepository.createSpatialIndex();
            generateWalkEdgesSafely();
            log.info("Proximity links generated successfully.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Linking process interrupted", e);
        }
    }

  public void generateWalkEdgesSafely() {
        List<Long> allIds = transportRepository.getAllTransportIds();
        int batchSize = 500;
        for (int i = 0; i < allIds.size(); i += batchSize) {
            int end = Math.min(allIds.size(), i + batchSize);
            List<Long> batch = allIds.subList(i, end);
            transportRepository.generateWalkEdgesForBatch(batch);
            log.info("Processed walk links for stations {} to {}", i, end);
        }
    }

    private void waitForTransportImportCompletion() throws InterruptedException {
        while (!isImportComplete()) {
            Thread.sleep(3000);
        }
    }

  public boolean isImportComplete() {
        return neo4jClient.query("MATCH (s:SystemState {type: 'transport_import', status: 'COMPLETED'}) RETURN count(s) > 0")
            .fetchAs(Boolean.class)
            .one()
            .orElse(false);
    }
}
