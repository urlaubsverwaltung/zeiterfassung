package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest implements ControllerTest {

    private SettingsController sut;

    @Mock
    private SettingsService settingsService;
    @Mock
    private SettingsDtoValidator settingsDtoValidator;
    @Mock
    private UserSettingsProvider userSettingsProvider;

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2025-05-30T22:00:00.000Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        sut = new SettingsController(settingsService, settingsDtoValidator, userSettingsProvider, fixedClock);
    }

    @Test
    void ensureGetSettings() throws Exception {

        final FederalStateSettings federalStateSettings = new FederalStateSettings(FederalState.NONE, false);
        final LockTimeEntriesSettings lockTimeEntriesSettings = new LockTimeEntriesSettings(true, 42);

        when(settingsService.getFederalStateSettings()).thenReturn(federalStateSettings);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        final SettingsDto expectedSettingsDto = new SettingsDto(FederalState.NONE, false, true, "42");

        perform(get("/settings"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("settings", expectedSettingsDto));
    }

    @Test
    void ensureGetSettingsWithCorrectExampleLockTimeEntriesTextDate() throws Exception {

        final Locale locale = Locale.GERMAN;

        final FederalStateSettings federalStateSettings = new FederalStateSettings(FederalState.NONE, false);
        final LockTimeEntriesSettings lockTimeEntriesSettings = new LockTimeEntriesSettings(true, 3);

        when(settingsService.getFederalStateSettings()).thenReturn(federalStateSettings);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        perform(get("/settings").locale(locale))
            .andExpect(model().attribute("timeslotLockedExampleDate", "Dienstag, 27.05.2025"));
    }

    @Test
    void ensureGetSettingsWithDisabledLockTimeEntriesSettings() throws Exception {

        final FederalStateSettings federalStateSettings = new FederalStateSettings(FederalState.NONE, false);
        final LockTimeEntriesSettings lockTimeEntriesSettings = new LockTimeEntriesSettings(false, -1);

        when(settingsService.getFederalStateSettings()).thenReturn(federalStateSettings);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        final SettingsDto expectedSettingsDto = new SettingsDto(FederalState.NONE, false, false, null);

        perform(get("/settings"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("settings", expectedSettingsDto));
    }

    @Test
    void ensureUpdateSettingsValidates() throws Exception {

        final SettingsDto dto = new SettingsDto(FederalState.NONE, false, true, "-1");

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        doAnswer(invocationOnMock -> {
            final BindingResult errors = invocationOnMock.getArgument(1, BindingResult.class);
            errors.reject("something.is.fishy");
            return null;
        }).when(settingsDtoValidator).validate(eq(dto), any(BindingResult.class));

        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "true")
            .param("lockTimeEntriesDaysInPast", "-1")
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(view().name("settings/settings"));

        verifyNoInteractions(settingsService);
    }

    @Test
    void ensureUpdateSettingsPreviewDoesNotPersist() throws Exception {

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        perform(post("/settings")
            .param("preview", "")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "true")
            .param("lockTimeEntriesDaysInPast", "42")
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(view().name("settings/settings"));

        verifyNoInteractions(settingsService);
    }

    @Test
    void ensureUpdateSettingsRedirectsToSettings() throws Exception {

        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "true")
            .param("lockTimeEntriesDaysInPast", "42")
        )
            .andExpect(flash().attributeCount(0))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        verify(settingsService).updateFederalStateSettings(FederalState.NONE, false);
        verify(settingsService).updateLockTimeEntriesSettings(true, 42);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1"})
    void ensureUpdateLockTimeSettingsWithDisabledLockingAndZeroOrPositiveDaysInPast(String daysInPast) throws Exception {

        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "false")
            .param("lockTimeEntriesDaysInPast", daysInPast)
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        verify(settingsService).updateLockTimeEntriesSettings(false, Integer.parseInt(daysInPast));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
