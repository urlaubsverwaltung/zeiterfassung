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
import de.focusshift.zeiterfassung.web.NotFoundException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.web.servlet.view.RedirectView;

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
import static de.focusshift.zeiterfassung.timeentry.TimeEntryViewHelper.TIME_ENTRY_MODEL_NAME;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.Month.DECEMBER;
import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.Objects.requireNonNullElseGet;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

@Controller
class TimeEntryController implements HasTimeClock, HasLaunchpad {

    private  static final String IS_REDIRECTED = "isRedirected";

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TimeEntryService timeEntryService;
    private final UserManagementService userManagementService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final TimeEntryViewHelper viewHelper;
    private final Clock clock;

    public TimeEntryController(TimeEntryService timeEntryService,
                               UserManagementService userManagementService, UserSettingsProvider userSettingsProvider,
                               DateFormatter dateFormatter, TimeEntryViewHelper viewHelper, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userManagementService = userManagementService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.viewHelper = viewHelper;
        this.clock = clock;
    }

    @GetMapping("/timeentries")
    public ModelAndView timeEntries(@RequestParam(value = "year", required = false) Integer year,
                              @RequestParam(value = "week", required = false) Integer weekOfYear,
                              @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                              Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);

        return prepareTimeEntriesForYearAndWeekOfYear(yearAndWeek, currentUserLocalId, currentUser, model, locale, turboFrame);
    }

    @GetMapping("/timeentries/users/{ownerLocalIdValue}")
    public ModelAndView userTimeEntries(@PathVariable Long ownerLocalIdValue,
                                       @RequestParam(value = "year", required = false) Integer year,
                                       @RequestParam(value = "week", required = false) Integer weekOfYear,
                                       @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                       Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        assertTimeEntryAccess(currentUser, ownerLocalIdValue);

        final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);
        final UserLocalId ownerUserLocalId = new UserLocalId(ownerLocalIdValue);
        prepareViewedUser(model, ownerUserLocalId);

        return prepareTimeEntriesForYearAndWeekOfYear(yearAndWeek, ownerUserLocalId, currentUser, model, locale, turboFrame);
    }

    private ModelAndView prepareTimeEntriesForYearAndWeekOfYear(YearAndWeek yearAndWeek, UserLocalId ownerLocalId, CurrentOidcUser currentUser, Model model, Locale locale, String turboFrame) {

        if (!model.containsAttribute(IS_REDIRECTED) && hasText(turboFrame)) {
            prepareTimeEntriesForYearAndWeekOfYear(yearAndWeek, ownerLocalId, model, locale);
            model.addAttribute("turboStreamsEnabled", true);
            return new ModelAndView("timeentries/index::#frame-time-entry-weeks");
        } else {
            return prepareTimeEntriesForYearAndWeekOfYear(yearAndWeek, ownerLocalId, model, locale);
        }
    }

    private ModelAndView prepareTimeEntriesForYearAndWeekOfYear(YearAndWeek yearAndWeek, UserLocalId ownerLocalId, Model model, Locale locale) {

        final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setUserLocalId(ownerLocalId.value());

        viewHelper.addTimeEntryToModel(model, timeEntryDTO);
        addTimeEntryWeeksPageToModel(yearAndWeek, model, ownerLocalId, locale);

        return new ModelAndView("timeentries/index");
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

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        LOG.info("User {} wants to create a new timeEntry.", currentUserLocalId);

        handleCreateTimeEntry(timeEntryDTO, bindingResult, currentUser, model, locale, redirectAttributes);

        if (bindingResult.hasErrors()) {
            LOG.info("Could not create timeEntry due to errors. Rendering timeentries page.");
            return "timeentries/index";
        }

        final String url = request.getHeader("referer");
        LOG.info("Created new timeEntry. Redirecting to {}", url);
        return "redirect:" + url;
    }

    @PostMapping("/timeentries/users/{ownerLocalIdValue}")
    public String userCreateTimeEntry(@PathVariable Long ownerLocalIdValue,
                                      @Valid @ModelAttribute(TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO, BindingResult bindingResult,
                                      @CurrentUser CurrentOidcUser currentUser,
                                      RedirectAttributes redirectAttributes,
                                      Model model, Locale locale) {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final UserLocalId ownerLocalId = new UserLocalId(ownerLocalIdValue);
        LOG.info("User {} wants to create a new timeEntry for user {}", currentUserLocalId, ownerLocalId);

        assertTimeEntryAccess(currentUser, ownerLocalIdValue);
        handleCreateTimeEntry(timeEntryDTO, bindingResult, currentUser, model, locale, redirectAttributes);

        if (bindingResult.hasErrors()) {
            LOG.info("Could not create timeEntry for user {} due to errors. Rendering timeentries page.", ownerLocalId);
            return "timeentries/index";
        }

        final String userTimeEntriesUri = fromMethodCall(on(TimeEntryController.class)
            .userTimeEntries(ownerLocalIdValue, null, null, null, model, locale, currentUser))
            .build().toUriString();

        LOG.info("User {} created new timeEntry for user {}. Redirecting to {}.", currentUserLocalId, ownerLocalId, userTimeEntriesUri);
        return "redirect:" + userTimeEntriesUri;
    }

    private void handleCreateTimeEntry(TimeEntryDTO timeEntryDto, BindingResult bindingResult,
                                       CurrentOidcUser currentUser,
                                       Model model, Locale locale, RedirectAttributes redirectAttributes) {

        if (timeEntryDto.getId() != null) {
            throw new IllegalStateException("Expected timeEntry id not to be defined but has value. Did you meant to update the time entry?");
        }

        final int year;
        final int weekOfYear;

        try {
            final LocalDate firstDateOfWeek = localDateToFirstDateOfWeek(timeEntryDto.getDate());
            year = firstDateOfWeek.getYear();
            weekOfYear = firstDateOfWeek.get(WEEK_OF_WEEK_BASED_YEAR);
        } catch (NullPointerException exception) {
            throw new InvalidTimeEntryException("Invalid timeEntry. Expected date to be set.");
        }

        viewHelper.createTimeEntry(timeEntryDto, bindingResult, currentUser);

        if (bindingResult.hasErrors()) {
            final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);
            final UserLocalId ownerLocalId = new UserLocalId(timeEntryDto.getUserLocalId());
            addTimeEntryWeeksPageToModel(yearAndWeek, model, ownerLocalId, locale);
            viewHelper.handleCrudTimeEntryErrors(timeEntryDto, bindingResult, model, redirectAttributes);
        }
    }

    @PostMapping(value = "/timeentries/{timeEntryIdValue}")
    public ModelAndView updateTimeEntry(
        @PathVariable Long timeEntryIdValue,
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @CurrentUser CurrentOidcUser currentUser,
        @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        LOG.info("User {} wants to update timeEntry {}.", currentUserLocalId, timeEntryIdValue);

        return updateTimeEntry(currentUser, timeEntryDTO, bindingResult, model, locale, redirectAttributes,
            turboFrame, request);
    }

    @PostMapping(value = "/timeentries/users/{ownerLocalIdValue}/timeentry/{timeEntryIdValue}")
    public ModelAndView userUpdateTimeEntry(
        @PathVariable Long timeEntryIdValue,
        @PathVariable Long ownerLocalIdValue,
        @Valid @ModelAttribute(name = TIME_ENTRY_MODEL_NAME) TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @CurrentUser CurrentOidcUser currentUser,
        @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
        Model model, Locale locale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) throws InvalidTimeEntryException {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final UserLocalId ownerLocalId = new UserLocalId(ownerLocalIdValue);
        LOG.info("User {} wants to update timeEntry {} of user {}.", currentUserLocalId, timeEntryIdValue, ownerLocalId);

        assertTimeEntryAccess(currentUser, ownerLocalIdValue);

        return updateTimeEntry(currentUser, timeEntryDTO, bindingResult, model, locale, redirectAttributes, turboFrame, request);
    }

    private ModelAndView updateTimeEntry(CurrentOidcUser currentUser, TimeEntryDTO timeEntryDTO, BindingResult bindingResult,
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

        final Long timeEntryId = timeEntryDTO.getId();
        final UserLocalId ownerLocalId = new UserLocalId(timeEntryDTO.getUserLocalId());

        viewHelper.updateTimeEntry(currentUser, timeEntryDTO, bindingResult, model, redirectAttributes);

        if (hasText(turboFrame)) {
            if (bindingResult.hasErrors()) {
                LOG.info("Could not update timeEntry {} due to constraint violation errors. Rendering turbo frame.", timeEntryId);
                model.addAttribute("turboEditedTimeEntry", timeEntryDTO);
            } else {
                LOG.info("Updated timeEntry {}. Rendering turbo frame.", timeEntryId);
                final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear);
                final TimeEntryDay timeEntryDay = entryWeekPage.timeEntryWeek().days()
                    .stream()
                    .filter(day -> day.date().equals(timeEntryDTO.getDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("expected a day"));

                final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();
                final TimeEntry editedTimeEntry = timeEntryDay.timeEntries().stream()
                    .filter(entry -> entry.id().value().equals(timeEntryId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("could not find edited timeEntry=%s".formatted(timeEntryId)));

                model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
                model.addAttribute("turboEditedDay", toTimeEntryDayDto(timeEntryDay, locale));
                model.addAttribute("turboEditedTimeEntry", viewHelper.toTimeEntryDto(editedTimeEntry));
            }
            return new ModelAndView("timeentries/index::#frame-time-entry");
        } else {
            if (bindingResult.hasErrors()) {
                final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);
                addTimeEntryWeeksPageToModel(yearAndWeek, model, ownerLocalId, locale);
                LOG.info("Could not update timeEntry {} due to constraint violoation errors. Rendering timeentries page.", timeEntryId);
                return new ModelAndView("timeentries/index");
            } else {
                final String url = request.getHeader("referer");
                LOG.info("Updated timeEntry {}. Redirecting to {}", timeEntryId, url);
                return new ModelAndView("redirect:" + url);
            }
        }
    }

    @PostMapping(value = "/timeentries/{timeEntryIdValue}", params = "delete")
    public String deleteTimeEntry(@PathVariable Long timeEntryIdValue,
                                  @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                  Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        LOG.info("User {} wants to delete timeEntry {}", currentUserLocalId, timeEntryIdValue);

        final TimeEntryId timeEntryId = new TimeEntryId(timeEntryIdValue);
        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new IllegalStateException("Could not find timeEntry %s.".formatted(timeEntryIdValue)));

        final UserLocalId ownerLocalId = timeEntry.userIdComposite().localId();
        final boolean isOwner = ownerLocalId.equals(currentUserLocalId);
        final boolean allowedToEdit = currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);
        if (!allowedToEdit && !isOwner) {
            throw new AccessDeniedException("You are not allowed to delete timeEntry %s.".formatted(timeEntryIdValue));
        }

        timeEntryService.deleteTimeEntry(timeEntryId);

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (hasText(turboFrame)) {
            LOG.info("Successfully deleted timeEntry {}. Rendering turboFrame section.", timeEntryId);
            final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);
            prepareTimeEntryDeletedModel(model, locale, timeEntry, yearAndWeek);
            return "timeentries/index::#" + turboFrame;
        } else {
            final String url = "/timeentries?year=%d&week=%d".formatted(year, weekOfYear);
            LOG.info("Successfully deleted timeEntry {}. Redirecting to {}.", timeEntryId, url);
            return "redirect:" + url;
        }
    }

    @PostMapping(value = "/timeentries/users/{ownerLocalIdValue}/timeentry/{timeEntryIdValue}", params = "delete")
    public ModelAndView userDeleteTimeEntry(@PathVariable Long ownerLocalIdValue,
                                            @PathVariable Long timeEntryIdValue,
                                            @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                            Model model, Locale locale, @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        LOG.info("User {} wants to delete timeEntry {} of user {}", currentUserLocalId, timeEntryIdValue, ownerLocalIdValue);

        final TimeEntryId timeEntryId = new TimeEntryId(timeEntryIdValue);
        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new IllegalStateException("Could not find timeEntry %s.".formatted(timeEntryIdValue)));

        assertTimeEntryAccess(currentUser, timeEntry);

        final UserLocalId ownerLocalId = timeEntry.userIdComposite().localId();

        timeEntryService.deleteTimeEntry(timeEntryId);

        final int year = timeEntry.start().getYear();
        final int weekOfYear = timeEntry.start().get(WEEK_OF_WEEK_BASED_YEAR);

        if (hasText(turboFrame)) {
            LOG.info("User {} deleted timeEntry {} of user {}. Rendering turboFrame section.", currentUserLocalId, timeEntryId, ownerLocalId);
            final YearAndWeek yearAndWeek = yearAndWeek(year, weekOfYear);
            prepareViewedUser(model, ownerLocalId);
            prepareTimeEntryDeletedModel(model, locale, timeEntry, yearAndWeek);
            return new ModelAndView("timeentries/index::#" + turboFrame);
        }

        final String userTimeEntriesUri = fromMethodCall(on(TimeEntryController.class)
            .userTimeEntries(ownerLocalIdValue, year, weekOfYear, null, model, locale, currentUser))
            .build().toUriString();

        LOG.info("User {} deleted timeEntry {} of user {}. Redirecting to {}.", currentUserLocalId, timeEntryId, ownerLocalId, userTimeEntriesUri);
        return new ModelAndView(new RedirectView(userTimeEntriesUri));
    }

    private void prepareViewedUser(Model model, UserLocalId userLocalId) {
        final User viewedUser = findUser(userLocalId);
        model.addAttribute("viewedUser", toViewedUserDto(viewedUser));
    }

    private void prepareTimeEntryDeletedModel(Model model, Locale locale, TimeEntry timeEntry, YearAndWeek yearAndWeek) {

        final Integer year = yearAndWeek.year();
        final Integer weekOfYear = yearAndWeek.week();

        final UserLocalId ownerLocalId = timeEntry.userIdComposite().localId();
        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear);

        final Optional<TimeEntryDay> timeEntryDay = entryWeekPage.timeEntryWeek().days()
            .stream()
            .filter(day -> day.date().equals(timeEntry.start().toLocalDate()))
            .findFirst();

        final TimeEntryWeek timeEntryWeek = entryWeekPage.timeEntryWeek();

        addTimeEntryWeeksPageToModel(yearAndWeek, model, ownerLocalId, locale);

        model.addAttribute("turboEditedWeek", toTimeEntryWeekDto(timeEntryWeek, locale));
        model.addAttribute("turboEditedDay", timeEntryDay.map(entry -> toTimeEntryDayDto(entry, locale)).orElse(null));
        model.addAttribute("turboDeletedTimeEntry", viewHelper.toTimeEntryDto(timeEntry));
    }

    private void assertTimeEntryAccess(CurrentOidcUser currentUser, TimeEntry timeEntry) {

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final UserLocalId ownerUserLocalId = timeEntry.userIdComposite().localId();

        final boolean isOwner = ownerUserLocalId.equals(currentUserLocalId);
        final boolean allowedToEdit = currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);

        if (!isOwner && !allowedToEdit) {
            throw new AccessDeniedException("You are not allowed to access timeentries of user %d".formatted(ownerUserLocalId.value()));
        }
    }

    private void assertTimeEntryAccess(CurrentOidcUser currentUser, Long ownerLocalIdValue) {
        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        if (!ownerLocalIdValue.equals(currentUserLocalId.value()) && !currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)) {
            throw new AccessDeniedException("You are not allowed to access timeentries of user " + ownerLocalIdValue);
        }
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new NotFoundException("could not find user " + userLocalId));
    }

    private void addTimeEntryWeeksPageToModel(YearAndWeek yearAndWeek, Model model, UserLocalId ownerLocalId, Locale locale) {

        final Integer year = yearAndWeek.year();
        final Integer weekOfYear = yearAndWeek.week();

        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(ownerLocalId, year, weekOfYear);
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

    private YearAndWeek yearAndWeek(@Nullable Integer year, @Nullable Integer weekOfYear) {

        final LocalDate now = LocalDate.now(clock);
        final Supplier<ZonedDateTime> userStartOfDay = () -> now.atStartOfDay(userSettingsProvider.zoneId());

        year = requireNonNullElseGet(year, () -> userStartOfDay.get().getYear());
        weekOfYear = requireNonNullElseGet(weekOfYear, () -> userStartOfDay.get().get(WEEK_OF_WEEK_BASED_YEAR));

        return new YearAndWeek(year, weekOfYear);
    }

    private record YearAndWeek(Integer year, Integer week) {}

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
            .isLocked(timeEntryDay.locked())
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

    private static ViewedUserDto toViewedUserDto(User user) {
        return new ViewedUserDto(user.userLocalId().value(), user.givenName(), user.familyName(),
            user.fullName(), user.email().value());
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
