package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.Optional;

@ControllerAdvice(assignableTypes = {HasTimeClock.class})
class TimeClockControllerAdvice {

    private final TimeClockService timeClockService;
    private TimeEntryLockService timeEntryLockService;

    TimeClockControllerAdvice(TimeClockService timeClockService, TimeEntryLockService timeEntryLockService) {
        this.timeClockService = timeClockService;
        this.timeEntryLockService = timeEntryLockService;
    }

    private static UserId userId(OidcUser oidcUser) {
        return new UserId(oidcUser.getUserInfo().getSubject());
    }

    @ModelAttribute
    public void addAttributes(Model model, @CurrentUser CurrentOidcUser principal) {
        final UserId userId = userId(principal);
        final Optional<TimeClock> currentTimeClock = timeClockService.getCurrentTimeClock(userId);

        currentTimeClock.map(TimeClockMapper::timeClockToTimeClockDto)
            .ifPresent(timeClockDto -> model.addAttribute("timeClock", timeClockDto));

        getMinValidTimeEntryDate(principal).ifPresent(date ->
            model.addAttribute("minValidTimeEntryDate", date)
        );
    }

    private Optional<LocalDate> getMinValidTimeEntryDate(CurrentOidcUser currentUser) {
        if (timeEntryLockService.isUserAllowedToBypassLock(currentUser.getRoles())) {
            return Optional.empty();
        } else {
            return timeEntryLockService.getMinValidTimeEntryDate();
        }
    }
}
