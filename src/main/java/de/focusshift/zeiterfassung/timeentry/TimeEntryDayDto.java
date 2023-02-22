package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

record TimeEntryDayDto(String date,
                       String hoursWorked,
                       String hoursWorkedShould,
                       String hoursDelta,
                       boolean hoursDeltaNegative,
                       double hoursWorkedRatio,
                       List<TimeEntryDTO> timeEntries) {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String date;
        private String hoursWorked;
        private String hoursWorkedShould;
        private String hoursDelta;
        private boolean hoursDeltaNegative;
        private double hoursWorkedRatio;
        private List<TimeEntryDTO> timeEntries;

        public Builder date(String date) {
            this.date = date;
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

        public TimeEntryDayDto build() {
            return new TimeEntryDayDto(date, hoursWorked, hoursWorkedShould, hoursDelta, hoursDeltaNegative,
                hoursWorkedRatio, timeEntries);
        }
    }
}
