package de.focusshift.zeiterfassung.timeentry;

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
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

@Audited(withModifiedFlag = true)
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

    @NotAudited
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private Instant updatedAt;

    @Column(name = "is_break", nullable = false)
    private boolean isBreak;

    protected TimeEntryEntity(String tenantId, Long id, String owner, String comment, Instant start, ZoneId startZoneId, Instant end, ZoneId endZoneId, Instant updatedAt, boolean isBreak) {
        super(tenantId);
        this.id = id;
        this.owner = owner;
        this.comment = comment;
        this.start = start;
        this.startZoneId = startZoneId.toString();
        this.end = end;
        this.endZoneId = endZoneId.toString();
        this.updatedAt = updatedAt;
        this.isBreak = isBreak;
    }

    protected TimeEntryEntity(Long id, String owner, String comment, Instant start, ZoneId startZoneId, Instant end, ZoneId endZoneId, Instant updatedAt, boolean isBreak) {
        this(null, id, owner, comment, start, startZoneId, end, endZoneId, updatedAt, isBreak);
    }

    protected TimeEntryEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public String getStartZoneId() {
        return startZoneId;
    }

    public void setStartZoneId(String startZoneId) {
        this.startZoneId = startZoneId;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }

    public String getEndZoneId() {
        return endZoneId;
    }

    public void setEndZoneId(String endZoneId) {
        this.endZoneId = endZoneId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isBreak() {
        return isBreak;
    }

    public void setBreak(boolean aBreak) {
        isBreak = aBreak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeEntryEntity that = (TimeEntryEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TimeEntryEntity{" +
            "id=" + id +
            ", owner='" + owner + '\'' +
            '}';
    }
}
