package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Optional;

import static de.focusshift.zeiterfassung.user.Theme.LIGHT;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    private UserSettingsService sut;

    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private LocaleResolver localeResolver;

    @BeforeEach
    void setUp() {
        sut = new UserSettingsService(userSettingsRepository, localeResolver);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    class GetUserSettings {

        @Test
        void ensureUserSettingsForPerson() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            final UserSettingsEntity entity = new UserSettingsEntity();
            entity.setTenantUserLocalId(localId);
            entity.setTheme(Theme.DARK);
            entity.setLocale(GERMAN);

            when(userSettingsRepository.findById(localId)).thenReturn(Optional.of(entity));

            final UserSettings actual = sut.getUserSettings(userIdComposite);

            assertThat(actual.theme()).isEqualTo(Theme.DARK);
            assertThat(actual.locale()).hasValue(GERMAN);
        }

        @Test
        void ensureUserSettingsForPersonReturnsDefault() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            when(userSettingsRepository.findById(localId)).thenReturn(Optional.empty());

            final UserSettings actual = sut.getUserSettings(userIdComposite);

            assertThat(actual.theme()).isEqualTo(Theme.SYSTEM);
            assertThat(actual.locale()).isEmpty();
        }
    }

    @Nested
    class UpdateUserPreferences {

        @Test
        void ensureUpdateUserPreference() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            final UserSettingsEntity entity = new UserSettingsEntity();
            entity.setTenantUserLocalId(localId);
            entity.setTheme(Theme.DARK);
            entity.setLocale(null);
            entity.setLocaleBrowserSpecific(ENGLISH);

            when(userSettingsRepository.findById(localId)).thenReturn(Optional.of(entity));

            final UserSettingsEntity entityToSave = new UserSettingsEntity();
            entityToSave.setTenantUserLocalId(localId);
            entityToSave.setTheme(LIGHT);
            entityToSave.setLocale(GERMAN);

            when(userSettingsRepository.save(entityToSave)).thenReturn(entityToSave);

            final UserSettings updatedUserSettings = sut.updateUserPreference(userIdComposite, LIGHT, GERMAN);
            assertThat(updatedUserSettings.theme()).isEqualTo(LIGHT);
            assertThat(updatedUserSettings.locale()).hasValue(GERMAN);
            assertThat(updatedUserSettings.localeBrowserSpecific()).isEmpty();

            final ArgumentCaptor<UserSettingsEntity> entityArgumentCaptor = ArgumentCaptor.forClass(UserSettingsEntity.class);
            verify(userSettingsRepository).save(entityArgumentCaptor.capture());
            assertThat(entityArgumentCaptor.getValue())
                .satisfies(userSettingsEntity -> {
                    assertThat(userSettingsEntity.getTheme()).isEqualTo(LIGHT);
                    assertThat(userSettingsEntity.getLocale()).isEqualTo(GERMAN);
                    assertThat(userSettingsEntity.getLocaleBrowserSpecific()).isNull();
                });
        }

        @Test
        void ensureUpdateUserPreferenceUsesBrowserSpecificLocaleLocaleWillBeNull() {

            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addPreferredLocale(GERMAN);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            final UserSettingsEntity entity = new UserSettingsEntity();
            entity.setTenantUserLocalId(localId);
            entity.setTheme(Theme.DARK);
            entity.setLocale(ENGLISH);
            entity.setLocaleBrowserSpecific(null);

            when(userSettingsRepository.findById(localId)).thenReturn(Optional.of(entity));

            final UserSettingsEntity entityToSave = new UserSettingsEntity();
            entityToSave.setTenantUserLocalId(localId);
            entityToSave.setTheme(LIGHT);
            entityToSave.setLocale(null);
            entityToSave.setLocaleBrowserSpecific(GERMAN);

            when(userSettingsRepository.save(entityToSave)).thenReturn(entityToSave);

            final UserSettings updatedUserSettings = sut.updateUserPreference(userIdComposite, LIGHT, null);
            assertThat(updatedUserSettings.theme()).isEqualTo(LIGHT);
            assertThat(updatedUserSettings.locale()).isEmpty();
            assertThat(updatedUserSettings.localeBrowserSpecific()).hasValue(GERMAN);

            final ArgumentCaptor<UserSettingsEntity> entityArgumentCaptor = ArgumentCaptor.forClass(UserSettingsEntity.class);
            verify(userSettingsRepository).save(entityArgumentCaptor.capture());
            assertThat(entityArgumentCaptor.getValue())
                .satisfies(userSettingsEntity -> {
                    assertThat(userSettingsEntity.getTheme()).isEqualTo(LIGHT);
                    assertThat(userSettingsEntity.getLocale()).isNull();
                    assertThat(userSettingsEntity.getLocaleBrowserSpecific()).isEqualTo(GERMAN);
                });
        }

        @Test
        void ensureUpdateUserPreferenceWhenNothingHasBeenPersistedYet() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            when(userSettingsRepository.findById(localId)).thenReturn(Optional.empty());

            final UserSettingsEntity entityToSave = new UserSettingsEntity();
            entityToSave.setTenantUserLocalId(localId);
            entityToSave.setTheme(LIGHT);
            entityToSave.setLocale(GERMAN);

            when(userSettingsRepository.save(entityToSave)).thenReturn(entityToSave);

            final UserSettings updatedUserSettings = sut.updateUserPreference(userIdComposite, LIGHT, GERMAN);
            assertThat(updatedUserSettings.theme()).isEqualTo(LIGHT);

            final ArgumentCaptor<UserSettingsEntity> entityArgumentCaptor = ArgumentCaptor.forClass(UserSettingsEntity.class);
            verify(userSettingsRepository).save(entityArgumentCaptor.capture());
            assertThat(entityArgumentCaptor.getValue())
                .satisfies(userSettingsEntity -> {
                    assertThat(userSettingsEntity.getTheme()).isEqualTo(LIGHT);
                    assertThat(userSettingsEntity.getLocale()).isEqualTo(GERMAN);
                    assertThat(userSettingsEntity.getLocaleBrowserSpecific()).isNull();
                });
        }
    }

    @Nested
    class FindTheme {

        @Test
        void ensureFindThemeForUsernameReturnsEmptyOptionalWhenUsernameIsUnknown() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            when(userSettingsRepository.findByTenantUserLocalId(localId)).thenReturn(Optional.empty());

            final Optional<Theme> actual = sut.findTheme(userIdComposite);
            assertThat(actual).isEmpty();
        }

        @Test
        void ensureFindThemeForUsernameReturnsEmptyOptionalWhenThereIsNoLocale() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            final UserSettingsEntity entity = new UserSettingsEntity();
            entity.setTheme(null);

            when(userSettingsRepository.findByTenantUserLocalId(localId)).thenReturn(Optional.of(entity));

            final Optional<Theme> actual = sut.findTheme(userIdComposite);
            assertThat(actual).isEmpty();
        }

        @Test
        void ensureFindThemeForUsernameReturnsLocale() {
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final Long localId = userIdComposite.localId().value();

            final UserSettingsEntity entity = new UserSettingsEntity();
            entity.setTheme(Theme.DARK);

            when(userSettingsRepository.findByTenantUserLocalId(localId)).thenReturn(Optional.of(entity));

            final Optional<Theme> actual = sut.findTheme(userIdComposite);
            assertThat(actual).hasValue(Theme.DARK);
        }
    }


    private static UserIdComposite anyUserIdComposite() {
        return anyUserIdComposite(new UserId("uuid"));
    }

    private static UserIdComposite anyUserIdComposite(UserId userId) {
        return new UserIdComposite(userId, new UserLocalId(1L));
    }
}

