package de.focusshift.zeiterfassung.usermanagement;

import java.util.Objects;

class OvertimeAccountDto {

    private boolean allowed;

    OvertimeAccountDto() {
        this(true);
    }

    OvertimeAccountDto(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeAccountDto that = (OvertimeAccountDto) o;
        return allowed == that.allowed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowed);
    }

    @Override
    public String toString() {
        return "OvertimeAccountDto{" +
            "allowed=" + allowed +
            '}';
    }
}
