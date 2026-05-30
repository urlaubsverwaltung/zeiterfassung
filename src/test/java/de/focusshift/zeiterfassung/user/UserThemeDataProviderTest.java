package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.ControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserThemeDataProviderTest implements ControllerTest {

    @Controller
    static class DummyController {
        @GetMapping("/test-endpoint")
        public String handleRequest() {
            return "dummy-view";
        }
    }

    private UserThemeDataProvider sut;

    @Mock
    private UserSettingsService userSettingsService;

    @BeforeEach
    void setUp() {
        sut = new UserThemeDataProvider(userSettingsService);
    }

    @Test
    void ensureNavigationCollapsedAndThemeModelAttributes() throws Exception {

        final UserIdComposite userIdComposite = anyUserIdComposite();
        when(userSettingsService.getUserSettings(userIdComposite))
            .thenReturn(new UserSettings(Theme.DARK, true, null, null));

        final MvcResult result = perform(
            get("/test-endpoint").with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER)))
        )
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getModelAndView().getModel())
            .containsEntry("theme", "dark")
            .containsEntry("navigationCollapsed", true);
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(new DummyController())
            .addInterceptors(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
