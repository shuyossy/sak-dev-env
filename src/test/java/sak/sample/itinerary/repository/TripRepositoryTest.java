package sak.sample.itinerary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import sak.sample.common.JpaConfig;
import sak.sample.itinerary.domain.Trip;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class TripRepositoryTest {

  @Autowired private TripRepository repository;

  @Test
  void Trip_を保存して_id_と_監査列が採番される() {
    Trip trip = new Trip();
    trip.setTitle("京都2泊3日");
    trip.setDestination("京都");
    trip.setStartDate(LocalDate.of(2026, 5, 1));
    trip.setEndDate(LocalDate.of(2026, 5, 3));

    Trip saved = repository.save(trip);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }
}
