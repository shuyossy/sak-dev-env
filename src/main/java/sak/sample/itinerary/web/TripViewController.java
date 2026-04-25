package sak.sample.itinerary.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sak.sample.itinerary.api.dto.TripResponse;
import sak.sample.itinerary.service.TripService;

/** 旅程の Thymeleaf 画面。 */
@Controller
@RequiredArgsConstructor
public class TripViewController {

  private final TripService service;

  @GetMapping({"/", "/trips"})
  public String list(final Model model) {
    model.addAttribute("trips", service.findAll().stream().map(TripResponse::from).toList());
    return "trips/list";
  }

  @GetMapping("/trips/{id}")
  public String detail(@PathVariable final Long id, final Model model) {
    model.addAttribute("trip", TripResponse.from(service.findById(id)));
    return "trips/detail";
  }
}
