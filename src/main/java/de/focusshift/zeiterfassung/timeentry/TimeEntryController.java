package de.focusshift.zeiterfassung.timeentry;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.Month.DECEMBER;
import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;

@Controller
class TimeEntryController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final Clock clock;

    public TimeEntryController(TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider,
                               DateFormatter dateFormatter, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.clock = clock;
    }

    @GetMapping("/timeentries")
    public String items(Model model, Locale locale, @AuthenticationPrincipal OidcUser principal) {

        final LocalDate now = LocalDate.now(clock);
        final ZonedDateTime userStartOfDay = now.atStartOfDay(userSettingsProvider.zoneId());

        final int year = userStartOfDay.getYear();
        final int weekOfYear = userStartOfDay.get(WEEK_OF_WEEK_BASED_YEAR);

        return prepareTimeEntriesView(year, weekOfYear, model, principal, locale);
    }

    @GetMapping("/timeentries/{year}/{weekOfYear}")
    public String timeEntriesForYearAndWeekOfYear(@PathVariable("year") int year,
                                                  @PathVariable("weekOfYear") int weekOfYear,
                                                  @AuthenticationPrincipal OidcUser principal,
                                                  @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                                  Model model, Locale locale) {

        if (hasText(turboFrame)) {
            prepareTimeEntriesView(year, weekOfYear, model, principal, locale);
            model.addAttribute("turboStreamsEnabled", true);
            return "timeentries/index::#frame-time-entry-weeks";
        } else {
            return prepareTimeEntriesView(year, weekOfYear, model, principal, locale);
        }
    }

    private String prepareTimeEntriesView(int year, int weekOfYear, Model model, OidcUser principal, Locale locale) {
        addTimeEntryToModel(model, new TimeEntryDTO());
        addTimeEntriesToModel(year, weekOfYear, model, principal, locale);

        return "timeentries/index";
    }

    @PostMapping("/timeentries")
    public String save(
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        Model model, Locale locale,
        @AuthenticationPrincipal OidcUser principal,
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

        final String viewName = saveOrUpdate(timeEntryDTO, bindingResult, model, principal, request);

        if (bindingResult.hasErrors()) {
            addTimeEntriesToModel(year, weekOfYear, model, principal, locale);
        }

        return viewName;
    }

    @PostMapping(value = "/timeentries/{id}")
    public ModelAndView update(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @AuthenticationPrincipal OidcUser principal,
        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
        Model model, Locale locale,
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

        final String viewName = saveOrUpdate(timeEntryDTO, bindingResult, model, principal, request);

        final UserId userId = new UserId(principal.getUserInfo().getSubject());

        if (hasText(turboFrame)) {
            if (bindingResult.hasErrors()) {
                model.addAttribute("turboEditedTimeEntry", timeEntryDTO);
            } else {
                final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userId, year, weekOfYear);
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
                model.addAttribute("turboEditedTimeEntry", toTimeEntryDto(editedTimeEntry));
            }
            return new ModelAndView("timeentries/index::#frame-time-entry");
        } else {
            if (bindingResult.hasErrors()) {
                addTimeEntriesToModel(year, weekOfYear, model, principal, locale);
            }
            return new ModelAndView(viewName);
        }
    }

    @PostMapping(value = "/timeentries/{id}", params = "delete")
    public String delete(@PathVariable("id") Long id, Model model,
                         @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                         @AuthenticationPrincipal OidcUser principal, Locale locale) {

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(id)
            .orElseThrow(() -> new IllegalStateException("could not find time entry with id=%s".formatted(id)));

        timeEntryService.deleteTimeEntry(id);

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (!hasText(turboFrame)) {
            return "redirect:/timeentries/%s/%s".formatted(year, weekOfYear);
        }

        final UserId userId = new UserId(principal.getUserInfo().getSubject());
        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userId, year, weekOfYear);

        final Optional<TimeEntryDay> timeEntryDay = entryWeekPage.timeEntryWeek().days()
            .stream()
            .filter(day -> day.date().equals(timeEntry.start().toLocalDate()))
            .findFirst();

        final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();

        addTimeEntriesToModel(year, weekOfYear, model, principal, locale);

        model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
        model.addAttribute("turboEditedDay", timeEntryDay.map(entry -> toTimeEntryDayDto(entry, locale)).orElse(null));
        model.addAttribute("turboDeletedTimeEntry", toTimeEntryDto(timeEntry));

        return "timeentries/index::#" + turboFrame;
    }

    private String saveOrUpdate(TimeEntryDTO dto, BindingResult bindingResult, Model model, OidcUser principal, HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("timeEntryErrorId", dto.getId());

            final boolean hasErrorStart = bindingResult.hasFieldErrors("start");
            final boolean hasErrorEnd = bindingResult.hasFieldErrors("end");
            final boolean hasErrorDuration = bindingResult.hasFieldErrors("value");

            if (hasErrorStart && hasErrorEnd && !hasErrorDuration) {
                bindingResult.reject("time-entry.validation.startOrEnd.required");
            } else if (hasErrorStart && !hasErrorEnd && hasErrorDuration) {
                bindingResult.reject("time-entry.validation.startOrDuration.required");
            } else if (!hasErrorStart && hasErrorEnd && hasErrorDuration) {
                bindingResult.reject("time-entry.validation.endOrDuration.required");
            }

            return "timeentries/index";
        }

        final UserId userId = new UserId(principal.getUserInfo().getSubject());
        final ZoneId zoneId = userSettingsProvider.zoneId();

        if (dto.getId() == null) {
            createTimeEntry(dto, userId, zoneId);
        } else {
            try {
                updateTimeEntry(dto, zoneId);
            } catch (TimeEntryUpdateNotPlausibleException e) {
                LOG.debug("could not update time-entry", e);

                bindingResult.reject("time-entry.validation.plausible");
                bindingResult.rejectValue("start", "");
                bindingResult.rejectValue("end", "");
                bindingResult.rejectValue("duration", "");

                model.addAttribute("timeEntryErrorId", dto.getId());

                return "timeentries/index";
            }
        }

        final String referer = request.getHeader("referer");
        return "redirect:" + referer;
    }

    private void createTimeEntry(TimeEntryDTO dto, UserId userId, ZoneId zoneId) {

        final ZonedDateTime start;
        final ZonedDateTime end;

        final Duration duration = toDuration(dto.getDuration());

        if (duration.equals(Duration.ZERO)) {
            // start and end should be given
            start = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
            end = getEndDate(dto, zoneId);
        } else if (dto.getStart() == null) {
            // end and value should be given
            end = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
            start = end.minusMinutes(duration.toMinutes());
        } else {
            // start and value should be given
            start = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
            end = start.plusMinutes(duration.toMinutes());
        }

        timeEntryService.createTimeEntry(userId, dto.getComment(), start, end, dto.isBreak());
    }

    private void updateTimeEntry(TimeEntryDTO dto, ZoneId zoneId) throws TimeEntryUpdateNotPlausibleException {

        final Duration duration = toDuration(dto.getDuration());
        final ZonedDateTime start = dto.getStart() == null ? null : ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
        final ZonedDateTime end = getEndDate(dto, zoneId);

        timeEntryService.updateTimeEntry(new TimeEntryId(dto.getId()), dto.getComment(), start, end, duration, dto.isBreak());
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

    private void addTimeEntryToModel(Model model, TimeEntryDTO timeEntryDTO) {
        model.addAttribute("timeEntry", timeEntryDTO);
    }

    private void addTimeEntriesToModel(int year, int weekOfYear, Model model, OidcUser principal, Locale locale) {

        final UserId userId = new UserId(principal.getUserInfo().getSubject());

        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userId, year, weekOfYear);
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
        final List<TimeEntryDTO> dayTimeEntryDtos = timeEntryDay.timeEntries().stream().map(this::toTimeEntryDto).toList();
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

    private TimeEntryDTO toTimeEntryDto(TimeEntry timeEntry) {

        final ZonedDateTime start = timeEntry.start();
        final ZonedDateTime end = timeEntry.end();

        final LocalDate date = start.toLocalDate();
        final LocalTime startTime = start.toLocalTime();
        final LocalTime endTime = end.toLocalTime();

        final Duration duration = timeEntry.durationInMinutes();
        final String durationString = toTimeEntryDTODurationString(duration);

        return TimeEntryDTO.builder()
            .id(timeEntry.id().value())
            .date(date)
            .start(startTime)
            .end(endTime)
            .duration(durationString)
            .comment(timeEntry.comment())
            .isBreak(timeEntry.isBreak())
            .build();
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
