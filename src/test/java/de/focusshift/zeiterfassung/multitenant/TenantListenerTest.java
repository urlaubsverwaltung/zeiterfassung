package de.focusshift.zeiterfassung.multitenant;

import de.focusshift.zeiterfassung.tenantuser.TenantUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantListenerTest {

    private TenantListener sut;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void setUp() {
        sut = new TenantListener(tenantContextHolder);
    }

    @Test
    void ensureToSetTenantIdIfAvailable() {
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("a154bc4e")));

        final TenantAwareTestClass tenantAwareTestClass = new TenantAwareTestClass();
        sut.setTenant(tenantAwareTestClass);
        assertThat(tenantAwareTestClass.getTenantId()).isEqualTo("a154bc4e");
    }

    @Test
    void ensureExceptionIfNoTenantIdIsSet() {
        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        final TenantAwareTestClass tenantAwareTestClass = new TenantAwareTestClass();
        assertThatThrownBy(() -> sut.setTenant(tenantAwareTestClass)).isInstanceOf(MissingTenantException.class);
    }

    @Test
    void ensureDoesNotSetTenantIdIfEntityIsNotTenantAware() {
        final NotTenantAwareTestClass notTenantAwareTestClass = new NotTenantAwareTestClass();
        sut.setTenant(notTenantAwareTestClass);
        assertThat(notTenantAwareTestClass.getTenantId()).isNull();
    }

    private static class TenantAwareTestClass extends AbstractTenantAwareEntity {
        protected TenantAwareTestClass() {
            super(null);
        }
    }

    private static class NotTenantAwareTestClass {

        private String tenantId;

        protected NotTenantAwareTestClass() {
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
