package sak.sample.itinerary.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import sak.sample.itinerary.ai.dto.SuggestedActivity;
import sak.sample.itinerary.ai.exception.SuggestFailedException;
import sak.sample.itinerary.ai.tool.WeatherTool;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

/** ChatClient.Builder のフルーエント呼び出しを deep stub でモックして ItinerarySuggestionService の主要パスを覆う。 */
@ExtendWith(MockitoExtension.class)
class ItinerarySuggestionServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ChatClient.Builder chatClientBuilder;

  @Mock private WeatherTool weatherTool;
  @Mock private TripService tripService;
  @InjectMocks private ItinerarySuggestionService service;

  @Test
  void suggest_Trip_期間外の日付なら_IllegalArgumentException() {
    when(tripService.findById(1L)).thenReturn(trip());

    assertThatThrownBy(() -> service.suggest(1L, LocalDate.of(2026, 4, 30)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void suggest_正常系_AI提案を_addActivity_に渡す() {
    when(tripService.findById(1L)).thenReturn(trip());
    SuggestedActivity suggested =
        new SuggestedActivity(
            LocalDate.of(2026, 5, 2), LocalTime.of(10, 0), "金閣寺", "京都市", "曇りでも屋外可", "曇り");
    when(chatClientBuilder
            .build()
            .prompt()
            .tools(any(Object[].class))
            .user(anyString())
            .call()
            .entity(SuggestedActivity.class))
        .thenReturn(suggested);
    Activity saved = new Activity();
    saved.setId(10L);
    when(tripService.addActivity(anyLong(), any(), any(), anyString(), anyString(), anyString()))
        .thenReturn(saved);

    Activity result = service.suggest(1L, LocalDate.of(2026, 5, 2));

    assertThat(result.getId()).isEqualTo(10L);
    verify(tripService)
        .addActivity(1L, LocalDate.of(2026, 5, 2), LocalTime.of(10, 0), "金閣寺", "京都市", "曇りでも屋外可");
  }

  @Test
  void suggest_LLM_例外時に_SuggestFailedException() {
    when(tripService.findById(1L)).thenReturn(trip());
    when(chatClientBuilder
            .build()
            .prompt()
            .tools(any(Object[].class))
            .user(anyString())
            .call()
            .entity(SuggestedActivity.class))
        .thenThrow(new RuntimeException("openai down"));

    assertThatThrownBy(() -> service.suggest(1L, LocalDate.of(2026, 5, 2)))
        .isInstanceOf(SuggestFailedException.class);
  }

  @Test
  void suggest_LLM_応答が_null_なら_SuggestFailedException() {
    when(tripService.findById(1L)).thenReturn(trip());
    when(chatClientBuilder
            .build()
            .prompt()
            .tools(any(Object[].class))
            .user(anyString())
            .call()
            .entity(SuggestedActivity.class))
        .thenReturn(null);

    assertThatThrownBy(() -> service.suggest(1L, LocalDate.of(2026, 5, 2)))
        .isInstanceOf(SuggestFailedException.class);
  }

  @Test
  void suggest_AI_が_date_を返さない場合は_リクエスト日付を_fallback_として使う() {
    when(tripService.findById(1L)).thenReturn(trip());
    SuggestedActivity suggested =
        new SuggestedActivity(null, LocalTime.of(10, 0), "金閣寺", "京都市", "曇りでも屋外可", "曇り");
    when(chatClientBuilder
            .build()
            .prompt()
            .tools(any(Object[].class))
            .user(anyString())
            .call()
            .entity(SuggestedActivity.class))
        .thenReturn(suggested);
    Activity saved = new Activity();
    saved.setId(10L);
    when(tripService.addActivity(anyLong(), any(), any(), anyString(), anyString(), anyString()))
        .thenReturn(saved);

    service.suggest(1L, LocalDate.of(2026, 5, 2));

    verify(tripService)
        .addActivity(1L, LocalDate.of(2026, 5, 2), LocalTime.of(10, 0), "金閣寺", "京都市", "曇りでも屋外可");
  }

  // -- helpers --

  private static Trip trip() {
    Trip trip = new Trip();
    trip.setId(1L);
    trip.setStartDate(LocalDate.of(2026, 5, 1));
    trip.setEndDate(LocalDate.of(2026, 5, 3));
    trip.setDestination("京都");
    return trip;
  }
}
