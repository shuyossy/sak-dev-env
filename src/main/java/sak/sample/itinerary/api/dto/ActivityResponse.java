package sak.sample.itinerary.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import sak.sample.itinerary.domain.Activity;

/** Activity の API レスポンス。 */
public record ActivityResponse(
    Long id, LocalDate date, LocalTime time, String title, String location, String note) {

  public static ActivityResponse from(final Activity a) {
    return new ActivityResponse(
        a.getId(), a.getDate(), a.getTime(), a.getTitle(), a.getLocation(), a.getNote());
  }
}
