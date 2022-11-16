package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.multitenant.AbstractTenantAwareEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "time_entry")
public class TimeEntryEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "time_entry_seq", sequenceName = "time_entry_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_entry_seq")
    protected Long id;

    @Column(name = "owner", nullable = false)
    @NotNull
    @Size(max = 255)
    private String owner;

    @Column(name = "comment")
    @Size(max = 255)
    private String comment;

    @Column(name = "start", nullable = false)
    @NotNull
    private Instant start;

    @Column(name = "start_zone_id", nullable = false)
    @NotNull
    private String startZoneId;

    @Column(name = "\"end\"", nullable = false)
    @NotNull
    private Instant end;

    @Column(name = "end_zone_id", nullable = false)
    @NotNull
    private String endZoneId;

    @Column(name = "updated_at", nullable = false)
    @NotNull
    private Instant updatedAt;

    protected TimeEntryEntity(String tenantId, Long id, String owner, String comment, Instant start, ZoneId startZoneId, Instant end, ZoneId endZoneId, Instant updatedAt) {
        super(tenantId);
        this.id = id;
        this.owner = owner;
        this.comment = comment;
        this.start = start;
        this.startZoneId = startZoneId.toString();
        this.end = end;
        this.endZoneId = endZoneId.toString();
        this.updatedAt = updatedAt;
    }

    protected TimeEntryEntity(Long id, String owner, String comment, Instant start, ZoneId startZoneId, Instant end, ZoneId endZoneId, Instant updatedAt) {
        this(null, id, owner, comment, start, startZoneId, end, endZoneId, updatedAt);
    }

    protected TimeEntryEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getComment() {
        return comment;
    }

    public Instant getStart() {
        return start;
    }

    public String getStartZoneId() {
        return startZoneId;
    }

    public Instant getEnd() {
        return end;
    }

    public String getEndZoneId() {
        return endZoneId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
