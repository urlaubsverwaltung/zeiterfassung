package de.focusshift.zeiterfassung.usermanagement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

class WorkingTimeDto {

    private Long userId;

    private List<String> workday;

    private BigDecimal workingTime;

    private BigDecimal workingTimeMonday;
    private BigDecimal workingTimeTuesday;
    private BigDecimal workingTimeWednesday;
    private BigDecimal workingTimeThursday;
    private BigDecimal workingTimeFriday;
    private BigDecimal workingTimeSaturday;
    private BigDecimal workingTimeSunday;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getWorkday() {
        return workday;
    }

    public void setWorkday(List<String> workday) {
        this.workday = workday;
    }

    public BigDecimal getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(BigDecimal workingTime) {
        this.workingTime = workingTime;
    }

    public BigDecimal getWorkingTimeMonday() {
        return workingTimeMonday;
    }

    public void setWorkingTimeMonday(BigDecimal workingTimeMonday) {
        this.workingTimeMonday = workingTimeMonday;
    }

    public BigDecimal getWorkingTimeTuesday() {
        return workingTimeTuesday;
    }

    public void setWorkingTimeTuesday(BigDecimal workingTimeTuesday) {
        this.workingTimeTuesday = workingTimeTuesday;
    }

    public BigDecimal getWorkingTimeWednesday() {
        return workingTimeWednesday;
    }

    public void setWorkingTimeWednesday(BigDecimal workingTimeWednesday) {
        this.workingTimeWednesday = workingTimeWednesday;
    }

    public BigDecimal getWorkingTimeThursday() {
        return workingTimeThursday;
    }

    public void setWorkingTimeThursday(BigDecimal workingTimeThursday) {
        this.workingTimeThursday = workingTimeThursday;
    }

    public BigDecimal getWorkingTimeFriday() {
        return workingTimeFriday;
    }

    public void setWorkingTimeFriday(BigDecimal workingTimeFriday) {
        this.workingTimeFriday = workingTimeFriday;
    }

    public BigDecimal getWorkingTimeSaturday() {
        return workingTimeSaturday;
    }

    public void setWorkingTimeSaturday(BigDecimal workingTimeSaturday) {
        this.workingTimeSaturday = workingTimeSaturday;
    }

    public BigDecimal getWorkingTimeSunday() {
        return workingTimeSunday;
    }

    public void setWorkingTimeSunday(BigDecimal workingTimeSunday) {
        this.workingTimeSunday = workingTimeSunday;
    }

    @Override
    public String toString() {
        return "WorkingTimeDto{" +
            "userId=" + userId +
            ", workday=" + workday +
            ", workingtime=" + workingTime +
            ", workingtimeMonday=" + workingTimeMonday +
            ", workingtimeTuesday=" + workingTimeTuesday +
            ", workingtimeWednesday=" + workingTimeWednesday +
            ", workingtimeThursday=" + workingTimeThursday +
            ", workingtimeFriday=" + workingTimeFriday +
            ", workingtimeSaturday=" + workingTimeSaturday +
            ", workingtimeSunday=" + workingTimeSunday +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private List<DayOfWeek> workday;
        private BigDecimal workingTime;
        private BigDecimal workingTimeMonday;
        private BigDecimal workingTimeTuesday;
        private BigDecimal workingTimeWednesday;
        private BigDecimal workingTimeThursday;
        private BigDecimal workingTimeFriday;
        private BigDecimal workingTimeSaturday;
        private BigDecimal workingTimeSunday;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder workday(List<DayOfWeek> workday) {
            this.workday = workday;
            return this;
        }

        public Builder workingTime(BigDecimal workingTime) {
            this.workingTime = workingTime;
            return this;
        }

        public Builder workingTimeMonday(BigDecimal workingTimeMonday) {
            this.workingTimeMonday = workingTimeMonday;
            return this;
        }

        public Builder workingTimeTuesday(BigDecimal workingTimeTuesday) {
            this.workingTimeTuesday = workingTimeTuesday;
            return this;
        }

        public Builder workingTimeWednesday(BigDecimal workingTimeWednesday) {
            this.workingTimeWednesday = workingTimeWednesday;
            return this;
        }

        public Builder workingTimeThursday(BigDecimal workingTimeThursday) {
            this.workingTimeThursday = workingTimeThursday;
            return this;
        }

        public Builder workingTimeFriday(BigDecimal workingTimeFriday) {
            this.workingTimeFriday = workingTimeFriday;
            return this;
        }

        public Builder workingTimeSaturday(BigDecimal workingTimeSaturday) {
            this.workingTimeSaturday = workingTimeSaturday;
            return this;
        }

        public Builder workingTimeSunday(BigDecimal workingTimeSunday) {
            this.workingTimeSunday = workingTimeSunday;
            return this;
        }

        public WorkingTimeDto build() {
            final WorkingTimeDto workingTimeDto = new WorkingTimeDto();
            workingTimeDto.setUserId(userId);
            workingTimeDto.setWorkday(workday.stream().map(DayOfWeek::name).map(String::toLowerCase).toList());
            workingTimeDto.setWorkingTime(workingTime);
            workingTimeDto.setWorkingTimeMonday(workingTimeMonday);
            workingTimeDto.setWorkingTimeTuesday(workingTimeTuesday);
            workingTimeDto.setWorkingTimeWednesday(workingTimeWednesday);
            workingTimeDto.setWorkingTimeThursday(workingTimeThursday);
            workingTimeDto.setWorkingTimeFriday(workingTimeFriday);
            workingTimeDto.setWorkingTimeSaturday(workingTimeSaturday);
            workingTimeDto.setWorkingTimeSunday(workingTimeSunday);
            return workingTimeDto;
        }
    }
}
