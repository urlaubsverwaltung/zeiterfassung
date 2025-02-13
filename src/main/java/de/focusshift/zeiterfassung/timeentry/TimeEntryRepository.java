package de.focusshift.zeiterfassung.timeentry;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface TimeEntryRepository extends CrudRepository<TimeEntryEntity, Long>, RevisionRepository<TimeEntryEntity, Long, Long> {

    long countAllByOwner(String owner);

    List<TimeEntryEntity> findAllByStartGreaterThanEqualAndStartLessThan(Instant start, Instant endExclusive);

    List<TimeEntryEntity> findAllByOwnerAndStartGreaterThanEqualAndStartLessThan(String owner, Instant start, Instant endExclusive);

    List<TimeEntryEntity> findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThan(List<String> owners, Instant start, Instant endExclusive);
}
