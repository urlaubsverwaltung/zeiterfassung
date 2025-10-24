package de.focusshift.zeiterfassung.timeentry;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class WorkDurationCalculationService {

    private final WorkDurationSubtractBreakCalculationService subtractBreakCalculationService;

    WorkDurationCalculationService(WorkDurationSubtractBreakCalculationService subtractBreakCalculationService) {
        this.subtractBreakCalculationService = subtractBreakCalculationService;
    }

    /**
     * Delegates calculation of {@link WorkDuration} for the given list of {@link TimeEntry}.
     *
     * @param timeEntries list of {@link TimeEntry} to calculate the {@link WorkDuration} for
     */
    WorkDuration calculateWorkDuration(List<TimeEntry> timeEntries) {
        return subtractBreakCalculationService.calculateWorkDuration(timeEntries);
    }
}
