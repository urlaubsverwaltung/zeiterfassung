package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthoritiesModelProviderTest {

    private AuthoritiesModelProvider sut;

    @BeforeEach
    void setUp() {
        sut = new AuthoritiesModelProvider();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = { "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL", "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL" })
    void ensureShowMainNavigationPersonsIsTrueForAuthority(SecurityRole securityRole) {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(securityRole.name()));
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, authorities, "client-registration-id");
        request.setUserPrincipal(authentication);

        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("any-view-name");

        sut.postHandle(request, response, new Object(), modelAndView);

        assertThat(modelAndView.getModelMap()).containsEntry("showMainNavigationPersons", true);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = { "ZEITERFASSUNG_USER", "ZEITERFASSUNG_VIEW_REPORT_ALL" })
    void ensureShowMainNavigationPersonsIsFalseForAuthority(SecurityRole securityRole) {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(securityRole.name()));
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, authorities, "client-registration-id");
        request.setUserPrincipal(authentication);

        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("any-view-name");

        sut.postHandle(request, response, new Object(), modelAndView);

        assertThat(modelAndView.getModelMap()).containsEntry("showMainNavigationPersons", false);
    }
}
