package de.focusshift.zeiterfassung.timeclock;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.user.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("timeclock")
class TimeClockController implements HasTimeClock, HasLaunchpad {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TimeClockService timeClockService;

    TimeClockController(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @GetMapping
    public String editTimeClockView(@AuthenticationPrincipal DefaultOidcUser principal, Model model) {

        final UserId userId = principalToUserId(principal);

        timeClockService.getCurrentTimeClock(userId)
            .map(TimeClockMapper::timeClockToTimeClockDto)
            .ifPresent(dto -> model.addAttribute("timeClockUpdate", dto));

        return "timeclock/timeclock-edit";
    }

    @PostMapping
    public ModelAndView editTimeClock(@Valid @ModelAttribute("timeClockUpdate") TimeClockDto timeClockUpdateDto, BindingResult errors,
                                @AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request,
                                @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        if (errors.hasErrors()) {
            if (hasText(turboFrame)) {
                return new ModelAndView("timeclock/timeclock-edit-form::navigation-box-update", UNPROCESSABLE_ENTITY);
            } else {
                return new ModelAndView("timeclock/timeclock-edit");
            }
        }

        final UserId userId = principalToUserId(principal);
        final TimeClockUpdate timeClockUpdate = toTimeClockUpdate(userId, timeClockUpdateDto);

        try {
            timeClockService.updateTimeClock(userId, timeClockUpdate);
        } catch (TimeClockNotStartedException e) {
            throw new ResponseStatusException(PRECONDITION_REQUIRED, "Time clock has not been started yet.");
        }

        return new ModelAndView(redirectToPreviousPage(request));
    }

    @PostMapping("/start")
    public String startTimeClock(@AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

        final UserId userId = principalToUserId(principal);

        try {
            timeClockService.startTimeClock(userId);
        } catch (TransactionSystemException e) {
            // just log and redirect the user to the previous page
            // since the time clock should be running already
            LOG.warn("TimeClock could not be started. Maybe it is running already and user submitted the form twice with a double click.", e);
        }

        return redirectToPreviousPage(request);
    }

    @PostMapping("/stop")
    public String stopTimeClock(@AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

        final UserId userId = principalToUserId(principal);

        timeClockService.stopTimeClock(userId);

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

    private static UserId principalToUserId(DefaultOidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }
}
