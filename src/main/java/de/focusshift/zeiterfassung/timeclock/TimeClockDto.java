package de.focusshift.zeiterfassung.timeclock;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneId;

@Validated
public class TimeClockDto {

    private Instant startedAt;
    private ZoneId zoneId;
    @Size(max = 255)
    private String comment;
    private String duration;

    private String date;
    private String time;

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }
}
