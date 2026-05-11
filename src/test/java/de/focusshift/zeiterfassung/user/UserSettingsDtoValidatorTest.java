package de.focusshift.zeiterfassung.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;

import java.util.Locale;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserSettingsDtoValidatorTest {

    private UserSettingsDtoValidator sut;

    @Mock
    private Errors errors;

    @BeforeEach
    void setUp() {
        sut = new UserSettingsDtoValidator();
    }

    @Test
    void ensureValidIfLocaleIsNotProvided() {
        final UserSettingsDto userSettingsDto = new UserSettingsDto(Theme.SYSTEM.name());
        sut.validate(userSettingsDto, errors);
        verifyNoInteractions(errors);
    }

    @Test
    void ensureThrowsErrorIfLocaleIsNotSupported() {
        final UserSettingsDto userSettingsDto = new UserSettingsDto(Theme.SYSTEM.name(), Locale.ITALIAN);
        sut.validate(userSettingsDto, errors);
        verify(errors).reject("Locale is not available");
    }

    @Test
    void ensureThrowsErrorIfThemeIsNotProvided() {
        final UserSettingsDto userSettingsDto = new UserSettingsDto(null, Locale.GERMAN);
        sut.validate(userSettingsDto, errors);
        verify(errors).reject("Theme is not available");
    }

    @Test
    void ensureThrowsErrorIfThemeIsNotSupported() {
        final UserSettingsDto userSettingsDto = new UserSettingsDto("someTheme", Locale.GERMAN);
        sut.validate(userSettingsDto, errors);
        verify(errors).reject("Theme is not available");
    }
}

