package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.YELLOW;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TimeEntryControllerTest implements ControllerTest {

    private TimeEntryController sut;

    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private DateFormatter dateFormatter;

    private Clock clock = Clock.systemUTC();

    private AuthenticationFacade authenticationFacade;
    private TimeEntryViewHelper timeEntryViewHelper;

    @BeforeEach
    void setUp() {
        authenticationFacade = new AuthenticationFacade();
        timeEntryViewHelper = new TimeEntryViewHelper(timeEntryService, userSettingsProvider, authenticationFacade);
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter, timeEntryViewHelper, clock);
    }

    @Test
    void ensureTimeEntriesDefaultShowsCurrentWeek() throws Exception {

        clock = Clock.fixed(Instant.parse("2025-02-28T15:03:00.00Z"), ZoneOffset.UTC);
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter, timeEntryViewHelper, clock);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime expectedStart = ZonedDateTime.parse("2025-02-28T14:30:00.00Z");
        final ZonedDateTime expectedEnd = ZonedDateTime.parse("2025-02-28T15:00:00.00Z");
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", expectedStart, expectedEnd, false);

        final LocalDate timeEntryDate = LocalDate.parse("2025-02-28");
        final LocalDate firstDateOfWeek = LocalDate.parse("2025-02-24");

        final TimeEntryDay timeEntryDay = new TimeEntryDay(timeEntryDate, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(firstDateOfWeek, PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(userId, 2025, 9)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(LocalDate.class), eq(MonthFormat.STRING), eq(YearFormat.FULL))).thenReturn("formatted-date-year-full");
        when(dateFormatter.formatDate(any(LocalDate.class), eq(MonthFormat.STRING), eq(YearFormat.NONE))).thenReturn("formatted-date");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(timeEntryDate)
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .date("formatted-date-year-full")
            .dayOfWeek(DayOfWeek.FRIDAY)
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(9)
            .from("formatted-date")
            .to("formatted-date-year-full")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .days(List.of(expectedTimeEntryDayDto))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2025, 10, 2025, 8, expectedTimeEntryWeekDto, 1337);

        perform(
            get("/timeentries")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYear() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        // monday     2024-12-30
        // tuesday    2024-12-31  work
        // wednesday  2025-01-01
        // thursday   2025-01-02  absent
        // friday     2025-01-03

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final ZonedDateTime expectedStart = ZonedDateTime.of(2024, 12, 31, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2024, 12, 31, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", expectedStart, expectedEnd, false);
        final Instant absenceStart = ZonedDateTime.of(2025, 1, 3, 14, 30, 0, 0, zoneIdBerlin).toInstant();
        final Instant absenceEnd = ZonedDateTime.of(2025, 1, 3, 15, 0, 0, 0, zoneIdBerlin).toInstant();
        final Absence absence = new Absence(new UserId("batman"), absenceStart, absenceEnd, DayLength.FULL, locale -> "", YELLOW, HOLIDAY);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2024, 12, 31), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryDay timeEntryDayWithAbsence = new TimeEntryDay(LocalDate.of(2025, 1, 2), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(), List.of(absence));
        final TimeEntryDay timeEntryDayWithoutTimeEntriesAndWithoutAbsences = new TimeEntryDay(LocalDate.of(2025, 1, 3), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2024, 12, 30), PlannedWorkingHours.EIGHT, List.of(timeEntryDay, timeEntryDayWithAbsence, timeEntryDayWithoutTimeEntriesAndWithoutAbsences));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2025, 1)).thenReturn(timeEntryWeekPage);

        // week from to
        when(dateFormatter.formatDate(LocalDate.of(2024, 12,30), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-30");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1,5), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-05");

        // time entry
        when(dateFormatter.formatDate(LocalDate.of(2024, 12,31) , MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-31");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1,2) , MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-02");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2024, 12, 31))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDtoTUESDAY = TimeEntryDayDto.builder()
            .date("formatted-2024-12-31")
            .dayOfWeek(DayOfWeek.TUESDAY)
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final AbsenceEntryDto absenceDto = new AbsenceEntryDto(LocalDate.of(2025,1,2), "", YELLOW);
        final TimeEntryDayDto expectedTimeEntryDayDtoTHURSDAY = TimeEntryDayDto.builder()
            .date("formatted-2025-01-02")
            .dayOfWeek(DayOfWeek.THURSDAY)
            .hoursWorked("00:00")
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0.0)
            .timeEntries(List.of())
            .absenceEntries(List.of(absenceDto))
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(1)
            .from("formatted-2024-12-30")
            .to("formatted-2025-01-05")
            .hoursWorked("00:30")
            .hoursWorkedShould("24:00")
            .hoursDelta("23:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(3.0)
            .days(List.of(expectedTimeEntryDayDtoTUESDAY, expectedTimeEntryDayDtoTHURSDAY))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2025, 2, 2024, 52, expectedTimeEntryWeekDto, 1337);

        perform(
            get("/timeentries/2025/1")
                .with(oidcSubject("batman"))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWithTurboFrame() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final ZonedDateTime expectedStart = ZonedDateTime.of(2024, 12, 31, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2024, 12, 31, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2024, 12, 31), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2024, 12, 30), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 42);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2025, 1)).thenReturn(timeEntryWeekPage);

        // week from to
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 30), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-30");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1, 5), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-05");

        // time entry
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 31), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-31");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2024, 12, 31))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .date("formatted-2024-12-31")
            .dayOfWeek(DayOfWeek.TUESDAY)
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(1)
            .from("formatted-2024-12-30")
            .to("formatted-2025-01-05")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .days(List.of(expectedTimeEntryDayDto))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2025, 2, 2024, 52, expectedTimeEntryWeekDto, 42);

        perform(
            get("/timeentries/2025/01")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject("batman"))
        )
            .andExpect(view().name("timeentries/index::#frame-time-entry-weeks"))
            .andExpect(model().attribute("turboStreamsEnabled", is(true)))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenNextYearWouldBeReachedWithLoadMore() throws Exception {
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 12, 27), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(52)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("00:00")
            .hoursDelta("00:00")
            .hoursDeltaNegative(false)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2023, 1, 2022, 51, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2022/52")
                .with(oidcSubject("batman"))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenPreviousYearWouldBeReachedWithShowPastWith52() throws Exception {
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 1, 3), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 1)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(1)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("00:00")
            .hoursDelta("00:00")
            .hoursDeltaNegative(false)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 2, 2021, 52, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2022/1")
                .with(oidcSubject("batman"))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenPreviousYearWouldBeReachedWithShowPastWith53() throws Exception {
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2021, 1)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(1)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("00:00")
            .hoursDelta("00:00")
            .hoursDeltaNegative(false)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2021, 2, 2020, 53, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2021/1")
                .with(oidcSubject("batman"))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryCreation() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject("batman")
            )
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 2, 15, 0, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, false);
    }

    @Test
    void ensureTimeEntryBreakCreation() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject("batman")
            )
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
                .param("break", "true")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 2, 15, 0, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, true);
    }

    @Test
    void ensureTimeEntryCreationForDateTouchingNextDay() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject("batman")
            )
                .param("date", "2022-01-02")
                .param("start", "22:30:00.000+01:00")
                .param("end", "01:15:00.000+01:00")
                .param("comment", "hard work")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 22, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 3, 1, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, false);
    }

    @Test
    void ensureTimeEntryCreationError() throws Exception {

        mockUserSettings(DayOfWeek.MONDAY);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 99);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2021, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(38)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("00:00")
            .hoursDelta("00:00")
            .hoursDeltaNegative(false)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2022, 1, 2021, 51, expectedTimeEntryWeekDto, 99);

        perform(
            post("/timeentries")
                .with(oidcSubject("batman")
            )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
                 // missing start/end/value
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", expectedPage));
    }

    @Test
    void ensureTimeEntryEditStartGivenAndEndGivenAndDurationNull() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        when(timeEntryService.findTimeEntry(1337))
            .thenReturn(Optional.of(anyTimeEntry(new UserId("batman"))));

        perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
        )
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "hard work extended", expectedStart, expectedEnd, Duration.ZERO, false);
    }

    @Test
    void ensureTimeEntryEditStartNullAndEndGivenAndDurationGiven() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        when(timeEntryService.findTimeEntry(1337))
            .thenReturn(Optional.of(anyTimeEntry(new UserId("batman"))));

        perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "")
                .param("end", "21:15:00.000+01:00")
                .param("duration", "01:00")
                .param("comment", "")
        )
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "", null, expectedEnd, Duration.ofHours(1), false);
    }

    @Test
    void ensureTimeEntryEditStartGivenAndEndNullAndDurationGiven() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        when(timeEntryService.findTimeEntry(1337))
            .thenReturn(Optional.of(anyTimeEntry(new UserId("batman"))));

        perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "21:15:00.000+01:00")
                .param("end", "")
                .param("duration", "01:00")
                .param("comment", "")
        )
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "", expectedStart, null, Duration.ofHours(1), false);
    }

    @Test
    void ensureTimeEntryBreakEdit() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        final UserId userId = new UserId("batman");
        when(timeEntryService.findTimeEntry(1337L)).thenReturn(Optional.of(anyTimeEntry(userId)));

        perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
                .param("break", "true")
        )
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "hard work extended", expectedStart, expectedEnd, Duration.ZERO, true);
    }

    @Test
    void ensureTimeEntryEditForAjax() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(zoneIdBerlin, DayOfWeek.MONDAY);

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntryId timeEntryId = new TimeEntryId(1337L);
        final TimeEntry timeEntryToEdit = new TimeEntry(timeEntryId, userIdComposite, "hard work extended", start, end, false);

        when(timeEntryService.findTimeEntry(1337L)).thenReturn(Optional.of(timeEntryToEdit));

        final ZonedDateTime startOtherDay = ZonedDateTime.of(2022, 9, 29, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime endOtherDay = ZonedDateTime.of(2022, 9, 29, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntryOtherDay = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", startOtherDay, endOtherDay, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(
            LocalDate.of(2022, 9, 26),
            new PlannedWorkingHours(Duration.ofHours(16)),
            List.of(
                new TimeEntryDay(
                    LocalDate.of(2022, 9, 28),
                    PlannedWorkingHours.EIGHT,
                    ShouldWorkingHours.EIGHT,
                    List.of(timeEntryToEdit),
                    List.of()
                ),
                new TimeEntryDay(
                    LocalDate.of(2022, 9, 29),
                    PlannedWorkingHours.EIGHT,
                    ShouldWorkingHours.EIGHT,
                    List.of(timeEntryOtherDay),
                    List.of()
                )
            )
        );

        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 39)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 26), MonthFormat.STRING, YearFormat.NONE)).thenReturn("formatted-2022-9-26");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 28), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-28");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 29), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-29");
        when(dateFormatter.formatDate(LocalDate.of(2022, 10, 2), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-10-2");

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
        );

        final TimeEntryDTO expectedUpdatedTimeEntryDto = TimeEntryDTO.builder()
            .id(1337L)
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(20, 30))
            .end(LocalTime.of(21, 15))
            .duration("00:45")
            .comment("hard work extended")
            .build();

        final TimeEntryDTO otherTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 29))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedDayDto = TimeEntryDayDto.builder()
            .date("formatted-2022-9-28")
            .dayOfWeek(DayOfWeek.WEDNESDAY)
            .hoursWorked("00:45")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:15")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(10.0)
            .timeEntries(List.of(expectedUpdatedTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final TimeEntryDayDto otherDayDto = TimeEntryDayDto.builder()
            .date("formatted-2022-9-29")
            .dayOfWeek(DayOfWeek.THURSDAY)
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(otherTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final TimeEntryWeekDto expectedWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(39)
            .from("formatted-2022-9-26")
            .to("formatted-2022-10-2")
            .hoursWorked("01:15")
            .hoursWorkedShould("16:00")
            .hoursDelta("14:45")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(8.0)
            .days(List.of(expectedDayDto, otherDayDto))
            .build();

        perform
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", expectedDayDto))
            .andExpect(model().attribute("turboEditedTimeEntry", expectedUpdatedTimeEntryDto))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(timeEntryId, "hard work extended", expectedStart, expectedEnd, Duration.ZERO, false);
    }

    @Test
    void ensureTimeEntryEditWithValidationError() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(DayOfWeek.MONDAY);

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntryId timeEntryId = new TimeEntryId(1337L);
        final TimeEntry timeEntry = new TimeEntry(timeEntryId, userIdComposite, "hack the planet", expectedStart, expectedEnd, false);

        when(timeEntryService.findTimeEntry(1337)).thenReturn(Optional.of(timeEntry));

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2021, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 22))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto timeEntryDayDto = TimeEntryDayDto.builder()
            .date("formatted-date")
            .dayOfWeek(DayOfWeek.MONDAY)
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .absenceEntries(List.of())
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(38)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .days(List.of(timeEntryDayDto))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 1, 2021, 51, expectedTimeEntryWeekDto, 1337);

        perform(
            post("/timeentries/1337")
                .with(oidcSubject("batman")
            )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/value
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryEditWithValidationErrorWithAjax() throws Exception {

        mockUserSettings(DayOfWeek.MONDAY);

        when(timeEntryService.findTimeEntry(1337))
            .thenReturn(Optional.of(anyTimeEntry(new UserId("batman"))));

        perform(
            post("/timeentries/1337")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject("batman")
            )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "")
                .param("end", "")
                .param("comment", "hard work extended")
        )
            .andExpect(model().attributeDoesNotExist("timeEntryWeeks"))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        verifyNoMoreInteractions(timeEntryService);
    }

    @Test
    void ensureDelete() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1337L), userIdComposite, "hard work extended", start, end, false);

        when(timeEntryService.findTimeEntry(1337)).thenReturn(Optional.of(timeEntry));

        perform(
            post("/timeentries/1337")
                .with(oidcSubject("batman")
            )
                .param("delete", "")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        verify(timeEntryService).deleteTimeEntry(1337);
    }

    @Test
    void ensureDeleteWithAjax() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", start, end, false);

        final ZonedDateTime start2 = ZonedDateTime.of(2022, 9, 28, 16, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end2 = ZonedDateTime.of(2022, 9, 28, 17, 30, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry2 = new TimeEntry(new TimeEntryId(2L), userIdComposite, "hack the planet", start2, end2, false);

        when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(timeEntry));

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 28), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry2), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 26), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userId, 2022, 39)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedDeletedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(20, 30))
            .end(LocalTime.of(21, 15))
            .duration("00:45")
            .comment("hard work")
            .build();

        final TimeEntryDayDto expectedDayDto = TimeEntryDayDto.builder()
            .date("formatted-date")
            .dayOfWeek(DayOfWeek.WEDNESDAY)
            .hoursWorked("01:00")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(13.0)
            .timeEntries(List.of(
                TimeEntryDTO.builder()
                    .id(2L)
                    .date(LocalDate.of(2022, 9, 28))
                    .start(LocalTime.of(16, 30))
                    .end(LocalTime.of(17, 30))
                    .duration("01:00")
                    .comment("hack the planet")
                    .build()
            ))
            .absenceEntries(List.of())
            .build();

        final TimeEntryWeekDto expectedWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(39)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("01:00")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(13.0)
            .days(List.of(expectedDayDto))
            .build();

        perform(
            post("/timeentries/1")
                .with(oidcSubject("batman")
            )
                .param("delete", "")
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", expectedDayDto))
            .andExpect(model().attribute("turboDeletedTimeEntry", expectedDeletedTimeEntryDto));

        verify(timeEntryService).deleteTimeEntry(1);
    }

    @Test
    void ensureDeleteWithAjaxLastTimeEntryOfDay() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", start, end, false);

        when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(timeEntry));

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 26), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userId, 2022, 39)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedDeletedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(20, 30))
            .end(LocalTime.of(21, 15))
            .duration("00:45")
            .comment("hard work")
            .build();

        final TimeEntryWeekDto expectedWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(39)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("00:00")
            .hoursDelta("00:00")
            .hoursDeltaNegative(false)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        perform(
            post("/timeentries/1")
                .with(oidcSubject("batman")
            )
                .param("delete", "")
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", nullValue()))
            .andExpect(model().attribute("turboDeletedTimeEntry", expectedDeletedTimeEntryDto));

        verify(timeEntryService).deleteTimeEntry(1);
    }

    private UserIdComposite anyUserIdComposite() {
        return anyUserIdComposite(new UserId("any-user-id"));
    }

    private UserIdComposite anyUserIdComposite(UserId userId) {
        return new UserIdComposite(userId, new UserLocalId(1L));
    }

    private TimeEntry anyTimeEntry(UserId userId) {
        return anyTimeEntry(new TimeEntryId(1L), userId);
    }

    private TimeEntry anyTimeEntry(TimeEntryId id, UserId userId) {

        final UserIdComposite userIdComposite = anyUserIdComposite(userId);
        final ZonedDateTime start = ZonedDateTime.now(clock).minusHours(1);
        final ZonedDateTime end = ZonedDateTime.now(clock);

        return new TimeEntry(id, userIdComposite, "", start, end, false);
    }

    private void mockUserSettings(ZoneId zoneId) {
        when(userSettingsProvider.zoneId()).thenReturn(zoneId);
    }

    private void mockUserSettings(DayOfWeek dayOfWeek) {
        when(userSettingsProvider.firstDayOfWeek()).thenReturn(dayOfWeek);
    }

    private void mockUserSettings(ZoneId zoneId, DayOfWeek dayOfWeek) {
        mockUserSettings(zoneId);
        mockUserSettings(dayOfWeek);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
