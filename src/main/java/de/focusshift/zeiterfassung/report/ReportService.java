package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;

/**
 * This Report Service is permission aware and filters returned information based on permissions of the current
 * {@link org.springframework.security.core.annotation.AuthenticationPrincipal}.
 */
interface ReportService {

    default ReportWeek getReportWeek(Year year, int week, UserLocalId userLocalId) {
        return getReportWeek(year, week, List.of(userLocalId));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds);

    ReportWeek getReportWeekForAllUsers(Year year, int week);

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId);

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds);

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth);
}
