package at.ac.tuwien.mogda.willgraph.service;

import at.ac.tuwien.mogda.willgraph.controller.dto.OverpassResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Service for querying the Overpass API to fetch amenities within a bounding box.
 * Uses Overpass QL (Query Language) to retrieve OSM amenity data.
 */
@Service
@Slf4j
public class OverpassApiService {
  /**
   * Maximum number of retry attempts for Overpass API requests
   */
  private static final int MAX_RETRY_ATTEMPTS = 10;

  /**
   * Maximum buffer size for Overpass API responses (16MB)
   */
  private static final int MAX_BUFFER_SIZE = 16 * 1024 * 1024;

  /**
   * Student/early-adult focused amenity types to fetch from OSM
   */
  private static final Set<String> STUDENT_AMENITIES = Set.of(
      "pub", "bar", "cafe", "restaurant", "fast_food",
      "gym", "fitness_center", "swimming_pool",
      "library", "university",
      "cinema", "theatre", "nightclub",
      "pharmacy", "doctors", "dentist",
      "supermarket", "bakery", "butcher",
      "laundry", "dry_cleaning",
      "bicycle_rental", "car_rental",
      "parking", "fuel"
  );

  /**
   * WebClient configured with increased buffer size for large Overpass API responses
   */
  private final WebClient webClient = WebClient.builder()
      .baseUrl("https://overpass-api.de/api")
      .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
      .build();

  public Mono<OverpassResponse> query(Double minLat, Double minLon, Double maxLat, Double maxLon) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("data", buildOverpassQuery(minLat, minLon, maxLat, maxLon));

    return webClient.post()
        .uri("/interpreter")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(form)
        .retrieve()
        .bodyToMono(OverpassResponse.class)
        .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(2))
            .maxBackoff(Duration.ofMinutes(2))
            .filter(throwable -> {
              if (throwable instanceof WebClientResponseException ex) {
                final HttpStatus status = (HttpStatus) ex.getStatusCode();
                final boolean shouldRetry = status == HttpStatus.GATEWAY_TIMEOUT
                    || status == HttpStatus.SERVICE_UNAVAILABLE
                    || status == HttpStatus.TOO_MANY_REQUESTS;
                if (shouldRetry) {
                  log.warn("Overpass API request failed with status {}, retrying...", status);
                }
                return shouldRetry;
              }
              return false;
            })
            .doBeforeRetry(retrySignal ->
                log.info("Retrying Overpass API request (attempt {}/{})",
                    retrySignal.totalRetries() + 1, MAX_RETRY_ATTEMPTS))
            .onRetryExhaustedThrow((_spec, retrySignal) -> {
              log.error("Exhausted all retry attempts for Overpass API request");
              return retrySignal.failure();
            })
        );
  }


  /**
   * Build an Overpass QL query for all student amenities
   */
  private String buildOverpassQuery(Double minLat, Double minLon, Double maxLat, Double maxLon) {
    // Bbox format for Overpass: (south, west, north, east)
    String bbox = String.format(Locale.US, "(%.6f,%.6f,%.6f,%.6f)", minLat, minLon, maxLat, maxLon);

    StringBuilder query = new StringBuilder();
    query.append("[out:json];(");

    for (String amenity : STUDENT_AMENITIES) {
      query.append(String.format(Locale.US, "node[\"amenity\"=\"%s\"]%s;", amenity, bbox));
    }

    query.append(");out;");
    return query.toString();
  }

  /**
   * Get all supported student amenity types
   */
  public Set<String> getSupportedAmenityTypes() {
    return new HashSet<>(STUDENT_AMENITIES);
  }

  /**
   * Data class for amenity points
   */
  public record AmenityPoint(Long osmId, String name, String amenityType, Double latitude, Double longitude) {
  }
}
