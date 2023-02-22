package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServicePermissionAwareTest {

    private ReportServicePermissionAware sut;

    @Mock
    private ReportPermissionService reportPermissionService;
    @Mock
    private ReportServiceRaw reportServiceRaw;
    @Mock
    private UserDateService userDateService;

    @BeforeEach
    void setUp() {
        sut = new ReportServicePermissionAware(reportPermissionService, reportServiceRaw, userDateService);
    }

    @Test
    void getReportWeekForMultipleUsersWhenCurrentUserHasNoPermissionForAnyGivenOne() {

        final List<UserLocalId> userIds = List.of(
            new UserLocalId(1L),
            new UserLocalId(2L)
        );

        when(reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userIds)).thenReturn(List.of());

        when(userDateService.firstDayOfWeek(Year.of(2023), 7))
            .thenReturn(LocalDate.of(2023, 2, 13));

        final ReportWeek actual = sut.getReportWeek(Year.of(2023), 7, userIds);

        assertThat(actual).isNotNull();
        assertThat(actual.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 13));
        assertThat(actual.reportDays()).containsExactly(
            new ReportDay(LocalDate.of(2023, 2, 13), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 14), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO, List.of()),
            new ReportDay(LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO, List.of())
        );
    }

    @Test
    void getReportMonthForMultipleUsersWhenCurrentUserHasNoPermissionForAnyGivenOne() {

        final List<UserLocalId> userIds = List.of(
            new UserLocalId(1L),
            new UserLocalId(2L)
        );

        when(reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userIds))
            .thenReturn(List.of());

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2023, 2, 1)))
            .thenReturn(LocalDate.of(2023, 1, 30));

        final ReportMonth actual = sut.getReportMonth(YearMonth.of(2023, 2), userIds);

        assertThat(actual).isNotNull();
        assertThat(actual.yearMonth()).isEqualTo(YearMonth.of(2023, 2));
        assertThat(actual.weeks()).hasSize(5);

        assertThat(actual.weeks().get(0)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 1, 30));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 1, 30), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 1, 31), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 1), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 2), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 3), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 4), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 5), PlannedWorkingHours.ZERO, List.of())
            );
        });

        assertThat(actual.weeks().get(1)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 6));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 6), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 7), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 8), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 9), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 10), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 11), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 12), PlannedWorkingHours.ZERO, List.of())
            );
        });

        assertThat(actual.weeks().get(2)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 13));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 13), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 14), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO, List.of())
            );
        });

        assertThat(actual.weeks().get(3)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 20));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 20), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 21), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 22), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 23), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 24), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 25), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 26), PlannedWorkingHours.ZERO, List.of())
            );
        });

        assertThat(actual.weeks().get(4)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 27));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 27), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 2, 28), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 3, 1), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 3, 2), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 3, 3), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 3, 4), PlannedWorkingHours.ZERO, List.of()),
                new ReportDay(LocalDate.of(2023, 3, 5), PlannedWorkingHours.ZERO, List.of())
            );
        });
    }
}
