package sak.sample.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.exception.ActivityNotFoundException;
import sak.sample.itinerary.exception.TripNotFoundException;
import sak.sample.itinerary.repository.ActivityRepository;
import sak.sample.itinerary.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

  @Mock private TripRepository tripRepository;
  @Mock private ActivityRepository activityRepository;
  @InjectMocks private TripService service;

  @Test
  void create_正常系() {
    Trip saved = trip(1L);
    when(tripRepository.save(any(Trip.class))).thenReturn(saved);

    Trip result = service.create("京都", "京都", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

    assertThat(result.getId()).isEqualTo(1L);
    verify(tripRepository).save(any(Trip.class));
  }

  @Test
  void create_endDate_が_startDate_より前なら例外() {
    assertThatThrownBy(
            () -> service.create("x", "y", LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById_存在しないなら_TripNotFoundException() {
    when(tripRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(TripNotFoundException.class);
  }

  @Test
  void findAll_リポジトリから取得() {
    when(tripRepository.findAll()).thenReturn(List.of(trip(1L), trip(2L)));

    List<Trip> result = service.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void update_正常系() {
    Trip existing = trip(1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));

    Trip result = service.update(1L, "新", "東京", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));

    assertThat(result.getTitle()).isEqualTo("新");
    assertThat(result.getDestination()).isEqualTo("東京");
  }

  @Test
  void update_endDate_が_startDate_より前なら例外() {
    assertThatThrownBy(
            () -> service.update(1L, "x", "y", LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void delete_正常系() {
    Trip existing = trip(1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));

    service.delete(1L);

    verify(tripRepository).delete(existing);
  }

  @Test
  void addActivity_正常系() {
    Trip existing = trip(1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));
    Activity saved = new Activity();
    saved.setId(10L);
    when(activityRepository.save(any(Activity.class))).thenReturn(saved);

    Activity result = service.addActivity(1L, LocalDate.of(2026, 5, 2), null, "清水寺", null, null);

    assertThat(result.getId()).isEqualTo(10L);
  }

  @Test
  void addActivity_Trip_期間外の日付なら例外() {
    Trip existing = trip(1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () -> service.addActivity(1L, LocalDate.of(2026, 4, 30), null, "x", null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void updateActivity_正常系() {
    Trip existing = trip(1L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));
    Activity activity = activity(10L, existing);
    when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

    Activity result =
        service.updateActivity(1L, 10L, LocalDate.of(2026, 5, 2), null, "新", null, null);

    assertThat(result.getTitle()).isEqualTo("新");
  }

  @Test
  void updateActivity_所属_Trip_不一致なら例外() {
    Trip trip1 = trip(1L);
    Trip trip2 = trip(2L);
    when(tripRepository.findById(1L)).thenReturn(Optional.of(trip1));
    Activity activity = activity(10L, trip2);
    when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

    assertThatThrownBy(
            () -> service.updateActivity(1L, 10L, LocalDate.of(2026, 5, 2), null, "x", null, null))
        .isInstanceOf(ActivityNotFoundException.class);
  }

  @Test
  void deleteActivity_正常系() {
    Activity activity = activity(10L, trip(1L));
    when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

    service.deleteActivity(1L, 10L);

    verify(activityRepository).delete(activity);
  }

  @Test
  void deleteActivity_Activity_存在しないなら例外() {
    when(activityRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteActivity(1L, 99L))
        .isInstanceOf(ActivityNotFoundException.class);
  }

  private static Trip trip(final Long id) {
    Trip t = new Trip();
    t.setId(id);
    t.setTitle("旅");
    t.setDestination("地");
    t.setStartDate(LocalDate.of(2026, 5, 1));
    t.setEndDate(LocalDate.of(2026, 5, 3));
    return t;
  }

  private static Activity activity(final Long id, final Trip trip) {
    Activity a = new Activity();
    a.setId(id);
    a.setTrip(trip);
    a.setDate(LocalDate.of(2026, 5, 2));
    a.setTitle("活動");
    return a;
  }
}
