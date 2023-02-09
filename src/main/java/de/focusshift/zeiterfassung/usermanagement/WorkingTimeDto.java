package de.focusshift.zeiterfassung.usermanagement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

@Validated
class WorkingTimeDto {

    private Long userId;

    private List<String> workday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTime;

    // a flag to enable simpler validation for individual working time.
    // summarizes workingTime-dayOfWeek
    private boolean workingTimeClash;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeMonday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeTuesday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeWednesday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeThursday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeFriday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeSaturday;

    @PositiveOrZero(message = "{usermanagement.working-time.positive-or-zero.constraint.message}")
    @Max(value = 24, message = "{usermanagement.working-time.24h.constraint.message}")
    private BigDecimal workingTimeSunday;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getWorkday() {
        return workday == null ? List.of() : workday;
    }

    public void setWorkday(List<String> workday) {
        this.workday = workday;
    }

    public BigDecimal getWorkingTime() {
        return workingTime;
    }

    public boolean isWorkingTimeClash() {
        return workingTimeClash;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTimeDto that = (WorkingTimeDto) o;
        return workingTimeClash == that.workingTimeClash
            && userId.equals(that.userId)
            && workday.equals(that.workday)
            && Objects.equals(workingTime, that.workingTime)
            && Objects.equals(workingTimeMonday, that.workingTimeMonday)
            && Objects.equals(workingTimeTuesday, that.workingTimeTuesday)
            && Objects.equals(workingTimeWednesday, that.workingTimeWednesday)
            && Objects.equals(workingTimeThursday, that.workingTimeThursday)
            && Objects.equals(workingTimeFriday, that.workingTimeFriday)
            && Objects.equals(workingTimeSaturday, that.workingTimeSaturday)
            && Objects.equals(workingTimeSunday, that.workingTimeSunday);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, workday, workingTime, workingTimeClash, workingTimeMonday, workingTimeTuesday,
            workingTimeWednesday, workingTimeThursday, workingTimeFriday, workingTimeSaturday, workingTimeSunday);
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
