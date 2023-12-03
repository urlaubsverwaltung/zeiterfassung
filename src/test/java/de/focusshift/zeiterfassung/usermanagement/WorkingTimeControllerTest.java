package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
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
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
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

    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeController(userManagementService, workingTimeService, workingTimeDtoValidator);
    }

    @Test
    void ensureSimpleGet() throws Exception {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.userIdComposite().localId().value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @ParameterizedTest
    @CsvSource({
        "ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false",
        "ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true"
    })
    void ensureSimpleGetAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount) throws Exception {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));


        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount));
    }

    @Test
    void ensureSimpleGetJavaScript() throws Exception {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("uuid-2");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.userIdComposite().localId().value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSimpleGetForWorkingTimeWithSpecialWorkingDays() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Alfred", "Pennyworth", new EMailAddress("alfred@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        final WorkingTime workingTime = WorkingTime.builder(userIdComposite)
            .monday(BigDecimal.valueOf(4))
            .wednesday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(1L))).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.userIdComposite().localId().value())
            .workday(List.of(MONDAY, WEDNESDAY, SATURDAY))
            .workingTimeMonday(4.0)
            .workingTimeWednesday(5.0)
            .workingTimeSaturday(6.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(1, "Alfred", "Pennyworth", "Alfred Pennyworth", "alfred@example.org");

        perform(
            get("/users/1/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "working-time"))
            .andExpect(model().attribute("users", contains(expectedSelectedUser)))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/1"));
    }

    @Test
    void ensureSearch() throws Exception {

        final UserId userId = new UserId("uuid-2");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTime workingTime = WorkingTime.builder(userIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.userIdComposite().localId().value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchJavaScript() throws Exception {

        final UserId userId = new UserId("uuid-2");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(user));

        final WorkingTime workingTime = WorkingTime.builder(userIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(userLocalId.value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQuery() throws Exception {

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

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(supermanLocalId)).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(supermanLocalId.value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQueryJavaScript() throws Exception {

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

        final WorkingTime workingTime = WorkingTime.builder(supermanIdComposite)
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getAllWorkingTimesByUser(new UserLocalId(42L))).thenReturn(List.of(workingTime));

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(supermanLocalId.value())
            .workday(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
            .workingTime(8.0)
            .build();

        final UserDto expectedSelectedUser = new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org");

        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("workingTimes", List.of(expectedWorkingTimeDto)))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensurePost() throws Exception {

        perform(
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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

        final WorkWeekUpdate expectedWorkWeekUpdate = WorkWeekUpdate.builder()
            .monday(Duration.ofHours(0))
            .tuesday(Duration.ofHours(1))
            .wednesday(Duration.ofHours(2))
            .thursday(Duration.ofHours(3))
            .friday(Duration.ofHours(4))
            .saturday(Duration.ofHours(5))
            .sunday(Duration.ofHours(6))
            .build();

        verify(workingTimeService).updateWorkingTime(new UserLocalId(42L), expectedWorkWeekUpdate);
    }

    @Test
    void ensurePostJavaScript() throws Exception {

        perform(
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
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

        final WorkWeekUpdate expectedWorkWeekUpdate = WorkWeekUpdate.builder()
            .monday(Duration.ofHours(0))
            .tuesday(Duration.ofHours(1))
            .wednesday(Duration.ofHours(2))
            .thursday(Duration.ofHours(3))
            .friday(Duration.ofHours(4))
            .saturday(Duration.ofHours(5))
            .sunday(Duration.ofHours(6))
            .build();

        verify(workingTimeService).updateWorkingTime(new UserLocalId(42L), expectedWorkWeekUpdate);
    }

    @Test
    void ensurePostWithValidationError() throws Exception {

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
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
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
            .andExpect(model().attribute("personSearchFormAction", is("/users/42")));

        verifyNoInteractions(workingTimeService);
    }

    @ParameterizedTest
    @CsvSource({
        "ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false",
        "ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true"
    })
    void ensurePostWithValidationErrorAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount) throws Exception {

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
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount));
    }

    @Test
    void ensurePostWithValidationErrorJavaScript() throws Exception {

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
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
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
            .andExpect(model().attribute("personSearchFormAction", is("/users/42")));

        verifyNoInteractions(workingTimeService);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
