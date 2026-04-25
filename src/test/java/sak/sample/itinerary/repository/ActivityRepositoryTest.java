package sak.sample.itinerary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import sak.sample.common.JpaConfig;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class ActivityRepositoryTest {

  @Autowired private TripRepository tripRepository;
  @Autowired private ActivityRepository activityRepository;

  @Test
  void Activity_を_Trip_と関連付けて保存できる() {
    Trip trip = new Trip();
    trip.setTitle("京都2泊3日");
    trip.setDestination("京都");
    trip.setStartDate(LocalDate.of(2026, 5, 1));
    trip.setEndDate(LocalDate.of(2026, 5, 3));
    Trip savedTrip = tripRepository.save(trip);

    Activity activity = new Activity();
    activity.setTrip(savedTrip);
    activity.setDate(LocalDate.of(2026, 5, 1));
    activity.setTime(LocalTime.of(9, 0));
    activity.setTitle("清水寺");

    Activity saved = activityRepository.save(activity);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTrip().getId()).isEqualTo(savedTrip.getId());
  }
}
