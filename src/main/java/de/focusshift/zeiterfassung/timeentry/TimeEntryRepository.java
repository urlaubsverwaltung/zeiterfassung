package de.focusshift.zeiterfassung.timeentry;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface TimeEntryRepository extends CrudRepository<TimeEntryEntity, Long> {

    long countAllByOwner(String owner);

    /**
     * Query all {@link TimeEntryEntity}'s that are touching the requested period. Means the {@link TimeEntryEntity}'s
     * start is before the period and end is after the period, or one of both is between the period.
     *
     * @param start        start of the period (inclusive)
     * @param endExclusive end of the period (exclusive)
     * @return list of every {@link TimeEntryEntity} fulfilling the criteria.
     */
    @Query(
        "select x from TimeEntryEntity x where "
            + "(x.start between ?1 and ?2) or (x.end between ?1 and ?2) or (x.start < ?1 and x.end > ?2) "
            + "order by x.start"
    )
    List<TimeEntryEntity> findAllByTouchingPeriod(Instant start, Instant endExclusive);

    /**
     * Query all {@link TimeEntryEntity}'s that are touching the requested period. Means the {@link TimeEntryEntity}'s
     * start is before the period and end is after the period, or one of both is between the period.
     *
     * @param start        start of the period (inclusive)
     * @param endExclusive end of the period (exclusive)
     * @param owner        owner of requested {@link TimeEntryEntity}'s
     * @return list of every {@link TimeEntryEntity} fulfilling the criteria.
     */
    @Query(
        "select x from TimeEntryEntity x "
            + "where x.owner = ?1 "
            + "and ((x.start between ?2 and ?3) or (x.end between ?2 and ?3) or (x.start < ?2 and x.end > ?3)) "
            + "order by x.start"
    )
    List<TimeEntryEntity> findAllByOwnerAndTouchingPeriod(String owner, Instant start, Instant endExclusive);

    @Query(
        "select x from TimeEntryEntity x "
            + "where x.owner in ?1 and ((x.start between ?2 and ?3) or (x.end between ?2 and ?3) "
            + "or (x.start < ?2 and x.end > ?3)) "
            + "order by x.start"
    )
    List<TimeEntryEntity> findAllByOwnerIsInAndTouchingPeriod(List<String> owners, Instant start, Instant endExclusive);
}
