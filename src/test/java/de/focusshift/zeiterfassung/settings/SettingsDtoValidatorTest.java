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
import java.util.List;

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

        final SettingsDto settingsDto = new SettingsDto(null, false, null, null, null, null, false, null, false, null, null);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("federalState", "jakarta.validation.constraints.NotNull.message");
    }

    @Test
    void ensureFederalStateValid() {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, null, false, null, null);
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesInPastMustBePositiveWhenFeatureIsEnabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, true, input, false, null, null);
        sut.validate(settingsDto, errors);

        verify(errors).rejectValue("lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "not-a-number"})
    @NullSource
    void ensureLockTimeEntriesValidWhenFeatureDisabled(String input) {

        final SettingsDto settingsDto = new SettingsDto(GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, input, false, null, null);
        sut.validate(settingsDto, errors);

        verifyNoInteractions(errors);
    }

    @Nested
    class SubtractBreaksFromOverlappingTimeEntries {

        @Test
        void ensureValidWhenFeatureDisabled() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, "30", false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @Test
        void ensureInvalidWhenEnabledButNoDateProvided() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, "30", true, null, null
            );
            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("subtractBreakFromTimeEntryActiveDate", "settings.work-duration.calculation.subtract-breaks.date.validation.NotNull");
        }

        @Test
        void ensureValidWhenEnabledWithDate() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, "30", true, LocalDate.now(), null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }
    }

    @Nested
    class WorkingTimeHours {

        @Test
        void ensureWorkingTimeIsValidWhenNull() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, List.of("monday"), null, null, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.5, 8.0, 7.5, 24.0})
        void ensureWorkingTimeIsValidWithinRange(double hours) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, List.of("monday"), hours, null, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.1, -1.0, 24.1, 25.0})
        void ensureWorkingTimeIsInvalidOutsideRange(double hours) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, List.of("monday"), hours, null, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("workingTime", "settings.working-time.validation.range");
        }
    }

    @Nested
    class TimeRoundingMinutes {

        @Test
        void ensureValidWhenNull() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 15, 30, 60})
        void ensureValidWithinRange(int rounding) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, rounding, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 61, 100})
        void ensureInvalidOutsideRange(int rounding) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, rounding, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("timeRoundingMinutes", "settings.time-rounding-minutes.validation.range");
        }
    }

    @Nested
    class MinSuggestedMinutes {

        @Test
        void ensureValidWhenNull() {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, null, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 15, 60, 480})
        void ensureValidWithinRange(int minMinutes) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, minMinutes, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verifyNoInteractions(errors);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 481, 600})
        void ensureInvalidOutsideRange(int minMinutes) {
            final SettingsDto settingsDto = new SettingsDto(
                GERMANY_BADEN_WUERTTEMBERG, false, null, null, null, minMinutes, false, null, false, null, null
            );
            sut.validate(settingsDto, errors);
            verify(errors).rejectValue("minSuggestedMinutes", "settings.min-suggested-minutes.validation.range");
        }
    }
}
