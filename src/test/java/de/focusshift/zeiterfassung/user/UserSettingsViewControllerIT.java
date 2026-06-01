package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.Errors;

import java.util.Locale;
import java.util.Set;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserSettingsViewControllerTest implements ControllerTest {

    private UserSettingsViewController sut;

    @Mock
    private UserSettingsService userSettingsService;
    @Mock
    private SupportedLocaleService supportedLocaleService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private UserSettingsDtoValidator userSettingsDtoValidator;
    @Mock
    private UserSearchViewHelper userSearchViewHelper;

    @BeforeEach
    void setUp() {
        sut = new UserSettingsViewController(userSettingsService, supportedLocaleService, messageSource, userSettingsDtoValidator, userSearchViewHelper);
    }

    @Test
    void ensureGetUserSettings() throws Exception {

        final UserIdComposite userIdComposite = anyUserIdComposite();

        final UserSettings userSettings = new UserSettings(Theme.DARK, Locale.GERMAN, null, null, false, null);
        when(userSettingsService.getUserSettings(userIdComposite)).thenReturn(userSettings);

        when(supportedLocaleService.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN, Locale.ENGLISH));

        when(messageSource.getMessage("user-settings.theme.DARK", new Object[]{}, Locale.GERMAN)).thenReturn("dark-label");
        when(messageSource.getMessage("user-settings.theme.LIGHT", new Object[]{}, Locale.GERMAN)).thenReturn("light-label");
        when(messageSource.getMessage("user-settings.theme.SYSTEM", new Object[]{}, Locale.GERMAN)).thenReturn("system-label");
        when(messageSource.getMessage("locale", new Object[]{}, Locale.GERMAN)).thenReturn("Deutsch");
        when(messageSource.getMessage("locale", new Object[]{}, Locale.ENGLISH)).thenReturn("English");

        perform(get("/personal-settings").with(oidcSubject(userIdComposite)).locale(Locale.GERMAN))
            .andExpect(status().isOk())
            .andExpect(model().attribute("supportedLocales",
                hasItems(
                    allOf(
                        hasProperty("locale", is(Locale.GERMAN)),
                        hasProperty("displayName", is("Deutsch"))
                    ),
                    allOf(
                        hasProperty("locale", is(Locale.ENGLISH)),
                        hasProperty("displayName", is("English"))
                    )
                )
            ))
            .andExpect(model().attribute("userSettings", new UserSettingsDto("DARK", Locale.GERMAN)))
            .andExpect(model().attribute("supportedThemes", contains(
                new ThemeDto("SYSTEM", "system-label"),
                new ThemeDto("LIGHT", "light-label"),
                new ThemeDto("DARK", "dark-label")
            )));
    }

    @ParameterizedTest
    @EnumSource(value = Theme.class)
    void ensureUpdateUserSettings(Theme givenTheme) throws Exception {
        final UserIdComposite userIdComposite = anyUserIdComposite();

        perform(post("/personal-settings").with(oidcSubject(userIdComposite)).locale(Locale.GERMANY)
            .param("theme", givenTheme.name())
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/personal-settings"));
    }

    @Test
    void ensureUpdateUserSettingsThrowsWhenThemeNameIsUnknown() throws Exception {
        final UserIdComposite userIdComposite = anyUserIdComposite();

        perform(post("/personal-settings").with(oidcSubject(userIdComposite)).locale(Locale.GERMANY)
            .param("theme", "UNKNOWN_THEME")
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    void ensureUpdateUserSettingsWithErrorOpensThePageAgainAndDoesNotSave() throws Exception {
        final UserIdComposite userIdComposite = anyUserIdComposite();

        doAnswer(invocation -> {
            final Errors errors = invocation.getArgument(1);
            errors.reject("errors");
            return null;
        }).when(userSettingsDtoValidator).validate(any(), any());

        when(supportedLocaleService.getSupportedLocales()).thenReturn(Set.of(Locale.GERMAN));

        perform(post("/personal-settings").with(oidcSubject(userIdComposite)).locale(Locale.ITALIAN)
            .param("theme", "someTheme")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("supportedThemes", contains(
                new ThemeDto("SYSTEM", null),
                new ThemeDto("LIGHT", null),
                new ThemeDto("DARK", null)
            )))
            .andExpect(model().attribute("supportedLocales", hasItem(hasProperty("locale", is(Locale.GERMAN)))));

        verify(userSettingsService, never()).updateUserPreference(any(), any(), any());
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
