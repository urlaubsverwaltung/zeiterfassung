package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest implements ControllerTest {

    private ReportController sut;

    @Mock
    private UserSearchViewHelper userSearchViewHelper;

    @BeforeEach
    void setUp() {
        sut = new ReportController(userSearchViewHelper);
    }

    @Test
    void ensureReportForwardsToTodayWeekReport() throws Exception {
        perform(get("/report")).andExpect(forwardedUrl("/report/week"));
    }

    @Test
    void ensureUserSearchReturnsSuggestionsFrameWithJavaScript() throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final CurrentOidcUser oidcUser = currentOidcUser(userIdComposite, List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_VIEW_REPORT_ALL));

        when(userSearchViewHelper.getSuggestionFragment(eq("super"), eq(oidcUser), any(Model.class), any(java.util.function.Function.class)))
            .thenReturn(new ModelAndView("user-search-view"));

        perform(get("/report")
            .with(oidcLogin().oidcUser(oidcUser))
            .header("Turbo-Frame", "frame-users-suggestions")
            .param("query", "super")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("user-search-view"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
