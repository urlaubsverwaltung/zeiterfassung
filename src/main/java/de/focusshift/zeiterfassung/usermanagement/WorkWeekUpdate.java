package de.focusshift.zeiterfassung.usermanagement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.focusshift.zeiterfassung.usermanagement.WorkingTime.hoursToDuration;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.springframework.util.Assert.isTrue;

/**
 * Used to update a {@linkplain WorkingTime}.
 *
 * @param validFrom empty Optional when updating the very first {@linkplain WorkingTime}, a date otherwise
 * @param workDays duration for every DayOfWeek. Map must contain non {@code null} values for all {@linkplain DayOfWeek}s.
 */
public record WorkWeekUpdate(
    Optional<LocalDate> validFrom,
    EnumMap<DayOfWeek, Duration> workDays
) {

    public WorkWeekUpdate {
        isTrue(workDays.keySet().containsAll(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)), "expected workDays to contain values for every DayOfWeek.");
        isTrue(workDays.values().stream().noneMatch(Objects::isNull), "expected all workDays values to be non null.");
    }

    static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate validFrom;
        private final EnumMap<DayOfWeek, Duration> workDays = new EnumMap<>(Map.of(
            MONDAY, Duration.ZERO,
            TUESDAY, Duration.ZERO,
            WEDNESDAY, Duration.ZERO,
            THURSDAY, Duration.ZERO,
            FRIDAY, Duration.ZERO,
            SATURDAY, Duration.ZERO,
            SUNDAY, Duration.ZERO
        ));

        public Builder validFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder monday(Duration duration) {
            this.workDays.put(MONDAY, duration);
            return this;
        }

        public Builder monday(double hours) {
            return this.monday(BigDecimal.valueOf(hours));
        }

        public Builder monday(BigDecimal hours) {
            return this.monday(hoursToDuration(hours));
        }

        public Builder tuesday(Duration duration) {
            this.workDays.put(TUESDAY, duration);
            return this;
        }

        public Builder tuesday(double hours) {
            return this.tuesday(BigDecimal.valueOf(hours));
        }

        public Builder tuesday(BigDecimal hours) {
            return this.tuesday(hoursToDuration(hours));
        }

        public Builder wednesday(Duration duration) {
            this.workDays.put(WEDNESDAY, duration);
            return this;
        }

        public Builder wednesday(double hours) {
            return this.wednesday(BigDecimal.valueOf(hours));
        }

        public Builder wednesday(BigDecimal hours) {
            return this.wednesday(hoursToDuration(hours));
        }

        public Builder thursday(Duration duration) {
            this.workDays.put(THURSDAY, duration);
            return this;
        }

        public Builder thursday(double hours) {
            return this.thursday(BigDecimal.valueOf(hours));
        }

        public Builder thursday(BigDecimal hours) {
            return this.thursday(hoursToDuration(hours));
        }

        public Builder friday(Duration duration) {
            this.workDays.put(FRIDAY, duration);
            return this;
        }

        public Builder friday(double hours) {
            return this.friday(BigDecimal.valueOf(hours));
        }

        public Builder friday(BigDecimal hours) {
            return this.friday(hoursToDuration(hours));
        }

        public Builder saturday(Duration duration) {
            this.workDays.put(SATURDAY, duration);
            return this;
        }

        public Builder saturday(double hours) {
            return this.saturday(BigDecimal.valueOf(hours));
        }

        public Builder saturday(BigDecimal hours) {
            return this.saturday(hoursToDuration(hours));
        }

        public Builder sunday(Duration duration) {
            this.workDays.put(SUNDAY, duration);
            return this;
        }

        public Builder sunday(double hours) {
            return this.sunday(BigDecimal.valueOf(hours));
        }

        public Builder sunday(BigDecimal hours) {
            return this.sunday(hoursToDuration(hours));
        }

        public WorkWeekUpdate build() {
            return new WorkWeekUpdate(Optional.ofNullable(validFrom), workDays);
        }
    }
}
