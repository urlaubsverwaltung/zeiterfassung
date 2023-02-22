package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

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

    @Mock
    private DateFormatter dateFormatter;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new ReportMonthController(reportService, reportPermissionService, dateFormatter, clock);
    }

    @Test
    void ensureReportMonthForwardsToTodayMonthReport() throws Exception {

        final int nowYear = Year.now(clock).getValue();
        final int nowMonth = Month.from(LocalDate.now(clock)).getValue();

        perform(get("/report/month"))
            .andExpect(forwardedUrl(String.format("/report/year/%d/month/%d", nowYear, nowMonth)));
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

        when(reportPermissionService.findAllPermittedUsersForCurrentUser())
            .thenReturn(List.of(new User(new UserId("batman"), new UserLocalId(1L), "", "", new EMailAddress(""), Set.of())));

        when(reportService.getReportMonth(YearMonth.of(2022, 1), new UserId("batman")))
            .thenReturn(anyReportMonth());

        perform(get("/report/year/2022/month/1").with(oidcLogin().userInfoToken(userInfo -> userInfo.subject("batman"))))
            .andExpect(model().attributeDoesNotExist("users", "selectedUserIds", "allUsersSelected", "userReportFilterUrl"));
    }

    @Test
    void ensureMonthReportUserFilterRelatedUrls() throws Exception {

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""), Set.of());
        final User joker = new User(new UserId("joker"), new UserLocalId(2L), "Jack", "Napier", new EMailAddress(""), Set.of());
        final User robin = new User(new UserId("robin"), new UserLocalId(3L), "Dick", "Grayson", new EMailAddress(""), Set.of());

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

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""), Set.of());
        final User joker = new User(new UserId("joker"), new UserLocalId(2L), "Jack", "Napier", new EMailAddress(""), Set.of());
        final User robin = new User(new UserId("robin"), new UserLocalId(3L), "Dick", "Grayson", new EMailAddress(""), Set.of());

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

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""), Set.of());
        final User joker = new User(new UserId("joker"), new UserLocalId(2L), "Jack", "Napier", new EMailAddress(""), Set.of());
        final User robin = new User(new UserId("robin"), new UserLocalId(3L), "Dick", "Grayson", new EMailAddress(""), Set.of());

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

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
