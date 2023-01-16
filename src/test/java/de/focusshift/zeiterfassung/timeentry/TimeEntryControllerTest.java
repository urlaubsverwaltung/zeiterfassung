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
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter, clock);
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYear() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(1L, new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), List.of(timeEntry));
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), List.of(timeEntryDay));
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

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(38, "formatted-2022-9-19", "formatted-2022-9-25", "00:30",
                List.of(new TimeEntryDayDto("formatted-2022-9-19", "00:30", List.of(expectedTimeEntryDto))));
        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 39, 2022, 37, expectedTimeEntryWeekDto, 1337);

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
        final TimeEntry timeEntry = new TimeEntry(1L, new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(LocalDate.of(2022, 9, 19), List.of(timeEntry));
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), List.of(timeEntryDay));
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

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(38, "formatted-2022-9-19", "formatted-2022-9-25", "00:30",
                List.of(new TimeEntryDayDto("formatted-2022-9-19", "00:30", List.of(expectedTimeEntryDto))));
        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 39, 2022, 37, expectedTimeEntryWeekDto, 42);

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
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 12, 27), List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(52, "formatted-date", "formatted-date", "00:00", List.of());
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
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 1, 3), List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 1)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(1, "formatted-date", "formatted-date", "00:00", List.of());
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
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2021, 1, 4), List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2021, 1)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(1, "formatted-date", "formatted-date", "00:00", List.of());
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
        final TimeEntry expectedTimeEntry = new TimeEntry(null, new UserId("batman"), "hard work", expectedStart, expectedEnd, false);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
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
        final TimeEntry expectedTimeEntry = new TimeEntry(null, new UserId("batman"), "hard work", expectedStart, expectedEnd, true);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
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
        final TimeEntry expectedTimeEntry = new TimeEntry(null, new UserId("batman"), "hard work", expectedStart, expectedEnd, false);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
    }

    @Test
    void ensureTimeEntryCreationError() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-09-22T00:00:00.00Z"), ZoneId.systemDefault());
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter, fixedClock);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 99);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 38)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(38, "formatted-date", "formatted-date", "00:00", List.of());
        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 39, 2022, 37, expectedTimeEntryWeekDto, 99);

        final ResultActions perform = perform(
            post("/timeentries")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/duration
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
        final TimeEntry expectedTimeEntry = new TimeEntry(1337L, new UserId("batman"), "hard work extended", expectedStart, expectedEnd, false);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
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
        final TimeEntry expectedTimeEntry = new TimeEntry(1337L, new UserId("batman"), "hard work extended", expectedStart, expectedEnd, true);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
    }

    @Test
    void ensureTimeEntryEditForAjax() throws Exception {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

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
            .duration(null)
            .comment("hard work extended")
            .build();

        perform
            .andExpect(model().attribute("turboEditedTimeEntry", expectedTimeEntryDto))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry expectedTimeEntry = new TimeEntry(1337L, new UserId("batman"), "hard work extended", expectedStart, expectedEnd, false);

        verify(timeEntryService).saveTimeEntry(expectedTimeEntry);
    }

    @Test
    void ensureTimeEntryEditWithValidationError() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.parse("2022-09-22T00:00:00.00Z"), ZoneId.systemDefault());
        sut = new TimeEntryController(timeEntryService, userSettingsProvider, dateFormatter, fixedClock);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(zoneIdBerlin);

        final ZonedDateTime expectedStart = ZonedDateTime.of(2022, 9, 22, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 9, 22, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry = new TimeEntry(1L, new UserId("batman"), "hack the planet", expectedStart, expectedEnd, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), List.of(new TimeEntryDay(LocalDate.of(2022, 9, 19), List.of(timeEntry))));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(new UserId("batman"), 2022, 38)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .date(LocalDate.of(2022, 9, 22))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(38, "formatted-date", "formatted-date", "00:30",
                List.of(new TimeEntryDayDto("formatted-date", "00:30", List.of(expectedTimeEntryDto))));
        final TimeEntryWeeksPageDto expectedPage = new TimeEntryWeeksPageDto(2022, 39, 2022, 37, expectedTimeEntryWeekDto, 1337);

        final ResultActions perform = perform(
            post("/timeentries/1337")
                .with(
                    oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))
                )
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/duration
        );

        perform
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryEditWithValidationErrorWithAjax() throws Exception {

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

        verifyNoInteractions(timeEntryService);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
