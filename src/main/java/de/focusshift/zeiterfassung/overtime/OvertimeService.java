package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;
import java.util.Map;

public interface OvertimeService {

    /**
     * Returns {@link OvertimeHours} for every user for the given date.
     *
     * @param date date
     * @return {@link OvertimeHours} for every user for the given date
     */
    Map<UserIdComposite, OvertimeHours> getOvertimeForDate(LocalDate date);
}
