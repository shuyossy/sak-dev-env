package sak.sample.itinerary.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sak.sample.common.GlobalExceptionHandler;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

@WebMvcTest(ActivityApiController.class)
@Import(GlobalExceptionHandler.class)
class ActivityApiControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private TripService service;

  @Test
  void create_201() throws Exception {
    when(service.addActivity(anyLong(), any(), any(), anyString(), any(), any()))
        .thenReturn(activity(10L));
    mvc.perform(
            post("/api/trips/1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-01","time":"09:00","title":"清水寺"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10));
  }

  @Test
  void create_バリデーション失敗で_400() throws Exception {
    mvc.perform(
            post("/api/trips/1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":null,"title":""}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_200() throws Exception {
    when(service.updateActivity(anyLong(), anyLong(), any(), any(), anyString(), any(), any()))
        .thenReturn(activity(10L));
    mvc.perform(
            put("/api/trips/1/activities/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-01","title":"金閣寺"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10));
  }

  @Test
  void delete_204() throws Exception {
    mvc.perform(delete("/api/trips/1/activities/10")).andExpect(status().isNoContent());
    verify(service).deleteActivity(1L, 10L);
  }

  private static Activity activity(final Long id) {
    Activity a = new Activity();
    a.setId(id);
    Trip t = new Trip();
    t.setId(1L);
    a.setTrip(t);
    a.setDate(LocalDate.of(2026, 5, 1));
    a.setTitle("活動");
    return a;
  }
}
