package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkDurationCalculationServiceTest {

    private WorkDurationCalculationService sut;

    @Mock
    private WorkDurationSubtractBreakCalculationService subtractBreakCalculationService;

    @BeforeEach
    void setUp() {
        sut = new WorkDurationCalculationService(subtractBreakCalculationService);
    }

    @Nested
    class CalculateWorkDuration {

        @Test
        void delegatesToSubtractBreakCalculation() {

            final TimeEntry timeEntry = anyTimeEntry();

            final WorkDuration workDuration = new WorkDuration(Duration.ofMinutes(42));
            when(subtractBreakCalculationService.calculateWorkDuration(List.of(timeEntry))).thenReturn(workDuration);

            final WorkDuration actual = sut.calculateWorkDuration(List.of(timeEntry));
            assertThat(actual).isEqualTo(workDuration);
        }
    }

    private static TimeEntry anyTimeEntry() {
        final ZonedDateTime now = ZonedDateTime.now();
        return new TimeEntry(new TimeEntryId(1L), anyUserIdComposite(), "", now.minusMinutes(60), now, false);
    }

    private static UserIdComposite anyUserIdComposite() {
        return new UserIdComposite(new UserId("user-id"), new UserLocalId(1L));
    }
}
