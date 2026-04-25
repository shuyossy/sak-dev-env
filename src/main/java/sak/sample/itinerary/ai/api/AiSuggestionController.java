package sak.sample.itinerary.ai.api;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sak.sample.itinerary.ai.dto.SuggestRequest;
import sak.sample.itinerary.ai.service.ItinerarySuggestionService;
import sak.sample.itinerary.api.dto.ActivityResponse;

/** AI による Activity 提案・登録の REST API。 */
@RestController
@RequestMapping("/api/trips/{tripId}/ai/suggest-activities")
@RequiredArgsConstructor
public class AiSuggestionController {

  private final ItinerarySuggestionService service;

  @PostMapping
  public ResponseEntity<ActivityResponse> suggest(
      @PathVariable final Long tripId, @Valid @RequestBody final SuggestRequest req) {
    ActivityResponse response = ActivityResponse.from(service.suggest(tripId, req.getDate()));
    return ResponseEntity.created(
            URI.create("/api/trips/" + tripId + "/activities/" + response.id()))
        .body(response);
  }
}
