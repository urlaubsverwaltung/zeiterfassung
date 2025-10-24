package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettings;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkDurationCalculationServiceTest {

    private WorkDurationCalculationService sut;

    @Mock
    private SimpleWorkDurationCalculator simpleWorkDurationCalculator;
    @Mock
    private OverlappingBreakCalculator subtractOverlappingBreakCalculator;

    @BeforeEach
    void setUp() {
        sut = new WorkDurationCalculationService(simpleWorkDurationCalculator, subtractOverlappingBreakCalculator);
    }

    @Nested
    class CalculateWorkDuration {

        @Test
        void delegatesToSubtractOverlappingBreakCalculation() {

            final TimeEntry timeEntry = anyTimeEntry();

            final WorkDuration workDuration = new WorkDuration(Duration.ofMinutes(42));
            when(subtractOverlappingBreakCalculator.calculateWorkDuration(List.of(timeEntry))).thenReturn(workDuration);

            final WorkDuration actual = sut.calculateWorkDuration(List.of(timeEntry));
            assertThat(actual).isEqualTo(workDuration);
        }
    }

    @Nested
    class CalculateWorkDurationWithSettings {

        @Test
        void delegatesToSimpleCalculationBecauseFeatureDisabled() {

            final TimeEntry timeEntry = anyTimeEntry();
            final SubtractBreakFromTimeEntrySettings settings = new SubtractBreakFromTimeEntrySettings(false, null);

            final WorkDuration workDuration = new WorkDuration(Duration.ofMinutes(42));
            when(simpleWorkDurationCalculator.calculateWorkDuration(List.of(timeEntry))).thenReturn(workDuration);

            final WorkDuration actual = sut.calculateWorkDuration(settings, List.of(timeEntry));
            assertThat(actual).isEqualTo(workDuration);

            verifyNoInteractions(subtractOverlappingBreakCalculator);
        }

        @Test
        void delegatesToSubtractBreakBecauseFeatureEnabled() {

            final Instant featureEnabledTimestamp = Instant.parse("2025-01-01T00:00:00Z");

            final TimeEntry workEntry = workEntry("2025-10-24T08:00:00Z", "2025-10-24T09:00:00Z");
            final SubtractBreakFromTimeEntrySettings settings = new SubtractBreakFromTimeEntrySettings(true, featureEnabledTimestamp);

            final WorkDuration workDuration = new WorkDuration(Duration.ofMinutes(42));
            when(subtractOverlappingBreakCalculator.calculateWorkDuration(List.of(workEntry))).thenReturn(workDuration);

            final WorkDuration actual = sut.calculateWorkDuration(settings, List.of(workEntry));
            assertThat(actual).isEqualTo(workDuration);

            verifyNoInteractions(simpleWorkDurationCalculator);
        }

        @Test
        void delegatesSubsetToSimpleCalculationAndSubsetToSubtractBreak() {

            final Instant featureEnabledTimestamp = Instant.parse("2025-10-24T00:00:00Z");

            final TimeEntry workEntry1 = workEntry("2025-10-23T08:00:00Z", "2025-10-23T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-10-24T08:00:00Z", "2025-10-24T09:00:00Z");
            final SubtractBreakFromTimeEntrySettings settings = new SubtractBreakFromTimeEntrySettings(true , featureEnabledTimestamp);

            final WorkDuration workDuration1 = new WorkDuration(Duration.ofMinutes(1));
            when(simpleWorkDurationCalculator.calculateWorkDuration(List.of(workEntry1))).thenReturn(workDuration1);

            final WorkDuration workDuration2 = new WorkDuration(Duration.ofMinutes(2));
            when(subtractOverlappingBreakCalculator.calculateWorkDuration(List.of(workEntry2))).thenReturn(workDuration2);

            final WorkDuration actual = sut.calculateWorkDuration(settings, List.of(workEntry1, workEntry2));
            assertThat(actual).isEqualTo(workDuration1.plus(workDuration2));
        }
    }

    private static TimeEntry anyTimeEntry() {
        return workEntry("2025-10-24T08:00:00Z", "2025-10-24T17:00:00Z");
    }

    private static TimeEntry workEntry(String start, String end) {
        return new TimeEntry(new TimeEntryId(1L), anyUserIdComposite(), "", ZonedDateTime.parse(start), ZonedDateTime.parse(end), false);
    }

    private static UserIdComposite anyUserIdComposite() {
        return new UserIdComposite(new UserId("user-id"), new UserLocalId(1L));
    }
}
