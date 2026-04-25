package sak.sample.itinerary.ai.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sak.sample.itinerary.ai.dto.SuggestedActivity;
import sak.sample.itinerary.ai.exception.SuggestFailedException;
import sak.sample.itinerary.ai.tool.WeatherTool;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.service.TripService;

/** 天気を考慮した Activity を生成して登録する。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItinerarySuggestionService {

  private final ChatClient.Builder chatClientBuilder;
  private final WeatherTool weatherTool;
  private final TripService tripService;

  @Transactional
  public Activity suggest(final Long tripId, final LocalDate date) {
    log.info("AI 提案受付: tripId={}, date={}", tripId, date);
    Trip trip = tripService.findById(tripId);
    if (date.isBefore(trip.getStartDate()) || date.isAfter(trip.getEndDate())) {
      throw new IllegalArgumentException("date は Trip の期間内である必要があります");
    }

    SuggestedActivity suggested;
    try {
      suggested =
          chatClientBuilder
              .build()
              .prompt()
              .tools(weatherTool)
              .user(
                  String.format(
                      "%s への %s の旅程に追加する Activity を 1 つ提案してください。"
                          + "天気を tool で取得し、それを考慮した内容にしてください。",
                      trip.getDestination(), date))
              .call()
              .entity(SuggestedActivity.class);
    } catch (RuntimeException ex) {
      throw new SuggestFailedException("AI 提案の取得に失敗しました", ex);
    }
    if (suggested == null) {
      throw new SuggestFailedException("AI 提案レスポンスが空でした", null);
    }
    log.info("AI 提案受信: title={}", suggested.title());

    Activity saved =
        tripService.addActivity(
            tripId,
            suggested.date() != null ? suggested.date() : date,
            suggested.time(),
            suggested.title(),
            suggested.location(),
            suggested.note());
    log.info("AI 提案永続化: activityId={}", saved.getId());
    return saved;
  }
}
