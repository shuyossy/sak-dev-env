package sak.sample.itinerary.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import sak.sample.itinerary.ai.tool.WeatherTool;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

/**
 * 期間外日付の検証経路を pure unit test で検証。Happy path（実際の ChatClient 経由）は Phase 13 の SpringBootTest + mock
 * プロファイルで覆う設計。
 */
@ExtendWith(MockitoExtension.class)
class ItinerarySuggestionServiceTest {

  @Mock private ChatClient.Builder chatClientBuilder;
  @Mock private WeatherTool weatherTool;
  @Mock private TripService tripService;
  @InjectMocks private ItinerarySuggestionService service;

  @Test
  void suggest_Trip_期間外の日付なら_IllegalArgumentException() {
    Trip trip = new Trip();
    trip.setId(1L);
    trip.setStartDate(LocalDate.of(2026, 5, 1));
    trip.setEndDate(LocalDate.of(2026, 5, 3));
    when(tripService.findById(1L)).thenReturn(trip);

    assertThatThrownBy(() -> service.suggest(1L, LocalDate.of(2026, 4, 30)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
