package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.report.ReportDay;
import de.focusshift.zeiterfassung.report.ReportServiceRaw;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
class OvertimeServiceImpl implements OvertimeService {

    private final ReportServiceRaw reportServiceRaw;

    OvertimeServiceImpl(ReportServiceRaw reportServiceRaw) {
        this.reportServiceRaw = reportServiceRaw;
    }

    @Override
    public Map<UserIdComposite, OvertimeHours> getOvertimeForDate(LocalDate date) {

        final ReportDay reportDay = reportServiceRaw.getReportDayForAllUsers(date);

        return reportDay.overtimeByUser().entrySet().stream().collect(
            toMap(
                Map.Entry::getKey,
                entry -> new OvertimeHours(entry.getValue().duration())
            )
        );
    }

    @Override
    public OvertimeHours getOvertimeForDateAndUser(LocalDate date, UserLocalId userLocalId) {

        return getOvertimeForDate(date).entrySet().stream()
            .filter(entry -> entry.getKey().localId().equals(userLocalId))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new IllegalStateException("expected OvertimeHours to exist for %s".formatted(userLocalId)));
    }
}
