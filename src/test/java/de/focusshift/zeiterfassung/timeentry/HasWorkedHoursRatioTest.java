package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.workduration.WorkDuration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static java.math.RoundingMode.CEILING;
import static org.assertj.core.api.Assertions.assertThat;

class HasWorkedHoursRatioTest {

    @Test
    void ensureWorkedHoursRatioIsZeroWhenWorkedDurationIsZero() {
        final HasWorkedHoursRatio sut = hasWorkedHoursRatio(WorkDuration.ZERO, ShouldWorkingHours.EIGHT);
        assertThat(sut.workedHoursRatio()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void ensureWorkedHoursRatioIsOneWhenShouldWorkingHoursIsZero() {
        final HasWorkedHoursRatio sut = hasWorkedHoursRatio(WorkDuration.EIGHT, ShouldWorkingHours.ZERO);
        assertThat(sut.workedHoursRatio()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void ensureWorkedHoursRatio() {
        final HasWorkedHoursRatio sut = hasWorkedHoursRatio(new WorkDuration(Duration.ofHours(4)), ShouldWorkingHours.EIGHT);
        assertThat(sut.workedHoursRatio()).isEqualTo(BigDecimal.valueOf(0.5).setScale(2, CEILING));
    }

    private HasWorkedHoursRatio hasWorkedHoursRatio(WorkDuration workDuration, ShouldWorkingHours shouldWorkingHours) {
        return new HasWorkedHoursRatio() {

            @Override
            public WorkDuration workDuration() {
                return workDuration;
            }

            @Override
            public ShouldWorkingHours shouldWorkingHours() {
                return shouldWorkingHours;
            }
        };
    }
}
