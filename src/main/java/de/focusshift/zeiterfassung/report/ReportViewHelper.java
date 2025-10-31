package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.DateRangeFormatter;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

@Component
class ReportViewHelper {

    private final DateFormatter dateFormatter;
    private final DateRangeFormatter dateRangeFormatter;

    ReportViewHelper(DateFormatter dateFormatter, DateRangeFormatter dateRangeFormatter) {
        this.dateFormatter = dateFormatter;
        this.dateRangeFormatter = dateRangeFormatter;
    }

    void addUserFilterModelAttributes(Model model, boolean allUsersSelected, List<User> users, List<UserLocalId> selectedUserLocalIds, String userReportFilterUrl) {

        final List<SelectableUserDto> selectableUserDtos = users
            .stream()
            .map(user -> userToSelectableUserDto(user, selectedUserLocalIds.contains(user.userLocalId())))
            .sorted(Comparator.comparing(SelectableUserDto::fullName))
            .toList();

        if (users.size() > 1) {
            model.addAttribute("users", selectableUserDtos);
            model.addAttribute("usersById", selectableUserDtos.stream().collect(toMap(SelectableUserDto::id, identity())));
            model.addAttribute("selectedUsers", selectableUserDtos.stream().filter(SelectableUserDto::selected).toList());
            model.addAttribute("selectedUserIds", selectedUserLocalIds.stream().map(UserLocalId::value).toList());
            model.addAttribute("allUsersSelected", allUsersSelected);
            model.addAttribute("userReportFilterUrl", userReportFilterUrl);
        }
    }

    void addSelectedUserDurationAggregationModelAttributes(Model model, boolean allUsersSelected, List<User> users, List<UserLocalId> selectedUserLocalIds, HasWorkDurationByUser report) {

        final List<User> usersToShowInTable = getSelectedUsers(allUsersSelected, users, selectedUserLocalIds)
            .stream()
            .sorted(Comparator.comparing(User::fullName))
            .toList();

        final Map<UserIdComposite, WorkDuration> workedByUser = report.workDurationByUser();
        final Map<UserIdComposite, ShouldWorkingHours> shouldByUser = report.shouldWorkingHoursByUser();
        final Map<UserIdComposite, OvertimeHours> overtimeByUser = report.overtimeByUser();

        final boolean showAggregatedInformation = report.overtimeByUser().size() > 1;

        if (showAggregatedInformation) {

            final List<ReportSelectedUserDurationAggregationDto> dtos = new ArrayList<>();

            for (User user : usersToShowInTable) {
                final UserIdComposite userIdComposite = user.userIdComposite();
                final OvertimeHours delta = overtimeByUser.get(userIdComposite);
                final ReportSelectedUserDurationAggregationDto dto = new ReportSelectedUserDurationAggregationDto(
                    userIdComposite.localId().value(),
                    user.fullName(),
                    durationToTimeString(delta.durationInMinutes()),
                    delta.isNegative(),
                    durationToTimeString(workedByUser.get(userIdComposite).durationInMinutes()),
                    durationToTimeString(shouldByUser.get(userIdComposite).durationInMinutes())
                );
                dtos.add(dto);
            }

            model.addAttribute("selectedUserDurationAggregation", dtos);
        }
    }

    private List<User> getSelectedUsers(boolean allUsersSelected, List<User> users, List<UserLocalId> selectedUserLocalIds) {
        if (allUsersSelected) {
            return users;
        } else {
            return users.stream().filter(user -> selectedUserLocalIds.contains(user.userLocalId())).toList();
        }
    }

    private static SelectableUserDto userToSelectableUserDto(User user, boolean selected) {
        return new SelectableUserDto(user.userLocalId().value(), user.fullName(), user.initials(), selected);
    }

    GraphWeekDto toGraphWeekDto(ReportWeek reportWeek, Month monthPivot) {

        final List<GraphDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toUserReportDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot)))
            .toList();

        final LocalDate firstDateOfWeek = reportWeek.firstDateOfWeek();
        final int calendarWeek = reportWeek.calenderWeek();

        final String dateRangeString = dateRangeFormatter.toDateRangeString(firstDateOfWeek, reportWeek.lastDateOfWeek());

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
        final double hoursWorkedShould = reportDay.shouldWorkingHours().hoursDoubleValue();

        return new GraphDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, hoursWorked, hoursWorkedShould);
    }

    DetailWeekDto toDetailWeekDto(ReportWeek reportWeek, Month monthPivot, Locale locale, TimeEntryIdLinkBuilder urlBuilder) {

        final List<DetailDayDto> dayReports = reportWeek.reportDays()
            .stream()
            .map(reportDay -> toDetailDayReportDto(reportDay, !reportDay.date().getMonth().equals(monthPivot), locale, urlBuilder))
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

    private DetailDayDto toDetailDayReportDto(ReportDay reportDay, boolean differentMonth, Locale locale, TimeEntryIdLinkBuilder urlBuilder) {

        final String dayOfWeekNarrow = dateFormatter.formatDayOfWeekNarrow(reportDay.date().getDayOfWeek());
        final String dayOfWeekFull = dateFormatter.formatDayOfWeekFull(reportDay.date().getDayOfWeek());
        final String dateString = dateFormatter.formatDate(reportDay.date());

        final List<DetailDayEntryDto> dayEntryDtos = reportDay.reportDayEntries().stream()
            .map(dayEntry -> toDetailDayEntryDto(dayEntry, urlBuilder))
            .toList();

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

        return new DetailDayDto(differentMonth, dayOfWeekNarrow, dayOfWeekFull, dateString, reportDay.locked(), workedWorkingHoursString,
            shouldWorkingHoursString, deltaHours, deltaDuration.isNegative(), dayEntryDtos, detailDayAbsenceDto);
    }

    private DetailDayEntryDto toDetailDayEntryDto(ReportDayEntry reportDayEntry, TimeEntryIdLinkBuilder urlBuilder) {

        final String dialogUrl = urlBuilder.getTimeEntryIdUrl(reportDayEntry.timeEntryId());
        final User user = reportDayEntry.user();

        return new DetailDayEntryDto(reportDayEntry.timeEntryId().value(), user.fullName(), user.initials(), user.userLocalId().value(),
            reportDayEntry.comment(), reportDayEntry.isBreak(), reportDayEntry.start().toLocalTime(),
            reportDayEntry.end().toLocalTime(), dialogUrl);
    }

    private DetailDayAbsenceDto toDetailDayAbsenceDto(ReportDayAbsence reportDayAbsence, Locale locale) {

        final User user = reportDayAbsence.user();
        final Absence absence = reportDayAbsence.absence();

        return new DetailDayAbsenceDto(user.fullName(), user.initials(), user.userLocalId().value(), absence.dayLength().name(),
            absence.label(locale), absence.color().name());
    }
}
