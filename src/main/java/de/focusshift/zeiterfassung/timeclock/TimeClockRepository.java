package de.focusshift.zeiterfassung.timeclock;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

interface TimeClockRepository extends CrudRepository<TimeClockEntity, Long> {

    Optional<TimeClockEntity> findByOwnerAndStoppedAtIsNull(String owner);

    List<TimeClockEntity> findAllByOwnerOrderByIdAsc(String owner);
}
