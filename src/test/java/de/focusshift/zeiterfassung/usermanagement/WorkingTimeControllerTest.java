package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
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

    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeController(userManagementService, workingTimeService, workingTimeDtoValidator);
    }

    @Test
    void ensureSimpleGet() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @ParameterizedTest
    @CsvSource({
        "ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false",
        "ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true"
    })
    void ensureSimpleGetAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount) throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);


        perform(
            get("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount));
    }

    @Test
    void ensureSimpleGetJavaScript() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSimpleGetForWorkingTimeWithSpecialWorkingDays() throws Exception {

        final User alfred = new User(new UserId("uuid"), new UserLocalId(1L), "Alfred", "Pennyworth", new EMailAddress("alfred@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(alfred));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(1L))
            .monday(BigDecimal.valueOf(4))
            .wednesday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(1L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(expectedSelectedUser)))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/1"));
    }

    @Test
    void ensureSearch() throws Exception {

        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchJavaScript() throws Exception {

        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQuery() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));
        when(userManagementService.findUserByLocalId(new UserLocalId(42L))).thenReturn(Optional.of(superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
            .andExpect(model().attribute("personSearchFormAction", "/users/42"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQueryJavaScript() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));
        when(userManagementService.findUserByLocalId(new UserLocalId(42L))).thenReturn(Optional.of(superman));

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(EIGHT)
            .tuesday(EIGHT)
            .wednesday(EIGHT)
            .thursday(EIGHT)
            .friday(EIGHT)
            .build();

        when(workingTimeService.getWorkingTimeByUser(new UserLocalId(42L))).thenReturn(workingTime);

        final WorkingTimeDto expectedWorkingTimeDto = WorkingTimeDto.builder()
            .userId(workingTime.getUserId().value())
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
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("workingTime", expectedWorkingTimeDto))
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

        final ArgumentCaptor<WorkingTime> captor = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeService).updateWorkingTime(captor.capture());

        final WorkingTime actual = captor.getValue();
        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(new WorkDay(MONDAY, Duration.ofHours(0)));
        assertThat(actual.getTuesday()).hasValue(new WorkDay(TUESDAY, Duration.ofHours(1)));
        assertThat(actual.getWednesday()).hasValue(new WorkDay(WEDNESDAY, Duration.ofHours(2)));
        assertThat(actual.getThursday()).hasValue(new WorkDay(THURSDAY, Duration.ofHours(3)));
        assertThat(actual.getFriday()).hasValue(new WorkDay(FRIDAY, Duration.ofHours(4)));
        assertThat(actual.getSaturday()).hasValue(new WorkDay(SATURDAY, Duration.ofHours(5)));
        assertThat(actual.getSunday()).hasValue(new WorkDay(SUNDAY, Duration.ofHours(6)));
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

        final ArgumentCaptor<WorkingTime> captor = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeService).updateWorkingTime(captor.capture());

        final WorkingTime actual = captor.getValue();
        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(new WorkDay(MONDAY, Duration.ofHours(0)));
        assertThat(actual.getTuesday()).hasValue(new WorkDay(TUESDAY, Duration.ofHours(1)));
        assertThat(actual.getWednesday()).hasValue(new WorkDay(WEDNESDAY, Duration.ofHours(2)));
        assertThat(actual.getThursday()).hasValue(new WorkDay(THURSDAY, Duration.ofHours(3)));
        assertThat(actual.getFriday()).hasValue(new WorkDay(FRIDAY, Duration.ofHours(4)));
        assertThat(actual.getSaturday()).hasValue(new WorkDay(SATURDAY, Duration.ofHours(5)));
        assertThat(actual.getSunday()).hasValue(new WorkDay(SUNDAY, Duration.ofHours(6)));
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

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("")))
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

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
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

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            post("/users/42/working-time")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("workday", "monday", "tuesday", "wednesday", "thursday", "friday")
                .param("workingTime", "48")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("")))
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
