package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice(basePackages = {"de.focusshift.zeiterfassung.feedback", "de.focusshift.zeiterfassung.report", "de.focusshift.zeiterfassung.timeclock", "de.focusshift.zeiterfassung.timeentry"})
class TimeClockControllerAdvice {

    private final TimeClockService timeClockService;

    TimeClockControllerAdvice(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @ModelAttribute
    public void addAttributes(Model model, @AuthenticationPrincipal DefaultOidcUser principal) {
        final UserId userId = userId(principal);
        final Optional<TimeClock> currentTimeClock = timeClockService.getCurrentTimeClock(userId);

        currentTimeClock.map(TimeClockMapper::timeClockToTimeClockDto)
            .ifPresent(timeClockDto -> model.addAttribute("timeClock", timeClockDto));
    }

    private static UserId userId(DefaultOidcUser oidcUser) {
        return new UserId(oidcUser.getUserInfo().getSubject());
    }
}
