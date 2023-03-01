package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TimeEntryControllerTest {

    private TimeEntryController sut;

    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private DateFormatter dateFormatter;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter);
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYear() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntry));
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 38)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 19), MonthFormat.NONE, YearFormat.NONE)).thenReturn("formatted-2022-9-19");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 19), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-19");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 25), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-25");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 22))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .date("formatted-2022-9-19")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(38)
            .from("formatted-2022-9-19")
            .to("formatted-2022-9-25")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .days(List.of(expectedTimeEntryDayDto))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2022, 39, 2022, 37, expectedTimeEntryWeekDto, 1337);

        perform(
            get("/timeentries/2022/38")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWithTurboFrame() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntry));
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 42);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 38)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 19), MonthFormat.NONE, YearFormat.NONE)).thenReturn("formatted-2022-9-19");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 19), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-19");
        when(dateFormatter.formatDate(LocalDate.of(2022, 9, 25), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2022-9-25");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 22))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .date("formatted-2022-9-19")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(38)
            .from("formatted-2022-9-19")
            .to("formatted-2022-9-25")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .days(List.of(expectedTimeEntryDayDto))
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2022, 39, 2022, 37, expectedTimeEntryWeekDto, 42);

        perform(
            get("/timeentries/2022/38")
                .header("Turbo-Frame", "any-value")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2023, 1, 2022, 51, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2022/52")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 2, 2021, 52, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2022/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2021, 2, 2020, 53, expectedTimeEntryWeekDto, 0);

        perform(
            get("/timeentries/2021/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryCreation() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ResultActions perform = perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
        );

        perform
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 2, 15, 0, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, false);
    }

    @Test
    void ensureTimeEntryBreakCreation() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ResultActions perform = perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
                .param("break", "true")
        );

        perform
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 2, 15, 0, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, true);
    }

    @Test
    void ensureTimeEntryCreationForDateTouchingNextDay() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ResultActions perform = perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("start", "22:30:00.000+01:00")
                .param("end", "01:15:00.000+01:00")
                .param("comment", "hard work")
        );

        perform
            .andExpect(redirectedUrl("/timeentries"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 1, 2, 22, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 1, 3, 1, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "hard work", expectedStart, expectedEnd, false);
    }

    @Test
    void ensureTimeEntryCreationError() throws Exception {

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 99);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2021, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = TimeEntryWeekDto.builder()
            .calendarWeek(38)
            .from("formatted-date")
            .to("formatted-date")
            .hoursWorked("00:00")
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(
            2022, 1, 2021, 51, expectedTimeEntryWeekDto, 99);

        final ResultActions perform = perform(
            post("/timeentries")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/value
        );

        perform
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", expectedPage));
    }

    @Test
    void ensureTimeEntryEdit() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
        );

        perform
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "hard work extended", expectedStart, expectedEnd, Duration.ZERO, false);
    }

    @Test
    void ensureTimeEntryBreakEdit() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
                .param("break", "true")
        );

        perform
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "hard work extended", expectedStart, expectedEnd, Duration.ZERO, true);
    }

    @Test
    void ensureTimeEntryEditForAjax() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1337L), new UserId("batman"), "hard work extended", start, end, false);

        final ZonedDateTime startOtherDay = ZonedDateTime.of(2022, 9, 29, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime endOtherDay = ZonedDateTime.of(2022, 9, 29, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntryOtherDay = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet", startOtherDay, endOtherDay, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(
            LocalDate.of(2022, 9, 26),
            new PlannedWorkingHours(Duration.ofHours(16)),
            List.of(
                new TimeEntryDay(
                    LocalDate.of(2022, 9, 28),
                    PlannedWorkingHours.EIGHT,
                    List.of(timeEntry)
                ),
                new TimeEntryDay(
                    LocalDate.of(2022, 9, 29),
                    PlannedWorkingHours.EIGHT,
                    List.of(timeEntryOtherDay)
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
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
        );

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
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
            .hoursWorked("00:45")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:15")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(10.0)
            .timeEntries(List.of(expectedTimeEntryDto))
            .build();

        final TimeEntryDayDto otherDayDto = TimeEntryDayDto.builder()
            .date("formatted-2022-9-29")
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(otherTimeEntryDto))
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
            .andExpect(model().attribute("turboEditedTimeEntry", expectedTimeEntryDto))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);

        verify(timeEntryService).updateTimeEntry(new TimeEntryId(1337L), "hard work extended", expectedStart, expectedEnd, Duration.ZERO, false);
    }

    @Test
    void ensureTimeEntryEditWithValidationError() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntry));
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
            .hoursWorked("00:30")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:30")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(7.0)
            .timeEntries(List.of(expectedTimeEntryDto))
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

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/value
        );

        perform
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryEditWithValidationErrorWithAjax() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1337L), new UserId("batman"), "hard work extended", start, end, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 28), PlannedWorkingHours.EIGHT, List.of(timeEntry));
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 26), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 39)).thenReturn(timeEntryWeekPage);

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .header("Turbo-Frame", "any-value")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("id", "1337")
                .param("date", "2022-09-28")
                .param("start", "")
                .param("end", "")
                .param("comment", "hard work extended")
        );

        perform
            .andExpect(model().attributeDoesNotExist("timeEntryWeeks"))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        verifyNoMoreInteractions(timeEntryService);
    }

    @Test
    void ensureDelete() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1337L), new UserId("batman"), "hard work extended", start, end, false);

        when(timeEntryService.findTimeEntry(1337)).thenReturn(Optional.of(timeEntry));

        perform(
            post("/timeentries/1337")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("delete", "")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        verify(timeEntryService).deleteTimeEntry(1337);
    }

    @Test
    void ensureDeleteWithAjax() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final UserId userId = new UserId("batman");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userId, "hard work", start, end, false);

        final ZonedDateTime start2 = ZonedDateTime.of(2022, 9, 28, 16, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end2 = ZonedDateTime.of(2022, 9, 28, 17, 30, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry2 = new TimeEntry(new TimeEntryId(2L), userId, "hack the planet", start2, end2, false);

        when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(timeEntry));

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 28), PlannedWorkingHours.EIGHT, List.of(timeEntry2));
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
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final UserId userId = new UserId("batman");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userId, "hard work", start, end, false);

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
            .hoursWorkedShould("08:00")
            .hoursDelta("08:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(0)
            .days(List.of())
            .build();

        perform(
            post("/timeentries/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("delete", "")
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", nullValue()))
            .andExpect(model().attribute("turboDeletedTimeEntry", expectedDeletedTimeEntryDto));

        verify(timeEntryService).deleteTimeEntry(1);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
