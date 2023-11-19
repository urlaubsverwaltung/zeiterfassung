package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static java.math.RoundingMode.CEILING;

/**
 * Describes the overtime account of a user. Like whether overtime is allowed or not.
 * Or how much overtime may be accumulated.
 */
public final class OvertimeAccount {

    private final UserIdComposite userIdComposite;
    private final boolean allowed;
    private final Duration maxAllowedOvertime;

    OvertimeAccount(UserIdComposite userIdComposite, boolean allowed) {
        this(userIdComposite, allowed, null);
    }

    OvertimeAccount(UserIdComposite userIdComposite, boolean allowed, Duration maxAllowedOvertime) {
        this.userIdComposite = userIdComposite;
        this.allowed = allowed;
        this.maxAllowedOvertime = maxAllowedOvertime;
    }

    public UserIdComposite getUserIdComposite() {
        return userIdComposite;
    }

    public boolean isAllowed() {
        return allowed;
    }

    /**
     *
     * @return the max allowed overtime duration, empty when nothing is set.
     */
    public Optional<Duration> getMaxAllowedOvertime() {
        return Optional.ofNullable(maxAllowedOvertime);
    }

    /**
     *
     * @return the max allowed overtime duration in hours, empty when nothing is set.
     */
    public Optional<BigDecimal> getMaxAllowedOvertimeHours() {
        if (maxAllowedOvertime == null) {
            return Optional.empty();
        }
        final BigDecimal minutes = BigDecimal.valueOf(maxAllowedOvertime.toMinutes());
        return Optional.of(minutes.divide(BigDecimal.valueOf(60), 2, CEILING));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeAccount that = (OvertimeAccount) o;
        return userIdComposite.equals(that.userIdComposite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdComposite);
    }

    @Override
    public String toString() {
        return "OvertimeSettings{" +
            "userIdComposite=" + userIdComposite +
            ", allowed=" + allowed +
            ", maxAllowedOvertime=" + maxAllowedOvertime +
            '}';
    }
}
