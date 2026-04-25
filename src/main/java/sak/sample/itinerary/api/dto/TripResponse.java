package sak.sample.itinerary.api.dto;

import java.time.LocalDate;
import java.util.List;
import sak.sample.itinerary.domain.Trip;

/** Trip の API レスポンス。 */
public record TripResponse(
    Long id,
    String title,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    List<ActivityResponse> activities) {

  public static TripResponse from(final Trip trip) {
    List<ActivityResponse> activities =
        trip.getActivities().stream().map(ActivityResponse::from).toList();
    return new TripResponse(
        trip.getId(),
        trip.getTitle(),
        trip.getDestination(),
        trip.getStartDate(),
        trip.getEndDate(),
        activities);
  }
}
