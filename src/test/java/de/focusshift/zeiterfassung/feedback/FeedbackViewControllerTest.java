package de.focusshift.zeiterfassung.feedback;


import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackViewControllerTest {

    private FeedbackViewController sut;

    @Mock
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        sut = new FeedbackViewController(feedbackService);
    }

    @Test
    void feedback() {
        final FeedbackDto feedbackDto = new FeedbackDto("body");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("referer", "someReferer");
        final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        final String redirect = sut.feedback(feedbackDto, getDefaultOidcUser("subject"), request, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("user-feedback-given")).isEqualTo(true);
        assertThat(redirect).isEqualTo("redirect:someReferer");

        verify(feedbackService).sendFeedback(new EMailAddress("wayne@example.org"), "body");
    }

    private OidcUser getDefaultOidcUser(String subject) {
        final OidcIdToken token = OidcIdToken.withTokenValue("tokenvalue").claim("claimName", "yeeehaw").subject(subject).build();
        final OidcUserInfo userInfo = OidcUserInfo.builder().name("Bruce").subject(subject).email("wayne@example.org").build();
        return new DefaultOidcUser(emptyList(), token, userInfo);
    }
}
