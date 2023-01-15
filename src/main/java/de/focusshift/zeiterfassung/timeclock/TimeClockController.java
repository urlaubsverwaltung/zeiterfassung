package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Controller
@RequestMapping("timeclock")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class TimeClockController implements HasTimeClock, HasLaunchpad {

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
    public String editTimeClock(@Valid @ModelAttribute("timeClockUpdate") TimeClockDto timeClockUpdateDto, BindingResult errors,
                                @AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request,
                                @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        if (errors.hasErrors()) {
            if (StringUtils.hasText(turboFrame)) {
                return "timeclock/timeclock-edit-form::navigation-box-update";
            } else {
                return "timeclock/timeclock-edit";
            }
        }

        final UserId userId = principalToUserId(principal);
        final TimeClockUpdate timeClockUpdate = toTimeClockUpdate(userId, timeClockUpdateDto);

        try {
            timeClockService.updateTimeClock(userId, timeClockUpdate);
        } catch (TimeClockNotStartedException e) {
            throw new ResponseStatusException(PRECONDITION_REQUIRED, "Time clock has not been started yet.");
        }

        return redirectToPreviousPage(request);
    }

    @PostMapping("/start")
    public String startTimeClock(@AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

        final UserId userId = principalToUserId(principal);

        // TODO should we do this in the service?
        final Optional<TimeClock> maybeCurrentTimeClock = timeClockService.getCurrentTimeClock(userId);
        if (maybeCurrentTimeClock.isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Time clock has been started already.");
        }

        timeClockService.startTimeClock(userId);

        return redirectToPreviousPage(request);
    }

    @PostMapping("/stop")
    public String stopTimeClock(@AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

        final UserId userId = principalToUserId(principal);

        timeClockService.stopTimeClock(userId);

        return redirectToPreviousPage(request);
    }

    private String redirectToPreviousPage(HttpServletRequest request) {
        return String.format("redirect:%s", getReferer(request));
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
