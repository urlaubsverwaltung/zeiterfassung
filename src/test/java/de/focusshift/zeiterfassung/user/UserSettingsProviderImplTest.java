package de.focusshift.zeiterfassung.user;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

class UserSettingsProviderImplTest {

    private final UserSettingsProviderImpl sut = new UserSettingsProviderImpl();

    @Test
    void ensureFirstDayOfWeekReturnsMonday() {
        assertThat(sut.firstDayOfWeek()).isEqualTo(MONDAY);
    }

    @Test
    void ensureZoneIdIsBerlin() {
        assertThat(sut.zoneId()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }
}
