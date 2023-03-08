package de.focusshift.zeiterfassung.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LoginHintAwareResolverTest {

    private ClientRegistration clientRegistration;
    private InMemoryClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() {
        this.clientRegistration = ClientRegistration.withRegistrationId("registration").clientId("client").clientSecret("secret").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).scope("user").authorizationUri("https://provider.com/oauth2/authorize").tokenUri("https://provider.com/oauth2/token").userInfoUri("https://provider.com/oauth2/user").redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}").userNameAttributeName("id").clientName("client").build();

        this.clientRegistrationRepository = new InMemoryClientRegistrationRepository(this.clientRegistration);
    }

    @Test
    void authorizationRequestRedirectWithLoginHint() throws ServletException, IOException {

        String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + this.clientRegistration.getRegistrationId();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(requestUri);
        String loginHintParamName = "login_hint";
        request.addParameter(loginHintParamName, "office");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        OAuth2AuthorizationRequestResolver sut = new LoginHintAwareResolver(this.clientRegistrationRepository);
        OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(sut);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).matches("https://provider.com/oauth2/authorize\\?response_type=code&client_id=client&scope=user&state=.{15,}&redirect_uri=http://localhost/login/oauth2/code/registration&login_hint=office");
    }

    @Test
    void authorizationRequestRedirectWithoutLoginHint() throws ServletException, IOException {

        String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + this.clientRegistration.getRegistrationId();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(requestUri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        OAuth2AuthorizationRequestResolver sut = new LoginHintAwareResolver(this.clientRegistrationRepository);
        OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(sut);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).matches("https://provider.com/oauth2/authorize\\?response_type=code&client_id=client&scope=user&state=.{15,}&redirect_uri=http://localhost/login/oauth2/code/registration");
    }

    @Test
    void authorizationRequestRedirectWithUnknownParameter() throws ServletException, IOException {

        String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + this.clientRegistration.getRegistrationId();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(requestUri);
        String loginHintParamName = "some_parameter";
        request.addParameter(loginHintParamName, "foobar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        OAuth2AuthorizationRequestResolver sut = new LoginHintAwareResolver(this.clientRegistrationRepository);
        OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(sut);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).matches("https://provider.com/oauth2/authorize\\?response_type=code&client_id=client&scope=user&state=.{15,}&redirect_uri=http://localhost/login/oauth2/code/registration");
    }
}
