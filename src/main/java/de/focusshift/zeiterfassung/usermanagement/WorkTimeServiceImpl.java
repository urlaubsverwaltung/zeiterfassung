package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserIdComposite;
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
import java.util.function.Function;

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
            final WorkingTimeEntity current = getCurrentWorkingTimeEntity(userLocalId);
            return entityToWorkingTime(entity, user.userIdComposite(), entity.equals(current));
        });
    }

    @Override
    public List<WorkingTime> getAllWorkingTimesByUser(UserLocalId userLocalId) {

        final User user = findUser(userLocalId);
        final List<WorkingTimeEntity> sortedEntities = findAllWorkingTimeEntitiesSorted(userLocalId);

        if (sortedEntities.isEmpty()) {
            return List.of(createDefaultWorkingTime(user.userIdComposite()));
        }

        final WorkingTimeEntity current = getCurrentWorkingTimeEntity(userLocalId);

        return sortedEntities.stream()
            .map(entity -> entityToWorkingTime(entity, user.userIdComposite(), entity.equals(current)))
            .toList();
    }

    @Override
    public Map<UserIdComposite, List<WorkingTime>> getWorkingTimesByUsers(Collection<UserLocalId> userLocalIds) {
        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);
        return findWorkingTimesSorted(users);
    }

    @Override
    public Map<UserIdComposite, List<WorkingTime>> getAllWorkingTimesByUsers() {
        return findWorkingTimesSorted(userManagementService.findAllUsers());
    }

    @Override
    public WorkingTime createWorkingTime(UserLocalId userLocalId, LocalDate validFrom, EnumMap<DayOfWeek, Duration> workdays) {

        final User user = findUser(userLocalId);

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(userLocalId.value());
        entity.setValidFrom(validFrom);
        setWorkDays(entity, workdays);

        final LocalDate today = LocalDate.now(clock);
        final boolean isCurrent;

        if (validFrom.isAfter(today)) {
            isCurrent = false;
        } else {
            final List<WorkingTimeEntity> all = findAllWorkingTimeEntitiesSorted(user.userLocalId());
            isCurrent = all.stream().noneMatch(w -> w.getValidFrom() != null && w.getValidFrom().isAfter(validFrom));
        }

        final WorkingTimeEntity saved = repository.save(entity);

        return entityToWorkingTime(saved, user.userIdComposite(), isCurrent);
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
        final WorkingTimeEntity current = getCurrentWorkingTimeEntity(userLocalId);

        return entityToWorkingTime(saved, user.userIdComposite(), saved.equals(current));
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

    private void setWorkDays(WorkingTimeEntity entity, EnumMap<DayOfWeek, Duration> workDays) {
        entity.setMonday(durationToString(workDays.get(MONDAY)));
        entity.setTuesday(durationToString(workDays.get(TUESDAY)));
        entity.setWednesday(durationToString(workDays.get(WEDNESDAY)));
        entity.setThursday(durationToString(workDays.get(THURSDAY)));
        entity.setFriday(durationToString(workDays.get(FRIDAY)));
        entity.setSaturday(durationToString(workDays.get(SATURDAY)));
        entity.setSunday(durationToString(workDays.get(SUNDAY)));
    }

    private WorkingTimeEntity getCurrentWorkingTimeEntity(UserLocalId userLocalId) {
        WorkingTimeEntity current = repository.findByPersonAndValidityDateEqualsOrMinorDate(userLocalId.value(), LocalDate.now(clock));
        if (current == null) {
            current = repository.findByUserIdAndValidFrom(userLocalId.value(), null)
                .orElseThrow(() -> new IllegalStateException("expected one working-time to exist for user=" + userLocalId));
        }
        return current;
    }

    private Map<UserIdComposite, List<WorkingTime>> findWorkingTimesSorted(Collection<User> users) {

        final List<Long> localIdValues = users.stream().map(User::userLocalId).map(UserLocalId::value).toList();
        final Map<UserLocalId, List<WorkingTimeEntity>> sortedEntities = findAllWorkingTimeEntitiesSorted(localIdValues);

        final Map<UserIdComposite, List<WorkingTime>> result = new HashMap<>(users.size());

        for (User user : users) {

            final UserIdComposite userIdComposite = user.userIdComposite();
            final List<WorkingTimeEntity> entities = sortedEntities.get(user.userLocalId());

            if (entities.isEmpty()) {
                result.put(userIdComposite, List.of(defaultWorkingTime(userIdComposite)));
            } else {
                // FIXME implement isCurrent
                final List<WorkingTime> workingTimes = entitiesToWorkingTimes(entities, userIdComposite, (entity) -> true);
                result.put(userIdComposite, workingTimes);
            }
        }

        return result;
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

    private String workdayToDurationString(@Nullable WorkDay workDay) {
        return durationToString(workDay == null ? null : workDay.duration());
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
        return entityToWorkingTime(persisted, userIdComposite, true);
    }

    private List<WorkingTime> entitiesToWorkingTimes(List<WorkingTimeEntity> entities, UserIdComposite userIdComposite, Function<WorkingTimeEntity, Boolean> currentSupplier) {
        return entities.stream()
            .map(entity -> entityToWorkingTime(entity, userIdComposite, currentSupplier.apply(entity)))
            .toList();
    }

    private WorkingTime entityToWorkingTime(WorkingTimeEntity entity, UserIdComposite userIdComposite, boolean current) {
        return WorkingTime.builder(userIdComposite, new WorkingTimeId(entity.getId()))
            .current(current)
            .validFrom(entity.getValidFrom())
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
