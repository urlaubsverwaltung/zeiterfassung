package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import java.util.Set;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.YELLOW;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private DateFormatter dateFormatter;
    @Mock
    private TimeEntryViewHelper timeEntryViewHelper;

    private Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new TimeEntryController(timeEntryService, userManagementService, userSettingsProvider, timeEntryLockService, dateFormatter, timeEntryViewHelper, clock);
    }

    @Test
    void ensureTimeEntriesDefaultShowsCurrentWeek() throws Exception {

        clock = Clock.fixed(Instant.parse("2025-02-28T15:03:00.00Z"), ZoneOffset.UTC);
        sut = new TimeEntryController(timeEntryService, userManagementService, userSettingsProvider, timeEntryLockService, dateFormatter, timeEntryViewHelper, clock);

        mockUserSettings(ZoneOffset.UTC);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime expectedStart = ZonedDateTime.parse("2025-02-28T14:30:00.00Z");
        final ZonedDateTime expectedEnd = ZonedDateTime.parse("2025-02-28T15:00:00.00Z");
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", expectedStart, expectedEnd, false);

        final LocalDate timeEntryDate = LocalDate.parse("2025-02-28");
        final LocalDate firstDateOfWeek = LocalDate.parse("2025-02-24");

        final TimeEntryDay timeEntryDay = new TimeEntryDay(false, timeEntryDate, new WorkDuration(Duration.ofMinutes(30)), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(firstDateOfWeek, PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2025, 9)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(LocalDate.class), eq(MonthFormat.STRING), eq(YearFormat.FULL))).thenReturn("formatted-date-year-full");
        when(dateFormatter.formatDate(any(LocalDate.class), eq(MonthFormat.STRING), eq(YearFormat.NONE))).thenReturn("formatted-date");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
            .date(timeEntryDate)
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntry)).thenReturn(expectedTimeEntryDto);

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
        final Absence absence = new Absence(userId, absenceStart, absenceEnd, DayLength.FULL, locale -> "", YELLOW, HOLIDAY);

        final TimeEntryDay timeEntryDay = new TimeEntryDay(false, LocalDate.of(2024, 12, 31), new WorkDuration(Duration.ofMinutes(30)), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryDay timeEntryDayWithAbsence = new TimeEntryDay(false, LocalDate.of(2025, 1, 2), WorkDuration.ZERO, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(), List.of(absence));
        final TimeEntryDay timeEntryDayWithoutTimeEntriesAndWithoutAbsences = new TimeEntryDay(false, LocalDate.of(2025, 1, 3), WorkDuration.ZERO, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2024, 12, 30), PlannedWorkingHours.EIGHT, List.of(timeEntryDay, timeEntryDayWithAbsence, timeEntryDayWithoutTimeEntriesAndWithoutAbsences));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2025, 1)).thenReturn(timeEntryWeekPage);

        // week from to
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 30), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-30");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1, 5), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-05");

        // time entry
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 31), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-31");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1, 2), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-02");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2024, 12, 31))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDtoTUESDAY = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
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

        final AbsenceEntryDto absenceDto = new AbsenceEntryDto(LocalDate.of(2025, 1, 2), "", YELLOW);
        final TimeEntryDayDto expectedTimeEntryDayDtoTHURSDAY = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntry)).thenReturn(expectedTimeEntryDto);

        perform(
            get("/timeentries").queryParam("year", "2025").queryParam("week", "1")
                .with(oidcSubject(userIdComposite))
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

        final TimeEntryDay timeEntryDay = new TimeEntryDay(false, LocalDate.of(2024, 12, 31), new WorkDuration(Duration.ofMinutes(30)), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2024, 12, 30), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 42);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2025, 1)).thenReturn(timeEntryWeekPage);

        // week from to
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 30), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-30");
        when(dateFormatter.formatDate(LocalDate.of(2025, 1, 5), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2025-01-05");

        // time entry
        when(dateFormatter.formatDate(LocalDate.of(2024, 12, 31), MonthFormat.STRING, YearFormat.FULL)).thenReturn("formatted-2024-12-31");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2024, 12, 31))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedTimeEntryDayDto = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntry)).thenReturn(expectedTimeEntryDto);

        perform(
            get("/timeentries").queryParam("year", "2025").queryParam("week", "01")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(view().name("timeentries/index::#frame-time-entry-weeks"))
            .andExpect(model().attribute("turboStreamsEnabled", is(true)))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));

        final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setUserLocalId(userIdComposite.localId().value());
        verify(timeEntryViewHelper).addTimeEntryToModel(any(Model.class), eq(timeEntryDTO));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenNextYearWouldBeReachedWithLoadMore() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 12, 27), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2022, 52)).thenReturn(timeEntryWeekPage);

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
            get("/timeentries").queryParam("year", "2022").queryParam("week", "52")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenPreviousYearWouldBeReachedWithShowPastWith52() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 1, 3), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2022, 1)).thenReturn(timeEntryWeekPage);

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
            get("/timeentries").queryParam("year", "2022").queryParam("week", "1")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForYearAndWeekOfYearWhenPreviousYearWouldBeReachedWithShowPastWith53() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2021, 1, 4), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2021, 1)).thenReturn(timeEntryWeekPage);

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
            get("/timeentries").queryParam("year", "2021").queryParam("week", "1")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attributeDoesNotExist("turboStreamsEnabled"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntriesForOtherUserIsForbidden() {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId("joker");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        assertThatThrownBy(() -> perform(get("/timeentries/users/1337").with(oidcSubject(userIdComposite))))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(AccessDeniedException.class)
            .hasRootCauseMessage("You are not allowed to access timeentries of user 1337");
    }

    @Test
    void ensureTimeEntriesForOtherUser() throws Exception {

        clock = Clock.fixed(Instant.parse("2025-03-18T10:04:00.00Z"), ZoneOffset.UTC);
        sut = new TimeEntryController(timeEntryService, userManagementService, userSettingsProvider, timeEntryLockService, dateFormatter, timeEntryViewHelper, clock);

        final int year = 2025;
        final int weekOfYear = 12;

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId("batman");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserLocalId ownerLocalId = new UserLocalId(2L);
        final UserId ownerId = new UserId("joker");
        final UserIdComposite ownerIdComposite = new UserIdComposite(ownerId, ownerLocalId);

        mockUserSettings(ZoneOffset.UTC);

        final User owner = anyUser(ownerIdComposite);
        when(userManagementService.findUserByLocalId(ownerLocalId)).thenReturn(Optional.of(owner));

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.parse("2025-03-17"), PlannedWorkingHours.ZERO, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear)).thenReturn(timeEntryWeekPage);

        final ViewedUserDto expectedViewedUserDto = new ViewedUserDto(ownerLocalId.value(), owner.givenName(), owner.familyName(), owner.fullName(), owner.initials(), owner.email().value());
        final TimeEntryDTO expectedTimeEntryDto = new TimeEntryDTO();
        expectedTimeEntryDto.setUserLocalId(ownerLocalId.value());

        final TimeEntryWeekDto expectedTimeEntryWeekDto = new TimeEntryWeekDto(weekOfYear, null, null, "00:00", "00:00", "00:00", false, 0.0, List.of());
        final TimeEntryWeeksPageDto expetectedWeeksPageDto = new TimeEntryWeeksPageDto(year, weekOfYear + 1, year, weekOfYear - 1, expectedTimeEntryWeekDto, 0);

        perform(get("/timeentries/users/2")
            .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)))
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("viewedUser", expectedViewedUserDto))
            .andExpect(model().attribute("timeEntryWeeksPage", expetectedWeeksPageDto))
            .andExpect(view().name("timeentries/index"));

        verify(timeEntryViewHelper).addTimeEntryToModel(any(Model.class), eq(expectedTimeEntryDto));
    }

    @Test
    void ensureTimeEntryCreation() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockUserSettings(DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject(userIdComposite)
                )
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final TimeEntryDTO expectedTimeEntryDto = new TimeEntryDTO();
        expectedTimeEntryDto.setUserLocalId(userLocalId.value());
        expectedTimeEntryDto.setDate(LocalDate.parse("2022-01-02"));
        expectedTimeEntryDto.setStart(LocalTime.parse("14:30"));
        expectedTimeEntryDto.setEnd(LocalTime.parse("15:00"));
        expectedTimeEntryDto.setComment("hard work");

        verify(timeEntryViewHelper).createTimeEntry(eq(expectedTimeEntryDto), any(BindingResult.class), any(CurrentOidcUser.class));
    }

    @Test
    void ensureTimeEntryBreakCreation() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockUserSettings(DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject(userIdComposite)
                )
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-01-02")
                .param("start", "14:30:00.000+01:00")
                .param("end", "15:00:00.000+01:00")
                .param("comment", "hard work")
                .param("break", "true")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setId(1337L);
        timeEntryDto.setUserLocalId(userIdComposite.localId().value());
        timeEntryDto.setDate(LocalDate.parse("2022-01-02"));
        timeEntryDto.setStart(LocalTime.parse("14:30"));
        timeEntryDto.setEnd(LocalTime.parse("15:00"));
        timeEntryDto.setComment("hard work");
        timeEntryDto.setBreak(true);

        verify(timeEntryViewHelper).createTimeEntry(eq(timeEntryDto), any(BindingResult.class), any(CurrentOidcUser.class));
    }

    @Test
    void ensureTimeEntryCreationForDateTouchingNextDay() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockUserSettings(DayOfWeek.MONDAY);

        perform(
            post("/timeentries")
                .header("Referer", "/timeentries")
                .with(oidcSubject(userIdComposite)
                )
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-01-02")
                .param("start", "22:30:00.000+01:00")
                .param("end", "01:15:00.000+01:00")
                .param("comment", "hard work")
        )
            .andExpect(redirectedUrl("/timeentries"));

        final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setId(1337L);
        timeEntryDto.setUserLocalId(userIdComposite.localId().value());
        timeEntryDto.setDate(LocalDate.parse("2022-01-02"));
        timeEntryDto.setStart(LocalTime.parse("22:30"));
        timeEntryDto.setEnd(LocalTime.parse("01:15"));
        timeEntryDto.setComment("hard work");

        verify(timeEntryViewHelper).createTimeEntry(eq(timeEntryDto), any(BindingResult.class), any(CurrentOidcUser.class));
    }

    @Test
    void ensureTimeEntryCreationErrorBecauseOfMissingStartEnd() throws Exception {

        mockUserSettings(DayOfWeek.MONDAY);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 99);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2021, 52)).thenReturn(timeEntryWeekPage);

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
                .with(oidcSubject(userIdComposite)
                )
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-01-02")
                .param("comment", "hard work")
                .param("duration", "08:00")
            // missing start/end/value
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", expectedPage));
    }

    @Test
    void ensureTimeEntryCreationForOtherUserIsForbidden() {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId("joker");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        assertThatThrownBy(() -> perform(post("/timeentries/users/1337").with(oidcSubject(userIdComposite))))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(AccessDeniedException.class)
            .hasRootCauseMessage("You are not allowed to access timeentries of user 1337");
    }

    @Test
    void ensureTimeEntryCreationForOtherUser() throws Exception {

        mockUserSettings(DayOfWeek.MONDAY);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserLocalId ownerLocalId = new UserLocalId(2L);

        perform(
            post("/timeentries/users/2")
                .header("Referer", "/timeentries/users/2")
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL))
                )
                .param("userLocalId", ownerLocalId.value().toString())
                .param("date", "2025-03-17")
                .param("start", "14:30:00.000+02:00")
                .param("end", "15:00:00.000+02:00")
                .param("comment", "hard work")
        )
            .andExpect(redirectedUrl("http://localhost/timeentries/users/2"));

        final TimeEntryDTO expectedTimeEntryDto = new TimeEntryDTO();
        expectedTimeEntryDto.setUserLocalId(ownerLocalId.value());
        expectedTimeEntryDto.setDate(LocalDate.parse("2025-03-17"));
        expectedTimeEntryDto.setStart(LocalTime.parse("14:30"));
        expectedTimeEntryDto.setEnd(LocalTime.parse("15:00"));
        expectedTimeEntryDto.setComment("hard work");

        verify(timeEntryViewHelper).createTimeEntry(eq(expectedTimeEntryDto), any(BindingResult.class), any(CurrentOidcUser.class));
    }

    @Test
    void ensureTimeEntryEdit() throws Exception {

        final UserId userId = new UserId("batman");
        final UserIdComposite userIdComposite = anyUserIdComposite(userId);

        mockUserSettings(DayOfWeek.MONDAY);

        perform(
            post("/timeentries/1337")
                .header("Referer", "/timeentries/2022/39")
                .with(oidcSubject(userIdComposite)
                )
                .param("id", "1337")
                .param("userLocalId", userIdComposite.localId().value().toString())
                .param("date", "2022-09-28")
                .param("start", "21:15:00.000+01:00")
                .param("end", "")
                .param("duration", "01:00")
                .param("comment", "")
        )
            .andExpect(redirectedUrl("/timeentries/2022/39"));

        final TimeEntryDTO expectedTimeEntryDto = new TimeEntryDTO();
        expectedTimeEntryDto.setId(1337L);
        expectedTimeEntryDto.setUserLocalId(userIdComposite.localId().value());
        expectedTimeEntryDto.setDate(LocalDate.parse("2022-09-28"));
        expectedTimeEntryDto.setStart(LocalTime.parse("21:15"));
        expectedTimeEntryDto.setDuration("01:00");
        expectedTimeEntryDto.setComment("");

        verify(timeEntryViewHelper).updateTimeEntry(any(CurrentOidcUser.class), eq(expectedTimeEntryDto), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
    }

    @Test
    void ensureTimeEntryEditForAjax() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");
        mockUserSettings(DayOfWeek.MONDAY);

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntryId timeEntryId = new TimeEntryId(1337L);
        final TimeEntry timeEntryToEdit = new TimeEntry(timeEntryId, userIdComposite, "hard work extended", start, end, false);

        final ZonedDateTime startOtherDay = ZonedDateTime.of(2022, 9, 29, 14, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime endOtherDay = ZonedDateTime.of(2022, 9, 29, 15, 0, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntryOtherDay = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet", startOtherDay, endOtherDay, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(
            LocalDate.of(2022, 9, 26),
            new PlannedWorkingHours(Duration.ofHours(16)),
            List.of(
                new TimeEntryDay(
                    false,
                    LocalDate.of(2022, 9, 28),
                    new WorkDuration(Duration.ofMinutes(45)),
                    PlannedWorkingHours.EIGHT,
                    ShouldWorkingHours.EIGHT,
                    List.of(timeEntryToEdit),
                    List.of()),
                new TimeEntryDay(
                    false,
                    LocalDate.of(2022, 9, 29),
                    new WorkDuration(Duration.ofMinutes(30)),
                    PlannedWorkingHours.EIGHT,
                    ShouldWorkingHours.EIGHT,
                    List.of(timeEntryOtherDay),
                    List.of())
            )
        );

        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2022, 39)).thenReturn(timeEntryWeekPage);

        perform(
            post("/timeentries/1337")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject(userIdComposite)
                )
                .param("id", "1337")
                .param("userLocalId", userIdComposite.localId().value().toString())
                .param("date", "2022-09-28")
                .param("start", "20:30:00.000+01:00")
                .param("end", "21:15:00.000+01:00")
                .param("comment", "hard work extended")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("timeentries/index::#frame-time-entry"));

        final TimeEntryDTO expectedUpdatedTimeEntryDto = TimeEntryDTO.builder()
            .id(1337L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(20, 30))
            .end(LocalTime.of(21, 15))
            .comment("hard work extended")
            .build();

        verify(timeEntryViewHelper).updateTimeEntry(any(CurrentOidcUser.class), eq(expectedUpdatedTimeEntryDto), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
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

        final TimeEntryDay timeEntryDay = new TimeEntryDay(false, LocalDate.of(2022, 9, 19), new WorkDuration(Duration.ofMinutes(30)), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 19), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 1337);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2021, 52)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2022, 9, 22))
            .start(LocalTime.of(14, 30))
            .end(LocalTime.of(15, 0))
            .duration("00:30")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto timeEntryDayDto = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntry)).thenReturn(expectedTimeEntryDto);

        perform(
            post("/timeentries/1337")
                .with(oidcSubject(userIdComposite)
                )
                .param("id", "1337")
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-01-02")
                .param("comment", "hard work")
            // missing start/end/value
        )
            .andExpect(view().name("timeentries/index"))
            .andExpect(model().attribute("timeEntryWeeksPage", is(expectedPage)));
    }

    @Test
    void ensureTimeEntryEditWithValidationErrorWithAjax() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockUserSettings(DayOfWeek.MONDAY);

        final TimeEntryDTO expectedTimeEntryDto = new TimeEntryDTO();
        expectedTimeEntryDto.setUserLocalId(userLocalId.value());
        expectedTimeEntryDto.setDate(LocalDate.parse("2022-09-28"));
        expectedTimeEntryDto.setComment("hard work extended");

        doAnswer(args -> {
            final BindingResult bindingResult = args.getArgument(2);
            bindingResult.reject("something.is.wrong");
            return null;
        }).when(timeEntryViewHelper).updateTimeEntry(any(CurrentOidcUser.class), eq(expectedTimeEntryDto), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));

        perform(
            post("/timeentries/1337")
                .header("Turbo-Frame", "any-value")
                .with(oidcSubject(userIdComposite)
                )
                .param("id", "1337")
                .param("userLocalId", userLocalId.value().toString())
                .param("date", "2022-09-28")
                .param("start", "")
                .param("end", "")
                .param("comment", "hard work extended")
        )
            .andExpect(model().attributeDoesNotExist("timeEntryWeeks"))
            .andExpect(view().name("timeentries/index::#frame-time-entry"));
    }

    @Test
    void ensureTimeEntryEditForOtherUserIsForbidden() {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId("joker");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        assertThatThrownBy(() -> perform(post("/timeentries/users/2/timeentry/42").with(oidcSubject(userIdComposite))))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(AccessDeniedException.class)
            .hasRootCauseMessage("You are not allowed to access timeentries of user 2");
    }

    @Test
    void ensureTimeEntryEditForOtherUser() throws Exception {

        mockUserSettings(DayOfWeek.MONDAY);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        perform(
            post("/timeentries/users/2/timeentry/42")
                .header("Referer", "/timeentries/users/2")
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL))
                )
                .param("id", "42")
                .param("userLocalId", "2")
                .param("date", "2025-03-17")
                .param("start", "20:30:00.000+02:00")
                .param("end", "21:15:00.000+02:00")
                .param("comment", "hard work extended")
        )
            .andExpect(redirectedUrl("/timeentries/users/2"));

        final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setId(42L);
        timeEntryDto.setUserLocalId(2L);
        timeEntryDto.setDate(LocalDate.parse("2025-03-17"));
        timeEntryDto.setStart(LocalTime.parse("20:30"));
        timeEntryDto.setEnd(LocalTime.parse("21:15"));
        timeEntryDto.setComment("hard work extended");

        verify(timeEntryViewHelper).updateTimeEntry(any(CurrentOidcUser.class), eq(timeEntryDto), any(BeanPropertyBindingResult.class), any(Model.class), any(RedirectAttributes.class));
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

        when(timeEntryService.findTimeEntry(new TimeEntryId(1337L))).thenReturn(Optional.of(timeEntry));

        perform(
            post("/timeentries/1337")
                .with(oidcSubject(userIdComposite)
                )
                .param("delete", "")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/timeentries?year=2022&week=39"));

        verify(timeEntryService).deleteTimeEntry(new TimeEntryId(1337L));
    }

    @Test
    void ensureDeleteWithAjax() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntryToDelete = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", start, end, false);

        final ZonedDateTime start2 = ZonedDateTime.of(2022, 9, 28, 16, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end2 = ZonedDateTime.of(2022, 9, 28, 17, 30, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntry2 = new TimeEntry(new TimeEntryId(2L), userIdComposite, "hack the planet", start2, end2, false);

        when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntryToDelete));

        final TimeEntryDay timeEntryDay = new TimeEntryDay(false, LocalDate.of(2022, 9, 28), new WorkDuration(Duration.ofHours(1)), PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry2), List.of());
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 26), PlannedWorkingHours.EIGHT, List.of(timeEntryDay));
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2022, 39)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedDeletedTimeEntryDeletedDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(20, 30))
            .end(LocalTime.of(21, 15))
            .duration("00:45")
            .comment("hard work")
            .build();

        final TimeEntryDTO expectedTimeEntry2Dto = TimeEntryDTO.builder()
            .id(2L)
            .userLocalId(userLocalId.value())
            .date(LocalDate.of(2022, 9, 28))
            .start(LocalTime.of(16, 30))
            .end(LocalTime.of(17, 30))
            .duration("01:00")
            .comment("hack the planet")
            .build();

        final TimeEntryDayDto expectedDayDto = TimeEntryDayDto.builder()
            .isAllowedToEdit(true)
            .date("formatted-date")
            .dayOfWeek(DayOfWeek.WEDNESDAY)
            .hoursWorked("01:00")
            .hoursWorkedShould("08:00")
            .hoursDelta("07:00")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(13.0)
            .timeEntries(List.of(expectedTimeEntry2Dto))
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntryToDelete)).thenReturn(expectedDeletedTimeEntryDeletedDto);
        when(timeEntryViewHelper.toTimeEntryDto(timeEntry2)).thenReturn(expectedTimeEntry2Dto);

        perform(
            post("/timeentries/1")
                .with(oidcSubject(userIdComposite)
                )
                .param("delete", "")
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", expectedDayDto))
            .andExpect(model().attribute("turboDeletedTimeEntry", expectedDeletedTimeEntryDeletedDto));

        verify(timeEntryService).deleteTimeEntry(new TimeEntryId(1L));
    }

    @Test
    void ensureDeleteWithAjaxLastTimeEntryOfDay() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        final ZonedDateTime start = ZonedDateTime.of(2022, 9, 28, 20, 30, 0, 0, zoneIdBerlin);
        final ZonedDateTime end = ZonedDateTime.of(2022, 9, 28, 21, 15, 0, 0, zoneIdBerlin);
        final TimeEntry timeEntryToDelete = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", start, end, false);

        when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntryToDelete));

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.of(2022, 9, 26), PlannedWorkingHours.EIGHT, List.of());
        final TimeEntryWeekPage timeEntryWeekPage = new TimeEntryWeekPage(timeEntryWeek, 0);
        when(timeEntryService.getEntryWeekPage(userLocalId, 2022, 39)).thenReturn(timeEntryWeekPage);

        when(dateFormatter.formatDate(any(), any(), any())).thenReturn("formatted-date");

        final TimeEntryDTO expectedDeletedTimeEntryDto = TimeEntryDTO.builder()
            .id(1L)
            .userLocalId(userLocalId.value())
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

        when(timeEntryViewHelper.toTimeEntryDto(timeEntryToDelete)).thenReturn(expectedDeletedTimeEntryDto);

        perform(
            post("/timeentries/1")
                .with(oidcSubject(userIdComposite)
                )
                .param("delete", "")
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("turboEditedWeek", expectedWeekDto))
            .andExpect(model().attribute("turboEditedDay", nullValue()))
            .andExpect(model().attribute("turboDeletedTimeEntry", expectedDeletedTimeEntryDto));

        verify(timeEntryService).deleteTimeEntry(new TimeEntryId(1L));
    }

    @Test
    void ensureDeleteForOtherUserIsForbidden() {

        final UserId ownerUserId = new UserId("batman");
        final UserLocalId ownerUserLocalId = new UserLocalId(1337L);
        final UserIdComposite ownerUserIdComposite = new UserIdComposite(ownerUserId, ownerUserLocalId);

        final TimeEntryId timeEntryId = new TimeEntryId(1L);
        final TimeEntry timeEntry = anyTimeEntry(timeEntryId, ownerUserIdComposite);
        when(timeEntryService.findTimeEntry(timeEntryId)).thenReturn(Optional.of(timeEntry));

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId("joker");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        assertThatThrownBy(() -> perform(post("/timeentries/users/1337/timeentry/1").with(oidcSubject(userIdComposite)).param("delete", "")))
            .isInstanceOf(ServletException.class)
            .hasCauseInstanceOf(AccessDeniedException.class)
            .hasRootCauseMessage("You are not allowed to access timeentries of user 1337");
    }

    @Test
    void ensureDeleteForOtherUser() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserId ownerId = new UserId("joker");
        final UserLocalId ownerLocalId = new UserLocalId(2L);
        final UserIdComposite ownerIdComposite = new UserIdComposite(ownerId, ownerLocalId);

        final TimeEntryId timeEntryId = new TimeEntryId(42L);
        final ZonedDateTime start = ZonedDateTime.parse("2025-03-17T09:00:00Z");
        final ZonedDateTime end = ZonedDateTime.parse("2025-03-17T17:00:00Z");
        final TimeEntry timeEntry = new TimeEntry(timeEntryId, ownerIdComposite, "", start, end, false);

        when(timeEntryService.findTimeEntry(timeEntryId)).thenReturn(Optional.of(timeEntry));

        perform(
            post("/timeentries/users/2/timeentry/42")
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL))
                )
                .param("delete", "")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/timeentries/users/2?year=2025&week=12"));

        verify(timeEntryService).deleteTimeEntry(timeEntryId);
    }

    private UserIdComposite anyUserIdComposite(UserId userId) {
        return new UserIdComposite(userId, new UserLocalId(1L));
    }

    private User anyUser(UserIdComposite userIdComposite) {
        return new User(userIdComposite, "given name", "family name", new EMailAddress("email@example.org"), Set.of());
    }

    private TimeEntry anyTimeEntry(TimeEntryId id, UserIdComposite userIdComposite) {

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

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
