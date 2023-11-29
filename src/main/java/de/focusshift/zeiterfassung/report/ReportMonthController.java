package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletRequest;
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

import java.time.Clock;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@PreAuthorize("hasAuthority('ZEITERFASSUNG_USER')")
class ReportMonthController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ReportService reportService;

    private final DateFormatter dateFormatter;
    private final ReportControllerHelper helper;
    private final Clock clock;

    ReportMonthController(ReportService reportService, DateFormatter dateFormatter, ReportControllerHelper helper, Clock clock) {
        this.reportService = reportService;
        this.dateFormatter = dateFormatter;
        this.helper = helper;
        this.clock = clock;
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
        final DetailMonthDto detailMonthDto = toDetailMonthDto(reportMonth);

        model.addAttribute("monthReport", graphMonthDto);
        model.addAttribute("monthReportDetail", detailMonthDto);

        final YearMonth todayYearMonth = YearMonth.now(clock);
        model.addAttribute("isThisMonth", todayYearMonth.equals(YearMonth.of(year, month)));

        model.addAttribute("chartNavigationFragment", "reports/user-report-month::chart-navigation");
        model.addAttribute("chartFragment", "reports/user-report-month::chart");
        model.addAttribute("entriesFragment", "reports/user-report-month::entries");
        model.addAttribute("weekAriaCurrent", "false");
        model.addAttribute("monthAriaCurrent", "location");

        final int previousYear = month == 1 ? year - 1 : year;
        final int previousMonth = month == 1 ? 12 : month - 1;
        final String previousSectionUrl = helper.createUrl(String.format("/report/year/%d/month/%d", previousYear, previousMonth), allUsersSelected, userLocalIds);

        final String todaySectionUrl = helper.createUrl("/report/month", allUsersSelected, userLocalIds);

        final int nextYear = month == 12 ? year + 1 : year;
        final int nextMonth = month == 12 ? 1 : month + 1;
        final String nextSectionUrl = helper.createUrl(String.format("/report/year/%d/month/%d", nextYear, nextMonth), allUsersSelected, userLocalIds);

        final int selectedYear = year;
        final int selectedMonth = month;
        final String selectedYearMonthUrl = helper.createUrl(String.format("/report/year/%d/month/%d", selectedYear, selectedMonth), allUsersSelected, userLocalIds);
        final String csvDownloadUrl = selectedYearMonthUrl.contains("?") ? selectedYearMonthUrl + "&csv" : selectedYearMonthUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        helper.addUserFilterModelAttributes(model, allUsersSelected, userLocalIds, String.format("/report/year/%d/month/%d", year, month));

        return "reports/user-report";
    }

    private ReportMonth getReportMonth(OidcUser principal, boolean allUsersSelected, YearMonth yearMonth, List<UserLocalId> userLocalIds) {

        final ReportMonth reportMonth;

        if (allUsersSelected) {
            reportMonth = reportService.getReportMonthForAllUsers(yearMonth);
        } else if (userLocalIds.isEmpty()) {
            reportMonth = reportService.getReportMonth(yearMonth, helper.principalToUserId(principal));
        } else {
            reportMonth = reportService.getReportMonth(yearMonth, userLocalIds);
        }

        return reportMonth;
    }

    private GraphMonthDto toGraphMonthDto(ReportMonth reportMonth) {

        final List<GraphWeekDto> graphWeekDtos = reportMonth.weeks().stream()
            .map(reportWeek -> helper.toGraphWeekDto(reportWeek, reportMonth.yearMonth().getMonth()))
            .toList();

        final String yearMonth = dateFormatter.formatYearMonth(reportMonth.yearMonth());

        final double maxHoursWorked = graphWeekDtos.stream()
            .flatMap(graphWeekDto -> graphWeekDto.dayReports().stream())
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final double hoursWorkedAverageADay = reportMonth.averageDayWorkDuration().hoursDoubleValue();

        return new GraphMonthDto(yearMonth, graphWeekDtos, maxHoursWorked, hoursWorkedAverageADay);
    }

    private DetailMonthDto toDetailMonthDto(ReportMonth reportMonth) {

        final List<DetailWeekDto> weeks = reportMonth.weeks()
            .stream()
            .map(week -> helper.toDetailWeekDto(week, reportMonth.yearMonth().getMonth()))
            .toList();

        final String yearMonth = dateFormatter.formatYearMonth(reportMonth.yearMonth());

        return new DetailMonthDto(yearMonth, weeks);
    }

    private static Optional<YearMonth> yearMonth(int year, int month) {
        try {
            return Optional.of(YearMonth.of(year, month));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearMonth with year={} month={}", year, month, exception);
            return Optional.empty();
        }
    }
}
