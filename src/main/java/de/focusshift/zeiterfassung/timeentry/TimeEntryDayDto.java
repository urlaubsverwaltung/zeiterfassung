package de.focusshift.zeiterfassung.timeentry;

import java.time.DayOfWeek;
import java.util.List;

record TimeEntryDayDto(String date,
                       DayOfWeek dayOfWeek,
                       String hoursWorked,
                       String hoursWorkedShould,
                       String hoursDelta,
                       boolean hoursDeltaNegative,
                       double hoursWorkedRatio,
                       List<TimeEntryDTO> timeEntries,
                       List<AbsenceEntryDto> absenceEntries) {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String date;
        private DayOfWeek dayOfWeek;
        private String hoursWorked;
        private String hoursWorkedShould;
        private String hoursDelta;
        private boolean hoursDeltaNegative;
        private double hoursWorkedRatio;
        private List<TimeEntryDTO> timeEntries;
        private List<AbsenceEntryDto> absenceEntries;

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

        public TimeEntryDayDto build() {
            return new TimeEntryDayDto(date, dayOfWeek, hoursWorked, hoursWorkedShould, hoursDelta, hoursDeltaNegative,
                hoursWorkedRatio, timeEntries, absenceEntries);
        }
    }
}
