package de.focusshift.zeiterfassung.timeentry;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface TimeEntryRepository extends CrudRepository<TimeEntryEntity, Long>, RevisionRepository<TimeEntryEntity, Long, Long> {

    long countAllByOwner(String owner);

    List<TimeEntryEntity> findAllByStartGreaterThanEqualAndStartLessThanOrderByStart(Instant start, Instant endExclusive);

    List<TimeEntryEntity> findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStart(String owner, Instant start, Instant endExclusive);

    List<TimeEntryEntity> findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThanOrderByStart(List<String> owners, Instant start, Instant endExclusive);
}
