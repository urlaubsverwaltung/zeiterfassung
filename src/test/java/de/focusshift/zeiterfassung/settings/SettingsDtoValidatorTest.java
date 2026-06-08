package de.focusshift.zeiterfassung.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SettingsDtoValidatorTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2025-05-30T12:00:00Z"), ZoneOffset.UTC);

    private SettingsDtoValidator sut;

    private Errors errors;

    @BeforeEach
    void setUp() {
        sut = new SettingsDtoValidator(fixedClock);
        errors = Mockito.mock(Errors.class);
    }

    @Test
    void ensureFederalStateMustNotBeNull() {

        final SettingsDto settingsDto = new SettingsDto(null, false, false, null, false, null, null, null);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("federalState", "jakarta.validation.constraints.NotNull.message");
    }

    @Test
    void ensureFederalStateValid() {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, null, false, null, null, null);
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesInPastMustBePositiveWhenFeatureIsEnabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, true, input, false, null, null, null);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesValidWhenFeatureDisabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, input, false, null, null, null);
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
                null
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
                null
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
                null
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }
    }

    @Nested
    class AutomaticBreakDeduction {

        @Test
        void ensureInvalidWhenActiveDateIsInThePast() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                null,
                null,
                true,
                LocalDate.parse("2025-05-29")
            );

            sut.validate(settingsDto, errors);

            verify(errors).rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.past");
        }

        @Test
        void ensureValidWhenActiveDateIsToday() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                null,
                null,
                true,
                LocalDate.parse("2025-05-30")
            );

            sut.validate(settingsDto, errors);

            verifyNoInteractions(errors);
        }

        @Test
        void ensureValidWhenActiveDateIsInTheFuture() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                null,
                null,
                true,
                LocalDate.parse("2025-05-31")
            );

            sut.validate(settingsDto, errors);

            verifyNoInteractions(errors);
        }
    }
}
