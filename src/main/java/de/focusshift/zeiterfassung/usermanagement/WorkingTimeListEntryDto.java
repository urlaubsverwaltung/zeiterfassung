package de.focusshift.zeiterfassung.usermanagement;

import java.util.Date;
import java.util.stream.Stream;

record WorkingTimeListEntryDto(
    String id,
    Long userId,
    Date validFrom,
    Date validTo,
    boolean validFromIsPast,
    boolean isCurrent,
    boolean isDeletable,
    String federalStateMessageKey,
    boolean worksOnPublicHoliday,
    Double workingTimeMonday,
    Double workingTimeTuesday,
    Double workingTimeWednesday,
    Double workingTimeThursday,
    Double workingTimeFriday,
    Double workingTimeSaturday,
    Double workingTimeSunday
) {

    public double max() {
        return Stream.of(workingTimeMonday, workingTimeTuesday, workingTimeWednesday, workingTimeThursday,
                workingTimeFriday, workingTimeSaturday, workingTimeSunday).max(Double::compareTo).orElse(0d);
    }
}
