package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Service;

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

    private final SubtractOverlappingBreakCalculator subtractOverlappingBreaksCalculator;

    WorkDurationCalculationService(SubtractOverlappingBreakCalculator subtractOverlappingBreaksCalculator) {
        this.subtractOverlappingBreaksCalculator = subtractOverlappingBreaksCalculator;
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    public WorkDuration calculateWorkDuration(List<TimeEntry> timeEntries) {
        return subtractOverlappingBreaksCalculator.calculateWorkDuration(timeEntries);
    }
}
