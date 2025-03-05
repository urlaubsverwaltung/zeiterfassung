package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDTO;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDialogHelper;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryViewHelper;
import de.focusshift.zeiterfassung.user.DateFormatterImpl;
import de.focusshift.zeiterfassung.user.DateRangeFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.ZoneOffset.UTC;
import static java.util.Locale.GERMAN;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
class ReportMonthControllerTest implements ControllerTest {

    private ReportMonthController sut;

    @Mock
    private ReportService reportService;
    @Mock
    private ReportPermissionService reportPermissionService;
    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private MessageSource messageSource;

    private DateFormatterImpl dateFormatter;
    private DateRangeFormatter dateRangeFormatter;
    private ReportViewHelper reportViewHelper;
    private TimeEntryViewHelper timeEntryViewHelper;
    private TimeEntryDialogHelper timeEntryDialogHelper;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        dateFormatter = new DateFormatterImpl();
        dateRangeFormatter = new DateRangeFormatter(dateFormatter, messageSource);
        reportViewHelper = new ReportViewHelper(dateFormatter, dateRangeFormatter);
        timeEntryViewHelper = new TimeEntryViewHelper(timeEntryService, userSettingsProvider, authenticationFacade);
        timeEntryDialogHelper = new TimeEntryDialogHelper(timeEntryService, timeEntryViewHelper, userSettingsProvider, userManagementService);
        sut = new ReportMonthController(reportService, reportPermissionService, dateFormatter, reportViewHelper, timeEntryDialogHelper, clock);
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

        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final ReportMonth reportMonth = new ReportMonth(
            YearMonth.of(2023, 2),
            List.of(
                new ReportWeek(
                    LocalDate.of(2023, 1, 30),
                    List.of(
                        zeroHoursDay(LocalDate.of(2023, 1, 30), user),
                        zeroHoursDay(LocalDate.of(2023, 1, 31), user),
                        eightHoursDay(LocalDate.of(2023, 2, 1), user),
                        eightHoursDay(LocalDate.of(2023, 2, 2), user),
                        eightHoursDay(LocalDate.of(2023, 2, 3), user),
                        zeroHoursDay(LocalDate.of(2023, 2, 4), user),
                        zeroHoursDay(LocalDate.of(2023, 2, 5), user)
                    )
                ),
                fourtyHourWeek(user, LocalDate.of(2023, 2, 6)),
                fourtyHourWeek(user, LocalDate.of(2023, 2, 13)),
                fourtyHourWeek(user, LocalDate.of(2023, 2, 20)),
                new ReportWeek(
                    LocalDate.of(2023, 2, 27),
                    List.of(
                        eightHoursDay(LocalDate.of(2023, 2, 27), user),
                        eightHoursDay(LocalDate.of(2023, 2, 28), user),
                        zeroHoursDay(LocalDate.of(2023, 3, 1), user),
                        zeroHoursDay(LocalDate.of(2023, 3, 2), user),
                        zeroHoursDay(LocalDate.of(2023, 3, 3), user),
                        zeroHoursDay(LocalDate.of(2023, 3, 4), user),
                        zeroHoursDay(LocalDate.of(2023, 3, 5), user)
                    )
                )
            )
        );

        when(reportService.getReportMonth(YearMonth.of(2023, 2), userId))
            .thenReturn(reportMonth);

        final GraphMonthDto graphMonthDto = new GraphMonthDto(
            "Februar 2023",
            List.of(
                new GraphWeekDto(
        5,
                    "date-range",
                    List.of(
                        new GraphDayDto(true, "M", "Montag", "30.01.2023", 0d, 0d),
                        new GraphDayDto(true, "D", "Dienstag", "31.01.2023", 0d, 0d),
                        new GraphDayDto(false, "M", "Mittwoch", "01.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Donnerstag", "02.02.2023", 8d, 8d),
                        new GraphDayDto(false, "F", "Freitag", "03.02.2023", 8d, 8d),
                        new GraphDayDto(false, "S", "Samstag", "04.02.2023", 0d, 0d),
                        new GraphDayDto(false, "S", "Sonntag", "05.02.2023", 0d, 0d)
                    ),
                    8d,
                    "24:00",
                    "24:00",
                    "00:00",
                    false,
                    100d
                ),
                new GraphWeekDto(
        6,
                    "date-range",
                    List.of(
                        new GraphDayDto(false, "M", "Montag", "06.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Dienstag", "07.02.2023", 8d, 8d),
                        new GraphDayDto(false, "M", "Mittwoch", "08.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Donnerstag", "09.02.2023", 8d, 8d),
                        new GraphDayDto(false, "F", "Freitag", "10.02.2023", 8d, 8d),
                        new GraphDayDto(false, "S", "Samstag", "11.02.2023", 0d, 0d),
                        new GraphDayDto(false, "S", "Sonntag", "12.02.2023", 0d, 0d)
                    ),
                    8d,
                    "40:00",
                    "40:00",
                    "00:00",
                    false,
                    100d
                ),
                new GraphWeekDto(
        7,
                    "date-range",
                    List.of(
                        new GraphDayDto(false, "M", "Montag", "13.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Dienstag", "14.02.2023", 8d, 8d),
                        new GraphDayDto(false, "M", "Mittwoch", "15.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Donnerstag", "16.02.2023", 8d, 8d),
                        new GraphDayDto(false, "F", "Freitag", "17.02.2023", 8d, 8d),
                        new GraphDayDto(false, "S", "Samstag", "18.02.2023", 0d, 0d),
                        new GraphDayDto(false, "S", "Sonntag", "19.02.2023", 0d, 0d)
                    ),
                    8d,
                    "40:00",
                    "40:00",
                    "00:00",
                    false,
                    100d
                ),
                new GraphWeekDto(
        8,
                    "date-range",
                    List.of(
                        new GraphDayDto(false, "M", "Montag", "20.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Dienstag", "21.02.2023", 8d, 8d),
                        new GraphDayDto(false, "M", "Mittwoch", "22.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Donnerstag", "23.02.2023", 8d, 8d),
                        new GraphDayDto(false, "F", "Freitag", "24.02.2023", 8d, 8d),
                        new GraphDayDto(false, "S", "Samstag", "25.02.2023", 0d, 0d),
                        new GraphDayDto(false, "S", "Sonntag", "26.02.2023", 0d, 0d)
                    ),
                    8d,
                    "40:00",
                    "40:00",
                    "00:00",
                    false,
                    100d
                ),
                new GraphWeekDto(
        9,
                    "date-range",
                    List.of(
                        new GraphDayDto(false, "M", "Montag", "27.02.2023", 8d, 8d),
                        new GraphDayDto(false, "D", "Dienstag", "28.02.2023", 8d, 8d),
                        new GraphDayDto(true, "M", "Mittwoch", "01.03.2023", 0d, 0d),
                        new GraphDayDto(true, "D", "Donnerstag", "02.03.2023", 0d, 0d),
                        new GraphDayDto(true, "F", "Freitag", "03.03.2023", 0d, 0d),
                        new GraphDayDto(true, "S", "Samstag", "04.03.2023", 0d, 0d),
                        new GraphDayDto(true, "S", "Sonntag", "05.03.2023", 0d, 0d)
                    ),
                    8d,
                    "16:00",
                    "16:00",
                    "00:00",
                    false,
                    100d
                )
            ),
            8d,
            "160:00",
            "160:00",
            "00:00",
            false,
            100d
        );

        perform(
            get("/report/year/2023/month/2")
                .with(oidcSubject(userIdComposite))
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
                .with(oidcSubject("batman"))
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
                .with(oidcSubject("batman"))
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
                .with(oidcSubject("batman"))
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
                .with(oidcSubject("batman"))
        )
            .andExpect(model().attribute("userReportCsvDownloadUrl", "/report/year/2022/month/1?csv"));
    }

    @Test
    void ensureMonthReportCsvDownloadUrlWithEveryoneParam() throws Exception {

        when(reportService.getReportMonthForAllUsers(YearMonth.of(2022, 1)))
            .thenReturn(anyReportMonth());

        perform(
            get("/report/year/2022/month/1")
                .with(oidcSubject("batman"))
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
                .with(oidcSubject("batman"))
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

        perform(get("/report/year/2022/month/1").with(oidcSubject("batman")))
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

        perform(get("/report/year/2022/month/1").with(oidcSubject("batman")))
            .andExpect(model().attribute("users", List.of(
                new SelectableUserDto(1L, "Bruce Wayne", false),
                new SelectableUserDto(3L, "Dick Grayson", false),
                new SelectableUserDto(2L, "Jack Napier", false))
            ))
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
            .andExpect(model().attribute("userReportFilterUrl", "/report/year/2022/month/1"));
    }

    @Nested
    class EditTimeEntry {

        @BeforeEach
        void setUp() {
            timeEntryDialogHelper = mock(TimeEntryDialogHelper.class);
            sut = new ReportMonthController(reportService, reportPermissionService, dateFormatter, reportViewHelper, timeEntryDialogHelper, clock);
        }

        @Test
        void ensureEditTimeEntryWithValidationConstraints() throws Exception {

            perform(post("/report/year/2025/month/2")
                .param("id", "1")
            )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/report/year/2025/month/2?timeEntryId=1"));

            // must be called even with initial constraint violations
            // since the helper adds further stuff to the model
            verify(timeEntryDialogHelper).saveTimeEntry(any(TimeEntryDTO.class), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
        }

        @Test
        void ensureEditTimeEntry() throws Exception {

            perform(post("/report/year/2025/month/2")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/report/year/2025/month/2"))
                .andExpect(flash().attribute("turboRefreshScroll", "preserve"));

            verify(timeEntryDialogHelper).saveTimeEntry(any(TimeEntryDTO.class), any(BindingResult.class), any(Model.class), any(RedirectAttributes.class));
        }

        @Test
        void ensureEditTimeEntryWithParameterEveryone() throws Exception {

            perform(post("/report/year/2025/month/2")
                .param("everyone", "true")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(redirectedUrl("http://localhost/report/year/2025/month/2?everyone=true"));
        }

        @Test
        void ensureEditTimeEntryWithParameterUser() throws Exception {

            perform(post("/report/year/2025/month/2")
                .param("user", "1", "2")
                .param("date", "2025-02-28")
                .param("start", "14:30")
                .param("end", "15:00")
            )
                .andExpect(redirectedUrl("http://localhost/report/year/2025/month/2?user=1&user=2"));
        }
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
                Map.of(user.userIdComposite(), new WorkingTimeCalendar(Map.of(firstDateOfWeek.plusDays(5), PlannedWorkingHours.ZERO), Map.of(firstDateOfWeek.plusDays(5), List.of()))),
                Map.of(user.userIdComposite(), List.of()),
                Map.of(user.userIdComposite(), List.of())
            ),
            new ReportDay(
                firstDateOfWeek.plusDays(6),
                Map.of(user.userIdComposite(), new WorkingTimeCalendar(Map.of(firstDateOfWeek.plusDays(6), PlannedWorkingHours.ZERO), Map.of(firstDateOfWeek.plusDays(6), List.of()))),
                Map.of(user.userIdComposite(), List.of()),
                Map.of(user.userIdComposite(), List.of())
            ))
        );
    }

    private ReportDay eightHoursDay(LocalDate date, User user) {
        return new ReportDay(
            date,
            Map.of(user.userIdComposite(), new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.EIGHT), Map.of(date, List.of()))),
            Map.of(user.userIdComposite(), List.of(reportDayEntry(user, date))),
            Map.of(user.userIdComposite(), List.of())
        );
    }

    private ReportDay zeroHoursDay(LocalDate date, User user) {
        return new ReportDay(
            date,
            Map.of(user.userIdComposite(), new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.ZERO), Map.of(date, List.of()))),
            Map.of(user.userIdComposite(), List.of()),
            Map.of(user.userIdComposite(), List.of())
        );
    }

    private ReportDayEntry reportDayEntry(User user, LocalDate date) {
        final long randomId = ThreadLocalRandom.current().nextLong();
        return new ReportDayEntry(new TimeEntryId(randomId), user, "", date.atStartOfDay().plusHours(8).atZone(UTC), date.atStartOfDay().plusHours(16).atZone(UTC), false);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
