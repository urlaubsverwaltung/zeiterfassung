package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsDtoValidatorTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2025-05-30T12:00:00Z"), ZoneOffset.UTC);

    private SettingsDtoValidator sut;

    @Mock
    private AutomaticBreakDeductionSettingsService automaticBreakDeductionSettingsService;
    @Mock
    private UserSettingsProvider userSettingsProvider;

    private Errors errors;

    @BeforeEach
    void setUp() {
        sut = new SettingsDtoValidator(fixedClock, automaticBreakDeductionSettingsService, userSettingsProvider);
        errors = Mockito.mock(Errors.class);
    }

    @Test
    void ensureFederalStateMustNotBeNull() {

        final SettingsDto settingsDto = new SettingsDto(null, false, false, null, false, null, null, null, false);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("federalState", "jakarta.validation.constraints.NotNull.message");
    }

    @Test
    void ensureFederalStateValid() {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, null, false, null, null, null, false);
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesInPastMustBePositiveWhenFeatureIsEnabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, true, input, false, null, null, null, false);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesValidWhenFeatureDisabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, input, false, null, null, null, false);
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @Nested
    class SubtractBreaksFromOverlappingTimeEntries {

        @Test
        void ensureValidWhenFeatureDisabled() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                "30",
                false,
                null,
                null,
                null,
            false
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @Test
        void ensureInvalidWhenEnabledButNoDateProvided() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                "30",
                true,
                null,
                null,
                null,
            false
            );

            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("subtractBreakFromTimeEntryActiveDate", "settings.work-duration.calculation.subtract-breaks.date.validation.NotNull");
        }

        @Test
        void ensureValidWhenEnabledWithDate() {

            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                "30",
                true,
                LocalDate.now(),
                null,
                null,
            false
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }
    }

    @Nested
    class AutomaticBreakDeduction {

        private SettingsDto settingsDto(boolean active, LocalDate date) {
            return new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, null, null, null, active, date, false);
        }

        @Test
        void ensureValidWhenInactive() {
            sut.validate(settingsDto(false, LocalDate.parse("2025-05-01")), errors);
            verifyNoInteractions(errors);
        }

        @Test
        void ensureInvalidWhenActiveWithoutDate() {
            sut.validate(settingsDto(true, null), errors);
            verify(errors).rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.NotNull");
        }

        @Test
        void ensureInvalidWhenActiveDateIsInThePast() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);
            when(automaticBreakDeductionSettingsService.getAutomaticBreakDeductionSettings()).thenReturn(Optional.empty());

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-29")), errors);

            verify(errors).rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.past");
        }

        @Test
        void ensureInvalidWhenReactivatingWithPastDate() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);
            final Instant persisted = Instant.parse("2025-05-01T00:00:00Z");
            when(automaticBreakDeductionSettingsService.getAutomaticBreakDeductionSettings())
                .thenReturn(Optional.of(new AutomaticBreakDeductionSettings(false, Optional.of(persisted), false)));

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-01")), errors);

            verify(errors).rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.past");
        }

        @Test
        void ensureValidWhenActiveDeductionKeepsItsPastDate() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);
            final Instant persisted = Instant.parse("2025-05-01T00:00:00Z");
            when(automaticBreakDeductionSettingsService.getAutomaticBreakDeductionSettings())
                .thenReturn(Optional.of(new AutomaticBreakDeductionSettings(true, Optional.of(persisted), false)));

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-01")), errors);

            verifyNoInteractions(errors);
        }

        @Test
        void ensureInvalidWhenActiveDeductionIsMovedToAnotherPastDate() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);
            final Instant persisted = Instant.parse("2025-05-10T00:00:00Z");
            when(automaticBreakDeductionSettingsService.getAutomaticBreakDeductionSettings())
                .thenReturn(Optional.of(new AutomaticBreakDeductionSettings(true, Optional.of(persisted), false)));

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-01")), errors);

            verify(errors).rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.past");
        }

        @Test
        void ensureValidWhenActiveDateIsToday() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-30")), errors);

            verifyNoInteractions(errors);
        }

        @Test
        void ensureValidWhenActiveDateIsInTheFuture() {
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);

            sut.validate(settingsDto(true, LocalDate.parse("2025-05-31")), errors);

            verifyNoInteractions(errors);
        }
    }
}
