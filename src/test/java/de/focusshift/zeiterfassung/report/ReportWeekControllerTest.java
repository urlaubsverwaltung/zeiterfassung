package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.DateFormatterImpl;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.threeten.extra.YearWeek;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static java.util.Locale.GERMAN;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ReportWeekControllerTest {

    private ReportWeekController sut;

    @Mock
    private ReportService reportService;
    @Mock
    private ReportPermissionService reportPermissionService;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        final DateFormatterImpl dateFormatter = new DateFormatterImpl();
        final ReportControllerHelper helper = new ReportControllerHelper(reportPermissionService, dateFormatter);
        sut = new ReportWeekController(reportService, helper, clock);
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

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final ReportWeek reportWeek = new ReportWeek(LocalDate.of(2023, 1, 30), List.of(
            eightHoursDay(LocalDate.of(2023, 1, 30), user),
            eightHoursDay(LocalDate.of(2023, 1, 31), user),
            eightHoursDay(LocalDate.of(2023, 2, 1), user),
            eightHoursDay(LocalDate.of(2023, 2, 2), user),
            eightHoursDay(LocalDate.of(2023, 2, 3), user),
            new ReportDay(
                LocalDate.of(2023, 2, 4),
                Map.of(userIdComposite, PlannedWorkingHours.ZERO),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of())
            ),
            new ReportDay(
                LocalDate.of(2023, 2, 5),
                Map.of(userIdComposite, PlannedWorkingHours.ZERO),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of())
            )
        ));

        when(reportService.getReportWeek(Year.of(2023), 5, new UserId("batman")))
            .thenReturn(reportWeek);

        final GraphWeekDto graphWeekDto = new GraphWeekDto(
            "Januar 2023 KW 5",
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
            8d
        );

        perform(
            get("/report/year/2023/week/5")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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

        final ReportWeek reportWeek = new ReportWeek(LocalDate.of(2023, 1, 30), List.of(
            new ReportDay(
                absenceDate,
                Map.of(userIdComposite, PlannedWorkingHours.EIGHT),
                Map.of(userIdComposite, List.of()),
                Map.of(userIdComposite, List.of(
                    new ReportDayAbsence(user, new Absence(
                        user.userId(),
                        absenceDate.atStartOfDay(UTC),
                        absenceDate.atStartOfDay(UTC),
                        DayLength.FULL,
                        locale -> "absence-full-de",
                        AbsenceColor.ORANGE
                    ))
                ))
            )
        ));

        when(reportService.getReportWeek(Year.of(2023), 5, new UserId("batman")))
            .thenReturn(reportWeek);

        final DetailWeekDto detailWeekDto = new DetailWeekDto(
            Date.from(ZonedDateTime.of(LocalDate.of(2023, 1,30), LocalTime.MIN, ZoneId.systemDefault()).toInstant()),
            Date.from(ZonedDateTime.of(LocalDate.of(2023, 2,5), LocalTime.MIN, ZoneId.systemDefault()).toInstant()),
            5,
            List.of(
                new DetailDayDto(true, "F", "Freitag", "03.02.2023", 0d, List.of(),
                    List.of(
                        new DetailDayAbsenceDto(
                            "Bruce Wayne",
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
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .locale(GERMAN)
        )
            .andExpect(model().attribute("weekReportDetail", detailWeekDto));
    }

    @Test
    void ensureWeekReportSectionUrls() throws Exception {

        when(reportService.getReportWeek(Year.of(2022), 1, new UserId("batman")))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/week/52?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/week?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/week/2?user=1&user=2&user=42"));
    }

    @Test
    void ensureWeekReportCsvDownloadUrl() throws Exception {

        when(reportService.getReportWeek(Year.of(2022), 1, new UserId("batman")))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/week/1?csv"));
    }

    @Test
    void ensureWeekReportCsvDownloadUrlWithEveryoneParam() throws Exception {

        when(reportService.getReportWeekForAllUsers(Year.of(2022), 1))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
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
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/week/1?user=1&user=2&user=42&csv"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsAreNotAddedWhenCurrentUserHasNoPermission() throws Exception {

        final User user = anyUser();
        when(reportPermissionService.findAllPermittedUsersForCurrentUser()).thenReturn(List.of(user));

        when(reportService.getReportWeek(Year.of(2022), 1, user.userId()))
            .thenReturn(anyReportWeek());

        perform(get("/report/year/2022/week/1").with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))))
            .andExpect(model().attributeDoesNotExist("users", "selectedUserIds", "allUsersSelected", "userReportFilterUrl"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrls() throws Exception {

        final UserId userId_1 = new UserId("batman");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("joker");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId_3 = new UserId("robin");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);
        final User user_3 = new User(userIdComposite_3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user_1, user_2, user_3));

        when(reportService.getReportWeek(Year.of(2022), 1, userId_1))
            .thenReturn(anyReportWeek());

        perform(get("/report/year/2022/week/1").with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))))
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(2L, "Jack Napier", false),
                new SelectableUserDto(3L, "Dick Grayson", false)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of()))
            .andExpect(model().attribute("allUsersSelected", false))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsForEveryone() throws Exception {

        final UserId userId_1 = new UserId("batman");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("joker");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId_3 = new UserId("robin");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);
        final User user_3 = new User(userIdComposite_3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user_1, user_2, user_3));

        when(reportService.getReportWeekForAllUsers(Year.of(2022), 1))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("everyone", "")
        )
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(2L, "Jack Napier", false),
                new SelectableUserDto(3L, "Dick Grayson", false)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of()))
            .andExpect(model().attribute("allUsersSelected", true))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
    }

    @Test
    void ensureWeekReportUserFilterRelatedUrlsForWithSelectedUser() throws Exception {

        final UserId userId_1 = new UserId("batman");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("joker");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId userId_3 = new UserId("robin");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);
        final User user_3 = new User(userIdComposite_3, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(user_1, user_2, user_3));

        when(reportService.getReportWeek(Year.of(2022), 1, List.of(userLocalId_2, userLocalId_3)))
            .thenReturn(anyReportWeek());

        perform(
            get("/report/year/2022/week/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("user", "2", "3")
        )
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(2L, "Jack Napier", true),
                new SelectableUserDto(3L, "Dick Grayson", true)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of(2L, 3L)))
            .andExpect(model().attribute("allUsersSelected", false))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/week/1"));
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

    private ReportDay eightHoursDay(LocalDate date, User user) {
        return new ReportDay(
            date,
            Map.of(user.userIdComposite(), PlannedWorkingHours.EIGHT),
            Map.of(user.userIdComposite(), List.of(reportDayEntry(user, date))),
            Map.of(user.userIdComposite(), List.of())
        );
    }

    private ReportDayEntry reportDayEntry(User user, LocalDate date) {
        return new ReportDayEntry(user, "", date.atStartOfDay().plusHours(8).atZone(UTC), date.atStartOfDay().plusHours(16).atZone(UTC), false);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
