package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettings;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    public WorkDuration calculateWorkDuration(List<TimeEntry> timeEntries) {
        // without given settings the current option is to subtract overlapping breaks
        return subtractOverlappingBreaksCalculator.calculateWorkDuration(timeEntries);
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    public WorkDuration calculateWorkDuration(SubtractBreakFromTimeEntrySettings settings, List<TimeEntry> timeEntries) {

        if (!settings.subtractBreakFromTimeEntryIsActive()) {
            return simpleWorkDurationCalculator.calculateWorkDuration(timeEntries);
        }

        final List<TimeEntry> timeEntriesBeforeEnabledFeature = new ArrayList<>();
        final List<TimeEntry> timeEntriesAfterEnabledFeature = new ArrayList<>();

        final Instant timestamp = settings.subtractBreakFromTimeEntryEnabledTimestamp();

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
