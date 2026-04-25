package sak.sample.itinerary.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sak.sample.itinerary.api.dto.TripForm;
import sak.sample.itinerary.api.dto.TripResponse;
import sak.sample.itinerary.service.TripService;

/** Trip リソースの REST API。 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripApiController {

  private final TripService service;

  @GetMapping
  public List<TripResponse> list() {
    return service.findAll().stream().map(TripResponse::from).toList();
  }

  @GetMapping("/{id}")
  public TripResponse get(@PathVariable final Long id) {
    return TripResponse.from(service.findById(id));
  }

  @PostMapping
  public ResponseEntity<TripResponse> create(@Valid @RequestBody final TripForm form) {
    TripResponse response =
        TripResponse.from(
            service.create(
                form.getTitle(), form.getDestination(), form.getStartDate(), form.getEndDate()));
    return ResponseEntity.created(URI.create("/api/trips/" + response.id())).body(response);
  }

  @PutMapping("/{id}")
  public TripResponse update(@PathVariable final Long id, @Valid @RequestBody final TripForm form) {
    return TripResponse.from(
        service.update(
            id, form.getTitle(), form.getDestination(), form.getStartDate(), form.getEndDate()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable final Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
