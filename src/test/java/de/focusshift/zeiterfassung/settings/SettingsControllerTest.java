package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.settings.WorkingTimeSettings;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
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
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
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
    @Mock
    private UserSearchViewHelper userSearchViewHelper;

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2025-05-30T22:00:00.000Z"), UTC);

    @BeforeEach
    void setUp() {
        sut = new SettingsController(settingsService, settingsDtoValidator, userSettingsProvider, userSearchViewHelper, fixedClock);
    }

    @Test
    void ensureGetSettings() throws Exception {

        final FederalStateSettings federalStateSettings = new FederalStateSettings(FederalState.NONE, false);
        final LockTimeEntriesSettings lockTimeEntriesSettings = new LockTimeEntriesSettings(true, 42);

        final Instant subtractBreakFeatureTimestamp = Instant.now();
        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        final LocalDate subtractBreakFeatureDate = subtractBreakFeatureTimestamp.atZone(berlin).toLocalDate();
        final SubtractBreakFromTimeEntrySettings subtractBreakFromTimeEntrySettings =
            new SubtractBreakFromTimeEntrySettings(true, Optional.of(subtractBreakFeatureTimestamp));

        when(settingsService.getFederalStateSettings()).thenReturn(federalStateSettings);
        when(settingsService.getWorkingTimeSettings()).thenReturn(WorkingTimeSettings.DEFAULT);
        when(settingsService.getCategorisationSettings()).thenReturn(de.focusshift.zeiterfassung.settings.CategorisationSettings.DEFAULT);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
        when(settingsService.getSubtractBreakFromTimeEntrySettings()).thenReturn(Optional.of(subtractBreakFromTimeEntrySettings));
        when(settingsService.getOooCalendarSettings()).thenReturn(OooCalendarSettings.DEFAULT);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        final SettingsDto expectedSettingsDto = new SettingsDto(
            FederalState.NONE,
            false,
            List.of("monday", "tuesday", "wednesday", "thursday", "friday"),
            8.0,
            5,
            15,
            false,
            false,
            true,
            "42",
            true,
            subtractBreakFeatureDate,
            null
        );

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
        when(settingsService.getWorkingTimeSettings()).thenReturn(WorkingTimeSettings.DEFAULT);
        when(settingsService.getCategorisationSettings()).thenReturn(de.focusshift.zeiterfassung.settings.CategorisationSettings.DEFAULT);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
        when(settingsService.getSubtractBreakFromTimeEntrySettings()).thenReturn(Optional.empty());
        when(settingsService.getOooCalendarSettings()).thenReturn(OooCalendarSettings.DEFAULT);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        perform(get("/settings").locale(locale))
            .andExpect(model().attribute("timeslotLockedExampleDate", "Dienstag, 27.05.2025"));
    }

    @Test
    void ensureGetSettingsWithDisabledLockTimeEntriesSettings() throws Exception {

        final FederalStateSettings federalStateSettings = new FederalStateSettings(FederalState.NONE, false);
        final LockTimeEntriesSettings lockTimeEntriesSettings = new LockTimeEntriesSettings(false, -1);

        when(settingsService.getFederalStateSettings()).thenReturn(federalStateSettings);
        when(settingsService.getWorkingTimeSettings()).thenReturn(WorkingTimeSettings.DEFAULT);
        when(settingsService.getCategorisationSettings()).thenReturn(de.focusshift.zeiterfassung.settings.CategorisationSettings.DEFAULT);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
        when(settingsService.getSubtractBreakFromTimeEntrySettings()).thenReturn(Optional.empty());
        when(settingsService.getOooCalendarSettings()).thenReturn(OooCalendarSettings.DEFAULT);
        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        final SettingsDto expectedSettingsDto = new SettingsDto(
            FederalState.NONE,
            false,
            List.of("monday", "tuesday", "wednesday", "thursday", "friday"),
            8.0,
            5,
            15,
            false,
            false,
            false,
            null,
            null,
            null,
            null
        );

        perform(get("/settings"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("settings", expectedSettingsDto));
    }

    @Test
    void ensureUpdateSettingsValidates() throws Exception {

        final SettingsDto dto = new SettingsDto(
            FederalState.NONE,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            "-1",
            false,
            null,
            null
        );

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
            .param("subtractBreakFromTimeEntryIsActive", "false")
        )
            .andExpect(status().isUnprocessableContent())
            .andExpect(view().name("settings/settings"));

        verifyNoInteractions(settingsService);
    }

    @Test
    void ensureUpdateSettingsPreviewDoesNotPersistAndUpdateSubtractBreakFromTimeEntryTimestamp() throws Exception {

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        final SettingsDto expectedSettingsDto = new SettingsDto(
            FederalState.NONE,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            "42",
            true,
            LocalDate.parse("2025-05-30"),
            null
        );

        perform(post("/settings")
            .param("preview", "")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "true")
            .param("lockTimeEntriesDaysInPast", "42")
            .param("subtractBreakFromTimeEntryIsActive", "true")
            .param("subtractBreakFromTimeEntryActiveDate", "2025-05-30")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("settings", expectedSettingsDto))
            .andExpect(view().name("settings/settings"));

        verifyNoInteractions(settingsService);
    }

    @Test
    void ensureUpdateSettingsRedirectsToSettings() throws Exception {

        when(userSettingsProvider.zoneId()).thenReturn(UTC);

        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "true")
            .param("lockTimeEntriesDaysInPast", "42")
            .param("subtractBreakFromTimeEntryIsActive", "true")
            .param("subtractBreakFromTimeEntryActiveDate", "2025-05-30")
        )
            .andExpect(flash().attributeCount(0))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        verify(settingsService).updateFederalStateSettings(FederalState.NONE, false);
        verify(settingsService).updateWorkingTimeSettings(any(EnumMap.class), eq(WorkingTimeSettings.DEFAULT_TIME_ROUNDING_MINUTES), eq(WorkingTimeSettings.DEFAULT_MIN_SUGGESTED_MINUTES));
        verify(settingsService).updateLockTimeEntriesSettings(true, 42);
        verify(settingsService).updateSubtractBreakFromTimeEntrySettings(true, LocalDate.parse("2025-05-30").atStartOfDay().toInstant(UTC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1"})
    void ensureUpdateLockTimeSettingsWithDisabledLockingAndZeroOrPositiveDaysInPast(String daysInPast) throws Exception {

        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "false")
            .param("lockTimeEntriesDaysInPast", daysInPast)
            .param("subtractBreakFromTimeEntryIsActive", "false")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        verify(settingsService).updateLockTimeEntriesSettings(false, Integer.parseInt(daysInPast));
    }

    @Test
    void ensureGetSettingsIncludesGlobalWorkingDays() throws Exception {

        final EnumMap<DayOfWeek, Duration> customDays = new EnumMap<>(DayOfWeek.class);
        customDays.put(DayOfWeek.MONDAY,    Duration.ofHours(8));
        customDays.put(DayOfWeek.TUESDAY,   Duration.ofHours(8));
        customDays.put(DayOfWeek.WEDNESDAY, Duration.ofHours(8));
        customDays.put(DayOfWeek.THURSDAY,  Duration.ofHours(8));
        customDays.put(DayOfWeek.FRIDAY,    Duration.ofHours(8));
        customDays.put(DayOfWeek.SATURDAY,  Duration.ZERO);
        customDays.put(DayOfWeek.SUNDAY,    Duration.ZERO);
        when(settingsService.getFederalStateSettings()).thenReturn(new FederalStateSettings(FederalState.NONE, false));
        when(settingsService.getWorkingTimeSettings()).thenReturn(new WorkingTimeSettings(customDays, 5, 15));
        when(settingsService.getCategorisationSettings()).thenReturn(de.focusshift.zeiterfassung.settings.CategorisationSettings.DEFAULT);
        when(settingsService.getLockTimeEntriesSettings()).thenReturn(new LockTimeEntriesSettings(false, -1));
        when(settingsService.getSubtractBreakFromTimeEntrySettings()).thenReturn(Optional.empty());
        when(settingsService.getOooCalendarSettings()).thenReturn(OooCalendarSettings.DEFAULT);
        when(userSettingsProvider.zoneId()).thenReturn(UTC);

        perform(get("/settings"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("settings"))
            .andExpect(result -> {
                final SettingsDto dto = (SettingsDto) result.getModelAndView().getModel().get("settings");
                org.assertj.core.api.Assertions.assertThat(dto.workday())
                    .containsExactly("monday", "tuesday", "wednesday", "thursday", "friday");
                org.assertj.core.api.Assertions.assertThat(dto.workingTime()).isEqualTo(8.0);
            });
    }

    @Test
    void ensurePostSettingsSavesWorkingDays() throws Exception {


        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
            .param("workingTime", "8.0")
            .param("lockingIsActive", "false")
            .param("lockTimeEntriesDaysInPast", "0")
        )
            .andExpect(status().is3xxRedirection());

        verify(settingsService).updateFederalStateSettings(FederalState.NONE, false);
        verify(settingsService).updateWorkingTimeSettings(
            org.mockito.ArgumentMatchers.assertArg(workdays -> {
                org.assertj.core.api.Assertions.assertThat(workdays.get(DayOfWeek.MONDAY)).isEqualTo(Duration.ofHours(8));
                org.assertj.core.api.Assertions.assertThat(workdays.get(DayOfWeek.SATURDAY)).isEqualTo(Duration.ZERO);
            }),
            eq(WorkingTimeSettings.DEFAULT_TIME_ROUNDING_MINUTES),
            eq(WorkingTimeSettings.DEFAULT_MIN_SUGGESTED_MINUTES)
        );
    }

    @Test
    void ensurePostSettingsWithNoDaysCheckedSavesAllZero() throws Exception {


        perform(post("/settings")
            .param("federalState", "NONE")
            .param("worksOnPublicHoliday", "false")
            .param("lockingIsActive", "false")
            .param("lockTimeEntriesDaysInPast", "0")
            // no workday params → all unchecked
        )
            .andExpect(status().is3xxRedirection());

        verify(settingsService).updateFederalStateSettings(FederalState.NONE, false);
        verify(settingsService).updateWorkingTimeSettings(
            org.mockito.ArgumentMatchers.assertArg(workdays ->
                org.assertj.core.api.Assertions.assertThat(workdays.values())
                    .allMatch(Duration::isZero)
            ),
            eq(WorkingTimeSettings.DEFAULT_TIME_ROUNDING_MINUTES),
            eq(WorkingTimeSettings.DEFAULT_MIN_SUGGESTED_MINUTES)
        );
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
