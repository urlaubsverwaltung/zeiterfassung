package de.focusshift.zeiterfassung.usermanagement;

import java.util.Objects;

class OvertimeAccountDto {

    private boolean allowed;
    private Double maxAllowedOvertime;

    OvertimeAccountDto() {
        this(true, null);
    }

    OvertimeAccountDto(boolean allowed, Double maxAllowedOvertime) {
        this.allowed = allowed;
        this.maxAllowedOvertime = maxAllowedOvertime;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public Double getMaxAllowedOvertime() {
        return maxAllowedOvertime;
    }

    public void setMaxAllowedOvertime(Double maxAllowedOvertime) {
        this.maxAllowedOvertime = maxAllowedOvertime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeAccountDto that = (OvertimeAccountDto) o;
        return allowed == that.allowed && Objects.equals(maxAllowedOvertime, that.maxAllowedOvertime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowed, maxAllowedOvertime);
    }

    @Override
    public String toString() {
        return "OvertimeAccountDto{" +
            "allowed=" + allowed +
            ", maxAllowedOvertime=" + maxAllowedOvertime +
            '}';
    }
}
