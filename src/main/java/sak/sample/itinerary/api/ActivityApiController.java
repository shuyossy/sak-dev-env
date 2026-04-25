package sak.sample.itinerary.api;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sak.sample.itinerary.api.dto.ActivityForm;
import sak.sample.itinerary.api.dto.ActivityResponse;
import sak.sample.itinerary.service.TripService;

/** Activity リソースの REST API（Trip サブリソース）。 */
@RestController
@RequestMapping("/api/trips/{tripId}/activities")
@RequiredArgsConstructor
public class ActivityApiController {

  private final TripService service;

  @PostMapping
  public ResponseEntity<ActivityResponse> create(
      @PathVariable final Long tripId, @Valid @RequestBody final ActivityForm form) {
    ActivityResponse response =
        ActivityResponse.from(
            service.addActivity(
                tripId,
                form.getDate(),
                form.getTime(),
                form.getTitle(),
                form.getLocation(),
                form.getNote()));
    return ResponseEntity.created(
            URI.create("/api/trips/" + tripId + "/activities/" + response.id()))
        .body(response);
  }

  @PutMapping("/{id}")
  public ActivityResponse update(
      @PathVariable final Long tripId,
      @PathVariable final Long id,
      @Valid @RequestBody final ActivityForm form) {
    return ActivityResponse.from(
        service.updateActivity(
            tripId,
            id,
            form.getDate(),
            form.getTime(),
            form.getTitle(),
            form.getLocation(),
            form.getNote()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable final Long tripId, @PathVariable final Long id) {
    service.deleteActivity(tripId, id);
    return ResponseEntity.noContent().build();
  }
}
