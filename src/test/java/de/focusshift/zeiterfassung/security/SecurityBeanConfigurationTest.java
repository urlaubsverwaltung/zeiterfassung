package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.JWT_BEARER;

class SecurityBeanConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withPropertyValues("zeiterfassung.security.oidc.login-form-url=http://localhost")
        .withUserConfiguration(SecurityBeanConfiguration.class)
        .withBean(InMemoryClientRegistrationRepository.class, anyClientRegistration())
        .withBean(TenantContextHolder.class, NoopTenantContextHolder::new)
        .withBean(TenantUserService.class, NoopTenantUserService::new);

    @Test
    void ensureUserServiceForSingleTenantWhenMissing() {
        applicationContextRunner
            .run(context -> {
                assertThat(context).getBean(OAuth2UserService.class).extracting("delegate")
                    .isInstanceOf(OAuth2UserServiceSingleTenant.class);
            });
    }

    @Test
    void ensureUserServiceForSingleTenantWhenSet() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.tenant.mode=single")
            .run(context -> {
                assertThat(context).getBean(OAuth2UserService.class).extracting("delegate")
                    .isInstanceOf(OAuth2UserServiceSingleTenant.class);
            });
    }

    @Test
    void ensureUserServiceForMultiTenant() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.tenant.mode=multi")
            .run(context -> {
                assertThat(context).getBean(OAuth2UserService.class).extracting("delegate")
                    .isInstanceOf(OAuth2UserServiceMultiTenant.class);
            });
    }

    private ClientRegistration anyClientRegistration() {
        return ClientRegistration.withRegistrationId("realm").authorizationGrantType(JWT_BEARER).build();
    }

    private static class NoopTenantContextHolder implements TenantContextHolder {

    }

    private static class NoopTenantUserService implements TenantUserService {
        @Override
        public TenantUser createNewUser(String uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRole> authorities) {
            return null;
        }

        @Override
        public TenantUser updateUser(TenantUser user) {
            return null;
        }

        @Override
        public List<TenantUser> findAllUsers() {
            return null;
        }

        @Override
        public List<TenantUser> findAllUsers(String query) {
            return null;
        }

        @Override
        public List<TenantUser> findAllUsersById(Collection<UserId> userIds) {
            return null;
        }

        @Override
        public List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds) {
            return null;
        }

        @Override
        public Optional<TenantUser> findById(UserId userId) {
            return Optional.empty();
        }

        @Override
        public Optional<TenantUser> findByLocalId(UserLocalId localId) {
            return Optional.empty();
        }

        @Override
        public void deleteUser(Long id) {

        }

        @Override
        public void activateUser(Long id) {

        }

        @Override
        public void deactivateUser(Long id) {

        }
    }
}

