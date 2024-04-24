package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import de.focusshift.zeiterfassung.workingtime.WorksOnPublicHoliday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BAYERN;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BERLIN;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class WorkingTimeControllerTest {

    private WorkingTimeController sut;

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private WorkingTimeService workingTimeService;
    @Mock
    private WorkingTimeDtoValidator workingTimeDtoValidator;
    @Mock
    private FederalStateSettingsService federalStateSettingsService;

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeController(userManagementService, workingTimeService, workingTimeDtoValidator, federalStateSettingsService);
    }

    @Test
    void ensureGetCreatePage() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, true));

        perform(
            get("/users/42/working-time/new")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("createMode", true))
            .andExpect(model().attribute("section", "working-time-edit"))
            .andExpect(model().attributeExists("federalStateSelect"));
    }

    @ParameterizedTest
    @CsvSource({
        "true, 1",
        "false, 0",
    })
    void ensureGetCreatePageWithCorrectGlobalWorksOnPublicHoliday(boolean worksOnPublicHoliday, Integer expectedValue) throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, worksOnPublicHoliday));

        perform(
            get("/users/42/working-time/new")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(model().attribute("globalWorksOnPublicHoliday", expectedValue));
    }

    @Test
    void ensurePostCreateWorkingTimeFailsWithValidationError() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, false));

        final WorkingTimeDto expectedWorkingTimeDto = new WorkingTimeDto();
        expectedWorkingTimeDto.setUserId(42L);

        doAnswer(
            invocation -> {
                final BindingResult result = invocation.getArgument(1);
                result.addError(new ObjectError("workingTime", "error"));
                return null;
            }
        ).when(workingTimeDtoValidator).validate(eq(expectedWorkingTimeDto), any(BindingResult.class));

        perform(
            post("/users/42/working-time/new")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("userId", "42")
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("createMode", true))
            .andExpect(view().name("usermanagement/users"));

        verifyNoInteractions(workingTimeService);
    }

    @Test
    void ensurePostCreateWorkingTimeFailsWithValidationErrorJavaScript() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, false));

        final WorkingTimeDto expectedWorkingTimeDto = new WorkingTimeDto();
        expectedWorkingTimeDto.setUserId(42L);

        doAnswer(
            invocation -> {
                final BindingResult result = invocation.getArgument(1);
                result.addError(new ObjectError("workingTime", "error"));
                return null;
            }
        ).when(workingTimeDtoValidator).validate(eq(expectedWorkingTimeDto), any(BindingResult.class));

        perform(
            post("/users/42/working-time/new")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "person-frame")
                .param("userId", "42")
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attribute("createMode", true))
            .andExpect(view().name("usermanagement/users::#person-frame"));

        verifyNoInteractions(workingTimeService);
    }

    @Test
    void ensurePostCreateWorkingTime() throws Exception {

        perform(
            post("/users/42/working-time/new")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("userId", "42")
                .param("validFrom", "2024-04-01")
                .param("federalState", "GERMANY_BADEN_WUERTTEMBERG")
                .param("workday", "monday", "wednesday")
                .param("workingTime", "4.0")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/42/working-time"));

        verify(workingTimeService).createWorkingTime(
            new UserLocalId(42L),
            LocalDate.of(2024, 4, 1),
            GERMANY_BADEN_WUERTTEMBERG,
            null,
            new EnumMap<>(Map.of(
                MONDAY, Duration.ofHours(4),
                TUESDAY, Duration.ZERO,
                WEDNESDAY, Duration.ofHours(4),
                THURSDAY, Duration.ZERO,
                FRIDAY, Duration.ZERO,
                SATURDAY, Duration.ZERO,
                SUNDAY, Duration.ZERO
            ))
        );
    }

    @Test
    void ensureGetEditPage() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, true));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId).worksOnPublicHoliday(WorksOnPublicHoliday.GLOBAL).build();
        when(workingTimeService.getWorkingTimeById(workingTimeId)).thenReturn(Optional.of(workingTime));

        perform(
            get("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("createMode", false))
            .andExpect(model().attribute("section", "working-time-edit"))
            .andExpect(model().attributeExists("workingTime", "federalStateSelect"));
    }

    @ParameterizedTest
    @CsvSource({
        "true, 1",
        "false, 0",
    })
    void ensureGetEditPage(boolean globalWorksOnPublicHoliday, Integer expectedValue) throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        when(federalStateSettingsService.getFederalStateSettings())
            .thenReturn(new FederalStateSettings(GERMANY_BERLIN, globalWorksOnPublicHoliday));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .worksOnPublicHoliday(WorksOnPublicHoliday.GLOBAL)
            .build();

        when(workingTimeService.getWorkingTimeById(workingTimeId)).thenReturn(Optional.of(workingTime));

        perform(
            get("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(model().attribute("globalWorksOnPublicHoliday", expectedValue));
    }

    @Test
    void ensureEdit() throws Exception {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        perform(
            post("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("id", workingTimeId.value())
                .param("federalState", "GERMANY_BADEN_WUERTTEMBERG")
                .param("worksOnPublicHoliday", "true")
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
                .param("workingTimeMonday", "0")
                .param("workingTimeTuesday", "1")
                .param("workingTimeWednesday", "2")
                .param("workingTimeThursday", "3")
                .param("workingTimeFriday", "4")
                .param("workingTimeSaturday", "5")
                .param("workingTimeSunday", "6")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/42/working-time"));

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(0),
            TUESDAY, Duration.ofHours(1),
            WEDNESDAY, Duration.ofHours(2),
            THURSDAY, Duration.ofHours(3),
            FRIDAY, Duration.ofHours(4),
            SATURDAY, Duration.ofHours(5),
            SUNDAY, Duration.ofHours(6)
        ));

        verify(workingTimeService).updateWorkingTime(workingTimeId, null, GERMANY_BADEN_WUERTTEMBERG, true, workdays);
    }

    @Test
    void ensureEditJavaScript() throws Exception {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        perform(
            post("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("id", workingTimeId.value())
                .param("federalState", "GERMANY_BADEN_WUERTTEMBERG")
                .param("worksOnPublicHoliday", "true")
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
                .param("workingTimeMonday", "0")
                .param("workingTimeTuesday", "1")
                .param("workingTimeWednesday", "2")
                .param("workingTimeThursday", "3")
                .param("workingTimeFriday", "4")
                .param("workingTimeSaturday", "5")
                .param("workingTimeSunday", "6")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/42/working-time"));

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(0),
            TUESDAY, Duration.ofHours(1),
            WEDNESDAY, Duration.ofHours(2),
            THURSDAY, Duration.ofHours(3),
            FRIDAY, Duration.ofHours(4),
            SATURDAY, Duration.ofHours(5),
            SUNDAY, Duration.ofHours(6)
        ));

        verify(workingTimeService).updateWorkingTime(workingTimeId, null, GERMANY_BADEN_WUERTTEMBERG, true, workdays);
    }

    @Test
    void ensureEditWithValidationError() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeDto expectedWorkingTimeDto = new WorkingTimeDto();
        expectedWorkingTimeDto.setId(workingTimeId.value());
        expectedWorkingTimeDto.setUserId(42L);
        expectedWorkingTimeDto.setWorkday(List.of("monday", "tuesday", "wednesday", "thursday", "friday"));
        expectedWorkingTimeDto.setWorkingTime(48.0);

        doAnswer(
            invocation -> {
                final BindingResult result = invocation.getArgument(1);
                result.addError(new ObjectError("workingTime", "error"));
                return null;
            }
        ).when(workingTimeDtoValidator).validate(eq(expectedWorkingTimeDto), any(BindingResult.class));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            post("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("id", workingTimeId.value())
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", is("/users/42")));

        verifyNoInteractions(workingTimeService);
    }

    @ParameterizedTest
    @CsvSource({
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false,false",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true,false",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL,false,false,true"
    })
    void ensureEditWithValidationErrorAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount, boolean editPermissions) throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final WorkingTimeDto expectedWorkingTimeDto = new WorkingTimeDto();
        expectedWorkingTimeDto.setUserId(42L);
        expectedWorkingTimeDto.setWorkday(List.of("monday", "tuesday", "wednesday", "thursday", "friday"));
        expectedWorkingTimeDto.setWorkingTime(48.0);

        doAnswer(
            invocation -> {
                final BindingResult result = invocation.getArgument(1);
                result.addError(new ObjectError("workingTime", "error"));
                return null;
            }
        ).when(workingTimeDtoValidator).validate(eq(expectedWorkingTimeDto), any(BindingResult.class));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            post("/users/42/working-time/" + UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount))
            .andExpect(model().attribute("allowedToEditPermissions", editPermissions));
    }

    @Test
    void ensureEditWithValidationErrorJavaScript() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BAYERN));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeDto expectedWorkingTimeDto = new WorkingTimeDto();
        expectedWorkingTimeDto.setId(workingTimeId.value());
        expectedWorkingTimeDto.setUserId(42L);
        expectedWorkingTimeDto.setWorkday(List.of("monday", "tuesday", "wednesday", "thursday", "friday"));
        expectedWorkingTimeDto.setWorkingTime(48.0);

        doAnswer(
            invocation -> {
                final BindingResult result = invocation.getArgument(1);
                result.addError(new ObjectError("workingTime", "error"));
                return null;
            }
        ).when(workingTimeDtoValidator).validate(eq(expectedWorkingTimeDto), any(BindingResult.class));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            post("/users/42/working-time/" + workingTimeId.value())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("id", workingTimeId.value())
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BAYERN"))
            .andExpect(model().attribute("personSearchFormAction", is("/users/42")));

        verifyNoInteractions(workingTimeService);
    }

    @Test
    void ensureDeleteWorkingTimeRendersErrorPageWhenItCannotBeDeleted() throws Exception {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        when(workingTimeService.deleteWorkingTime(workingTimeId)).thenReturn(false);

        perform(
            post("/users/42/working-time/%s/delete".formatted(workingTimeId.value()))
                .with(
                    oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL"))
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error/5xx"));
    }

    @Test
    void ensureDeleteWorkingTimeRedirectsToWorkingTimeView() throws Exception {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        when(workingTimeService.deleteWorkingTime(workingTimeId)).thenReturn(true);

        perform(
            post("/users/42/working-time/%s/delete".formatted(workingTimeId.value()))
                .with(
                    oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL"))
                )
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/42/working-time"));
    }

    private FederalStateSettings federalStateSettings(FederalState globalFederalState) {
        return new FederalStateSettings(globalFederalState, false);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new CurrentSecurityContextArgumentResolver())
            .build()
            .perform(builder);
    }
}
