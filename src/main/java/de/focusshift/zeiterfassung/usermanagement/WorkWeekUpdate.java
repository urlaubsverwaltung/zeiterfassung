package de.focusshift.zeiterfassung.usermanagement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Optional;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

public record WorkWeekUpdate(
    Optional<WorkDay> monday,
    Optional<WorkDay> tuesday,
    Optional<WorkDay> wednesday,
    Optional<WorkDay> thursday,
    Optional<WorkDay> friday,
    Optional<WorkDay> saturday,
    Optional<WorkDay> sunday
) {

    static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnumMap<DayOfWeek, Duration> workDays = new EnumMap<>(DayOfWeek.class);

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
            return new WorkWeekUpdate(getWorkDay(MONDAY), getWorkDay(TUESDAY), getWorkDay(WEDNESDAY),
                getWorkDay(THURSDAY), getWorkDay(FRIDAY), getWorkDay(SATURDAY), getWorkDay(SUNDAY));
        }

        private Optional<WorkDay> getWorkDay(DayOfWeek dayOfWeek) {
            final Duration hours = workDays.get(dayOfWeek);
            return hours == null ? Optional.empty() : Optional.of(new WorkDay(dayOfWeek, hours));
        }

        private static Duration hoursToDuration(BigDecimal hours) {
            final int hoursPart = hours.setScale(0, DOWN).abs().intValue();
            final int minutesPart = hours.remainder(ONE).multiply(BigDecimal.valueOf(60)).setScale(0, HALF_EVEN).abs().intValueExact();
            return Duration.ofHours(hoursPart).plusMinutes(minutesPart);
        }
    }
}