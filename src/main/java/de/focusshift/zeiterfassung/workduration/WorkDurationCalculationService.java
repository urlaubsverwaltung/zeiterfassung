package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkDurationCalculationService {

    private final WorkDurationSubtractBreakCalculationService subtractBreakCalculationService;

    WorkDurationCalculationService(WorkDurationSubtractBreakCalculationService subtractBreakCalculationService) {
        this.subtractBreakCalculationService = subtractBreakCalculationService;
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    public WorkDuration calculateWorkDuration(List<TimeEntry> timeEntries) {
        return subtractBreakCalculationService.calculateWorkDuration(timeEntries);
    }
}
