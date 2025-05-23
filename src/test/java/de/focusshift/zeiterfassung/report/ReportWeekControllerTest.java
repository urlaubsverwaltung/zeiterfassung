package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDTO;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDialogHelper;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.DateFormatterImpl;
import de.focusshift.zeiterfassung.user.DateRangeFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.threeten.extra.YearWeek;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.ORANGE;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static java.time.ZoneOffset.UTC;
import static java.util.Locale.GERMAN;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ReportWeekControllerTest implements ControllerTest {

    private ReportWeekController sut;

    @Mock
    private ReportService reportService;
    @Mock
    private ReportPermissionService reportPermissionService;
    @Mock
    private TimeEntryDialogHelper timeEntryDialogHelper;
    @Mock
    private MessageSource messageSource;

    private DateFormatter dateFormatter;
    private DateRangeFormatter dateRangeFormatter;
    private ReportViewHelper reportViewHelper;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        dateFormatter = new DateFormatterImpl();
        dateRangeFormatter = new DateRangeFormatter(dateFormatter, messageSource);
        reportViewHelper = new ReportViewHelper(dateFormatter, dateRangeFormatter);
        sut = new ReportWeekController(reportService, reportPermissionService, reportViewHelper, timeEntryDialogHelper, clock);
    }

    @Test
    void ensureReportWeekForwardsToTodayWeekReport() throws Exception {

        final int nowYear = YearWeek.now(clock).getYear();
        final int nowWeek = YearWeek.now(clock).getWeek();

        perform(get("/report/week"))
            .andExpect(forwardedUrl(String.format("/report/year/%d/week/%d", nowYear, nowWeek)));
    }

    @Test
    void ensureReportWeek() throws Exception {

        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(
            Map.of(
                LocalDate.of(2023, 1, 30), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 1, 31), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 1), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 2), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 3), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 4), PlannedWorkingHours.ZERO,
                LocalDate.of(2023, 2, 5), PlannedWorkingHours.ZERO
            ),
            Map.of()
        );

        final ReportWeek reportWeek = new ReportWeek(LocalDate.of(2023, 1, 30), List.of(
            eightHoursDay(LocalDate.of(2023, 1, 30), user, workingTimeCalendar),
            eightHoursDay(LocalDate.of(2023, 1, 31), user, workingTimeCalendar),
            eightHoursDay(LocalDate.of(2023, 2, 1), user, workingTimeCalendar),
            eightHoursDay(LocalDate.of(2023, 2, 2), user, workingTimeCalendar),
            eightHoursDay(LocalDate.of(2023, 2, 3), user, workingTimeCalendar),
            new ReportDay(
                LocalDate.of(2023, 2, 4),
                false, Map.of(userIdComposite, workingTimeCalendar),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of())
            ),
            new ReportDay(
                LocalDate.of(2023, 2, 5),
                false, Map.of(userIdComposite, workingTimeCalendar),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of())
            )
        ));

        when(reportService.getReportWeek(Year.of(2023), 5, userLocalId))
            .thenReturn(reportWeek);

        final GraphWeekDto graphWeekDto = new GraphWeekDto(
            5,
            "date-range",
            List.of(
                new GraphDayDto(false, "M", "Montag", "30.01.2023", 8d, 8d),
                new GraphDayDto(false, "D", "Dienstag", "31.01.2023", 8d, 8d),
                new GraphDayDto(true, "M", "Mittwoch", "01.02.2023", 8d, 8d),
                new GraphDayDto(true, "D", "Donnerstag", "02.02.2023", 8d, 8d),
                new GraphDayDto(true, "F", "Freitag", "03.02.2023", 8d, 8d),
                new GraphDayDto(true, "S", "Samstag", "04.02.2023", 0d, 0d),
                new GraphDayDto(true, "S", "Sonntag", "05.02.2023", 0d, 0d)
            ),
            8d,
            "40:00",
            "40:00",
            "00:00",
            false,
            100d
        );

        perform(
            get("/report/year/2023/week/5")
                .with(oidcSubject(userIdComposite))
                .locale(GERMAN)
        )
            .andExpect(model().attribute("weekReport", graphWeekDto));
    }

    @Test
    void ensureReportWeekWithAbsences() throws Exception {

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final LocalDate absenceDate = LocalDate.of(2023, 2, 3);
        Absence absence = new Absence(
            user.userId(),
            absenceDate.atStartOfDay(UTC).toInstant(),
            absenceDate.atStartOfDay(UTC).toInstant(),
            DayLength.FULL,
            locale -> "absence-full-de",
            ORANGE,
            HOLIDAY
        );

        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(
            Map.of(LocalDate.of(2023, 2, 3), PlannedWorkingHours.EIGHT),
            Map.of(LocalDate.of(2023, 2, 3), List.of(absence))
        );


        final ReportWeek reportWeek = new ReportWeek(LocalDate.of(2023, 1, 30), List.of(
            new ReportDay(
                absenceDate,
                false, Map.of(userIdComposite, workingTimeCalendar),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of(
                    new ReportDayAbsence(user, absence)
                ))
            )
        ));

        when(reportService.getReportWeek(Year.of(2023), 5, userLocalId))
            .thenReturn(reportWeek);

        final DetailWeekDto detailWeekDto = new DetailWeekDto(
            Date.from(ZonedDateTime.of(LocalDate.of(2023, 1, 30), LocalTime.MIN, ZoneId.systemDefault()).toInstant()),
            Date.from(ZonedDateTime.of(LocalDate.of(2023, 2, 5), LocalTime.MIN, ZoneId.systemDefault()).toInstant()),
            5,
            List.of(
                new DetailDayDto(true, "F", "Freitag", "03.02.2023", false, "00:00", "00:00", "00:00", false, List.of(),
                    List.of(
                        new DetailDayAbsenceDto(
                            "Bruce Wayne",
                            1L,
                            "FULL",
                            "absence-full-de",
                            "ORANGE"
                        )
                    )
                )
            )
        );

        perform(
            get("/report/year/2023/week/5")
                .with(oidcSubject(userIdComposite))
                .locale(GERMAN)
        )
            .andExpect(model().attribute("weekReportDetail", detailWeekDto));
    }

    @Test
    void ensureWeekReportSectionUrls() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        when(reportService.getReportWeek(Year.of(2022), 1, userLocalId))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/week/52"))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/week"))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/week/2"));
    }

    @Test
    void ensureWeekReportSectionUrlsWithEveryoneParam() throws Exception {

        when(reportService.getReportWeekForAllUsers(Year.of(2022), 1))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("everyone", "")
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/week/52?everyone="))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/week?everyone="))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/week/2?everyone="));
    }

    @Test
    void ensureWeekReportSectionUrlsWithUsersParam() throws Exception {

        when(reportService.getReportWeek(Year.of(2022), 1, List.of(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(42L))))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/week/52?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/week?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/week/2?user=1&user=2&user=42"));
    }

    @Test
    void ensureWeekReportCsvDownloadUrl() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        when(reportService.getReportWeek(Year.of(2022), 1, userLocalId))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject(userIdComposite))
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/week/1?csv"));
    }

    @Test
    void ensureWeekReportCsvDownloadUrlWithEveryoneParam() throws Exception {

        when(reportService.getReportWeekForAllUsers(Year.of(2022), 1))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("everyone", "")
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/week/1?everyone=&csv"));
    }

    @Test
    void ensureWeekReportCsvDownloadUrlWithUsersParam() throws Exception {

        when(reportService.getReportWeek(Year.of(2022), 1, List.of(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(42L))))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/week/1?user=1&user=2&user=42&csv"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsAreNotAddedWhenCurrentUserHasNoPermission() throws Exception {

        final User user = anyUser();
        when(reportPermissionService.findAllPermittedUsersForCurrentUser()).thenReturn(List.of(user));

        when(reportService.getReportWeek(Year.of(2022), 1, user.userLocalId()))
            .thenReturn(anyReportWeek());

        perform(get("/report/year/2022/week/1").with(oidcSubject(user.userIdComposite())))
            .andExpect(model().attributeDoesNotExist("users", "selectedUserIds", "allUsersSelected", "userReportFilterUrl"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrls() throws Exception {

        final UserId userId1 = new UserId("batman");
        final UserLocalId userLocalId1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite1 = new UserIdComposite(userId1, userLocalId1);
        final User user1 = new User(userIdComposite1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId2 = new UserId("joker");
        final UserLocalId userLocalId2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite2 = new UserIdComposite(userId2, userLocalId2);
        final User user2 = new User(userIdComposite2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId3 = new UserId("robin");
        final UserLocalId userLocalId3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite3 = new UserIdComposite(userId3, userLocalId3);
        final User user3 = new User(userIdComposite3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user1, user2, user3));

        when(reportService.getReportWeek(Year.of(2022), 1, userLocalId1))
            .thenReturn(anyReportWeek());

        perform(get("/report/year/2022/week/1").with(oidcSubject(userIdComposite1)))
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(3L, "Dick Grayson", false),
                new SelectableUserDto(2L, "Jack Napier", false)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of()))
            .andExpect(model().attribute("allUsersSelected", false))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsForEveryone() throws Exception {

        final UserId userId1 = new UserId("batman");
        final UserLocalId userLocalId1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite1 = new UserIdComposite(userId1, userLocalId1);
        final User user1 = new User(userIdComposite1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId2 = new UserId("joker");
        final UserLocalId userLocalId2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite2 = new UserIdComposite(userId2, userLocalId2);
        final User user2 = new User(userIdComposite2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId3 = new UserId("robin");
        final UserLocalId userLocalId3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite3 = new UserIdComposite(userId3, userLocalId3);
        final User user3 = new User(userIdComposite3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user1, user2, user3));

        when(reportService.getReportWeekForAllUsers(Year.of(2022), 1))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("everyone", "")
        )
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(3L, "Dick Grayson", false),
                new SelectableUserDto(2L, "Jack Napier", false)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of()))
            .andExpect(model().attribute("allUsersSelected", true))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsForWithSelectedUser() throws Exception {

        final UserId userId1 = new UserId("batman");
        final UserLocalId userLocalId1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite1 = new UserIdComposite(userId1, userLocalId1);
        final User user1 = new User(userIdComposite1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId2 = new UserId("joker");
        final UserLocalId userLocalId2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite2 = new UserIdComposite(userId2, userLocalId2);
        final User user2 = new User(userIdComposite2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId3 = new UserId("robin");
        final UserLocalId userLocalId3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite3 = new UserIdComposite(userId3, userLocalId3);
        final User user3 = new User(userIdComposite3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user1, user2, user3));

        when(reportService.getReportWeek(Year.of(2022), 1, List.of(userLocalId2, userLocalId3)))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcSubject("batman"))
                .param("user", "2", "3")
        )
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(3L, "Dick Grayson", true),
                new SelectableUserDto(2L, "Jack Napier", true)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of(2L, 3L)))
            .andExpect(model().attribute("allUsersSelected", false))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
    }

    @Nested
    class EditTimeEntry {

        @BeforeEach
        void setUp() {
            timeEntryDialogHelper = mock(TimeEntryDialogHelper.class);
            sut = new ReportWeekController(reportService, reportPermissionService, reportViewHelper, timeEntryDialogHelper, clock);
        }

        @Test
        void ensureEditTimeEntryWithValidationConstraints() throws Exception {

            perform(post("/report/year/2025/week/9")
                .param("id", "1")
            )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/report/year/2025/week/9?timeEntryId=1"));

            // must be called even with initial constraint violations
            // since the helper adds further stuff to the model
            verify(timeEntryDialogHelper).saveTimeEntry(nullable(CurrentOidcUser.class), any(TimeEntryDTO.class), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
        }

        @Test
        void ensureEditTimeEntry() throws Exception {

            perform(post("/report/year/2025/week/9")
                .param("id", "1")
                .param("userLocalId", "1")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/report/year/2025/week/9"))
                .andExpect(flash().attribute("turboRefreshScroll", "preserve"));

            verify(timeEntryDialogHelper).saveTimeEntry(nullable(CurrentOidcUser.class), any(TimeEntryDTO.class), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
        }

        @Test
        void ensureEditTimeEntryWithParameterEveryone() throws Exception {

            perform(post("/report/year/2025/week/9")
                .param("everyone", "true")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(redirectedUrl("http://localhost/report/year/2025/week/9?everyone=true"));
        }

        @Test
        void ensureEditTimeEntryWithParameterUser() throws Exception {

            perform(post("/report/year/2025/week/9")
                .param("user", "1", "2")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(redirectedUrl("http://localhost/report/year/2025/week/9?user=1&user=2"));
        }
    }

    private static User anyUser() {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        return new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
    }

    private static ReportWeek anyReportWeek() {
        return new ReportWeek(LocalDate.of(2022, 1, 1), List.of());
    }

    private ReportDay eightHoursDay(LocalDate date, User user, WorkingTimeCalendar workingTimeCalendar) {
        return new ReportDay(
            date,
            false, Map.of(user.userIdComposite(), workingTimeCalendar),
            Map.of(user.userIdComposite(), List.of(reportDayEntry(user, date))),
            Map.of(user.userIdComposite(), List.of())
        );
    }

    private ReportDayEntry reportDayEntry(User user, LocalDate date) {
        final long randomId = ThreadLocalRandom.current().nextLong();
        return new ReportDayEntry(new TimeEntryId(randomId), user, "", date.atStartOfDay().plusHours(8).atZone(UTC), date.atStartOfDay().plusHours(16).atZone(UTC), false);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
