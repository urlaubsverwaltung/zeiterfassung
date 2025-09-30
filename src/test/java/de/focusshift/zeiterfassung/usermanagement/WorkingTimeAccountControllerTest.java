package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.ControllerTest;
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
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BERLIN;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class WorkingTimeAccountControllerTest implements ControllerTest {

    private WorkingTimeAccountController sut;

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private WorkingTimeService workingTimeService;
    @Mock
    private FederalStateSettingsService federalStateSettingsService;

    private static final Clock clockFixed = Clock.fixed(Instant.now(), UTC);
    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeAccountController(userManagementService, workingTimeService, federalStateSettingsService, clockFixed);
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        );

        final List<WorkingTimeListEntryDto> workingTimes = getModelAttribute("workingTimes", result);
        assertThat(workingTimes).extracting(WorkingTimeListEntryDto::isDeletable).contains(true);
    }

    @Test
    void ensureSimpleGetWithNotDeletableWorkingTimeIsEmptyBecauseItWillNotBeDisplayed() throws Exception {

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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(userLocalId)).thenReturn(List.of(workingTime));

        final ResultActions result = perform(
            get("/users/1337/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        );

        final List<WorkingTimeListEntryDto> workingTimes = getModelAttribute("workingTimes", result);
        assertThat(workingTimes).isEmpty();
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));


        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority(authority)))
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(1, "Alfred", "Pennyworth", "Alfred Pennyworth", "AP", "alfred@example.org");

        perform(
            get("/users/1/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
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

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime workingTime = WorkingTime.builder(userIdComposite, workingTimeId)
            .current(true)
            .federalState(GERMANY_BADEN_WUERTTEMBERG)
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "bat"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org")
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
            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
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

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", "bat"))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeListEntryDto)))
            .andExpect(model().attribute("globalFederalState", GERMANY_BERLIN))
            .andExpect(model().attribute("globalFederalStateMessageKey", "federalState.GERMANY_BERLIN"))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
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
