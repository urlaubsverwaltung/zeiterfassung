package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class ReportCsvService {

    private static final int FRACTION_DIGITS = 3;
    private final ReportService reportService;
    private final DateFormatter dateFormatter;
    private final MessageSource messageSource;
    private final UserManagementService userManagementService;

    ReportCsvService(ReportService reportService, DateFormatter dateFormatter, MessageSource messageSource, UserManagementService userManagementService) {
        this.reportService = reportService;
        this.dateFormatter = dateFormatter;
        this.messageSource = messageSource;
        this.userManagementService = userManagementService;
    }

    // ------------------------------------------------------------
    // detailed CSV (one row per time entry)
    // ------------------------------------------------------------

    void writeWeekReportCsv(Year year, int week, Locale locale, UserLocalId userLocalId, PrintWriter writer) {
        writeWeekReportCsvForUserLocalIds(year, week, locale, List.of(userLocalId), writer);
    }

    void writeWeekReportCsvForUserLocalIds(Year year, int week, Locale locale, List<UserLocalId> userLocalIds, PrintWriter writer) {
        final ReportWeek reportWeek = reportService.getReportWeek(year, week, userLocalIds);
        writeWeekCsv(reportWeek, locale, writer);
    }

    void writeMonthReportCsv(YearMonth yearMonth, Locale locale, UserId userId, PrintWriter writer) {
        final ReportMonth reportMonth = reportService.getReportMonth(yearMonth, userId);
        writeMonthCsv(reportMonth, yearMonth, locale, writer);
    }

    void writeMonthReportCsvForUserLocalIds(YearMonth yearMonth, Locale locale, List<UserLocalId> userLocalIds, PrintWriter writer) {
        final ReportMonth reportMonth = reportService.getReportMonth(yearMonth, userLocalIds);
        writeMonthCsv(reportMonth, yearMonth, locale, writer);
    }

    private void writeWeekCsv(ReportWeek reportWeek, Locale locale, PrintWriter writer) {
        writeDetailHeader(locale, writer);
        writeWeek(reportWeek, locale, writer);
    }

    private void writeMonthCsv(ReportMonth reportMonth, YearMonth yearMonth, Locale locale, PrintWriter writer) {
        writeDetailHeader(locale, writer);
        reportMonth.weeks()
            .stream()
            .map(reportWeek -> reportWeekForMonthOnly(reportWeek, yearMonth.getMonth()))
            .forEach(reportWeek -> writeWeek(reportWeek, locale, writer));
    }

    private ReportWeek reportWeekForMonthOnly(ReportWeek reportWeek, Month month) {
        final List<ReportDay> reportDays = reportWeek.reportDays()
            .stream()
            .filter(reportDay -> reportDay.date().getMonth().equals(month))
            .toList();

        return new ReportWeek(reportWeek.firstDateOfWeek(), reportDays);
    }

    private void writeDetailHeader(Locale locale, PrintWriter writer) {
        writer.println(csvLine(
            message("report.csv.header.date", locale),
            message("report.csv.header.person.givenName", locale),
            message("report.csv.header.person.familyName", locale),
            message("report.csv.header.start", locale),
            message("report.csv.header.end", locale),
            message("report.csv.header.workedHours", locale),
            message("report.csv.header.shouldWorkingHours", locale),
            message("report.csv.header.comment", locale),
            message("report.csv.header.break", locale)
        ));
    }

    private void writeWeek(ReportWeek reportWeek, Locale locale, PrintWriter writer) {

        final NumberFormat numberFormat = numberFormat(locale);

        reportWeek.reportDays().forEach(reportDay ->
            reportDay.reportDayEntries().stream()
                .map(reportDayEntry -> reportDayEntryToCsvLine(reportDay, reportDayEntry, numberFormat))
                .forEach(writer::println)
        );
    }

    private String reportDayEntryToCsvLine(ReportDay reportDay, ReportDayEntry reportDayEntry, NumberFormat numberFormat) {
        final String date = dateFormatter.formatDate(reportDayEntry.start().toLocalDate());
        final String givenName = reportDayEntry.user().givenName();
        final String familyName = reportDayEntry.user().familyName();
        final LocalTime start = reportDayEntry.start().toLocalTime();
        final LocalTime end = reportDayEntry.end().toLocalTime();
        final String hoursWorked = numberFormat.format(reportDayEntry.workDuration().hoursDoubleValue());
        final String shouldWorkingHours = shouldWorkingHoursForUserOnDay(reportDay, reportDayEntry.user().userIdComposite())
            .map(hours -> numberFormat.format(hours.hoursDoubleValue()))
            .orElse("");
        final String comment = reportDayEntry.comment();
        final boolean isBreak = reportDayEntry.isBreak();

        return csvLine(date, givenName, familyName, start, end, hoursWorked, shouldWorkingHours, comment, isBreak);
    }

    private Optional<ShouldWorkingHours> shouldWorkingHoursForUserOnDay(ReportDay reportDay, UserIdComposite userIdComposite) {
        return Optional.ofNullable(reportDay.workingTimeCalendarByUser().get(userIdComposite))
            .flatMap(calendar -> calendar.shouldWorkingHours(reportDay.date()));
    }

    // ------------------------------------------------------------
    // aggregated CSV (one row per person and period)
    // ------------------------------------------------------------

    void writeWeekReportCsvAggregated(Year year, int week, Locale locale, UserLocalId userLocalId, PrintWriter writer) {
        writeWeekReportCsvAggregatedForUserLocalIds(year, week, locale, List.of(userLocalId), writer);
    }

    void writeWeekReportCsvAggregatedForUserLocalIds(Year year, int week, Locale locale, List<UserLocalId> userLocalIds, PrintWriter writer) {
        final ReportWeek reportWeek = reportService.getReportWeek(year, week, userLocalIds);
        writeWeekCsvAggregated(reportWeek, locale, writer);
    }

    void writeMonthReportCsvAggregated(YearMonth yearMonth, Locale locale, UserId userId, PrintWriter writer) {
        final ReportMonth reportMonth = reportService.getReportMonth(yearMonth, userId);
        writeMonthCsvAggregated(reportMonth, locale, writer);
    }

    void writeMonthReportCsvAggregatedForUserLocalIds(YearMonth yearMonth, Locale locale, List<UserLocalId> userLocalIds, PrintWriter writer) {
        final ReportMonth reportMonth = reportService.getReportMonth(yearMonth, userLocalIds);
        writeMonthCsvAggregated(reportMonth, locale, writer);
    }

    private void writeWeekCsvAggregated(ReportWeek reportWeek, Locale locale, PrintWriter writer) {

        writer.println(csvLine(
            message("report.csv.header.calendarWeek", locale),
            message("report.csv.header.start", locale),
            message("report.csv.header.end", locale),
            message("report.csv.header.person.givenName", locale),
            message("report.csv.header.person.familyName", locale),
            message("report.csv.header.workedHours", locale),
            message("report.csv.header.shouldWorkingHours", locale)
        ));

        final NumberFormat numberFormat = numberFormat(locale);
        final String dateFrom = dateFormatter.formatDate(reportWeek.firstDateOfWeek());
        final String dateTo = dateFormatter.formatDate(reportWeek.lastDateOfWeek());
        final int calendarWeek = reportWeek.calenderWeek();

        for (User user : usersOrderedByName(reportWeek)) {
            final UserIdComposite userIdComposite = user.userIdComposite();
            final WorkDuration workDuration = reportWeek.workDurationByUser().getOrDefault(userIdComposite, WorkDuration.ZERO);
            final ShouldWorkingHours shouldWorkingHours = reportWeek.shouldWorkingHoursByUser().getOrDefault(userIdComposite, ShouldWorkingHours.ZERO);

            writer.println(csvLine(
                calendarWeek,
                dateFrom,
                dateTo,
                user.givenName(),
                user.familyName(),
                numberFormat.format(workDuration.hoursDoubleValue()),
                numberFormat.format(shouldWorkingHours.hoursDoubleValue())
            ));
        }
    }

    private void writeMonthCsvAggregated(ReportMonth reportMonth, Locale locale, PrintWriter writer) {

        writer.println(csvLine(
            message("report.csv.header.month", locale),
            message("report.csv.header.start", locale),
            message("report.csv.header.end", locale),
            message("report.csv.header.person.givenName", locale),
            message("report.csv.header.person.familyName", locale),
            message("report.csv.header.workedHours", locale),
            message("report.csv.header.shouldWorkingHours", locale)
        ));

        final NumberFormat numberFormat = numberFormat(locale);
        final YearMonth yearMonth = reportMonth.yearMonth();
        final String monthLabel = dateFormatter.formatYearMonth(yearMonth);
        final String dateFrom = dateFormatter.formatDate(yearMonth.atDay(1));
        final String dateTo = dateFormatter.formatDate(yearMonth.atEndOfMonth());

        for (User user : usersOrderedByName(reportMonth)) {
            final UserIdComposite userIdComposite = user.userIdComposite();
            final WorkDuration workDuration = reportMonth.workDurationByUser().getOrDefault(userIdComposite, WorkDuration.ZERO);
            final ShouldWorkingHours shouldWorkingHours = reportMonth.shouldWorkingHoursByUser().getOrDefault(userIdComposite, ShouldWorkingHours.ZERO);

            writer.println(csvLine(
                monthLabel,
                dateFrom,
                dateTo,
                user.givenName(),
                user.familyName(),
                numberFormat.format(workDuration.hoursDoubleValue()),
                numberFormat.format(shouldWorkingHours.hoursDoubleValue())
            ));
        }
    }

    private List<User> usersOrderedByName(HasWorkDurationByUser report) {

        final List<UserLocalId> userLocalIds = report.workDurationByUser().keySet().stream()
            .map(UserIdComposite::localId)
            .toList();

        return userManagementService.findAllUsersByLocalIds(userLocalIds).stream()
            .sorted(Comparator.comparing(User::familyName).thenComparing(User::givenName))
            .toList();
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, new Object[]{}, locale);
    }

    private static NumberFormat numberFormat(Locale locale) {
        final NumberFormat numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setMaximumFractionDigits(FRACTION_DIGITS);
        numberFormat.setMinimumFractionDigits(FRACTION_DIGITS);
        return numberFormat;
    }

    private static String csvLine(Object... values) {
        return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(";"));
    }
}
