package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
class WorkTimeServiceImpl implements WorkingTimeService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final WorkingTimeRepository repository;
    private final UserDateService userDateService;
    private final UserManagementService userManagementService;

    WorkTimeServiceImpl(WorkingTimeRepository repository, UserDateService userDateService,
                        UserManagementService userManagementService) {

        this.repository = repository;
        this.userDateService = userDateService;
        this.userManagementService = userManagementService;
    }

    @Override
    public WorkingTime getWorkingTimeByUser(UserLocalId userLocalId) {

        final User user = findUser(userLocalId);

        return repository.findByUserId(userLocalId.value())
            .map(workingTimeEntity -> entityToWorkingTime(workingTimeEntity, user.userIdComposite()))
            .orElseGet(() -> createDefaultWorkingTime(user.userIdComposite()));
    }

    @Override
    public Optional<WorkingTime> getWorkingTimeById(WorkingTimeId workingTimeId) {
        return repository.findById(workingTimeId.uuid()).map(entity -> {
            final User user = findUser(new UserLocalId(entity.getUserId()));
            return entityToWorkingTime(entity, user.userIdComposite());
        });
    }

    @Override
    public List<WorkingTime> getAllWorkingTimesByUser(UserLocalId userLocalId) {

        final User user = findUser(userLocalId);

        final List<WorkingTime> workingTimes = repository.findAllByUserIdOrderByValidFrom(userLocalId.value()).stream()
            .map(workingTimeEntity -> entityToWorkingTime(workingTimeEntity, user.userIdComposite()))
            .toList();

        return workingTimes.isEmpty() ? List.of(createDefaultWorkingTime(user.userIdComposite())) : workingTimes;
    }

    @Override
    public Map<UserIdComposite, WorkingTime> getWorkingTimeByUsers(Collection<UserLocalId> userLocalIds) {
        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);
        return getWorkingTime(users);
    }

    @Override
    public Map<UserIdComposite, WorkingTime> getAllWorkingTimeByUsers() {
        return getWorkingTime(userManagementService.findAllUsers());
    }

    @Override
    public Map<LocalDate, PlannedWorkingHours> getWorkingHoursByUserAndYearWeek(UserLocalId userLocalId, Year year, int weekOfYear) {

        final WorkingTime workingTime = getWorkingTimeByUser(userLocalId);
        final LocalDate firstDayOfWeek = userDateService.firstDayOfWeek(year, weekOfYear);

        return IntStream.range(0, 7)
            .mapToObj(firstDayOfWeek::plusDays)
            .map(day -> Map.entry(day, workDayToPlannedHours(workingTime.getForDayOfWeek(day.getDayOfWeek()))))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public WorkingTime createWorkingTime(UserLocalId userLocalId, LocalDate validFrom, EnumMap<DayOfWeek, Duration> workdays) {

        final User user = findUser(userLocalId);

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(userLocalId.value());
        entity.setValidFrom(validFrom);
        entity.setMonday(durationToString(workdays.get(MONDAY)));
        entity.setTuesday(durationToString(workdays.get(TUESDAY)));
        entity.setWednesday(durationToString(workdays.get(WEDNESDAY)));
        entity.setThursday(durationToString(workdays.get(THURSDAY)));
        entity.setFriday(durationToString(workdays.get(FRIDAY)));
        entity.setSaturday(durationToString(workdays.get(SATURDAY)));
        entity.setSunday(durationToString(workdays.get(SUNDAY)));

        final WorkingTimeEntity saved = repository.save(entity);

        return entityToWorkingTime(saved, user.userIdComposite());
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

        entity.setMonday(workdayToDurationString(workWeekUpdate.monday()));
        entity.setTuesday(workdayToDurationString(workWeekUpdate.tuesday()));
        entity.setWednesday(workdayToDurationString(workWeekUpdate.wednesday()));
        entity.setThursday(workdayToDurationString(workWeekUpdate.thursday()));
        entity.setFriday(workdayToDurationString(workWeekUpdate.friday()));
        entity.setSaturday(workdayToDurationString(workWeekUpdate.saturday()));
        entity.setSunday(workdayToDurationString(workWeekUpdate.sunday()));

        final User user = findUser(new UserLocalId(entity.getUserId()));

        return entityToWorkingTime(repository.save(entity), user.userIdComposite());
    }

    private Map<UserIdComposite, WorkingTime> getWorkingTime(Collection<User> users) {

        final List<Long> localIdValues = new ArrayList<>();
        final Map<Long, UserIdComposite> userIdCompositeByLocalIdValue = new HashMap<>();
        for (User user : users) {
            localIdValues.add(user.userLocalId().value());
            userIdCompositeByLocalIdValue.put(user.userLocalId().value(), user.userIdComposite());
        }

        final Map<UserIdComposite, WorkingTime> result = repository.findAllByUserIdIsIn(localIdValues)
            .stream()
            .map(workingTimeEntity -> entityToWorkingTime(workingTimeEntity, userIdCompositeByLocalIdValue.get(workingTimeEntity.getUserId())))
            .collect(toMap(WorkingTime::userIdComposite, identity()));

        for (User user : users) {
            result.computeIfAbsent(user.userIdComposite(), this::defaultWorkingTime);
        }

        return result;
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("expected user=%s to exist. but got nothing.".formatted(userLocalId)));
    }

    private String workdayToDurationString(Optional<WorkDay> workDay) {
        return durationToString(workDay.map(WorkDay::duration).orElse(null));
    }

    private String durationToString(@Nullable Duration duration) {
        return duration == null ? Duration.ZERO.toString() : duration.toString();
    }

    private WorkingTime defaultWorkingTime(UserIdComposite userIdComposite) {
        final Duration eight = Duration.ofHours(8);
        return WorkingTime.builder(userIdComposite, null)
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
        return entityToWorkingTime(persisted, userIdComposite);
    }

    private static WorkingTime entityToWorkingTime(WorkingTimeEntity entity, UserIdComposite userIdComposite) {
        return WorkingTime.builder(userIdComposite, new WorkingTimeId(entity.getId()))
            .validFrom(entity.getValidFrom())
            .monday(orZero(entity.getMonday()))
            .tuesday(orZero(entity.getTuesday()))
            .wednesday(orZero(entity.getWednesday()))
            .thursday(orZero(entity.getThursday()))
            .friday(orZero(entity.getFriday()))
            .saturday(orZero(entity.getSaturday()))
            .sunday(orZero(entity.getSunday()))
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

    private PlannedWorkingHours workDayToPlannedHours(Optional<WorkDay> workDay) {
        return workDay
            .map(WorkDay::duration)
            .map(PlannedWorkingHours::new)
            .orElse(PlannedWorkingHours.ZERO);
    }

    private static Duration orZero(String durationString) {
        if (durationString == null) {
            return Duration.ZERO;
        } else {
            return Duration.parse(durationString);
        }
    }
}
