package de.focusshift.zeiterfassung.absence;

public enum DayLength {

    FULL(1.0),
    MORNING(0.5),
    NOON(0.5);

    /**
     * number value of the day length
     */
    private final double value;

    DayLength(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public double plus(DayLength dayLength) {
        return this.value + dayLength.value;
    }

    public boolean isFull() {
        return this.value == FULL.value;
    }

    public boolean isHalf() {
        return !isFull();
    }
}
