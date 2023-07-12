package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentTenantInterceptorTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    @InjectMocks
    private CurrentTenantInterceptor sut;

    @Test
    void testPreHandleWithOAuthToken() {

        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, List.of(), "a154bc4e");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(authentication);

        sut.preHandle(request, mock(HttpServletResponse.class), null);

        verify(tenantContextHolder).setTenantId(new TenantId("a154bc4e"));
        verifyNoMoreInteractions(tenantContextHolder);
    }

    @Test
    void testPreHandleWithoutAuthorizedClientRegistrationId() {

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(authentication);

        sut.preHandle(request, mock(HttpServletResponse.class), null);

        verify(tenantContextHolder, never()).setTenantId(any());
        verify(tenantContextHolder).clear();
    }

    @Test
    void testPreHandleWithoutOAuth2AuthenticationToken() {

        final UsernamePasswordAuthenticationToken authentication = mock(UsernamePasswordAuthenticationToken.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getUserPrincipal()).thenReturn(authentication);

        sut.preHandle(request, mock(HttpServletResponse.class), null);

        verify(tenantContextHolder, never()).setTenantId(any());
    }

    @Test
    void testpostHandle() {

        sut.postHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), null, null);

        verify(tenantContextHolder, never()).setTenantId(any());
        verify(tenantContextHolder).clear();
    }
}
