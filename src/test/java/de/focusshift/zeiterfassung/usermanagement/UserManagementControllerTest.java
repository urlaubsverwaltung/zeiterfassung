package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest implements ControllerTest {

    private UserManagementController sut;

    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new UserManagementController(userManagementService);
    }

    @Test
    void ensureUsers() throws Exception {

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
            get("/users")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersWithJavaScript() throws Exception {

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
            get("/users")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersSearch() throws Exception {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));

        perform(
            get("/users")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("bat")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersSearchWithJavaScript() throws Exception {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));

        perform(
            get("/users")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("bat")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUserForwardsToWorkingTime() throws Exception {
        perform(
            get("/users/42")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_WORKING_TIME_EDIT_ALL")))
        )
            .andExpect(view().name("forward:/users/42/working-time"));
    }

    @Test
    void ensureUserForwardsToOvertimeAccount() throws Exception {
        perform(
            get("/users/42")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL")))
        )
            .andExpect(view().name("forward:/users/42/overtime-account"));
    }

    @Test
    void ensureUserForwardsToPermissions() throws Exception {
        perform(
            get("/users/42")
                .with(oidcSubject("uuid").authorities(new SimpleGrantedAuthority("ZEITERFASSUNG_PERMISSIONS_EDIT_ALL")))
        )
            .andExpect(view().name("forward:/users/42/permissions"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new CurrentSecurityContextArgumentResolver())
            .build()
            .perform(builder);
    }
}
