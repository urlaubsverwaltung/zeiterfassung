package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkTimeServiceImplTest {

    private WorkTimeServiceImpl sut;

    @Mock
    private WorkingTimeRepository workingTimeRepository;

    @BeforeEach
    void setUp() {
        sut = new WorkTimeServiceImpl(workingTimeRepository);
    }

    @Test
    void ensureWorkingTimeByUser() {

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(42L);
        entity.setMonday("PT1H");
        entity.setTuesday("PT2H");
        entity.setWednesday("PT3H");
        entity.setThursday("PT4H");
        entity.setFriday("PT5H");
        entity.setSaturday("PT6H");
        entity.setSunday("PT7H");

        when(workingTimeRepository.findByUserId(42L)).thenReturn(Optional.of(entity));

        final WorkingTime actual = sut.getWorkingTimeByUser(new UserLocalId(42L));

        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).hasValue(WorkDay.tuesday(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).hasValue(WorkDay.wednesday(Duration.ofHours(3)));
        assertThat(actual.getThursday()).hasValue(WorkDay.thursday(Duration.ofHours(4)));
        assertThat(actual.getFriday()).hasValue(WorkDay.friday(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).hasValue(WorkDay.saturday(Duration.ofHours(6)));
        assertThat(actual.getSunday()).hasValue(WorkDay.sunday(Duration.ofHours(7)));
    }

    @Test
    void ensureWorkingTimeByUserReturnsDefault() {

        when(workingTimeRepository.findByUserId(42L)).thenReturn(Optional.empty());

        final WorkingTime actual = sut.getWorkingTimeByUser(new UserLocalId(42L));

        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(WorkDay.monday(Duration.ofHours(8)));
        assertThat(actual.getTuesday()).hasValue(WorkDay.tuesday(Duration.ofHours(8)));
        assertThat(actual.getWednesday()).hasValue(WorkDay.wednesday(Duration.ofHours(8)));
        assertThat(actual.getThursday()).hasValue(WorkDay.thursday(Duration.ofHours(8)));
        assertThat(actual.getFriday()).hasValue(WorkDay.friday(Duration.ofHours(8)));
        assertThat(actual.getSaturday()).hasValue(WorkDay.saturday(Duration.ZERO));
        assertThat(actual.getSunday()).hasValue(WorkDay.sunday(Duration.ZERO));
    }

    @Test
    void ensureUpdateWorkingTime() {

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setUserId(42L);
        entity.setMonday("PT24H");
        entity.setTuesday("PT24H");
        entity.setWednesday("PT24H");
        entity.setThursday("PT24H");
        entity.setFriday("PT24H");
        entity.setSaturday("PT24H");
        entity.setSunday("PT24H");

        when(workingTimeRepository.findByUserId(42L)).thenReturn(Optional.of(entity));
        when(workingTimeRepository.save(any())).thenAnswer(returnsFirstArg());

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(BigDecimal.valueOf(1))
            .tuesday(BigDecimal.valueOf(2))
            .wednesday(BigDecimal.valueOf(3))
            .thursday(BigDecimal.valueOf(4))
            .friday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .sunday(BigDecimal.valueOf(7))
            .build();

        final WorkingTime actual = sut.updateWorkingTime(workingTime);

        assertThat(actual).isNotSameAs(workingTime);
        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).hasValue(WorkDay.tuesday(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).hasValue(WorkDay.wednesday(Duration.ofHours(3)));
        assertThat(actual.getThursday()).hasValue(WorkDay.thursday(Duration.ofHours(4)));
        assertThat(actual.getFriday()).hasValue(WorkDay.friday(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).hasValue(WorkDay.saturday(Duration.ofHours(6)));
        assertThat(actual.getSunday()).hasValue(WorkDay.sunday(Duration.ofHours(7)));

        final ArgumentCaptor<WorkingTimeEntity> captor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(captor.capture());

        final WorkingTimeEntity actualEntity = captor.getValue();
        assertThat(actualEntity.getUserId()).isEqualTo(42L);
        assertThat(actualEntity.getMonday()).isEqualTo("PT1H");
        assertThat(actualEntity.getTuesday()).isEqualTo("PT2H");
        assertThat(actualEntity.getWednesday()).isEqualTo("PT3H");
        assertThat(actualEntity.getThursday()).isEqualTo("PT4H");
        assertThat(actualEntity.getFriday()).isEqualTo("PT5H");
        assertThat(actualEntity.getSaturday()).isEqualTo("PT6H");
        assertThat(actualEntity.getSunday()).isEqualTo("PT7H");
    }

    @Test
    void ensureUpdateWorkingTimeWithNewItem() {

        when(workingTimeRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(workingTimeRepository.save(any())).thenAnswer(returnsFirstArg());

        final WorkingTime workingTime = WorkingTime.builder()
            .userId(new UserLocalId(42L))
            .monday(BigDecimal.valueOf(1))
            .tuesday(BigDecimal.valueOf(2))
            .wednesday(BigDecimal.valueOf(3))
            .thursday(BigDecimal.valueOf(4))
            .friday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .sunday(BigDecimal.valueOf(7))
            .build();

        final WorkingTime actual = sut.updateWorkingTime(workingTime);

        assertThat(actual).isNotSameAs(workingTime);
        assertThat(actual.getUserId()).isEqualTo(new UserLocalId(42L));
        assertThat(actual.getMonday()).hasValue(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).hasValue(WorkDay.tuesday(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).hasValue(WorkDay.wednesday(Duration.ofHours(3)));
        assertThat(actual.getThursday()).hasValue(WorkDay.thursday(Duration.ofHours(4)));
        assertThat(actual.getFriday()).hasValue(WorkDay.friday(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).hasValue(WorkDay.saturday(Duration.ofHours(6)));
        assertThat(actual.getSunday()).hasValue(WorkDay.sunday(Duration.ofHours(7)));

        final ArgumentCaptor<WorkingTimeEntity> captor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(captor.capture());

        final WorkingTimeEntity actualEntity = captor.getValue();
        assertThat(actualEntity.getUserId()).isEqualTo(42L);
        assertThat(actualEntity.getMonday()).isEqualTo("PT1H");
        assertThat(actualEntity.getTuesday()).isEqualTo("PT2H");
        assertThat(actualEntity.getWednesday()).isEqualTo("PT3H");
        assertThat(actualEntity.getThursday()).isEqualTo("PT4H");
        assertThat(actualEntity.getFriday()).isEqualTo("PT5H");
        assertThat(actualEntity.getSaturday()).isEqualTo("PT6H");
        assertThat(actualEntity.getSunday()).isEqualTo("PT7H");
    }
}
