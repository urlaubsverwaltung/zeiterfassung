package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WorkingTimeRepositoryIT extends TestContainersBase {

    @Autowired
    private WorkingTimeRepository sut;

    @Autowired
    private TenantUserService tenantUserService;

    @AfterEach
    void tearDown() {
        sut.deleteAll();
    }

    @Test
    void ensureFindAllByUserIdIsIn() {

        final TenantUser tenantUser_1 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());
        final TenantUser tenantUser_2 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());
        final TenantUser tenantUser_3 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());

        final WorkingTimeEntity workingTimeEntity_1 = new WorkingTimeEntity();
        workingTimeEntity_1.setUserId(tenantUser_1.localId());
        workingTimeEntity_1.setMonday(Duration.ofHours(1).toString());
        workingTimeEntity_1.setTuesday(Duration.ofHours(2).toString());
        workingTimeEntity_1.setWednesday(Duration.ofHours(3).toString());
        workingTimeEntity_1.setThursday(Duration.ofHours(4).toString());
        workingTimeEntity_1.setFriday(Duration.ofHours(5).toString());
        workingTimeEntity_1.setSaturday(Duration.ofHours(6).toString());
        workingTimeEntity_1.setSunday(Duration.ofHours(7).toString());

        final WorkingTimeEntity workingTimeEntity_2 = new WorkingTimeEntity();
        workingTimeEntity_2.setUserId(tenantUser_2.localId());
        workingTimeEntity_2.setMonday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setTuesday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setWednesday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setThursday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setFriday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setSaturday(Duration.ofHours(10).toString());
        workingTimeEntity_2.setSunday(Duration.ofHours(10).toString());

        final WorkingTimeEntity workingTimeEntity_3 = new WorkingTimeEntity();
        workingTimeEntity_3.setUserId(tenantUser_3.localId());
        workingTimeEntity_3.setMonday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setTuesday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setWednesday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setThursday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setFriday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setSaturday(Duration.ofHours(8).toString());
        workingTimeEntity_3.setSunday(Duration.ofHours(8).toString());

        sut.save(workingTimeEntity_1);
        sut.save(workingTimeEntity_2);
        sut.save(workingTimeEntity_3);

        final List<WorkingTimeEntity> actual = sut.findAllByUserIdIsIn(List.of(tenantUser_1.localId(), tenantUser_3.localId()));

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).satisfies(entity -> {
            assertThat(entity.getUserId()).isEqualTo(tenantUser_1.localId());
            assertThat(entity.getMonday()).isEqualTo("PT1H");
            assertThat(entity.getTuesday()).isEqualTo("PT2H");
            assertThat(entity.getWednesday()).isEqualTo("PT3H");
            assertThat(entity.getThursday()).isEqualTo("PT4H");
            assertThat(entity.getFriday()).isEqualTo("PT5H");
            assertThat(entity.getSaturday()).isEqualTo("PT6H");
            assertThat(entity.getSunday()).isEqualTo("PT7H");
        });
        assertThat(actual.get(1)).satisfies(entity -> {
            assertThat(entity.getUserId()).isEqualTo(tenantUser_3.localId());
            assertThat(entity.getMonday()).isEqualTo("PT8H");
            assertThat(entity.getTuesday()).isEqualTo("PT8H");
            assertThat(entity.getWednesday()).isEqualTo("PT8H");
            assertThat(entity.getThursday()).isEqualTo("PT8H");
            assertThat(entity.getFriday()).isEqualTo("PT8H");
            assertThat(entity.getSaturday()).isEqualTo("PT8H");
            assertThat(entity.getSunday()).isEqualTo("PT8H");
        });
    }

    @Test
    void ensureFindByUserIdAndValidFromIsNullOrIsBetween() {

        final LocalDate validFrom = LocalDate.of(2023, 12, 4);
        final LocalDate exclusive = validFrom.plusWeeks(1);

        final TenantUser tenantUser = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());
        final Long userId = tenantUser.localId();

        final WorkingTimeEntity firstWorkingTimeEntity = anyWorkingTimeEntity(userId, null);
        final WorkingTimeEntity workingTimeEntity_equal = anyWorkingTimeEntity(userId, validFrom);
        final WorkingTimeEntity workingTimeEntity_between = anyWorkingTimeEntity(userId, exclusive.minusDays(1));
        final WorkingTimeEntity workingTimeEntity_exclusive = anyWorkingTimeEntity(userId, exclusive);

        sut.save(firstWorkingTimeEntity);
        sut.save(workingTimeEntity_equal);
        sut.save(workingTimeEntity_between);
        sut.save(workingTimeEntity_exclusive);

        final List<WorkingTimeEntity> actual = sut.findByUserIdAndValidFromIsNullOrIsBetween(userId, validFrom, exclusive);

        assertThat(actual)
            .containsExactlyInAnyOrder(firstWorkingTimeEntity, workingTimeEntity_equal, workingTimeEntity_between);
    }

    private WorkingTimeEntity anyWorkingTimeEntity(Long userId, LocalDate validFrom) {

        final String durationZero = Duration.ofHours(0).toString();

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(userId);
        entity.setValidFrom(validFrom);
        entity.setMonday(durationZero);
        entity.setTuesday(durationZero);
        entity.setWednesday(durationZero);
        entity.setThursday(durationZero);
        entity.setFriday(durationZero);
        entity.setSaturday(durationZero);
        entity.setSunday(durationZero);

        return entity;
    }
}
