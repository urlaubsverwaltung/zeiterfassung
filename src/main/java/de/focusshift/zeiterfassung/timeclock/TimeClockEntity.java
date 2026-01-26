package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "time_clock")
public class TimeClockEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "time_clock_seq", sequenceName = "time_clock_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_clock_seq")
    protected Long id;

    @Column(name = "owner", nullable = false)
    @NotNull
    @Size(max = 255)
    private String owner;

    @Column(name = "started_at", nullable = false)
    @NotNull
    private Instant startedAt;

    @Column(name = "started_at_zone_id", nullable = false)
    @NotNull
    private String startedAtZoneId;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "stopped_at_zone_id")
    private String stoppedAtZoneId;

    @Column(name = "comment", nullable = false)
    @NotNull
    private String comment;

    @Column(name = "is_break", nullable = false)
    @NotNull
    private boolean isBreak;

    protected TimeClockEntity() {
        super(null);
    }

    private TimeClockEntity(Long id, String owner, Instant startedAt, ZoneId startedAtZoneId, Instant stoppedAt, ZoneId stoppedAtZoneId, String comment, boolean isBreak) {
        this(null, id, owner, startedAt, startedAtZoneId, stoppedAt, stoppedAtZoneId, comment, isBreak);
    }

    private TimeClockEntity(String tenantId, Long id, String owner, Instant startedAt, ZoneId startedAtZoneId, @Nullable Instant stoppedAt, @Nullable ZoneId stoppedAtZoneId, String comment, boolean isBreak) {
        super(tenantId);
        this.id = id;
        this.owner = owner;
        this.startedAt = startedAt;
        this.startedAtZoneId = startedAtZoneId.toString();
        this.stoppedAt = stoppedAt;
        this.stoppedAtZoneId = stoppedAtZoneId == null ? null : stoppedAtZoneId.toString();
        this.comment = comment;
        this.isBreak = isBreak;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getStartedAtZoneId() {
        return startedAtZoneId;
    }

    public Instant getStoppedAt() {
        return stoppedAt;
    }

    public String getStoppedAtZoneId() {
        return stoppedAtZoneId;
    }

    public String getComment() {
        return comment;
    }

    public boolean isBreak() {
        return isBreak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeClockEntity that = (TimeClockEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    static Builder builder() {
        return new Builder();
    }

    static Builder builder(TimeClockEntity timeClockEntity) {
        return new Builder()
            .id(timeClockEntity.getId())
            .owner(timeClockEntity.getOwner())
            .startedAt(timeClockEntity.getStartedAt())
            .startedAtZoneId(ZoneId.of(timeClockEntity.getStartedAtZoneId()))
            .stoppedAt(timeClockEntity.getStoppedAt())
            .stoppedAtZoneId(Optional.ofNullable(timeClockEntity.getStoppedAtZoneId()).map(ZoneId::of).orElse(null))
            .comment(timeClockEntity.getComment())
            .isBreak(timeClockEntity.isBreak());
    }

    public static class Builder {

        private Long id;
        private String owner;
        private Instant startedAt;
        private ZoneId startedAtZoneId;
        private Instant stoppedAt;
        private ZoneId stoppedAtZoneId;
        private String comment;
        private boolean isBreak;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder startedAtZoneId(ZoneId startedAtZoneId) {
            this.startedAtZoneId = startedAtZoneId;
            return this;
        }

        public Builder stoppedAt(Instant stoppedAt) {
            this.stoppedAt = stoppedAt;
            return this;
        }

        public Builder stoppedAtZoneId(ZoneId stoppedAtZoneId) {
            this.stoppedAtZoneId = stoppedAtZoneId;
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

        public TimeClockEntity build() {
            return new TimeClockEntity(id, owner, startedAt, startedAtZoneId, stoppedAt, stoppedAtZoneId, comment, isBreak);
        }
    }
}
