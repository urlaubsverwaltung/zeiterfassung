package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Default {@link WorkDuration} calculation simply summarizing the duration of {@link TimeEntry}.
 *
 * <p>
 * Overlapping entries are not handled, see other strategies for that.
 */
@Component
class SimpleWorkDurationCalculator implements WorkDurationCalculator {

    @Override
    public WorkDuration calculateWorkDuration(Collection<TimeEntry> timeEntries) {
        return timeEntries.stream().distinct()
            .map(TimeEntry::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }
}
