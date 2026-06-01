package de.focusshift.zeiterfassung.report;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.report.BreakdownService.ActivityTypeBreakdown;
import de.focusshift.zeiterfassung.report.BreakdownService.BreakdownResult;
import de.focusshift.zeiterfassung.report.BreakdownService.CustomerBreakdown;
import de.focusshift.zeiterfassung.report.BreakdownService.ProjectBreakdown;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;

@Controller
@RequestMapping("/report/breakdown")
class BreakdownController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    record ProjectBreakdownDto(String projectName, String hours, int percent) {}

    record CustomerBreakdownDto(String customerName, String hours, int percent, List<ProjectBreakdownDto> projects) {}

    record ActivityTypeBreakdownDto(String name, String hours, int percent) {}

    record BreakdownDto(
        List<CustomerBreakdownDto> byCustomer,
        List<ActivityTypeBreakdownDto> byActivityType,
        String totalHours,
        boolean hasData
    ) {}

    private final BreakdownService breakdownService;
    private final ReportPermissionService reportPermissionService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final Clock clock;

    BreakdownController(BreakdownService breakdownService,
                        ReportPermissionService reportPermissionService,
                        UserSearchViewHelper userSearchViewHelper,
                        Clock clock) {
        this.breakdownService = breakdownService;
        this.reportPermissionService = reportPermissionService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.clock = clock;
    }

    @GetMapping
    ModelAndView breakdown(
        @RequestParam(required = false, defaultValue = "month") String preset,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(value = "user", required = false) List<Long> userLocalIdValues,
        Model model, @CurrentUser CurrentOidcUser currentUser
    ) {
        final LocalDate today = LocalDate.now(clock);

        final LocalDate rangeFrom;
        final LocalDate rangeToInclusive;

        switch (preset) {
            case "week" -> {
                rangeFrom = today.with(DayOfWeek.MONDAY);
                rangeToInclusive = rangeFrom.plusDays(6);
            }
            case "custom" -> {
                rangeFrom = from != null ? from : today.withDayOfMonth(1);
                rangeToInclusive = to != null ? to : today;
            }
            default -> {
                rangeFrom = today.withDayOfMonth(1);
                rangeToInclusive = today.withDayOfMonth(today.lengthOfMonth());
            }
        }

        // Resolve permitted users
        final List<UserLocalId> allPermittedIds = reportPermissionService.findAllPermittedUserLocalIdsForCurrentUser();
        final List<UserLocalId> selectedIds;
        if (userLocalIdValues == null || userLocalIdValues.isEmpty()) {
            selectedIds = allPermittedIds;
        } else {
            selectedIds = userLocalIdValues.stream()
                .map(UserLocalId::new)
                .filter(allPermittedIds::contains)
                .toList();
        }

        final BreakdownResult result = breakdownService.breakdown(rangeFrom, rangeToInclusive.plusDays(1), selectedIds);

        model.addAttribute("breakdown", toDto(result));
        model.addAttribute("from", rangeFrom);
        model.addAttribute("to", rangeToInclusive);
        model.addAttribute("preset", preset);
        model.addAttribute("selectedUserIds", userLocalIdValues == null ? List.of() : userLocalIdValues);
        model.addAttribute("canViewAllUsers", reportPermissionService.currentUserHasPermissionForAllUsers());

        // Tab state
        model.addAttribute("weekAriaCurrent", "false");
        model.addAttribute("monthAriaCurrent", "false");
        model.addAttribute("breakdownAriaCurrent", "location");

        // Override the chart+entries section with breakdown content
        model.addAttribute("chartNavigationFragment", "reports/breakdown::empty");
        model.addAttribute("chartFragment", "reports/breakdown::empty");
        model.addAttribute("entriesFragment", "reports/breakdown::empty");
        model.addAttribute("overrideContentFragment", "reports/breakdown::content");

        return new ModelAndView("reports/user-report");
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query,
                                    @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> "/report/breakdown?user=%s".formatted(suggestion.userLocalId().value())
        );
    }

    private BreakdownDto toDto(BreakdownResult result) {
        final Duration total = result.total();
        final String totalHours = formatDuration(total);

        final List<CustomerBreakdownDto> customers = result.byCustomer().stream()
            .map(c -> {
                final List<ProjectBreakdownDto> projects = c.projects().stream()
                    .map(p -> new ProjectBreakdownDto(p.projectName(), formatDuration(p.duration()), percent(p.duration(), total)))
                    .toList();
                return new CustomerBreakdownDto(c.customerName(), formatDuration(c.duration()), percent(c.duration(), total), projects);
            })
            .toList();

        final List<ActivityTypeBreakdownDto> activities = result.byActivityType().stream()
            .map(a -> new ActivityTypeBreakdownDto(a.name(), formatDuration(a.duration()), percent(a.duration(), total)))
            .toList();

        return new BreakdownDto(customers, activities, totalHours, !result.isEmpty());
    }

    private static int percent(Duration part, Duration total) {
        if (total.isZero()) return 0;
        return (int) Math.round(part.toMinutes() * 100.0 / total.toMinutes());
    }

    private static String formatDuration(Duration duration) {
        return "%02d:%02d".formatted(Math.abs(duration.toHours()), Math.abs(duration.toMinutesPart()));
    }
}
