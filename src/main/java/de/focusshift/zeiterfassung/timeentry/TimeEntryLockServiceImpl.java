package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettingsService;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Optional;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class TimeEntryLockServiceImpl implements TimeEntryLockService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final LockTimeEntriesSettingsService lockTimeEntriesSettingsService;
    private final UserSettingsProvider userSettingsProvider;
    private final Clock clock;

    TimeEntryLockServiceImpl(LockTimeEntriesSettingsService lockTimeEntriesSettingsService, UserSettingsProvider userSettingsProvider, Clock clock) {
        this.lockTimeEntriesSettingsService = lockTimeEntriesSettingsService;
        this.userSettingsProvider = userSettingsProvider;
        this.clock = clock;
    }

    @Override
    public LockTimeEntriesSettings getLockTimeEntriesSettings() {
        return lockTimeEntriesSettingsService.getLockTimeEntriesSettings();
    }

    @Override
    public boolean isLocked(Temporal temporal, LockTimeEntriesSettings lockTimeEntriesSettings) {

        if (!lockTimeEntriesSettings.lockingIsActive()) {
            LOG.debug("temporal is not locked. Feature is disabled in settings.");
            return false;
        }

        final int daysInPastAllowed = lockTimeEntriesSettings.lockTimeEntriesDaysInPast();
        boolean isLocked = isDateLocked(temporal, daysInPastAllowed);

        LOG.debug("temporal {} locked={}", temporal, isLocked);

        return isLocked;
    }

    @Override
    public boolean isTimespanLocked(Temporal start, Temporal end) {

        final LockTimeEntriesSettings settings = getLockTimeEntriesSettings();
        if (!settings.lockingIsActive()) {
            LOG.debug("Timespan is not locked. Feature is disabled in settings.");
            return false;
        }

        final int daysInPastAllowed = settings.lockTimeEntriesDaysInPast();

        final boolean startLocked = isDateLocked(start, daysInPastAllowed);
        final boolean endLocked = isDateLocked(end, daysInPastAllowed);
        final boolean isLocked = startLocked || endLocked;

        LOG.debug("start={} locked={}, end={} locked={}", start, startLocked, end, endLocked);

        return isLocked;
    }

    @Override
    public boolean isUserAllowedToBypassLock(Collection<SecurityRole> roles) {
        return roles.contains(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);
    }

    @Override
    public Optional<LocalDate> getMinValidTimeEntryDate() {

        final LockTimeEntriesSettings settings = getLockTimeEntriesSettings();

        final LocalDate minimumValidDate;
        if (settings.lockingIsActive()) {
            final int daysInPast = settings.lockTimeEntriesDaysInPast();
            final ZoneId userZoneId = userSettingsProvider.zoneId();
            minimumValidDate = LocalDate.ofInstant(clock.instant(), userZoneId).minusDays(daysInPast);
        } else {
            minimumValidDate = null;
        }

        return Optional.ofNullable(minimumValidDate);
    }

    private boolean isDateLocked(Temporal date, int daysInPastAllowed) {

        final ZonedDateTime now = ZonedDateTime.now(clock);
        final long daysBetween = DAYS.between(date, now);

        return daysBetween > daysInPastAllowed;
    }
}
