package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.workduration.WorkDuration;

import java.math.BigDecimal;

import static java.math.RoundingMode.CEILING;

public interface HasWorkedHoursRatio {

    /**
     * The calculated {@link WorkDuration}.
     *
     * <p>
     * See {@link de.focusshift.zeiterfassung.workduration.WorkDurationCalculationService} for more detail.
     *
     * @return calculated {@link WorkDuration}
     */
    WorkDuration workDuration();

    ShouldWorkingHours shouldWorkingHours();

    default BigDecimal workedHoursRatio() {

        final double worked = workDuration().durationInMinutes().toMinutes();
        if (worked == 0) {
            return BigDecimal.ZERO;
        }

        final double should = shouldWorkingHours().durationInMinutes().toMinutes();
        if (should == 0) {
            return BigDecimal.ONE;
        }

        final BigDecimal ratio = BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(should), 2, CEILING);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
    }
}
