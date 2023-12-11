package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService) {
        this.workingTimeService = workingTimeService;
    }

    @Override
    public WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        return getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId))
            .values().stream().toList().getFirst();
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getAllWorkingTimes(from, toExclusive));
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        return toWorkingTimeCalendar(from, toExclusive, workingTimeService.getWorkingTimesByUsers(from, toExclusive, userLocalIds));
    }

    private Map<UserIdComposite, WorkingTimeCalendar> toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, Map<UserIdComposite, List<WorkingTime>> sortedWorkingTimes) {

        final HashMap<UserIdComposite, WorkingTimeCalendar> result = new HashMap<>();

        sortedWorkingTimes.forEach((userIdComposite, workingTimes) -> {
            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(from, toExclusive, workingTimes);
            result.put(userIdComposite, workingTimeCalendar);
        });

        return result;
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, List<WorkingTime> sortedWorkingTimes) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate nextEnd = toExclusive.minusDays(1);

        for (WorkingTime workingTime : sortedWorkingTimes) {

            final DateRange workingTimeDateRange;
            final LocalDate validFrom = workingTime.validFrom().orElse(null);
            if (validFrom == null || validFrom.isBefore(from)) {
                workingTimeDateRange = new DateRange(from, nextEnd);
            } else {
                workingTimeDateRange = new DateRange(validFrom, nextEnd);
            }

            for (LocalDate localDate : workingTimeDateRange) {
                plannedWorkingHoursByDate.put(localDate, workingTime.getForDayOfWeek(localDate.getDayOfWeek()));
            }

            if (workingTimeDateRange.startDate().equals(from)) {
                break;
            } else {
                nextEnd = workingTime.validFrom().map(date -> date.minusDays(1))
                    .orElseThrow(() -> new IllegalStateException("from cannot be before the very first workingTime with validFrom=null."));
            }
        }

        return new WorkingTimeCalendar(plannedWorkingHoursByDate);
    }
}
