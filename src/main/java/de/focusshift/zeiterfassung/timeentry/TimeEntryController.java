package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.MonthFormat;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.YearFormat;
import org.slf4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.Month.DECEMBER;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class TimeEntryController implements HasTimeClock {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final Clock clock;

    public TimeEntryController(TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider, DateFormatter dateFormatter, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.clock = clock;
    }

    @GetMapping("/timeentries")
    public String items(Model model, @AuthenticationPrincipal OidcUser principal) {

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final LocalDate nowUserAware = LocalDate.now(userZoneId);

        final int year = nowUserAware.getYear();
        final int weekOfYear = nowUserAware.get(WEEK_OF_WEEK_BASED_YEAR);

        return prepareTimeEntriesView(year, weekOfYear, model, principal);
    }

    @GetMapping("/timeentries/{year}/{weekOfYear}")
    public String timeEntriesForYearAndWeekOfYear(@PathVariable("year") int year,
                                                  @PathVariable("weekOfYear") int weekOfYear,
                                                  @AuthenticationPrincipal OidcUser principal,
                                                  @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                                  Model model) {

        if (StringUtils.hasText(turboFrame)) {
            prepareTimeEntriesView(year, weekOfYear, model, principal);
            model.addAttribute("turboStreamsEnabled", true);
            return "timeentries/index::#frame-time-entry-weeks";
        } else {
            return prepareTimeEntriesView(year, weekOfYear, model, principal);
        }
    }

    private String prepareTimeEntriesView(int year, int weekOfYear, Model model, OidcUser principal) {
        addTimeEntryToModel(model, new TimeEntryDTO());
        addTimeEntriesToModel(year, weekOfYear, model, principal);

        return "timeentries/index";
    }

    @PostMapping("/timeentries")
    public String save(
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        Model model,
        @AuthenticationPrincipal OidcUser principal,
        HttpServletRequest request
    ) {
        final String viewName = saveOrUpdate(timeEntryDTO, bindingResult, model, principal, request);
        if (bindingResult.hasErrors()) {
            final ZoneId userZoneId = userSettingsProvider.zoneId();
            final LocalDate now = LocalDate.now(clock);
            final ZonedDateTime userAwareNow = ZonedDateTime.ofLocal(now.atStartOfDay(), userZoneId, UTC);
            final int year = userAwareNow.getYear();
            final int weekOfYear = userAwareNow.get(WEEK_OF_WEEK_BASED_YEAR);
            addTimeEntriesToModel(year, weekOfYear, model, principal);
        }
        return viewName;
    }

    @PostMapping(value = "/timeentries/{id}")
    public String update(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute(name = "timeEntry") TimeEntryDTO timeEntryDTO,
        BindingResult bindingResult,
        @AuthenticationPrincipal OidcUser principal,
        @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
        Model model,
        HttpServletRequest request
    ) {

        final String viewName = saveOrUpdate(timeEntryDTO, bindingResult, model, principal, request);

        if (StringUtils.hasText(turboFrame)) {
            model.addAttribute("turboEditedTimeEntry", timeEntryDTO);
            return "timeentries/index::#frame-time-entry";
        } else {
            if (bindingResult.hasErrors()) {
                final ZoneId userZoneId = userSettingsProvider.zoneId();
                final LocalDate now = LocalDate.now(clock);
                final ZonedDateTime userAwareNow = ZonedDateTime.ofLocal(now.atStartOfDay(), userZoneId, UTC);
                final int year = userAwareNow.getYear();
                final int weekOfYear = userAwareNow.get(WEEK_OF_WEEK_BASED_YEAR);
                addTimeEntriesToModel(year, weekOfYear, model, principal);
            }
            return viewName;
        }
    }

    private String saveOrUpdate(TimeEntryDTO timeEntryDTO, BindingResult bindingResult, Model model, OidcUser principal, HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("timeEntryErrorId", timeEntryDTO);
            final boolean hasErrorStart = bindingResult.hasFieldErrors("start");
            final boolean hasErrorEnd = bindingResult.hasFieldErrors("end");
            final boolean hasErrorDuration = bindingResult.hasFieldErrors("duration");

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
        final TimeEntry timeEntry = toTimeEntry(timeEntryDTO, userId);

        timeEntryService.saveTimeEntry(timeEntry);

        final String referer = request.getHeader("referer");
        return "redirect:" + referer;
    }

    @PostMapping(value = "/timeentries/{id}", params = "delete")
    public String delete(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal OidcUser principal) {

        timeEntryService.deleteTimeEntry(id);

        return "redirect:/timeentries";
    }

    @ResponseBody
    @PostMapping(value = "/timeentries/{id}", params = "delete", headers = {"X-Requested-With=ajax"})
    public void deleteJs(@PathVariable("id") Long id, @AuthenticationPrincipal OidcUser principal) {

        timeEntryService.deleteTimeEntry(id);
    }

    private void addTimeEntryToModel(Model model, TimeEntryDTO timeEntryDTO) {
        model.addAttribute("timeEntry", timeEntryDTO);
    }

    private void addTimeEntriesToModel(int year, int weekOfYear, Model model, OidcUser principal) {

        final UserId userId = new UserId(principal.getUserInfo().getSubject());

        final TimeEntryWeekPage entryWeekPage = timeEntryService.getEntryWeekPage(userId, year, weekOfYear);
        final TimeEntryWeekDto timeEntryWeekDto = toTimeEntryWeekDto(entryWeekPage.timeEntryWeek());

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

    private TimeEntryWeekDto toTimeEntryWeekDto(TimeEntryWeek timeEntryWeek) {

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

        final List<TimeEntryDTO> timeEntryDtos = timeEntryWeek.timeEntries()
            .stream()
            .map(this::toTimeEntryDto)
            .toList();

        final Duration durationMinutes = timeEntryWeek.workDuration().minutes().duration();
        final String hoursWorked = String.format("%02d:%02d", durationMinutes.toHours(), durationMinutes.toMinutesPart());

        return new TimeEntryWeekDto(timeEntryWeek.week(), firstDateString, lastDateString, hoursWorked, timeEntryDtos);
    }

    private TimeEntryDTO toTimeEntryDto(TimeEntry timeEntry) {

        final ZonedDateTime start = timeEntry.start();
        final ZonedDateTime end = timeEntry.end();

        final LocalDate date = start.toLocalDate();
        final LocalTime startTime = start.toLocalTime();
        final LocalTime endTime = end.toLocalTime();

        final Duration duration = timeEntry.workDuration().minutes().duration();
        final String durationString = toTimeEntryDTODurationString(duration);

        return TimeEntryDTO.builder()
            .id(timeEntry.id())
            .date(date)
            .start(startTime)
            .end(endTime)
            .duration(durationString)
            .comment(timeEntry.comment())
            .isBreak(timeEntry.isBreak())
            .build();
    }

    private TimeEntry toTimeEntry(TimeEntryDTO timeEntryDTO, UserId userId) {

        LocalDate date = timeEntryDTO.getDate();
        LocalTime startTime = timeEntryDTO.getStart();
        LocalTime endTime = timeEntryDTO.getEnd();
        Duration duration = toDuration(timeEntryDTO.getDuration());

        final ZoneId zoneId = userSettingsProvider.zoneId();

        if (startTime != null && endTime == null && duration != null) {
            endTime = startTime.plusMinutes(duration.toMinutes());
        } else if (startTime == null && duration != null) {
            startTime = endTime.minusMinutes(duration.toMinutes());
        } else if (startTime != null && endTime != null && duration == null) {
            duration = Duration.between(startTime, endTime);
        }

        ZonedDateTime start = null;
        ZonedDateTime end = null;

        if (startTime != null && endTime != null) {
            start = ZonedDateTime.of(LocalDateTime.of(date, startTime), zoneId);
            if (endTime.isBefore(startTime)) {
                end = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), endTime), zoneId);
            } else {
                end = ZonedDateTime.of(LocalDateTime.of(date, endTime), zoneId);
            }
        } else if (startTime == null && duration != null) {
            start = ZonedDateTime.of(LocalDateTime.of(date, endTime), zoneId).minusSeconds(duration.toSeconds());
            if (endTime.isBefore(startTime)) {
                end = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), endTime), zoneId);
            } else {
                end = ZonedDateTime.of(LocalDateTime.of(date, endTime), zoneId);
            }
        } else if (startTime != null && duration != null) {
            start = ZonedDateTime.of(LocalDateTime.of(date, startTime), zoneId);
            if (endTime.isBefore(startTime)) {
                end = ZonedDateTime.of(LocalDateTime.of(date.plusDays(1), startTime), zoneId).plusSeconds(duration.toSeconds());
            } else {
                end = ZonedDateTime.of(LocalDateTime.of(date, startTime), zoneId).plusSeconds(duration.toSeconds());
            }
        }

        if (start == null) {
            LOG.info("start was `null` while converting TimeEntryDTO. seems fishy <°))))><");
        }
        if (end == null) {
            LOG.info("end was `null` while converting TimeEntryDTO. seems fishy <°))))><");
        }

        return new TimeEntry(timeEntryDTO.getId(), userId, timeEntryDTO.getComment(), start, end, timeEntryDTO.isBreak());
    }

    private static Duration toDuration(String timeEntryDTODurationString) {
        if (StringUtils.hasText(timeEntryDTODurationString)) {
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
        return Year.of(year).atMonth(DECEMBER).atEndOfMonth().get(WEEK_OF_WEEK_BASED_YEAR);
    }
}
