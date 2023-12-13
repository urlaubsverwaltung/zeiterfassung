package de.focusshift.zeiterfassung.report;

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

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
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
class ReportMonthControllerTest {

    private ReportMonthController sut;

    @Mock
    private ReportService reportService;
    @Mock
    private ReportPermissionService reportPermissionService;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        final DateFormatterImpl dateFormatter = new DateFormatterImpl();
        final ReportControllerHelper helper = new ReportControllerHelper(reportPermissionService, dateFormatter);
        sut = new ReportMonthController(reportService, dateFormatter, helper, clock);
    }

    @Test
    void ensureReportMonthForwardsToTodayMonthReport() throws Exception {

        final int nowYear = Year.now(clock).getValue();
        final int nowMonth = Month.from(LocalDate.now(clock)).getValue();

        perform(get("/report/month"))
            .andExpect(forwardedUrl(String.format("/report/year/%d/month/%d", nowYear, nowMonth)));
    }

    @Test
    void ensureReportMonth() throws Exception {

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final ReportMonth reportMonth = new ReportMonth(
            YearMonth.of(2023, 2),
            List.of(
                fourtyHourWeek(user, LocalDate.of(2023, 1, 30))
            )
        );

        when(reportService.getReportMonth(YearMonth.of(2023, 2), new UserId("batman")))
            .thenReturn(reportMonth);

        final GraphMonthDto graphMonthDto = new GraphMonthDto(
            "Februar 2023",
            List.of(
                new GraphWeekDto(
                    "Januar 2023 KW 5",
                    List.of(
                        new GraphDayDto(true, "M", "Montag", "30.01.2023", 8d, 8d),
                        new GraphDayDto(true, "D", "Dienstag", "31.01.2023", 8d, 8d),
                        new GraphDayDto(false, "M", "Mittwoch", "01.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Donnerstag", "02.02.2023", 8d, 8d),
                        new GraphDayDto(false, "F", "Freitag", "03.02.2023", 8d, 8d),
                        new GraphDayDto(false, "S", "Samstag", "04.02.2023", 0d, 0d),
                        new GraphDayDto(false, "S", "Sonntag", "05.02.2023", 0d, 0d)
                    ),
                    8d,
                    8d
                )
            ),
            8d,
            8d
        );

        perform(
            get("/report/year/2023/month/2")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .locale(GERMAN)
        )
            .andExpect(model().attribute("monthReport", graphMonthDto));
    }

    @Test
    void ensureMonthReportSectionUrls() throws Exception {

        when(reportService.getReportMonth(YearMonth.of(2022, 1), new UserId("batman")))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/month/12"))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/month"))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/month/2"));
    }

    @Test
    void ensureMonthReportSectionUrlsWithEveryoneParam() throws Exception {

        when(reportService.getReportMonthForAllUsers(YearMonth.of(2022, 1)))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("everyone", "")
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/month/12?everyone="))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/month?everyone="))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/month/2?everyone="));
    }

    @Test
    void ensureMonthReportSectionUrlsWithUsersParam() throws Exception {

        when(reportService.getReportMonth(YearMonth.of(2022, 1), List.of(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(42L))))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportPreviousSectionUrl", "/report/year/2021/month/12?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportTodaySectionUrl", "/report/month?user=1&user=2&user=42"))
            .andExpect(model().attribute("userReportNextSectionUrl", "/report/year/2022/month/2?user=1&user=2&user=42"));
    }

    @Test
    void ensureMonthReportCsvDownloadUrl() throws Exception {

        when(reportService.getReportMonth(YearMonth.of(2022, 1), new UserId("batman")))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/month/1?csv"));
    }

    @Test
    void ensureMonthReportCsvDownloadUrlWithEveryoneParam() throws Exception {

        when(reportService.getReportMonthForAllUsers(YearMonth.of(2022, 1)))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("everyone", "")
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/month/1?everyone=&csv"));
    }

    @Test
    void ensureMonthReportCsvDownloadUrlWithUsersParam() throws Exception {

        when(reportService.getReportMonth(YearMonth.of(2022, 1), List.of(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(42L))))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman")))
                .param("user", "1", "2", "42")
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/month/1?user=1&user=2&user=42&csv"));
    }

    @Test
    void ensureMonthReportUserFilterRelatedUrlsAreNotAddedWhenCurrentUserHasNoPermission() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(new User(userIdComposite, "", "", new EMailAddress(""), Set.of())));

        when(reportService.getReportMonth(YearMonth.of(2022, 1), userId))
            .thenReturn(anyReportMonth());

        perform(get("/report/year/2022/month/1").with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))))
            .andExpect(model().attributeDoesNotExist("users", "selectedUserIds", "allUsersSelected", "userReportFilterUrl"));
    }

    @Test
    void ensureMonthReportUserFilterRelatedUrls() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId jokerId = new UserId("joker");
        final UserLocalId jokerLocalId = new UserLocalId(2L);
        final UserIdComposite jokerIdComposite = new UserIdComposite(jokerId, jokerLocalId);
        final User joker = new User(jokerIdComposite, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId robinId = new UserId("robin");
        final UserLocalId robinLocalId = new UserLocalId(3L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);
        final User robin = new User(robinIdComposite, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(batman, joker, robin));

        when(reportService.getReportMonth(YearMonth.of(2022, 1), new UserId("batman")))
            .thenReturn(anyReportMonth());

        perform(get("/report/year/2022/month/1").with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))))
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(2L, "Jack Napier", false),
                new SelectableUserDto(3L, "Dick Grayson", false)
            )))
            .andExpect(model().attribute("selectedUserIds", List.of()))
            .andExpect(model().attribute("allUsersSelected", false))
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/month/1"));
    }

    @Test
    void ensureMonthReportUserFilterRelatedUrlsForEveryone() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId jokerId = new UserId("joker");
        final UserLocalId jokerLocalId = new UserLocalId(2L);
        final UserIdComposite jokerIdComposite = new UserIdComposite(jokerId, jokerLocalId);
        final User joker = new User(jokerIdComposite, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId robinId = new UserId("robin");
        final UserLocalId robinLocalId = new UserLocalId(3L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);
        final User robin = new User(robinIdComposite, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(batman, joker, robin));

        when(reportService.getReportMonthForAllUsers(YearMonth.of(2022, 1)))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
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
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/month/1"));
    }

    @Test
    void ensureMonthReportUserFilterRelatedUrlsForWithSelectedUser() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId jokerId = new UserId("joker");
        final UserLocalId jokerLocalId = new UserLocalId(2L);
        final UserIdComposite jokerIdComposite = new UserIdComposite(jokerId, jokerLocalId);
        final User joker = new User(jokerIdComposite, "Jack", "Napier", new EMailAddress(""), Set.of());

        final UserId robinId = new UserId("robin");
        final UserLocalId robinLocalId = new UserLocalId(3L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);
        final User robin = new User(robinIdComposite, "Dick", "Grayson", new EMailAddress(""), Set.of());

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(batman, joker, robin));

        when(reportService.getReportMonth(YearMonth.of(2022, 1), List.of(new UserLocalId(2L), new UserLocalId(3L))))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
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
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/month/1"));
    }

    private static ReportMonth anyReportMonth() {
        return new ReportMonth(YearMonth.of(2022, 1), List.of());
    }

    private ReportWeek fourtyHourWeek(User user, LocalDate firstDateOfWeek) {
        return new ReportWeek(firstDateOfWeek, List.of(
            eightHoursDay(firstDateOfWeek, user),
            eightHoursDay(firstDateOfWeek.plusDays(1), user),
            eightHoursDay(firstDateOfWeek.plusDays(2), user),
            eightHoursDay(firstDateOfWeek.plusDays(3), user),
            eightHoursDay(firstDateOfWeek.plusDays(4), user),
            new ReportDay(
                firstDateOfWeek.plusDays(5),
                Map.of(user.userIdComposite(), PlannedWorkingHours.ZERO),
                Map.of(user.userIdComposite(), List.of()),
                Map.of(user.userIdComposite(), List.of())
            ),
            new ReportDay(
                firstDateOfWeek.plusDays(6),
                Map.of(user.userIdComposite(), PlannedWorkingHours.ZERO),
                Map.of(user.userIdComposite(), List.of()),
                Map.of(user.userIdComposite(), List.of())
            )
        ));
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
