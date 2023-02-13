package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;

public interface TimeEntryDuration {

    /**
     *
     * @return the exact {@linkplain Duration} value.
     */
    Duration value();

    /**
     * @return {@linkplain Duration} value rounded up to full minutes.
     */
    Duration minutes();

    double hoursDoubleValue();
}
