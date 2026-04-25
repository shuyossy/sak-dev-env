package sak.sample.itinerary.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sak.sample.itinerary.domain.Activity;
import sak.sample.itinerary.domain.Trip;
import sak.sample.itinerary.exception.ActivityNotFoundException;
import sak.sample.itinerary.exception.TripNotFoundException;
import sak.sample.itinerary.repository.ActivityRepository;
import sak.sample.itinerary.repository.TripRepository;

/** Trip と Activity の CRUD ロジックを集約するサービス。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

  private final TripRepository tripRepository;
  private final ActivityRepository activityRepository;

  @Transactional(readOnly = true)
  public List<Trip> findAll() {
    return tripRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Trip findById(final Long id) {
    return tripRepository.findById(id).orElseThrow(() -> new TripNotFoundException(id));
  }

  @Transactional
  public Trip create(
      final String title,
      final String destination,
      final LocalDate startDate,
      final LocalDate endDate) {
    validateDateRange(startDate, endDate);
    Trip trip = new Trip();
    trip.setTitle(title);
    trip.setDestination(destination);
    trip.setStartDate(startDate);
    trip.setEndDate(endDate);
    Trip saved = tripRepository.save(trip);
    log.info("Trip created: id={}", saved.getId());
    return saved;
  }

  @Transactional
  public Trip update(
      final Long id,
      final String title,
      final String destination,
      final LocalDate startDate,
      final LocalDate endDate) {
    validateDateRange(startDate, endDate);
    Trip trip = findById(id);
    trip.setTitle(title);
    trip.setDestination(destination);
    trip.setStartDate(startDate);
    trip.setEndDate(endDate);
    log.info("Trip updated: id={}", id);
    return trip;
  }

  @Transactional
  public void delete(final Long id) {
    Trip trip = findById(id);
    tripRepository.delete(trip);
    log.info("Trip deleted: id={}", id);
  }

  @Transactional
  public Activity addActivity(
      final Long tripId,
      final LocalDate date,
      final LocalTime time,
      final String title,
      final String location,
      final String note) {
    Trip trip = findById(tripId);
    validateActivityDate(trip, date);
    Activity activity = new Activity();
    activity.setTrip(trip);
    activity.setDate(date);
    activity.setTime(time);
    activity.setTitle(title);
    activity.setLocation(location);
    activity.setNote(note);
    Activity saved = activityRepository.save(activity);
    log.info("Activity added: tripId={}, activityId={}", tripId, saved.getId());
    return saved;
  }

  @Transactional
  public Activity updateActivity(
      final Long tripId,
      final Long activityId,
      final LocalDate date,
      final LocalTime time,
      final String title,
      final String location,
      final String note) {
    Trip trip = findById(tripId);
    validateActivityDate(trip, date);
    Activity activity = findActivity(tripId, activityId);
    activity.setDate(date);
    activity.setTime(time);
    activity.setTitle(title);
    activity.setLocation(location);
    activity.setNote(note);
    log.info("Activity updated: tripId={}, activityId={}", tripId, activityId);
    return activity;
  }

  @Transactional
  public void deleteActivity(final Long tripId, final Long activityId) {
    Activity activity = findActivity(tripId, activityId);
    activityRepository.delete(activity);
    log.info("Activity deleted: tripId={}, activityId={}", tripId, activityId);
  }

  private Activity findActivity(final Long tripId, final Long activityId) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(() -> new ActivityNotFoundException(activityId));
    if (!activity.getTrip().getId().equals(tripId)) {
      throw new ActivityNotFoundException(activityId);
    }
    return activity;
  }

  private void validateDateRange(final LocalDate startDate, final LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate は startDate 以降である必要があります");
    }
  }

  private void validateActivityDate(final Trip trip, final LocalDate date) {
    if (date.isBefore(trip.getStartDate()) || date.isAfter(trip.getEndDate())) {
      throw new IllegalArgumentException("Activity の日付は Trip の期間内である必要があります");
    }
  }
}
