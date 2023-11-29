package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkingTimeRepositoryIT extends TestContainersBase {

    @Autowired
    private WorkingTimeRepository sut;

    @Autowired
    private WorkingTimeService workingTimeService;
    @MockBean
    private UserManagementService userManagementService;

    @AfterEach
    void tearDown() {
        sut.deleteAll();
    }

    @Test
    void ensureFindAllByUserIdIsIn() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "", "", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("uuid-2");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "", "", new EMailAddress(""), Set.of());

        final UserId userId_3 = new UserId("uuid-3");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);
        final User user_3 = new User(userIdComposite_3, "", "", new EMailAddress(""), Set.of());

        Mockito.when(userManagementService.findUserByLocalId(userLocalId_1)).thenReturn(Optional.of(user_1));
        Mockito.when(userManagementService.findUserByLocalId(userLocalId_2)).thenReturn(Optional.of(user_2));
        Mockito.when(userManagementService.findUserByLocalId(userLocalId_3)).thenReturn(Optional.of(user_3));

        workingTimeService.updateWorkingTime(
            userLocalId_1,
            WorkWeekUpdate.builder()
                .monday(1)
                .tuesday(2)
                .wednesday(3)
                .thursday(4)
                .friday(5)
                .saturday(6)
                .sunday(7)
                .build()
        );

        workingTimeService.updateWorkingTime(
            userLocalId_2,
            WorkWeekUpdate.builder()
                .monday(10)
                .tuesday(10)
                .wednesday(10)
                .thursday(10)
                .friday(10)
                .saturday(10)
                .sunday(10)
                .build()
        );

        workingTimeService.updateWorkingTime(
            userLocalId_3,
            WorkWeekUpdate.builder()
                .monday(8)
                .tuesday(8)
                .wednesday(8)
                .thursday(8)
                .friday(8)
                .saturday(8)
                .sunday(8)
                .build()
        );

        final List<WorkingTimeEntity> actual = sut.findAllByUserIdIsIn(List.of(1L, 3L));

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).satisfies(entity -> {
            assertThat(entity.getUserId()).isEqualTo(1L);
            assertThat(entity.getMonday()).isEqualTo("PT1H");
            assertThat(entity.getTuesday()).isEqualTo("PT2H");
            assertThat(entity.getWednesday()).isEqualTo("PT3H");
            assertThat(entity.getThursday()).isEqualTo("PT4H");
            assertThat(entity.getFriday()).isEqualTo("PT5H");
            assertThat(entity.getSaturday()).isEqualTo("PT6H");
            assertThat(entity.getSunday()).isEqualTo("PT7H");
        });
        assertThat(actual.get(1)).satisfies(entity -> {
            assertThat(entity.getUserId()).isEqualTo(3L);
            assertThat(entity.getMonday()).isEqualTo("PT8H");
            assertThat(entity.getTuesday()).isEqualTo("PT8H");
            assertThat(entity.getWednesday()).isEqualTo("PT8H");
            assertThat(entity.getThursday()).isEqualTo("PT8H");
            assertThat(entity.getFriday()).isEqualTo("PT8H");
            assertThat(entity.getSaturday()).isEqualTo("PT8H");
            assertThat(entity.getSunday()).isEqualTo("PT8H");
        });
    }
}
