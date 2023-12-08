package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
import static java.time.Duration.ZERO;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

/**
 * Contractual working time. This representation has no context of absences like public holidays.
 */
public final class WorkingTime implements HasUserIdComposite {

    private final UserIdComposite userIdComposite;
    private final WorkingTimeId id;
    private final LocalDate validFrom;
    private final EnumMap<DayOfWeek, WorkDay> workdays;

    private WorkingTime(UserIdComposite userIdComposite, WorkingTimeId id, @Nullable LocalDate validFrom,
                         EnumMap<DayOfWeek, WorkDay> workdays) {
        this.userIdComposite = userIdComposite;
        this.id = id;
        this.validFrom = validFrom;
        this.workdays = workdays;
    }

    @Override
    public UserIdComposite userIdComposite() {
        return userIdComposite;
    }

    public WorkingTimeId id() {
        return id;
    }

    /**
     * @return empty optional when this is the very first {@linkplain WorkingTime} of a person, validFrom date otherwise.
     */
    public Optional<LocalDate> validFrom() {
        return Optional.ofNullable(validFrom);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of the given {@linkplain DayOfWeek}, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getForDayOfWeek(DayOfWeek dayOfWeek) {
        return Optional.ofNullable(workdays.get(dayOfWeek));
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of monday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getMonday() {
        return getForDayOfWeek(MONDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of tuesday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getTuesday() {
        return getForDayOfWeek(TUESDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of wednesday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getWednesday() {
        return getForDayOfWeek(WEDNESDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of thursday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getThursday() {
        return getForDayOfWeek(THURSDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of friday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getFriday() {
        return getForDayOfWeek(FRIDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of saturday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getSaturday() {
        return getForDayOfWeek(SATURDAY);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of sunday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getSunday() {
        return getForDayOfWeek(SUNDAY);
    }

    /**
     *
     * @return list of {@linkplain WorkDay}s with working hours greater ZERO.
     */
    public List<WorkDay> getWorkingDays() {
        return workdays.values()
            .stream()
            .filter(not(w -> ZERO.equals(w.duration())))
            .toList();
    }

    /**
     * Checks whether there are different WorkDay Hours or not.
     *
     * @return {@code true} when there are differences, {@code false} if every WorkDay has the same Hours.
     */
    public boolean hasDifferentWorkingHours() {
        return Stream.of(getMonday(), getTuesday(), getWednesday(), getThursday(), getFriday(), getSaturday(), getSunday())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(WorkDay::duration)
            .filter(not(Duration.ZERO::equals))
            .collect(toSet())
            .size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTime that = (WorkingTime) o;
        return Objects.equals(userIdComposite, that.userIdComposite) && Objects.equals(validFrom, that.validFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdComposite, validFrom);
    }

    @Override
    public String toString() {
        return "WorkingTime{" +
            "userIdComposite=" + userIdComposite +
            ", id=" + id +
            '}';
    }

    public static Duration hoursToDuration(@Nullable Double hours) {
        return hours == null ? ZERO : hoursToDuration(BigDecimal.valueOf(hours));
    }

    public static Duration hoursToDuration(BigDecimal hours) {
        final int hoursPart = hours.setScale(0, DOWN).abs().intValue();
        final int minutesPart = hours.remainder(ONE).multiply(BigDecimal.valueOf(60)).setScale(0, HALF_EVEN).abs().intValueExact();
        return Duration.ofHours(hoursPart).plusMinutes(minutesPart);
    }

    public static Builder builder(UserIdComposite userIdComposite, WorkingTimeId id) {
        return new Builder(userIdComposite, id);
    }

    public static class Builder {
        private final UserIdComposite userIdComposite;
        private final WorkingTimeId id;
        private LocalDate validFrom;
        private final EnumMap<DayOfWeek, WorkDay> workDays = new EnumMap<>(DayOfWeek.class);

        public Builder(UserIdComposite userIdComposite, WorkingTimeId id) {
            this.userIdComposite = userIdComposite;
            this.id = id;
        }

        public Builder validFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder monday(Duration duration) {
            this.workDays.put(MONDAY, new WorkDay(MONDAY, duration));
            return this;
        }

        public Builder monday(double hours) {
            return this.monday(BigDecimal.valueOf(hours));
        }

        public Builder monday(BigDecimal hours) {
            return this.monday(hoursToDuration(hours));
        }

        public Builder tuesday(Duration duration) {
            this.workDays.put(TUESDAY, new WorkDay(TUESDAY, duration));
            return this;
        }

        public Builder tuesday(double hours) {
            return this.tuesday(BigDecimal.valueOf(hours));
        }

        public Builder tuesday(BigDecimal hours) {
            return this.tuesday(hoursToDuration(hours));
        }

        public Builder wednesday(Duration duration) {
            this.workDays.put(WEDNESDAY, new WorkDay(WEDNESDAY, duration));
            return this;
        }

        public Builder wednesday(double hours) {
            return this.wednesday(BigDecimal.valueOf(hours));
        }

        public Builder wednesday(BigDecimal hours) {
            return this.wednesday(hoursToDuration(hours));
        }

        public Builder thursday(Duration duration) {
            this.workDays.put(THURSDAY, new WorkDay(THURSDAY, duration));
            return this;
        }

        public Builder thursday(double hours) {
            return this.thursday(BigDecimal.valueOf(hours));
        }

        public Builder thursday(BigDecimal hours) {
            return this.thursday(hoursToDuration(hours));
        }

        public Builder friday(Duration duration) {
            this.workDays.put(FRIDAY, new WorkDay(FRIDAY, duration));
            return this;
        }

        public Builder friday(double hours) {
            return this.friday(BigDecimal.valueOf(hours));
        }

        public Builder friday(BigDecimal hours) {
            return this.friday(hoursToDuration(hours));
        }

        public Builder saturday(Duration duration) {
            this.workDays.put(SATURDAY, new WorkDay(SATURDAY, duration));
            return this;
        }

        public Builder saturday(double hours) {
            return this.saturday(BigDecimal.valueOf(hours));
        }

        public Builder saturday(BigDecimal hours) {
            return this.saturday(hoursToDuration(hours));
        }

        public Builder sunday(Duration duration) {
            this.workDays.put(SUNDAY, new WorkDay(SUNDAY, duration));
            return this;
        }

        public Builder sunday(double hours) {
            return this.sunday(BigDecimal.valueOf(hours));
        }

        public Builder sunday(BigDecimal hours) {
            return this.sunday(hoursToDuration(hours));
        }

        public WorkingTime build() {
            return new WorkingTime(userIdComposite, id, validFrom, workDays);
        }
    }
}
