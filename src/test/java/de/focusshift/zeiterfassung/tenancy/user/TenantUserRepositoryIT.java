package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Rollback(value = false)
class TenantUserRepositoryIT extends SingleTenantTestContainersBase {

    @Autowired
    private TenantUserRepository sut;

    @Autowired
    private TenantUserService tenantUserService;

    @AfterEach
    void tearDown() {
        sut.deleteAll();
    }

    @Test
    void ensureFindAllByUuidIsInReturnsEmpty() {

        tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByUuidIsIn(List.of("uuid"));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllByUuidIsIn() {

        final TenantUser bruce = tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final TenantUser clark = tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());
        tenantUserService.createNewUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Clark", "Kent", new EMailAddress("kent@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByUuidIsIn(List.of(bruce.id(), clark.id()));
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Bruce");
            assertThat(entity.getFamilyName()).isEqualTo("Wayne");
        });
        assertThat(actual.get(1)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Kent");
            assertThat(entity.getFamilyName()).isEqualTo("Clark");
        });
    }

    @Test
    void ensureFindAllByIdIsInReturnsEmpty() {

        tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByIdIsIn(List.of(42L));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllByIdIsIn() {

        final TenantUser bruce = tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final TenantUser clark = tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());
        tenantUserService.createNewUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Clark", "Kent", new EMailAddress("kent@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByIdIsIn(List.of(bruce.localId(), clark.localId()));
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Bruce");
            assertThat(entity.getFamilyName()).isEqualTo("Wayne");
        });
        assertThat(actual.get(1)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Kent");
            assertThat(entity.getFamilyName()).isEqualTo("Clark");
        });
    }

    @Test
    void ensureFindAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCaseReturnsEmpty() {

        tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase("xxx", "xxx");
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase() {

        tenantUserService.createNewUser("8b913da0-2711-4da8-9216-9904e11944ac", "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        tenantUserService.createNewUser("2256a744-31f9-4f87-8189-fe0d471e6537", "Kent", "Clark", new EMailAddress("Clark@example.org"), Set.of());
        tenantUserService.createNewUser("1a432ba3-cb93-463b-813b-8e065c1e0a24", "Clark", "Kent", new EMailAddress("Kent@example.org"), Set.of());

        final List<TenantUserEntity> actual = sut.findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase("cla", "cla");
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Kent");
            assertThat(entity.getFamilyName()).isEqualTo("Clark");
        });
        assertThat(actual.get(1)).satisfies(entity -> {
            assertThat(entity.getGivenName()).isEqualTo("Clark");
            assertThat(entity.getFamilyName()).isEqualTo("Kent");
        });
    }
}
