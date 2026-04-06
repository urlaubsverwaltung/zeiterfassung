package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest implements ControllerTest {

    private static final String USERS_ALL_URL_TEMPLATE = "/users";
    private static final String USERS_USER_REDIRECT_URL_TEMPLATE = "/users/{userId}";

    private UserManagementController sut;

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSearchViewHelper userSearchViewHelper;

    @BeforeEach
    void setUp() {
        sut = new UserManagementController(userManagementService, userSearchViewHelper);
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

        perform(get(USERS_ALL_URL_TEMPLATE)
            .with(oidcSubject(batmanIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()));
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

        perform(get(USERS_ALL_URL_TEMPLATE)
            .with(oidcSubject(batmanIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)))
            .header("Turbo-Frame", "awesome-frame")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("usermanagement/users::#awesome-frame"))
            .andExpect(model().attribute("query", is("")))
            .andExpect(model().attribute("users", contains(
                new UserDto(1337, "Bruce", "Wayne", "Bruce Wayne", "BW", "batman@example.org"),
                new UserDto(42, "Clark", "Kent", "Clark Kent", "CK", "superman@example.org")
            )))
            .andExpect(model().attribute("selectedUser", nullValue()));
    }

    @Test
    void ensureUserSearchReturnsSuggestionsFrameWithJavaScript() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final CurrentOidcUser oidcUser = currentOidcUser(userIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL));

        when(userSearchViewHelper.getSuggestionFragment(eq("super"), eq(oidcUser), any(Model.class), any(Function.class)))
            .thenReturn(new ModelAndView("user-search-view"));

        perform(get(USERS_ALL_URL_TEMPLATE)
            .with(oidcLogin().oidcUser(oidcUser))
            .header("Turbo-Frame", "frame-users-suggestions")
            .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("user-search-view"));
    }

    @Nested
    class EnsureForwards {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        @Test
        void ensureUserForwardsToWorkingTime() throws Exception {

            perform(get(USERS_USER_REDIRECT_URL_TEMPLATE, userLocalId.value())
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)))
            )
                .andExpect(view().name("forward:/users/%s/working-time".formatted(userLocalId.value())));
        }

        @Test
        void ensureUserForwardsToOvertimeAccount() throws Exception {

            perform(get(USERS_USER_REDIRECT_URL_TEMPLATE, userLocalId.value())
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL)))
            )
                .andExpect(view().name("forward:/users/%s/overtime-account".formatted(userLocalId.value())));
        }

        @Test
        void ensureUserForwardsToPermissions() throws Exception {

            perform(get(USERS_USER_REDIRECT_URL_TEMPLATE, userLocalId.value())
                .with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_PERMISSIONS_EDIT_ALL)))
            )
                .andExpect(view().name("forward:/users/%s/permissions".formatted(userLocalId.value())));
        }
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
