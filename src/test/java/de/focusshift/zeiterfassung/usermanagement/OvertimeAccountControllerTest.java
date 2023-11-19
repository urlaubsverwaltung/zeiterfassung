package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.web.DoubleFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
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
class OvertimeAccountControllerTest {

    private OvertimeAccountController sut;

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private OvertimeAccountServiceImpl overtimeAccountService;

    @BeforeEach
    void setUp() {
        sut = new OvertimeAccountController(userManagementService, overtimeAccountService);
    }

    @Test
    void ensureSimpleGet() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("section", "overtime"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/overtime-account"))
            .andExpect(model().attribute("overtimeAccount", new OvertimeAccountDto(true, 10.5)));
    }

    @ParameterizedTest
    @CsvSource({
        "ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false",
        "ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true"
    })
    void ensureSimpleGetAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount) throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount));
    }

    @Test
    void ensureSimpleGetWithJavaScript() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-turbo-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-turbo-frame"))
            .andExpect(model().attribute("section", "overtime"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/overtime-account"))
            .andExpect(model().attribute("overtimeAccount", new OvertimeAccountDto(true, 10.5)));
    }

    @Test
    void ensureSearch() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("awesome-query")).thenReturn(List.of(batman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .param("query", "awesome-query")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "awesome-query"));
    }

    @Test
    void ensureSearchWithJavaScript() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("awesome-query")).thenReturn(List.of(batman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-turbo-frame")
                .param("query", "awesome-query")
        )
            .andExpect(view().name("usermanagement/users::#awesome-turbo-frame"))
            .andExpect(model().attribute("query", "awesome-query"));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQuery() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(superman));
        when(userManagementService.findUserByLocalId(batmanLocalId)).thenReturn(Optional.of(batman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser));
    }

    @Test
    void ensureSearchWithSelectedUserNotInQueryWithJavaScript() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("super")).thenReturn(List.of(superman));
        when(userManagementService.findUserByLocalId(batmanLocalId)).thenReturn(Optional.of(batman));

        final OvertimeAccount overtimeAccount = new OvertimeAccount(batmanLocalId, true, Duration.ofHours(10).plusMinutes(30));
        when(overtimeAccountService.getOvertimeAccount(batmanLocalId)).thenReturn(overtimeAccount);

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            get("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-turbo-frame")
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-turbo-frame"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser));
    }

    @ParameterizedTest
    @ValueSource(strings = {",", "."})
    void ensurePost(String separator) throws Exception {

        perform(
            post("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .param("allowed", "true")
                .param("maxAllowedOvertime", "5%s25".formatted(separator))
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/1337/overtime-account"));

        verify(overtimeAccountService).updateOvertimeAccount(new UserLocalId(1337L), true, Duration.ofHours(5).plusMinutes(15));
    }

    @ParameterizedTest
    @ValueSource(strings = {",", "."})
    void ensurePostWithJavaScript(String separator) throws Exception {

        perform(
            post("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-turbo-frame")
                .param("allowed", "true")
                .param("maxAllowedOvertime", "5%s25".formatted(separator))
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/1337/overtime-account"));

        verify(overtimeAccountService).updateOvertimeAccount(new UserLocalId(1337L), true, Duration.ofHours(5).plusMinutes(15));
    }

    @Test
    void ensurePostWithValidationError() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            post("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .param("allowed", "true")
                .param("maxAllowedOvertime", "must-be-a-number")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("section", "overtime"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/overtime-account"))
            .andExpect(model().attribute("overtimeAccount", new OvertimeAccountDto(true, null)));

        verifyNoInteractions(overtimeAccountService);
    }

    @ParameterizedTest
    @CsvSource({
        "ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false",
        "ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true"
    })
    void ensurePostWithValidationErrorAllowedToEditX(String authority, boolean editWorkingTime, boolean editOvertimeAccount) throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman));

        perform(
            post("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
                .param("allowed", "true")
                .param("maxAllowedOvertime", "must-be-a-number")
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTime))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount));
    }

    @Test
    void ensurePostWithValidationErrorWithJavaScript() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            post("/users/1337/overtime-account")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-turbo-frame")
                .param("allowed", "true")
                .param("maxAllowedOvertime", "must-be-a-number")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-turbo-frame"))
            .andExpect(model().attribute("section", "overtime"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "overtime-account"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/overtime-account"))
            .andExpect(model().attribute("overtimeAccount", new OvertimeAccountDto(true, null)));

        verifyNoInteractions(overtimeAccountService);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {

        // real integration test would be better, wouldn't it?
        final FormattingConversionService formattingConversionService = new FormattingConversionService();
        formattingConversionService.addFormatter(new DoubleFormatter());

        return standaloneSetup(sut)
            .setConversionService(formattingConversionService)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
