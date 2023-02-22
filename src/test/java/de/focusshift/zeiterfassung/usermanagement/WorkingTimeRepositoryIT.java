package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.TestContainersBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkingTimeRepositoryIT extends TestContainersBase {

    @Autowired
    private WorkingTimeRepository sut;

    @Autowired
    private WorkingTimeService workingTimeService;

    @AfterEach
    void tearDown() {
        sut.deleteAll();
    }

    @Test
    void ensureFindAllByUserIdIsIn() {

        workingTimeService.updateWorkingTime(
            WorkingTime.builder()
                .userId(new UserLocalId(1L))
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
            WorkingTime.builder()
                .userId(new UserLocalId(2L))
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
            WorkingTime.builder()
                .userId(new UserLocalId(3L))
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
