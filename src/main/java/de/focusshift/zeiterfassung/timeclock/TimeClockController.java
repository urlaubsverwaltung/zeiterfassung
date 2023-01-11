package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Controller
@RequestMapping("timeclock")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class TimeClockController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TimeClockService timeClockService;

    TimeClockController(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @GetMapping
    public String editTimeClockView(@AuthenticationPrincipal DefaultOidcUser principal, Model model) {

        final UserId userId = principalToUserId(principal);

        timeClockService.getCurrentTimeClock(userId)
            .map(this::toTimeClockDto)
            .ifPresent(dto -> model.addAttribute("timeClockUpdate", dto));

        return "timeclock/timeclock-edit";
    }

    @PostMapping
    public String editTimeClock(@Valid @ModelAttribute("timeClockUpdate") TimeClockDto timeClockUpdateDto, BindingResult errors,
                                @AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest request) {

        if (errors.hasErrors()) {
            return "timeclock/timeclock-edit";
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

    private TimeClockDto toTimeClockDto(TimeClock timeClock) {

        final Instant startedAt = timeClock.startedAt().toInstant();

        final Duration duration = Duration.between(startedAt, Instant.now());
        final int hours = duration.toHoursPart();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        final String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        final ZoneId zoneId = timeClock.startedAt().getZone();

        final TimeClockDto timeClockDto = new TimeClockDto();
        timeClockDto.setStartedAt(startedAt);

        timeClockDto.setDate(ZonedDateTime.ofInstant(startedAt, zoneId).format(DATE_FORMATTER));
        timeClockDto.setTime(ZonedDateTime.ofInstant(startedAt, zoneId).format(TIME_FORMATTER));
        timeClockDto.setZoneId(timeClock.startedAt().getZone());
        timeClockDto.setComment(timeClock.comment());
        timeClockDto.setDuration(durationString);

        return timeClockDto;
    }

    private TimeClockUpdate toTimeClockUpdate(UserId userId, TimeClockDto timeClockDto) {

        final ZonedDateTime startedAt = ZonedDateTime.of(LocalDate.parse(timeClockDto.getDate()), LocalTime.parse(timeClockDto.getTime()), timeClockDto.getZoneId());

        return new TimeClockUpdate(userId, startedAt, timeClockDto.getComment());
    }

    private static UserId principalToUserId(DefaultOidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }
}
