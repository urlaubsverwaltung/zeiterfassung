package de.focusshift.zeiterfassung.usermanagement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
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
public final class WorkingTime {

    private final UserLocalId userId;
    private final WorkDay monday;
    private final WorkDay tuesday;
    private final WorkDay wednesday;
    private final WorkDay thursday;
    private final WorkDay friday;
    private final WorkDay saturday;
    private final WorkDay sunday;

    private WorkingTime(UserLocalId userId, WorkDay monday, WorkDay tuesday, WorkDay wednesday, WorkDay thursday,
                        WorkDay friday, WorkDay saturday, WorkDay sunday) {

        this.userId = userId;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
    }

    public UserLocalId getUserId() {
        return userId;
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of monday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getMonday() {
        return Optional.ofNullable(monday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of tuesday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getTuesday() {
        return Optional.ofNullable(tuesday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of wednesday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getWednesday() {
        return Optional.ofNullable(wednesday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of thursday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getThursday() {
        return Optional.ofNullable(thursday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of friday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getFriday() {
        return Optional.ofNullable(friday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of saturday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getSaturday() {
        return Optional.ofNullable(saturday);
    }

    /**
     *
     * @return optional {@linkplain WorkDay} of sunday, or {@linkplain Optional#empty()} when it is not a working day.
     */
    public Optional<WorkDay> getSunday() {
        return Optional.ofNullable(sunday);
    }

    /**
     *
     * @return list of {@linkplain WorkDay}s with working hours greater ZERO.
     */
    public List<WorkDay> getWorkingDays() {
        return Stream.of(monday, tuesday, wednesday, thursday, friday, saturday, sunday)
            .filter(Objects::nonNull)
            .filter(not(w -> ZERO.equals(w.duration()))).toList();
    }

    public List<DayOfWeek> getWorkingDayOfWeeks() {
        return hoursSupplierByDayMap(this).entrySet()
            .stream()
            .filter(entry -> entry.getValue().get() != null)
            .map(Map.Entry::getKey)
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

    /**
     * @return common working duration for working days, {@linkplain Optional#empty()} when hours are different.
     */
    public Optional<Duration> getWorkingDuration() {
        final List<Duration> workDays = Stream.of(tuesday, wednesday, thursday, friday, saturday, sunday).map(WorkDay::duration).toList();

        Duration workingHours = monday.duration();

        for (Duration hours : workDays) {
            if (!ZERO.equals(hours) && !workingHours.equals(hours)) {
                return Optional.empty();
            }
        }

        return Optional.of(workingHours);
    }

    private static Map<DayOfWeek, Supplier<BigDecimal>> hoursSupplierByDayMap(WorkingTime workTime) {
        return Map.of(
            MONDAY, () -> workTime.getMonday().map(WorkDay::hours).orElse(null),
            TUESDAY, () -> workTime.getTuesday().map(WorkDay::hours).orElse(null),
            WEDNESDAY, () -> workTime.getWednesday().map(WorkDay::hours).orElse(null),
            THURSDAY, () -> workTime.getThursday().map(WorkDay::hours).orElse(null),
            FRIDAY, () -> workTime.getFriday().map(WorkDay::hours).orElse(null),
            SATURDAY, () -> workTime.getSaturday().map(WorkDay::hours).orElse(null),
            SUNDAY, () -> workTime.getSunday().map(WorkDay::hours).orElse(null)
        );
    }

    /**
     * @return common working hours for working days, {@linkplain Optional#empty()} when hours are different.
     */
    public Optional<BigDecimal> getWorkingHours() {
        return getWorkingDuration().map(duration -> new WorkDay(MONDAY, duration).hours());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTime that = (WorkingTime) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "WorkingTime{" +
            "userId=" + userId +
            ", monday=" + monday +
            ", tuesday=" + tuesday +
            ", wednesday=" + wednesday +
            ", thursday=" + thursday +
            ", friday=" + friday +
            ", saturday=" + saturday +
            ", sunday=" + sunday +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserLocalId userId;
        private final EnumMap<DayOfWeek, Duration> workDays = new EnumMap<>(DayOfWeek.class);

        public Builder userId(UserLocalId userId) {
            this.userId = userId;
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

        public WorkingTime build() {
            return new WorkingTime(userId, getWorkDay(MONDAY), getWorkDay(TUESDAY), getWorkDay(WEDNESDAY),
                getWorkDay(THURSDAY), getWorkDay(FRIDAY), getWorkDay(SATURDAY), getWorkDay(SUNDAY));
        }

        private WorkDay getWorkDay(DayOfWeek dayOfWeek) {
            final Duration hours = workDays.get(dayOfWeek);
            return hours == null ? null : new WorkDay(dayOfWeek, hours);
        }

        private static Duration hoursToDuration(BigDecimal hours) {
            final int hoursPart = hours.setScale(0, DOWN).abs().intValue();
            final int minutesPart = hours.remainder(ONE).multiply(BigDecimal.valueOf(60)).setScale(0, HALF_EVEN).abs().intValueExact();
            return Duration.ofHours(hoursPart).plusMinutes(minutesPart);
        }
    }
}
