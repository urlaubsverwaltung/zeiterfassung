package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;

/**
 * Contractual working time. This representation has no context of absences like public holidays.
 */
public final class WorkingTime implements HasUserIdComposite {

    private final UserIdComposite userIdComposite;
    private final WorkingTimeId id;
    private final boolean current;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final LocalDate minValidFrom;
    private final FederalState federalState;
    private final boolean worksOnPublicHoliday;
    private final boolean worksOnPublicHolidayGlobal;
    private final EnumMap<DayOfWeek, PlannedWorkingHours> workdays;

    private WorkingTime(Builder builder) {
        this.userIdComposite = builder.userIdComposite;
        this.id = builder.id;
        this.current = builder.current;
        this.validFrom = builder.validFrom;
        this.validTo = builder.validTo;
        this.minValidFrom = builder.minValidFrom;
        this.federalState = builder.federalState;
        this.worksOnPublicHoliday = builder.worksOnPublicHoliday;
        this.worksOnPublicHolidayGlobal = builder.worksOnPublicHolidayGlobal;
        this.workdays = builder.workDays;
    }

    @Override
    public UserIdComposite userIdComposite() {
        return userIdComposite;
    }

    public WorkingTimeId id() {
        return id;
    }

    /**
     * @return {@code true} when this WorkingTime is currently active, otherwise {@code false}.
     */
    public boolean isCurrent() {
        return current;
    }

    /**
     * @return empty optional when this is the very first {@linkplain WorkingTime} of a person, validFrom date otherwise.
     */
    public Optional<LocalDate> validFrom() {
        return Optional.ofNullable(validFrom);
    }

    /**
     * @return empty optional when this is the last known {@linkplain WorkingTime} entry, validTo (inclusive) date otherwise.
     */
    public Optional<LocalDate> validTo() {
        return Optional.ofNullable(validTo);
    }

    /**
     * The minimum possible validFrom date that can be set for this {@linkplain WorkingTime}.
     * Equals the {@linkplain WorkingTime#validTo()} date of the previous one plus one day.
     *
     * @return empty optional when this is the very first {@linkplain WorkingTime} entry, minValidFrom date otherwise.
     */
    public Optional<LocalDate> minValidFrom() {
        return Optional.ofNullable(minValidFrom);
    }

    public FederalState federalState() {
        return federalState;
    }

    /**
     * Whether the related person works on public holidays or not.
     * Check {@linkplain WorkingTime#isWorksOnPublicHolidayGlobal()} if it is the system-wide setting or not.
     */
    public boolean worksOnPublicHoliday() {
        return worksOnPublicHoliday;
    }

    /**
     * Whether the {@linkplain WorkingTime#worksOnPublicHoliday()} value is the system-wide setting or not.
     */
    public boolean isWorksOnPublicHolidayGlobal() {
        return worksOnPublicHolidayGlobal;
    }

    public Map<DayOfWeek, PlannedWorkingHours> workdays() {
        return unmodifiableMap(workdays);
    }

    /**
     * @return {@linkplain Duration} of the given {@linkplain DayOfWeek}, never {@code null}
     */
    public PlannedWorkingHours getForDayOfWeek(DayOfWeek dayOfWeek) {
        return workdays.get(dayOfWeek);
    }

    /**
     * @return {@linkplain Duration} of monday, never {@code null}
     */
    public PlannedWorkingHours getMonday() {
        return getForDayOfWeek(MONDAY);
    }

    /**
     * @return {@linkplain Duration} of tuesday, never {@code null}
     */
    public PlannedWorkingHours getTuesday() {
        return getForDayOfWeek(TUESDAY);
    }

    /**
     * @return {@linkplain Duration} of wednesday, never {@code null}
     */
    public PlannedWorkingHours getWednesday() {
        return getForDayOfWeek(WEDNESDAY);
    }

    /**
     * @return {@linkplain Duration} of thursday, never {@code null}
     */
    public PlannedWorkingHours getThursday() {
        return getForDayOfWeek(THURSDAY);
    }

    /**
     * @return {@linkplain Duration} of friday, never {@code null}
     */
    public PlannedWorkingHours getFriday() {
        return getForDayOfWeek(FRIDAY);
    }

    /**
     * @return {@linkplain Duration} of saturday, never {@code null}
     */
    public PlannedWorkingHours getSaturday() {
        return getForDayOfWeek(SATURDAY);
    }

    /**
     * @return {@linkplain Duration} of sunday, never {@code null}
     */
    public PlannedWorkingHours getSunday() {
        return getForDayOfWeek(SUNDAY);
    }

    /**
     * Checks whether there are different WorkDay Hours or not.
     *
     * @return {@code true} when there are differences, {@code false} if every WorkDay has the same Hours.
     */
    public boolean hasDifferentWorkingHours() {
        return Stream.of(getMonday(), getTuesday(), getWednesday(), getThursday(), getFriday(), getSaturday(), getSunday())
            .map(PlannedWorkingHours::duration)
            .filter(not(Duration.ZERO::equals))
            .collect(toSet())
            .size() > 1;
    }

    /**
     * @return {@linkplain DayOfWeek}s with duration greater than ZERO.
     */
    public List<DayOfWeek> actualWorkingDays() {
        return workdays.entrySet().stream()
            .filter(not(entry -> PlannedWorkingHours.ZERO.equals(entry.getValue())))
            .map(Map.Entry::getKey)
            .sorted(comparing(DayOfWeek::getValue))
            .toList();
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

    public static Duration hoursToDuration(double hours) {
        return hoursToDuration(BigDecimal.valueOf(hours));
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
        private boolean current;
        private LocalDate validFrom;
        private LocalDate validTo;
        private LocalDate minValidFrom;
        private FederalState federalState;
        private boolean worksOnPublicHoliday;
        private boolean worksOnPublicHolidayGlobal;
        private final EnumMap<DayOfWeek, PlannedWorkingHours> workDays = new EnumMap<>(Map.of(
            MONDAY, PlannedWorkingHours.ZERO,
            TUESDAY, PlannedWorkingHours.ZERO,
            WEDNESDAY, PlannedWorkingHours.ZERO,
            THURSDAY, PlannedWorkingHours.ZERO,
            FRIDAY, PlannedWorkingHours.ZERO,
            SATURDAY, PlannedWorkingHours.ZERO,
            SUNDAY, PlannedWorkingHours.ZERO
        ));

        public Builder(UserIdComposite userIdComposite, WorkingTimeId id) {
            this.userIdComposite = userIdComposite;
            this.id = id;
        }

        public Builder current(boolean current) {
            this.current = current;
            return this;
        }

        public Builder validFrom(@Nullable LocalDate validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder validTo(@Nullable LocalDate validTo) {
            this.validTo = validTo;
            return this;
        }

        public Builder minValidFrom(@Nullable LocalDate minValidFrom) {
            this.minValidFrom = minValidFrom;
            return this;
        }

        public Builder federalState(FederalState federalState) {
            this.federalState = federalState;
            return this;
        }

        public Builder worksOnPublicHoliday(boolean worksOnPublicHoliday, boolean isGlobal) {
            this.worksOnPublicHoliday = worksOnPublicHoliday;
            this.worksOnPublicHolidayGlobal = isGlobal;
            return this;
        }

        public Builder monday(Duration duration) {
            this.workDays.put(MONDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder monday(double hours) {
            return this.monday(BigDecimal.valueOf(hours));
        }

        public Builder monday(BigDecimal hours) {
            return this.monday(hoursToDuration(hours));
        }

        public Builder tuesday(Duration duration) {
            this.workDays.put(TUESDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder tuesday(double hours) {
            return this.tuesday(BigDecimal.valueOf(hours));
        }

        public Builder tuesday(BigDecimal hours) {
            return this.tuesday(hoursToDuration(hours));
        }

        public Builder wednesday(Duration duration) {
            this.workDays.put(WEDNESDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder wednesday(double hours) {
            return this.wednesday(BigDecimal.valueOf(hours));
        }

        public Builder wednesday(BigDecimal hours) {
            return this.wednesday(hoursToDuration(hours));
        }

        public Builder thursday(Duration duration) {
            this.workDays.put(THURSDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder thursday(double hours) {
            return this.thursday(BigDecimal.valueOf(hours));
        }

        public Builder thursday(BigDecimal hours) {
            return this.thursday(hoursToDuration(hours));
        }

        public Builder friday(Duration duration) {
            this.workDays.put(FRIDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder friday(double hours) {
            return this.friday(BigDecimal.valueOf(hours));
        }

        public Builder friday(BigDecimal hours) {
            return this.friday(hoursToDuration(hours));
        }

        public Builder saturday(Duration duration) {
            this.workDays.put(SATURDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder saturday(double hours) {
            return this.saturday(BigDecimal.valueOf(hours));
        }

        public Builder saturday(BigDecimal hours) {
            return this.saturday(hoursToDuration(hours));
        }

        public Builder sunday(Duration duration) {
            this.workDays.put(SUNDAY, new PlannedWorkingHours(duration));
            return this;
        }

        public Builder sunday(double hours) {
            return this.sunday(BigDecimal.valueOf(hours));
        }

        public Builder sunday(BigDecimal hours) {
            return this.sunday(hoursToDuration(hours));
        }

        public WorkingTime build() {
            return new WorkingTime(this);
        }
    }
}
