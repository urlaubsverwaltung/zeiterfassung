package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@ControllerAdvice(basePackages = {"de.focusshift.zeiterfassung.feedback", "de.focusshift.zeiterfassung.report", "de.focusshift.zeiterfassung.timeclock", "de.focusshift.zeiterfassung.timeentry"})
class TimeClockControllerAdvice {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TimeClockService timeClockService;

    TimeClockControllerAdvice(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @ModelAttribute
    public void addAttributes(Model model, @AuthenticationPrincipal DefaultOidcUser principal) {
        final UserId userId = userId(principal);
        final Optional<TimeClock> currentTimeClock = timeClockService.getCurrentTimeClock(userId);

        currentTimeClock.map(this::toTimeClockDto)
            .ifPresent(timeClockDto -> model.addAttribute("timeClock", timeClockDto));
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

    private static UserId userId(DefaultOidcUser oidcUser) {
        return new UserId(oidcUser.getUserInfo().getSubject());
    }
}
