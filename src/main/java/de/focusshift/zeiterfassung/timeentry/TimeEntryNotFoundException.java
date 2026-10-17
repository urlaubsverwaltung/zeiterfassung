package de.focusshift.zeiterfassung.timeentry;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TimeEntryNotFoundException extends RuntimeException {

    TimeEntryNotFoundException(TimeEntryId timeEntryId) {
        super("TimeEntry with %s not found.".formatted(timeEntryId));
    }
}
