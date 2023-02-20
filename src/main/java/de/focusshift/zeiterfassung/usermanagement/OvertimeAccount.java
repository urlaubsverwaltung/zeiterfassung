package de.focusshift.zeiterfassung.usermanagement;

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

    private final UserLocalId userLocalId;
    private final boolean allowed;
    private final Duration maxAllowedOvertime;

    OvertimeAccount(UserLocalId userLocalId, boolean allowed) {
        this(userLocalId, allowed, null);
    }

    OvertimeAccount(UserLocalId userLocalId, boolean allowed, Duration maxAllowedOvertime) {
        this.userLocalId = userLocalId;
        this.allowed = allowed;
        this.maxAllowedOvertime = maxAllowedOvertime;
    }

    public UserLocalId getUserLocalId() {
        return userLocalId;
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
        return userLocalId.equals(that.userLocalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userLocalId);
    }

    @Override
    public String toString() {
        return "OvertimeSettings{" +
            "userLocalId=" + userLocalId +
            ", allowed=" + allowed +
            ", maxAllowedOvertime=" + maxAllowedOvertime +
            '}';
    }
}
