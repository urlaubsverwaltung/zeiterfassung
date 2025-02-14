package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDTO;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDialogHelper;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.ScrollPreservation.PRESERVE;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_REFRESH_SCROLL_ATTRIBUTE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

@Controller
class ReportMonthController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ReportService reportService;

    private final DateFormatter dateFormatter;
    private final ReportViewHelper viewHelper;
    private final TimeEntryDialogHelper timeEntryDialogHelper;
    private final Clock clock;
    private final ReportPermissionService reportPermissionService;

    ReportMonthController(ReportService reportService, ReportPermissionService reportPermissionService,
                          DateFormatter dateFormatter, ReportViewHelper viewHelper,
                          TimeEntryDialogHelper timeEntryDialogHelper, Clock clock) {
        this.reportService = reportService;
        this.dateFormatter = dateFormatter;
        this.viewHelper = viewHelper;
        this.timeEntryDialogHelper = timeEntryDialogHelper;
        this.clock = clock;
        this.reportPermissionService = reportPermissionService;
    }

    @GetMapping("/report/month")
    public String monthlyUserReportToday(RedirectAttributes redirectAttributes, HttpServletRequest request) {

        redirectAttributes.mergeAttributes(request.getParameterMap());

        final YearMonth thisMonth = YearMonth.now(clock);

        return String.format("forward:/report/year/%s/month/%s", thisMonth.getYear(), thisMonth.getMonthValue());
    }

    @GetMapping("/report/year/{year}/month/{month}")
    public ModelAndView monthlyUserReport(
        @PathVariable("year") Integer year,
        @PathVariable("month") Integer month,
        @RequestParam(value = "everyone", required = false) String allUsersSelectedParam,
        @RequestParam(value = "user", required = false, defaultValue = "") List<Long> userIdsParam,
        @RequestParam(value = "timeEntryId", required = false) Long timeEntryId,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Model model, Locale locale
    ) {

        if (timeEntryId != null) {
            return monthlyUserReportWithDialog(year, month, allUsersSelectedParam, userIdsParam, timeEntryId, model);
        }

        final YearMonth yearMonth = yearMonth(year, month)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid month."));

        final List<UserLocalId> userLocalIds = userIdsParam.stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = allUsersSelectedParam != null;

        final ReportMonth reportMonth = getReportMonth(principal, allUsersSelected, yearMonth, userLocalIds);
        final GraphMonthDto graphMonthDto = toGraphMonthDto(reportMonth);
        final DetailMonthDto detailMonthDto = toDetailMonthDto(reportMonth, allUsersSelectedParam, userIdsParam, locale);

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
        final String previousSectionUrl = viewHelper.createUrl(String.format("/report/year/%d/month/%d", previousYear, previousMonth), allUsersSelected, userLocalIds);

        final String todaySectionUrl = viewHelper.createUrl("/report/month", allUsersSelected, userLocalIds);

        final int nextYear = month == 12 ? year + 1 : year;
        final int nextMonth = month == 12 ? 1 : month + 1;
        final String nextSectionUrl = viewHelper.createUrl(String.format("/report/year/%d/month/%d", nextYear, nextMonth), allUsersSelected, userLocalIds);

        final int selectedYear = year;
        final int selectedMonth = month;
        final String selectedYearMonthUrl = viewHelper.createUrl(String.format("/report/year/%d/month/%d", selectedYear, selectedMonth), allUsersSelected, userLocalIds);
        final String csvDownloadUrl = selectedYearMonthUrl.contains("?") ? selectedYearMonthUrl + "&csv" : selectedYearMonthUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        final List<User> users = reportPermissionService.findAllPermittedUsersForCurrentUser();

        viewHelper.addUserFilterModelAttributes(model, allUsersSelected, users, userLocalIds, String.format("/report/year/%d/month/%d", year, month));
        viewHelper.addSelectedUserDurationAggregationModelAttributes(model, allUsersSelected, users, userLocalIds, reportMonth);

        return new ModelAndView("reports/user-report");
    }

    @PostMapping("/report/year/{year}/month/{month}")
    public ModelAndView postEditTimeEntry(
        @PathVariable("year") Integer year,
        @PathVariable("month") Integer month,
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO, BindingResult errors,
        @RequestParam(value = "everyone", required = false) String allUsersSelectedParam,
        @RequestParam(value = "user", required = false, defaultValue = "") List<Long> userIdsParam,
        Model model,
        @AuthenticationPrincipal OidcUser oidcUser,
        RedirectAttributes redirectAttributes) {

        timeEntryDialogHelper.saveTimeEntry(timeEntryDTO, errors, model, redirectAttributes, oidcUser);
        if (errors.hasErrors()) {
            LOG.debug("validation errors occurred on editing TimeEntry via ReportWeek TimeEntry Dialog. Redirecting to Dialog.");
            final String url = getMonthlyUserReportUrl(year, month, allUsersSelectedParam, userIdsParam, timeEntryDTO.getId());
            return new ModelAndView("redirect:%s".formatted(url));
        }

        // preserve scroll position after editing a timeEntry
        redirectAttributes.addFlashAttribute(TURBO_REFRESH_SCROLL_ATTRIBUTE, PRESERVE);

        final String url = getMonthlyUserReportUrl(year, month, allUsersSelectedParam, userIdsParam, null);
        return new ModelAndView("redirect:%s".formatted(url));
    }

    private ModelAndView monthlyUserReportWithDialog(int year, int month, @Nullable String everyoneParam, List<Long> userParam, Long timeEntryId, Model model) {

        final String editFormAction = getEditTimeEntryFormAction(year, month, everyoneParam, userParam);
        final String cancelAction = getMonthlyUserReportUrl(year, month, everyoneParam, userParam, null);
        timeEntryDialogHelper.addTimeEntryEditToModel(model, timeEntryId, editFormAction, cancelAction);

        return new ModelAndView("reports/user-report-edit-time-entry");
    }

    private ReportMonth getReportMonth(OidcUser principal, boolean allUsersSelected, YearMonth yearMonth, List<UserLocalId> userLocalIds) {

        final ReportMonth reportMonth;

        if (allUsersSelected) {
            reportMonth = reportService.getReportMonthForAllUsers(yearMonth);
        } else if (userLocalIds.isEmpty()) {
            reportMonth = reportService.getReportMonth(yearMonth, viewHelper.principalToUserId(principal));
        } else {
            reportMonth = reportService.getReportMonth(yearMonth, userLocalIds);
        }

        return reportMonth;
    }

    private GraphMonthDto toGraphMonthDto(ReportMonth reportMonth) {

        final List<GraphWeekDto> graphWeekDtos = reportMonth.weeks().stream()
            .map(reportWeek -> viewHelper.toGraphWeekDto(reportWeek, reportMonth.yearMonth().getMonth()))
            .toList();

        final String yearMonth = dateFormatter.formatYearMonth(reportMonth.yearMonth());

        final double maxHoursWorked = graphWeekDtos.stream()
            .flatMap(graphWeekDto -> graphWeekDto.dayReports().stream())
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final WorkDuration workDuration = reportMonth.workDuration();
        final ShouldWorkingHours shouldWorkingHours = reportMonth.shouldWorkingHours();
        final String shouldWorkingHoursString = durationToTimeString(shouldWorkingHours.duration());
        final String workedWorkingHoursString = durationToTimeString(workDuration.duration());

        final Duration deltaDuration = workDuration.duration().minus(shouldWorkingHours.duration());
        final String deltaHours = durationToTimeString(deltaDuration);

        final double weekRatio = reportMonth.workedHoursRatio().multiply(BigDecimal.valueOf(100), new MathContext(2)).doubleValue();

        return new GraphMonthDto(yearMonth, graphWeekDtos, maxHoursWorked, workedWorkingHoursString, shouldWorkingHoursString, deltaHours, deltaDuration.isNegative(), weekRatio);
    }

    private DetailMonthDto toDetailMonthDto(ReportMonth reportMonth, @Nullable String everyoneParam, List<Long> userParam, Locale locale) {

        final YearMonth yearMonth = reportMonth.yearMonth();

        final List<DetailWeekDto> weeks = reportMonth.weeks()
            .stream()
            .map(week -> viewHelper.toDetailWeekDto(week, reportMonth.yearMonth().getMonth(), locale,
                id -> getMonthlyUserReportUrl(yearMonth.getYear(), yearMonth.getMonthValue(), everyoneParam, userParam, id.value())))
            .toList();

        final String yearMonthFormatted = dateFormatter.formatYearMonth(yearMonth);

        return new DetailMonthDto(yearMonthFormatted, weeks);
    }

    /**
     * Creates the base url to be used for editing a timeEntry on the report view.
     *
     * @param year report year
     * @param month report month
     * @param everyoneParam whether every user should be shown, or not
     * @param userParam list of user ids that should be shown
     */
    private String getEditTimeEntryFormAction(int year, int month, @Nullable String everyoneParam, List<Long> userParam) {
        return fromMethodCall(on(ReportMonthController.class)
            .postEditTimeEntry(year, month, null, null, everyoneParam, userParam, null, null, null))
            .build().toUriString();
    }

    /**
     * Creates the url which then shows report view or the timeEntry dialog. Depends on value of timeEntryId.
     *
     * @param year report year
     * @param month report month
     * @param everyoneParam whether every user should be shown, or not
     * @param userParam list of user ids that should be shown
     * @param timeEntryId <code>null</code> to show reports, <code>timeEntryId</code> to show the dialog
     */
    private String getMonthlyUserReportUrl(int year, int month, @Nullable String everyoneParam, List<Long> userParam, @Nullable Long timeEntryId) {
        return fromMethodCall(on(ReportMonthController.class)
            .monthlyUserReport(year, month, everyoneParam, userParam, timeEntryId, null,null,null))
            .build().toUriString();
    }

    private static Optional<YearMonth> yearMonth(int year, int month) {
        try {
            return Optional.of(YearMonth.of(year, month));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearMonth with year={} month={}", year, month, exception);
            return Optional.empty();
        }
    }

    private static String durationToTimeString(Duration duration) {
        // use positive values to format duration string
        // negative value is handled in template
        return String.format("%02d:%02d", Math.abs(duration.toHours()), Math.abs(duration.toMinutesPart()));
    }
}
