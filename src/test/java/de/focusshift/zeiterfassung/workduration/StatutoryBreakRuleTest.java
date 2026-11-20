package de.focusshift.zeiterfassung.workduration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class StatutoryBreakRuleTest {

    private final StatutoryBreakRule sut = new StatutoryBreakRule();

    @ParameterizedTest
    @CsvSource({
        "0,   0",
        "360, 0",
        "361, 30",
        "540, 30",
        "541, 45",
    })
    void calculate_returnsStatutoryBreak(long grossMinutes, long expectedBreakMinutes) {
        final Duration grossWorkingTime = Duration.ofMinutes(grossMinutes);
        final Duration result = sut.calculate(grossWorkingTime);
        assertThat(result).isEqualTo(Duration.ofMinutes(expectedBreakMinutes));
    }
}
