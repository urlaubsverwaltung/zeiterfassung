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

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GLOBAL;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
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
        sut = new PublicHolidaysServiceImpl(Map.of("de", getHolidayManager("de")), federalStateSettingsService);
    }

    @Test
    void ensureGetPublicHolidays() {

        final List<FederalState> federalStates = List.of(NONE, GERMANY_BADEN_WUERTTEMBERG);
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, federalStates);

        assertThat(actual).hasSize(2);
        assertThat(actual).hasEntrySatisfying(NONE, this::assertNone);
        assertThat(actual).hasEntrySatisfying(GERMANY_BADEN_WUERTTEMBERG, this::assertBadenWuerttemberg);

        verifyNoInteractions(federalStateSettingsService);
    }

    @Test
    void ensureGetPublicHolidaysWhenGlobalFederalStateIsNone() {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(NONE));

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(GLOBAL));

        assertThat(actual).hasSize(1);
        assertThat(actual).hasEntrySatisfying(NONE, this::assertNone);
    }

    @ParameterizedTest
    @EnumSource(value = FederalState.class, names = {"GERMANY_BADEN_WUERTTEMBERG"})
    void ensureGetPublicHolidaysWhenGlobalFederalStateIs(FederalState federalState) {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(federalState));

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 1, 1);

        final Map<FederalState, PublicHolidayCalendar> actual = sut.getPublicHolidays(from, toExclusive, List.of(GLOBAL));

        assertThat(actual).hasSize(1);
        assertThat(actual).hasEntrySatisfying(GERMANY_BADEN_WUERTTEMBERG, this::assertBadenWuerttemberg);
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
