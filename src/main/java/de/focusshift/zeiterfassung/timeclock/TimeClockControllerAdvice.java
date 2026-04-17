package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static de.focusshift.zeiterfassung.timeclock.TimeClockMapper.timeClockToTimeClockDto;

@ControllerAdvice(assignableTypes = {HasTimeClock.class})
class TimeClockControllerAdvice {

    private final TimeClockService timeClockService;
    private final TimeEntryLockService timeEntryLockService;
    private final UserSettingsProvider userSettingsProvider;
    private final Clock clock;

    TimeClockControllerAdvice(
        TimeClockService timeClockService,
        TimeEntryLockService timeEntryLockService,
        UserSettingsProvider userSettingsProvider,
        Clock clock
    ) {
        this.timeClockService = timeClockService;
        this.timeEntryLockService = timeEntryLockService;
        this.userSettingsProvider = userSettingsProvider;
        this.clock = clock;
    }

    private static UserId userId(OidcUser oidcUser) {
        return new UserId(oidcUser.getUserInfo().getSubject());
    }

    @ModelAttribute
    public void addAttributes(Model model, @CurrentUser CurrentOidcUser principal) {
        final UserId userId = userId(principal);

        timeClockService.getCurrentTimeClock(userId)
            .map(timeClock -> timeClockToTimeClockDto(timeClock, clock))
            .ifPresent(timeClockDto -> model.addAttribute("timeClock", timeClockDto));

        getMinValidTimeEntryDate(principal)
            .ifPresent(date -> model.addAttribute("minValidTimeEntryDate", date));
    }

    private Optional<LocalDate> getMinValidTimeEntryDate(CurrentOidcUser currentUser) {
        if (timeEntryLockService.isUserAllowedToBypassLock(currentUser.getRoles())) {
            return Optional.empty();
        } else {
            final ZoneId userZoneId = userSettingsProvider.zoneId();
            return timeEntryLockService.getMinValidTimeEntryDate(userZoneId);
        }
    }
}
