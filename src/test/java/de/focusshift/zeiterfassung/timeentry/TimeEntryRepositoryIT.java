package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Transactional
class TimeEntryRepositoryIT extends SingleTenantTestContainersBase {

    @Autowired
    private TimeEntryRepository sut;

    @Autowired
    private TenantUserService tenantUserService;
    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");
    }

    @Test
    void countAllByOwner() {

        final TenantUser batman = tenantUserService.createNewUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final TenantUser superman = tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());

        final LocalDateTime anyDate = LocalDateTime.of(2022, 9, 24, 14, 0, 0, 0);

        final TimeEntryEntity timeEntryOneBatman = createTimeEntryEntity(batman.id(), "hard work", anyDate, anyDate);
        final TimeEntryEntity timeEntryTwoBatman = createTimeEntryEntity(batman.id(), "even more hard work", anyDate, anyDate);
        final TimeEntryEntity timeEntrySuperman = createTimeEntryEntity(superman.id(), "", anyDate, anyDate);

        sut.save(timeEntryOneBatman);
        sut.save(timeEntryTwoBatman);
        sut.save(timeEntrySuperman);

        assertThat(sut.countAllByOwner(batman.id())).isEqualTo(2);
        assertThat(sut.countAllByOwner(superman.id())).isEqualTo(1);
    }

    @Test
    void countAllEnsureFindAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc() {

        final TenantUser batman = tenantUserService.createNewUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final TenantUser superman = tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastStartFutureEnd = LocalDateTime.of(periodFrom.plusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPastStart = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastStartFutureEnd);

        final LocalDateTime matchStartAtPeriodStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime matchEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work period start", matchStartAtPeriodStart, matchEnd);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartAtPeriodStart, matchEnd);

        final LocalDateTime matchStartBetween = LocalDateTime.of(periodFrom.plusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime matchStartBetweenFutureEnd = LocalDateTime.of(periodFrom.plusDays(1), LocalTime.of(10, 0, 0));
        final TimeEntryEntity entityMatchBetween = createTimeEntryEntity(batman.id(), "hard work in between", matchStartBetween, matchStartBetweenFutureEnd);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.saveAll(List.of(entityPastStart, entityMatch, entityMatchDifferentOwner, entityMatchBetween, entityFuture));

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(2);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work in between");
        assertThat(actualEntries.get(1).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(1).getComment()).isEqualTo("hard work period start");
    }

    private static TimeEntryEntity createTimeEntryEntity(String owner, String comment, LocalDateTime start, LocalDateTime end) {
        return new TimeEntryEntity(null, owner, comment, start.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), end.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), Instant.now(), false);
    }

    private static void prepareSecurityContextWithTenantId(final String tenantId) {
        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, List.of(), tenantId);

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
