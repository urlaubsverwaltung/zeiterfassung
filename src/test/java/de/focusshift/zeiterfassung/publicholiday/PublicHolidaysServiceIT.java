package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.assertj.core.api.Assertions.assertThat;

class PublicHolidaysServiceIT {

    private PublicHolidaysService sut;

    @BeforeEach
    void setUp() {
        sut = new PublicHolidaysServiceImpl(Map.of("de", getHolidayManager("de")));
    }

    @Test
    void ensureGetPublicHolidays() {

        final List<FederalState> federalStates = List.of(GERMANY_BADEN_WUERTTEMBERG);
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, federalStates);

        assertThat(actual).hasEntrySatisfying(GERMANY_BADEN_WUERTTEMBERG, publicHolidayCalendar -> {
            assertThat(publicHolidayCalendar.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
            assertThat(publicHolidayCalendar.publicHolidays()).satisfies(map -> {
                assertThat(map).hasSize(14);
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 6), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 1), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 7), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 10), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 1), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 18), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 29), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 8), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 10, 3), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 11, 1), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 24), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 25), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 26), anyDescription())));
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 31), anyDescription())));
            });
        });
    }

    private HolidayManager getHolidayManager(String country) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final URL url = cl.getResource("Holidays_%s.xml".formatted(country));
        return HolidayManager.getInstance(ManagerParameters.create(url));
    }

    private Function<Locale, String> anyDescription() {
        return locale -> "";
    }
}
