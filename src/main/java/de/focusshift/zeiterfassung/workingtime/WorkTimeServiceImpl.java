package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class WorkTimeServiceImpl implements WorkingTimeService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final WorkingTimeRepository repository;
    private final UserManagementService userManagementService;
    private final Clock clock;

    WorkTimeServiceImpl(WorkingTimeRepository repository, UserManagementService userManagementService, Clock clock) {
        this.repository = repository;
        this.userManagementService = userManagementService;
        this.clock = clock;
    }

    @Override
    public Optional<WorkingTime> getWorkingTimeById(WorkingTimeId workingTimeId) {
        return repository.findById(workingTimeId.uuid()).map(entity -> {
            final UserLocalId userLocalId = new UserLocalId(entity.getUserId());
            final User user = findUser(userLocalId);
            final List<WorkingTimeEntity> allEntitiesSorted = findAllWorkingTimeEntitiesSorted(userLocalId);
            return entityToWorkingTime(entity, user.userIdComposite(), allEntitiesSorted);
        });
    }

    @Override
    public List<WorkingTime> getAllWorkingTimesByUser(UserLocalId userLocalId) {

        final User user = findUser(userLocalId);
        final List<WorkingTimeEntity> sortedEntities = findAllWorkingTimeEntitiesSorted(userLocalId);

        if (sortedEntities.isEmpty()) {
            return List.of(createDefaultWorkingTime(user.userIdComposite()));
        }

        return sortedEntities.stream()
            .map(entity -> entityToWorkingTime(entity, user.userIdComposite(), sortedEntities))
            .toList();
    }

    @Override
    public Map<UserIdComposite, List<WorkingTime>> getWorkingTimesByUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);
        return findWorkingTimesForUsers(from, toExclusive, users);
    }

    @Override
    public Map<UserIdComposite, List<WorkingTime>> getAllWorkingTimes(LocalDate from, LocalDate toExclusive) {
        final List<User> users = userManagementService.findAllUsers();
        return findWorkingTimesForUsers(from, toExclusive, users);
    }

    @Override
    public WorkingTime createWorkingTime(UserLocalId userLocalId, LocalDate validFrom, EnumMap<DayOfWeek, Duration> workdays) {

        final User user = findUser(userLocalId);

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(userLocalId.value());
        entity.setValidFrom(validFrom);
        setWorkDays(entity, workdays);

        final WorkingTimeEntity saved = repository.save(entity);
        final List<WorkingTimeEntity> allEntitiesSorted = findAllWorkingTimeEntitiesSorted(userLocalId);

        return entityToWorkingTime(saved, user.userIdComposite(), allEntitiesSorted);
    }

    @Override
    public WorkingTime updateWorkingTime(WorkingTimeId workingTimeId, WorkWeekUpdate workWeekUpdate) {

        final WorkingTimeEntity entity = repository.findById(workingTimeId.uuid())
            .orElseThrow(() -> new IllegalStateException("could not find working-time with id=%s".formatted(workingTimeId)));

        final LocalDate nextValidFrom = workWeekUpdate.validFrom().orElse(null);
        if (entity.getValidFrom() == null) {
            LOG.info("ignore updating validFrom of very first workingTime={} to validFrom={}", workingTimeId, nextValidFrom);
        } else if (nextValidFrom != null) {
            entity.setValidFrom(nextValidFrom);
        } else {
            throw new WorkingTimeUpdateException("cannot update WorkingTime=%s without validFrom".formatted(workingTimeId));
        }

        setWorkDays(entity, workWeekUpdate.workDays());

        final UserLocalId userLocalId = new UserLocalId(entity.getUserId());
        final User user = findUser(userLocalId);

        final WorkingTimeEntity saved = repository.save(entity);
        final List<WorkingTimeEntity> allEntitiesSorted = findAllWorkingTimeEntitiesSorted(userLocalId);

        return entityToWorkingTime(saved, user.userIdComposite(), allEntitiesSorted);
    }

    @Override
    public boolean deleteWorkingTime(WorkingTimeId workingTimeId) {

        final WorkingTimeEntity toDelete = repository.findById(workingTimeId.uuid())
            .orElseThrow(() -> new IllegalStateException("could not find WorkingTime=" + workingTimeId));

        if (!LocalDate.now(clock).isBefore(toDelete.getValidFrom())) {
            LOG.info("could not delete WorkingTime={} because it is in the past.", workingTimeId);
            return false;
        }

        repository.deleteById(workingTimeId.uuid());
        return true;
    }

    private Map<UserIdComposite, List<WorkingTime>> findWorkingTimesForUsers(LocalDate from, LocalDate toExclusive, Collection<User> users) {
        final Map<UserIdComposite, List<WorkingTime>> workingTimesSorted = findWorkingTimesSorted(users);
        return workingTimesSorted.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().filter(workingTimeTouchingDateRange(from, toExclusive)).toList()
            ));
    }

    private static Predicate<WorkingTime> workingTimeTouchingDateRange(LocalDate from, LocalDate toExclusive) {
        return workingTime -> {
            if (workingTime.validFrom().isEmpty() && workingTime.validTo().isEmpty()) {
                // very first working-time, which is the only entry
                return true;
            } else if (workingTime.validFrom().isEmpty()) {
                // very first working-time, but other entries exist
                final LocalDate validTo = workingTime.validTo().get();
                return !validTo.isBefore(from);
            } else if (workingTime.validTo().isEmpty()) {
                // most recent working-time entry
                final LocalDate validFrom = workingTime.validFrom().get();
                return validFrom.isBefore(toExclusive);
            } else {
                // not the very first working-time
                final LocalDate validFrom = workingTime.validFrom().get();
                final LocalDate validTo = workingTime.validTo().get();
                return validFrom.isBefore(toExclusive) && validTo.isAfter(from);
            }
        };
    }

    private void setWorkDays(WorkingTimeEntity entity, EnumMap<DayOfWeek, Duration> workDays) {
        entity.setMonday(durationToString(workDays.get(MONDAY)));
        entity.setTuesday(durationToString(workDays.get(TUESDAY)));
        entity.setWednesday(durationToString(workDays.get(WEDNESDAY)));
        entity.setThursday(durationToString(workDays.get(THURSDAY)));
        entity.setFriday(durationToString(workDays.get(FRIDAY)));
        entity.setSaturday(durationToString(workDays.get(SATURDAY)));
        entity.setSunday(durationToString(workDays.get(SUNDAY)));
    }

    private Map<UserIdComposite, List<WorkingTime>> findWorkingTimesSorted(Collection<User> users) {

        final List<Long> localIdValues = users.stream().map(User::userLocalId).map(UserLocalId::value).toList();
        final Map<UserLocalId, List<WorkingTimeEntity>> sortedEntitiesByUserLocalId = findAllWorkingTimeEntitiesSorted(localIdValues);

        final Map<UserIdComposite, List<WorkingTime>> result = new HashMap<>(users.size());

        for (User user : users) {

            final UserIdComposite userIdComposite = user.userIdComposite();
            final List<WorkingTimeEntity> sortedEntities = sortedEntitiesByUserLocalId.get(user.userLocalId());

            if (sortedEntities.isEmpty()) {
                result.put(userIdComposite, List.of(defaultWorkingTime(userIdComposite)));
            } else {
                final List<WorkingTime> workingTimes = entitiesToWorkingTimes(sortedEntities, userIdComposite);
                result.put(userIdComposite, workingTimes);
            }
        }

        return result;
    }

    @Nullable
    private LocalDate getValidToDate(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {
        final WorkingTimeEntity adjacentNewer = getAdjacentNewer(entity, allEntitiesSorted);
        if (adjacentNewer == null) {
            // valid till infinite
            return null;
        } else {
            return adjacentNewer.getValidFrom().minusDays(1);
        }
    }

    @Nullable
    private LocalDate getMinValidFromDate(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {
        if (entity.getValidFrom() == null) {
            // I am the very first working-time
            return null;
        }
        final WorkingTimeEntity adjacentOlder = getAdjacentOlder(entity, allEntitiesSorted);
        if (adjacentOlder == null) {
            throw new IllegalStateException("expected adjacentOlder to exist. null case has been handle upfront.");
        }
        final LocalDate validFrom = adjacentOlder.getValidFrom();
        if (validFrom == null) {
            // adjacentOlder is the very first working-time. user has to decide ¯\_(ツ)_/¯
            return null;
        }
        return validFrom.minusDays(1);
    }

    private boolean isCurrent(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {

        final LocalDate today = LocalDate.now(clock);
        final WorkingTimeEntity adjacentNewer = getAdjacentNewer(entity, allEntitiesSorted);
        final WorkingTimeEntity adjacentOlder = getAdjacentOlder(entity, allEntitiesSorted);

        if (adjacentNewer == null && adjacentOlder == null) {
            // I am the only working-time entry, therefore I am the current \o/
            return true;
        } else if (adjacentNewer == null) {
            // I am the most recent entry
            if (adjacentOlder.getValidFrom() == null) {
                // and my ancestor is the very first working-time, so check myself
                return !entity.getValidFrom().isAfter(today);
            } else if (entity.getValidFrom().isAfter(today)) {
                return false;
            } else {
                // otherwise check my ancestor
                return adjacentOlder.getValidFrom().isBefore(today);
            }
        } else if (adjacentOlder == null) {
            // I am the very first working-time entry, so I can only be the current if I am valid from today or past.
            return !adjacentNewer.getValidFrom().isEqual(today) && !adjacentNewer.getValidFrom().isBefore(today);
        } else {
            // I am in the middle of working times. check if previous or next is current, if not -> I am
            final boolean newerIsBeforeToday = adjacentNewer.getValidFrom().isBefore(today);
            final boolean newerIsToday = adjacentNewer.getValidFrom().isEqual(today);
            if (newerIsBeforeToday || newerIsToday) {
              return false;
            }
            if (adjacentOlder.getValidFrom() == null) {
                return entity.getValidFrom().isEqual(today) || entity.getValidFrom().isBefore(today);
            }
            final boolean olderIsToday = adjacentOlder.getValidFrom().isEqual(today);
            final boolean olderIsAfter = adjacentOlder.getValidFrom().isAfter(today);
            return !olderIsToday && !olderIsAfter;
        }
    }

    @Nullable
    private WorkingTimeEntity getAdjacentNewer(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {
        final int indexOf = indexOf(entity, allEntitiesSorted);
        return indexOf == 0 ? null : allEntitiesSorted.get(indexOf - 1);
    }

    @Nullable
    private WorkingTimeEntity getAdjacentOlder(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {
        final int indexOf = indexOf(entity, allEntitiesSorted);
        return indexOf + 1 == allEntitiesSorted.size() ? null : allEntitiesSorted.get(indexOf + 1);
    }

    private int indexOf(WorkingTimeEntity entity, List<WorkingTimeEntity> allEntitiesSorted) {
        final int indexOf = allEntitiesSorted.indexOf(entity);
        if (indexOf == -1) {
            throw new IllegalStateException("expected entity to exist in allEntities list");
        }
        return indexOf;
    }

    private List<WorkingTimeEntity> findAllWorkingTimeEntitiesSorted(UserLocalId userLocalId) {
        return sortWorkingTimeEntitiesByValidFrom(repository.findAllByUserId(userLocalId.value()));
    }

    private Map<UserLocalId, List<WorkingTimeEntity>> findAllWorkingTimeEntitiesSorted(Collection<Long> userLocalIdValues) {

        final Map<UserLocalId, List<WorkingTimeEntity>> unsorted = repository.findAllByUserIdIsIn(userLocalIdValues)
            .stream()
            .collect(groupingBy(entity -> new UserLocalId(entity.getUserId())));

        final Map<UserLocalId, List<WorkingTimeEntity>> sorted = new HashMap<>(unsorted.size());

        for (Long userLocalIdValue : userLocalIdValues) {
            final UserLocalId userLocalId = new UserLocalId(userLocalIdValue);
            final List<WorkingTimeEntity> unsortedEntities = unsorted.getOrDefault(userLocalId, List.of());
            sorted.put(userLocalId, sortWorkingTimeEntitiesByValidFrom(unsortedEntities));
        }

        return sorted;
    }

    private List<WorkingTimeEntity> sortWorkingTimeEntitiesByValidFrom(Collection<WorkingTimeEntity> entities) {
        return entities.stream()
            .sorted(comparing(WorkingTimeEntity::getValidFrom, nullsLast(reverseOrder())))
            .toList();
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("expected user=%s to exist. but got nothing.".formatted(userLocalId)));
    }

    private String workdayToDurationString(@Nullable PlannedWorkingHours plannedWorkingHours) {
        return durationToString(plannedWorkingHours == null ? null : plannedWorkingHours.duration());
    }

    private String durationToString(@Nullable Duration duration) {
        return duration == null ? Duration.ZERO.toString() : duration.toString();
    }

    private WorkingTime defaultWorkingTime(UserIdComposite userIdComposite) {
        final Duration eight = Duration.ofHours(8);
        return WorkingTime.builder(userIdComposite, null)
            .current(true)
            .monday(eight)
            .tuesday(eight)
            .wednesday(eight)
            .thursday(eight)
            .friday(eight)
            .saturday(Duration.ZERO)
            .sunday(Duration.ZERO)
            .build();
    }

    private WorkingTime createDefaultWorkingTime(UserIdComposite userIdComposite) {
        final WorkingTime defaultWorkingTime = defaultWorkingTime(userIdComposite);
        final WorkingTimeEntity persisted = repository.save(workingTimeToEntity(defaultWorkingTime));
        return entityToWorkingTime(persisted, userIdComposite, List.of(persisted));
    }

    private List<WorkingTime> entitiesToWorkingTimes(List<WorkingTimeEntity> entities, UserIdComposite userIdComposite) {
        return entities.stream()
            .map(entity -> entityToWorkingTime(entity, userIdComposite, entities))
            .toList();
    }

    private WorkingTime entityToWorkingTime(WorkingTimeEntity entity, UserIdComposite userIdComposite, List<WorkingTimeEntity> allEntitiesSorted) {
        return WorkingTime.builder(userIdComposite, new WorkingTimeId(entity.getId()))
            .current(isCurrent(entity, allEntitiesSorted))
            .validFrom(entity.getValidFrom())
            .validTo(getValidToDate(entity, allEntitiesSorted))
            .minValidFrom(getMinValidFromDate(entity, allEntitiesSorted))
            .monday(Duration.parse(entity.getMonday()))
            .tuesday(Duration.parse(entity.getTuesday()))
            .wednesday(Duration.parse(entity.getWednesday()))
            .thursday(Duration.parse(entity.getThursday()))
            .friday(Duration.parse(entity.getFriday()))
            .saturday(Duration.parse(entity.getSaturday()))
            .sunday(Duration.parse(entity.getSunday()))
            .build();
    }

    private WorkingTimeEntity workingTimeToEntity(WorkingTime workingTime) {

        final WorkingTimeEntity entity = new WorkingTimeEntity();

        if (workingTime.id() != null) {
            entity.setId(workingTime.id().uuid());
        }

        entity.setUserId(workingTime.userLocalId().value());
        entity.setValidFrom(workingTime.validFrom().orElse(null));
        entity.setMonday(workdayToDurationString(workingTime.getMonday()));
        entity.setTuesday(workdayToDurationString(workingTime.getTuesday()));
        entity.setWednesday(workdayToDurationString(workingTime.getWednesday()));
        entity.setThursday(workdayToDurationString(workingTime.getThursday()));
        entity.setFriday(workdayToDurationString(workingTime.getFriday()));
        entity.setSaturday(workdayToDurationString(workingTime.getSaturday()));
        entity.setSunday(workdayToDurationString(workingTime.getSunday()));

        return entity;
    }
}
