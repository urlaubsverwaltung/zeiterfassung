package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.annotation.Rollback;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(properties = "zeiterfassung.tenant.registration.enabled=false")
@Rollback(value = false)
class TimeEntryRepositoryIT extends TestContainersBase {

    @Autowired
    private TimeEntryRepository sut;

    @Autowired
    private TenantUserService tenantUserService;
    @Autowired
    private TenantService tenantService;

    @Test
    void countAllByOwner() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

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
    void ensureFindAllByOwnerForTimePeriodIncludesTimeEntryWithStartAtPeriodStartAndEndAfterPeriod() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastEnd = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPast = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastEnd);

        final LocalDateTime matchStartAtPeriodStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime matchEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work", matchStartAtPeriodStart, matchEnd);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartAtPeriodStart, matchEnd);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.save(entityPast);
        sut.save(entityMatch);
        sut.save(entityMatchDifferentOwner);
        sut.save(entityFuture);

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndTouchingPeriod(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(1);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work");

        sut.deleteAll();
        tenantUserService.deleteUser(batman.localId());
        tenantUserService.deleteUser(superman.localId());
        tenantService.delete("ab143c2f");
    }

    @Test
    void ensureFindAllByOwnerForTimePeriodIncludesTimeEntryWithStartWithinPeriodAndEndAfterPeriod() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastEnd = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPast = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastEnd);

        final LocalDateTime matchStartWithinPeriod = LocalDateTime.of(periodFrom.plusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime matchEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work", matchStartWithinPeriod, matchEnd);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartWithinPeriod, matchEnd);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.save(entityPast);
        sut.save(entityMatch);
        sut.save(entityMatchDifferentOwner);
        sut.save(entityFuture);

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndTouchingPeriod(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(1);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work");

        sut.deleteAll();
        tenantUserService.deleteUser(batman.localId());
        tenantUserService.deleteUser(superman.localId());
        tenantService.delete("ab143c2f");
    }

    @Test
    void ensureFindAllByOwnerForTimePeriodIncludesTimeEntryWithStartBeforePeriodAndEndAtPeriodStart() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastEnd = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPast = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastEnd);

        final LocalDateTime matchStartBeforePeriod = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime matchEndPeriodStart = LocalDateTime.of(periodFrom, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work", matchStartBeforePeriod, matchEndPeriodStart);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartBeforePeriod, matchEndPeriodStart);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.save(entityPast);
        sut.save(entityMatch);
        sut.save(entityMatchDifferentOwner);
        sut.save(entityFuture);

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndTouchingPeriod(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(1);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work");

        sut.deleteAll();
        tenantUserService.deleteUser(batman.localId());
        tenantUserService.deleteUser(superman.localId());
        tenantService.delete("ab143c2f");
    }

    @Test
    void ensureFindAllByOwnerForTimePeriodIncludesTimeEntryWithStartBeforePeriodAndEndWithinPeriod() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastEnd = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPast = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastEnd);

        final LocalDateTime matchStartBeforePeriod = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime matchEndWithinPeriod = LocalDateTime.of(periodFrom.plusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work", matchStartBeforePeriod, matchEndWithinPeriod);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartBeforePeriod, matchEndWithinPeriod);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.save(entityPast);
        sut.save(entityMatch);
        sut.save(entityMatchDifferentOwner);
        sut.save(entityFuture);

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndTouchingPeriod(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(1);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work");

        sut.deleteAll();
        tenantUserService.deleteUser(batman.localId());
        tenantUserService.deleteUser(superman.localId());
        tenantService.delete("ab143c2f");
    }

    @Test
    void ensureFindAllByOwnerForTimePeriodIncludesTimeEntryStartingBeforePeriodAndEndingAfterPeriod() {

        tenantService.create("ab143c2f");
        prepareSecurityContextWithTenantId("ab143c2f");

        final TenantUser batman = tenantUserService.createNewUser(UUID.randomUUID(), "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final TenantUser superman = tenantUserService.createNewUser(UUID.randomUUID(), "Kent", "Clark", new EMailAddress("Clark@example.org"));

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime pastStart = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime pastEnd = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityPast = createTimeEntryEntity(batman.id(), "hard work past", pastStart, pastEnd);

        final LocalDateTime matchStartBeforePeriod = LocalDateTime.of(periodFrom.minusDays(1), LocalTime.of(10, 0, 0));
        final LocalDateTime matchEndAfterPeriod = LocalDateTime.of(periodToExclusive.plusDays(1), LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityMatch = createTimeEntryEntity(batman.id(), "hard work", matchStartBeforePeriod, matchEndAfterPeriod);
        final TimeEntryEntity entityMatchDifferentOwner = createTimeEntryEntity(superman.id(), "", matchStartBeforePeriod, matchEndAfterPeriod);

        final LocalDateTime futureStart = LocalDateTime.of(periodToExclusive, LocalTime.of(10, 0, 0));
        final LocalDateTime futureEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity entityFuture = createTimeEntryEntity(batman.id(), "hard work future", futureStart, futureEnd);

        sut.save(entityPast);
        sut.save(entityMatch);
        sut.save(entityMatchDifferentOwner);
        sut.save(entityFuture);

        final Instant periodFromStartOfDayInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodToStartOfDayInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<TimeEntryEntity> actualEntries = sut.findAllByOwnerAndTouchingPeriod(batman.id(), periodFromStartOfDayInstant, periodToStartOfDayInstant);

        assertThat(actualEntries).hasSize(1);
        assertThat(actualEntries.get(0).getOwner()).isEqualTo(batman.id());
        assertThat(actualEntries.get(0).getComment()).isEqualTo("hard work");

        sut.deleteAll();
        tenantUserService.deleteUser(batman.localId());
        tenantUserService.deleteUser(superman.localId());
        tenantService.delete("ab143c2f");
    }

    private static TimeEntryEntity createTimeEntryEntity(String owner, String comment, LocalDateTime start, LocalDateTime end) {
        return new TimeEntryEntity(null, owner, comment, start.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), end.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), Instant.now());
    }

    private static void prepareSecurityContextWithTenantId(final String tenantId) {
        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, List.of(), tenantId);

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
