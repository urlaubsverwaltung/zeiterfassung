package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettings;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Calculates the {@link WorkDuration} of a {@link TimeEntry} collection based on a strategy that has been chosen
 * by the user.
 *
 * <p>
 * For instance, a break can overlap a timeEntry which can be considered to be subtracted or not for
 * the calculated duration value.
 */
@Service
public class WorkDurationCalculationService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final SimpleWorkDurationCalculator simpleWorkDurationCalculator;
    private final OverlappingBreakCalculator subtractOverlappingBreaksCalculator;

    WorkDurationCalculationService(
        SimpleWorkDurationCalculator simpleWorkDurationCalculator,
        OverlappingBreakCalculator subtractOverlappingBreaksCalculator
    ) {
        this.simpleWorkDurationCalculator = simpleWorkDurationCalculator;
        this.subtractOverlappingBreaksCalculator = subtractOverlappingBreaksCalculator;
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    public WorkDuration calculateWorkDuration(Collection<TimeEntry> timeEntries) {
        // without given settings the current option is to subtract overlapping breaks
        return subtractOverlappingBreaksCalculator.calculateWorkDuration(timeEntries);
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    public WorkDuration calculateWorkDuration(SubtractBreakFromTimeEntrySettings settings, Collection<TimeEntry> timeEntries) {

        if (!settings.subtractBreakFromTimeEntryIsActive()) {
            return simpleWorkDurationCalculator.calculateWorkDuration(timeEntries);
        }

        final Optional<Instant> maybeTimestamp = settings.subtractBreakFromTimeEntryEnabledTimestamp();
        if (maybeTimestamp.isEmpty()) {
            LOG.error("Expected subtractBreakFromTimeEntry timestamp to exist, but doesn't. Falling back to SimpleWorkDuration calculation.");
            return simpleWorkDurationCalculator.calculateWorkDuration(timeEntries);
        }

        final Instant timestamp = maybeTimestamp.get();

        final List<TimeEntry> timeEntriesBeforeEnabledFeature = new ArrayList<>();
        final List<TimeEntry> timeEntriesAfterEnabledFeature = new ArrayList<>();

        for (TimeEntry timeEntry : timeEntries) {
            if (timeEntry.start().toInstant().isBefore(timestamp)) {
                timeEntriesBeforeEnabledFeature.add(timeEntry);
            } else {
                timeEntriesAfterEnabledFeature.add(timeEntry);
            }
        }

        final WorkDuration workDuration1 = timeEntriesBeforeEnabledFeature.isEmpty()
            ? WorkDuration.ZERO
            : simpleWorkDurationCalculator.calculateWorkDuration(timeEntriesBeforeEnabledFeature);

        final WorkDuration workDuration2 = timeEntriesAfterEnabledFeature.isEmpty()
            ? WorkDuration.ZERO
            : subtractOverlappingBreaksCalculator.calculateWorkDuration(timeEntriesAfterEnabledFeature);

        return workDuration1.plus(workDuration2);
    }
}
