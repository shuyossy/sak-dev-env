package sak.sample.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sak.sample.itinerary.domain.Activity;

/** Activity エンティティのリポジトリ。 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {}
