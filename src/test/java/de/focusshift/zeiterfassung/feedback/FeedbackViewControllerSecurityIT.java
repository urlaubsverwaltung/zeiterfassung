package de.focusshift.zeiterfassung.feedback;


import de.focusshift.zeiterfassung.TestContainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackViewControllerSecurityIT extends TestContainersBase {

    @MockBean
    private FeedbackService feedbackService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ensureFeedbackCanOnlySendWithCorrectRole() throws Exception {
        mockMvc
            .perform(
                post("/feedback")
                    .with(csrf())
                    .with(oidcLogin().userInfoToken(builder -> builder.subject("subject").email("wayne@example.org"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ZEITERFASSUNG_USER")))
            )
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void ensureFeedbackCanNotBeSentWithoutRole() throws Exception {
        mockMvc
            .perform(
                post("/feedback")
                    .with(csrf())
                    .with(oidcLogin().userInfoToken(builder -> builder.subject("subject").email("wayne@example.org")))
            )
            .andExpect(status().isForbidden());
    }
}
