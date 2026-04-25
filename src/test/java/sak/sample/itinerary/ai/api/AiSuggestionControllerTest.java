package sak.sample.itinerary.ai.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import sak.sample.itinerary.ai.exception.SuggestFailedException;
import sak.sample.itinerary.ai.service.ItinerarySuggestionService;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;

@WebMvcTest(AiSuggestionController.class)
@Import(GlobalExceptionHandler.class)
class AiSuggestionControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private ItinerarySuggestionService service;

  @Test
  void suggest_201_で_Activity_を返す() throws Exception {
    when(service.suggest(anyLong(), any(LocalDate.class))).thenReturn(activity(10L));
    mvc.perform(
            post("/api/trips/1/ai/suggest-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-02"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10));
  }

  @Test
  void suggest_バリデーション失敗で_400() throws Exception {
    mvc.perform(
            post("/api/trips/1/ai/suggest-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void suggest_LLM_失敗で_502() throws Exception {
    when(service.suggest(anyLong(), any(LocalDate.class)))
        .thenThrow(new SuggestFailedException("AI 提案の取得に失敗しました", new RuntimeException("boom")));
    mvc.perform(
            post("/api/trips/1/ai/suggest-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-05-02"}
                    """))
        .andExpect(status().isBadGateway());
  }

  private static Activity activity(final Long id) {
    Activity a = new Activity();
    a.setId(id);
    Trip t = new Trip();
    t.setId(1L);
    a.setTrip(t);
    a.setDate(LocalDate.of(2026, 5, 2));
    a.setTitle("AI 提案アクティビティ");
    return a;
  }
}
