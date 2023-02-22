package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserDateService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
class WorkTimeServiceImpl implements WorkingTimeService {

    private final WorkingTimeRepository repository;
    private final UserDateService userDateService;

    WorkTimeServiceImpl(WorkingTimeRepository repository, UserDateService userDateService) {
        this.repository = repository;
        this.userDateService = userDateService;
    }

    @Override
    public WorkingTime getWorkingTimeByUser(UserLocalId userLocalId) {
        return repository.findByUserId(userLocalId.value())
            .map(WorkTimeServiceImpl::entityToWorkingTime)
            .orElseGet(() -> defaultWorkingTime(userLocalId));
    }

    @Override
    public Map<LocalDate, PlannedWorkingHours> getWorkingHoursByUserAndYearWeek(UserLocalId userLocalId, Year year, int weekOfYear) {

        final WorkingTime workingTime = getWorkingTimeByUser(userLocalId);
        final LocalDate firstDayOfWeek = userDateService.firstDayOfWeek(year, weekOfYear);

        return IntStream.range(0, 7)
            .mapToObj(firstDayOfWeek::plusDays)
            .map(day -> Map.entry(day, workDayToPlannedHours(workingTime.getForDayOfWeek(day.getDayOfWeek()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public WorkingTime updateWorkingTime(WorkingTime workingTime) {

        final WorkingTimeEntity entity = repository.findByUserId(workingTime.getUserId().value()).orElseGet(WorkingTimeEntity::new);

        entity.setMonday(toDurationString(workingTime.getMonday()));
        entity.setTuesday(toDurationString(workingTime.getTuesday()));
        entity.setWednesday(toDurationString(workingTime.getWednesday()));
        entity.setThursday(toDurationString(workingTime.getThursday()));
        entity.setFriday(toDurationString(workingTime.getFriday()));
        entity.setSaturday(toDurationString(workingTime.getSaturday()));
        entity.setSunday(toDurationString(workingTime.getSunday()));

        if (entity.getId() == null) {
            entity.setUserId(workingTime.getUserId().value());
        }

        return entityToWorkingTime(repository.save(entity));
    }

    private String toDurationString(Optional<WorkDay> workDay) {
        return workDay.map(WorkDay::duration).map(Duration::toString).orElse(Duration.ZERO.toString());
    }

    private WorkingTime defaultWorkingTime(UserLocalId userLocalId) {
        final Duration eight = Duration.ofHours(8);
        return WorkingTime.builder()
            .userId(userLocalId)
            .monday(eight)
            .tuesday(eight)
            .wednesday(eight)
            .thursday(eight)
            .friday(eight)
            .saturday(Duration.ZERO)
            .sunday(Duration.ZERO)
            .build();
    }

    private static WorkingTime entityToWorkingTime(WorkingTimeEntity entity) {
        return WorkingTime.builder()
            .userId(new UserLocalId(entity.getUserId()))
            .monday(orZero(entity.getMonday()))
            .tuesday(orZero(entity.getTuesday()))
            .wednesday(orZero(entity.getWednesday()))
            .thursday(orZero(entity.getThursday()))
            .friday(orZero(entity.getFriday()))
            .saturday(orZero(entity.getSaturday()))
            .sunday(orZero(entity.getSunday()))
            .build();
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
