package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.DateRangeFormatter;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.joining;

@Component
class ReportControllerHelper {

    private final ReportPermissionService reportPermissionService;
    private final DateFormatter dateFormatter;
    private final DateRangeFormatter dateRangeFormatter;

    ReportControllerHelper(ReportPermissionService reportPermissionService, DateFormatter dateFormatter, DateRangeFormatter dateRangeFormatter) {
        this.reportPermissionService = reportPermissionService;
        this.dateFormatter = dateFormatter;
        this.dateRangeFormatter = dateRangeFormatter;
    }

    UserId principalToUserId(OidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }

    void addUserFilterModelAttributes(Model model, boolean allUsersSelected, List<UserLocalId> selectedUserLocalIds, String userReportFilterUrl) {

        final List<User> permittedUsers = reportPermissionService.findAllPermittedUsersForCurrentUser();
        if (permittedUsers.size() > 1) {
            final List<SelectableUserDto> selectableUserDtos = permittedUsers
                .stream()
                .map(user -> userToSelectableUserDto(user, selectedUserLocalIds.contains(user.userLocalId())))
                .toList();

            model.addAttribute("users", selectableUserDtos);
            model.addAttribute("selectedUserIds", selectedUserLocalIds.stream().map(UserLocalId::value).toList());
            model.addAttribute("allUsersSelected", allUsersSelected);
            model.addAttribute("userReportFilterUrl", userReportFilterUrl);
        }
    }

    private static SelectableUserDto userToSelectableUserDto(User user, boolean selected) {
        return new SelectableUserDto(user.userLocalId().value(), user.givenName() + " " + user.familyName(), selected);
    }

    GraphWeekDto toGraphWeekDto(ReportWeek reportWeek, Month monthPivot) {

        final List<GraphDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toUserReportDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot)))
            .toList();

        final int calendarWeek = reportWeek.firstDateOfWeek().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        final String dateRangeString = dateRangeFormatter.toDateRangeString(reportWeek.firstDateOfWeek(), reportWeek.lastDateOfWeek());

        final double maxHoursWorked = dayReports.stream()
            .map(GraphDayDto::hoursWorked)
            .mapToDouble(value -> value)
            .max().orElse(0.0);

        final WorkDuration workDuration = reportWeek.workDuration();
        final ShouldWorkingHours shouldWorkingHours = reportWeek.shouldWorkingHours();
        final String shouldWorkingHoursString = durationToTimeString(shouldWorkingHours.duration());
        final String workedWorkingHoursString = durationToTimeString(workDuration.duration());

        final Duration deltaDuration = workDuration.duration().minus(shouldWorkingHours.duration());
        final String deltaHours = durationToTimeString(deltaDuration);

        final double weekRatio = reportWeek.workedHoursRatio().multiply(BigDecimal.valueOf(100), new MathContext(2)).doubleValue();

        return new GraphWeekDto(calendarWeek, dateRangeString, dayReports, maxHoursWorked, workedWorkingHoursString, shouldWorkingHoursString, deltaHours, deltaDuration.isNegative(), weekRatio);
    }

    private static String durationToTimeString(Duration duration) {
        // use positive values to format duration string
        // negative value is handled in template
        return String.format("%02d:%02d", Math.abs(duration.toHours()), Math.abs(duration.toMinutesPart()));
    }

    private GraphDayDto toUserReportDayReportDto(ReportDay reportDay, boolean differentMonth) {

        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());
        final double hoursWorked = reportDay.workDuration().hoursDoubleValue();
        final double hoursWorkedShould = reportDay.plannedWorkingHours().hoursDoubleValue();

        return new GraphDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, hoursWorked, hoursWorkedShould);
    }

    DetailWeekDto toDetailWeekDto(ReportWeek reportWeek, Month monthPivot, Locale locale) {

        final List<DetailDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toDetailDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot), locale))
            .toList();

        final LocalDate first = reportWeek.firstDateOfWeek();
        final LocalDate last = reportWeek.lastDateOfWeek();
        final int calendarWeek = first.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        final ZonedDateTime firstOfWeek = ZonedDateTime.of(first, LocalTime.MIN, ZoneId.systemDefault());
        final ZonedDateTime lastOfWeek = ZonedDateTime.of(last, LocalTime.MIN, ZoneId.systemDefault());

        return new DetailWeekDto(Date.from(firstOfWeek.toInstant()), Date.from(lastOfWeek.toInstant()), calendarWeek, dayReports);
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

    private DetailDayDto toDetailDayReportDto(ReportDay reportDay, boolean differentMonth, Locale locale) {

        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());
        final List<DetailDayEntryDto> dayEntryDtos = reportDay.reportDayEntries().stream().map(this::toDetailDayEntryDto).toList();

        final List<DetailDayAbsenceDto> detailDayAbsenceDto = reportDay.detailDayAbsencesByUser().values().stream()
            .flatMap(Collection::stream)
            .map(reportDayAbsence -> toDetailDayAbsenceDto(reportDayAbsence, locale))
            .toList();

        final WorkDuration workDuration = reportDay.workDuration();
        final ShouldWorkingHours shouldWorkingHours = reportDay.shouldWorkingHours();
        final String shouldWorkingHoursString = durationToTimeString(shouldWorkingHours.duration());
        final String workedWorkingHoursString = durationToTimeString(workDuration.duration());

        final Duration deltaDuration = workDuration.duration().minus(shouldWorkingHours.duration());
        final String deltaHours = durationToTimeString(deltaDuration);

        return new DetailDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, workedWorkingHoursString, shouldWorkingHoursString, deltaHours, deltaDuration.isNegative(), dayEntryDtos, detailDayAbsenceDto);
    }

    private DetailDayEntryDto toDetailDayEntryDto(ReportDayEntry reportDayEntry) {
        return new DetailDayEntryDto(reportDayEntry.user().fullName(), reportDayEntry.comment(), reportDayEntry.isBreak(), reportDayEntry.start().toLocalTime(), reportDayEntry.end().toLocalTime());
    }

    private DetailDayAbsenceDto toDetailDayAbsenceDto(ReportDayAbsence reportDayAbsence, Locale locale) {
        final User user = reportDayAbsence.user();
        final Absence absence = reportDayAbsence.absence();
        return new DetailDayAbsenceDto(user.fullName(), absence.dayLength().name(), absence.label(locale), absence.color().name());
    }
}
