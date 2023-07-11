package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCsvServiceTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    private ReportCsvService sut;

    @Mock
    private ReportService reportService;

    @Mock
    private DateFormatter dateFormatter;

    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {

        // return message key
        when(messageSource.getMessage(any(), any(), any()))
            .thenAnswer((Answer<String>) invocationOnMock -> invocationOnMock.getArgument(0));

        sut = new ReportCsvService(reportService, dateFormatter, messageSource);
    }

    // ------------------------------------------------------------
    // WEEK report csv
    // ------------------------------------------------------------

    @Test
    void ensureWeekReportCsvWithEmptyReport() {

        when(reportService.getReportWeek(Year.of(2021), 1, new UserId("batman")))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of()));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            """);
    }

    @Test
    void ensureWeekReportCsvRoundsWorkedHoursToTwoDigit() {

        mockDateFormatter("dd.MM.yyyy");

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime to = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 30), ZONE_ID_BERLIN);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, false);
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(reportDayEntry)), Map.of());

        when(reportService.getReportWeek(Year.of(2021), 1, new UserId("batman")))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDay)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;0,50;hard work;false
            """);
    }

    @Test
    void ensureWeekReportCsvContainsSummarizedInfoPerDay() {

        mockDateFormatter("dd.MM.yyyy");

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        // day one
        final ZonedDateTime d1_1_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d1_1_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 11, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d1_1_ReportDayEntry = new ReportDayEntry(batman, "hard work", d1_1_From, d1_1_To, false);
        final ZonedDateTime d1_2_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 14, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d1_2_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 15, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d1_2_ReportDayEntry = new ReportDayEntry(batman, "hard work", d1_2_From, d1_2_To, false);
        final ReportDay reportDayOne = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(d1_1_ReportDayEntry, d1_2_ReportDayEntry)), Map.of());

        // day two
        final ZonedDateTime d2_1_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 9, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d2_1_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 17, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d2_1_ReportDayEntry = new ReportDayEntry(batman, "hard work", d2_1_From, d2_1_To, false);
        final ReportDay reportDayTwo = new ReportDay(LocalDate.of(2021, 1, 5), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(d2_1_ReportDayEntry)), Map.of());


        when(reportService.getReportWeek(Year.of(2021), 1, new UserId("batman")))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDayOne, reportDayTwo)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;1,00;hard work;false
            04.01.2021;Bruce;Wayne;1,00;hard work;false
            05.01.2021;Bruce;Wayne;8,00;hard work;false
            """);
    }

    // ------------------------------------------------------------
    // MONTH report csv
    // ------------------------------------------------------------

    @Test
    void ensureMonthReportCsvWithEmptyReport() {

        when(reportService.getReportMonth(YearMonth.of(2021, 1), new UserId("batman")))
            .thenReturn(new ReportMonth(YearMonth.of(2021, 1), List.of()));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeMonthReportCsv(YearMonth.of(2021, 1), Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            """);
    }

    @Test
    void ensureMonthReportCsvRoundsWorkedHoursToTwoDigit() {

        mockDateFormatter("dd.MM.yyyy");

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime to = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 30), ZONE_ID_BERLIN);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, false);
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(reportDayEntry)), Map.of());

        final ReportWeek firstWeek = new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDay));
        final ReportWeek secondWeek = new ReportWeek(LocalDate.of(2021, 1, 4), List.of());
        final ReportWeek thirdWeek = new ReportWeek(LocalDate.of(2021, 1, 11), List.of());
        final ReportWeek fourthWeek = new ReportWeek(LocalDate.of(2021, 1, 18), List.of());
        final ReportWeek fifthWeek = new ReportWeek(LocalDate.of(2021, 1, 25), List.of());

        when(reportService.getReportMonth(YearMonth.of(2021, 1), new UserId("batman")))
            .thenReturn(new ReportMonth(YearMonth.of(2021, 1), List.of(firstWeek, secondWeek, thirdWeek, fourthWeek, fifthWeek)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeMonthReportCsv(YearMonth.of(2021, 1), Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;0,50;hard work;false
            """);
    }

    @Test
    void ensureMonthReportCsvContainsSummarizedInfoPerDay() {

        mockDateFormatter("dd.MM.yyyy");

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        // week one, day one
        final ZonedDateTime d1_1_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d1_1_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 11, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d1_1_ReportDayEntry = new ReportDayEntry(batman, "hard work", d1_1_From, d1_1_To, false);
        final ZonedDateTime d1_2_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 14, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d1_2_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 15, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d1_2_ReportDayEntry = new ReportDayEntry(batman, "hard work", d1_2_From, d1_2_To, false);
        final ReportDay w1_reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(d1_1_ReportDayEntry, d1_2_ReportDayEntry)), Map.of());

        // week two, day one
        final ZonedDateTime d2_1_From = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 9, 0), ZONE_ID_BERLIN);
        final ZonedDateTime d2_1_To = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 17, 0), ZONE_ID_BERLIN);
        final ReportDayEntry d2_1_ReportDayEntry = new ReportDayEntry(batman, "hard work", d2_1_From, d2_1_To, false);
        final ReportDay w2_reportDay = new ReportDay(LocalDate.of(2021, 1, 5), Map.of(batman.localId(), PlannedWorkingHours.EIGHT), Map.of(batman.localId(), List.of(d2_1_ReportDayEntry)), Map.of());

        final ReportWeek firstWeek = new ReportWeek(LocalDate.of(2020, 12, 28), List.of(w1_reportDay));
        final ReportWeek secondWeek = new ReportWeek(LocalDate.of(2021, 1, 4), List.of(w2_reportDay));
        final ReportWeek thirdWeek = new ReportWeek(LocalDate.of(2021, 1, 11), List.of());
        final ReportWeek fourthWeek = new ReportWeek(LocalDate.of(2021, 1, 18), List.of());
        final ReportWeek fifthWeek = new ReportWeek(LocalDate.of(2021, 1, 25), List.of());

        when(reportService.getReportMonth(YearMonth.of(2021, 1), new UserId("batman")))
            .thenReturn(new ReportMonth(YearMonth.of(2021, 1), List.of(firstWeek, secondWeek, thirdWeek, fourthWeek, fifthWeek)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeMonthReportCsv(YearMonth.of(2021, 1), Locale.GERMAN, new UserId("batman"), printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;1,00;hard work;false
            04.01.2021;Bruce;Wayne;1,00;hard work;false
            05.01.2021;Bruce;Wayne;8,00;hard work;false
            """);
    }

    private void mockDateFormatter(String datePattern) {
        when(dateFormatter.formatDate(any()))
            .thenAnswer(invocation -> DateTimeFormatter.ofPattern(datePattern).format(invocation.getArgument(0)));
    }
}
