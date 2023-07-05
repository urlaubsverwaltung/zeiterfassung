package de.focusshift.zeiterfassung.timeentry;

import java.util.Collection;
import java.util.List;

record TimeEntryWeekDto(Integer calendarWeek,
                        String from,
                        String to,
                        String hoursWorked,
                        String hoursWorkedShould,
                        String hoursDelta,
                        boolean hoursDeltaNegative,
                        double hoursWorkedRatio,
                        List<TimeEntryDayDto> days) {

    public List<TimeEntryDTO> timeEntries() {
        return days.stream().map(TimeEntryDayDto::timeEntries).flatMap(Collection::stream).toList();
    }

    public List<AbsenceEntryDto> absenceEntries() {
        return days.stream().map(TimeEntryDayDto::absenceEntries).flatMap(Collection::stream).toList();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private Integer calendarWeek;
        private String from;
        private String to;
        private String hoursWorked;
        private String hoursWorkedShould;
        private String hoursDelta;
        private boolean hoursDeltaNegative;
        private double hoursWorkedRatio;
        private List<TimeEntryDayDto> days;

        public Builder calendarWeek(Integer calendarWeek) {
            this.calendarWeek = calendarWeek;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
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

        public Builder days(List<TimeEntryDayDto> days) {
            this.days = days;
            return this;
        }

        public TimeEntryWeekDto build() {
            return new TimeEntryWeekDto(calendarWeek, from, to, hoursWorked, hoursWorkedShould, hoursDelta,
                hoursDeltaNegative, hoursWorkedRatio, days);
        }
    }
}
