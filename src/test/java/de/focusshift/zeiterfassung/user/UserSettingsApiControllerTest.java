package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.ControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserSettingsApiControllerTest implements ControllerTest {

    private UserSettingsApiController sut;

    @Mock
    private UserSettingsService userSettingsService;

    @BeforeEach
    void setUp() {
        sut = new UserSettingsApiController(userSettingsService);
    }

    @Nested
    class UpdateSettings {

        @Test
        void ensureUpdateNavigationCollapsedReturnsNoContentWithoutUpdatingNavigationCollapsed() throws Exception {
            final UserIdComposite userIdComposite = anyUserIdComposite();

            perform(patch("/api/users/me/settings").with(oidcSubject(userIdComposite))
                .contentType(APPLICATION_JSON)
                .content(asJsonString(new UpdateUserSettingsDto(null)))
            )
                .andExpect(status().isNoContent());

            verifyNoInteractions(userSettingsService);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void ensureUpdateNavigationCollapsedReturnsNoContentAndPersists(boolean givenNavigationCollapsed) throws Exception {
            final UserIdComposite userIdComposite = anyUserIdComposite();

            perform(patch("/api/users/me/settings").with(oidcSubject(userIdComposite))
                .contentType(APPLICATION_JSON)
                .content(asJsonString(new UpdateUserSettingsDto(givenNavigationCollapsed)))
            )
                .andExpect(status().isNoContent());

            verify(userSettingsService).updateNavigationCollapsed(userIdComposite, givenNavigationCollapsed);
        }
    }

    public static String asJsonString(final Object obj) {
        try {
            return new JsonMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
