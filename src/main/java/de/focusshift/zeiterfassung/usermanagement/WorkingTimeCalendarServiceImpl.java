package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService) {
        this.workingTimeService = workingTimeService;
    }

    @Override
    public Map<UserLocalId, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive) {
        return entitiesToWorkingTime(from, toExclusive, workingTimeService.getAllWorkingTimeByUsers());
    }

    @Override
    public Map<UserLocalId, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        return entitiesToWorkingTime(from, toExclusive, workingTimeService.getWorkingTimeByUsers(userLocalIds));
    }

    private Map<UserLocalId, WorkingTimeCalendar> entitiesToWorkingTime(LocalDate from, LocalDate toExclusive, Map<UserLocalId, WorkingTime> workingTimes) {

        final HashMap<UserLocalId, WorkingTimeCalendar> result = new HashMap<>();

        for (Map.Entry<UserLocalId, WorkingTime> entry : workingTimes.entrySet()) {
            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(from, toExclusive, entry.getValue());
            result.put(entry.getKey(), workingTimeCalendar);
        }

        return result;
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, WorkingTime workingTime) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate date = from;
        while (date.isBefore(toExclusive)) {
            final Duration duration = workingTime.getForDayOfWeek(date.getDayOfWeek()).map(WorkDay::duration).orElse(Duration.ZERO);
            plannedWorkingHoursByDate.put(date, new PlannedWorkingHours(duration));
            date = date.plusDays(1);
        }

        return new WorkingTimeCalendar(plannedWorkingHoursByDate);
    }
}
