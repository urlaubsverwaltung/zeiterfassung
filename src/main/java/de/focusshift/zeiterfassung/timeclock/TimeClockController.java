package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Controller
@RequestMapping("timeclock")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class TimeClockController {

    private final TimeClockService timeClockService;

    TimeClockController(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @PostMapping
    public String editTimeClock(TimeClockDto timeClockUpdateDto, @AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

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
        final String previousPage = Optional.ofNullable(request.getHeader("Referer")).orElse("");
        return String.format("redirect:%s", previousPage);
    }

    private TimeClockUpdate toTimeClockUpdate(UserId userId, TimeClockDto timeClockDto) {

        final ZonedDateTime startedAt = ZonedDateTime.of(LocalDate.parse(timeClockDto.getDate()), LocalTime.parse(timeClockDto.getTime()), timeClockDto.getZoneId());

        return new TimeClockUpdate(userId, startedAt, timeClockDto.getComment());
    }

    private static UserId principalToUserId(DefaultOidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }
}
