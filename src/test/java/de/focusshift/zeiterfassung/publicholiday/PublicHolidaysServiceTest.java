package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.FINLAND;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.FINLAND_ALAND;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GLOBAL;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.ROMANIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicHolidaysServiceTest {

    private PublicHolidaysService sut;

    @Mock
    private FederalStateSettingsService federalStateSettingsService;

    @BeforeEach
    void setUp() {
        sut = new PublicHolidaysServiceImpl(
            Map.of(
                "de", getHolidayManager("de"),
                "fi", getHolidayManager("fi"),
                "ro", getHolidayManager("ro")
            ),
            federalStateSettingsService
        );
    }

    @Test
    void ensureGetPublicHolidays() {

        final List<FederalState> federalStates = List.of(NONE, GERMANY_BADEN_WUERTTEMBERG);
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, federalStates);

        assertThat(actual)
            .hasSize(2)
            .hasEntrySatisfying(NONE, this::assertNone)
            .hasEntrySatisfying(GERMANY_BADEN_WUERTTEMBERG, this::assertBadenWuerttemberg);

        verifyNoInteractions(federalStateSettingsService);
    }

    @Test
    void ensureGetPublicHolidaysWhenGlobalFederalStateIsNone() {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(NONE));

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(GLOBAL));

        assertThat(actual)
            .hasSize(1)
            .hasEntrySatisfying(NONE, this::assertNone);
    }

    @ParameterizedTest
    @EnumSource(value = FederalState.class, names = {"GERMANY_BADEN_WUERTTEMBERG"})
    void ensureGetPublicHolidaysWhenGlobalFederalStateIs(FederalState federalState) {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(federalState));

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(GLOBAL));

        assertThat(actual)
            .hasSize(1)
            .hasEntrySatisfying(GERMANY_BADEN_WUERTTEMBERG, this::assertBadenWuerttemberg);
    }

    @Test
    void ensureGetPublicHolidaysForFinland() {
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(FINLAND));

        assertThat(actual).hasSize(1).hasEntrySatisfying(FINLAND, this::assertFinland);
    }

    @Test
    void ensureGetPublicHolidaysForFinlandAland() {
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(FINLAND_ALAND));

        assertThat(actual).hasSize(1).hasEntrySatisfying(FINLAND_ALAND, calendar -> {
            assertThat(calendar.federalState()).isEqualTo(FINLAND_ALAND);
            assertThat(calendar.publicHolidays()).satisfies(map -> {
                assertThat(map).hasSize(16); // 15 national + Self-Governance Day (9 June)
                assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 9), anyDescription())));
            });
        });
    }

    @Test
    void ensureGetPublicHolidaysForRomania() {
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(ROMANIA));

        assertThat(actual).hasSize(1).hasEntrySatisfying(ROMANIA, this::assertRomania);
    }

    private void assertRomania(PublicHolidayCalendar publicHolidayCalendar) {
        // Orthodox Easter 2023 = April 16 (Julian calendar)
        assertThat(publicHolidayCalendar.federalState()).isEqualTo(ROMANIA);
        assertThat(publicHolidayCalendar.publicHolidays()).satisfies(map -> {
            assertThat(map).hasSize(15);
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 1), anyDescription())));  // New Year
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 2), anyDescription())));  // New Year (day 2)
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 24), anyDescription()))); // Unification Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 14), anyDescription()))); // Orthodox Good Friday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 16), anyDescription()))); // Orthodox Easter
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 17), anyDescription()))); // Orthodox Easter Monday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 1), anyDescription())));  // Labour Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 1), anyDescription())));  // Children's Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 4), anyDescription())));  // Orthodox Pentecost
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 5), anyDescription())));  // Orthodox Whit Monday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 8, 15), anyDescription()))); // Assumption (Navy Day)
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 11, 30), anyDescription()))); // St. Andrew
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 1), anyDescription())));  // National Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 25), anyDescription()))); // Christmas
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 26), anyDescription()))); // Christmas (day 2)
        });
    }

    private void assertFinland(PublicHolidayCalendar publicHolidayCalendar) {
        // Easter 2023 = April 9 (Gregorian)
        assertThat(publicHolidayCalendar.federalState()).isEqualTo(FINLAND);
        assertThat(publicHolidayCalendar.publicHolidays()).satisfies(map -> {
            assertThat(map).hasSize(15);
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 1), anyDescription())));  // New Year
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 1, 6), anyDescription())));  // Epiphany
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 7), anyDescription())));  // Good Friday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 9), anyDescription())));  // Easter
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 4, 10), anyDescription()))); // Easter Monday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 1), anyDescription())));  // Labour Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 18), anyDescription()))); // Ascension Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 5, 28), anyDescription()))); // Whit Sunday
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 23), anyDescription()))); // Midsummer Eve (Fri between 19-25 Jun)
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 6, 24), anyDescription()))); // Midsummer Day (Sat between 20-26 Jun)
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 11, 4), anyDescription()))); // All Saints (Sat between 31 Oct-6 Nov)
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 6), anyDescription())));  // Independence Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 24), anyDescription()))); // Christmas Eve
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 25), anyDescription()))); // Christmas Day
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 26), anyDescription()))); // St. Stephen's Day
        });
    }

    private void assertNone(PublicHolidayCalendar publicHolidayCalendar) {
        assertThat(publicHolidayCalendar.federalState()).isEqualTo(NONE);
        assertThat(publicHolidayCalendar.publicHolidays()).isEmpty();
    }

    private void assertBadenWuerttemberg(PublicHolidayCalendar publicHolidayCalendar) {
        assertThat(publicHolidayCalendar.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(publicHolidayCalendar.publicHolidays()).satisfies(map -> {
            assertThat(map).hasSize(12);
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
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 25), anyDescription())));
            assertThat(map).containsValue(List.of(new PublicHoliday(LocalDate.of(2023, 12, 26), anyDescription())));
        });
    }

    private FederalStateSettings federalStateSettings(FederalState globalFederalState) {
        return new FederalStateSettings(globalFederalState, false);
    }

    private HolidayManager getHolidayManager(String country) {
        return HolidayManager.getInstance(ManagerParameters.create(country));
    }

    private Function<Locale, String> anyDescription() {
        return locale -> "";
    }
}
