package de.focusshift.zeiterfassung.timeentry;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidTimeEntryException extends RuntimeException {
    InvalidTimeEntryException(String message) {
        super(message);
    }
}
