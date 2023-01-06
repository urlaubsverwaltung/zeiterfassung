package de.focusshift.zeiterfassung.report;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.threeten.extra.YearWeek;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class ReportController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ReportService reportService;
    private final ReportPermissionService reportPermissionService;
    private final DateFormatter dateFormatter;
    private final Clock clock;

    ReportController(ReportService reportService, ReportPermissionService reportPermissionService, DateFormatter dateFormatter, Clock clock) {
        this.reportService = reportService;
        this.reportPermissionService = reportPermissionService;
        this.dateFormatter = dateFormatter;
        this.clock = clock;
    }

    @GetMapping("/report")
    public String userReport(RedirectAttributes redirectAttributes, HttpServletRequest request) {

        redirectAttributes.mergeAttributes(request.getParameterMap());

        return "forward:/report/week";
    }

    @GetMapping("/report/week")
    public String weeklyUserReportToday(RedirectAttributes redirectAttributes, HttpServletRequest request) {

        redirectAttributes.mergeAttributes(request.getParameterMap());

        final YearWeek yearWeek = YearWeek.now(clock);

        return String.format("forward:/report/year/%s/week/%s", yearWeek.getYear(), yearWeek.getWeek());
    }

    @GetMapping("/report/year/{year}/week/{week}")
    public String weeklyUserReport(
        @PathVariable("year") Integer year,
        @PathVariable("week") Integer week,
        @RequestParam(value = "everyone", required = false) Optional<String> optionalAllUsersSelected,
        @RequestParam(value = "user", required = false) Optional<List<Long>> optionalUserIds,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Model model) {

        final YearWeek reportYearWeek = yearWeek(year, week)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));

        final Year reportYear = Year.of(reportYearWeek.getYear());
        final List<UserLocalId> userLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = optionalAllUsersSelected.isPresent();

        final ReportWeek reportWeek = getReportWeek(principal, reportYearWeek, allUsersSelected, reportYear, userLocalIds);
        final GraphWeekDto graphWeekDto = toGraphWeekDto(reportWeek, reportWeek.firstDateOfWeek().getMonth());

        model.addAttribute("weekReport", graphWeekDto);

        final YearWeek todayYearWeek = YearWeek.now(clock);
        model.addAttribute("isThisWeek", todayYearWeek.equals(reportYearWeek));

        model.addAttribute("chartNavigationFragment", "reports/user-report-week::chart-navigation");
        model.addAttribute("chartFragment", "reports/user-report-week::chart");
        model.addAttribute("weekAriaCurrent", "location");
        model.addAttribute("monthAriaCurrent", "false");

        final int previousYear = reportYearWeek.minusWeeks(1).getYear();
        final int previousWeek = reportYearWeek.minusWeeks(1).getWeek();
        final String previousSectionUrl = createUrl(String.format("/report/year/%d/week/%d", previousYear, previousWeek), allUsersSelected, userLocalIds);

        final String todaySectionUrl = createUrl("/report/week", allUsersSelected, userLocalIds);

        final int nextYear = reportYearWeek.plusWeeks(1).getYear();
        final int nextWeek = reportYearWeek.plusWeeks(1).getWeek();
        final String nextSectionUrl = createUrl(String.format("/report/year/%d/week/%d", nextYear, nextWeek), allUsersSelected, userLocalIds);

        final int selectedYear = reportYearWeek.getYear();
        final int selectedWeek = reportYearWeek.getWeek();
        final String selectedYearWeekUrl = createUrl(String.format("/report/year/%d/week/%d", selectedYear, selectedWeek), allUsersSelected, userLocalIds);
        final String csvDownloadUrl = selectedYearWeekUrl.contains("?") ? selectedYearWeekUrl + "&csv" : selectedYearWeekUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        addUserFilterModelAttributes(model, allUsersSelected, userLocalIds, String.format("/report/year/%d/week/%d", year, week));

        return "reports/user-report";
    }

    @GetMapping("/report/month")
    public String monthlyUserReportToday(RedirectAttributes redirectAttributes, HttpServletRequest request) {

        redirectAttributes.mergeAttributes(request.getParameterMap());

        final YearMonth thisMonth = YearMonth.now(clock);

        return String.format("forward:/report/year/%s/month/%s", thisMonth.getYear(), thisMonth.getMonthValue());
    }

    @GetMapping("/report/year/{year}/month/{month}")
    public String monthlyUserReport(
        @PathVariable("year") Integer year,
        @PathVariable("month") Integer month,
        @RequestParam(value = "everyone", required = false) Optional<String> optionalAllUsersSelected,
        @RequestParam(value = "user", required = false) Optional<List<Long>> optionalUserIds,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Model model
    ) {

        final YearMonth yearMonth = yearMonth(year, month)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid month."));

        final List<UserLocalId> userLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = optionalAllUsersSelected.isPresent();

        final ReportMonth reportMonth = getReportMonth(principal, allUsersSelected, yearMonth, userLocalIds);
        final GraphMonthDto graphMonthDto = toGraphMonthDto(reportMonth);

        model.addAttribute("monthReport", graphMonthDto);

        final YearMonth todayYearMonth = YearMonth.now(clock);
        model.addAttribute("isThisMonth", todayYearMonth.equals(YearMonth.of(year, month)));

        model.addAttribute("chartNavigationFragment", "reports/user-report-month::chart-navigation");
        model.addAttribute("chartFragment", "reports/user-report-month::chart");
        model.addAttribute("weekAriaCurrent", "false");
        model.addAttribute("monthAriaCurrent", "location");

        final int previousYear = month == 1 ? year - 1 : year;
        final int previousMonth = month == 1 ? 12 : month - 1;
        final String previousSectionUrl = createUrl(String.format("/report/year/%d/month/%d", previousYear, previousMonth), allUsersSelected, userLocalIds);

        final String todaySectionUrl = createUrl("/report/month", allUsersSelected, userLocalIds);

        final int nextYear = month == 12 ? year + 1 : year;
        final int nextMonth = month == 12 ? 1 : month + 1;
        final String nextSectionUrl = createUrl(String.format("/report/year/%d/month/%d", nextYear, nextMonth), allUsersSelected, userLocalIds);

        final int selectedYear = year;
        final int selectedMonth = month;
        final String selectedYearMonthUrl = createUrl(String.format("/report/year/%d/month/%d", selectedYear, selectedMonth), allUsersSelected, userLocalIds);
        final String csvDownloadUrl = selectedYearMonthUrl.contains("?") ? selectedYearMonthUrl + "&csv" : selectedYearMonthUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        addUserFilterModelAttributes(model, allUsersSelected, userLocalIds, String.format("/report/year/%d/month/%d", year, month));

        return "reports/user-report";
    }

    private ReportWeek getReportWeek(OidcUser principal, YearWeek reportYearWeek, boolean allUsersSelected, Year reportYear, List<UserLocalId> userLocalIds) {
        final ReportWeek reportWeek;

        if (allUsersSelected) {
            reportWeek = reportService.getReportWeekForAllUsers(reportYear, reportYearWeek.getWeek());
        } else if (userLocalIds.isEmpty()) {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), principalToUserId(principal));
        } else {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), userLocalIds);
        }

        return reportWeek;
    }

    private ReportMonth getReportMonth(OidcUser principal, boolean allUsersSelected, YearMonth yearMonth, List<UserLocalId> userLocalIds) {
        final ReportMonth reportMonth;

        if (allUsersSelected) {
            reportMonth = reportService.getReportMonthForAllUsers(yearMonth);
        } else if (userLocalIds.isEmpty()) {
            reportMonth = reportService.getReportMonth(yearMonth, principalToUserId(principal));
        } else {
            reportMonth = reportService.getReportMonth(yearMonth, userLocalIds);
        }

        return reportMonth;
    }

    private static UserId principalToUserId(OidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }

    private String createUrl(String prefix, boolean allUsersSelected, List<UserLocalId> selectedUserLocalIds) {
        String url = prefix;

        if (allUsersSelected || !selectedUserLocalIds.isEmpty()) {
            url += "?";
        }

        if (allUsersSelected) {
            url += "everyone=";
        }

        final String usersParam = selectedUserLocalIds.stream()
            .map(UserLocalId::value)
            .map(id -> "user=" + id)
            .collect(joining("&"));

        if (!usersParam.isEmpty()) {
            if (allUsersSelected) {
                url += "&";
            }
            url += usersParam;
        }

        return url;
    }

    private GraphMonthDto toGraphMonthDto(ReportMonth reportMonth) {
        final List<GraphWeekDto> graphWeekDtos = reportMonth.weeks().stream()
            .map(reportWeek -> toGraphWeekDto(reportWeek, reportMonth.yearMonth().getMonth()))
            .toList();

        final double maxHoursWorked = graphWeekDtos.stream()
            .flatMap(graphWeekDto -> graphWeekDto.dayReports().stream())
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final double hoursWorkedAverageADay = graphWeekDtos.stream()
            .flatMap(graphWeekDto -> graphWeekDto.dayReports().stream())
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .average().orElse(0.0);

        final double averageHoursWorkedRounded = BigDecimal.valueOf(hoursWorkedAverageADay)
            .setScale(2, RoundingMode.DOWN)
            .doubleValue();

        final String yearMonth = dateFormatter.formatYearMonth(reportMonth.yearMonth());

        return new GraphMonthDto(yearMonth, graphWeekDtos, maxHoursWorked, averageHoursWorkedRounded);
    }

    private GraphWeekDto toGraphWeekDto(ReportWeek reportWeek, Month monthPivot) {
        final List<GraphDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toUserReportDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot)))
            .toList();

        final String yearMonthWeek = dateFormatter.formatYearMonthWeek(reportWeek.firstDateOfWeek());

        final double maxHoursWorked = dayReports.stream()
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final double hoursWorkedAverageADay = dayReports.stream()
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .average().orElse(0.0);

        return new GraphWeekDto(yearMonthWeek, dayReports, maxHoursWorked, hoursWorkedAverageADay);
    }

    private GraphDayDto toUserReportDayReportDto(ReportDay reportDay, boolean differentMonth) {
        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());
        final double hoursWorked = reportDay.workDuration().minutes().hoursDoubleValue();

        return new GraphDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, hoursWorked);
    }

    private static Optional<YearWeek> yearWeek(int year, int week) {
        try {
            return Optional.of(YearWeek.of(Year.of(year), week));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearWeek with year={} week={}", year, week, exception);
            return Optional.empty();
        }
    }

    private static Optional<YearMonth> yearMonth(int year, int month) {
        try {
            return Optional.of(YearMonth.of(year, month));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearMonth with year={} month={}", year, month, exception);
            return Optional.empty();
        }
    }

    private void addUserFilterModelAttributes(Model model, boolean allUsersSelected, List<UserLocalId> selectedUserLocalIds, String userReportFilterUrl) {
        final List<User> permittedUsers = reportPermissionService.findAllPermittedUsersForCurrentUser();
        if (permittedUsers.size() > 1) {
            final List<SelectableUserDto> selectableUserDtos = permittedUsers
                .stream()
                .map(user -> userToSelectableUserDto(user, selectedUserLocalIds.contains(user.localId())))
                .toList();

            model.addAttribute("users", selectableUserDtos);
            model.addAttribute("selectedUserIds", selectedUserLocalIds.stream().map(UserLocalId::value).toList());
            model.addAttribute("allUsersSelected", allUsersSelected);
            model.addAttribute("userReportFilterUrl", userReportFilterUrl);
        }
    }

    private static SelectableUserDto userToSelectableUserDto(User user, boolean selected) {
        return new SelectableUserDto(user.localId().value(), user.givenName() + " " + user.familyName(), selected);
    }
}
