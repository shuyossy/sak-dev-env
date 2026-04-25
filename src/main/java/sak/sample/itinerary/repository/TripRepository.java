package sak.sample.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sak.sample.itinerary.domain.Trip;

/** Trip エンティティのリポジトリ。 */
public interface TripRepository extends JpaRepository<Trip, Long> {}
