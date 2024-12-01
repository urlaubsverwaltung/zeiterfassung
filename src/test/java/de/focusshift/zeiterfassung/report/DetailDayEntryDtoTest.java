package de.focusshift.zeiterfassung.report;


import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class DetailDayEntryDtoTest {

    @Test
    void durationSameDay() {
        DetailDayEntryDto detailDayEntryDto = new DetailDayEntryDto(null, null, null, false, LocalTime.of(0, 0), LocalTime.of(1, 0));
        assertThat(detailDayEntryDto.getDuration()).isEqualTo(Duration.ofHours(1L));
    }

    @Test
    void durationWithEndNextDay() {
        DetailDayEntryDto detailDayEntryDto = new DetailDayEntryDto(null, null, null, false, LocalTime.of(23, 0), LocalTime.of(1, 0));
        assertThat(detailDayEntryDto.getDuration()).isEqualTo(Duration.ofHours(2L));
    }
}
