package com.beingadish.AroundU.Repository.Analytics;

import com.beingadish.AroundU.Entities.AggregatedMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedMetricsRepository extends JpaRepository<AggregatedMetrics, Long> {

    Optional<AggregatedMetrics> findByMetricDate(LocalDate date);

    List<AggregatedMetrics> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate start, LocalDate end);

    boolean existsByMetricDate(LocalDate date);
}
