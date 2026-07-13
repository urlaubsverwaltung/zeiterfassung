package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Transactional
class TimeClockRepositoryIT extends SingleTenantTestContainersBase {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Autowired
    private TimeClockRepository sut;

    @Autowired
    private TenantService tenantService;
    @Autowired
    private TenantUserService tenantUserService;

    @PersistenceContext
    private EntityManager entityManager;

    private String batman;
    private String robin;

    @BeforeEach
    void setUp() {
        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        batman = createUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Bruce", "Wayne", "batman@example.org");
        robin = createUser("8b913da0-2711-4da8-9216-9904e11944ac", "Dick", "Grayson", "robin@example.org");
    }

    @Test
    void ensureSecondRunningTimeClockForSameOwnerIsRejected() {

        sut.save(runningTimeClock(batman));
        entityManager.flush();

        sut.save(runningTimeClock(batman));

        assertThatThrownBy(() -> entityManager.flush())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    void ensureRunningTimeClocksForDifferentOwnersAreAllowed() {

        sut.saveAll(List.of(runningTimeClock(batman), runningTimeClock(robin)));

        assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
    }

    @Test
    void ensureMultipleStoppedTimeClocksAndOneRunningForSameOwnerAreAllowed() {

        sut.saveAll(List.of(stoppedTimeClock(batman), stoppedTimeClock(batman), runningTimeClock(batman)));

        assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
    }

    private String createUser(String uuid, String givenName, String familyName, String email) {
        final TenantUser tenantUser = tenantUserService.createNewUser(uuid, givenName, familyName, new EMailAddress(email), Set.of());
        return tenantUser.id();
    }

    private static TimeClockEntity runningTimeClock(String owner) {
        return TimeClockEntity.builder()
            .owner(owner)
            .startedAt(Instant.now())
            .startedAtZoneId(UTC)
            .comment("")
            .isBreak(false)
            .build();
    }

    private static TimeClockEntity stoppedTimeClock(String owner) {
        final Instant now = Instant.now();
        return TimeClockEntity.builder()
            .owner(owner)
            .startedAt(now.minusSeconds(120))
            .startedAtZoneId(UTC)
            .stoppedAt(now)
            .stoppedAtZoneId(UTC)
            .comment("")
            .isBreak(false)
            .build();
    }

    private static void prepareSecurityContextWithTenantId(final String tenantId) {
        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, List.of(), tenantId);

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
