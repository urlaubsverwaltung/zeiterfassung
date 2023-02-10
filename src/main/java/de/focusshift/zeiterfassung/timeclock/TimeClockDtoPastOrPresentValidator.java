package de.focusshift.zeiterfassung.timeclock;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeClockDtoPastOrPresentValidator implements ConstraintValidator<PastOrPresent, TimeClockDto> {

    @Override
    public boolean isValid(TimeClockDto object, ConstraintValidatorContext context) {
        // #date and #time are parsed from string to LocalDate / LocalTime.
        // so we just have to check whether it is past, present or future.
        // (user time zone has not to be considered here)
        final LocalDate date = object.getDate();
        final LocalTime time = object.getTime();

        if (date == null || time == null) {
            return false;
        }

        // using `clock` to be able to do deterministic tests.
        // however, we are interested in the LocalDateTime default zone.
        final Clock clock = context.getClockProvider().getClock();
        final LocalDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        final String message = context.getDefaultConstraintMessageTemplate();

        final boolean valid;

        if (date.isAfter(now.toLocalDate())) {
            // given date is in the future.
            // -> `date` AND `time` are both invalid
            context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode("date").addConstraintViolation();
            context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode("time").addConstraintViolation();
            valid = false;
        }
        else if (date.isEqual(now.toLocalDate()) && time.isAfter(now.toLocalTime())) {
            // given date is today. given time is in the future.
            // -> `time` is invalid
            context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode("time").addConstraintViolation();
            valid = false;
        }
        else {
            // given date is in the past. time cannot be in the future anymore.
            // -> everything is fine
            valid = true;
        }

        return valid;
    }
}
