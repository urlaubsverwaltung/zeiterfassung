package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.MONTHS;

@Service
class ReportServicePermissionAware implements ReportService {

    private final ReportPermissionService reportPermissionService;
    private final ReportServiceRaw reportServiceRaw;
    private final UserDateService userDateService;
    private final TimeEntryLockService timeEntryLockService;

    ReportServicePermissionAware(ReportPermissionService reportPermissionService, ReportServiceRaw reportServiceRaw, UserDateService userDateService, TimeEntryLockService timeEntryLockService) {
        this.reportPermissionService = reportPermissionService;
        this.reportServiceRaw = reportServiceRaw;
        this.userDateService = userDateService;
        this.timeEntryLockService = timeEntryLockService;
    }

    @Override
    public ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {

        final List<UserLocalId> permittedUserLocalIds =
            reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userLocalIds);

        if (permittedUserLocalIds.isEmpty()) {
            return emptyReportWeek(year, week);
        }

        return reportWeekForPermittedUserIds(year, week, permittedUserLocalIds);
    }

    @Override
    public ReportWeek getReportWeekForAllUsers(Year year, int week) {

        final boolean permittedForAll = reportPermissionService.currentUserHasPermissionForAllUsers();
        if (permittedForAll) {
            return reportServiceRaw.getReportWeekForAllUsers(year, week);
        }

        final List<UserLocalId> permittedUserLocalIds =
            reportPermissionService.findAllPermittedUserLocalIdsForCurrentUser();

        return reportWeekForPermittedUserIds(year, week, permittedUserLocalIds);
    }

    @Override
    public ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {

        // UserId is considered trustworthy since it is created on server side only (without client input, at least for time of writing this...)
        return reportServiceRaw.getReportMonth(yearMonth, userId);
    }

    @Override
    public ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {

        final List<UserLocalId> permittedUserLocalIds =
            reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userLocalIds);

        if (permittedUserLocalIds.isEmpty()) {
            return emptyReportMonth(yearMonth);
        }

        return reportMonthForPermittedUserIds(yearMonth, permittedUserLocalIds);
    }

    @Override
    public ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {

        final boolean permittedForAll = reportPermissionService.currentUserHasPermissionForAllUsers();
        if (permittedForAll) {
            return reportServiceRaw.getReportMonthForAllUsers(yearMonth);
        }

        final List<UserLocalId> permittedUserLocalIds =
            reportPermissionService.findAllPermittedUserLocalIdsForCurrentUser();

        return reportMonthForPermittedUserIds(yearMonth, permittedUserLocalIds);
    }

    private ReportWeek reportWeekForPermittedUserIds(Year year, int week, List<UserLocalId> permittedUserLocalIds) {
        return reportServiceRaw.getReportWeek(year, week, permittedUserLocalIds);
    }

    private ReportMonth reportMonthForPermittedUserIds(YearMonth yearMonth, List<UserLocalId> permittedUserLocalIds) {
        return reportServiceRaw.getReportMonth(yearMonth, permittedUserLocalIds);
    }

    private ReportWeek emptyReportWeek(Year year, int week) {
        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);
        return emptyReportWeek(firstDateOfWeek);
    }

    private ReportWeek emptyReportWeek(LocalDate startOfWeekDate) {
        Optional<LocalDate> minValidTimeEntryDate = timeEntryLockService.getMinValidTimeEntryDate();

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd -> {
                LocalDate date = startOfWeekDate.plusDays(daysToAdd);
                boolean locked = isDateLocked(date, minValidTimeEntryDate);
                return new ReportDay(date, locked, Map.of(), Map.of(), Map.of());
            })
            .toList();

        return new ReportWeek(startOfWeekDate, reportDays);
    }

    private ReportMonth emptyReportMonth(YearMonth yearMonth) {

        final List<ReportWeek> weeks = getStartOfWeekDatesForMonth(yearMonth)
            .stream()
            .map(this::emptyReportWeek)
            .toList();

        return new ReportMonth(yearMonth, weeks);
    }

    private List<LocalDate> getStartOfWeekDatesForMonth(YearMonth yearMonth) {
        final List<LocalDate> startOfWeekDates = new ArrayList<>();

        final LocalDate firstOfMonth = yearMonth.atDay(1);
        LocalDate date = userDateService.localDateToFirstDateOfWeek(firstOfMonth);

        while (isPreviousMonth(date, yearMonth) || date.getMonthValue() == yearMonth.getMonthValue()) {
            startOfWeekDates.add(date);
            date = date.plusWeeks(1);
        }

        return startOfWeekDates;
    }

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }

    private static boolean isDateLocked(LocalDate date, Optional<LocalDate> minValidTimeEntryDate) {
        return minValidTimeEntryDate.map(date::isBefore).orElse(false);
    }
}
