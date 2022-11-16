package de.focusshift.zeiterfassung.timeclock;

import java.time.Instant;

record TimeClockDto(Instant startedAt, String duration) {
}
