package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDTO;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryUpdateNotPlausibleException;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.threeten.extra.YearWeek;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.util.StringUtils.hasText;

@Controller
class ReportWeekController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final String REPORT_YEAR_WEEK_URL_TEMPLATE = "/report/year/%d/week/%d";

    private final ReportService reportService;
    private final ReportPermissionService reportPermissionService;
    private final ReportControllerHelper helper;
    private final Clock clock;
    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;

    ReportWeekController(ReportService reportService, ReportPermissionService reportPermissionService,
                         ReportControllerHelper helper, Clock clock, TimeEntryService timeEntryService,
                         UserSettingsProvider userSettingsProvider) {
        this.reportService = reportService;
        this.reportPermissionService = reportPermissionService;
        this.helper = helper;
        this.clock = clock;
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
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
            final TimeEntry timeEntry = timeEntryService.findTimeEntry(id)
                .orElseThrow(() -> new IllegalArgumentException("Could not find timeEntry with id=%s".formatted(id)));

            final TimeEntryDTO dto = TimeEntryDTO.builder()
                .id(timeEntry.id().value())
                // TODO this has to be transformed to the user timezone (not just mapping to system localTime same hour/minute ...)
                .date(timeEntry.start().toLocalDate())
                .start(timeEntry.start().toLocalTime())
                .start(timeEntry.end().toLocalTime())
                .duration(toTimeEntryDTODurationString(timeEntry.durationInMinutes()))
                .isBreak(timeEntry.isBreak())
                .comment(timeEntry.comment())
                .build();

            model.addAttribute("timeEntry", dto);

            return "reports/user-report-edit-time-entry";
        }

        final YearWeek reportYearWeek = yearWeek(year, week)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));

        final Year reportYear = Year.of(reportYearWeek.getYear());
        final List<UserLocalId> selectedUserLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final boolean allUsersSelected = optionalAllUsersSelected.isPresent();

        final ReportWeek reportWeek = getReportWeek(principal, reportYearWeek, allUsersSelected, reportYear, selectedUserLocalIds);
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
        final String previousSectionUrl = helper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, previousYear, previousWeek), allUsersSelected, selectedUserLocalIds);

        final String todaySectionUrl = helper.createUrl("/report/week", allUsersSelected, selectedUserLocalIds);

        final int nextYear = reportYearWeek.plusWeeks(1).getYear();
        final int nextWeek = reportYearWeek.plusWeeks(1).getWeek();
        final String nextSectionUrl = helper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, nextYear, nextWeek), allUsersSelected, selectedUserLocalIds);

        final int selectedYear = reportYearWeek.getYear();
        final int selectedWeek = reportYearWeek.getWeek();
        final String selectedYearWeekUrl = helper.createUrl(format(REPORT_YEAR_WEEK_URL_TEMPLATE, selectedYear, selectedWeek), allUsersSelected, selectedUserLocalIds);
        final String csvDownloadUrl = selectedYearWeekUrl.contains("?") ? selectedYearWeekUrl + "&csv" : selectedYearWeekUrl + "?csv";

        model.addAttribute("userReportPreviousSectionUrl", previousSectionUrl);
        model.addAttribute("userReportTodaySectionUrl", todaySectionUrl);
        model.addAttribute("userReportNextSectionUrl", nextSectionUrl);
        model.addAttribute("userReportCsvDownloadUrl", csvDownloadUrl);

        final List<User> users = reportPermissionService.findAllPermittedUsersForCurrentUser();

        helper.addUserFilterModelAttributes(model, allUsersSelected, users, selectedUserLocalIds, format(REPORT_YEAR_WEEK_URL_TEMPLATE, year, week));
        helper.addSelectedUserDurationAggregationModelAttributes(model, allUsersSelected, users, selectedUserLocalIds, reportWeek);

        return "reports/user-report";
    }

//    @GetMapping("/report/year/{year}/week/{week}")
//    public String editTimeEntry(@PathVariable("year") Integer year, @PathVariable("week") Integer week, @RequestParam("timeentry") Long id, Model model, Locale locale) {
//
//        final TimeEntry timeEntry = timeEntryService.findTimeEntry(id)
//            .orElseThrow(() -> new IllegalArgumentException("Could not find timeEntry with id=%s".formatted(id)));
//
////        final User user = userManagementService.findUserByLocalId(timeEntry.userIdComposite().localId())
////            .orElseThrow(() -> new IllegalStateException("Could not find user with id=%s".formatted(timeEntry.userIdComposite())));
//
//        final TimeEntryDTO dto = TimeEntryDTO.builder()
//            .id(timeEntry.id().value())
//            // TODO this has to be transformed to the user timezone (not just mapping to system localTime same hour/minute ...)
//            .date(timeEntry.start().toLocalDate())
//            .start(timeEntry.start().toLocalTime())
//            .start(timeEntry.end().toLocalTime())
//            .duration(toTimeEntryDTODurationString(timeEntry.durationInMinutes()))
//            .isBreak(timeEntry.isBreak())
//            .comment(timeEntry.comment())
//            .build();
//
//        model.addAttribute("timeEntry", dto);
//
////        if (hasText(turboRequestId)) {
//////            return "reports/dialog::time-entry-edit-modal";
////            return "reports/user-report-edit-time-entry::turbo-modal";
////        } else {
//            return "reports/user-report-edit-time-entry";
////        }
//    }

    @PostMapping("/report/year/{year}/week/{week}/timeentry/{id}")
    public String postEditTimeEntry(
        @PathVariable("year") Integer year,
        @PathVariable("week") Integer week,
        @PathVariable("id") Long id,
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO, RedirectAttributes redirectAttributes,
        Model model, Locale locale) {

        // TODO TimeEntryController already handles everthing with mapping stuff and validation -> reuse it somehow

        final ZoneId zoneId = userSettingsProvider.zoneId();

        try {
            updateTimeEntry(timeEntryDTO, zoneId);
        } catch (TimeEntryUpdateNotPlausibleException e) {
            throw new RuntimeException(e);
        }

        redirectAttributes.addFlashAttribute("turboVisitControlReload", true);

//        final ModelAndView modelAndView = new ModelAndView("redirect:/report/year/%s/week/%s".formatted(year, week));
//        modelAndView.setStatus(SEE_OTHER);
//        return modelAndView;

        return "redirect:/report/year/%s/week/%s".formatted(year, week);
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

    private static Optional<YearWeek> yearWeek(int year, int week) {
        try {
            return Optional.of(YearWeek.of(Year.of(year), week));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearWeek with year={} week={}", year, week, exception);
            return Optional.empty();
        }
    }

    private void updateTimeEntry(TimeEntryDTO dto, ZoneId zoneId) throws TimeEntryUpdateNotPlausibleException {

        final Duration duration = toDuration(dto.getDuration());
        final ZonedDateTime start = dto.getStart() == null ? null : ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
        final ZonedDateTime end = getEndDate(dto, zoneId);

        timeEntryService.updateTimeEntry(new TimeEntryId(dto.getId()), dto.getComment(), start, end, duration, dto.isBreak());
    }

    private static Duration toDuration(String timeEntryDTODurationString) {
        if (hasText(timeEntryDTODurationString)) {
            final String[] split = timeEntryDTODurationString.split(":");
            return Duration.ofHours(Integer.parseInt(split[0])).plusMinutes(Integer.parseInt(split[1]));
        }
        return Duration.ZERO;
    }

    private static String toTimeEntryDTODurationString(Duration duration) {
        if (duration == null) {
            return "00:00";
        }
        return String.format("%02d:%02d", duration.toHours(), duration.toMinutes() % 60);
    }

    private ZonedDateTime getEndDate(TimeEntryDTO dto, ZoneId zoneId) {
        if (dto.getEnd() == null) {
            return null;
        } else if (dto.getStart() == null) {
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
        } else if (dto.getEnd().isBefore(dto.getStart())) {
            // end is on next day
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate().plusDays(1), dto.getEnd()), zoneId);
        } else {
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
        }
    }
}
