package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.overtime.OvertimeDuration;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.apache.commons.collections4.SetUtils;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Component
class ReportControllerHelper {

    private final ReportPermissionService reportPermissionService;
    private final DateFormatter dateFormatter;

    ReportControllerHelper(ReportPermissionService reportPermissionService, DateFormatter dateFormatter) {
        this.reportPermissionService = reportPermissionService;
        this.dateFormatter = dateFormatter;
    }

    UserId principalToUserId(OidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }

    void addUserFilterModelAttributes(Model model, boolean allUsersSelected, List<UserLocalId> selectedUserLocalIds, String userReportFilterUrl) {

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

    GraphWeekDto toGraphWeekDto(ReportWeek reportWeek, Month monthPivot) {

        final List<GraphDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toUserReportDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot)))
            .toList();

        final String yearMonthWeek = dateFormatter.formatYearMonthWeek(reportWeek.firstDateOfWeek());

        final double maxHoursWorked = dayReports.stream()
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final double hoursWorkedAverageADay = reportWeek.averageDayWorkDuration().hoursDoubleValue();

        return new GraphWeekDto(yearMonthWeek, dayReports, maxHoursWorked, hoursWorkedAverageADay);
    }

    private GraphDayDto toUserReportDayReportDto(ReportDay reportDay, boolean differentMonth) {

        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());
        final double hoursWorked = reportDay.workDuration().hoursDoubleValue();
        final double hoursWorkedShould = reportDay.plannedWorkingHours().hoursDoubleValue();

        return new GraphDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, hoursWorked, hoursWorkedShould);
    }

    DetailWeekDto toDetailWeekDto(ReportWeek reportWeek, Month monthPivot) {

        final List<DetailDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toDetailDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot)))
            .toList();

        final LocalDate first = reportWeek.firstDateOfWeek();
        final LocalDate last = reportWeek.lastDateOfWeek();
        final int calendarWeek = first.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        final ZonedDateTime firstOfWeek = ZonedDateTime.of(first, LocalTime.MIN, ZoneId.systemDefault());
        final ZonedDateTime lastOfWeek = ZonedDateTime.of(last, LocalTime.MIN, ZoneId.systemDefault());

        return new DetailWeekDto(Date.from(firstOfWeek.toInstant()), Date.from(lastOfWeek.toInstant()), calendarWeek, dayReports);
    }

    ReportOvertimesDto reportOvertimesDto(ReportWeek reportWeek) {
        // person | M | T | W | T | F | S | S |
        // -----------------------------------
        // john   | 1 | 2 | 2 | 3 | 4 | 4 | 4 |   <- `ReportOvertimeDto ( personName, overtimes )`
        // jane   | 0 | 0 | 2 | 3 | 4 | 4 | 4 |   entries in the middle of the week
        // jack   | 0 | 0 | 0 | 0 | 0 | 0 | 0 |   no entries this week
        //
        // note that the first overtime won't be empty actually, but the `accumulatedOvertimeToDate`.

        // build up `users` peace by peace. one person could have the first working day in the middle of the week (jane).
        final Set<User> users = new HashSet<>();

        // {john} -> [1, 2, 2, 3, 4, 4, 4]
        // {jane} -> [empty, empty, 2, 3, 4, 4, 4]
        // {jack} -> [empty, empty, empty, empty, empty, empty, empty] (has no entries this week)
        final Map<User, List<Optional<OvertimeDuration>>> overtimeDurationsByUser = new HashMap<>();

        // used to initiate the persons list of overtimes.
        // jane will be seen first on the third reportDay. she initially needs a list of `[null, null]`.
        int nrOfHandledDays = 0;

        for (ReportDay reportDay : reportWeek.reportDays()) {

            // planned working hours contains all users. even users without time entries at this day
            final Map<User, PlannedWorkingHours> plannedByUser = reportDay.plannedWorkingHoursByUser();
            users.addAll(plannedByUser.keySet());

            for (User user : users) {
                final var durations = overtimeDurationsByUser.computeIfAbsent(user, prepareOvertimeDurationList(nrOfHandledDays));
                durations.add(reportDay.accumulatedOvertimeToDateEndOfBusinessByUser(user.localId()));
            }

            nrOfHandledDays++;
        }

        final Set<UserLocalId> userIdsWithDayEntries = users.stream().map(User::localId).collect(toSet());
        final Map<User, List<PlannedWorkingHours>> usersWithPlannedWorkingHours = reportWeek.plannedWorkingHoursByUser();
        final Map<UserLocalId, User> usersWithPlannedWorkingHoursById = usersWithPlannedWorkingHours.keySet().stream().collect(toMap(User::localId, identity()));
        final Set<UserLocalId> userIdsWithPlannedWorkingHours = usersWithPlannedWorkingHours.keySet().stream().map(User::localId).collect(toSet());
        final SetUtils.SetView<UserLocalId> userIdsWithoutDayEntries = SetUtils.difference(userIdsWithPlannedWorkingHours, userIdsWithDayEntries);
        for (UserLocalId userLocalId : userIdsWithoutDayEntries) {
            overtimeDurationsByUser.computeIfAbsent(usersWithPlannedWorkingHoursById.get(userLocalId), prepareOvertimeDurationList(nrOfHandledDays));
        }

        final List<ReportOvertimeDto> overtimeDtos = overtimeDurationsByUser.entrySet().stream()
            .map(entry -> new ReportOvertimeDto(entry.getKey().fullName(), overtimeDurationToDouble(entry.getValue())))
            .sorted(comparing(ReportOvertimeDto::personName))
            .collect(toList());

        return new ReportOvertimesDto(reportWeek.dateOfWeeks(), overtimeDtos);
    }

    private static Function<User, List<Optional<OvertimeDuration>>> prepareOvertimeDurationList(int nrOfHandledDays) {
        return (unused) -> {
            final List<Optional<OvertimeDuration>> objects = new ArrayList<>();
            for (int i = 0; i < nrOfHandledDays; i++) {
                objects.add(Optional.empty());
            }
            return objects;
        };
    }

    private static List<Double> overtimeDurationToDouble(List<Optional<OvertimeDuration>> overtimeDurations) {
        return overtimeDurations.stream()
            .map(maybe -> maybe.orElse(null))
            .map(overtimeDuration -> overtimeDuration == null ? null : overtimeDuration.hoursDoubleValue())
            .collect(toList());
    }

    String createUrl(String prefix, boolean allUsersSelected, List<UserLocalId> selectedUserLocalIds) {
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

    private DetailDayDto toDetailDayReportDto(ReportDay reportDay, boolean differentMonth) {

        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());
        final double hoursWorked = reportDay.workDuration().hoursDoubleValue();
        final List<DetailDayEntryDto> dayEntryDtos = reportDay.reportDayEntries().stream().map(this::toDetailDayEntryDto).toList();

        return new DetailDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, hoursWorked, dayEntryDtos);
    }

    private DetailDayEntryDto toDetailDayEntryDto(ReportDayEntry reportDayEntry) {
        return new DetailDayEntryDto(reportDayEntry.user().fullName(), reportDayEntry.comment(), reportDayEntry.start().toLocalTime(), reportDayEntry.end().toLocalTime());
    }
}
