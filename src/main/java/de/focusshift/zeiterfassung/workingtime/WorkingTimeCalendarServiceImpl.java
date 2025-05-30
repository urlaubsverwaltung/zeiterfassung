package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidayCalendar;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidaysService;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.max;
import static java.util.Collections.min;
import static java.util.stream.Collectors.toUnmodifiableSet;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;
    private final PublicHolidaysService publicHolidaysService;
    private final AbsenceService absenceService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService,
                                   PublicHolidaysService publicHolidaysService,
                                   AbsenceService absenceService) {
        this.workingTimeService = workingTimeService;
        this.publicHolidaysService = publicHolidaysService;
        this.absenceService = absenceService;
    }

    @Override
    public WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        return getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId))
            .values().stream().toList().getFirst();
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive) {
        final Map<UserIdComposite, List<Absence>> absences = absenceService.getAbsencesForAllUsers(from, toExclusive);
        return toWorkingTimeCalendar(from, toExclusive, absences, workingTimeService::getAllWorkingTimes);
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        final Map<UserIdComposite, List<Absence>> absences = absenceService.getAbsencesByUserIds(userLocalIds.stream().toList(), from, toExclusive);
        return toWorkingTimeCalendar(from, toExclusive, absences, (start, endExclusive) -> workingTimeService.getWorkingTimesByUsers(start, endExclusive, userLocalIds));
    }

    private Map<UserIdComposite, WorkingTimeCalendar> toWorkingTimeCalendar(
        LocalDate from,
        LocalDate toExclusive,
        Map<UserIdComposite, List<Absence>> absencesByUser,
        BiFunction<LocalDate, LocalDate, Map<UserIdComposite, List<WorkingTime>>> workingTimesSupplier
    ) {

        // from / toExclusive is the requested date-range
        // however, absences have to be considered calculating ShouldWorkingHours etc.
        // a reduction in overtime can take several days. the hours are then distributed over these days.
        // therefore we have to fetch WorkingTime for the biggest data-range of from/toExclusive AND absences.
        final LocalDate minDate = minStartDate(absencesByUser.values(), from);
        final LocalDate maxDateExclusive = maxEndDate(absencesByUser.values(), toExclusive.minusDays(1)).plusDays(1);
        final Map<UserIdComposite, List<WorkingTime>> sortedWorkingTimes = workingTimesSupplier.apply(minDate, maxDateExclusive);

        // fetch public holidays, now that we know the date-range
        // public holidays currently means: no ShouldWorkingHours -> this has to be considered by WorkingTimeCalendar
        final Map<FederalState, PublicHolidayCalendar> publicHolidayCalendars =
            getPublicHolidayCalendars(sortedWorkingTimes.values(), minDate, maxDateExclusive);

        final BiPredicate<LocalDate, FederalState> isPublicHoliday = (localDate, federalState) ->
            publicHolidayCalendars.containsKey(federalState) && publicHolidayCalendars.get(federalState).isPublicHoliday(localDate);

        final HashMap<UserIdComposite, WorkingTimeCalendar> workingTimeCalenderByUser = new HashMap<>();

        sortedWorkingTimes.forEach((userIdComposite, workingTimes) -> {

            final Map<LocalDate, List<Absence>> absencesByDate = new HashMap<>();
            for (Absence absence : absencesByUser.get(userIdComposite)) {
                Instant date = absence.startDate();
                while (!date.isAfter(absence.endDate())) {
                    absencesByDate.computeIfAbsent(LocalDate.ofInstant(date, UTC), unused -> new ArrayList<>()).add(absence);
                    date = date.plus(1, DAYS);
                }
            }

            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(minDate, maxDateExclusive, workingTimes, absencesByDate, isPublicHoliday);
            workingTimeCalenderByUser.put(userIdComposite, workingTimeCalendar);
        });

        return workingTimeCalenderByUser;
    }

    private Map<FederalState, PublicHolidayCalendar> getPublicHolidayCalendars(Collection<List<WorkingTime>> workingTimes, LocalDate from, LocalDate toExclusive) {

        final Set<FederalState> federalStates = workingTimes
            .stream()
            .flatMap(List::stream)
            .map(WorkingTime::federalState)
            .collect(toUnmodifiableSet());

       return publicHolidaysService.getPublicHolidays(from, toExclusive, federalStates);
    }

    private static LocalDate minStartDate(Collection<List<Absence>> absences, LocalDate start) {

        final Optional<LocalDate> minStartDateOfAbsences = absences.stream()
            .flatMap(List::stream)
            .map(Absence::startDate)
            .min(Instant::compareTo)
            .map((Instant instant) -> LocalDate.ofInstant(instant, UTC));

        return minStartDateOfAbsences.map(absenceStartDate -> min(List.of(absenceStartDate, start))).orElse(start);
    }

    private static LocalDate maxEndDate(Collection<List<Absence>> absences, LocalDate end) {

        final Optional<LocalDate> maxEndDateOfAbsences = absences.stream()
            .flatMap(List::stream)
            .map(Absence::endDate)
            .max(Instant::compareTo)
            .map((Instant instant) -> LocalDate.ofInstant(instant, UTC));

        return maxEndDateOfAbsences.map(absenceEndDate -> max(List.of(absenceEndDate, end))).orElse(end);
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate minDate, LocalDate maxDateExclusive, List<WorkingTime> sortedWorkingTimes,
                                                      Map<LocalDate, List<Absence>> absencesByDate, BiPredicate<LocalDate, FederalState> isPublicHoliday) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate nextEnd = maxDateExclusive.minusDays(1);

        for (WorkingTime workingTime : sortedWorkingTimes) {
            final DateRange workingTimeDateRange = getDateRange(minDate, workingTime, nextEnd);

            for (LocalDate localDate : workingTimeDateRange) {
                if (workingTime.worksOnPublicHoliday() || !isPublicHoliday.test(localDate, workingTime.federalState())) {
                    plannedWorkingHoursByDate.put(localDate, workingTime.getForDayOfWeek(localDate.getDayOfWeek()));
                } else {
                    plannedWorkingHoursByDate.put(localDate, PlannedWorkingHours.ZERO);
                }
            }

            if (workingTimeDateRange.startDate().equals(minDate)) {
                break;
            } else {
                nextEnd = workingTime.validFrom().map(date -> date.minusDays(1))
                    .orElseThrow(() -> new IllegalStateException("minDate cannot be before the very first workingTime with validFrom=null."));
            }
        }

        return new WorkingTimeCalendar(plannedWorkingHoursByDate, absencesByDate);
    }

    private static DateRange getDateRange(LocalDate from, WorkingTime workingTime, LocalDate nextEnd) {
        final LocalDate validFrom = workingTime.validFrom().orElse(null);
        if (validFrom == null || validFrom.isBefore(from)) {
            return new DateRange(from, nextEnd);
        } else {
            return new DateRange(validFrom, nextEnd);
        }
    }
}
