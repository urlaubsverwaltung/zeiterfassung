package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import jakarta.annotation.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class WorkingTimeDto {

    private Long userId;
    private String id;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate minValidFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate maxValidFrom;
    private FederalState federalState;
    private Boolean worksOnPublicHoliday;
    private List<String> workday = new ArrayList<>();
    private Double workingTime;
    private Double workingTimeMonday;
    private Double workingTimeTuesday;
    private Double workingTimeWednesday;
    private Double workingTimeThursday;
    private Double workingTimeFriday;
    private Double workingTimeSaturday;
    private Double workingTimeSunday;
    private boolean defaultWorkingTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getMinValidFrom() {
        return minValidFrom;
    }

    public void setMinValidFrom(LocalDate minValidFrom) {
        this.minValidFrom = minValidFrom;
    }

    public LocalDate getMaxValidFrom() {
        return maxValidFrom;
    }

    public void setMaxValidFrom(LocalDate maxValidFrom) {
        this.maxValidFrom = maxValidFrom;
    }

    public FederalState getFederalState() {
        return federalState;
    }

    public void setFederalState(FederalState federalState) {
        this.federalState = federalState;
    }

    @Nullable
    public Boolean getWorksOnPublicHoliday() {
        return worksOnPublicHoliday;
    }

    public void setWorksOnPublicHoliday(@Nullable Boolean worksOnPublicHoliday) {
        this.worksOnPublicHoliday = worksOnPublicHoliday;
    }

    public List<String> getWorkday() {
        return workday;
    }

    public void setWorkday(List<String> workday) {
        this.workday = workday;
    }

    public Double getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(Double workingTime) {
        this.workingTime = workingTime;
    }

    public Double getWorkingTimeMonday() {
        return workingTimeMonday;
    }

    public void setWorkingTimeMonday(Double workingTimeMonday) {
        this.workingTimeMonday = workingTimeMonday;
    }

    public Double getWorkingTimeTuesday() {
        return workingTimeTuesday;
    }

    public void setWorkingTimeTuesday(Double workingTimeTuesday) {
        this.workingTimeTuesday = workingTimeTuesday;
    }

    public Double getWorkingTimeWednesday() {
        return workingTimeWednesday;
    }

    public void setWorkingTimeWednesday(Double workingTimeWednesday) {
        this.workingTimeWednesday = workingTimeWednesday;
    }

    public Double getWorkingTimeThursday() {
        return workingTimeThursday;
    }

    public void setWorkingTimeThursday(Double workingTimeThursday) {
        this.workingTimeThursday = workingTimeThursday;
    }

    public Double getWorkingTimeFriday() {
        return workingTimeFriday;
    }

    public void setWorkingTimeFriday(Double workingTimeFriday) {
        this.workingTimeFriday = workingTimeFriday;
    }

    public Double getWorkingTimeSaturday() {
        return workingTimeSaturday;
    }

    public void setWorkingTimeSaturday(Double workingTimeSaturday) {
        this.workingTimeSaturday = workingTimeSaturday;
    }

    public Double getWorkingTimeSunday() {
        return workingTimeSunday;
    }

    public void setWorkingTimeSunday(Double workingTimeSunday) {
        this.workingTimeSunday = workingTimeSunday;
    }

    public boolean isDefaultWorkingTime() {
        return defaultWorkingTime;
    }

    public void setDefaultWorkingTime(boolean defaultWorkingTime) {
        this.defaultWorkingTime = defaultWorkingTime;
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return always {@code false}
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when monday is selected, {@code false} otherwise
     */
    public boolean isWorkDayMonday() {
        return getWorkday().contains("monday");
    }

        /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when tuesday is selected, {@code false} otherwise
     */
    public boolean isWorkDayTuesday() {
        return getWorkday().contains("tuesday");
    }

        /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when wednesday is selected, {@code false} otherwise
     */
    public boolean isWorkDayWednesday() {
        return getWorkday().contains("wednesday");
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when thursday is selected, {@code false} otherwise
     */
    public boolean isWorkDayThursday() {
        return getWorkday().contains("thursday");
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when friday is selected, {@code false} otherwise
     */
    public boolean isWorkDayFriday() {
        return getWorkday().contains("friday");
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when saturday is selected, {@code false} otherwise
     */
    public boolean isWorkDaySaturday() {
        return getWorkday().contains("saturday");
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return {@code true} when sunday is selected, {@code false} otherwise
     */
    public boolean isWorkDaySunday() {
        return getWorkday().contains("sunday");
    }

    /**
     * marker to ease validation feedback for the user. field error is set in {@linkplain WorkingTimeDtoValidator}.
     * @return always {@code false}
     */
    public boolean isWorkingTimeClash() {
        return false;
    }

    @Override
    public String toString() {
        return "WorkingTimeDto{" +
            "userId=" + userId +
            ", id=" + id +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTimeDto dto = (WorkingTimeDto) o;
        return Objects.equals(userId, dto.userId)
            && Objects.equals(id, dto.id)
            && Objects.equals(validFrom, dto.validFrom)
            && Objects.equals(minValidFrom, dto.minValidFrom)
            && Objects.equals(maxValidFrom, dto.maxValidFrom)
            && federalState == dto.federalState
            && Objects.equals(worksOnPublicHoliday, dto.worksOnPublicHoliday)
            && Objects.equals(workday, dto.workday)
            && Objects.equals(workingTime, dto.workingTime)
            && Objects.equals(workingTimeMonday, dto.workingTimeMonday)
            && Objects.equals(workingTimeTuesday, dto.workingTimeTuesday)
            && Objects.equals(workingTimeWednesday, dto.workingTimeWednesday)
            && Objects.equals(workingTimeThursday, dto.workingTimeThursday)
            && Objects.equals(workingTimeFriday, dto.workingTimeFriday)
            && Objects.equals(workingTimeSaturday, dto.workingTimeSaturday)
            && Objects.equals(workingTimeSunday, dto.workingTimeSunday);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, id, validFrom, minValidFrom, maxValidFrom, federalState, worksOnPublicHoliday,
            workday, workingTime, workingTimeMonday, workingTimeTuesday, workingTimeWednesday, workingTimeThursday,
            workingTimeFriday, workingTimeSaturday, workingTimeSunday);
    }
}
