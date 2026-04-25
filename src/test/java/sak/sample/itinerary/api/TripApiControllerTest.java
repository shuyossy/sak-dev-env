package sak.sample.itinerary.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sak.sample.common.GlobalExceptionHandler;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

@WebMvcTest(TripApiController.class)
@Import(GlobalExceptionHandler.class)
class TripApiControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private TripService service;

  @Test
  void list_OK() throws Exception {
    when(service.findAll()).thenReturn(List.of(trip(1L)));
    mvc.perform(get("/api/trips"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void create_201() throws Exception {
    when(service.create(anyString(), anyString(), any(), any())).thenReturn(trip(1L));
    mvc.perform(
            post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
{"title":"旅","destination":"京都","startDate":"2026-05-01","endDate":"2026-05-03"}
"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void create_バリデーション失敗で_400() throws Exception {
    mvc.perform(
            post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"","destination":"","startDate":null,"endDate":null}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void delete_204() throws Exception {
    mvc.perform(delete("/api/trips/1")).andExpect(status().isNoContent());
    verify(service).delete(1L);
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
