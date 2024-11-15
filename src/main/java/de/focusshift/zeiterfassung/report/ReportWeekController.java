package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
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
import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
class ReportWeekController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ReportService reportService;
    private final ReportControllerHelper helper;
    private final Clock clock;

    ReportWeekController(ReportService reportService, ReportControllerHelper helper, Clock clock) {
        this.reportService = reportService;
        this.helper = helper;
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
        @AuthenticationPrincipal DefaultOidcUser principal,
        Model model, Locale locale) {

        final YearWeek reportYearWeek = yearWeek(year, week)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));

        final Year reportYear = Year.of(reportYearWeek.getYear());
        final List<UserLocalId> userLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = optionalAllUsersSelected.isPresent();

        final ReportWeek reportWeek = getReportWeek(principal, reportYearWeek, allUsersSelected, reportYear, userLocalIds);
        final GraphWeekDto graphWeekDto = helper.toGraphWeekDto(reportWeek, reportWeek.firstDateOfWeek().getMonth());
        final DetailWeekDto detailWeekDto = helper.toDetailWeekDto(reportWeek, reportWeek.firstDateOfWeek().getMonth(), locale);

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
        final String previousSectionUrl = helper.createUrl(format("/report/year/%d/week/%d", previousYear, previousWeek), allUsersSelected, userLocalIds);

        final String todaySectionUrl = helper.createUrl("/report/week", allUsersSelected, userLocalIds);

        final int nextYear = reportYearWeek.plusWeeks(1).getYear();
        final int nextWeek = reportYearWeek.plusWeeks(1).getWeek();
        final String nextSectionUrl = helper.createUrl(format("/report/year/%d/week/%d", nextYear, nextWeek), allUsersSelected, userLocalIds);

        final int selectedYear = reportYearWeek.getYear();
        final int selectedWeek = reportYearWeek.getWeek();
        final String selectedYearWeekUrl = helper.createUrl(format("/report/year/%d/week/%d", selectedYear, selectedWeek), allUsersSelected, userLocalIds);
        final String csvDownloadUrl = selectedYearWeekUrl.contains("?") ? selectedYearWeekUrl + "&csv" : selectedYearWeekUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        helper.addUserFilterModelAttributes(model, allUsersSelected, userLocalIds, format("/report/year/%d/week/%d", year, week));
        model.addAttribute("selectedUserDurationAggregation", toReportSelectedUserDurationAggregationDto(reportWeek));

        return "reports/user-report";
    }

    private ReportWeek getReportWeek(OidcUser principal, YearWeek reportYearWeek, boolean allUsersSelected, Year reportYear, List<UserLocalId> userLocalIds) {

        final ReportWeek reportWeek;

        if (allUsersSelected) {
            reportWeek = reportService.getReportWeekForAllUsers(reportYear, reportYearWeek.getWeek());
        } else if (userLocalIds.isEmpty()) {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), helper.principalToUserId(principal));
        } else {
            reportWeek = reportService.getReportWeek(reportYear, reportYearWeek.getWeek(), userLocalIds);
        }

        return reportWeek;
    }

    record ReportSelectedUserDurationAggregationDto(Long userId, String delta, String worked, String should) {}

    private List<ReportSelectedUserDurationAggregationDto> toReportSelectedUserDurationAggregationDto(ReportWeek reportWeek) {

        final Map<UserIdComposite, WorkDuration> workedByUser = reportWeek.workDurationByUser();
        final Map<UserIdComposite, ShouldWorkingHours> shouldByUser = reportWeek.shouldWorkingHoursByUser();
        final Map<UserIdComposite, PlannedWorkingHours> plannedByUser = reportWeek.plannedWorkingHoursByUser();

        return plannedByUser.keySet().stream().map(userIdComposite -> new ReportSelectedUserDurationAggregationDto(
            userIdComposite.localId().value(),
            durationToTimeString(Duration.ZERO), // TODO
            durationToTimeString(workedByUser.get(userIdComposite).duration()),
            durationToTimeString(shouldByUser.get(userIdComposite).duration())
        )).toList();
    }

    private static Optional<YearWeek> yearWeek(int year, int week) {
        try {
            return Optional.of(YearWeek.of(Year.of(year), week));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearWeek with year={} week={}", year, week, exception);
            return Optional.empty();
        }
    }

    private static String durationToTimeString(Duration duration) {
        // use positive values to format duration string
        // negative value is handled in template
        return String.format("%02d:%02d", Math.abs(duration.toHours()), Math.abs(duration.toMinutesPart()));
    }
}
