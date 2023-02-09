package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

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
class UserManagementControllerTest {

    private UserManagementController sut;

    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new UserManagementController(userManagementService);
    }

    @Test
    void ensureUsers() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"));
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersWithJavaScript() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final User superman = new User(new UserId("uuid-2"), new UserLocalId(42L), "Clark", "Kent", new EMailAddress("superman@example.org"));
        when(userManagementService.findAllUsers("")).thenReturn(List.of(batman, superman));

        perform(
            get("/users")
                .header("Turbo-Frame", "awesome-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersSearch() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));

        perform(
            get("/users")
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("bat")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUsersSearchWithJavaScript() throws Exception {

        final User batman = new User(new UserId("uuid"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        when(userManagementService.findAllUsers("bat")).thenReturn(List.of(batman));

        perform(
            get("/users")
                .header("Turbo-Frame", "awesome-frame")
                .param("query", "bat")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("bat")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "batman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()))
            .andExpect(model().attribute("personSearchFormAction", is("/users")));
    }

    @Test
    void ensureUserForwardsToWorkingTime() throws Exception {
        perform(get("/users/42"))
            .andExpect(view().name("forward:/users/42/working-time"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
