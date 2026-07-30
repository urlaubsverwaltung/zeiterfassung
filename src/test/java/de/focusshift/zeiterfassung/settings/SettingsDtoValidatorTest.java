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

import java.time.LocalDate;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SettingsDtoValidatorTest {

    private SettingsDtoValidator sut;

    private Errors errors;

    @BeforeEach
    void setUp() {
        sut = new SettingsDtoValidator();
        errors = Mockito.mock(Errors.class);
    }

    @Test
    void ensureFederalStateMustNotBeNull() {

        final SettingsDto settingsDto = new SettingsDto(null, false, false, null, false, null, true, false, "45");
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("federalState", "jakarta.validation.constraints.NotNull.message");
    }

    @Test
    void ensureFederalStateValid() {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, null, false, null, true, false, "45");
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesInPastMustBePositiveWhenFeatureIsEnabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, true, input, false, null, true, false, "45");
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesValidWhenFeatureDisabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, false, input, false, null, true, false, "45");
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
                true,
                false,
                "45"
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
                true,
                false,
                "45"
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
                true,
                false,
                "45"
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }
    }

    @Nested
    class DefaultBreakMinutes {

        @Test
        void ensureValidWhenBreakIntegratedDisabledRegardlessOfMinutes() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                false,
                null,
                true,
                false,
                "not-a-number"
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "121", "not-a-number", ""})
        @NullSource
        void ensureInvalidWhenBreakIntegratedEnabledAndMinutesOutOfRange(String input) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                false,
                null,
                true,
                true,
                input
            );

            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("defaultBreakMinutes", "settings.time-entry.default-break-minutes.validation.range");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "45", "120"})
        void ensureValidWhenBreakIntegratedEnabledAndMinutesInRange(String input) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG,
                false,
                false,
                null,
                false,
                null,
                true,
                true,
                input
            );

            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }
    }
}
