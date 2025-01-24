package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.TimeEntryEditModalHelper;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
class ReportWeekController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final String REPORT_YEAR_WEEK_URL_TEMPLATE = "/report/year/%d/week/%d";

    private final ReportService reportService;
    private final ReportPermissionService reportPermissionService;
    private final ReportViewHelper reportViewHelper;
    private final TimeEntryEditModalHelper timeEntryEditModalHelper;
    private final Clock clock;

    ReportWeekController(ReportService reportService, ReportPermissionService reportPermissionService,
                         ReportViewHelper reportViewHelper, TimeEntryEditModalHelper timeEntryEditModalHelper,
                         Clock clock) {
        this.reportService = reportService;
        this.reportPermissionService = reportPermissionService;
        this.reportViewHelper = reportViewHelper;
        this.timeEntryEditModalHelper = timeEntryEditModalHelper;
        this.clock = clock;
    }

    @GetMapping("/report/week")
    public String weeklyUserReportToday(RedirectAttributes redirectAttributes, HttpServletRequest request) {

        redirectAttributes.mergeAttributes(request.getParameterMap());

        final YearWeek yearWeek = YearWeek.now(clock);

        return format("forward:/report/year/%s/week/%s", yearWeek.getYear(), yearWeek.getWeek());
    }

    @GetMapping("/report/year/{year}/week/{week}")
    public String weeklyUserReport(
        @PathVariable("year") Integer year,
        @PathVariable("week") Integer week,
        @RequestParam(value = "everyone", required = false) Optional<String> optionalAllUsersSelected,
        @RequestParam(value = "user", required = false) Optional<List<Long>> optionalUserIds,
        @RequestParam(value = "timeentry", required = false) Long id,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Model model, Locale locale) {

        if (id != null) {
            return weeklyUserReportWithDialog(id, model);
        }

        final YearWeek reportYearWeek = yearWeek(year, week)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));

        final Year reportYear = Year.of(reportYearWeek.getYear());
        final List<UserLocalId> selectedUserLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = optionalAllUsersSelected.isPresent();

        final ReportWeek reportWeek = getReportWeek(principal, reportYearWeek, allUsersSelected, reportYear, selectedUserLocalIds);
        final GraphWeekDto graphWeekDto = reportViewHelper.toGraphWeekDto(reportWeek, reportWeek.firstDateOfWeek().getMonth());
        final DetailWeekDto detailWeekDto = reportViewHelper.toDetailWeekDto(reportWeek, reportWeek.firstDateOfWeek().getMonth(), locale);

        model.addAttribute("weekReport", graphWeekDto);
        model.addAttribute("weekReportDetail", detailWeekDto);

        final YearWeek todayYearWeek = YearWeek.now(clock);
        model.addAttribute("isThisWeek", todayYearWeek.equals(reportYearWeek));

        model.addAttribute("chartNavigationFragment", "reports/user-report-week::chart-navigation");
        model.addAttribute("chartFragment", "reports/user-report-week::chart");
        model.addAttribute("entriesFragment", "reports/user-report-week::entries");
        model.addAttribute("weekAriaCurrent", "location");
        model.addAttribute("monthAriaCurrent", "false");

        final int previousYear = reportYearWeek.minusWeeks(1).getYear();
        final int previousWeek = reportYearWeek.minusWeeks(1).getWeek();
        final String previousSectionUrl = reportViewHelper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, previousYear, previousWeek), allUsersSelected, selectedUserLocalIds);

        final String todaySectionUrl = reportViewHelper.createUrl("/report/week", allUsersSelected, selectedUserLocalIds);

        final int nextYear = reportYearWeek.plusWeeks(1).getYear();
        final int nextWeek = reportYearWeek.plusWeeks(1).getWeek();
        final String nextSectionUrl = reportViewHelper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, nextYear, nextWeek), allUsersSelected, selectedUserLocalIds);

        final int selectedYear = reportYearWeek.getYear();
        final int selectedWeek = reportYearWeek.getWeek();
        final String selectedYearWeekUrl = reportViewHelper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, selectedYear, selectedWeek), allUsersSelected, selectedUserLocalIds);
        final String csvDownloadUrl = selectedYearWeekUrl.contains("?") ? selectedYearWeekUrl + "&csv" : selectedYearWeekUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        final List<User> users = reportPermissionService.findAllPermittedUsersForCurrentUser();

        reportViewHelper.addUserFilterModelAttributes(model, allUsersSelected, users, selectedUserLocalIds, format(REPORT_YEAR_WEEK_URL_TEMPLATE, year, week));
        reportViewHelper.addSelectedUserDurationAggregationModelAttributes(model, allUsersSelected, users, selectedUserLocalIds, reportWeek);

        return "reports/user-report";
    }

    private String weeklyUserReportWithDialog(Long timeEntryId, Model model) {
        timeEntryEditModalHelper.addTimeEntryEditToModel(model, timeEntryId);
        return "reports/user-report-edit-time-entry";
    }

    private ReportWeek getReportWeek(OidcUser principal, YearWeek reportYearWeek, boolean allUsersSelected, Year reportYear, List<UserLocalId> userLocalIds) {

        final ReportWeek reportWeek;

        if (allUsersSelected) {
            reportWeek = reportService.getReportWeekForAllUsers(reportYear, reportYearWeek.getWeek());
        } else if (userLocalIds.isEmpty()) {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), reportViewHelper.principalToUserId(principal));
        } else {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), userLocalIds);
        }

        return reportWeek;
    }

    private static Optional<YearWeek> yearWeek(int year, int week) {
        try {
            return Optional.of(YearWeek.of(Year.of(year), week));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearWeek with year={} week={}", year, week, exception);
            return Optional.empty();
        }
    }
}
