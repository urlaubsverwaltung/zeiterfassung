package de.focusshift.zeiterfassung.timeentry;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.TIME;

@FieldsNotAllEmpty.List({
    @FieldsNotAllEmpty(
        // only duration is given
        fields = {"start", "end"},
        message = "{time-entry.validation.startOrEnd.required}"
    ),
    @FieldsNotAllEmpty(
        // only end is given
        fields = {"start", "duration"},
        message = "{time-entry.validation.startOrDuration.required}"
    ),
    @FieldsNotAllEmpty(
        // only start is given
        fields = {"end", "duration"},
        message = "{time-entry.validation.endOrDuration.required}"
    )
})
public class TimeEntryDTO {

    private Long id;

    @DateTimeFormat(iso = DATE)
    private LocalDate date;

    @DateTimeFormat(iso = TIME)
    private LocalTime start;

    @DateTimeFormat(iso = TIME)
    private LocalTime end;

    // | means emptyString OR the pattern
    @Pattern(regexp = "^\\d\\d:\\d\\d$|", message = "{time-entry.validation.duration.pattern}")
    private String duration;

    private String comment;

    private boolean isBreak;

    public TimeEntryDTO() {
        date = LocalDate.now();
    }

    private TimeEntryDTO(Long id, LocalDate date, LocalTime start, LocalTime end, String duration, String comment, boolean isBreak) {
        this.id = id;
        this.date = date;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.comment = comment;
        this.isBreak = isBreak;
    }

    public Long getId() {
        return id;
    }

    public TimeEntryDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isToday() {
        return LocalDate.now().atStartOfDay().equals(date.atStartOfDay());
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setBreak(boolean isBreak) {
        this.isBreak = isBreak;
    }

    public boolean isBreak() {
        return isBreak;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeEntryDTO that = (TimeEntryDTO) o;
        return Objects.equals(date, that.date)
            && Objects.equals(start, that.start)
            && Objects.equals(end, that.end)
            && Objects.equals(duration, that.duration)
            && Objects.equals(comment, that.comment)
            && Objects.equals(isBreak, that.isBreak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, start, end, duration, comment);
    }

    @Override
    public String toString() {
        return "TimeEntryDTO{" +
            "id=" + id +
            ", date=" + date +
            ", start=" + start +
            ", end=" + end +
            ", duration='" + duration + '\'' +
            ", comment='" + comment + '\'' +
            ", isBreak='" + isBreak + '\'' +
            '}';
    }

    public static class Builder {
        private Long id;
        private LocalDate date;
        private LocalTime start;
        private LocalTime end;
        private String duration;
        private String comment;
        private boolean isBreak;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder start(LocalTime start) {
            this.start = start;
            return this;
        }

        public Builder end(LocalTime end) {
            this.end = end;
            return this;
        }

        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder isBreak(boolean isBreak) {
            this.isBreak = isBreak;
            return this;
        }

        public TimeEntryDTO build() {
            return new TimeEntryDTO(id, date, start, end, duration, comment, isBreak);
        }
    }
}
