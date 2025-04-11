package de.focusshift.zeiterfassung.timeclock;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("timeclock")
class TimeClockController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final String IS_REDIRECTED = "isRedirected";

    private final TimeClockService timeClockService;
    private final TimeEntryLockService timeEntryLockService;

    TimeClockController(TimeClockService timeClockService, TimeEntryLockService timeEntryLockService) {
        this.timeClockService = timeClockService;
        this.timeEntryLockService = timeEntryLockService;
    }

    @GetMapping
    public String editTimeClockView(@CurrentUser CurrentOidcUser currentUser, Model model) {

        final UserId userId = currentUser.getUserIdComposite().id();

        timeClockService.getCurrentTimeClock(userId)
            .map(TimeClockMapper::timeClockToTimeClockDto)
            .ifPresent(dto -> model.addAttribute("timeClockUpdate", dto));

        return "timeclock/timeclock-edit";
    }

    @PostMapping
    public ModelAndView editTimeClock(
        @Valid @ModelAttribute("timeClockUpdate") TimeClockDto timeClockUpdateDto, BindingResult errors,
        @CurrentUser CurrentOidcUser currentUser, HttpServletRequest request,
        @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
        RedirectAttributes redirectAttributes
    ) {
        timeEntryLockService.getMinValidTimeEntryDate().ifPresent(date -> {
         if (timeClockUpdateDto.getDate().isBefore(date) || !timeEntryLockService.isUserAllowedToBypassLock(currentUser.getRoles())) {
                LOG.info("Editing TimeClock is not allowed since currentUser is not privileged to bypass timespan lock.");
                errors.reject("time-entry.validation.timespan.locked");
            }
        });

        if (errors.hasErrors()) {
            if (hasText(turboFrame)) {
                return new ModelAndView("timeclock/timeclock-edit-form::navigation-box-update", UNPROCESSABLE_ENTITY);
            } else {
                return new ModelAndView("timeclock/timeclock-edit");
            }
        }

        final UserId userId = currentUser.getUserIdComposite().id();
        final TimeClockUpdate timeClockUpdate = toTimeClockUpdate(userId, timeClockUpdateDto);

        try {
            timeClockService.updateTimeClock(userId, timeClockUpdate);
        } catch (TimeClockNotStartedException e) {
            throw new ResponseStatusException(PRECONDITION_REQUIRED, "Time clock has not been started yet.");
        }

        redirectAttributes.addFlashAttribute(IS_REDIRECTED, true);

        return new ModelAndView(redirectToPreviousPage(request));
    }

    @PostMapping("/start")
    public String startTimeClock(@CurrentUser CurrentOidcUser currentUser, HttpServletRequest request) {

        final UserId userId = currentUser.getUserIdComposite().id();

        // TODO should we do this in the service?
        final Optional<TimeClock> maybeCurrentTimeClock = timeClockService.getCurrentTimeClock(userId);
        if (maybeCurrentTimeClock.isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Time clock has been started already.");
        }

        timeClockService.startTimeClock(userId);

        return redirectToPreviousPage(request);
    }

    @PostMapping("/stop")
    public String stopTimeClock(@CurrentUser CurrentOidcUser currentUser, HttpServletRequest request) {

        timeClockService.stopTimeClock(currentUser.getUserIdComposite());

        return redirectToPreviousPage(request);
    }

    private String redirectToPreviousPage(HttpServletRequest request) {
        return format("redirect:%s", getReferer(request));
    }

    private String getReferer(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer")).orElse("");
    }

    private TimeClockUpdate toTimeClockUpdate(UserId userId, TimeClockDto timeClockDto) {

        final ZonedDateTime startedAt = ZonedDateTime.of(timeClockDto.getDate(), timeClockDto.getTime(), timeClockDto.getZoneId());

        return new TimeClockUpdate(userId, startedAt, timeClockDto.getComment(), timeClockDto.isBreak());
    }
}
