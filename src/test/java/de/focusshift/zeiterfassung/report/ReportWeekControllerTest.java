package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;

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

    @Mock
    private DateFormatter dateFormatter;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
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

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
