package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@Service
class ReportCsvService {

    private final ReportService reportService;
    private final DateFormatter dateFormatter;
    private final MessageSource messageSource;

    ReportCsvService(ReportService reportService, DateFormatter dateFormatter, MessageSource messageSource) {
        this.reportService = reportService;
        this.dateFormatter = dateFormatter;
        this.messageSource = messageSource;
    }

    void writeWeekReportCsv(Year year, int week, Locale locale, UserId userId, PrintWriter writer) {
        final ReportWeek reportWeek = reportService.getReportWeek(year, week, userId);
        writeWeekCsv(reportWeek, locale, writer);
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
        writeHeader(locale, writer);
        writeWeek(reportWeek, locale, writer);
    }

    private void writeMonthCsv(ReportMonth reportMonth, YearMonth yearMonth, Locale locale, PrintWriter writer) {
        writeHeader(locale, writer);
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

    private void writeHeader(Locale locale, PrintWriter writer) {
        final String date = messageSource.getMessage("report.csv.header.date", new Object[]{}, locale);
        final String givenName = messageSource.getMessage("report.csv.header.person.givenName", new Object[]{}, locale);
        final String familyName = messageSource.getMessage("report.csv.header.person.familyName", new Object[]{}, locale);
        final String workedHours = messageSource.getMessage("report.csv.header.workedHours", new Object[]{}, locale);
        final String comment = messageSource.getMessage("report.csv.header.comment", new Object[]{}, locale);

        writer.println(String.format("%s;%s;%s;%s;%s", date, givenName, familyName, workedHours, comment));
    }

    private void writeWeek(ReportWeek reportWeek, Locale locale, PrintWriter writer) {

        final NumberFormat numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);

        reportWeek.reportDays()
            .stream()
            .map(ReportDay::reportDayEntries)
            .flatMap(List::stream)
            .map(reportDayEntry -> reportDayEntryToCsvLine(reportDayEntry, numberFormat))
            .forEach(writer::println);
    }

    private String reportDayEntryToCsvLine(ReportDayEntry reportDayEntry, NumberFormat numberFormat) {
        final String date = dateFormatter.formatDate(reportDayEntry.start().toLocalDate());
        final String givenName = reportDayEntry.user().givenName();
        final String familyName = reportDayEntry.user().familyName();
        final String hoursWorked = numberFormat.format(reportDayEntry.workDuration().minutes().hoursDoubleValue());
        final String comment = reportDayEntry.comment();

        return String.format("%s;%s;%s;%s;%s", date, givenName, familyName, hoursWorked, comment);
    }
}
