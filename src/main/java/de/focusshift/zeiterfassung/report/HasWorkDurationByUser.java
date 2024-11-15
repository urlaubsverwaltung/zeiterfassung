package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

interface HasWorkDurationByUser {

    Map<UserIdComposite, WorkDuration> workDurationByUser();

    Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser();

    Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser();

    /**
     * Returns the difference between {@linkplain ShouldWorkingHours} and the {@linkplain WorkDuration}of every user in this week.
     *
     * @return Map of delta duration for every user in this week
     */
    default Map<UserIdComposite, DeltaWorkingHours> deltaDurationByUser() {

        final Map<UserIdComposite, WorkDuration> workedByUser = workDurationByUser();

        return shouldWorkingHoursByUser().entrySet().stream().collect(toMap(
            Map.Entry::getKey,
            entry -> new DeltaWorkingHours(workedByUser.get(entry.getKey()).duration().minus(entry.getValue().duration()))
        ));
    }
}
