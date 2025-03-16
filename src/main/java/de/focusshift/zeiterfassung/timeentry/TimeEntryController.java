package de.focusshift.zeiterfassung.timeentry;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.timeentry.TimeEntryViewHelper.TIME_ENTRY_MODEL_NAME;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.time.Month.DECEMBER;
import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.Objects.requireNonNullElseGet;
import static org.springframework.util.StringUtils.hasText;

@Controller
class TimeEntryController implements HasTimeClock, HasLaunchpad {

    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final TimeEntryViewHelper viewHelper;
    private final Clock clock;

    public TimeEntryController(TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider,
                               DateFormatter dateFormatter, TimeEntryViewHelper viewHelper, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.viewHelper = viewHelper;
        this.clock = clock;
    }

    @GetMapping("/timeentries")
    public String items(@RequestParam(value = "year", required = false) Integer year,
                        @RequestParam(value = "week", required = false) Integer weekOfYear,
                        @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                        Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        final LocalDate now = LocalDate.now(clock);
        final Supplier<ZonedDateTime> userStartOfDay = () -> now.atStartOfDay(userSettingsProvider.zoneId());

        year = requireNonNullElseGet(year, () -> userStartOfDay.get().getYear());
        weekOfYear = requireNonNullElseGet(weekOfYear, () -> userStartOfDay.get().get(WEEK_OF_WEEK_BASED_YEAR));

        if (hasText(turboFrame)) {
            prepareTimeEntriesView(year, weekOfYear, model, currentUser, locale);
            model.addAttribute("turboStreamsEnabled", true);
            return "timeentries/index::#frame-time-entry-weeks";
        } else {
            return prepareTimeEntriesView(year, weekOfYear, model, currentUser, locale);
        }
    }

    private String prepareTimeEntriesView(int year, int weekOfYear, Model model, CurrentOidcUser currentUser, Locale locale) {
        viewHelper.addTimeEntryToModel(model, new TimeEntryDTO());
        addTimeEntriesToModel(year, weekOfYear, model, currentUser, locale);

        return "timeentries/index";
    }

    @PostMapping("/timeentries")
    public String save(
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        @CurrentUser CurrentOidcUser currentUser,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        final int year;
        final int weekOfYear;

        try {
            final LocalDate firstDateOfWeek = localDateToFirstDateOfWeek(timeEntryDTO.getDate());
            year = firstDateOfWeek.getYear();
            weekOfYear = firstDateOfWeek.get(WEEK_OF_WEEK_BASED_YEAR);
        } catch(NullPointerException exception) {
            throw new InvalidTimeEntryException("invalid time entry. date must be set.");
        }

        viewHelper.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

        if (bindingResult.hasErrors()) {
            addTimeEntriesToModel(year, weekOfYear, model, currentUser, locale);
            return "timeentries/index";
        }

        return "redirect:" + request.getHeader("referer");
    }

    @PostMapping(value = "/timeentries/{id}")
    public ModelAndView update(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @CurrentUser CurrentOidcUser currentUser,
        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        final int year;
        final int weekOfYear;

        try {
            final LocalDate firstDateOfWeek = localDateToFirstDateOfWeek(timeEntryDTO.getDate());
            year = firstDateOfWeek.getYear();
            weekOfYear = firstDateOfWeek.get(WEEK_OF_WEEK_BASED_YEAR);
        } catch(NullPointerException exception) {
            throw new InvalidTimeEntryException("invalid time entry. date must be set.");
        }

        viewHelper.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

        final String viewName;
        if (bindingResult.hasErrors()) {
            viewName = "timeentries/index";
        } else {
            viewName = "redirect:" + request.getHeader("referer");
        }

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();

        if (hasText(turboFrame)) {
            if (bindingResult.hasErrors()) {
                model.addAttribute("turboEditedTimeEntry", timeEntryDTO);
            } else {
                final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userLocalId, year, weekOfYear);
                final TimeEntryDay timeEntryDay = entryWeekPage.timeEntryWeek().days()
                    .stream()
                    .filter(day -> day.date().equals(timeEntryDTO.getDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("expected a day"));

                final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();
                final TimeEntry editedTimeEntry = timeEntryDay.timeEntries().stream()
                    .filter(entry -> entry.id().value().equals(timeEntryDTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("could not find edited timeEntry=%s".formatted(timeEntryDTO.getId())));

                model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
                model.addAttribute("turboEditedDay", toTimeEntryDayDto(timeEntryDay, locale));
                model.addAttribute("turboEditedTimeEntry", viewHelper.toTimeEntryDto(editedTimeEntry));
            }
            return new ModelAndView("timeentries/index::#frame-time-entry");
        } else {
            if (bindingResult.hasErrors()) {
                addTimeEntriesToModel(year, weekOfYear, model, currentUser, locale);
            }
            return new ModelAndView(viewName);
        }
    }

    @PostMapping(value = "/timeentries/{id}", params = "delete")
    public String delete(@PathVariable("id") Long id, Model model,
                         @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                         @CurrentUser CurrentOidcUser currentUser, Locale locale) {

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(new TimeEntryId(id))
            .orElseThrow(() -> new IllegalStateException("could not find time entry with id=%s".formatted(id)));

        timeEntryService.deleteTimeEntry(new TimeEntryId(id));

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (!hasText(turboFrame)) {
            return "redirect:/timeentries?year=%d&week=%d".formatted(year, weekOfYear);
        }

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userLocalId, year, weekOfYear);

        final Optional<TimeEntryDay> timeEntryDay = entryWeekPage.timeEntryWeek().days()
            .stream()
            .filter(day -> day.date().equals(timeEntry.start().toLocalDate()))
            .findFirst();

        final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();

        addTimeEntriesToModel(year, weekOfYear, model, currentUser, locale);

        model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
        model.addAttribute("turboEditedDay", timeEntryDay.map(entry -> toTimeEntryDayDto(entry, locale)).orElse(null));
        model.addAttribute("turboDeletedTimeEntry", viewHelper.toTimeEntryDto(timeEntry));

        return "timeentries/index::#" + turboFrame;
    }

    private void addTimeEntriesToModel(int year, int weekOfYear, Model model, CurrentOidcUser currentUser, Locale locale) {

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();

        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userLocalId, year, weekOfYear);
        final TimeEntryWeekDto timeEntryWeekDto = toTimeEntryWeekDto(entryWeekPage.timeEntryWeek(), locale);

        final int futureYear = lastWeekOfYear(year) == weekOfYear ? year + 1 : year;
        // using weekOfYear=1 instead of 0 since we need startOfWeek (monday). 0 could be .../friday/saturday/sunday
        final int futureWeekOfYear = futureYear == year ? weekOfYear + 1 : 1;

        final int pastYear = weekOfYear == 1 ? year - 1 : year;
        final int pastWeekOfYear = weekOfYear == 1 ? lastWeekOfYear(pastYear) : weekOfYear - 1;

        final TimeEntryWeeksPageDto paginationDto = new TimeEntryWeeksPageDto(
            futureYear,
            futureWeekOfYear,
            pastYear,
            pastWeekOfYear,
            timeEntryWeekDto,
            entryWeekPage.totalTimeEntries());

        model.addAttribute("timeEntryWeeksPage", paginationDto);
    }

    private TimeEntryWeekDto toTimeEntryWeekDto(TimeEntryWeek timeEntryWeek, Locale locale) {

        final LocalDate firstDateOfWeek = timeEntryWeek.firstDateOfWeek();
        final LocalDate lastDateOfWeek = timeEntryWeek.lastDateOfWeek();

        final MonthFormat firstMonthFormat =
            firstDateOfWeek.getMonthValue() == lastDateOfWeek.getMonthValue() ? MonthFormat.NONE : MonthFormat.STRING;

        final YearFormat firstYearFormat =
            firstDateOfWeek.getYear() == lastDateOfWeek.getYear() ? YearFormat.NONE : YearFormat.FULL;

        final MonthFormat lastMonthFormat = MonthFormat.STRING;
        final YearFormat lastYearFormat = YearFormat.FULL;

        final String firstDateString = dateFormatter.formatDate(firstDateOfWeek, firstMonthFormat, firstYearFormat);
        final String lastDateString = dateFormatter.formatDate(lastDateOfWeek, lastMonthFormat, lastYearFormat);

        final List<TimeEntryDayDto> daysDto = timeEntryWeek.days()
            .stream()
            .filter(timeEntryDay -> !timeEntryDay.timeEntries().isEmpty() || !timeEntryDay.absences().isEmpty())
            .map(entry -> toTimeEntryDayDto(entry, locale))
            .toList();

        final String weekHoursWorked = durationToTimeString(timeEntryWeek.workDuration().durationInMinutes());
        final String weekHoursWorkedShould = durationToTimeString(timeEntryWeek.shouldWorkingHours().durationInMinutes());
        final Duration weekOvertimeDuration = timeEntryWeek.overtime();
        final String weekOvertime = durationToTimeString(weekOvertimeDuration);
        final double weekRatio = timeEntryWeek.workedHoursRatio().multiply(BigDecimal.valueOf(100), new MathContext(2)).doubleValue();

        return new TimeEntryWeekDto(timeEntryWeek.calendarWeek(), firstDateString, lastDateString, weekHoursWorked,
            weekHoursWorkedShould, weekOvertime, weekOvertimeDuration.isNegative(), weekRatio, daysDto);
    }

    private static String durationToTimeString(Duration duration) {
        // negative duration is only the case for overtime.
        // negative overtime is handled in the template.
        // -> just use positive values to format duration string
        return String.format("%02d:%02d", Math.abs(duration.toHours()), Math.abs(duration.toMinutesPart()));
    }

    private TimeEntryDayDto toTimeEntryDayDto(TimeEntryDay timeEntryDay, Locale locale) {

        final String dateString = dateFormatter.formatDate(timeEntryDay.date(), MonthFormat.STRING, YearFormat.FULL);
        final String workedHours = durationToTimeString(timeEntryDay.workDuration().durationInMinutes());
        final String workedHoursShould = durationToTimeString(timeEntryDay.shouldWorkingHours().durationInMinutes());
        final Duration hoursDelta = timeEntryDay.overtime();
        final double ratio = timeEntryDay.workedHoursRatio().multiply(BigDecimal.valueOf(100), new MathContext(2)).doubleValue();
        final List<TimeEntryDTO> dayTimeEntryDtos = timeEntryDay.timeEntries().stream().map(viewHelper::toTimeEntryDto).toList();
        final List<AbsenceEntryDto> absenceEntryDtos = timeEntryDay.absences().stream()
            .map(absence -> new AbsenceEntryDto(timeEntryDay.date(), absence.label(locale), absence.color()))
            .toList();

        return TimeEntryDayDto.builder()
            .date(dateString)
            .dayOfWeek(timeEntryDay.date().getDayOfWeek())
            .hoursWorked(workedHours)
            .hoursWorkedShould(workedHoursShould)
            .hoursDelta(durationToTimeString(hoursDelta))
            .hoursDeltaNegative(hoursDelta.isNegative())
            .hoursWorkedRatio(ratio)
            .timeEntries(dayTimeEntryDtos)
            .absenceEntries(absenceEntryDtos)
            .build();
    }

    private int lastWeekOfYear(int year) {
        final LocalDate date = Year.of(year).atMonth(DECEMBER).atEndOfMonth();
        final int week = date.get(WEEK_OF_WEEK_BASED_YEAR);
        if (week == 1) {
            // last week cannot be the first one of a year :-)
            // e.g. 2024-12-31 is calWeek 1 (of next year) -> wee need calWeek 52 2024-12-23 to 2024-12-29
            return date.minusWeeks(1).get(WEEK_OF_WEEK_BASED_YEAR);
        } else {
            return week;
        }
    }

    private LocalDate localDateToFirstDateOfWeek(LocalDate localDate) {
        final DayOfWeek firstDayOfWeek = userSettingsProvider.firstDayOfWeek();
        return localDate.with(previousOrSame(firstDayOfWeek));
    }
}
