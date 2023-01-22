package de.focusshift.zeiterfassung.timeclock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
class TimeClockDtoValidationTest {

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    private final Validator validator = Validation.buildDefaultValidatorFactory()
        .usingContext()
        .clockProvider(() -> clock)
        .getValidator();

    @Nested
    class InvalidWhen {

        @Test
        void zoneIdIsNull() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(null);
            timeClockDto.setDate(LocalDate.now(clock));
            timeClockDto.setTime(LocalTime.now(clock));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).hasSize(1);
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getPropertyPath()).hasToString("zoneId");
                assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
            });
        }

        @Test
        void dateIsNull() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(null);
            timeClockDto.setTime(LocalTime.now(clock));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).hasSize(2);
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.date.error.required}");
            });
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
            });
        }

        @Test
        void timeIsNull() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock));
            timeClockDto.setTime(null);

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).hasSize(2);
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.time.error.required}");
            });
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
            });
        }

        @Test
        void dateIsInTheFuture() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock).plusDays(1));
            timeClockDto.setTime(LocalTime.now(clock));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).hasSize(3); // +1 item -> class level violation is reported somehow
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getPropertyPath()).hasToString("time");
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
            });
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getPropertyPath()).hasToString("date");
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
            });
        }

        @Test
        void timeIsInTheFuture() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock));
            timeClockDto.setTime(LocalTime.now(clock).plusMinutes(1));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).hasSize(2); // +1 item -> class level violation is reported somehow
            assertThat(violations).anySatisfy(violation -> {
                assertThat(violation.getPropertyPath()).hasToString("time");
                assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
            });
        }
    }

    @Nested
    class ValidWhen {
        @Test
        void dateAndTimeIsNow() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock));
            timeClockDto.setTime(LocalTime.now(clock));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).isEmpty();
        }

        @Test
        void dateIsInThePast() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock).minusDays(1));
            timeClockDto.setTime(LocalTime.now(clock));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).isEmpty();
        }

        @Test
        void timeIsInThePast() {

            final TimeClockDto timeClockDto = new TimeClockDto();
            timeClockDto.setZoneId(ZoneId.systemDefault());
            timeClockDto.setDate(LocalDate.now(clock));
            timeClockDto.setTime(LocalTime.now(clock).minusMinutes(1));

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(timeClockDto);

            assertThat(violations).isEmpty();
        }
    }
}
