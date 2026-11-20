package de.focusshift.zeiterfassung.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphWeekDtoTest {

    @ParameterizedTest
    @CsvSource({
        // single person: unchanged legend with a line every 2 hours (at least 8 hours without day reports)
        "0, 10, 2",
        "8, 10, 2",
        "9, 10, 2",
        "9.5, 12, 2",
        "18, 20, 2",
        // many persons: bigger steps keep the legend readable
        "40, 45, 5",
        "130, 140, 20",
        "2081.92, 2250, 250",
    })
    void ensureLegendMaxAndStep(double maxHours, double expectedMax, int expectedStep) {

        final GraphWeekDto sut = new GraphWeekDto(1, "", List.of(), maxHours, "", "", "", false, 0);

        assertThat(sut.graphLegendMaxHour()).isEqualTo(expectedMax);
        assertThat(sut.graphLegendStepHour()).isEqualTo(expectedStep);
        assertThat(sut.graphLegendMaxHour() / sut.graphLegendStepHour()).isLessThanOrEqualTo(10);
    }

    @Test
    void ensureMonthLegendUsesSameStep() {

        final GraphWeekDto week = new GraphWeekDto(1, "", List.of(), 130.0, "", "", "", false, 0);
        final GraphMonthDto sut = new GraphMonthDto("", List.of(week), 130.0, "", "", "", false, 0);

        assertThat(sut.graphLegendMaxHour()).isEqualTo(160);
        assertThat(sut.graphLegendStepHour()).isEqualTo(20);
    }
}
