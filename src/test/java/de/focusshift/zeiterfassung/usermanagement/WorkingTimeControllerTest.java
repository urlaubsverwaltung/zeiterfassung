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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BERLIN;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
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

    private static final Clock clockFixed = Clock.fixed(Instant.now(), UTC);
    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeController(userManagementService, workingTimeService, workingTimeDtoValidator, federalStateSettingsService, clockFixed);
    }

    @Test
    void ensureSimpleGet() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSimpleGetWithDeletableWorkingTimeBecauseValidFromIsInTheFuture() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .validFrom(LocalDate.now(clockFixed).plusDays(1))
            .federalState(NONE)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        );

        final List<WorkingTimeListEntryDto> workingTimes = getModelAttribute("workingTimes", result);
        assertThat(workingTimes).extracting(WorkingTimeListEntryDto::isDeletable).contains(true);
    }

    static Stream<Arguments> nowAndPastDate() {
        return Stream.of(
            Arguments.of(LocalDate.now(clockFixed)),
            Arguments.of(LocalDate.now(clockFixed).minusDays(1))
        );
    }

    @ParameterizedTest
    @MethodSource("nowAndPastDate")
    void ensureSimpleGetWithDeletableWorkingTimeDespiteValidFromIsNowOrInThePast(LocalDate givenValidFrom) throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .validFrom(givenValidFrom)
            .federalState(NONE)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        );

        final List<WorkingTimeListEntryDto> workingTimes = getModelAttribute("workingTimes", result);
        assertThat(workingTimes).extracting(WorkingTimeListEntryDto::isDeletable).contains(true);
    }
    @Test
    void ensureSimpleGetWithNotDeletableWorkingTimeBecauseVeryFirstWorkingTime() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .validFrom(null)
            .federalState(NONE)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        );

        final List<WorkingTimeListEntryDto> workingTimes = getModelAttribute("workingTimes", result);
        assertThat(workingTimes).extracting(WorkingTimeListEntryDto::isDeletable).contains(false);
    }

    @ParameterizedTest
    @CsvSource({
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false,false",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true,false",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL,false,false,true"
    })
    void ensureSimpleGetAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount, boolean editPermissions) throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite, new WorkingTimeId(UUID.randomUUID()))
            .federalState(NONE)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));


        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount))
            .andExpect(model().attribute("allowedToEditPermissions", editPermissions));
    }

    @Test
    void ensureSimpleGetJavaScript() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSimpleGetForWorkingTimeWithSpecialWorkingDays() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Alfred", "Pennyworth", new EMailAddress("alfred@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(BigDecimal.valueOf(4))
            .wednesday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(1L))).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            4.0,
            0d,
            5.0,
            0d,
            0d,
            6.0,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(1, "Alfred", "Pennyworth", "Alfred Pennyworth", "alfred@example.org");

        perform(
            get("/users/1/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(expectedSelectedUser)))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/1"));
    }

    @Test
    void ensureSearch() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid-2");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchJavaScript() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid-2");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQuery() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));
        when(userManagementService.findUserByLocalId(new UserLocalId(42L))).thenReturn(Optional.of(superman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "bat"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQueryJavaScript() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));
        when(userManagementService.findUserByLocalId(supermanLocalId)).thenReturn(Optional.of(superman));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeListEntryDto expectedWorkingTimeListEntryDto = new WorkingTimeListEntryDto(
            workingTimeId.value(),
            workingTime.userIdComposite().localId().value(),
            null,
            null,
            true,
            true,
            false,
            "federalState.GERMANY_BADEN_WUERTTEMBERG",
            false,
            8.0,
            8.0,
            8.0,
            8.0,
            8.0,
            0d,
            0d
        );

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", "bat"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensurePost() throws Exception {

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
    void ensurePostJavaScript() throws Exception {

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
    void ensurePostWithValidationError() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .id(workingTimeId.value())
            .userId(42L)
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(48.0)
            .build();

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
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
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
    void ensurePostWithValidationErrorAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount, boolean editPermissions) throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(42L)
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(48.0)
            .build();

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
    void ensurePostWithValidationErrorJavaScript() throws Exception {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .id(workingTimeId.value())
            .userId(42L)
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(48.0)
            .build();

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
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
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

    @SuppressWarnings("unchecked")
    private <T> T getModelAttribute(String attributeName, ResultActions result) {
        final ModelAndView modelAndView = requireNonNull(result.andReturn().getModelAndView());
        return (T) modelAndView.getModel().get(attributeName);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new CurrentSecurityContextArgumentResolver())
            .build()
            .perform(builder);
    }
}
