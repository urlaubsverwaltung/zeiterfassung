package de.focusshift.zeiterfassung.timeentry;

import jakarta.annotation.Nullable;

import java.time.DayOfWeek;
import java.util.List;

record TimeEntryDayDto(
    boolean isLocked,
    boolean isAllowedToEdit,
    String date,
    DayOfWeek dayOfWeek,
    String hoursWorked,
    String hoursWorkedShould,
    String hoursDelta,
    boolean hoursDeltaNegative,
    double hoursWorkedRatio,
    List<TimeEntryDTO> timeEntries,
    List<AbsenceEntryDto> absenceEntries,
    @Nullable String breakDeduction,
    @Nullable String breakViolationMessageKey
) {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private boolean isLocked;
        private boolean isAllowedToEdit;
        private String date;
        private DayOfWeek dayOfWeek;
        private String hoursWorked;
        private String hoursWorkedShould;
        private String hoursDelta;
        private boolean hoursDeltaNegative;
        private double hoursWorkedRatio;
        private List<TimeEntryDTO> timeEntries;
        private List<AbsenceEntryDto> absenceEntries;
        private String breakDeduction;
        private String breakViolationMessageKey;

        public Builder isLocked(boolean isLocked) {
            this.isLocked = isLocked;
            return this;
        }

        public Builder isAllowedToEdit(boolean isAllowedToEdit) {
            this.isAllowedToEdit = isAllowedToEdit;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder dayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        public Builder hoursWorked(String hoursWorked) {
            this.hoursWorked = hoursWorked;
            return this;
        }

        public Builder hoursWorkedShould(String hoursWorkedShould) {
            this.hoursWorkedShould = hoursWorkedShould;
            return this;
        }

        public Builder hoursDelta(String hoursDelta) {
            this.hoursDelta = hoursDelta;
            return this;
        }

        public Builder hoursDeltaNegative(boolean hoursDeltaNegative) {
            this.hoursDeltaNegative = hoursDeltaNegative;
            return this;
        }

        public Builder hoursWorkedRatio(double hoursWorkedRatio) {
            this.hoursWorkedRatio = hoursWorkedRatio;
            return this;
        }

        public Builder timeEntries(List<TimeEntryDTO> timeEntries) {
            this.timeEntries = timeEntries;
            return this;
        }

        public Builder absenceEntries(List<AbsenceEntryDto> absenceEntries) {
            this.absenceEntries = absenceEntries;
            return this;
        }

        public Builder breakDeduction(@Nullable String breakDeduction) {
            this.breakDeduction = breakDeduction;
            return this;
        }

        public Builder breakViolationMessageKey(@Nullable String breakViolationMessageKey) {
            this.breakViolationMessageKey = breakViolationMessageKey;
            return this;
        }

        public TimeEntryDayDto build() {
            return new TimeEntryDayDto(isLocked, isAllowedToEdit, date, dayOfWeek, hoursWorked, hoursWorkedShould, hoursDelta,
                hoursDeltaNegative, hoursWorkedRatio, timeEntries, absenceEntries, breakDeduction, breakViolationMessageKey);
        }
    }
}
