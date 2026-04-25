package sak.sample.itinerary.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

@WebMvcTest(TripViewController.class)
class TripViewControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private TripService service;

  @Test
  void 一覧画面が描画される() throws Exception {
    when(service.findAll()).thenReturn(List.of(trip(1L)));
    mvc.perform(get("/trips")).andExpect(status().isOk()).andExpect(view().name("trips/list"));
  }

  @Test
  void 詳細画面が描画される() throws Exception {
    when(service.findById(1L)).thenReturn(trip(1L));
    mvc.perform(get("/trips/1")).andExpect(status().isOk()).andExpect(view().name("trips/detail"));
  }

  private static Trip trip(final Long id) {
    Trip t = new Trip();
    t.setId(id);
    t.setTitle("旅");
    t.setDestination("京都");
    t.setStartDate(LocalDate.of(2026, 5, 1));
    t.setEndDate(LocalDate.of(2026, 5, 3));
    return t;
  }
}
