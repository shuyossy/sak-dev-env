package sak.sample.itinerary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sak.sample.itinerary.domain.Trip;

/**
 * Trip エンティティのリポジトリ。 一覧 / 詳細では Activity を一緒に取得するため @EntityGraph で fetch ヒントを指定し、
 * spring.jpa.open-in-view=false の構成下でも LazyInitializationException が起きないようにする。
 */
public interface TripRepository extends JpaRepository<Trip, Long> {

  @Override
  @EntityGraph(attributePaths = {"activities"})
  List<Trip> findAll();

  @Override
  @EntityGraph(attributePaths = {"activities"})
  Optional<Trip> findById(Long id);
}
