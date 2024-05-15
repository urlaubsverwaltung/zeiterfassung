package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.SessionService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.web.DoubleFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.verify;
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
class PermissionsControllerTest {

    private PermissionsController sut;

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sut = new PermissionsController(userManagementService, sessionService);
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

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        final PermissionsDto expectedPermissionsDto = new PermissionsDto();
        expectedPermissionsDto.setViewReportAll(false);
        expectedPermissionsDto.setWorkingTimeEditAll(false);
        expectedPermissionsDto.setOvertimeEditAll(false);
        expectedPermissionsDto.setPermissionsEditAll(false);

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("section", "permissions"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "permissions"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/permissions"))
            .andExpect(model().attribute("permissions", expectedPermissionsDto));
    }

    static Stream<Arguments> expectedPermissionDtos() {

        final PermissionsDto reportPermissionDto = new PermissionsDto();
        reportPermissionDto.setViewReportAll(true);

        final PermissionsDto workingTimeAllPermissionDto = new PermissionsDto();
        workingTimeAllPermissionDto.setWorkingTimeEditAll(true);

        final PermissionsDto workingTimeGlobalPermissionDto = new PermissionsDto();
        workingTimeGlobalPermissionDto.setWorkingTimeEditGlobal(true);

        final PermissionsDto overtimePermissionDto = new PermissionsDto();
        overtimePermissionDto.setOvertimeEditAll(true);

        final PermissionsDto permissionsPermissionDto = new PermissionsDto();
        permissionsPermissionDto.setPermissionsEditAll(true);

        return Stream.of(
            Arguments.of(SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL, reportPermissionDto),
            Arguments.of(SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, workingTimeAllPermissionDto),
            Arguments.of(SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL, workingTimeGlobalPermissionDto),
            Arguments.of(SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, overtimePermissionDto),
            Arguments.of(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, permissionsPermissionDto)
        );
    }

    @ParameterizedTest
    @MethodSource("expectedPermissionDtos")
    void ensureSimpleGetWithEnabledPermission(SecurityRole role, PermissionsDto permissionsDto) throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of(role));
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("permissions", permissionsDto));
    }

    @ParameterizedTest
    @CsvSource({
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,true,false,false",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,true,false",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL,false,false,true"
    })
    void ensureSimpleGetAllowedToEditX(String authority, boolean editWorkingTimeAll, boolean editOvertimeAccount, boolean editPermissions) throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId supermanId = new UserId("superman");
        final UserLocalId supermanLocalId = new UserLocalId(42L);
        final UserIdComposite supermanIdComposite = new UserIdComposite(supermanId, supermanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User superman = new User(supermanIdComposite, "Clark", "Kent", new EMailAddress("superman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority(authority)))
        )
            .andExpect(model().attribute("allowedToEditWorkingTime", editWorkingTimeAll))
            .andExpect(model().attribute("allowedToEditOvertimeAccount", editOvertimeAccount))
            .andExpect(model().attribute("allowedToEditPermissions", editPermissions));
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

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        final PermissionsDto expectedPermissionsDto = new PermissionsDto();
        expectedPermissionsDto.setViewReportAll(false);
        expectedPermissionsDto.setWorkingTimeEditAll(false);
        expectedPermissionsDto.setOvertimeEditAll(false);
        expectedPermissionsDto.setPermissionsEditAll(false);

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
                .header("Turbo-Frame", "person-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#person-frame"))
            .andExpect(model().attribute("section", "permissions"))
            .andExpect(model().attribute("query", ""))
            .andExpect(model().attribute("slug", "permissions"))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser))
            .andExpect(model().attribute("personSearchFormAction", "/users/1337/permissions"))
            .andExpect(model().attribute("permissions", expectedPermissionsDto));
    }

    @Test
    void ensureSearch() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("awesome-query")).thenReturn(List.of(batman));

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
                .param("query", "awesome-query")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "awesome-query"));
    }

    @Test
    void ensureSearchWithJavascript() throws Exception {

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("awesome-query")).thenReturn(List.of(batman));

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
                .param("query", "awesome-query")
                .header("Turbo-Frame", "person-list-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#person-list-frame"))
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

        final UserDto expectedSelectedUser = new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org");

        perform(
            get("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
                .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", "super"))
            .andExpect(model().attribute("slug", "permissions"))
            .andExpect(model().attribute("users", contains(
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", expectedSelectedUser));
    }

    @ParameterizedTest
    @CsvSource({
        "ZEITERFASSUNG_VIEW_REPORT_ALL,true,false,false,false",
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,false,true,false,false",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,false,false,true,false",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL,false,false,false,true"
    })
    void ensurePost(String roleName, boolean viewReportAll, boolean workingTimeEditAll, boolean overtimeEditAll, boolean permissionsEditAll) throws Exception {

        final SecurityRole securityRole = SecurityRole.valueOf(roleName);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("")).thenReturn(List.of(user));

        perform(
            post("/users/1337/permissions")
                .with(oidcLogin().authorities(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL.authority()))
                .param("viewReportAll", String.valueOf(viewReportAll))
                .param("workingTimeEditAll", String.valueOf(workingTimeEditAll))
                .param("overtimeEditAll", String.valueOf(overtimeEditAll))
                .param("permissionsEditAll", String.valueOf(permissionsEditAll))
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/users/1337/permissions"));

        verify(userManagementService).updateUserPermissions(new UserLocalId(1337L), Set.of(securityRole));
        verify(sessionService).markSessionToReloadAuthorities(new UserLocalId(1337L));
    }


    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {

        // real integration test would be better, wouldn't it?
        final FormattingConversionService formattingConversionService = new FormattingConversionService();
        formattingConversionService.addFormatter(new DoubleFormatter());

        return standaloneSetup(sut)
            .setConversionService(formattingConversionService)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new CurrentSecurityContextArgumentResolver())
            .build()
            .perform(builder);
    }
}
