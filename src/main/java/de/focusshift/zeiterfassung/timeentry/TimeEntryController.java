package de.focusshift.zeiterfassung.timeentry;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.timeentry.ImpressionUserMapper.toImpressionUserDto;
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
    private final UserManagementService userManagementService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final TimeEntryViewHelper viewHelper;
    private final Clock clock;

    public TimeEntryController(TimeEntryService timeEntryService, UserManagementService userManagementService,
                               UserSettingsProvider userSettingsProvider, DateFormatter dateFormatter,
                               TimeEntryViewHelper viewHelper, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userManagementService = userManagementService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.viewHelper = viewHelper;
        this.clock = clock;
    }

    @GetMapping("/timeentries")
    public String timeEntries(@RequestParam(value = "year", required = false) Integer year,
                              @RequestParam(value = "week", required = false) Integer weekOfYear,
                              @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                              Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {
        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        return timeEntriesView(userLocalId, year, weekOfYear, model, locale);
    }

    @GetMapping("/person/{userId}/timeentries")
    public String personTimeEntries(@PathVariable("userId") Long userLocalIdValue,
                                    @RequestParam(value = "year", required = false) Integer year,
                                    @RequestParam(value = "week", required = false) Integer weekOfYear,
                                    @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                    Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId ownerUserLocalId = new UserLocalId(userLocalIdValue);

        assertTimeEntryAccess(userLocalIdValue, currentUser);
        prepareUserImpression(model, ownerUserLocalId);

        return timeEntriesView(ownerUserLocalId, year, weekOfYear, model, locale);
    }

    private String timeEntriesView(UserLocalId timeEntryUserLocalId, Integer year, Integer weekOfYear, Model model, Locale locale) {


        final LocalDate now = LocalDate.now(clock);
        final Supplier<ZonedDateTime> userStartOfDay = () -> now.atStartOfDay(userSettingsProvider.zoneId());

        year = requireNonNullElseGet(year, () -> userStartOfDay.get().getYear());
        weekOfYear = requireNonNullElseGet(weekOfYear, () -> userStartOfDay.get().get(WEEK_OF_WEEK_BASED_YEAR));

        return prepareTimeEntriesForYearAndWeekOfYear(year, weekOfYear, model, timeEntryUserLocalId, locale);
    }

    @GetMapping("/timeentries/{year}/{weekOfYear}")
    public String timeEntriesForYearAndWeekOfYear(@PathVariable("year") int year,
                                                  @PathVariable("weekOfYear") int weekOfYear,
                                                  @CurrentUser CurrentOidcUser currentUser,
                                                  @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                                  Model model, Locale locale) {

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        return prepareTimeEntriesForYearAndWeekOfYear(year, weekOfYear, userLocalId, model, locale, turboFrame);
    }

    @GetMapping("/person/{userId}/timeentries/{year}/{weekOfYear}")
    public String personTimeEntriesForYearAndWeekOfYear(@PathVariable("userId") Long userLocalIdValue,
                                                        @PathVariable("year") int year,
                                                        @PathVariable("weekOfYear") int weekOfYear,
                                                        @CurrentUser CurrentOidcUser currentUser,
                                                        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                                        Model model, Locale locale) {


        final UserLocalId userImpressionLocalId = new UserLocalId(userLocalIdValue);

        assertTimeEntryAccess(userLocalIdValue, currentUser);
        prepareUserImpression(model, userImpressionLocalId);

        return prepareTimeEntriesForYearAndWeekOfYear(year, weekOfYear, userImpressionLocalId, model, locale, turboFrame);
    }

    private String prepareTimeEntriesForYearAndWeekOfYear(int year, int weekOfYear, UserLocalId userLocalId, Model model, Locale locale, String turboFrame) {
        if (hasText(turboFrame)) {
            prepareTimeEntriesForYearAndWeekOfYear(year, weekOfYear, model, userLocalId, locale);
            model.addAttribute("turboStreamsEnabled", true);
            return "timeentries/index::#frame-time-entry-weeks";
        } else {
            return prepareTimeEntriesForYearAndWeekOfYear(year, weekOfYear, model, userLocalId, locale);
        }
    }

    private String prepareTimeEntriesForYearAndWeekOfYear(int year, int weekOfYear, Model model, UserLocalId userLocalId, Locale locale) {
        viewHelper.addTimeEntryToModel(model, new TimeEntryDTO());
        addTimeEntriesToModel(year, weekOfYear, model, userLocalId, locale);
        return "timeentries/index";
    }

    @PostMapping("/timeentries")
    public String createTimeEntry(
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        @CurrentUser CurrentOidcUser currentUser,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        prepareTimeEntriesForYearAndWeekOfYear(currentUser, timeEntryDTO, bindingResult, model, locale, redirectAttributes);

        if (bindingResult.hasErrors()) {
            return "timeentries/index";
        } else {
            return "redirect:" + request.getHeader("referer");
        }
    }

    @PostMapping("/person/{userId}/timeentries")
    public String personCreateTimeEntry(@PathVariable("userId") Long userLocalIdValue,
                                        @Valid @ModelAttribute(TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO, BindingResult bindingResult,
                                        @CurrentUser CurrentOidcUser currentUser,
                                        RedirectAttributes redirectAttributes,
                                        Model model, Locale locale) {

        assertTimeEntryAccess(userLocalIdValue, currentUser);
        prepareTimeEntriesForYearAndWeekOfYear(currentUser, timeEntryDTO, bindingResult, model, locale, redirectAttributes);

        if (bindingResult.hasErrors()) {
            return "timeentries/index";
        } else {
            return "redirect:/timeentries/person/%s".formatted(userLocalIdValue);
        }
    }

    private void prepareTimeEntriesForYearAndWeekOfYear(CurrentOidcUser currentUser, TimeEntryDTO timeEntryDTO, BindingResult bindingResult,
                                                        Model model, Locale locale, RedirectAttributes redirectAttributes) {

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
            final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
            addTimeEntriesToModel(year, weekOfYear, model, userLocalId, locale);
        }
    }

    @PostMapping(value = "/timeentries/{id}")
    public ModelAndView updateTimeEntry(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @CurrentUser CurrentOidcUser currentUser,
        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();

        return updateTimeEntry(userLocalId, timeEntryDTO, bindingResult, model, locale, redirectAttributes,
            turboFrame, request);
    }

    @PostMapping(value = "/person/{userId}/timeentries/{id}")
    public ModelAndView personUpdateTimeEntry(
        @PathVariable("id") Long id,
        @PathVariable("userId") Long userLocalIdValue,
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @CurrentUser CurrentOidcUser currentUser,
        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        assertTimeEntryAccess(userLocalIdValue, currentUser);

        final UserLocalId userImpressionLocalId = new UserLocalId(userLocalIdValue);

        return updateTimeEntry(userImpressionLocalId, timeEntryDTO, bindingResult, model, locale, redirectAttributes,
            turboFrame, request);
    }

    private ModelAndView updateTimeEntry(UserLocalId ownerLocalId, TimeEntryDTO timeEntryDTO, BindingResult bindingResult,
                                         Model model, Locale locale, RedirectAttributes redirectAttributes,
                                         String turboFrame, HttpServletRequest request) {

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

        if (hasText(turboFrame)) {
            if (bindingResult.hasErrors()) {
                model.addAttribute("turboEditedTimeEntry", timeEntryDTO);
            } else {
                final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear);
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
                addTimeEntriesToModel(year, weekOfYear, model, ownerLocalId, locale);
                return new ModelAndView("timeentries/index");
            } else {
                return new ModelAndView("redirect:" + request.getHeader("referer"));
            }
        }
    }

    @PostMapping(value = "/timeentries/{id}", params = "delete")
    public String deleteTimeEntry(@PathVariable("id") Long id, Model model,
                                  @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                  @CurrentUser CurrentOidcUser currentUser, Locale locale) {

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(id)
            .orElseThrow(() -> new IllegalStateException("could not find time entry with id=%s".formatted(id)));

        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        if (!userLocalId.equals(timeEntry.userIdComposite().localId())) {
            throw new AuthorizationDeniedException("You are not allowed to delete timeEntry id=%s".formatted(id));
        }

        timeEntryService.deleteTimeEntry(id);

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (hasText(turboFrame)) {
            prepareTimeEntryDeletedModel(model, locale, timeEntry, userLocalId, year, weekOfYear);
            return "timeentries/index::#" + turboFrame;
        } else {
            return "redirect:/timeentries/%s/%s".formatted(year, weekOfYear);
        }
    }

    @PostMapping(value = "/person/{userId}/timeentries/{id}", params = "delete")
    public String personDeleteTimeEntry(@PathVariable("userId") Long ownerUserLocalIdValue, @PathVariable("id") Long id, Model model,
                                        @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                        @CurrentUser CurrentOidcUser currentUser, Locale locale) {


        final TimeEntry timeEntry = timeEntryService.findTimeEntry(id)
            .orElseThrow(() -> new IllegalStateException("could not find time entry with id=%s".formatted(id)));

        final UserLocalId ownerLocalId = timeEntry.userIdComposite().localId();
        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        final boolean isOwner = userLocalId.equals(timeEntry.userIdComposite().localId());
        if (!isOwner && currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)) {
            throw new AuthorizationDeniedException("You are not allowed to delete timeEntry id=%s".formatted(id));
        }

        timeEntryService.deleteTimeEntry(id);

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (hasText(turboFrame)) {
            prepareUserImpression(model, ownerLocalId);
            prepareTimeEntryDeletedModel(model, locale, timeEntry, ownerLocalId, year, weekOfYear);
            return "timeentries/index::#" + turboFrame;
        } else {
            return "redirect:/person/%s/timeentries?year=%s&week=%s".formatted(ownerUserLocalIdValue, year, weekOfYear);
        }
    }

    private void prepareUserImpression(Model model, UserLocalId userImpressionLocalId) {
        final User userImpression = findUser(userImpressionLocalId);
        model.addAttribute("impression", toImpressionUserDto(userImpression));
    }

    private void prepareTimeEntryDeletedModel(Model model, Locale locale, TimeEntry timeEntry, UserLocalId ownerLocalId, int year, int weekOfYear) {

        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear);

        final Optional<TimeEntryDay> timeEntryDay = entryWeekPage.timeEntryWeek().days()
            .stream()
            .filter(day -> day.date().equals(timeEntry.start().toLocalDate()))
            .findFirst();

        final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();

        addTimeEntriesToModel(year, weekOfYear, model, ownerLocalId, locale);

        model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
        model.addAttribute("turboEditedDay", timeEntryDay.map(entry -> toTimeEntryDayDto(entry, locale)).orElse(null));
        model.addAttribute("turboDeletedTimeEntry", viewHelper.toTimeEntryDto(timeEntry));
    }

    private void assertTimeEntryAccess(Long userLocalIdValue, CurrentOidcUser currentUser) {
        if (!userLocalIdValue.equals(currentUser.getUserIdComposite().localId().value()) && !currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)) {
            throw new AuthorizationDeniedException("You are not allowed to see entries for user localId=" + userLocalIdValue);
        }
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            // TODO not found instead of 5xx
            .orElseThrow(() -> new IllegalStateException("could not find user " + userLocalId));
    }

    private void addTimeEntriesToModel(int year, int weekOfYear, Model model, UserLocalId userLocalId, Locale locale) {

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
