package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
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

        final UserLocalId userLocalId = new UserLocalId(1L);

        when(reportService.getReportWeek(Year.of(2021), 1, List.of(userLocalId)))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of()));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, userLocalId, printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            """);
    }

    @Test
    void ensureWeekReportCsvRoundsWorkedHoursToTwoDigit() {

        mockDateFormatter("dd.MM.yyyy");

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT),
                Map.of()
            )
        );

        final ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime to = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 30), ZONE_ID_BERLIN);
        final WorkDuration workDuration = new WorkDuration(Duration.ofMinutes(30));
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, batman, "hard work", from, to, workDuration, false);
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        when(reportService.getReportWeek(Year.of(2021), 1, List.of(batmanLocalId)))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDay)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, batmanLocalId, printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;10:00;10:30;0,500;hard work;false
            """);
    }

    @Test
    void ensureWeekReportCsvContainsSummarizedInfoPerDay() {

        mockDateFormatter("dd.MM.yyyy");

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(
                    LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2021, 1, 5), PlannedWorkingHours.EIGHT
                ),
                Map.of()
            )
        );

        // day one
        final ZonedDateTime dayOneFirstFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayOneFirstTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 11, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayOneFirstReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayOneFirstFrom, dayOneFirstTo, new WorkDuration(Duration.ofHours(1)), false);
        final ZonedDateTime dayOneSecondFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 14, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayOneSecondTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 15, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayOneSecondReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayOneSecondFrom, dayOneSecondTo, new WorkDuration(Duration.ofHours(1)), false);
        final ReportDay reportDayOne = new ReportDay(LocalDate.of(2021, 1, 4), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(dayOneFirstReportDayEntry, dayOneSecondReportDayEntry)), Map.of());

        // day two
        final ZonedDateTime dayTwoFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 9, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayTwoTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 17, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayTwoReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayTwoFrom, dayTwoTo, new WorkDuration(Duration.ofHours(8)), false);
        final ReportDay reportDayTwo = new ReportDay(LocalDate.of(2021, 1, 5), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(dayTwoReportDayEntry)), Map.of());

        when(reportService.getReportWeek(Year.of(2021), 1, List.of(batmanLocalId)))
            .thenReturn(new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDayOne, reportDayTwo)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeWeekReportCsv(Year.of(2021), 1, Locale.GERMAN, batmanLocalId, printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;10:00;11:00;1,000;hard work;false
            04.01.2021;Bruce;Wayne;14:00;15:00;1,000;hard work;false
            05.01.2021;Bruce;Wayne;09:00;17:00;8,000;hard work;false
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
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            """);
    }

    @Test
    void ensureMonthReportCsvRoundsWorkedHoursToTwoDigit() {

        mockDateFormatter("dd.MM.yyyy");

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT),
                Map.of()
            )
        );

        final ZonedDateTime from = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime to = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 30), ZONE_ID_BERLIN);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, batman, "hard work", from, to, new WorkDuration(Duration.ofMinutes(30)), false);
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        final ReportWeek firstWeek = new ReportWeek(LocalDate.of(2020, 12, 28), List.of(reportDay));
        final ReportWeek secondWeek = new ReportWeek(LocalDate.of(2021, 1, 4), List.of());
        final ReportWeek thirdWeek = new ReportWeek(LocalDate.of(2021, 1, 11), List.of());
        final ReportWeek fourthWeek = new ReportWeek(LocalDate.of(2021, 1, 18), List.of());
        final ReportWeek fifthWeek = new ReportWeek(LocalDate.of(2021, 1, 25), List.of());

        when(reportService.getReportMonth(YearMonth.of(2021, 1), batmanId))
            .thenReturn(new ReportMonth(YearMonth.of(2021, 1), List.of(firstWeek, secondWeek, thirdWeek, fourthWeek, fifthWeek)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeMonthReportCsv(YearMonth.of(2021, 1), Locale.GERMAN, batmanId, printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;10:00;10:30;0,500;hard work;false
            """);
    }

    @Test
    void ensureMonthReportCsvContainsSummarizedInfoPerDay() {

        mockDateFormatter("dd.MM.yyyy");

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(
                    LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2021, 1, 5), PlannedWorkingHours.EIGHT
                ),
                Map.of()
            )
        );

        // week one, day one
        final ZonedDateTime dayOneFirstFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 10, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayOneFirstTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 11, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayOneFirstReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayOneFirstFrom, dayOneFirstTo, new WorkDuration(Duration.ofHours(1)), false);
        final ZonedDateTime dayOneSecondFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 14, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayOneSecondTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 4, 15, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayOneSecondReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayOneSecondFrom, dayOneSecondTo, new WorkDuration(Duration.ofHours(1)), false);
        final ReportDay weekOneReportDay = new ReportDay(LocalDate.of(2021, 1, 4), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(dayOneFirstReportDayEntry, dayOneSecondReportDayEntry)), Map.of());

        // week two, day one
        final ZonedDateTime dayTwoFrom = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 9, 0), ZONE_ID_BERLIN);
        final ZonedDateTime dayTwoTo = ZonedDateTime.of(LocalDateTime.of(2021, 1, 5, 17, 0), ZONE_ID_BERLIN);
        final ReportDayEntry dayTwoReportDayEntry = new ReportDayEntry(null, batman, "hard work", dayTwoFrom, dayTwoTo, new WorkDuration(Duration.ofHours(8)), false);
        final ReportDay weekTwoReportDay = new ReportDay(LocalDate.of(2021, 1, 5), false, workingTimeCalendarByUser, Map.of(batmanIdComposite, List.of(dayTwoReportDayEntry)), Map.of());

        final ReportWeek firstWeek = new ReportWeek(LocalDate.of(2020, 12, 28), List.of(weekOneReportDay));
        final ReportWeek secondWeek = new ReportWeek(LocalDate.of(2021, 1, 4), List.of(weekTwoReportDay));
        final ReportWeek thirdWeek = new ReportWeek(LocalDate.of(2021, 1, 11), List.of());
        final ReportWeek fourthWeek = new ReportWeek(LocalDate.of(2021, 1, 18), List.of());
        final ReportWeek fifthWeek = new ReportWeek(LocalDate.of(2021, 1, 25), List.of());

        when(reportService.getReportMonth(YearMonth.of(2021, 1), batmanId))
            .thenReturn(new ReportMonth(YearMonth.of(2021, 1), List.of(firstWeek, secondWeek, thirdWeek, fourthWeek, fifthWeek)));

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        sut.writeMonthReportCsv(YearMonth.of(2021, 1), Locale.GERMAN, batmanId, printWriter);

        assertThat(stringWriter).hasToString("""
            report.csv.header.date;report.csv.header.person.givenName;report.csv.header.person.familyName;report.csv.header.start;report.csv.header.end;report.csv.header.workedHours;report.csv.header.comment;report.csv.header.break
            04.01.2021;Bruce;Wayne;10:00;11:00;1,000;hard work;false
            04.01.2021;Bruce;Wayne;14:00;15:00;1,000;hard work;false
            05.01.2021;Bruce;Wayne;09:00;17:00;8,000;hard work;false
            """);
    }

    private void mockDateFormatter(String datePattern) {
        when(dateFormatter.formatDate(any()))
            .thenAnswer(invocation -> DateTimeFormatter.ofPattern(datePattern).format(invocation.getArgument(0)));
    }
}
