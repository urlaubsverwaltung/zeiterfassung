package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public final class TimeEntryOverlapChecker {

    private TimeEntryOverlapChecker() {
    }

    public static boolean overlaps(TimeEntryService timeEntryService, UserLocalId ownerLocalId,
                                   @Nullable TimeEntryId excludedTimeEntryId, ZonedDateTime start,
                                   ZonedDateTime end, boolean isBreak) {

        final LocalDate fromDate = start.toLocalDate().minusDays(1);
        final LocalDate toDateExclusive = end.toLocalDate().plusDays(1);
        final List<TimeEntry> existingEntries = timeEntryService.getEntries(fromDate, toDateExclusive, ownerLocalId);

        return overlaps(existingEntries, excludedTimeEntryId, start, end, isBreak);
    }

    public static boolean overlaps(Collection<TimeEntry> existingEntries, @Nullable TimeEntryId excludedTimeEntryId,
                                   ZonedDateTime start, ZonedDateTime end, boolean isBreak) {
        return existingEntries.stream()
            .filter(entry -> !entry.id().equals(excludedTimeEntryId))
            .filter(entry -> entry.isBreak() == isBreak)
            .anyMatch(entry -> entry.start().isBefore(end) && entry.end().isAfter(start));
    }
}
