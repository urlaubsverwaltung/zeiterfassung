package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettingsService;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class TimeEntryLockServiceImpl implements TimeEntryLockService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final LockTimeEntriesSettingsService lockTimeEntriesSettingsService;
    private final Clock clock;

    TimeEntryLockServiceImpl(LockTimeEntriesSettingsService lockTimeEntriesSettingsService, Clock clock) {
        this.lockTimeEntriesSettingsService = lockTimeEntriesSettingsService;
        this.clock = clock;
    }

    @Override
    public boolean isTimespanLocked(Temporal start, Temporal end) {

        final LockTimeEntriesSettings settings = lockTimeEntriesSettingsService.getLockTimeEntriesSettings();
        if (!settings.lockingIsActive()) {
            LOG.debug("Timespan is not locked. Feature is disabled in settings.");
            return false;
        }

        final int daysInPastAllowed = settings.lockTimeEntriesDaysInPast();

        final boolean startLocked = isDateLocked(start, daysInPastAllowed);
        final boolean endLocked = isDateLocked(end, daysInPastAllowed);
        final boolean isLocked = startLocked || endLocked;

        LOG.debug("{} locked={}", start, isLocked);

        return isLocked;
    }

    @Override
    public boolean isUserAllowedToBypassLock(Collection<SecurityRole> roles) {
        return roles.contains(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);
    }

    private boolean isDateLocked(Temporal date, int daysInPastAllowed) {

        final ZonedDateTime now = ZonedDateTime.now(clock);
        final long daysBetween = DAYS.between(date, now);

        return daysBetween > daysInPastAllowed;
    }
}
